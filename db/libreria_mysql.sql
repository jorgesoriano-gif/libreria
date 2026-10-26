-- =====================================================================
-- Sistema de Gestión de Librería - Script de base de datos MySQL
-- Basado en Cap. 2 (2.4 Modelo de Datos y 2.5 Diccionario de Datos)
-- Requiere MySQL 8.0.16 o superior (aplica restricciones CHECK)
-- =====================================================================

DROP DATABASE IF EXISTS libreria;
CREATE DATABASE libreria
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;
USE libreria;

-- ---------------------------------------------------------------------
-- 1. ROL
-- ---------------------------------------------------------------------
CREATE TABLE rol (
    rol_id  INT          NOT NULL AUTO_INCREMENT,
    nombre  VARCHAR(30)  NOT NULL,
    CONSTRAINT pk_rol        PRIMARY KEY (rol_id),
    CONSTRAINT uq_rol_nombre UNIQUE (nombre)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- 2. USUARIO
-- ---------------------------------------------------------------------
CREATE TABLE usuario (
    usuario_id     INT          NOT NULL AUTO_INCREMENT,
    rol_id         INT          NOT NULL,
    usuario        VARCHAR(30)  NOT NULL,
    password_hash  VARCHAR(100) NOT NULL,          -- BCrypt (60 car.), RNF02
    nombre         VARCHAR(80)  NOT NULL,
    activo         BOOLEAN      NOT NULL DEFAULT TRUE,   -- RF15
    CONSTRAINT pk_usuario         PRIMARY KEY (usuario_id),
    CONSTRAINT uq_usuario_usuario UNIQUE (usuario),
    CONSTRAINT fk_usuario_rol     FOREIGN KEY (rol_id) REFERENCES rol (rol_id)
        ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- 3. CATEGORIA
-- ---------------------------------------------------------------------
CREATE TABLE categoria (
    categoria_id  INT          NOT NULL AUTO_INCREMENT,
    nombre        VARCHAR(50)  NOT NULL,           -- RF16
    CONSTRAINT pk_categoria        PRIMARY KEY (categoria_id),
    CONSTRAINT uq_categoria_nombre UNIQUE (nombre)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- 4. PRODUCTO
-- ---------------------------------------------------------------------
CREATE TABLE producto (
    producto_id   INT            NOT NULL AUTO_INCREMENT,
    categoria_id  INT            NOT NULL,
    codigo        VARCHAR(20)    NOT NULL,
    titulo        VARCHAR(120)   NOT NULL,
    autor         VARCHAR(80)    NULL,             -- RF03
    precio        DECIMAL(10,2)  NOT NULL,         -- soles
    stock         INT            NOT NULL DEFAULT 0,
    stock_minimo  INT            NOT NULL DEFAULT 0,   -- RF05
    activo        BOOLEAN        NOT NULL DEFAULT TRUE,
    CONSTRAINT pk_producto        PRIMARY KEY (producto_id),
    CONSTRAINT uq_producto_codigo UNIQUE (codigo),
    CONSTRAINT fk_producto_categoria FOREIGN KEY (categoria_id)
        REFERENCES categoria (categoria_id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT ck_producto_precio       CHECK (precio >= 0),
    CONSTRAINT ck_producto_stock        CHECK (stock >= 0),
    CONSTRAINT ck_producto_stock_minimo CHECK (stock_minimo >= 0)
) ENGINE=InnoDB;

CREATE INDEX ix_producto_titulo ON producto (titulo);
CREATE INDEX ix_producto_autor  ON producto (autor);

-- ---------------------------------------------------------------------
-- 5. CLIENTE
-- ---------------------------------------------------------------------
CREATE TABLE cliente (
    cliente_id  INT          NOT NULL AUTO_INCREMENT,
    documento   VARCHAR(15)  NULL,                 -- DNI o RUC (UNIQUE si existe)
    nombre      VARCHAR(80)  NOT NULL,
    telefono    VARCHAR(15)  NULL,
    CONSTRAINT pk_cliente           PRIMARY KEY (cliente_id),
    CONSTRAINT uq_cliente_documento UNIQUE (documento)  -- MySQL permite varios NULL
) ENGINE=InnoDB;

CREATE INDEX ix_cliente_nombre ON cliente (nombre);

-- ---------------------------------------------------------------------
-- 6. VENTA
-- ---------------------------------------------------------------------
CREATE TABLE venta (
    venta_id         INT            NOT NULL AUTO_INCREMENT,
    cliente_id       INT            NULL,          -- cliente opcional
    usuario_id       INT            NOT NULL,      -- vendedor
    nro_comprobante  VARCHAR(12)    NOT NULL,      -- formato INT-NNNNNN (RF17)
    fecha            DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    total            DECIMAL(10,2)  NOT NULL DEFAULT 0,
    CONSTRAINT pk_venta                PRIMARY KEY (venta_id),
    CONSTRAINT uq_venta_comprobante    UNIQUE (nro_comprobante),
    CONSTRAINT fk_venta_cliente        FOREIGN KEY (cliente_id)
        REFERENCES cliente (cliente_id)
        ON UPDATE CASCADE ON DELETE SET NULL,
    CONSTRAINT fk_venta_usuario        FOREIGN KEY (usuario_id)
        REFERENCES usuario (usuario_id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT ck_venta_total          CHECK (total >= 0),
    CONSTRAINT ck_venta_comprobante    CHECK (nro_comprobante REGEXP '^INT-[0-9]{6}$')
) ENGINE=InnoDB;

CREATE INDEX ix_venta_fecha ON venta (fecha);

-- ---------------------------------------------------------------------
-- 7. DETALLE_VENTA
-- ---------------------------------------------------------------------
CREATE TABLE detalle_venta (
    detalle_id       INT            NOT NULL AUTO_INCREMENT,
    venta_id         INT            NOT NULL,
    producto_id      INT            NOT NULL,
    cantidad         INT            NOT NULL,
    precio_unitario  DECIMAL(10,2)  NOT NULL,      -- precio al momento de la venta
    CONSTRAINT pk_detalle_venta PRIMARY KEY (detalle_id),
    CONSTRAINT fk_detalle_venta FOREIGN KEY (venta_id)
        REFERENCES venta (venta_id)
        ON UPDATE CASCADE ON DELETE CASCADE,       -- composición: no existe sin su venta
    CONSTRAINT fk_detalle_producto FOREIGN KEY (producto_id)
        REFERENCES producto (producto_id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT ck_detalle_cantidad CHECK (cantidad > 0),
    CONSTRAINT ck_detalle_precio   CHECK (precio_unitario >= 0)
) ENGINE=InnoDB;

CREATE INDEX ix_detalle_venta    ON detalle_venta (venta_id);
CREATE INDEX ix_detalle_producto ON detalle_venta (producto_id);

-- =====================================================================
-- DATOS INICIALES
-- =====================================================================
INSERT INTO rol (nombre) VALUES
    ('Administrador'),
    ('Vendedor'),
    ('Encargado de inventario');

-- IMPORTANTE: los hashes son de EJEMPLO (formato BCrypt) y NO corresponden
-- a ninguna contraseña real. Reemplácelos con hashes generados por su
-- aplicación (BCryptPasswordEncoder en Spring Security) antes de usar el sistema.
INSERT INTO usuario (rol_id, usuario, password_hash, nombre, activo) VALUES
    (1, 'admin',      '$2a$10$REEMPLAZAR.CON.HASH.BCRYPT.GENERADO.POR.LA.APP.0000000000', 'Administrador General', TRUE),
    (2, 'vendedor1',  '$2a$10$REEMPLAZAR.CON.HASH.BCRYPT.GENERADO.POR.LA.APP.0000000000', 'Vendedor Uno',         TRUE),
    (3, 'inventario1','$2a$10$REEMPLAZAR.CON.HASH.BCRYPT.GENERADO.POR.LA.APP.0000000000', 'Encargado Inventario', TRUE);

INSERT INTO categoria (nombre) VALUES
    ('Novela'), ('Texto escolar'), ('Infantil'), ('Útiles de escritorio'), ('Papelería');

INSERT INTO producto (categoria_id, codigo, titulo, autor, precio, stock, stock_minimo) VALUES
    (1, 'LIB-0001', 'Conversación en La Catedral', 'Mario Vargas Llosa', 69.90, 12, 3),
    (1, 'LIB-0002', 'Los ríos profundos',          'José María Arguedas', 39.50,  8, 3),
    (3, 'LIB-0003', 'El principito',               'Antoine de Saint-Exupéry', 29.90, 20, 5),
    (2, 'LIB-0004', 'Matemática 5.º primaria',     'Varios',              45.00, 15, 5),
    (4, 'PAP-0001', 'Cuaderno cuadriculado A4',    NULL,                   6.50, 60, 20),
    (4, 'PAP-0002', 'Lapicero azul',               NULL,                   1.50, 200, 50),
    (5, 'PAP-0003', 'Papel bond A4 (500 hojas)',   NULL,                  22.00, 25, 10);

INSERT INTO cliente (documento, nombre, telefono) VALUES
    ('45871236', 'María Quispe', '987654321'),
    ('20512345678', 'Colegio San Martín S.A.C.', '014567890');

-- =====================================================================
-- VISTAS ÚTILES PARA REPORTES (RF11, RF12, RF05)
-- =====================================================================
CREATE VIEW v_productos_stock_bajo AS
SELECT p.producto_id, p.codigo, p.titulo, p.stock, p.stock_minimo
FROM producto p
WHERE p.activo = TRUE AND p.stock <= p.stock_minimo;

CREATE VIEW v_ventas_detalle AS
SELECT v.venta_id, v.nro_comprobante, v.fecha, c.nombre AS cliente,
       u.nombre AS vendedor, p.codigo, p.titulo, d.cantidad,
       d.precio_unitario, (d.cantidad * d.precio_unitario) AS subtotal, v.total
FROM venta v
JOIN detalle_venta d ON d.venta_id = v.venta_id
JOIN producto p      ON p.producto_id = d.producto_id
JOIN usuario u       ON u.usuario_id = v.usuario_id
LEFT JOIN cliente c  ON c.cliente_id = v.cliente_id;

CREATE VIEW v_productos_mas_vendidos AS
SELECT p.producto_id, p.codigo, p.titulo, SUM(d.cantidad) AS unidades_vendidas,
       SUM(d.cantidad * d.precio_unitario) AS monto
FROM detalle_venta d
JOIN producto p ON p.producto_id = d.producto_id
GROUP BY p.producto_id, p.codigo, p.titulo
ORDER BY unidades_vendidas DESC;

-- =====================================================================
-- PROCEDIMIENTO: registrar una venta de un solo producto (ejemplo de la
-- regla transaccional del informe). Para varios productos, la aplicación
-- repite los pasos 3-5 dentro de la misma transacción.
-- =====================================================================
DELIMITER $$

CREATE PROCEDURE sp_registrar_venta_item(
    IN  p_cliente_id  INT,
    IN  p_usuario_id  INT,
    IN  p_producto_id INT,
    IN  p_cantidad    INT,
    OUT p_venta_id    INT,
    OUT p_comprobante VARCHAR(12)
)
BEGIN
    DECLARE v_precio DECIMAL(10,2);
    DECLARE v_filas  INT;

    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;

    START TRANSACTION;

    -- 1. Verificar existencias y obtener precio (bloquea la fila)
    SELECT precio INTO v_precio
    FROM producto
    WHERE producto_id = p_producto_id AND activo = TRUE AND stock >= p_cantidad
    FOR UPDATE;

    IF v_precio IS NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Stock insuficiente o producto inactivo';
    END IF;

    -- 2. Insertar venta con comprobante correlativo
    SET p_comprobante = CONCAT('INT-', LPAD((SELECT IFNULL(MAX(venta_id),0) + 1 FROM venta), 6, '0'));

    INSERT INTO venta (cliente_id, usuario_id, nro_comprobante, total)
    VALUES (p_cliente_id, p_usuario_id, p_comprobante, v_precio * p_cantidad);
    SET p_venta_id = LAST_INSERT_ID();

    -- 3. Detalle
    INSERT INTO detalle_venta (venta_id, producto_id, cantidad, precio_unitario)
    VALUES (p_venta_id, p_producto_id, p_cantidad, v_precio);

    -- 4. Actualizar stock con condición stock >= cantidad
    UPDATE producto SET stock = stock - p_cantidad
    WHERE producto_id = p_producto_id AND stock >= p_cantidad;
    SET v_filas = ROW_COUNT();

    IF v_filas = 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Stock insuficiente';
    END IF;

    COMMIT;
END$$

DELIMITER ;

-- =====================================================================
-- PRUEBAS RÁPIDAS (descomente para ejecutar)
-- =====================================================================
-- CALL sp_registrar_venta_item(1, 2, 3, 2, @vid, @comp);
-- SELECT @vid, @comp;
-- SELECT * FROM v_ventas_detalle;
-- SELECT * FROM v_productos_stock_bajo;
-- CALL sp_registrar_venta_item(1, 2, 3, 9999, @vid, @comp);   -- debe fallar: stock insuficiente
-- INSERT INTO producto (categoria_id,codigo,titulo,precio,stock) VALUES (1,'X','X',-1,0); -- debe fallar: CHECK
