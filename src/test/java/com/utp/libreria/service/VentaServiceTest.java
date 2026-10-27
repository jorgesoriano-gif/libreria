package com.utp.libreria.service;

import com.utp.libreria.model.Producto;
import com.utp.libreria.model.TipoMovimiento;
import com.utp.libreria.model.Venta;
import com.utp.libreria.repository.MovimientoInventarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** TC03, TC04 y TC09: la venta es transaccional (RF09, RF10, RF17). */
@SpringBootTest
class VentaServiceTest extends PruebaBase {

    @Autowired VentaService ventas;
    @Autowired MovimientoInventarioRepository movimientos;

    private List<CarritoVenta.Item> items(Producto p, int cantidad) {
        CarritoVenta c = new CarritoVenta();
        // el carrito valida contra el stock del momento: se arma con stock suficiente
        Producto copia = new Producto();
        copia.setId(p.getId());
        copia.setCodigo(p.getCodigo());
        copia.setTitulo(p.getTitulo());
        copia.setPrecio(p.getPrecio());
        copia.setStock(Math.max(p.getStock(), cantidad));
        c.agregar(copia, cantidad);
        return c.getItems();
    }

    @Test
    void tc03_rechazaVentaConStockInsuficienteYNoCambiaElStock() {
        Producto p = crearProducto(2, "10.00");
        NegocioException e = assertThrows(NegocioException.class,
                () -> ventas.registrar(null, "vendedor1", items(p, 3)));
        assertTrue(e.getMessage().contains("Stock insuficiente"));
        assertEquals(2, stockDe(p.getId()));
    }

    @Test
    void tc04_ventaValidaDescuentaElStockYRegistraLaSalida() {
        Producto p = crearProducto(5, "10.00");
        Venta v = ventas.registrar(null, "vendedor1", items(p, 2));

        assertEquals(3, stockDe(p.getId()));
        assertEquals(0, new BigDecimal("20.00").compareTo(v.getTotal()));
        var salidas = movimientos.findByVentaId(v.getId());
        assertEquals(1, salidas.size());
        assertEquals(TipoMovimiento.SALIDA, salidas.get(0).getTipo());
        assertEquals(2, salidas.get(0).getCantidad());
    }

    @Test
    void tc09_dosVentasSeguidasTienenComprobantesDistintos() {
        Producto p = crearProducto(10, "1.00");
        Venta a = ventas.registrar(null, "vendedor1", items(p, 1));
        Venta b = ventas.registrar(null, "vendedor1", items(p, 1));
        assertNotEquals(a.getNroComprobante(), b.getNroComprobante());
        assertTrue(a.getNroComprobante().matches("INT-\\d{6}"));
    }

    @Test
    void rechazaCarritoVacio() {
        assertThrows(NegocioException.class, () -> ventas.registrar(null, "vendedor1", List.of()));
    }
}
