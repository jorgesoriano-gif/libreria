package com.utp.libreria.repository;

import com.utp.libreria.model.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {
    Optional<Cliente> findByDocumento(String documento);

    @Query("""
           select c from Cliente c
           where lower(c.nombre) like lower(concat('%', :q, '%'))
              or coalesce(c.documento, '') like concat('%', :q, '%')
           order by c.nombre
           """)
    List<Cliente> buscar(@Param("q") String q);
}
