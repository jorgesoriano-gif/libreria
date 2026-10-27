-- Mínimo privilegio (RNF02): usuario de MySQL propio de la aplicación, sin usar root.
-- Ejecutar como administrador de MySQL DESPUÉS de db/libreria_db.sql.
-- Cambie <CLAVE_SEGURA> y publíquela solo en la variable de entorno DB_PASSWORD.
CREATE USER IF NOT EXISTS 'libreria_app'@'localhost' IDENTIFIED BY '<CLAVE_SEGURA>';
GRANT SELECT, INSERT, UPDATE, DELETE ON libreria_db.* TO 'libreria_app'@'localhost';
-- ddl-auto=update puede requerir ALTER/CREATE la primera vez; quítelos luego:
GRANT CREATE, ALTER, INDEX, REFERENCES ON libreria_db.* TO 'libreria_app'@'localhost';
FLUSH PRIVILEGES;
