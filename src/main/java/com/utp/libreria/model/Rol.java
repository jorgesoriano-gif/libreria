package com.utp.libreria.model;

import jakarta.persistence.*;

@Entity
@Table(name = "rol")
public class Rol {
    public static final String ADMINISTRADOR = "Administrador";
    public static final String VENDEDOR = "Vendedor";
    public static final String INVENTARIO = "Encargado de inventario";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "rol_id")
    private Long id;

    @Column(nullable = false, length = 30, unique = true)
    private String nombre;

    public Rol(String nombre) { this.nombre = nombre; }

    /** Código usado por Spring Security (ROLE_ADMIN, ROLE_VENDEDOR, ROLE_INVENTARIO). */
    public String codigo() {
        return switch (nombre) {
            case ADMINISTRADOR -> "ADMIN";
            case VENDEDOR -> "VENDEDOR";
            default -> "INVENTARIO";
        };
    }

    // --- getters y setters ---
    public Rol() { }
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
}
