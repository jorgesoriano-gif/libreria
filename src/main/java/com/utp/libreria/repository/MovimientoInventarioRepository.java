package com.utp.libreria.repository;

import com.utp.libreria.model.MovimientoInventario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MovimientoInventarioRepository extends JpaRepository<MovimientoInventario, Long> {
    List<MovimientoInventario> findTop100ByOrderByFechaDescIdDesc();

    List<MovimientoInventario> findTop100ByProductoIdOrderByFechaDescIdDesc(Long productoId);

    List<MovimientoInventario> findByOrdenId(Long ordenId);

    List<MovimientoInventario> findByVentaId(Long ventaId);
}
