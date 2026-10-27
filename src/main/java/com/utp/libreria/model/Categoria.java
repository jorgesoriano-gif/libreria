package com.utp.libreria.model;

import jakarta.persistence.*;

@Entity
@Table(name = "categoria")
public class Categoria {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "categoria_id")
    private Long id;

    @Column(nullable = false, length = 10, unique = true)
    private String codigo;

    @Column(nullable = false, length = 50, unique = true)
    private String nombre;

    @Column(length = 150)
    private String descripcion;

    public Categoria() { }

    public Categoria(String codigo, String nombre) {
        this.codigo = codigo;
        this.nombre = nombre;
    }

    // --- getters y setters ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
}
