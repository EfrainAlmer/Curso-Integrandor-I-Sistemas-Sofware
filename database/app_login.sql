USE master;
GO
IF NOT EXISTS (SELECT 1 FROM sys.server_principals WHERE name = 'app_login')
    CREATE LOGIN app_login WITH PASSWORD = 'UnaClaveSegura123!';
GO

USE BD_GestionAcademica;
GO
IF NOT EXISTS (SELECT 1 FROM sys.database_principals WHERE name = 'app_login')
    CREATE USER app_login FOR LOGIN app_login;
GO
ALTER ROLE db_datareader ADD MEMBER app_login;
ALTER ROLE db_datawriter ADD MEMBER app_login;

GRANT EXECUTE ON sp_ObtenerUsuarioParaLogin TO app_login;
GRANT EXECUTE ON sp_RegistrarIntentoFallido TO app_login;
GRANT EXECUTE ON sp_RegistrarLoginExitoso TO app_login;
GRANT EXECUTE ON sp_EstablecerUsuarioAuditoria TO app_login;
GRANT EXECUTE ON sp_ActualizarReporteRendimiento TO app_login;
GO