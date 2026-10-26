package com.utp.libreria.web;

import com.utp.libreria.model.Producto;
import com.utp.libreria.repository.CategoriaRepository;
import com.utp.libreria.repository.ProductoRepository;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/productos")
public class ProductoController {

    private final ProductoRepository productos;
    private final CategoriaRepository categorias;

    public ProductoController(ProductoRepository productos, CategoriaRepository categorias) {
        this.productos = productos;
        this.categorias = categorias;
    }

    /** M03 - Lista y búsqueda (todos los roles; solo lectura salvo Administrador). */
    @GetMapping
    public String lista(@RequestParam(defaultValue = "") String q,
                        @RequestParam(defaultValue = "0") long categoria, Model model) {
        model.addAttribute("productos", productos.buscar(q.trim(), categoria, false));
        model.addAttribute("categorias", categorias.findAllByOrderByNombre());
        model.addAttribute("q", q);
        model.addAttribute("categoria", categoria);
        return "productos";
    }

    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        model.addAttribute("form", new ProductoForm());
        model.addAttribute("categorias", categorias.findAllByOrderByNombre());
        return "producto-form";
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Model model, RedirectAttributes ra) {
        return productos.findById(id).map(p -> {
            model.addAttribute("form", ProductoForm.desde(p));
            model.addAttribute("categorias", categorias.findAllByOrderByNombre());
            return "producto-form";
        }).orElseGet(() -> {
            ra.addFlashAttribute("error", "El producto no existe.");
            return "redirect:/productos";
        });
    }

    @PostMapping
    public String guardar(@Valid @ModelAttribute("form") ProductoForm form, BindingResult result,
                          Model model, RedirectAttributes ra) {
        if (form.getCodigo() != null && !form.getCodigo().isBlank()) {
            productos.findByCodigo(form.getCodigo().trim()).ifPresent(otro -> {
                if (!otro.getId().equals(form.getId())) {
                    result.rejectValue("codigo", "duplicado", "El código ya existe (" + otro.getTitulo() + ").");
                }
            });
        }
        if (result.hasErrors()) {
            model.addAttribute("categorias", categorias.findAllByOrderByNombre());
            return "producto-form";
        }
        Producto p = form.getId() == null ? new Producto()
                : productos.findById(form.getId()).orElseThrow();
        p.setCodigo(form.getCodigo().trim());
        p.setTitulo(form.getTitulo().trim());
        p.setAutor(form.getAutor() == null || form.getAutor().isBlank() ? null : form.getAutor().trim());
        p.setCategoria(categorias.getReferenceById(form.getCategoriaId()));
        p.setPrecio(form.getPrecio());
        p.setStockMinimo(form.getStockMinimo());
        p.setActivo(form.isActivo());
        p.setStock(form.getStock());   // sin módulo de inventario: el stock se ajusta aquí
        productos.save(p);
        ra.addFlashAttribute("ok", "Producto guardado: " + p.getCodigo() + ".");
        return "redirect:/productos";
    }

    /** Desactivar / activar sin borrar: conserva el historial (CU-05, A2). */
    @PostMapping("/{id}/estado")
    public String cambiarEstado(@PathVariable Long id, RedirectAttributes ra) {
        productos.findById(id).ifPresent(p -> {
            p.setActivo(!p.isActivo());
            productos.save(p);
            ra.addFlashAttribute("ok", "Producto " + p.getCodigo() + (p.isActivo() ? " activado." : " desactivado."));
        });
        return "redirect:/productos";
    }
}
