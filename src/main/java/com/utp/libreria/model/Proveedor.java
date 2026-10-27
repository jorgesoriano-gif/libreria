package com.utp.libreria.model;

import jakarta.persistence.*;

@Entity
@Table(name = "proveedor")
public class Proveedor {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "proveedor_id")
    private Long id;

    @Column(nullable = false, length = 11, unique = true)
    private String ruc;

    @Column(name = "razon_social", nullable = false, length = 150)
    private String razonSocial;

    @Column(length = 20)
    private String telefono;

    @Column(length = 120)
    private String correo;

    @Column(length = 200)
    private String direccion;

    @Column(nullable = false)
    private boolean activo = true;

    // --- getters y setters ---
    public Proveedor() { }
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
    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }
}
