package com.utp.libreria.web;

import com.utp.libreria.model.EstadoOrden;
import com.utp.libreria.model.Producto;
import com.utp.libreria.model.Catalogos;
import com.utp.libreria.repository.ProductoRepository;
import com.utp.libreria.service.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** M13 Órdenes de compra y M14 Recepción de mercadería (RF19 a RF21). Administrador e Inventario. */
@Controller
@RequestMapping("/compras")
public class CompraController {

    private final CompraService servicio;
    private final ProveedorService proveedores;
    private final ProductoRepository productos;
    private final CarritoCompra carrito;

    public CompraController(CompraService servicio, ProveedorService proveedores,
                            ProductoRepository productos, CarritoCompra carrito) {
        this.servicio = servicio;
        this.proveedores = proveedores;
        this.productos = productos;
        this.carrito = carrito;
    }

    // ---------- Lista y filtros (RF21) ----------

    @GetMapping
    public String lista(@RequestParam(defaultValue = "0") long proveedor,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                        @RequestParam(required = false) EstadoOrden estado, Model model) {
        model.addAttribute("ordenes", servicio.buscar(proveedor, desde, hasta, estado));
        model.addAttribute("proveedores", proveedores.buscar(""));
        model.addAttribute("estados", Catalogos.ETIQUETA_ESTADO_ORDEN);
        model.addAttribute("proveedor", proveedor);
        model.addAttribute("desde", desde);
        model.addAttribute("hasta", hasta);
        model.addAttribute("estado", estado);
        return "compras";
    }

    // ---------- Nueva orden (RF19) ----------

    @GetMapping("/nueva")
    public String nueva(Model model) {
        model.addAttribute("proveedores", proveedores.activos());
        model.addAttribute("catalogo", productos.findByActivoTrueOrderByTitulo());
        model.addAttribute("items", carrito.getItems());
        model.addAttribute("total", carrito.getTotal());
        model.addAttribute("proveedorSel", carrito.getProveedorId());
        model.addAttribute("entregaSel", carrito.getEntrega());
        model.addAttribute("hoy", LocalDate.now());
        return "compra-nueva";
    }

    @PostMapping("/agregar")
    public String agregar(@RequestParam(required = false) Long proveedorId,
                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate entrega,
                          @RequestParam(required = false) Long productoId,
                          @RequestParam(defaultValue = "1") int cantidad,
                          @RequestParam(required = false) BigDecimal costo, RedirectAttributes ra) {
        carrito.setProveedorId(proveedorId);
        carrito.setEntrega(entrega);
        try {
            Producto p = productoId == null ? null : productos.findById(productoId).orElse(null);
            if (p == null) {
                throw new NegocioException("Seleccione un producto del catálogo.");
            }
            carrito.agregar(p, cantidad, costo);
        } catch (NegocioException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/compras/nueva";
    }

    @PostMapping("/quitar/{productoId}")
    public String quitar(@PathVariable Long productoId) {
        carrito.quitar(productoId);
        return "redirect:/compras/nueva";
    }

    @PostMapping("/vaciar")
    public String vaciar() {
        carrito.vaciar();
        return "redirect:/compras/nueva";
    }

    @PostMapping("/confirmar")
    public String confirmar(Authentication auth, RedirectAttributes ra) {
        try {
            List<CompraService.Linea> lineas = carrito.getItems().stream()
                    .map(i -> new CompraService.Linea(i.getProductoId(), i.getCantidad(), i.getCosto()))
                    .toList();
            var orden = servicio.crear(carrito.getProveedorId(), carrito.getEntrega(), lineas, auth.getName());
            carrito.vaciar();
            ra.addFlashAttribute("ok", "Orden " + orden.getNumero() + " creada en estado PENDIENTE.");
            return "redirect:/compras/" + orden.getId();
        } catch (NegocioException e) {
            ra.addFlashAttribute("error", e.getMessage());
        } catch (RuntimeException e) {
            ra.addFlashAttribute("error", "No se pudo registrar la orden; se revirtió la operación.");
        }
        return "redirect:/compras/nueva";
    }

    // ---------- Detalle y recepción (RF20, RF21) ----------

    @GetMapping("/{id}")
    public String detalle(@PathVariable Long id, Model model, RedirectAttributes ra) {
        try {
            var orden = servicio.obtener(id);
            model.addAttribute("orden", orden);
            model.addAttribute("detalles", servicio.detalleDe(id));
            return "compra-detalle";
        } catch (NegocioException e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/compras";
        }
    }

    @PostMapping("/{id}/recepcionar")
    public String recepcionar(@PathVariable Long id, Authentication auth, RedirectAttributes ra) {
        try {
            var orden = servicio.recepcionar(id, auth.getName());
            ra.addFlashAttribute("ok", "Orden " + orden.getNumero() + " recepcionada: el stock fue actualizado.");
        } catch (NegocioException e) {
            ra.addFlashAttribute("error", e.getMessage());
        } catch (RuntimeException e) {
            ra.addFlashAttribute("error", "No se pudo recepcionar; se revirtió la operación.");
        }
        return "redirect:/compras/" + id;
    }
}

