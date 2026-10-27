package com.utp.libreria.repository;

import com.utp.libreria.model.DetalleOrdenCompra;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DetalleOrdenCompraRepository extends JpaRepository<DetalleOrdenCompra, Long> {
    List<DetalleOrdenCompra> findByOrdenId(Long ordenId);
}
