package com.utp.libreria.web;

import com.utp.libreria.model.Cliente;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class ClienteForm {
    private Long id;

    @Pattern(regexp = "^(\\d{8}|\\d{11})?$", message = "El documento debe tener 8 (DNI) u 11 (RUC) dígitos.")
    private String documento;

    @NotBlank(message = "El nombre es obligatorio.")
    @Size(max = 80, message = "El nombre admite hasta 80 caracteres.")
    private String nombre;

    @Size(max = 15, message = "El teléfono admite hasta 15 caracteres.")
    private String telefono;

    public static ClienteForm desde(Cliente c) {
        ClienteForm f = new ClienteForm();
        f.id = c.getId();
        f.documento = c.getDocumento();
        f.nombre = c.getNombre();
        f.telefono = c.getTelefono();
        return f;
    }

    // --- getters y setters ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getDocumento() { return documento; }
    public void setDocumento(String documento) { this.documento = documento; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }
}
