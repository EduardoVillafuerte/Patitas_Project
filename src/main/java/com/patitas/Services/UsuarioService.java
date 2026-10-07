package com.patitas.Services;

import com.patitas.dto.PerfilForm;
import com.patitas.dto.RegistroForm;
import com.patitas.exception.BusinessException;

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

    /**
     * LOGIN: Spring llama a este método con lo que el usuario escribió en el campo "username"
     * (en nuestro caso, el correo). Devolvemos el Usuario; luego Spring compara por su cuenta
     * la contraseña escrita con usuario.getPassword() (texto plano, ver SecurityConfig).
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


    @Transactional //si algo falla a la mitad, se deshace todo
    public Usuario registrar(RegistroForm f) {
        if (f.getTipoCuenta() == EnumRol.ADMIN) {
            throw new BusinessException("Tipo de cuenta no permitido");
        }
        Usuario u = new Usuario();
        u.setNombre(f.getNombre().trim());
        u.setEmail(f.getEmail().trim().toLowerCase());
        u.setPassword(f.getPassword()); 
        u.setRol(f.getTipoCuenta());
        if (f.getTipoCuenta() == EnumRol.REFUGIO) {
            u.setDocumentoLegal(f.getDocumentoLegal().trim());
            u.setEstado(EnumEstadoCuenta.PENDIENTE_APROBACION);
        } else {
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
        u.setEstado(nuevo);
        repo.save(u);
    }
}
