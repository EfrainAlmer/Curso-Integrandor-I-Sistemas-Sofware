/* =====================================================================
   Login de la aplicación (mínimo privilegio). Ejecutar después de 01.
   Cambie CAMBIAR_CLAVE por una clave segura y escríbala en config.properties.
   ===================================================================== */
USE master;
GO
IF NOT EXISTS (SELECT 1 FROM sys.server_principals WHERE name = N'sga_app')
    CREATE LOGIN sga_app WITH PASSWORD = N'CAMBIAR_CLAVE', CHECK_POLICY = ON;
GO
USE BD_GestionAcademica_Pitagoras;
GO
IF NOT EXISTS (SELECT 1 FROM sys.database_principals WHERE name = N'sga_app')
    CREATE USER sga_app FOR LOGIN sga_app;
GO
ALTER ROLE db_datareader ADD MEMBER sga_app;
ALTER ROLE db_datawriter ADD MEMBER sga_app;
-- Copias de seguridad desde el módulo "Auditoría y respaldo" (RF10)
ALTER ROLE db_backupoperator ADD MEMBER sga_app;
GO
-- Restaurar una copia reemplaza la base completa: requiere el rol de servidor dbcreator.
-- Actívelo solo en el equipo de Dirección si van a restaurar desde la aplicación.
-- USE master; ALTER SERVER ROLE dbcreator ADD MEMBER sga_app;
GO
