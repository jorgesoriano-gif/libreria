package com.utp.libreria.service;

import com.utp.libreria.model.Proveedor;
import com.utp.libreria.repository.ProveedorRepository;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.EmailValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** M12 - Proveedores (RF18, CU-18). */
@Service
public class ProveedorService {

    private static final Logger log = LoggerFactory.getLogger(ProveedorService.class);

    private final ProveedorRepository proveedores;

    public ProveedorService(ProveedorRepository proveedores) {
        this.proveedores = proveedores;
    }

    public List<Proveedor> buscar(String q) {
        return proveedores.buscar(StringUtils.normalizeSpace(StringUtils.trimToEmpty(q)));
    }

    public List<Proveedor> activos() {
        return proveedores.findByActivoTrueOrderByRazonSocial();
    }

    @Transactional
    public Proveedor guardar(Long id, String ruc, String razonSocial, String telefono, String correo, String direccion) {
        String r = StringUtils.trimToEmpty(ruc);
        if (r.length() != 11 || !StringUtils.isNumeric(r)) {
            throw new NegocioException("El RUC debe tener 11 dígitos numéricos.");
        }
        String rs = StringUtils.normalizeSpace(StringUtils.trimToEmpty(razonSocial));
        if (rs.isEmpty()) {
            throw new NegocioException("La razón social es obligatoria.");
        }
        String mail = StringUtils.trimToNull(correo);
        if (mail != null && !EmailValidator.getInstance().isValid(mail)) {
            throw new NegocioException("El correo no tiene un formato válido.");
        }
        proveedores.findByRuc(r).ifPresent(otro -> {
            if (!otro.getId().equals(id)) {
                throw new NegocioException("El RUC ya está registrado (" + otro.getRazonSocial() + ").");
            }
        });
        Proveedor p = id == null ? new Proveedor()
                : proveedores.findById(id).orElseThrow(() -> new NegocioException("El proveedor no existe."));
        p.setRuc(r);
        p.setRazonSocial(rs);
        p.setTelefono(StringUtils.trimToNull(telefono));
        p.setCorreo(mail);
        p.setDireccion(StringUtils.trimToNull(direccion));
        Proveedor guardado = proveedores.save(p);
        log.info("Proveedor id={} guardado", guardado.getId());   // sin datos personales
        return guardado;
    }

    @Transactional
    public Proveedor cambiarEstado(Long id) {
        Proveedor p = proveedores.findById(id).orElseThrow(() -> new NegocioException("El proveedor no existe."));
        p.setActivo(!p.isActivo());
        return proveedores.save(p);
    }
}
