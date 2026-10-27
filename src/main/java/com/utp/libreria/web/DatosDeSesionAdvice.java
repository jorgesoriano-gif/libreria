package com.utp.libreria.web;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/** Datos del usuario autenticado disponibles en todas las vistas (menú y botones según el rol). */
@ControllerAdvice
public class DatosDeSesionAdvice {

    private static boolean tiene(Authentication auth, String rol) {
        if (auth == null) return false;
        for (GrantedAuthority a : auth.getAuthorities()) {
            if (a.getAuthority().equals("ROLE_" + rol)) return true;
        }
        return false;
    }

    @ModelAttribute("usuarioActual")
    public String usuarioActual(Authentication auth) {
        return auth == null ? "" : auth.getName();
    }

    @ModelAttribute("esAdmin")
    public boolean esAdmin(Authentication auth) {
        return tiene(auth, "ADMIN");
    }

    @ModelAttribute("puedeVender")
    public boolean puedeVender(Authentication auth) {
        return tiene(auth, "ADMIN") || tiene(auth, "VENDEDOR");
    }

    @ModelAttribute("puedeInventario")
    public boolean puedeInventario(Authentication auth) {
        return tiene(auth, "ADMIN") || tiene(auth, "INVENTARIO");
    }

    @ModelAttribute("rolEtiqueta")
    public String rolEtiqueta(Authentication auth) {
        if (tiene(auth, "ADMIN")) return "Administrador";
        if (tiene(auth, "VENDEDOR")) return "Vendedor";
        if (tiene(auth, "INVENTARIO")) return "Encargado de inventario";
        return "";
    }
}
