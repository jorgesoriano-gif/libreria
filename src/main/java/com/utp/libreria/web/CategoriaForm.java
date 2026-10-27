package com.utp.libreria.web;

import com.utp.libreria.model.Categoria;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CategoriaForm {
    private Long id;

    @NotBlank(message = "El nombre es obligatorio.")
    @Size(max = 50, message = "El nombre admite hasta 50 caracteres.")
    private String nombre;

    @Size(max = 150, message = "La descripción admite hasta 150 caracteres.")
    private String descripcion;

    public static CategoriaForm desde(Categoria c) {
        CategoriaForm f = new CategoriaForm();
        f.id = c.getId();
        f.nombre = c.getNombre();
        f.descripcion = c.getDescripcion();
        return f;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
}
