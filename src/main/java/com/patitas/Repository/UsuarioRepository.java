package com.patitas.Repository;

import com.patitas.Models.Enums.EnumEstadoCuenta;
import com.patitas.Models.Enums.EnumRol;
import com.patitas.Models.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByEmail(String email);
    boolean existsByEmail(String email);
    List<Usuario> findByRolAndEstado(EnumRol rol, EnumEstadoCuenta estado);
    List<Usuario> findByRolNotOrderByIdDesc(EnumRol rol);
    long countByRol(EnumRol rol);
    long countByRolAndEstado(EnumRol rol, EnumEstadoCuenta estado);
}
