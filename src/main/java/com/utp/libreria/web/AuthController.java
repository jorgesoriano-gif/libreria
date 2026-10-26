package com.utp.libreria.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AuthController {

    /** M01 - Inicio de sesión. */
    @GetMapping("/login")
    public String login() {
        return "login";
    }
}
