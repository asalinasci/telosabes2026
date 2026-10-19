-- ============================================================
--  Creacion de la base de datos para telosabes2026
--  Ejecutar como root una sola vez:
--
--    mariadb -u root -p -e "source db/crear_bd.sql"
--
--  Si quieres otro usuario o contraseña, cambia las lineas
--  marcadas abajo y ajusta tambien application.properties.
-- ============================================================

CREATE DATABASE IF NOT EXISTS telosabes2026
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

-- Usuario de la aplicacion (no uses root para la app)
CREATE USER IF NOT EXISTS 'telosabes2026'@'localhost' IDENTIFIED BY 'telosabes2026';
ALTER USER 'telosabes2026'@'localhost' IDENTIFIED BY 'telosabes2026';

GRANT ALL PRIVILEGES ON telosabes2026.* TO 'telosabes2026'@'localhost';

FLUSH PRIVILEGES;

-- Comprobacion
SHOW DATABASES LIKE 'telosabes2026';
SELECT user, host FROM mysql.user WHERE user = 'telosabes2026';
