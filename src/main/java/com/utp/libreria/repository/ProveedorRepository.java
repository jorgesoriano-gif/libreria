package com.utp.libreria.repository;

import com.utp.libreria.model.Proveedor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProveedorRepository extends JpaRepository<Proveedor, Long> {
    Optional<Proveedor> findByRuc(String ruc);

    List<Proveedor> findByActivoTrueOrderByRazonSocial();

    @Query("""
           select p from Proveedor p
           where lower(p.razonSocial) like lower(concat('%', :q, '%'))
              or p.ruc like concat('%', :q, '%')
           order by p.razonSocial
           """)
    List<Proveedor> buscar(@Param("q") String q);
}
