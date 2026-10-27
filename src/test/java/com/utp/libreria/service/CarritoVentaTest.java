package com.utp.libreria.service;

import com.utp.libreria.model.Producto;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/** RF07 a RF09: pruebas unitarias del carrito (sin base de datos). */
class CarritoVentaTest {

    private Producto producto(long id, int stock, boolean activo, String precio) {
        Producto p = new Producto();
        p.setId(id);
        p.setCodigo("T-" + id);
        p.setTitulo("Producto " + id);
        p.setPrecio(new BigDecimal(precio));
        p.setStock(stock);
        p.setActivo(activo);
        return p;
    }

    @Test
    void calculaElTotalDeVariasLineas() {
        CarritoVenta c = new CarritoVenta();
        c.agregar(producto(1, 10, true, "10.00"), 2);
        c.agregar(producto(2, 10, true, "5.50"), 3);
        assertEquals(new BigDecimal("36.50"), c.getTotal());
        assertEquals(5, c.getTotalUnidades());
    }

    @Test
    void rechazaCantidadMayorAlStock() {
        CarritoVenta c = new CarritoVenta();
        NegocioException e = assertThrows(NegocioException.class, () -> c.agregar(producto(1, 2, true, "10.00"), 3));
        assertTrue(e.getMessage().contains("Stock insuficiente"));
        assertTrue(c.isVacio());
    }

    @Test
    void acumulaLoYaAgregadoAlValidarElStock() {
        CarritoVenta c = new CarritoVenta();
        Producto p = producto(1, 5, true, "10.00");
        c.agregar(p, 3);
        assertThrows(NegocioException.class, () -> c.agregar(p, 3));
        assertEquals(3, c.getTotalUnidades());
    }

    @Test
    void rechazaProductoDesactivadoYCantidadInvalida() {
        CarritoVenta c = new CarritoVenta();
        assertThrows(NegocioException.class, () -> c.agregar(producto(1, 5, false, "10.00"), 1));
        assertThrows(NegocioException.class, () -> c.agregar(producto(2, 5, true, "10.00"), 0));
    }
}
