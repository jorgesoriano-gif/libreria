package com.utp.libreria.service;

import com.utp.libreria.model.*;
import com.utp.libreria.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.List;

@Service
public class VentaService {

    /** Serializa el cálculo del correlativo entre ventas simultáneas. */
    private static final Object BLOQUEO_CORRELATIVO = new Object();

    private final VentaRepository ventas;
    private final ProductoRepository productos;
    private final ClienteRepository clientes;
    private final UsuarioRepository usuarios;
    private final TransactionTemplate tx;

    public VentaService(VentaRepository ventas, ProductoRepository productos, ClienteRepository clientes,
                        UsuarioRepository usuarios,
                        PlatformTransactionManager txManager) {
        this.ventas = ventas;
        this.productos = productos;
        this.clientes = clientes;
        this.usuarios = usuarios;
        this.tx = new TransactionTemplate(txManager);
    }

    /**
     * RF10 / CU-07: transacción atómica. Verifica existencias, inserta la venta y sus detalles
     * y descuenta el stock. Ante cualquier fallo se revierte todo.
     */
    public Venta registrar(Long clienteId, String nombreUsuario, List<CarritoVenta.Item> items) {
        if (items.isEmpty()) {
            throw new NegocioException("Agregue al menos un producto a la venta.");
        }
        synchronized (BLOQUEO_CORRELATIVO) {
            return tx.execute(status -> {
                Usuario vendedor = usuarios.findByUsuario(nombreUsuario)
                        .orElseThrow(() -> new NegocioException("Usuario no encontrado."));
                Cliente cliente = null;
                if (clienteId != null) {
                    cliente = clientes.findById(clienteId)
                            .orElseThrow(() -> new NegocioException("El cliente seleccionado no existe."));
                }

                Venta venta = new Venta();
                venta.setCliente(cliente);
                venta.setUsuario(vendedor);
                venta.setNroComprobante(siguienteComprobante());

                BigDecimal total = BigDecimal.ZERO;

                for (CarritoVenta.Item item : items) {
                    Producto p = productos.bloquearPorId(item.getProductoId())
                            .orElseThrow(() -> new NegocioException("El producto " + item.getCodigo() + " ya no existe."));
                    if (!p.isActivo()) {
                        throw new NegocioException("El producto " + p.getCodigo() + " está desactivado.");
                    }
                    if (p.getStock() < item.getCantidad()) {
                        throw new NegocioException("Stock insuficiente: disponible " + p.getStock()
                                + (p.getStock() == 1 ? " unidad" : " unidades") + " (" + p.getCodigo() + ")");
                    }

                    DetalleVenta d = new DetalleVenta();
                    d.setProducto(p);
                    d.setCantidad(item.getCantidad());
                    d.setPrecioUnitario(p.getPrecio());
                    venta.agregarDetalle(d);
                    total = total.add(d.getSubtotal());

                    p.setStock(p.getStock() - item.getCantidad());
                    productos.save(p);
                }

                venta.setTotal(total);
                Venta guardada = ventas.save(venta);
                return guardada;
            });
        }
    }

    /** RF17: correlativo interno con formato INT-NNNNNN. */
    private String siguienteComprobante() {
        int siguiente = ventas.findTopByOrderByNroComprobanteDesc()
                .map(v -> Integer.parseInt(v.getNroComprobante().substring(4)) + 1)
                .orElse(1);
        return String.format("INT-%06d", siguiente);
    }
}
