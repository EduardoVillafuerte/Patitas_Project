package com.patitas.Services;

import com.patitas.dto.PerfilForm;
import com.patitas.dto.RegistroForm;
import com.patitas.exception.BusinessException;
import com.patitas.validation.IdentificacionEc;

import org.springframework.security.crypto.password.PasswordEncoder;

import com.patitas.Models.Enums.EnumRol;
import com.patitas.Models.Enums.EnumEstadoCuenta;
import com.patitas.Models.Usuario;
import com.patitas.Repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/*Lógica de usuarios: login, registro, perfil y acciones del administrador.
 *Implementa UserDetailsService: es la interfaz que Spring Security usa en el login
 */
@Service
@RequiredArgsConstructor
public class UsuarioService implements UserDetailsService {

    private final UsuarioRepository repo;
    private final PasswordEncoder encoder;

    /**
     * LOGIN: Spring llama a este método con lo que el usuario escribió en el campo "username"
     * (en nuestro caso, el correo). Devolvemos el Usuario; luego Spring compara por su cuenta
     * la contraseña escrita con usuario.getPassword() (guardada con BCrypt, ver SecurityConfig).
     * Si lanzamos UsernameNotFoundException, Spring responde "login fallido" (/login?error).
     */
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return repo.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new UsernameNotFoundException("No existe el usuario " + email));
    }

    /**
     * Devuelve el usuario que tiene la sesión abierta, recargado desde la BD para que los datos
     * estén frescos (por ejemplo, si el admin lo baneó hace un minuto).
     * 'auth.getName()' es el correo con el que inició sesión.
     */
    public Usuario actual(Authentication auth) {
        return repo.findByEmail(auth.getName())
                .orElseThrow(() -> new BusinessException("Sesión inválida"));
    }

    public boolean emailExiste(String email) {
        return repo.existsByEmail(email.trim().toLowerCase());
    }


    public boolean cedulaExiste(String cedula) {
        return repo.existsByCedula(cedula.trim());
    }

    public boolean documentoExiste(String documento) {
        return repo.existsByDocumentoLegal(documento.trim());
    }

    /**
     * REGISTRO. Esta es la última barrera antes de la base de datos: aunque el navegador o el
     * controlador ya hayan validado, aquí se vuelve a validar la identificación (dato sensible)
     * y se guarda la contraseña cifrada con BCrypt.
     */
    @Transactional //si algo falla a la mitad, se deshace todo
    public Usuario registrar(RegistroForm f) {
        if (f.getTipoCuenta() == EnumRol.ADMIN) {
            throw new BusinessException("Tipo de cuenta no permitido");
        }
        String email = f.getEmail().trim().toLowerCase();
        if (repo.existsByEmail(email)) {
            throw new BusinessException("Ya existe una cuenta con ese correo");
        }

        Usuario u = new Usuario();
        u.setNombre(f.getNombre().trim());
        u.setEmail(email);
        u.setPassword(encoder.encode(f.getPassword()));
        u.setRol(f.getTipoCuenta());

        if (f.getTipoCuenta() == EnumRol.REFUGIO) {
            String ruc = f.getDocumentoLegal() == null ? "" : f.getDocumentoLegal().trim();
            if (!IdentificacionEc.esRucValido(ruc)) {
                throw new BusinessException("El RUC no es válido");
            }
            if (repo.existsByDocumentoLegal(ruc)) {
                throw new BusinessException("Ya existe un refugio registrado con ese RUC");
            }
            u.setDocumentoLegal(ruc);
            u.setEstado(EnumEstadoCuenta.PENDIENTE_APROBACION);
        } else {
            String cedula = f.getCedula() == null ? "" : f.getCedula().trim();
            if (!IdentificacionEc.esCedulaValida(cedula)) {
                throw new BusinessException("La cédula no es válida");
            }
            // Si esa cédula pertenece a una cuenta baneada, también cae aquí: no puede volver con otro correo.
            if (repo.existsByCedula(cedula)) {
                throw new BusinessException("Ya existe una cuenta registrada con esa cédula");
            }
            u.setCedula(cedula);
            u.setEstado(EnumEstadoCuenta.ACTIVO);
        }
        return repo.save(u);
    }

    /** Guarda el perfil de estilo de vida; con él se calcula el % de compatibilidad. */
    @Transactional
    public void guardarPerfil(Usuario u, PerfilForm f) {
        u.setNombre(f.getNombre().trim());
        u.setFechaNacimiento(f.getFechaNacimiento());
        u.setCiudad(f.getCiudad().trim());
        u.setOcupacion(vacioANull(f.getOcupacion()));
        u.setTelefono(vacioANull(f.getTelefono()));
        u.setHobbies(vacioANull(f.getHobbies()));
        u.setSobreMi(vacioANull(f.getSobreMi()));
        u.setExperiencia(f.getExperiencia());
        u.setHorasFueraCasa(f.getHorasFueraCasa());
        u.setTipoVivienda(f.getTipoVivienda());
        u.setPresupuestoMensual(f.getPresupuestoMensual());
        u.setTieneNinos(f.isTieneNinos());
        u.setPerfilCompleto(true);
        repo.save(u);
    }

    private static String vacioANull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    // ---- Acciones de administración ----
    /** Cambia el estado de una cuenta (aprobar refugio, banear, desbanear). */
    @Transactional
    public void cambiarEstado(Long usuarioId, EnumEstadoCuenta nuevo) {
        Usuario u = repo.findById(usuarioId)
                .orElseThrow(() -> new BusinessException("Usuario no encontrado"));
        if (u.getRol() == EnumRol.ADMIN) {
            throw new BusinessException("No se puede modificar a un administrador");
        }
        // Dato sensible: el RUC se revalida en el servidor justo antes de aprobar al refugio.
        if (nuevo == EnumEstadoCuenta.ACTIVO && u.getRol() == EnumRol.REFUGIO
                && u.getEstado() == EnumEstadoCuenta.PENDIENTE_APROBACION
                && !IdentificacionEc.esRucValido(u.getDocumentoLegal())) {
            throw new BusinessException("No se puede aprobar: el RUC del refugio no es válido");
        }
        u.setEstado(nuevo);
        repo.save(u);
    }
}
