package com.patitas.Services;

import com.patitas.dto.FeriaForm;
import com.patitas.dto.FeriaItem;
import com.patitas.dto.FeriaRefugioItem;
import com.patitas.exception.BusinessException;
import com.patitas.Models.Enums.EnumEstadoCuenta;
import com.patitas.Models.Enums.EnumRol;
import com.patitas.Models.Feria;
import com.patitas.Models.InscripcionFeria;
import com.patitas.Models.Usuario;
import com.patitas.Repository.FeriaRepository;
import com.patitas.Repository.InscripcionFeriaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FeriaService {

    private final FeriaRepository ferias;
    private final InscripcionFeriaRepository inscripciones;
    private final NotificacionService notificaciones;

    // ---------------- Refugio ----------------

    @Transactional
    public Feria crear(Usuario refugio, FeriaForm f) {
        if (refugio.getRol() != EnumRol.REFUGIO || !refugio.isActivo()) {
            throw new BusinessException("Tu refugio aún no está activo.");
        }
        if (f.getHoraFin() != null && !f.getHoraFin().isAfter(f.getHoraInicio())) {
            throw new BusinessException("La hora de fin debe ser posterior a la de inicio.");
        }
        Feria feria = new Feria();
        feria.setRefugio(refugio);
        feria.setTitulo(f.getTitulo().trim());
        feria.setDescripcion(vacioANull(f.getDescripcion()));
        feria.setFecha(f.getFecha());
        feria.setHoraInicio(f.getHoraInicio());
        feria.setHoraFin(f.getHoraFin());
        feria.setLugar(f.getLugar().trim());
        feria.setCupo(f.getCupo());
        return ferias.save(feria);
    }

    /** Cancelar = ocultar (activa = false). Las inscripciones se conservan como historial. */
    @Transactional
    public void cancelar(Usuario refugio, Long feriaId) {
        Feria f = ferias.findById(feriaId)
                .filter(x -> x.getRefugio().getId().equals(refugio.getId()))
                .orElseThrow(() -> new BusinessException("Feria no encontrada."));
        if (!f.isActiva()) return;
        f.setActiva(false);
        // Avisa a quienes estaban inscritos.
        for (InscripcionFeria i : inscripciones.findByFeriaOrderByFechaAsc(f)) {
            notificaciones.feriaCancelada(f, i.getAdoptante());
        }
    }

    @Transactional(readOnly = true)
    public List<FeriaRefugioItem> delRefugio(Usuario refugio) {
        LocalDate hoy = LocalDate.now();
        return ferias.findByRefugioOrderByFechaDesc(refugio).stream()
                .map(f -> new FeriaRefugioItem(f, inscripciones.findByFeriaOrderByFechaAsc(f), f.getFecha().isBefore(hoy)))
                .toList();
    }

    // ---------------- Lista pública ----------------

    /** Ferias activas de hoy en adelante. 'quien' puede ser null (visitante sin sesión). */
    @Transactional(readOnly = true)
    public List<FeriaItem> proximas(Usuario quien) {
        return ferias.findByActivaTrueAndFechaGreaterThanEqualOrderByFechaAscHoraInicioAsc(LocalDate.now()).stream()
                .map(f -> {
                    long n = inscripciones.countByFeria(f);
                    boolean yo = quien != null && quien.getRol() == EnumRol.ADOPTANTE
                            && inscripciones.existsByFeriaAndAdoptante(f, quien);
                    boolean lleno = f.getCupo() != null && n >= f.getCupo();
                    return new FeriaItem(f, n, yo, lleno);
                })
                .toList();
    }

    // ---------------- Adoptante ----------------

    @Transactional
    public void inscribir(Usuario adoptante, Long feriaId) {
        if (adoptante.getRol() != EnumRol.ADOPTANTE) {
            throw new BusinessException("Solo los adoptantes se pueden inscribir.");
        }
        if (adoptante.getEstado() == EnumEstadoCuenta.BANNED) {
            throw new BusinessException("Tu cuenta está en la lista negra: no puedes inscribirte a ferias.");
        }
        if (adoptante.getEstado() != EnumEstadoCuenta.ACTIVO) {
            throw new BusinessException("Tu cuenta aún no está activa.");
        }
        // Bloquea la feria mientras se cuenta el cupo (evita sobrepasarlo con inscripciones simultáneas).
        Feria f = ferias.buscarParaInscribir(feriaId)
                .filter(Feria::isActiva)
                .orElseThrow(() -> new BusinessException("La feria no existe o fue cancelada."));
        if (f.getFecha().isBefore(LocalDate.now())) {
            throw new BusinessException("Esta feria ya pasó.");
        }
        if (inscripciones.existsByFeriaAndAdoptante(f, adoptante)) {
            throw new BusinessException("Ya estás inscrito en esta feria.");
        }
        if (f.getCupo() != null && inscripciones.countByFeria(f) >= f.getCupo()) {
            throw new BusinessException("Ya no quedan cupos para esta feria.");
        }
        InscripcionFeria i = new InscripcionFeria();
        i.setFeria(f);
        i.setAdoptante(adoptante);
        inscripciones.save(i);
        notificaciones.nuevaInscripcionFeria(f, adoptante);
    }

    @Transactional
    public void cancelarInscripcion(Usuario adoptante, Long feriaId) {
        Feria f = ferias.findById(feriaId)
                .orElseThrow(() -> new BusinessException("Feria no encontrada."));
        InscripcionFeria i = inscripciones.findByFeriaAndAdoptante(f, adoptante)
                .orElseThrow(() -> new BusinessException("No estabas inscrito en esta feria."));
        inscripciones.delete(i);
    }

    private static String vacioANull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}
