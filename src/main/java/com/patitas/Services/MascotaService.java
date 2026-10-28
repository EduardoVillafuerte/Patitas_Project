package com.patitas.Services;

import com.patitas.dto.MascotaForm;
import com.patitas.exception.BusinessException;
import com.patitas.Models.Enums.*;
import com.patitas.Models.*;
import com.patitas.Repository.MascotaRepository;
import com.patitas.Repository.PostulacionRepository;
import com.patitas.Repository.SeguimientoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MascotaService {

    private final MascotaRepository mascotas;
    private final PostulacionRepository postulaciones;
    private final SeguimientoRepository seguimientos;

    @Transactional
    public Mascota crear(Usuario refugio, MascotaForm f) {
        Mascota m = new Mascota();
        m.setRefugio(refugio);
        
        // Atributos inmutables (se asignan SOLO al crear)
        m.setNombre(f.getNombre().trim());
        m.setEspecie(f.getEspecie().trim());
        
        aplicarMutables(m, f);
        return mascotas.save(m);
    }

    @Transactional
    public Mascota actualizar(Usuario refugio, Long id, MascotaForm f) {
        Mascota m = propia(refugio, id);

        // Si la mascota ya está ADOPTADA, el backend bloquea por completo la edición de todos los campos.
        // Únicamente permite procesar si el refugio decidió cambiar su estado para regresarla a adopción.
        if (m.getEstado() == EnumEstadoMascota.ADOPTADO) {
            if (f.getEstado() != null && f.getEstado() != EnumEstadoMascota.ADOPTADO) {
                revocarAdopcionActiva(m);
                m.setEstado(f.getEstado());
            }
            return mascotas.save(m);
        }

        // Si NO está adoptada, aplicamos los cambios mutables (ignorando nombre, especie, etc. que ya son inmutables)
        aplicarMutables(m, f);
        return mascotas.save(m);
    }

    /** Mascota activa que pertenece a ese refugio, o error. */
    public Mascota propia(Usuario refugio, Long id) {
        return mascotas.findById(id)
                .filter(Mascota::isActivo)
                .filter(m -> m.getRefugio().getId().equals(refugio.getId()))
                .orElseThrow(() -> new BusinessException("Mascota no encontrada"));
    }

    /** Soft delete: activo = false. Prohibido si la mascota está ADOPTADA. */
    @Transactional
    public void eliminar(Usuario quien, Long id) {
        Mascota m = mascotas.findById(id)
                .orElseThrow(() -> new BusinessException("Mascota no encontrada"));
        boolean esDueno = m.getRefugio().getId().equals(quien.getId());
        if (quien.getRol() != EnumRol.ADMIN && !esDueno) {
            throw new BusinessException("No puedes eliminar esta mascota");
        }
        if (m.getEstado() == EnumEstadoMascota.ADOPTADO) {
            throw new BusinessException("No se puede eliminar una mascota adoptada. Si regresó al refugio, edita su ficha y cambia su estado a Disponible.");
        }
        m.setActivo(false);
        mascotas.save(m);
    }

    /** Aplica únicamente los campos que sí están permitidos editar a lo largo del tiempo */
    private void aplicarMutables(Mascota m, MascotaForm f) {
        m.setEdadAnios(f.getEdadAnios());
        m.setDescripcion(f.getDescripcion());
        m.setNivelEnergia(f.getNivelEnergia());
        m.setSociabilidad(f.getSociabilidad());
        m.setPresupuestoMinimo(f.getPresupuestoMinimo());
        m.getEtiquetas().clear();
        m.getEtiquetas().addAll(f.getEtiquetas());
        m.setVacunado(f.isVacunado());
        m.setEsterilizado(f.isEsterilizado());
        m.setFichaMedica(f.getFichaMedica());

        // El estado manual solo se aplica si no estamos intentando saltar a ADOPTADO desde el form
        if (f.getEstado() != null && f.getEstado() != EnumEstadoMascota.ADOPTADO) {
            m.setEstado(f.getEstado());
        }

        MultipartFile img = f.getImagen();
        if (img != null && !img.isEmpty()) {
            String tipo = img.getContentType();
            if (tipo == null || !tipo.startsWith("image/")) {
                throw new BusinessException("El archivo debe ser una imagen");
            }
            try {
                m.setImagen(img.getBytes());
                m.setImagenTipo(tipo);
            } catch (IOException e) {
                throw new BusinessException("No se pudo leer la imagen");
            }
        }
    }

    private void revocarAdopcionActiva(Mascota m) {
        List<PostulacionAdopcion> delRefugio = postulaciones.findByMascotaRefugioOrderByFechaDesc(m.getRefugio());
        for (PostulacionAdopcion p : delRefugio) {
            if (p.getMascota().getId().equals(m.getId()) &&
                (p.getEstado() == EnumEstadoPostulacion.COMPLETADA || p.getEstado() == EnumEstadoPostulacion.APROBADA)) {
                p.setEstado(EnumEstadoPostulacion.RECHAZADA);
                p.setMotivoRechazo("[RETIRO POR REFUGIO] Mascota devuelta a adopción por falta de evidencias o sospecha/confirmación de maltrato.");
                postulaciones.save(p);

                List<Seguimiento> segs = seguimientos.findByPostulacionMascotaRefugioOrderByFechaLimiteAsc(m.getRefugio())
                        .stream()
                        .filter(s -> s.getPostulacion().getId().equals(p.getId()))
                        .toList();
                seguimientos.deleteAll(segs);
            }
        }
    }
}