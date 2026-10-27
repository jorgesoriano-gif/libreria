package com.utp.libreria.service;

import com.utp.libreria.model.*;
import com.utp.libreria.repository.MovimientoInventarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** TC11, TC13 y TC19: órdenes de compra y recepción atómica (RF19 a RF21). */
@SpringBootTest
class CompraServiceTest extends PruebaBase {

    @Autowired CompraService compras;
    @Autowired MovimientoInventarioRepository movimientos;

    private OrdenCompra ordenCon(Producto a, int cantA, Producto b, int cantB) {
        Proveedor pr = crearProveedor();
        return compras.crear(pr.getId(), null, List.of(
                new CompraService.Linea(a.getId(), cantA, new BigDecimal("4.00")),
                new CompraService.Linea(b.getId(), cantB, new BigDecimal("2.50"))), "inventario1");
    }

    @Test
    void tc11_creaOrdenPendienteConTotalCorrectoSinTocarElStock() {
        Producto a = crearProducto(5, "10.00");
        Producto b = crearProducto(7, "10.00");
        OrdenCompra o = ordenCon(a, 10, b, 4);

        assertEquals(EstadoOrden.PENDIENTE, o.getEstado());
        assertEquals(0, new BigDecimal("50.00").compareTo(o.getTotal()));   // 10*4.00 + 4*2.50
        assertEquals(2, compras.detalleDe(o.getId()).size());
        assertEquals(5, stockDe(a.getId()));
        assertEquals(7, stockDe(b.getId()));
        assertTrue(o.getNumero().matches("OC-\\d{6}"));
    }

    @Test
    void recepcionSumaElStockRegistraEntradasYMarcaRecibida() {
        Producto a = crearProducto(5, "10.00");
        Producto b = crearProducto(7, "10.00");
        OrdenCompra o = ordenCon(a, 10, b, 4);

        OrdenCompra r = compras.recepcionar(o.getId(), "inventario1");

        assertEquals(EstadoOrden.RECIBIDA, r.getEstado());
        assertNotNull(r.getFechaRecepcion());
        assertEquals(15, stockDe(a.getId()));
        assertEquals(11, stockDe(b.getId()));
        var entradas = movimientos.findByOrdenId(o.getId());
        assertEquals(2, entradas.size());
        assertTrue(entradas.stream().allMatch(m -> m.getTipo() == TipoMovimiento.ENTRADA));
    }

    @Test
    void tc13_noPermiteRecepcionarDosVeces() {
        Producto a = crearProducto(5, "10.00");
        Producto b = crearProducto(7, "10.00");
        OrdenCompra o = ordenCon(a, 10, b, 4);
        compras.recepcionar(o.getId(), "inventario1");

        NegocioException e = assertThrows(NegocioException.class,
                () -> compras.recepcionar(o.getId(), "inventario1"));
        assertTrue(e.getMessage().contains("ya fue recepcionada"));
        assertEquals(15, stockDe(a.getId()));   // sin duplicar
    }

    @Test
    void rechazaOrdenSinLineasOProveedorDesactivado() {
        Proveedor pr = crearProveedor();
        assertThrows(NegocioException.class, () -> compras.crear(pr.getId(), null, List.of(), "inventario1"));

        pr.setActivo(false);
        proveedores.save(pr);
        Producto a = crearProducto(1, "1.00");
        assertThrows(NegocioException.class, () -> compras.crear(pr.getId(), null,
                List.of(new CompraService.Linea(a.getId(), 1, BigDecimal.ONE)), "inventario1"));
    }

    @Test
    void rechazaCantidadCeroYFechaPasada() {
        Proveedor pr = crearProveedor();
        Producto a = crearProducto(1, "1.00");
        assertThrows(NegocioException.class, () -> compras.crear(pr.getId(), null,
                List.of(new CompraService.Linea(a.getId(), 0, BigDecimal.ONE)), "inventario1"));
        assertThrows(NegocioException.class, () -> compras.crear(pr.getId(), LocalDate.now().minusDays(1),
                List.of(new CompraService.Linea(a.getId(), 1, BigDecimal.ONE)), "inventario1"));
    }

    @Test
    void tc19_filtraPorEstadoYProveedor() {
        Producto a = crearProducto(5, "10.00");
        Producto b = crearProducto(7, "10.00");
        OrdenCompra pendiente = ordenCon(a, 1, b, 1);
        OrdenCompra recibida = ordenCon(a, 1, b, 1);
        compras.recepcionar(recibida.getId(), "inventario1");

        var soloPendientes = compras.buscar(pendiente.getProveedor().getId(), null, null, EstadoOrden.PENDIENTE);
        assertEquals(1, soloPendientes.size());
        assertEquals(pendiente.getId(), soloPendientes.get(0).getId());
        assertTrue(compras.buscar(recibida.getProveedor().getId(), null, null, EstadoOrden.PENDIENTE).isEmpty());
    }
}
