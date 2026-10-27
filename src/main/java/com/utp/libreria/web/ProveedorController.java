package com.utp.libreria.web;

import com.utp.libreria.service.NegocioException;
import com.utp.libreria.service.ProveedorService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** M12 - Proveedores (RF18). Administrador e Inventario. */
@Controller
@RequestMapping("/proveedores")
public class ProveedorController {

    private final ProveedorService servicio;

    public ProveedorController(ProveedorService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public String lista(@RequestParam(defaultValue = "") String q, Model model) {
        model.addAttribute("proveedores", servicio.buscar(q));
        model.addAttribute("q", q);
        return "proveedores";
    }

    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        model.addAttribute("form", new ProveedorForm());
        return "proveedor-form";
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Model model, RedirectAttributes ra) {
        return servicio.buscar("").stream().filter(p -> p.getId().equals(id)).findFirst().map(p -> {
            model.addAttribute("form", ProveedorForm.desde(p));
            return "proveedor-form";
        }).orElseGet(() -> {
            ra.addFlashAttribute("error", "El proveedor no existe.");
            return "redirect:/proveedores";
        });
    }

    @PostMapping
    public String guardar(@Valid @ModelAttribute("form") ProveedorForm form, BindingResult result,
                          RedirectAttributes ra) {
        if (!result.hasErrors()) {
            try {
                var p = servicio.guardar(form.getId(), form.getRuc(), form.getRazonSocial(),
                        form.getTelefono(), form.getCorreo(), form.getDireccion());
                ra.addFlashAttribute("ok", "Proveedor guardado: " + p.getRazonSocial() + ".");
                return "redirect:/proveedores";
            } catch (NegocioException e) {
                result.reject("negocio", e.getMessage());
            }
        }
        return "proveedor-form";
    }

    @PostMapping("/{id}/estado")
    public String cambiarEstado(@PathVariable Long id, RedirectAttributes ra) {
        try {
            var p = servicio.cambiarEstado(id);
            ra.addFlashAttribute("ok", "Proveedor " + p.getRazonSocial() + (p.isActivo() ? " activado." : " desactivado."));
        } catch (NegocioException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/proveedores";
    }
}
