package com.utp.libreria.repository;

import com.utp.libreria.model.Venta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface VentaRepository extends JpaRepository<Venta, Long> {

    Optional<Venta> findTopByOrderByNroComprobanteDesc();

    List<Venta> findByFechaGreaterThanEqualAndFechaLessThanOrderByFecha(LocalDateTime desde, LocalDateTime hasta);

    @Query("""
           select v from Venta v
           where v.fecha >= :desde and v.fecha < :hasta
             and v.nroComprobante like concat('%', :nro, '%')
             and (:cliente = '' or (v.cliente is not null
                  and (lower(v.cliente.nombre) like lower(concat('%', :cliente, '%'))
                       or coalesce(v.cliente.documento, '') like concat('%', :cliente, '%'))))
           order by v.fecha desc
           """)
    List<Venta> buscar(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta,
                       @Param("nro") String nro, @Param("cliente") String cliente);
}
