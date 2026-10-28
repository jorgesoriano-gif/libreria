package com.utp.libreria.web;

import com.utp.libreria.model.Categoria;
import com.utp.libreria.model.Cliente;
import com.utp.libreria.model.Producto;
import com.utp.libreria.model.Proveedor;
import com.utp.libreria.repository.CategoriaRepository;
import com.utp.libreria.repository.ClienteRepository;
import com.utp.libreria.repository.ProductoRepository;
import com.utp.libreria.repository.ProveedorRepository;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api")
public class BusquedaGlobalController {

    private final ProductoRepository productos;
    private final ClienteRepository clientes;
    private final ProveedorRepository proveedores;
    private final CategoriaRepository categorias;

    public BusquedaGlobalController(ProductoRepository productos,
                                    ClienteRepository clientes,
                                    ProveedorRepository proveedores,
                                    CategoriaRepository categorias) {
        this.productos = productos;
        this.clientes = clientes;
        this.proveedores = proveedores;
        this.categorias = categorias;
    }

    public record ItemResultado(String categoria, String titulo, String detalle, String url, String codigo) {}

    @GetMapping("/busqueda-global")
    public ResponseEntity<List<ItemResultado>> buscar(@RequestParam(defaultValue = "") String q) {
        String cleanQ = StringUtils.normalizeSpace(q);
        if (cleanQ.length() < 1) {
            return ResponseEntity.ok(List.of());
        }

        List<ItemResultado> resultados = new ArrayList<>();

        // 1. Productos (hasta 6)
        List<Producto> prods = productos.buscar(cleanQ, 0, false);
        for (int i = 0; i < Math.min(prods.size(), 6); i++) {
            Producto p = prods.get(i);
            String autor = p.getAutor() != null ? " · " + p.getAutor() : "";
            resultados.add(new ItemResultado(
                    "Productos",
                    p.getCodigo() + " - " + p.getTitulo(),
                    "Stock: " + p.getStock() + " · S/ " + p.getPrecio() + autor,
                    "/productos?q=" + p.getCodigo(),
                    p.getCodigo()
            ));
        }

        // 2. Clientes (hasta 4)
        List<Cliente> clis = clientes.buscar(cleanQ);
        for (int i = 0; i < Math.min(clis.size(), 4); i++) {
            Cliente c = clis.get(i);
            String doc = c.getDocumento() != null ? "Doc: " + c.getDocumento() : "Sin documento";
            String tel = c.getTelefono() != null ? " · Tel: " + c.getTelefono() : "";
            resultados.add(new ItemResultado(
                    "Clientes",
                    c.getNombre(),
                    doc + tel,
                    "/clientes?q=" + (c.getDocumento() != null ? c.getDocumento() : c.getNombre()),
                    c.getDocumento()
            ));
        }

        // 3. Proveedores (hasta 4)
        List<Proveedor> provs = proveedores.buscar(cleanQ);
        for (int i = 0; i < Math.min(provs.size(), 4); i++) {
            Proveedor pr = provs.get(i);
            resultados.add(new ItemResultado(
                    "Proveedores",
                    pr.getRazonSocial(),
                    "RUC: " + pr.getRuc() + (pr.getTelefono() != null ? " · Tel: " + pr.getTelefono() : ""),
                    "/proveedores?q=" + pr.getRuc(),
                    pr.getRuc()
            ));
        }

        // 4. Categorías (hasta 3)
        String lowerQ = cleanQ.toLowerCase();
        List<Categoria> cats = categorias.findAllByOrderByNombre().stream()
                .filter(c -> c.getNombre().toLowerCase().contains(lowerQ) || c.getCodigo().toLowerCase().contains(lowerQ))
                .limit(3)
                .toList();
        for (Categoria cat : cats) {
            resultados.add(new ItemResultado(
                    "Categorías",
                    cat.getCodigo() + " - " + cat.getNombre(),
                    cat.getDescripcion() != null ? cat.getDescripcion() : "Categoría del catálogo",
                    "/categorias",
                    cat.getCodigo()
            ));
        }

        return ResponseEntity.ok(resultados);
    }
}

