package com.patitas.Services;

import com.patitas.dto.MascotaForm;
import com.patitas.exception.BusinessException;
import com.patitas.Models.Enums.*;
import com.patitas.Models.*;
import com.patitas.Repository.MascotaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
@RequiredArgsConstructor
public class MascotaService {

    private final MascotaRepository mascotas;

    @Transactional
    public Mascota crear(Usuario refugio, MascotaForm f) {
        Mascota m = new Mascota();
        m.setRefugio(refugio);
        aplicar(m, f);
        return mascotas.save(m);
    }

    @Transactional
    public Mascota actualizar(Usuario refugio, Long id, MascotaForm f) {
        Mascota m = propia(refugio, id);
        aplicar(m, f);
        return mascotas.save(m);
    }

    /** Mascota activa que pertenece a ese refugio, o error. */
    public Mascota propia(Usuario refugio, Long id) {
        return mascotas.findById(id)
                .filter(Mascota::isActivo)
                .filter(m -> m.getRefugio().getId().equals(refugio.getId()))
                .orElseThrow(() -> new BusinessException("Mascota no encontrada"));
    }

    /** Soft delete: activo = false. El historial queda para las métricas. */
    @Transactional
    public void eliminar(Usuario quien, Long id) {
        Mascota m = mascotas.findById(id)
                .orElseThrow(() -> new BusinessException("Mascota no encontrada"));
        boolean esDueno = m.getRefugio().getId().equals(quien.getId());
        if (quien.getRol() != EnumRol.ADMIN && !esDueno) {
            throw new BusinessException("No puedes eliminar esta mascota");
        }
        m.setActivo(false);
        mascotas.save(m);
    }

    private void aplicar(Mascota m, MascotaForm f) {
        m.setNombre(f.getNombre().trim());
        m.setEspecie(f.getEspecie().trim());
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

        // "Adoptado" solo se alcanza completando una adopción, nunca desde el formulario.
        if (m.getEstado() != EnumEstadoMascota.ADOPTADO && f.getEstado() != EnumEstadoMascota.ADOPTADO) {
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
}
