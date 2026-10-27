package com.utp.libreria.service;

import com.google.common.base.Preconditions;
import com.utp.libreria.model.*;
import com.utp.libreria.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.List;

@Service
public class VentaService {

    private static final Logger log = LoggerFactory.getLogger(VentaService.class);

    /** Serializa el cálculo del correlativo entre ventas simultáneas. */
    private static final Object BLOQUEO_CORRELATIVO = new Object();

    private final VentaRepository ventas;
    private final ProductoRepository productos;
    private final ClienteRepository clientes;
    private final UsuarioRepository usuarios;
    private final MovimientoInventarioRepository movimientos;
    private final TransactionTemplate tx;

    public VentaService(VentaRepository ventas, ProductoRepository productos, ClienteRepository clientes,
                        UsuarioRepository usuarios, MovimientoInventarioRepository movimientos,
                        PlatformTransactionManager txManager) {
        this.ventas = ventas;
        this.productos = productos;
        this.clientes = clientes;
        this.usuarios = usuarios;
        this.movimientos = movimientos;
        this.tx = new TransactionTemplate(txManager);
    }

    /**
     * RF10 / CU-07: transacción atómica. Verifica existencias, inserta la venta y sus detalles
     * y descuenta el stock. Ante cualquier fallo se revierte todo.
     */
    public Venta registrar(Long clienteId, String nombreUsuario, List<CarritoVenta.Item> items) {
        Preconditions.checkNotNull(items, "items");
        Preconditions.checkNotNull(nombreUsuario, "nombreUsuario");
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

                // RF04: cada línea deja su movimiento de SALIDA con trazabilidad a la venta
                for (DetalleVenta d : guardada.getDetalles()) {
                    MovimientoInventario mov = new MovimientoInventario();
                    mov.setProducto(d.getProducto());
                    mov.setVenta(guardada);
                    mov.setUsuario(vendedor);
                    mov.setTipo(TipoMovimiento.SALIDA);
                    mov.setCantidad(d.getCantidad());
                    mov.setMotivo("Venta " + guardada.getNroComprobante());
                    movimientos.save(mov);
                }
                log.info("Venta {} registrada: {} línea(s), total {}", guardada.getNroComprobante(),
                        guardada.getDetalles().size(), guardada.getTotal());
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
