-- =====================================================================
--  Sistema de Gestión de Librería "Pluma y Papel"
--  Script de base de datos para MySQL Workbench (MySQL 8.0.16 o superior)
--  Basado en el Modelo entidad-relación (2.4.2) y el Diccionario de datos
--  (2.5, Tablas 30 a 40) del informe.
--  11 tablas: ROL, USUARIO, CATEGORIA, PRODUCTO, CLIENTE, VENTA,
--  DETALLE_VENTA, PROVEEDOR, ORDEN_COMPRA, DETALLE_ORDEN_COMPRA,
--  MOVIMIENTO_INVENTARIO.
--  Nota: las restricciones CHECK solo se aplican desde MySQL 8.0.16.
-- =====================================================================

DROP DATABASE IF EXISTS libreria_db;
CREATE DATABASE libreria_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;
USE libreria_db;

-- ---------------------------------------------------------------------
-- 1. ROL
-- ---------------------------------------------------------------------
CREATE TABLE rol (
    rol_id  INT         NOT NULL AUTO_INCREMENT,
    nombre  VARCHAR(30) NOT NULL,
    CONSTRAINT pk_rol        PRIMARY KEY (rol_id),
    CONSTRAINT uq_rol_nombre UNIQUE (nombre)
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
-- 2. USUARIO
-- ---------------------------------------------------------------------
CREATE TABLE usuario (
    usuario_id     INT          NOT NULL AUTO_INCREMENT,
    rol_id         INT          NOT NULL,
    usuario        VARCHAR(30)  NOT NULL,
    password_hash  VARCHAR(100) NOT NULL,          -- RNF02: nunca texto plano
    nombre         VARCHAR(80)  NOT NULL,
    activo         BOOLEAN      NOT NULL DEFAULT TRUE,   -- RF15
    ultimo_acceso  DATETIME     NULL,
    CONSTRAINT pk_usuario         PRIMARY KEY (usuario_id),
    CONSTRAINT uq_usuario_usuario UNIQUE (usuario),
    CONSTRAINT fk_usuario_rol     FOREIGN KEY (rol_id) REFERENCES rol (rol_id)
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
-- 3. CATEGORIA
-- ---------------------------------------------------------------------
CREATE TABLE categoria (
    categoria_id  INT          NOT NULL AUTO_INCREMENT,
    codigo        VARCHAR(10)  NOT NULL,           -- p. ej. CAT-001
    nombre        VARCHAR(50)  NOT NULL,           -- RF16
    descripcion   VARCHAR(150) NULL,
    activo        BOOLEAN      NOT NULL DEFAULT TRUE,
    CONSTRAINT pk_categoria        PRIMARY KEY (categoria_id),
    CONSTRAINT uq_categoria_codigo UNIQUE (codigo),
    CONSTRAINT uq_categoria_nombre UNIQUE (nombre)
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
-- 4. PRODUCTO
-- ---------------------------------------------------------------------
CREATE TABLE producto (
    producto_id   INT           NOT NULL AUTO_INCREMENT,
    categoria_id  INT           NOT NULL,
    codigo        VARCHAR(20)   NOT NULL,
    titulo        VARCHAR(120)  NOT NULL,
    autor         VARCHAR(80)   NULL,              -- RF03: búsqueda por autor
    precio        DECIMAL(10,2) NOT NULL,          -- soles
    stock         INT           NOT NULL DEFAULT 0,
    stock_minimo  INT           NOT NULL DEFAULT 0, -- RF05: alerta de reposición
    activo        BOOLEAN       NOT NULL DEFAULT TRUE,
    CONSTRAINT pk_producto        PRIMARY KEY (producto_id),
    CONSTRAINT uq_producto_codigo UNIQUE (codigo),
    CONSTRAINT fk_producto_categoria FOREIGN KEY (categoria_id)
        REFERENCES categoria (categoria_id),
    CONSTRAINT ck_producto_precio       CHECK (precio > 0),
    CONSTRAINT ck_producto_stock        CHECK (stock >= 0),
    CONSTRAINT ck_producto_stock_minimo CHECK (stock_minimo >= 0)
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
-- 5. CLIENTE
-- ---------------------------------------------------------------------
CREATE TABLE cliente (
    cliente_id  INT          NOT NULL AUTO_INCREMENT,
    documento   VARCHAR(15)  NOT NULL,             -- DNI o RUC
    nombre      VARCHAR(80)  NOT NULL,
    telefono    VARCHAR(15)  NULL,
    email       VARCHAR(100) NULL,
    activo      BOOLEAN      NOT NULL DEFAULT TRUE,
    CONSTRAINT pk_cliente           PRIMARY KEY (cliente_id),
    CONSTRAINT uq_cliente_documento UNIQUE (documento)
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
-- 6. VENTA
-- ---------------------------------------------------------------------
CREATE TABLE venta (
    venta_id         INT           NOT NULL AUTO_INCREMENT,
    cliente_id       INT           NULL,           -- cliente opcional
    usuario_id       INT           NOT NULL,       -- vendedor que registra
    nro_comprobante  VARCHAR(12)   NOT NULL,       -- p. ej. INT-004127
    fecha            DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    total            DECIMAL(10,2) NOT NULL DEFAULT 0,   -- RF08
    estado           VARCHAR(15)   NOT NULL DEFAULT 'CONFIRMADA',
    CONSTRAINT pk_venta                PRIMARY KEY (venta_id),
    CONSTRAINT uq_venta_comprobante    UNIQUE (nro_comprobante),
    CONSTRAINT fk_venta_cliente        FOREIGN KEY (cliente_id)
        REFERENCES cliente (cliente_id),
    CONSTRAINT fk_venta_usuario        FOREIGN KEY (usuario_id)
        REFERENCES usuario (usuario_id),
    CONSTRAINT ck_venta_total          CHECK (total >= 0),
    CONSTRAINT ck_venta_estado         CHECK (estado IN ('CONFIRMADA'))
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
-- 7. DETALLE_VENTA
-- ---------------------------------------------------------------------
CREATE TABLE detalle_venta (
    detalle_id       INT           NOT NULL AUTO_INCREMENT,
    venta_id         INT           NOT NULL,
    producto_id      INT           NOT NULL,
    cantidad         INT           NOT NULL,
    precio_unitario  DECIMAL(10,2) NOT NULL,       -- precio al momento de la venta
    CONSTRAINT pk_detalle_venta          PRIMARY KEY (detalle_id),
    CONSTRAINT fk_detalle_venta_venta    FOREIGN KEY (venta_id)
        REFERENCES venta (venta_id),
    CONSTRAINT fk_detalle_venta_producto FOREIGN KEY (producto_id)
        REFERENCES producto (producto_id),
    CONSTRAINT ck_detalle_venta_cantidad CHECK (cantidad > 0),
    CONSTRAINT ck_detalle_venta_precio   CHECK (precio_unitario > 0)
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
-- 8. PROVEEDOR
-- ---------------------------------------------------------------------
CREATE TABLE proveedor (
    proveedor_id  INT          NOT NULL AUTO_INCREMENT,
    ruc           CHAR(11)     NOT NULL,
    razon_social  VARCHAR(150) NOT NULL,
    telefono      VARCHAR(20)  NULL,
    correo        VARCHAR(120) NULL,
    direccion     VARCHAR(200) NULL,
    activo        BOOLEAN      NOT NULL DEFAULT TRUE,
    CONSTRAINT pk_proveedor     PRIMARY KEY (proveedor_id),
    CONSTRAINT uq_proveedor_ruc UNIQUE (ruc),
    CONSTRAINT ck_proveedor_ruc CHECK (ruc REGEXP '^[0-9]{11}$')
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
-- 9. ORDEN_COMPRA
-- ---------------------------------------------------------------------
CREATE TABLE orden_compra (
    orden_id                INT           NOT NULL AUTO_INCREMENT,
    numero                  VARCHAR(30)   NOT NULL,
    proveedor_id            INT           NOT NULL,
    usuario_id              INT           NOT NULL,
    fecha_emision           DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_entrega_estimada  DATE          NULL,
    estado                  VARCHAR(12)   NOT NULL DEFAULT 'PENDIENTE',
    total                   DECIMAL(12,2) NOT NULL DEFAULT 0,
    fecha_recepcion         DATETIME      NULL,
    CONSTRAINT pk_orden_compra           PRIMARY KEY (orden_id),
    CONSTRAINT uq_orden_compra_numero    UNIQUE (numero),
    CONSTRAINT fk_orden_compra_proveedor FOREIGN KEY (proveedor_id)
        REFERENCES proveedor (proveedor_id),
    CONSTRAINT fk_orden_compra_usuario   FOREIGN KEY (usuario_id)
        REFERENCES usuario (usuario_id),
    CONSTRAINT ck_orden_compra_estado    CHECK (estado IN ('PENDIENTE','RECIBIDA')),
    CONSTRAINT ck_orden_compra_total     CHECK (total >= 0)
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
-- 10. DETALLE_ORDEN_COMPRA
-- ---------------------------------------------------------------------
CREATE TABLE detalle_orden_compra (
    detalle_orden_id  INT           NOT NULL AUTO_INCREMENT,
    orden_id          INT           NOT NULL,
    producto_id       INT           NOT NULL,
    cantidad          INT           NOT NULL,
    cantidad_recibida INT           NOT NULL DEFAULT 0,
    costo_unitario    DECIMAL(12,2) NOT NULL,
    subtotal          DECIMAL(12,2) GENERATED ALWAYS AS (cantidad * costo_unitario) STORED,
    CONSTRAINT pk_detalle_orden_compra   PRIMARY KEY (detalle_orden_id),
    CONSTRAINT fk_doc_orden              FOREIGN KEY (orden_id)
        REFERENCES orden_compra (orden_id),
    CONSTRAINT fk_doc_producto           FOREIGN KEY (producto_id)
        REFERENCES producto (producto_id),
    CONSTRAINT ck_doc_cantidad           CHECK (cantidad > 0),
    CONSTRAINT ck_doc_cantidad_recibida  CHECK (cantidad_recibida >= 0
                                                AND cantidad_recibida <= cantidad),
    CONSTRAINT ck_doc_costo              CHECK (costo_unitario >= 0)
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
-- 11. MOVIMIENTO_INVENTARIO
-- ---------------------------------------------------------------------
CREATE TABLE movimiento_inventario (
    movimiento_id  INT          NOT NULL AUTO_INCREMENT,
    producto_id    INT          NOT NULL,
    venta_id       INT          NULL,              -- salida originada por venta
    orden_id       INT          NULL,              -- entrada por recepción de compra
    usuario_id     INT          NOT NULL,
    tipo           VARCHAR(7)   NOT NULL,
    cantidad       INT          NOT NULL,
    motivo         VARCHAR(100) NULL,
    fecha          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_movimiento          PRIMARY KEY (movimiento_id),
    CONSTRAINT fk_mov_producto        FOREIGN KEY (producto_id)
        REFERENCES producto (producto_id),
    CONSTRAINT fk_mov_venta           FOREIGN KEY (venta_id)
        REFERENCES venta (venta_id),
    CONSTRAINT fk_mov_orden           FOREIGN KEY (orden_id)
        REFERENCES orden_compra (orden_id),
    CONSTRAINT fk_mov_usuario         FOREIGN KEY (usuario_id)
        REFERENCES usuario (usuario_id),
    CONSTRAINT ck_mov_tipo            CHECK (tipo IN ('ENTRADA','SALIDA')),
    CONSTRAINT ck_mov_cantidad        CHECK (cantidad > 0)
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
-- ÍNDICES sobre campos de búsqueda (RNF01)
-- (código de producto, cliente.documento y los UNIQUE ya generan índice)
-- ---------------------------------------------------------------------
CREATE INDEX idx_producto_titulo   ON producto (titulo);
CREATE INDEX idx_producto_autor    ON producto (autor);
CREATE INDEX idx_producto_categoria_activo ON producto (categoria_id, activo);
CREATE INDEX idx_cliente_nombre    ON cliente  (nombre);
CREATE INDEX idx_venta_fecha       ON venta    (fecha);
CREATE INDEX idx_venta_cliente     ON venta    (cliente_id);
CREATE INDEX idx_mov_producto_fecha ON movimiento_inventario (producto_id, fecha);
CREATE INDEX idx_mov_fecha         ON movimiento_inventario (fecha);
CREATE INDEX idx_orden_estado      ON orden_compra (estado, fecha_emision);

-- ---------------------------------------------------------------------
-- VISTAS de apoyo (reportes y alertas)
-- ---------------------------------------------------------------------
-- RF05: productos con stock por debajo o igual al mínimo
CREATE OR REPLACE VIEW vw_stock_bajo AS
SELECT p.producto_id, p.codigo, p.titulo, c.nombre AS categoria,
       p.stock, p.stock_minimo
FROM producto p
JOIN categoria c ON c.categoria_id = p.categoria_id
WHERE p.activo = TRUE AND p.stock <= p.stock_minimo;

-- RF11/RF12: ventas con vendedor y cliente
CREATE OR REPLACE VIEW vw_ventas_detalle AS
SELECT v.venta_id, v.nro_comprobante, v.fecha, v.total, v.estado,
       u.nombre AS vendedor,
       COALESCE(cl.nombre, 'Cliente general') AS cliente,
       p.codigo, p.titulo, d.cantidad, d.precio_unitario,
       (d.cantidad * d.precio_unitario) AS subtotal
FROM venta v
JOIN usuario u          ON u.usuario_id = v.usuario_id
LEFT JOIN cliente cl    ON cl.cliente_id = v.cliente_id
JOIN detalle_venta d    ON d.venta_id = v.venta_id
JOIN producto p         ON p.producto_id = d.producto_id;

-- ---------------------------------------------------------------------
-- DATOS INICIALES
-- ---------------------------------------------------------------------
INSERT INTO rol (nombre) VALUES
    ('Administrador'), ('Vendedor'), ('Encargado de inventario');

-- IMPORTANTE: reemplazar password_hash por hashes reales (p. ej. BCrypt
-- generado desde Spring Security) antes de usar el sistema.
INSERT INTO usuario (rol_id, usuario, password_hash, nombre) VALUES
    (1, 'admin',       '$2a$10$REEMPLAZAR_CON_HASH_BCRYPT_ADMIN',       'Administrador de la librería'),
    (2, 'vendedor1',   '$2a$10$REEMPLAZAR_CON_HASH_BCRYPT_VENDEDOR',    'Vendedor de mostrador'),
    (3, 'inventario1', '$2a$10$REEMPLAZAR_CON_HASH_BCRYPT_INVENTARIO',  'Encargado de inventario');

INSERT INTO categoria (codigo, nombre, descripcion) VALUES
    ('CAT-001', 'Literatura',  'Novelas, cuentos y poesía'),
    ('CAT-002', 'Textos escolares', 'Libros de estudio para colegio'),
    ('CAT-003', 'Papelería',   'Cuadernos, lapiceros y útiles'),
    ('CAT-004', 'Infantil',    'Libros ilustrados y cuentos infantiles');

INSERT INTO producto (categoria_id, codigo, titulo, autor, precio, stock, stock_minimo) VALUES
    (1, 'LIB-0001', 'Cien años de soledad',        'Gabriel García Márquez', 59.90, 12, 5),
    (1, 'LIB-0002', 'La ciudad y los perros',      'Mario Vargas Llosa',     49.90,  8, 4),
    (2, 'LIB-0003', 'Matemática 5.º de secundaria', NULL,                    45.00, 20, 10),
    (3, 'PAP-0001', 'Cuaderno cuadriculado A4',    NULL,                      6.50, 60, 20),
    (3, 'PAP-0002', 'Lapicero azul (caja x12)',    NULL,                     15.00,  3, 5),
    (4, 'LIB-0004', 'El principito',               'Antoine de Saint-Exupéry', 29.90, 10, 4);

INSERT INTO cliente (documento, nombre, telefono, email) VALUES
    ('45879632',    'María Fernández Rojas',       '987654321', 'maria.fernandez@example.com'),
    ('20512345678', 'Colegio San Martín S.A.C.',   '014567890', 'compras@sanmartin.example.com');

INSERT INTO proveedor (ruc, razon_social, telefono, correo, direccion) VALUES
    ('20123456789', 'Distribuidora de Libros Andina S.A.C.', '012345678', 'ventas@andina.example.com', 'Av. Arequipa 1234, Lima'),
    ('20987654321', 'Papelera Central E.I.R.L.',             '017654321', 'pedidos@papelera.example.com', 'Jr. Lampa 456, Lima');

-- Venta de ejemplo (confirmada) con su detalle y movimiento de salida
INSERT INTO venta (cliente_id, usuario_id, nro_comprobante, total) VALUES
    (1, 2, 'INT-000001', 66.40);
INSERT INTO detalle_venta (venta_id, producto_id, cantidad, precio_unitario) VALUES
    (1, 1, 1, 59.90),
    (1, 4, 1,  6.50);
UPDATE producto SET stock = stock - 1 WHERE producto_id IN (1, 4);
INSERT INTO movimiento_inventario (producto_id, venta_id, usuario_id, tipo, cantidad, motivo) VALUES
    (1, 1, 2, 'SALIDA', 1, 'Venta INT-000001'),
    (4, 1, 2, 'SALIDA', 1, 'Venta INT-000001');

-- Orden de compra pendiente de ejemplo
INSERT INTO orden_compra (numero, proveedor_id, usuario_id, fecha_entrega_estimada, estado, total) VALUES
    ('OC-000001', 2, 3, DATE_ADD(CURDATE(), INTERVAL 7 DAY), 'PENDIENTE', 300.00);
INSERT INTO detalle_orden_compra (orden_id, producto_id, cantidad, costo_unitario) VALUES
    (1, 5, 40, 7.50);

-- ---------------------------------------------------------------------
-- PROCEDIMIENTO: registrar venta de una línea con transacción (Regla
-- transaccional del informe: verificar stock, insertar venta y detalle,
-- descontar stock con condición stock >= cantidad, registrar movimiento;
-- ante cualquier fallo, revertir).
-- Para ventas con varias líneas, la aplicación Java repite los pasos
-- 3-5 por cada producto dentro de la misma transacción.
-- ---------------------------------------------------------------------
DELIMITER $$
CREATE PROCEDURE sp_registrar_venta_item(
    IN  p_cliente_id  INT,
    IN  p_usuario_id  INT,
    IN  p_producto_id INT,
    IN  p_cantidad    INT,
    OUT p_venta_id    INT
)
BEGIN
    DECLARE v_precio DECIMAL(10,2);
    DECLARE v_nro    VARCHAR(12);
    DECLARE v_filas  INT;

    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;

    START TRANSACTION;

    SELECT precio INTO v_precio
    FROM producto
    WHERE producto_id = p_producto_id AND activo = TRUE
    FOR UPDATE;

    IF v_precio IS NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Producto inexistente o inactivo';
    END IF;

    UPDATE producto
       SET stock = stock - p_cantidad
     WHERE producto_id = p_producto_id AND stock >= p_cantidad;
    SET v_filas = ROW_COUNT();

    IF v_filas = 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Stock insuficiente';
    END IF;

    SELECT CONCAT('INT-', LPAD(COALESCE(MAX(venta_id), 0) + 1, 6, '0'))
      INTO v_nro FROM venta;

    INSERT INTO venta (cliente_id, usuario_id, nro_comprobante, total)
    VALUES (p_cliente_id, p_usuario_id, v_nro, v_precio * p_cantidad);
    SET p_venta_id = LAST_INSERT_ID();

    INSERT INTO detalle_venta (venta_id, producto_id, cantidad, precio_unitario)
    VALUES (p_venta_id, p_producto_id, p_cantidad, v_precio);

    INSERT INTO movimiento_inventario (producto_id, venta_id, usuario_id, tipo, cantidad, motivo)
    VALUES (p_producto_id, p_venta_id, p_usuario_id, 'SALIDA', p_cantidad, CONCAT('Venta ', v_nro));

    COMMIT;
END$$
DELIMITER ;

-- ---------------------------------------------------------------------
-- Verificación rápida
-- ---------------------------------------------------------------------
-- SHOW TABLES;
-- SELECT * FROM vw_stock_bajo;
-- CALL sp_registrar_venta_item(1, 2, 3, 2, @id); SELECT @id;
