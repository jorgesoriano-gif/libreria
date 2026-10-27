package com.utp.libreria.repository;

import com.utp.libreria.model.Producto;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductoRepository extends JpaRepository<Producto, Long> {

    Optional<Producto> findByCodigo(String codigo);

    long countByCategoriaId(Long categoriaId);

    long countByActivoTrue();

    List<Producto> findAllByOrderByTitulo();

    List<Producto> findByActivoTrueOrderByTitulo();

    /** Búsqueda por código, título, autor o categoría. cat = 0 significa todas. */
    @Query("""
           select p from Producto p
           where (lower(p.codigo) like lower(concat('%', :q, '%'))
                  or lower(p.titulo) like lower(concat('%', :q, '%'))
                  or lower(coalesce(p.autor, '')) like lower(concat('%', :q, '%'))
                  or lower(p.categoria.nombre) like lower(concat('%', :q, '%')))
             and (:cat = 0 or p.categoria.id = :cat)
             and (:soloActivos = false or p.activo = true)
           order by p.titulo
           """)
    List<Producto> buscar(@Param("q") String q, @Param("cat") long cat,
                          @Param("soloActivos") boolean soloActivos);

    @Query("select p from Producto p where p.activo = true and p.stock <= p.stockMinimo order by p.stock, p.titulo")
    List<Producto> bajoMinimo();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Producto p where p.id = :id")
    Optional<Producto> bloquearPorId(@Param("id") Long id);
}
