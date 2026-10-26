package com.utp.libreria.web;

import com.utp.libreria.model.Producto;
import com.utp.libreria.model.Venta;
import com.utp.libreria.repository.ProductoRepository;
import com.utp.libreria.repository.VentaRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Controller
public class HomeController {

    private final VentaRepository ventas;
    private final ProductoRepository productos;

    public HomeController(VentaRepository ventas, ProductoRepository productos) {
        this.ventas = ventas;
        this.productos = productos;
    }

    /** M02 - Panel principal. */
    @GetMapping("/")
    public String panel(Model model) {
        LocalDate hoy = LocalDate.now();
        List<Venta> deHoy = ventas.findByFechaGreaterThanEqualAndFechaLessThanOrderByFecha(
                hoy.atStartOfDay(), hoy.plusDays(1).atStartOfDay());
        BigDecimal total = deHoy.stream().map(Venta::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        List<Producto> bajo = productos.bajoMinimo();

        model.addAttribute("totalHoy", total);
        model.addAttribute("cantidadVentas", deHoy.size());
        model.addAttribute("productosActivos", productos.countByActivoTrue());
        model.addAttribute("bajoMinimo", bajo);
        return "panel";
    }
}
