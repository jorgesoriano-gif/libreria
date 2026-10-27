package com.utp.libreria.service;

import com.utp.libreria.model.Rol;
import com.utp.libreria.model.Usuario;
import com.utp.libreria.repository.RolRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;

/** RF15 y RNF02: usuarios, roles y contraseñas con hash. */
@SpringBootTest
class UsuarioServiceTest {

    @Autowired UsuarioService usuarios;
    @Autowired RolRepository roles;
    @Autowired PasswordEncoder encoder;

    @Test
    void guardaLaContrasenaComoHashBCrypt() {
        Long rol = roles.findByNombre(Rol.VENDEDOR).orElseThrow().getId();
        String nombre = "u" + System.nanoTime() % 1_000_000_000L;
        Usuario u = usuarios.guardar(null, nombre, "Usuario de prueba", rol, "Clave1234", true);
        assertNotEquals("Clave1234", u.getPasswordHash());
        assertTrue(encoder.matches("Clave1234", u.getPasswordHash()));
    }

    @Test
    void rechazaContrasenasDebilesYUsuariosRepetidos() {
        Long rol = roles.findByNombre(Rol.VENDEDOR).orElseThrow().getId();
        assertThrows(NegocioException.class, () -> usuarios.guardar(null, "debil1", "X", rol, "abc", true));
        assertThrows(NegocioException.class, () -> usuarios.guardar(null, "debil2", "X", rol, "soloLetras", true));
        assertThrows(NegocioException.class, () -> usuarios.guardar(null, "admin", "X", rol, "Clave1234", true));
    }

    @Test
    void noPermiteDesactivarseNiQuedarseSinAdministrador() {
        Usuario admin = usuarios.listar().stream().filter(u -> u.getUsuario().equals("admin")).findFirst().orElseThrow();
        assertThrows(NegocioException.class, () -> usuarios.cambiarEstado(admin.getId(), "admin"));
        Long rolVendedor = roles.findByNombre(Rol.VENDEDOR).orElseThrow().getId();
        assertThrows(NegocioException.class,
                () -> usuarios.guardar(admin.getId(), "admin", "Administrador General", rolVendedor, null, true));
    }
}
