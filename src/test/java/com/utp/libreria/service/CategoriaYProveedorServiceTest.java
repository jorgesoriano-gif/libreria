package com.utp.libreria.service;

import com.utp.libreria.model.Categoria;
import com.utp.libreria.model.Producto;
import com.utp.libreria.model.Proveedor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/** TC08 (categorías) y TC10 (proveedores). */
@SpringBootTest
class CategoriaYProveedorServiceTest extends PruebaBase {

    @Autowired CategoriaService categoriasSrv;
    @Autowired ProveedorService proveedoresSrv;

    @Test
    void tc08_noEliminaUnaCategoriaConProductos() {
        Producto p = crearProducto(1, "1.00");
        Long catId = p.getCategoria().getId();
        NegocioException e = assertThrows(NegocioException.class, () -> categoriasSrv.eliminar(catId));
        assertTrue(e.getMessage().contains("producto"));
        assertTrue(categorias.existsById(catId));
    }

    @Test
    void creaYEliminaUnaCategoriaSinProductos() {
        String nombre = "Cat " + System.nanoTime();
        Categoria c = categoriasSrv.guardar(null, nombre, "descripción");
        assertTrue(c.getCodigo().matches("CAT-\\d{3}"));
        assertThrows(NegocioException.class, () -> categoriasSrv.guardar(null, nombre.toUpperCase(), null));
        categoriasSrv.eliminar(c.getId());
        assertFalse(categorias.existsById(c.getId()));
    }

    @Test
    void tc10_rechazaRucDuplicadoYConservaElOriginal() {
        Proveedor original = crearProveedor();
        NegocioException e = assertThrows(NegocioException.class, () ->
                proveedoresSrv.guardar(null, original.getRuc(), "Otro S.A.C.", null, null, null));
        assertTrue(e.getMessage().contains("RUC"));
        assertEquals("Proveedor de prueba", proveedores.findById(original.getId()).orElseThrow().getRazonSocial());
    }

    @Test
    void validaRucYCorreo() {
        assertThrows(NegocioException.class, () -> proveedoresSrv.guardar(null, "123", "X", null, null, null));
        assertThrows(NegocioException.class, () -> proveedoresSrv.guardar(null, "20555555551", "X", null, "no-es-correo", null));
    }
}
