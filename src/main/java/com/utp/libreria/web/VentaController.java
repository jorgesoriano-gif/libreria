package com.utp.libreria.web;

import com.utp.libreria.model.Producto;
import com.utp.libreria.model.Venta;
import com.utp.libreria.repository.*;
import com.utp.libreria.service.CarritoVenta;
import com.utp.libreria.service.NegocioException;
import com.utp.libreria.service.VentaService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;

/** M07 Nueva venta, M08 Comprobante y M09 Historial. Vendedor y Administrador. */
@Controller
@RequestMapping("/ventas")
public class VentaController {

    private final VentaService servicio;
    private final CarritoVenta carrito;
    private final ProductoRepository productos;
    private final ClienteRepository clientes;
    private final VentaRepository ventas;

    public VentaController(VentaService servicio, CarritoVenta carrito, ProductoRepository productos,
                           ClienteRepository clientes, VentaRepository ventas) {
        this.servicio = servicio;
        this.carrito = carrito;
        this.productos = productos;
        this.clientes = clientes;
        this.ventas = ventas;
    }

    // ---------- M07 Nueva venta ----------

    @GetMapping("/nueva")
    public String nueva(@RequestParam(defaultValue = "") String q, Model model) {
        model.addAttribute("q", q);
        model.addAttribute("resultados", productos.buscar(q.trim(), 0, true));
        model.addAttribute("catalogo", productos.findByActivoTrueOrderByTitulo());
        model.addAttribute("items", carrito.getItems());
        model.addAttribute("total", carrito.getTotal());
        model.addAttribute("totalUnidades", carrito.getTotalUnidades());
        model.addAttribute("clientes", clientes.buscar(""));
        model.addAttribute("fechaHoy", LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        return "venta-nueva";
    }

    @PostMapping("/agregar")
    public String agregar(@RequestParam(required = false) Long productoId,
                          @RequestParam(required = false) String codigoOProducto,
                          @RequestParam(defaultValue = "1") int cantidad,
                          @RequestParam(defaultValue = "") String q, RedirectAttributes ra) {
        try {
            Producto producto = null;
            if (productoId != null) {
                producto = productos.findById(productoId).orElse(null);
            }
            if (producto == null && codigoOProducto != null && !codigoOProducto.isBlank()) {
                String input = codigoOProducto.trim();
                // El datalist envía "CODIGO - Título", separar por " - "
                String codigo = input.contains(" - ") ? input.substring(0, input.indexOf(" - ")).trim() : input;
                producto = productos.findByCodigo(codigo)
                        .or(() -> productos.findByCodigo(input))
                        .orElse(null);
                if (producto == null) {
                    var busqueda = productos.buscar(input, 0, true);
                    if (!busqueda.isEmpty()) {
                        producto = busqueda.get(0);
                    }
                }
            }
            if (producto == null) {
                throw new NegocioException("Seleccione o ingrese un producto válido del catálogo.");
            }
            carrito.agregar(producto, cantidad);
        } catch (NegocioException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        ra.addAttribute("q", q);
        return "redirect:/ventas/nueva";
    }

    @PostMapping("/quitar/{productoId}")
    public String quitar(@PathVariable Long productoId) {
        carrito.quitar(productoId);
        return "redirect:/ventas/nueva";
    }

    @PostMapping("/vaciar")
    public String vaciar() {
        carrito.vaciar();
        return "redirect:/ventas/nueva";
    }

    @PostMapping("/confirmar")
    public String confirmar(@RequestParam(required = false) Long clienteId, Authentication auth, RedirectAttributes ra) {
        try {
            Venta venta = servicio.registrar(clienteId, auth.getName(), carrito.getItems());
            carrito.vaciar();
            return "redirect:/ventas/" + venta.getId() + "/comprobante";
        } catch (NegocioException e) {
            ra.addFlashAttribute("error", e.getMessage());
        } catch (RuntimeException e) {
            // Falla de la base de datos: la transacción ya se revirtió (CU-07, A2)
            ra.addFlashAttribute("error", "No se pudo registrar la venta; se revirtió la operación. Intente de nuevo.");
        }
        return "redirect:/ventas/nueva";
    }

    // ---------- M08 Comprobante ----------

    @GetMapping("/{id}/comprobante")
    public String comprobante(@PathVariable Long id, Model model, RedirectAttributes ra) {
        return ventas.findById(id).map(v -> {
            model.addAttribute("venta", v);
            return "comprobante";
        }).orElseGet(() -> {
            ra.addFlashAttribute("error", "La venta no existe.");
            return "redirect:/ventas";
        });
    }

    // ---------- M09 Historial ----------

    @GetMapping
    public String historial(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                            @RequestParam(defaultValue = "") String nro,
                            @RequestParam(defaultValue = "") String cliente,
                            @RequestParam(required = false) Long ver, Model model) {
        LocalDate d = desde == null ? LocalDate.of(2000, 1, 1) : desde;
        LocalDate h = hasta == null ? LocalDate.of(2999, 1, 1) : hasta;
        List<Venta> lista = ventas.buscar(d.atStartOfDay(), h.plusDays(1).atStartOfDay(), nro.trim(), cliente.trim());
        model.addAttribute("ventas", lista);
        model.addAttribute("desde", desde);
        model.addAttribute("hasta", hasta);
        model.addAttribute("nro", nro);
        model.addAttribute("cliente", cliente);
        model.addAttribute("seleccion", ver == null ? null : ventas.findById(ver).orElse(null));
        return "historial";
    }
}
