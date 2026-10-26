# Librería «Pluma y Papel» (login, clientes, productos y ventas)

Spring Boot 4.1.2 + Thymeleaf + Spring Security + Spring Data JPA + MySQL (Gradle, Java 25).

Módulos programados: **M01 Login**, **M03 Productos**, **M06 Clientes**, **M07–M09 Ventas** (nueva venta, comprobante, historial).
Inventario, categorías, reportes y usuarios quedan fuera (vacíos). Las categorías y usuarios existen solo como datos iniciales.

## Ejecutar
1. Ejecute `db/libreria_mysql.sql` en MySQL 8.0.16+ (o use el perfil h2).
2. Ajuste `DB_USER` / `DB_PASSWORD` (variables de entorno) o `application.properties`.
3. `./gradlew bootRun` → <http://localhost:8080>

Sin MySQL (datos en memoria): `./gradlew bootRun --args="--spring.profiles.active=h2"`

## Usuarios iniciales
| Usuario | Contraseña | Rol | Accesos |
|---|---|---|---|
| admin | Admin123 | Administrador | todo |
| vendedor1 | Vendedor123 | Vendedor | clientes, ventas, consulta de productos |
| inventario1 | Inventario123 | Encargado de inventario | consulta de productos |
