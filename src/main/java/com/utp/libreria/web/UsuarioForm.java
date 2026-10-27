package com.utp.libreria.web;

import com.utp.libreria.model.Usuario;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class UsuarioForm {
    private Long id;

    @NotBlank(message = "El usuario es obligatorio.")
    @Size(min = 3, max = 30, message = "El usuario debe tener de 3 a 30 caracteres.")
    private String usuario;

    @NotBlank(message = "El nombre es obligatorio.")
    @Size(max = 80, message = "El nombre admite hasta 80 caracteres.")
    private String nombre;

    @NotNull(message = "Seleccione un rol.")
    private Long rolId;

    /** Obligatoria al crear; al editar, vacía significa «no cambiar». */
    private String clave;

    private boolean activo = true;

    public static UsuarioForm desde(Usuario u) {
        UsuarioForm f = new UsuarioForm();
        f.id = u.getId();
        f.usuario = u.getUsuario();
        f.nombre = u.getNombre();
        f.rolId = u.getRol().getId();
        f.activo = u.isActivo();
        return f;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public Long getRolId() { return rolId; }
    public void setRolId(Long rolId) { this.rolId = rolId; }
    public String getClave() { return clave; }
    public void setClave(String clave) { this.clave = clave; }
    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }
}
