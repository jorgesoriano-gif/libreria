package com.utp.libreria.config;

import com.utp.libreria.model.*;
import com.utp.libreria.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Al arrancar:
 *  1) Si la base está vacía (perfil h2) carga roles, usuarios y datos de ejemplo.
 *  2) Si los usuarios vienen del script SQL con el hash de ejemplo, les asigna una contraseña inicial BCrypt.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private static final Map<String, String> CLAVES_INICIALES = Map.of(
            "admin", "Admin123",
            "vendedor1", "Vendedor123",
            "inventario1", "Inventario123");

    private final RolRepository roles;
    private final UsuarioRepository usuarios;
    private final CategoriaRepository categorias;
    private final ProductoRepository productos;
    private final ClienteRepository clientes;
    private final ProveedorRepository proveedores;
    private final PasswordEncoder encoder;

    public DataInitializer(RolRepository roles, UsuarioRepository usuarios, CategoriaRepository categorias,
                           ProductoRepository productos, ClienteRepository clientes,
                           ProveedorRepository proveedores, PasswordEncoder encoder) {
        this.roles = roles;
        this.usuarios = usuarios;
        this.categorias = categorias;
        this.productos = productos;
        this.clientes = clientes;
        this.proveedores = proveedores;
        this.encoder = encoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (roles.count() == 0) {
            cargarDatosDeEjemplo();
        }
        for (Usuario u : usuarios.findAll()) {
            if (u.getPasswordHash().contains("REEMPLAZAR")) {
                u.setPasswordHash(encoder.encode(CLAVES_INICIALES.getOrDefault(u.getUsuario(), "Cambiar123")));
                usuarios.save(u);
                log.info("Contraseña inicial asignada al usuario '{}'. Cámbiela desde el módulo Usuarios.", u.getUsuario());
            }
        }
    }

    private void cargarDatosDeEjemplo() {
        Rol admin = roles.save(new Rol(Rol.ADMINISTRADOR));
        Rol vendedor = roles.save(new Rol(Rol.VENDEDOR));
        Rol inventario = roles.save(new Rol(Rol.INVENTARIO));

        crearUsuario(admin, "admin", "Administrador General", true);
        crearUsuario(vendedor, "vendedor1", "Vendedor Uno", true);
        crearUsuario(vendedor, "vendedor2", "Vendedor Dos (inactivo)", false);
        crearUsuario(inventario, "inventario1", "Encargado Inventario", true);

        Categoria novela = categorias.save(new Categoria("CAT-001", "Novela"));
        Categoria escolar = categorias.save(new Categoria("CAT-002", "Texto escolar"));
        Categoria infantil = categorias.save(new Categoria("CAT-003", "Infantil"));
        Categoria utiles = categorias.save(new Categoria("CAT-004", "Útiles de escritorio"));
        Categoria papel = categorias.save(new Categoria("CAT-005", "Papelería"));

        producto(novela, "LIB-0001", "Conversación en La Catedral", "Mario Vargas Llosa", "69.90", 12, 3);
        producto(novela, "LIB-0002", "Los ríos profundos", "José María Arguedas", "39.50", 8, 3);
        producto(infantil, "LIB-0003", "El principito", "Antoine de Saint-Exupéry", "29.90", 20, 5);
        producto(escolar, "LIB-0004", "Matemática 5.º primaria", "Varios", "45.00", 15, 5);
        producto(novela, "LIB-0005", "Aves sin nido", "Clorinda Matto de Turner", "34.00", 1, 3);
        producto(utiles, "PAP-0001", "Cuaderno cuadriculado A4", null, "6.50", 60, 20);
        producto(utiles, "PAP-0002", "Lapicero azul", null, "1.50", 200, 50);
        producto(papel, "PAP-0003", "Papel bond A4 (500 hojas)", null, "22.00", 25, 10);

        Cliente c1 = new Cliente();
        c1.setDocumento("45871236"); c1.setNombre("María Quispe"); c1.setTelefono("987654321");
        clientes.save(c1);
        Cliente c2 = new Cliente();
        c2.setDocumento("20512345678"); c2.setNombre("Colegio San Martín S.A.C."); c2.setTelefono("014567890");
        clientes.save(c2);
        proveedor("20123456789", "Distribuidora de Libros Andina S.A.C.", "012345678", "ventas@andina.example.com");
        proveedor("20987654321", "Papelera Central E.I.R.L.", "017654321", "pedidos@papelera.example.com");
        log.info("Base vacía: datos de ejemplo cargados.");
    }

    private void proveedor(String ruc, String razonSocial, String telefono, String correo) {
        Proveedor p = new Proveedor();
        p.setRuc(ruc);
        p.setRazonSocial(razonSocial);
        p.setTelefono(telefono);
        p.setCorreo(correo);
        proveedores.save(p);
    }

    private void crearUsuario(Rol rol, String usuario, String nombre, boolean activo) {
        Usuario u = new Usuario();
        u.setRol(rol);
        u.setUsuario(usuario);
        u.setNombre(nombre);
        u.setActivo(activo);
        u.setPasswordHash(encoder.encode(CLAVES_INICIALES.getOrDefault(usuario, "Cambiar123")));
        usuarios.save(u);
    }

    private void producto(Categoria cat, String codigo, String titulo, String autor, String precio, int stock, int minimo) {
        Producto p = new Producto();
        p.setCategoria(cat);
        p.setCodigo(codigo);
        p.setTitulo(titulo);
        p.setAutor(autor);
        p.setPrecio(new BigDecimal(precio));
        p.setStock(stock);
        p.setStockMinimo(minimo);
        productos.save(p);
    }
}
