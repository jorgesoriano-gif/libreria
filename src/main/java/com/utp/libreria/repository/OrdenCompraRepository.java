package com.utp.libreria.repository;

import com.utp.libreria.model.OrdenCompra;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OrdenCompraRepository extends JpaRepository<OrdenCompra, Long> {

    Optional<OrdenCompra> findTopByOrderByNumeroDesc();

    long countByProveedorId(Long proveedorId);

    /** prov = 0 significa todos los proveedores. El estado se filtra en el servicio. */
    @Query("""
           select o from OrdenCompra o
           where o.fechaEmision >= :desde and o.fechaEmision < :hasta
             and (:prov = 0 or o.proveedor.id = :prov)
           order by o.fechaEmision desc
           """)
    List<OrdenCompra> buscar(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta,
                             @Param("prov") long prov);

    /** Bloqueo pesimista: evita dobles recepciones concurrentes de la misma orden. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from OrdenCompra o where o.id = :id")
    Optional<OrdenCompra> bloquearPorId(@Param("id") Long id);
}
