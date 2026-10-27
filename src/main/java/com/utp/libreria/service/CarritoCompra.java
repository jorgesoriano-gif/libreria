package com.utp.libreria.service;

import com.utp.libreria.model.Producto;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Detalle de la orden de compra en curso (M13). Vive en la sesión HTTP. */
@Component
@SessionScope
public class CarritoCompra {

    public static class Item {
        private final Long productoId;
        private final String codigo;
        private final String titulo;
        private int cantidad;
        private BigDecimal costo;

        Item(Producto p, int cantidad, BigDecimal costo) {
            this.productoId = p.getId();
            this.codigo = p.getCodigo();
            this.titulo = p.getTitulo();
            this.cantidad = cantidad;
            this.costo = costo;
        }

        public Long getProductoId() { return productoId; }
        public String getCodigo() { return codigo; }
        public String getTitulo() { return titulo; }
        public int getCantidad() { return cantidad; }
        public BigDecimal getCosto() { return costo; }
        public BigDecimal getSubtotal() { return costo.multiply(BigDecimal.valueOf(cantidad)); }
    }

    private final Map<Long, Item> items = new LinkedHashMap<>();
    private Long proveedorId;
    private LocalDate entrega;

    public Long getProveedorId() { return proveedorId; }
    public void setProveedorId(Long proveedorId) { this.proveedorId = proveedorId; }
    public LocalDate getEntrega() { return entrega; }
    public void setEntrega(LocalDate entrega) { this.entrega = entrega; }

    public void agregar(Producto p, int cantidad, BigDecimal costo) {
        if (cantidad <= 0) {
            throw new NegocioException("La cantidad debe ser mayor a 0.");
        }
        if (costo == null || costo.signum() < 0) {
            throw new NegocioException("El costo unitario debe ser mayor o igual a 0.");
        }
        if (!p.isActivo()) {
            throw new NegocioException("El producto " + p.getCodigo() + " está desactivado.");
        }
        Item existente = items.get(p.getId());
        if (existente == null) {
            items.put(p.getId(), new Item(p, cantidad, costo));
        } else {
            existente.cantidad += cantidad;
            existente.costo = costo;
        }
    }

    public void quitar(Long productoId) { items.remove(productoId); }

    public void vaciar() {
        items.clear();
        proveedorId = null;
        entrega = null;
    }

    public boolean isVacio() { return items.isEmpty(); }

    public List<Item> getItems() { return new ArrayList<>(items.values()); }

    public BigDecimal getTotal() {
        return items.values().stream().map(Item::getSubtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
