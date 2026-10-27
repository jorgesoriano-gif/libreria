package com.utp.libreria.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;

/** Registra inicios de sesión (Logback). Nunca se escribe la contraseña. */
@Component
public class AuditoriaAccesoListener {

    private static final Logger log = LoggerFactory.getLogger(AuditoriaAccesoListener.class);

    @EventListener
    public void alIniciarSesion(AuthenticationSuccessEvent evento) {
        log.info("Inicio de sesión correcto: usuario '{}'", evento.getAuthentication().getName());
    }

    @EventListener
    public void alFallar(AbstractAuthenticationFailureEvent evento) {
        log.warn("Inicio de sesión rechazado: usuario '{}' ({})", evento.getAuthentication().getName(),
                evento.getException().getClass().getSimpleName());
    }
}
