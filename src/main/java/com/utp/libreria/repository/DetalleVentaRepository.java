package com.utp.libreria.repository;

import com.utp.libreria.model.DetalleVenta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface DetalleVentaRepository extends JpaRepository<DetalleVenta, Long> {

    /** Devuelve: código, título, unidades vendidas y monto. */
    @Query("""
           select p.codigo, p.titulo, sum(d.cantidad), sum(d.cantidad * d.precioUnitario)
           from DetalleVenta d join d.producto p join d.venta v
           where v.fecha >= :desde and v.fecha < :hasta
           group by p.codigo, p.titulo
           order by sum(d.cantidad) desc
           """)
    List<Object[]> masVendidos(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);
}
