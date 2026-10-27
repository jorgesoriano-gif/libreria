package com.utp.libreria.repository;

import com.utp.libreria.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByUsuario(String usuario);
    List<Usuario> findAllByOrderByNombre();
    long countByRolIdAndActivoTrue(Long rolId);
}
