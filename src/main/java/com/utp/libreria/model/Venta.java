package com.utp.libreria.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "venta")
public class Venta {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "venta_id")
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "nro_comprobante", nullable = false, length = 12, unique = true)
    private String nroComprobante;

    @Column(nullable = false)
    private LocalDateTime fecha = LocalDateTime.now();

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal total = BigDecimal.ZERO;

    /** Tipo de comprobante: BOLETA, FACTURA, NOTA_VENTA */
    @Column(name = "tipo_comprobante", nullable = false, length = 20)
    private String tipoComprobante = "NOTA_VENTA";

    /** Método de pago: EFECTIVO, TARJETA, YAPE_PLIN */
    @Column(name = "metodo_pago", nullable = false, length = 20)
    private String metodoPago = "EFECTIVO";

    /** Monto entregado por el cliente (para calcular vuelto). Opcional. */
    @Column(name = "monto_pagado", precision = 10, scale = 2)
    private BigDecimal montoPagado;

    @OneToMany(mappedBy = "venta", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DetalleVenta> detalles = new ArrayList<>();

    public void agregarDetalle(DetalleVenta d) {
        d.setVenta(this);
        detalles.add(d);
    }

    // Vuelto calculado (transitorio, no persiste)
    @Transient
    public BigDecimal getVuelto() {
        if (montoPagado == null || montoPagado.compareTo(BigDecimal.ZERO) <= 0) return BigDecimal.ZERO;
        BigDecimal v = montoPagado.subtract(total);
        return v.compareTo(BigDecimal.ZERO) >= 0 ? v : BigDecimal.ZERO;
    }

    // --- getters y setters ---
    public Venta() { }
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Cliente getCliente() { return cliente; }
    public void setCliente(Cliente cliente) { this.cliente = cliente; }
    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
    public String getNroComprobante() { return nroComprobante; }
    public void setNroComprobante(String nroComprobante) { this.nroComprobante = nroComprobante; }
    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }
    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }
    public String getTipoComprobante() { return tipoComprobante; }
    public void setTipoComprobante(String tipoComprobante) { this.tipoComprobante = tipoComprobante; }
    public String getMetodoPago() { return metodoPago; }
    public void setMetodoPago(String metodoPago) { this.metodoPago = metodoPago; }
    public BigDecimal getMontoPagado() { return montoPagado; }
    public void setMontoPagado(BigDecimal montoPagado) { this.montoPagado = montoPagado; }
    public List<DetalleVenta> getDetalles() { return detalles; }
    public void setDetalles(List<DetalleVenta> detalles) { this.detalles = detalles; }
}
