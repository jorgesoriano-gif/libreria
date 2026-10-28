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
     * RF10 / CU-07: transacción atómica. Sobrecarga compatible hacia atrás.
     */
    public Venta registrar(Long clienteId, String nombreUsuario, List<CarritoVenta.Item> items) {
        return registrar(clienteId, nombreUsuario, "NOTA_VENTA", "EFECTIVO", null, items);
    }

    /**
     * Registro completo con Tipo de Comprobante, Método de Pago y Monto Pagado.
     */
    public Venta registrar(Long clienteId, String nombreUsuario, String tipoComprobante,
                           String metodoPago, BigDecimal montoPagado, List<CarritoVenta.Item> items) {
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

                String tipoComp = (tipoComprobante != null && !tipoComprobante.isBlank())
                        ? tipoComprobante.trim().toUpperCase() : "NOTA_VENTA";
                String metPago = (metodoPago != null && !metodoPago.isBlank())
                        ? metodoPago.trim().toUpperCase() : "EFECTIVO";

                if ("FACTURA".equals(tipoComp)) {
                    if (cliente == null || cliente.getDocumento() == null || cliente.getDocumento().trim().length() != 11) {
                        throw new NegocioException("Para emitir Factura es obligatorio registrar o seleccionar un cliente con RUC válido (11 dígitos).");
                    }
                }

                Venta venta = new Venta();
                venta.setCliente(cliente);
                venta.setUsuario(vendedor);
                venta.setTipoComprobante(tipoComp);
                venta.setMetodoPago(metPago);
                venta.setMontoPagado(montoPagado);
                venta.setNroComprobante(siguienteComprobante(tipoComp));

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

                if (montoPagado != null && "EFECTIVO".equals(metPago) && montoPagado.compareTo(total) < 0) {
                    throw new NegocioException("El monto pagado en efectivo (S/ " + montoPagado
                            + ") no puede ser menor al total de la venta (S/ " + total + ").");
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
                log.info("Venta {} [{}] registrada: {} línea(s), total S/ {}", guardada.getNroComprobante(),
                        guardada.getTipoComprobante(), guardada.getDetalles().size(), guardada.getTotal());
                return guardada;
            });
        }
    }

    /** Correlativo según el tipo de comprobante (BOL-NNNNNN, FAC-NNNNNN, INT-NNNNNN). */
    private String siguienteComprobante(String tipo) {
        String prefijo = switch (tipo) {
            case "BOLETA" -> "BOL-";
            case "FACTURA" -> "FAC-";
            default -> "INT-";
        };
        int siguiente = ventas.findAll().stream()
                .map(Venta::getNroComprobante)
                .filter(nro -> nro != null && nro.startsWith(prefijo))
                .mapToInt(nro -> {
                    try {
                        return Integer.parseInt(nro.substring(prefijo.length()));
                    } catch (Exception e) {
                        return 0;
                    }
                })
                .max()
                .orElse(0) + 1;
        return String.format("%s%06d", prefijo, siguiente);
    }
}
