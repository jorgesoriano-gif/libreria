package com.utp.libreria.service;

import com.utp.libreria.model.Producto;
import com.utp.libreria.model.TipoMovimiento;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/** TC15 y RF04: movimientos manuales y alertas por stock mínimo. */
@SpringBootTest
class InventarioServiceTest extends PruebaBase {

    @Autowired InventarioService inventario;

    @Test
    void entradaYSalidaActualizanElStockYDejanHistorial() {
        Producto p = crearProducto(10, "5.00");
        inventario.registrar(p.getId(), TipoMovimiento.ENTRADA, 5, "Conteo físico", "inventario1");
        assertEquals(15, stockDe(p.getId()));
        inventario.registrar(p.getId(), TipoMovimiento.SALIDA, 3, "Merma", "inventario1");
        assertEquals(12, stockDe(p.getId()));
        assertEquals(2, inventario.historial(p.getId()).size());
    }

    @Test
    void noPermiteStockNegativoNiDatosInvalidos() {
        Producto p = crearProducto(2, "5.00");
        assertThrows(NegocioException.class,
                () -> inventario.registrar(p.getId(), TipoMovimiento.SALIDA, 3, "Merma", "inventario1"));
        assertThrows(NegocioException.class,
                () -> inventario.registrar(p.getId(), TipoMovimiento.ENTRADA, 0, "Conteo", "inventario1"));
        assertThrows(NegocioException.class,
                () -> inventario.registrar(p.getId(), TipoMovimiento.ENTRADA, 1, "  ", "inventario1"));
        assertEquals(2, stockDe(p.getId()));
    }

    @Test
    void tc15_alertaCuandoElStockLlegaAlMinimo() {
        Producto p = crearProducto(5, "5.00");   // mínimo = 2
        assertFalse(inventario.alertas().stream().anyMatch(a -> a.getId().equals(p.getId())));
        inventario.registrar(p.getId(), TipoMovimiento.SALIDA, 3, "Ajuste", "inventario1");
        assertTrue(inventario.alertas().stream().anyMatch(a -> a.getId().equals(p.getId())));
    }
}
