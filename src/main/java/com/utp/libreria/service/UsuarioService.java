package com.utp.libreria.service;

import com.google.common.base.Preconditions;
import com.utp.libreria.model.Rol;
import com.utp.libreria.model.Usuario;
import com.utp.libreria.repository.RolRepository;
import com.utp.libreria.repository.UsuarioRepository;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** M11 - Usuarios y roles (RF15, CU-03). */
@Service
public class UsuarioService {

    private static final Logger log = LoggerFactory.getLogger(UsuarioService.class);

    private final UsuarioRepository usuarios;
    private final RolRepository roles;
    private final PasswordEncoder encoder;

    public UsuarioService(UsuarioRepository usuarios, RolRepository roles, PasswordEncoder encoder) {
        this.usuarios = usuarios;
        this.roles = roles;
        this.encoder = encoder;
    }

    public List<Usuario> listar() {
        return usuarios.findAllByOrderByNombre();
    }

    public List<Rol> roles() {
        return roles.findAll();
    }

    /** RNF02: mínimo 8 caracteres con al menos una letra y un número. */
    public static void validarClave(String clave) {
        if (clave == null || clave.length() < 8
                || !StringUtils.containsAny(clave, "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ")
                || !StringUtils.containsAny(clave, "0123456789")) {
            throw new NegocioException("La contraseña debe tener al menos 8 caracteres, con letras y números.");
        }
    }

    @Transactional
    public Usuario guardar(Long id, String usuario, String nombre, Long rolId, String clave, boolean activo) {
        String u = StringUtils.trimToEmpty(usuario);
        if (u.length() < 3 || u.length() > 30 || !u.matches("[A-Za-z0-9._-]+")) {
            throw new NegocioException("El usuario debe tener de 3 a 30 caracteres (letras, números, punto, guion).");
        }
        String n = StringUtils.normalizeSpace(StringUtils.trimToEmpty(nombre));
        if (n.isEmpty() || n.length() > 80) {
            throw new NegocioException("El nombre es obligatorio (hasta 80 caracteres).");
        }
        Rol rol = roles.findById(rolId).orElseThrow(() -> new NegocioException("Seleccione un rol válido."));
        usuarios.findByUsuario(u).ifPresent(otro -> {
            if (!otro.getId().equals(id)) {
                throw new NegocioException("El nombre de usuario ya existe.");
            }
        });

        Usuario entidad;
        if (id == null) {
            validarClave(clave);
            entidad = new Usuario();
            entidad.setPasswordHash(encoder.encode(clave));
        } else {
            entidad = usuarios.findById(id).orElseThrow(() -> new NegocioException("El usuario no existe."));
            asegurarAdministrador(entidad, rol, activo);
            if (StringUtils.isNotEmpty(clave)) {
                validarClave(clave);
                entidad.setPasswordHash(encoder.encode(clave));
            }
        }
        entidad.setUsuario(u);
        entidad.setNombre(n);
        entidad.setRol(rol);
        entidad.setActivo(activo);
        Usuario guardado = usuarios.save(entidad);
        log.info("Usuario '{}' guardado con rol {}", guardado.getUsuario(), rol.getNombre());
        return guardado;
    }

    /** Activa o desactiva; no permite desactivarse a sí mismo ni dejar al sistema sin administrador. */
    @Transactional
    public Usuario cambiarEstado(Long id, String usuarioActual) {
        Usuario u = usuarios.findById(id).orElseThrow(() -> new NegocioException("El usuario no existe."));
        Preconditions.checkNotNull(usuarioActual, "usuarioActual");
        if (u.isActivo()) {
            if (u.getUsuario().equals(usuarioActual)) {
                throw new NegocioException("No puede desactivar su propia cuenta.");
            }
            asegurarAdministrador(u, u.getRol(), false);
        }
        u.setActivo(!u.isActivo());
        log.info("Usuario '{}' {}", u.getUsuario(), u.isActivo() ? "activado" : "desactivado");
        return usuarios.save(u);
    }

    /** Evita quedarse sin ningún Administrador activo. */
    private void asegurarAdministrador(Usuario actual, Rol nuevoRol, boolean nuevoActivo) {
        boolean eraAdminActivo = actual.isActivo() && Rol.ADMINISTRADOR.equals(actual.getRol().getNombre());
        boolean seguiraSiendo = nuevoActivo && Rol.ADMINISTRADOR.equals(nuevoRol.getNombre());
        if (eraAdminActivo && !seguiraSiendo
                && usuarios.countByRolIdAndActivoTrue(actual.getRol().getId()) <= 1) {
            throw new NegocioException("Debe existir al menos un Administrador activo.");
        }
    }
}
