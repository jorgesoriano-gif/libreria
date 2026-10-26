package com.utp.libreria.service;

import com.utp.libreria.model.Producto;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Detalle de la venta en curso (M07). Vive en la sesión HTTP de cada vendedor. */
@Component
@SessionScope
public class CarritoVenta {

    public static class Item {
        private final Long productoId;
        private final String codigo;
        private final String titulo;
        private final BigDecimal precio;
        private final int stock;
        private int cantidad;

        public Item(Producto p, int cantidad) {
            this.productoId = p.getId();
            this.codigo = p.getCodigo();
            this.titulo = p.getTitulo();
            this.precio = p.getPrecio();
            this.stock = p.getStock();
            this.cantidad = cantidad;
        }

        public Long getProductoId() { return productoId; }
        public String getCodigo() { return codigo; }
        public String getTitulo() { return titulo; }
        public BigDecimal getPrecio() { return precio; }
        public int getStock() { return stock; }
        public int getCantidad() { return cantidad; }
        public BigDecimal getSubtotal() { return precio.multiply(BigDecimal.valueOf(cantidad)); }
    }

    private final Map<Long, Item> items = new LinkedHashMap<>();

    /** RF07 y RF09: agrega un producto validando activo y existencias (acumulando lo ya agregado). */
    public void agregar(Producto p, int cantidad) {
        if (cantidad <= 0) {
            throw new NegocioException("La cantidad debe ser mayor a 0.");
        }
        if (!p.isActivo()) {
            throw new NegocioException("El producto " + p.getCodigo() + " está desactivado.");
        }
        Item existente = items.get(p.getId());
        int total = cantidad + (existente == null ? 0 : existente.cantidad);
        if (total > p.getStock()) {
            throw new NegocioException("Stock insuficiente para " + p.getCodigo()
                    + ". Disponible: " + p.getStock() + (p.getStock() == 1 ? " unidad" : " unidades")
                    + "; solicitadas: " + total + ".");
        }
        if (existente == null) {
            items.put(p.getId(), new Item(p, cantidad));
        } else {
            existente.cantidad = total;
        }
    }

    public void quitar(Long productoId) { items.remove(productoId); }

    public void vaciar() { items.clear(); }

    public boolean isVacio() { return items.isEmpty(); }

    public List<Item> getItems() { return new ArrayList<>(items.values()); }

    public int getTotalUnidades() {
        return items.values().stream().mapToInt(Item::getCantidad).sum();
    }

    /** RF08: total de la venta. */
    public BigDecimal getTotal() {
        return items.values().stream().map(Item::getSubtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
