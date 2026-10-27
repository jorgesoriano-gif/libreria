package com.utp.libreria.service;

import com.google.common.base.Preconditions;
import com.utp.libreria.model.*;
import com.utp.libreria.repository.MovimientoInventarioRepository;
import com.utp.libreria.repository.ProductoRepository;
import com.utp.libreria.repository.UsuarioRepository;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** M05 - Inventario (RF04, RF05, CU-10 a CU-12). */
@Service
public class InventarioService {

    private static final Logger log = LoggerFactory.getLogger(InventarioService.class);

    private final ProductoRepository productos;
    private final MovimientoInventarioRepository movimientos;
    private final UsuarioRepository usuarios;

    public InventarioService(ProductoRepository productos, MovimientoInventarioRepository movimientos,
                             UsuarioRepository usuarios) {
        this.productos = productos;
        this.movimientos = movimientos;
        this.usuarios = usuarios;
    }

    /** RF05: productos activos con stock menor o igual al mínimo. */
    public List<Producto> alertas() {
        return productos.bajoMinimo();
    }

    public List<MovimientoInventario> historial(long productoId) {
        return productoId == 0 ? movimientos.findTop100ByOrderByFechaDescIdDesc()
                : movimientos.findTop100ByProductoIdOrderByFechaDescIdDesc(productoId);
    }

    /** Entrada o salida manual (ajuste, merma, devolución). Atómico y con stock nunca negativo. */
    @Transactional
    public MovimientoInventario registrar(Long productoId, TipoMovimiento tipo, int cantidad,
                                          String motivo, String nombreUsuario) {
        Preconditions.checkNotNull(tipo, "tipo");
        Preconditions.checkNotNull(nombreUsuario, "nombreUsuario");
        if (productoId == null) {
            throw new NegocioException("Seleccione un producto.");
        }
        if (cantidad <= 0) {
            throw new NegocioException("La cantidad debe ser mayor a 0.");
        }
        String m = StringUtils.normalizeSpace(StringUtils.trimToEmpty(motivo));
        if (m.isEmpty()) {
            throw new NegocioException("Indique el motivo del movimiento.");
        }
        if (m.length() > 100) {
            throw new NegocioException("El motivo admite hasta 100 caracteres.");
        }
        Usuario usuario = usuarios.findByUsuario(nombreUsuario)
                .orElseThrow(() -> new NegocioException("Usuario no encontrado."));
        Producto p = productos.bloquearPorId(productoId)
                .orElseThrow(() -> new NegocioException("El producto no existe."));
        if (tipo == TipoMovimiento.SALIDA) {
            if (p.getStock() < cantidad) {
                throw new NegocioException("Stock insuficiente en " + p.getCodigo() + ": disponible " + p.getStock() + ".");
            }
            p.setStock(p.getStock() - cantidad);
        } else {
            p.setStock(p.getStock() + cantidad);
        }
        productos.save(p);

        MovimientoInventario mov = new MovimientoInventario();
        mov.setProducto(p);
        mov.setUsuario(usuario);
        mov.setTipo(tipo);
        mov.setCantidad(cantidad);
        mov.setMotivo(m);
        MovimientoInventario guardado = movimientos.save(mov);
        log.info("Movimiento {} de {} unidades en {} (stock {})", tipo, cantidad, p.getCodigo(), p.getStock());
        return guardado;
    }
}
