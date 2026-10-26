package com.utp.libreria.repository;

import com.utp.libreria.model.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoriaRepository extends JpaRepository<Categoria, Long> {
    List<Categoria> findAllByOrderByNombre();
    Optional<Categoria> findByNombreIgnoreCase(String nombre);
}
