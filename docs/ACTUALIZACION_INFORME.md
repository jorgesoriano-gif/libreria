# Qué actualizar en el informe (según la rúbrica)

## 3.8 Arquitectura inicial y principios (Tabla 63)
- **SOLID**: S (un servicio por módulo) y D (inyección por constructor); en O/I cite `ReporteService` (exportadores por formato) y `Catalogos`.
- **TDD**: "pruebas en `src/test/java/.../service`: 8 clases, casos TC03, TC04, TC08–TC11, TC13, TC15, TC17, TC19". Pegue la captura de `./gradlew test`.
- Estructura de paquetes (3.8.1): agregue `VentaService, CompraService, InventarioService, CategoriaService, ProveedorService, UsuarioService, ReporteService, CarritoCompra`; `repository/` ahora tiene 11 interfaces.

## 3.8.4 Seguridad (Tabla 64) → pasar a «Implementado»
- Secretos en el código: `application.properties` usa `${DB_USER}` y `${DB_PASSWORD}`.
- Mínimo privilegio: `db/usuario_app.sql`.
- Sesión y respaldo: `server.servlet.session.timeout=30m`, `db/respaldo.sh` (programar con cron/Programador de tareas).
- Agregar filas: contraseña robusta (UsuarioService.validarClave), no desactivar la propia cuenta / último admin, neutralización de fórmulas en Excel/CSV.

## 3.9 Recursos Java (Tabla 65) → «Incorporado»
| Librería | Evidencia |
|---|---|
| Guava | `Preconditions` en VentaService, CompraService, InventarioService, UsuarioService, ReporteService; `Catalogos`; `ImmutableList` en ReporteService |
| Apache POI | `ReporteService.ventasXlsx / existenciasXlsx` (M10 Reportes) |
| Apache Commons | `StringUtils` (CategoriaService, ProveedorService, InventarioService, controladores); `EmailValidator` (ProveedorService) |
| Logback | `logback-spring.xml`; `AuditoriaAccesoListener`; logs en servicios |
Pegue el bloque real de `build.gradle` (versiones: guava 33.4.8-jre, poi-ooxml 5.4.1, commons-lang3 3.18.0, commons-validator 1.9.0).
RF12 ya cumple «exportar a CSV y Excel».

## 3.11 Interfaces gráficas (Tabla 66) → las 14 pantallas «Implementada»
M04 categorias.html · M05 inventario.html · M10 reportes.html · M11 usuarios.html/usuario-form.html · M12 proveedores.html/proveedor-form.html · M13 compras.html/compra-nueva.html · M14 compra-detalle.html.
Reemplace "siete pantallas están implementadas…" por "las catorce pantallas están implementadas".

## 3.5 Modelamiento de BD
El script `db/libreria_db.sql` ya existe: cite el archivo (tablas, PK/FK, CHECK, índices, vistas).

## Evidencias que SOLO usted puede generar (no las invente)
1. **Git (3.7.1, Fig. 77)**: `git add -A && git commit -m "feat: módulos M04, M05, M10–M14, librerías y pruebas"` y `git push`; capture `git log --oneline --graph --all` y la página de GitHub. Cree los tags prometidos: `git tag v0.3-integrada && git push --tags`.
2. **cloc (3.7.2, Tabla 62/Fig. 78)**: `cloc src db --exclude-dir=bin,build`. Pegue cifras reales y el hash (`git rev-parse --short HEAD`).
3. **Pruebas (Cap. 4)**: ejecute `./gradlew test` y complete Tabla 67 con resultados reales (aprobado/falló). Las pruebas manuales TC01, TC02, TC05–TC07, TC12, TC14, TC16, TC18 se hacen en el navegador.
4. **Capturas** de los 14 formularios en funcionamiento (3.6.1 / 3.11).
5. **Manual de instalación (4.2.4)** con capturas.
