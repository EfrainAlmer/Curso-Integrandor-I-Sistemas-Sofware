
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
GO