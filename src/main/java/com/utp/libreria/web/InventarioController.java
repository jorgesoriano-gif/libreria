package com.utp.libreria.web;

import com.utp.libreria.model.TipoMovimiento;
import com.utp.libreria.repository.CategoriaRepository;
import com.utp.libreria.repository.ProductoRepository;
import com.utp.libreria.service.InventarioService;
import com.utp.libreria.service.NegocioException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** M05 - Inventario: existencias, alertas y movimientos (RF04, RF05). Administrador e Inventario. */
@Controller
@RequestMapping("/inventario")
public class InventarioController {

    private final InventarioService servicio;
    private final ProductoRepository productos;
    private final CategoriaRepository categorias;

    public InventarioController(InventarioService servicio, ProductoRepository productos,
                                CategoriaRepository categorias) {
        this.servicio = servicio;
        this.productos = productos;
        this.categorias = categorias;
    }

    @GetMapping
    public String inventario(@RequestParam(defaultValue = "") String q,
                             @RequestParam(defaultValue = "0") long categoria,
                             @RequestParam(defaultValue = "0") long historial, Model model) {
        model.addAttribute("productos", productos.buscar(StringUtils.normalizeSpace(q), categoria, false));
        model.addAttribute("catalogo", productos.findByActivoTrueOrderByTitulo());
        model.addAttribute("categorias", categorias.findAllByOrderByNombre());
        model.addAttribute("alertas", servicio.alertas());
        model.addAttribute("movimientos", servicio.historial(historial));
        model.addAttribute("q", q);
        model.addAttribute("categoria", categoria);
        model.addAttribute("historial", historial);
        return "inventario";
    }

    @PostMapping("/movimiento")
    public String movimiento(@RequestParam(required = false) Long productoId,
                             @RequestParam TipoMovimiento tipo,
                             @RequestParam(defaultValue = "0") int cantidad,
                             @RequestParam(defaultValue = "") String motivo,
                             Authentication auth, RedirectAttributes ra) {
        try {
            var mov = servicio.registrar(productoId, tipo, cantidad, motivo, auth.getName());
            ra.addFlashAttribute("ok", "Movimiento registrado: " + mov.getProducto().getCodigo()
                    + " · stock actual " + mov.getProducto().getStock() + ".");
        } catch (NegocioException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/inventario";
    }
}
