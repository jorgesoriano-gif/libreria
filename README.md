# Librería «Pluma y Papel» — Sistema de Gestión de Librería

Spring Boot 4.1.2 + Thymeleaf + Spring Security + Spring Data JPA + MySQL (Gradle, Java 25).

## Módulos (14 pantallas, M01–M14)
| Pantalla | Ruta | Rol |
|---|---|---|
| M01 Inicio de sesión | `/login` | todos |
| M02 Panel operativo | `/` | todos |
| M03 Productos | `/productos` | todos consultan; Administrador edita |
| M04 Categorías | `/categorias` | Administrador |
| M05 Inventario (stock, alertas, movimientos) | `/inventario` | Administrador, Inventario |
| M06 Clientes | `/clientes` | Administrador, Vendedor |
| M07 Nueva venta · M08 Comprobante · M09 Historial | `/ventas/**` | Administrador, Vendedor |
| M10 Reportes (Excel y CSV) | `/reportes` | Administrador |
| M11 Usuarios y roles | `/usuarios` | Administrador |
| M12 Proveedores | `/proveedores` | Administrador, Inventario |
| M13 Órdenes de compra · M14 Recepción | `/compras/**` | Administrador, Inventario |

## Arquitectura y principios
- **MVC**: `web/` (controladores) · `templates/` (vistas Thymeleaf) · `model/` + `service/` (modelo).
- **DAO**: `repository/` (interfaces Spring Data JPA, consultas con parámetros nombrados).
- **SOLID**: una responsabilidad por servicio (`VentaService`, `CompraService`, `InventarioService`, …); dependencias por constructor.
- **TDD**: pruebas JUnit 5 en `src/test` (`./gradlew test`): carrito, venta, compras, inventario, categorías, proveedores, usuarios y reportes.

## Librerías de apoyo
| Librería | Dónde se usa |
|---|---|
| Google Guava | `Preconditions` en servicios; `ImmutableList/ImmutableMap` en `model/Catalogos` y `ReporteService` |
| Apache POI | `ReporteService`: exportación a `.xlsx` (con neutralización de fórmulas) |
| Apache Commons | `commons-lang3` (`StringUtils`) en servicios y búsquedas; `commons-validator` (`EmailValidator`) en proveedores |
| Logback | `logback-spring.xml` (archivo rotativo `logs/libreria.log`); auditoría de accesos, ventas, compras e inventario |

## Ejecutar
1. Ejecute `db/libreria_db.sql` en MySQL 8.0.16+ (crea la base **desde cero**; use esto si su base anterior fue creada por Hibernate).
2. Opcional (mínimo privilegio): `db/usuario_app.sql` y luego defina variables de entorno:
   - PowerShell: `$env:DB_USER="libreria_app"; $env:DB_PASSWORD="<clave>"`
   - Linux/macOS: `export DB_USER=libreria_app DB_PASSWORD=<clave>`
3. `./gradlew bootRun` → <http://localhost:8080>

Sin MySQL (datos en memoria): `./gradlew bootRun --args="--spring.profiles.active=h2"`

Pruebas: `./gradlew test` · Respaldo diario: `db/respaldo.sh`

## Usuarios iniciales
| Usuario | Contraseña | Rol |
|---|---|---|
| admin | Admin123 | Administrador |
| vendedor1 | Vendedor123 | Vendedor |
| inventario1 | Inventario123 | Encargado de inventario |

Cambie estas contraseñas desde **Usuarios** antes de usar el sistema.

## Seguridad
BCrypt para contraseñas · reglas por rol en `SecurityConfig` · CSRF activo · salida escapada (`th:text`) ·
Bean Validation + validaciones en servicios · consultas parametrizadas · credenciales por variables de entorno ·
sesión con expiración (30 min) · logs sin contraseñas ni documentos · exportaciones sin inyección de fórmulas.
