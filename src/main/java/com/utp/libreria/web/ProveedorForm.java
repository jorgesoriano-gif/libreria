package com.utp.libreria.web;

import com.utp.libreria.model.Proveedor;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class ProveedorForm {
    private Long id;

    @Pattern(regexp = "^\\d{11}$", message = "El RUC debe tener 11 dígitos.")
    private String ruc;

    @NotBlank(message = "La razón social es obligatoria.")
    @Size(max = 150, message = "La razón social admite hasta 150 caracteres.")
    private String razonSocial;

    @Size(max = 20, message = "El teléfono admite hasta 20 caracteres.")
    private String telefono;

    @Size(max = 120, message = "El correo admite hasta 120 caracteres.")
    private String correo;

    @Size(max = 200, message = "La dirección admite hasta 200 caracteres.")
    private String direccion;

    public static ProveedorForm desde(Proveedor p) {
        ProveedorForm f = new ProveedorForm();
        f.id = p.getId();
        f.ruc = p.getRuc();
        f.razonSocial = p.getRazonSocial();
        f.telefono = p.getTelefono();
        f.correo = p.getCorreo();
        f.direccion = p.getDireccion();
        return f;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getRuc() { return ruc; }
    public void setRuc(String ruc) { this.ruc = ruc; }
    public String getRazonSocial() { return razonSocial; }
    public void setRazonSocial(String razonSocial) { this.razonSocial = razonSocial; }
    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }
    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }
    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }
}
