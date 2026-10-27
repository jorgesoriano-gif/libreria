package com.utp.libreria.service;

import com.utp.libreria.model.Categoria;
import com.utp.libreria.repository.CategoriaRepository;
import com.utp.libreria.repository.ProductoRepository;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** M04 - Categorías (RF16, CU-04). */
@Service
public class CategoriaService {

    private static final Logger log = LoggerFactory.getLogger(CategoriaService.class);

    private final CategoriaRepository categorias;
    private final ProductoRepository productos;

    public CategoriaService(CategoriaRepository categorias, ProductoRepository productos) {
        this.categorias = categorias;
        this.productos = productos;
    }

    public List<Categoria> listar() {
        return categorias.findAllByOrderByNombre();
    }

    public long cantidadProductos(Long categoriaId) {
        return productos.countByCategoriaId(categoriaId);
    }

    @Transactional
    public Categoria guardar(Long id, String nombre, String descripcion) {
        String n = StringUtils.normalizeSpace(StringUtils.trimToEmpty(nombre));
        if (n.isEmpty()) {
            throw new NegocioException("El nombre de la categoría es obligatorio.");
        }
        if (n.length() > 50) {
            throw new NegocioException("El nombre admite hasta 50 caracteres.");
        }
        String d = StringUtils.trimToNull(descripcion);
        if (d != null && d.length() > 150) {
            throw new NegocioException("La descripción admite hasta 150 caracteres.");
        }
        categorias.findByNombreIgnoreCase(n).ifPresent(otra -> {
            if (!otra.getId().equals(id)) {
                throw new NegocioException("Ya existe una categoría con el nombre «" + otra.getNombre() + "».");
            }
        });
        Categoria c;
        if (id == null) {
            c = new Categoria();
            c.setCodigo(siguienteCodigo());
        } else {
            c = categorias.findById(id).orElseThrow(() -> new NegocioException("La categoría no existe."));
        }
        c.setNombre(n);
        c.setDescripcion(d);
        Categoria guardada = categorias.save(c);
        log.info("Categoría {} guardada", guardada.getCodigo());
        return guardada;
    }

    /** TC08: no se elimina una categoría con productos asociados. */
    @Transactional
    public void eliminar(Long id) {
        Categoria c = categorias.findById(id).orElseThrow(() -> new NegocioException("La categoría no existe."));
        long n = productos.countByCategoriaId(id);
        if (n > 0) {
            throw new NegocioException("No se puede eliminar «" + c.getNombre() + "»: tiene " + n
                    + (n == 1 ? " producto asociado." : " productos asociados."));
        }
        categorias.delete(c);
        log.info("Categoría {} eliminada", c.getCodigo());
    }

    /** Código correlativo CAT-NNN. */
    private String siguienteCodigo() {
        int max = 0;
        for (Categoria c : categorias.findAll()) {
            String cod = c.getCodigo();
            if (cod != null && cod.startsWith("CAT-") && StringUtils.isNumeric(cod.substring(4))) {
                max = Math.max(max, Integer.parseInt(cod.substring(4)));
            }
        }
        return String.format("CAT-%03d", max + 1);
    }
}
