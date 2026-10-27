package com.utp.libreria.service;

import com.utp.libreria.model.*;
import com.utp.libreria.repository.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicInteger;

/** Utilidades comunes: cada prueba crea sus propios productos con códigos únicos (H2 en memoria). */
abstract class PruebaBase {

    private static final AtomicInteger SECUENCIA = new AtomicInteger();

    @Autowired protected ProductoRepository productos;
    @Autowired protected CategoriaRepository categorias;
    @Autowired protected ProveedorRepository proveedores;

    protected Categoria categoria() {
        return categorias.findAllByOrderByNombre().get(0);
    }

    protected Producto crearProducto(int stock, String precio) {
        Producto p = new Producto();
        p.setCategoria(categoria());
        p.setCodigo("TST-" + System.nanoTime() + "-" + SECUENCIA.incrementAndGet());
        p.setTitulo("Producto de prueba");
        p.setPrecio(new BigDecimal(precio));
        p.setStock(stock);
        p.setStockMinimo(2);
        return productos.save(p);
    }

    protected int stockDe(Long productoId) {
        return productos.findById(productoId).orElseThrow().getStock();
    }

    protected Proveedor crearProveedor() {
        Proveedor pr = new Proveedor();
        long n = System.nanoTime() % 100_000_000_000L;
        pr.setRuc(String.format("%011d", n));
        pr.setRazonSocial("Proveedor de prueba");
        return proveedores.save(pr);
    }
}
