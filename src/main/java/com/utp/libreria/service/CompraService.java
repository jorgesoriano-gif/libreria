package com.utp.libreria.service;

import com.google.common.base.Preconditions;
import com.utp.libreria.model.*;
import com.utp.libreria.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** M13 y M14 - Órdenes de compra y recepción de mercadería (RF19 a RF21, CU-19 a CU-21). */
@Service
public class CompraService {

    private static final Logger log = LoggerFactory.getLogger(CompraService.class);

    /** Serializa el cálculo del número de orden entre usuarios simultáneos. */
    private static final Object BLOQUEO_NUMERO = new Object();

    /** Línea solicitada al crear una orden. */
    public record Linea(Long productoId, int cantidad, BigDecimal costoUnitario) { }

    private final OrdenCompraRepository ordenes;
    private final DetalleOrdenCompraRepository detalles;
    private final ProveedorRepository proveedores;
    private final ProductoRepository productos;
    private final UsuarioRepository usuarios;
    private final MovimientoInventarioRepository movimientos;
    private final TransactionTemplate tx;

    public CompraService(OrdenCompraRepository ordenes, DetalleOrdenCompraRepository detalles,
                         ProveedorRepository proveedores, ProductoRepository productos,
                         UsuarioRepository usuarios, MovimientoInventarioRepository movimientos,
                         PlatformTransactionManager txManager) {
        this.ordenes = ordenes;
        this.detalles = detalles;
        this.proveedores = proveedores;
        this.productos = productos;
        this.usuarios = usuarios;
        this.movimientos = movimientos;
        this.tx = new TransactionTemplate(txManager);
    }

    /** RF19: crea una orden PENDIENTE. No modifica el stock (TC11). */
    public OrdenCompra crear(Long proveedorId, LocalDate entregaEstimada, List<Linea> lineas, String nombreUsuario) {
        Preconditions.checkNotNull(lineas, "lineas");
        Preconditions.checkNotNull(nombreUsuario, "nombreUsuario");
        if (proveedorId == null) {
            throw new NegocioException("Seleccione un proveedor.");
        }
        if (lineas.isEmpty()) {
            throw new NegocioException("Agregue al menos un producto a la orden.");
        }
        if (entregaEstimada != null && entregaEstimada.isBefore(LocalDate.now())) {
            throw new NegocioException("La fecha de entrega estimada no puede ser anterior a hoy.");
        }
        synchronized (BLOQUEO_NUMERO) {
            return tx.execute(status -> {
                Proveedor proveedor = proveedores.findById(proveedorId)
                        .orElseThrow(() -> new NegocioException("El proveedor no existe."));
                if (!proveedor.isActivo()) {
                    throw new NegocioException("El proveedor está desactivado.");
                }
                Usuario usuario = usuarios.findByUsuario(nombreUsuario)
                        .orElseThrow(() -> new NegocioException("Usuario no encontrado."));

                OrdenCompra orden = new OrdenCompra();
                orden.setNumero(siguienteNumero());
                orden.setProveedor(proveedor);
                orden.setUsuario(usuario);
                orden.setFechaEntregaEstimada(entregaEstimada);

                BigDecimal total = BigDecimal.ZERO;
                for (Linea l : lineas) {
                    if (l.cantidad() <= 0) {
                        throw new NegocioException("Las cantidades deben ser mayores a 0.");
                    }
                    if (l.costoUnitario() == null || l.costoUnitario().signum() < 0) {
                        throw new NegocioException("El costo unitario debe ser mayor o igual a 0.");
                    }
                    Producto p = productos.findById(l.productoId())
                            .orElseThrow(() -> new NegocioException("Un producto de la orden ya no existe."));
                    if (!p.isActivo()) {
                        throw new NegocioException("El producto " + p.getCodigo() + " está desactivado.");
                    }
                    DetalleOrdenCompra d = new DetalleOrdenCompra();
                    d.setProducto(p);
                    d.setCantidad(l.cantidad());
                    d.setCostoUnitario(l.costoUnitario());
                    orden.agregarDetalle(d);
                    total = total.add(d.getSubtotal());
                }
                orden.setTotal(total);
                OrdenCompra guardada = ordenes.save(orden);
                log.info("Orden {} creada: {} línea(s), total {}", guardada.getNumero(), lineas.size(), total);
                return guardada;
            });
        }
    }

    /**
     * RF20: recepción atómica. Bloquea la orden, suma el stock de cada línea y deja un movimiento
     * de ENTRADA vinculado a la orden. Si algo falla se revierte todo; una orden RECIBIDA no
     * puede recepcionarse otra vez (TC12, TC13).
     */
    @Transactional
    public OrdenCompra recepcionar(Long ordenId, String nombreUsuario) {
        Preconditions.checkNotNull(nombreUsuario, "nombreUsuario");
        OrdenCompra orden = ordenes.bloquearPorId(ordenId)
                .orElseThrow(() -> new NegocioException("La orden no existe."));
        if (orden.getEstado() == EstadoOrden.RECIBIDA) {
            throw new NegocioException("La orden " + orden.getNumero() + " ya fue recepcionada.");
        }
        Usuario usuario = usuarios.findByUsuario(nombreUsuario)
                .orElseThrow(() -> new NegocioException("Usuario no encontrado."));

        for (DetalleOrdenCompra d : orden.getDetalles()) {
            Producto p = productos.bloquearPorId(d.getProducto().getId())
                    .orElseThrow(() -> new NegocioException("Un producto de la orden ya no existe."));
            p.setStock(p.getStock() + d.getCantidad());
            productos.save(p);
            d.setCantidadRecibida(d.getCantidad());

            MovimientoInventario mov = new MovimientoInventario();
            mov.setProducto(p);
            mov.setOrden(orden);
            mov.setUsuario(usuario);
            mov.setTipo(TipoMovimiento.ENTRADA);
            mov.setCantidad(d.getCantidad());
            mov.setMotivo("Recepción " + orden.getNumero());
            movimientos.save(mov);
        }
        orden.setEstado(EstadoOrden.RECIBIDA);
        orden.setFechaRecepcion(LocalDateTime.now());
        OrdenCompra guardada = ordenes.save(orden);
        log.info("Orden {} recepcionada por '{}'", guardada.getNumero(), nombreUsuario);
        return guardada;
    }

    /** RF21: consulta con filtros por proveedor, fechas y estado (estado null = todos). */
    public List<OrdenCompra> buscar(long proveedorId, LocalDate desde, LocalDate hasta, EstadoOrden estado) {
        LocalDate d = desde == null ? LocalDate.of(2000, 1, 1) : desde;
        LocalDate h = hasta == null ? LocalDate.of(2999, 1, 1) : hasta;
        return ordenes.buscar(d.atStartOfDay(), h.plusDays(1).atStartOfDay(), proveedorId).stream()
                .filter(o -> estado == null || o.getEstado() == estado)
                .toList();
    }

    public OrdenCompra obtener(Long id) {
        return ordenes.findById(id).orElseThrow(() -> new NegocioException("La orden no existe."));
    }

    public List<DetalleOrdenCompra> detalleDe(Long ordenId) {
        return detalles.findByOrdenId(ordenId);
    }

    /** Correlativo interno OC-NNNNNN. */
    private String siguienteNumero() {
        int siguiente = ordenes.findTopByOrderByNumeroDesc()
                .map(o -> Integer.parseInt(o.getNumero().substring(3)) + 1)
                .orElse(1);
        return String.format("OC-%06d", siguiente);
    }
}
