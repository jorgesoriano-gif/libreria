package com.utp.libreria.web;

import com.utp.libreria.model.Categoria;
import com.utp.libreria.service.CategoriaService;
import com.utp.libreria.service.NegocioException;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.LinkedHashMap;
import java.util.Map;

/** M04 - Categorías (RF16). Solo Administrador. */
@Controller
@RequestMapping("/categorias")
public class CategoriaController {

    private final CategoriaService servicio;

    public CategoriaController(CategoriaService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public String lista(Model model) {
        var lista = servicio.listar();
        Map<Long, Long> conteo = new LinkedHashMap<>();
        for (Categoria c : lista) {
            conteo.put(c.getId(), servicio.cantidadProductos(c.getId()));
        }
        model.addAttribute("categorias", lista);
        model.addAttribute("conteo", conteo);
        return "categorias";
    }

    @GetMapping("/nueva")
    public String nueva(Model model) {
        model.addAttribute("form", new CategoriaForm());
        return "categoria-form";
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Model model, RedirectAttributes ra) {
        return servicio.listar().stream().filter(c -> c.getId().equals(id)).findFirst().map(c -> {
            model.addAttribute("form", CategoriaForm.desde(c));
            return "categoria-form";
        }).orElseGet(() -> {
            ra.addFlashAttribute("error", "La categoría no existe.");
            return "redirect:/categorias";
        });
    }

    @PostMapping
    public String guardar(@Valid @ModelAttribute("form") CategoriaForm form, BindingResult result,
                          RedirectAttributes ra) {
        if (result.hasErrors()) {
            return "categoria-form";
        }
        try {
            Categoria c = servicio.guardar(form.getId(), form.getNombre(), form.getDescripcion());
            ra.addFlashAttribute("ok", "Categoría guardada: " + c.getNombre() + ".");
            return "redirect:/categorias";
        } catch (NegocioException e) {
            result.reject("negocio", e.getMessage());
            return "categoria-form";
        }
    }

    /** TC08: se rechaza con mensaje si tiene productos asociados. */
    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable Long id, RedirectAttributes ra) {
        try {
            servicio.eliminar(id);
            ra.addFlashAttribute("ok", "Categoría eliminada.");
        } catch (NegocioException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/categorias";
    }
}
