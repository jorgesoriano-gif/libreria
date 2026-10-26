package com.utp.libreria.service;

/** Error de regla de negocio: el mensaje se muestra tal cual al usuario. */
public class NegocioException extends RuntimeException {
    public NegocioException(String mensaje) {
        super(mensaje);
    }
}
