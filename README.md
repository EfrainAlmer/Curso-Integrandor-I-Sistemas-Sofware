# Curso-Integrandor-I-Sistemas-Sofware
CURSO INTEGRADOR

Orden de Ejecución de base de datos:

bd_gestion_academica.sql
triggers_auditoria.sql
app_login

Razón: ese script de app_login da permisos (GRANT EXECUTE) sobre sp_EstablecerUsuarioAuditoria, que recién se crea en Triggers_Auditoria.sql. Si lo corres antes, el GRANT falla porque el procedimiento todavía no existe.
