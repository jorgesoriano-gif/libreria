package com.utp.libreria.web;

import com.utp.libreria.service.NegocioException;
import com.utp.libreria.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** M11 - Usuarios y roles (RF15). Solo Administrador (TC07). */
@Controller
@RequestMapping("/usuarios")
public class UsuarioController {

    private final UsuarioService servicio;

    public UsuarioController(UsuarioService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public String lista(Model model) {
        model.addAttribute("usuarios", servicio.listar());
        return "usuarios";
    }

    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        model.addAttribute("form", new UsuarioForm());
        model.addAttribute("roles", servicio.roles());
        return "usuario-form";
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Model model, RedirectAttributes ra) {
        return servicio.listar().stream().filter(u -> u.getId().equals(id)).findFirst().map(u -> {
            model.addAttribute("form", UsuarioForm.desde(u));
            model.addAttribute("roles", servicio.roles());
            return "usuario-form";
        }).orElseGet(() -> {
            ra.addFlashAttribute("error", "El usuario no existe.");
            return "redirect:/usuarios";
        });
    }

    @PostMapping
    public String guardar(@Valid @ModelAttribute("form") UsuarioForm form, BindingResult result,
                          Model model, RedirectAttributes ra) {
        if (!result.hasErrors()) {
            try {
                var u = servicio.guardar(form.getId(), form.getUsuario(), form.getNombre(),
                        form.getRolId(), form.getClave(), form.isActivo());
                ra.addFlashAttribute("ok", "Usuario guardado: " + u.getUsuario() + ".");
                return "redirect:/usuarios";
            } catch (NegocioException e) {
                result.reject("negocio", e.getMessage());
            }
        }
        model.addAttribute("roles", servicio.roles());
        return "usuario-form";
    }

    @PostMapping("/{id}/estado")
    public String cambiarEstado(@PathVariable Long id, Authentication auth, RedirectAttributes ra) {
        try {
            var u = servicio.cambiarEstado(id, auth.getName());
            ra.addFlashAttribute("ok", "Usuario " + u.getUsuario() + (u.isActivo() ? " activado." : " desactivado."));
        } catch (NegocioException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/usuarios";
    }
}
