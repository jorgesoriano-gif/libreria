package com.utp.libreria.web;

import com.utp.libreria.model.Cliente;
import com.utp.libreria.repository.ClienteRepository;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** M06 - Clientes (RF06, RF13). Vendedor y Administrador. */
@Controller
@RequestMapping("/clientes")
public class ClienteController {

    private final ClienteRepository clientes;

    public ClienteController(ClienteRepository clientes) {
        this.clientes = clientes;
    }

    @GetMapping
    public String lista(@RequestParam(defaultValue = "") String q, Model model) {
        model.addAttribute("clientes", clientes.buscar(q.trim()));
        model.addAttribute("q", q);
        return "clientes";
    }

    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        model.addAttribute("form", new ClienteForm());
        return "cliente-form";
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Model model, RedirectAttributes ra) {
        return clientes.findById(id).map(c -> {
            model.addAttribute("form", ClienteForm.desde(c));
            return "cliente-form";
        }).orElseGet(() -> {
            ra.addFlashAttribute("error", "El cliente no existe.");
            return "redirect:/clientes";
        });
    }

    @PostMapping
    public String guardar(@Valid @ModelAttribute("form") ClienteForm form, BindingResult result, RedirectAttributes ra) {
        String doc = form.getDocumento() == null || form.getDocumento().isBlank() ? null : form.getDocumento().trim();
        if (doc != null) {
            clientes.findByDocumento(doc).ifPresent(otro -> {
                if (!otro.getId().equals(form.getId())) {
                    result.rejectValue("documento", "duplicado", "El documento ya está registrado (" + otro.getNombre() + ").");
                }
            });
        }
        if (result.hasErrors()) {
            return "cliente-form";
        }
        Cliente c = form.getId() == null ? new Cliente() : clientes.findById(form.getId()).orElseThrow();
        c.setDocumento(doc);
        c.setNombre(form.getNombre().trim());
        c.setTelefono(form.getTelefono() == null || form.getTelefono().isBlank() ? null : form.getTelefono().trim());
        clientes.save(c);
        ra.addFlashAttribute("ok", "Cliente guardado: " + c.getNombre() + ".");
        return "redirect:/clientes";
    }
}
