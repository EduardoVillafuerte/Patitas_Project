package com.patitas.config;

import com.patitas.Models.*;
import com.patitas.Models.Enums.*;
import com.patitas.Repository.MascotaRepository;
import com.patitas.Repository.UsuarioRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Datos de prueba. CommandLineRunner = Spring ejecuta run() una vez al arrancar la app.
 * Como la BD H2 está en memoria (se borra al apagar), estos datos se recrean en cada arranque.
 */
@Component
public class DataSeed implements CommandLineRunner {

    private final UsuarioRepository usuarios;
    private final MascotaRepository mascotas;

    public DataSeed(UsuarioRepository usuarios, MascotaRepository mascotas) {
        this.usuarios = usuarios;
        this.mascotas = mascotas;
    }

    @Override
    public void run(String... args) {
        if (usuarios.count() > 0) return;

        usuarios.save(usuario("Administrador", "admin@patitas.ec", "admin123", EnumRol.ADMIN, EnumEstadoCuenta.ACTIVO));

        Usuario refugio = usuario("Refugio Huellas de Quito", "refugio@patitas.ec", "refugio123", EnumRol.REFUGIO, EnumEstadoCuenta.ACTIVO);
        refugio.setDocumentoLegal("1790123456001");
        usuarios.save(refugio);

        Usuario pendiente = usuario("Fundación Patas Unidas", "pendiente@patitas.ec", "refugio123", EnumRol.REFUGIO, EnumEstadoCuenta.PENDIENTE_APROBACION);
        pendiente.setDocumentoLegal("1791234567001");
        usuarios.save(pendiente);

        Usuario adoptante = usuario("Ana Adoptante", "adoptante@patitas.ec", "adopta123", EnumRol.ADOPTANTE, EnumEstadoCuenta.ACTIVO);
        usuarios.save(adoptante); // sin perfil: lo llenará en el primer login

        mascotas.save(mascota(refugio, "Luna", "Perro", 3, 4, 4, 60,
                "Cariñosa y juguetona. Ama correr en espacios abiertos.",
                Set.of(EnumEtiqueta.REQUIERE_PATIO)));
        mascotas.save(mascota(refugio, "Michi", "Gato", 2, 2, 3, 25,
                "Tranquilo, ideal para departamento. Se lleva bien con niños.",
                Set.of()));
        mascotas.save(mascota(refugio, "Rocky", "Perro", 5, 5, 2, 90,
                "Muy enérgico. Requiere paseos largos y un hogar sin niños pequeños.",
                Set.of(EnumEtiqueta.REQUIERE_PATIO, EnumEtiqueta.NO_APTO_NINOS)));
        mascotas.save(mascota(refugio, "Canela", "Perro", 1, 3, 5, 45,
                "Cachorra rescatada. No tolera quedarse sola mucho tiempo.",
                Set.of(EnumEtiqueta.ANSIEDAD_SEPARACION)));
        mascotas.save(mascota(refugio, "Simba", "Gato", 7, 1, 2, 140,
                "Gato senior con tratamiento renal. Necesita dieta especial.",
                Set.of(EnumEtiqueta.COSTO_ALTO)));
    }

    private Usuario usuario(String nombre, String email, String clave, EnumRol rol, EnumEstadoCuenta estado) {
        Usuario u = new Usuario();
        u.setNombre(nombre);
        u.setEmail(email);
        u.setPassword(clave);
        u.setRol(rol);
        u.setEstado(estado);
        return u;
    }

    private Mascota mascota(Usuario refugio, String nombre, String especie, int edad, int energia,
                            int sociabilidad, double presupuesto, String desc, Set<EnumEtiqueta> etiquetas) {
        Mascota m = new Mascota();
        m.setRefugio(refugio);
        m.setNombre(nombre);
        m.setEspecie(especie);
        m.setEdadAnios(edad);
        m.setNivelEnergia(energia);
        m.setSociabilidad(sociabilidad);
        m.setPresupuestoMinimo(presupuesto);
        m.setDescripcion(desc);
        m.getEtiquetas().addAll(etiquetas);
        m.setVacunado(true);
        m.setEsterilizado(edad > 1);
        m.setFichaMedica("Desparasitado. Vacunas al día.");
        return m;
    }
}
