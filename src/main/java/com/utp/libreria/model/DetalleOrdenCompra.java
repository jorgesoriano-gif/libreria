package com.utp.libreria.model;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "detalle_orden_compra")
public class DetalleOrdenCompra {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "detalle_orden_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "orden_id", nullable = false)
    private OrdenCompra orden;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @Column(nullable = false)
    private int cantidad;

    @Column(name = "cantidad_recibida", nullable = false)
    private int cantidadRecibida;

    @Column(name = "costo_unitario", nullable = false, precision = 12, scale = 2)
    private BigDecimal costoUnitario;

    /** En MySQL la columna "subtotal" es generada; aquí se calcula y no se mapea. */
    public BigDecimal getSubtotal() {
        return costoUnitario.multiply(BigDecimal.valueOf(cantidad));
    }

    // --- getters y setters ---
    public DetalleOrdenCompra() { }
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public OrdenCompra getOrden() { return orden; }
    public void setOrden(OrdenCompra orden) { this.orden = orden; }
    public Producto getProducto() { return producto; }
    public void setProducto(Producto producto) { this.producto = producto; }
    public int getCantidad() { return cantidad; }
    public void setCantidad(int cantidad) { this.cantidad = cantidad; }
    public int getCantidadRecibida() { return cantidadRecibida; }
    public void setCantidadRecibida(int cantidadRecibida) { this.cantidadRecibida = cantidadRecibida; }
    public BigDecimal getCostoUnitario() { return costoUnitario; }
    public void setCostoUnitario(BigDecimal costoUnitario) { this.costoUnitario = costoUnitario; }
}
