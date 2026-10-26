package com.utp.libreria.web;

import com.utp.libreria.model.Producto;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public class ProductoForm {
    private Long id;

    @NotBlank(message = "El código es obligatorio.")
    @Size(max = 20, message = "El código admite hasta 20 caracteres.")
    private String codigo;

    @NotBlank(message = "El título es obligatorio.")
    @Size(max = 120, message = "El título admite hasta 120 caracteres.")
    private String titulo;

    @Size(max = 80, message = "El autor admite hasta 80 caracteres.")
    private String autor;

    @NotNull(message = "Seleccione una categoría.")
    private Long categoriaId;

    @NotNull(message = "El precio es obligatorio.")
    @DecimalMin(value = "0.00", message = "El precio debe ser mayor o igual a 0.")
    @Digits(integer = 8, fraction = 2, message = "El precio admite hasta 2 decimales.")
    private BigDecimal precio;

    @NotNull(message = "El stock es obligatorio.")
    @Min(value = 0, message = "El stock debe ser mayor o igual a 0.")
    private Integer stock = 0;

    @NotNull(message = "El stock mínimo es obligatorio.")
    @Min(value = 0, message = "El stock mínimo debe ser mayor o igual a 0.")
    private Integer stockMinimo = 0;

    private boolean activo = true;

    public static ProductoForm desde(Producto p) {
        ProductoForm f = new ProductoForm();
        f.id = p.getId();
        f.codigo = p.getCodigo();
        f.titulo = p.getTitulo();
        f.autor = p.getAutor();
        f.categoriaId = p.getCategoria().getId();
        f.precio = p.getPrecio();
        f.stock = p.getStock();
        f.stockMinimo = p.getStockMinimo();
        f.activo = p.isActivo();
        return f;
    }

    // --- getters y setters ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public String getAutor() { return autor; }
    public void setAutor(String autor) { this.autor = autor; }
    public Long getCategoriaId() { return categoriaId; }
    public void setCategoriaId(Long categoriaId) { this.categoriaId = categoriaId; }
    public BigDecimal getPrecio() { return precio; }
    public void setPrecio(BigDecimal precio) { this.precio = precio; }
    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { this.stock = stock; }
    public Integer getStockMinimo() { return stockMinimo; }
    public void setStockMinimo(Integer stockMinimo) { this.stockMinimo = stockMinimo; }
    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }
}
