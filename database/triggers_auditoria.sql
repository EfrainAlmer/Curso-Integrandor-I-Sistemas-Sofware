-- =========================================================
-- TRIGGERS DE AUDITORÍA (RF09)
-- Ejecutar DESPUÉS de BD_GestionAcademica_v3.sql
-- =========================================================
USE BD_GestionAcademica;
GO

-- =========================================================
-- 1. QUIÉN HACE EL CAMBIO: SESSION_CONTEXT
-- =========================================================
-- Un trigger no sabe qué usuario de la aplicación hizo el cambio (la
-- conexión Java usa una sola cuenta de SQL Server). Por eso la aplicación
-- debe registrar el UsuarioId en la sesión ANTES de modificar Nota o
-- Asistencia. Si no lo hace, los triggers cancelan la operación.
CREATE PROCEDURE sp_EstablecerUsuarioAuditoria
    @UsuarioId INT
AS
BEGIN
    SET NOCOUNT ON;
    EXEC sp_set_session_context @key = N'UsuarioId', @value = @UsuarioId;
END;
GO

-- =========================================================
-- 2. TRIGGER SOBRE NOTA
-- =========================================================
-- Un solo trigger para INSERT, UPDATE y DELETE (FULL OUTER JOIN entre
-- inserted y deleted):
--   * Solo en inserted  -> INSERT : ValorAnterior = NULL
--   * En ambos          -> UPDATE : se audita solo si cambió el Puntaje
--   * Solo en deleted   -> DELETE : ValorNuevo = 'ELIMINADO'
CREATE TRIGGER trg_Nota_Auditoria
ON Nota
AFTER INSERT, UPDATE, DELETE
AS
BEGIN
    SET NOCOUNT ON;

    IF NOT EXISTS (SELECT 1 FROM inserted) AND NOT EXISTS (SELECT 1 FROM deleted)
        RETURN;

    DECLARE @UsuarioId INT = TRY_CAST(SESSION_CONTEXT(N'UsuarioId') AS INT);

    IF @UsuarioId IS NULL
    BEGIN
        IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
        THROW 50010, 'Auditoria: falta el usuario de la sesion. Ejecute sp_EstablecerUsuarioAuditoria antes de modificar Nota.', 1;
    END;

    INSERT INTO Auditoria (Tabla, ClavePrimaria, UsuarioId, ValorAnterior, ValorNuevo)
    SELECT
        'Nota',
        'AlumnoId=' + CAST(COALESCE(i.AlumnoId, d.AlumnoId) AS VARCHAR(10))
            + ';EvaluacionId=' + CAST(COALESCE(i.EvaluacionId, d.EvaluacionId) AS VARCHAR(10)),
        @UsuarioId,
        CASE WHEN d.AlumnoId IS NULL THEN NULL ELSE CAST(d.Puntaje AS VARCHAR(20)) END,
        CASE WHEN i.AlumnoId IS NULL THEN 'ELIMINADO' ELSE CAST(i.Puntaje AS VARCHAR(20)) END
    FROM inserted i
    FULL OUTER JOIN deleted d
        ON  i.AlumnoId = d.AlumnoId
        AND i.EvaluacionId = d.EvaluacionId
    WHERE d.AlumnoId IS NULL
       OR i.AlumnoId IS NULL
       OR i.Puntaje <> d.Puntaje;
END;
GO

-- =========================================================
-- 3. TRIGGER SOBRE ASISTENCIA
-- =========================================================
-- Misma lógica. Se audita el cambio de Estado (A/F/T/J); un cambio
-- solo en Observacion no genera registro.
CREATE TRIGGER trg_Asistencia_Auditoria
ON Asistencia
AFTER INSERT, UPDATE, DELETE
AS
BEGIN
    SET NOCOUNT ON;

    IF NOT EXISTS (SELECT 1 FROM inserted) AND NOT EXISTS (SELECT 1 FROM deleted)
        RETURN;

    DECLARE @UsuarioId INT = TRY_CAST(SESSION_CONTEXT(N'UsuarioId') AS INT);

    IF @UsuarioId IS NULL
    BEGIN
        IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
        THROW 50010, 'Auditoria: falta el usuario de la sesion. Ejecute sp_EstablecerUsuarioAuditoria antes de modificar Asistencia.', 1;
    END;

    INSERT INTO Auditoria (Tabla, ClavePrimaria, UsuarioId, ValorAnterior, ValorNuevo)
    SELECT
        'Asistencia',
        'AlumnoId=' + CAST(COALESCE(i.AlumnoId, d.AlumnoId) AS VARCHAR(10))
            + ';CursoSeccionId=' + CAST(COALESCE(i.CursoSeccionId, d.CursoSeccionId) AS VARCHAR(10))
            + ';Fecha=' + CONVERT(VARCHAR(10), COALESCE(i.Fecha, d.Fecha), 23),
        @UsuarioId,
        CASE WHEN d.AlumnoId IS NULL THEN NULL ELSE d.Estado END,
        CASE WHEN i.AlumnoId IS NULL THEN 'ELIMINADO' ELSE i.Estado END
    FROM inserted i
    FULL OUTER JOIN deleted d
        ON  i.AlumnoId = d.AlumnoId
        AND i.CursoSeccionId = d.CursoSeccionId
        AND i.Fecha = d.Fecha
    WHERE d.AlumnoId IS NULL
       OR i.AlumnoId IS NULL
       OR i.Estado <> d.Estado;
END;
GO

-- =========================================================
-- 4. AUDITORIA DE SOLO INSERCIÓN
-- =========================================================
-- Evita que alguien edite o borre el historial de auditoría.
CREATE TRIGGER trg_Auditoria_SoloInsercion
ON Auditoria
INSTEAD OF UPDATE, DELETE
AS
BEGIN
    SET NOCOUNT ON;
    THROW 50011, 'La tabla Auditoria es de solo insercion: no se permite modificar ni eliminar registros.', 1;
END;
GO

-- =========================================================
-- 5. PRUEBAS (no dejan datos: todo se revierte con ROLLBACK)
-- =========================================================
-- Prueba A: con usuario en sesión (3 = jgarcia) se generan registros.
BEGIN TRANSACTION;

    EXEC sp_EstablecerUsuarioAuditoria @UsuarioId = 3;

    INSERT INTO Evaluacion (PeriodoId, CursoSeccionId, NumEval) VALUES (1, 1, 1);
    DECLARE @EvalId INT = SCOPE_IDENTITY();

    INSERT INTO Nota (AlumnoId, EvaluacionId, Puntaje) VALUES (1, @EvalId, 14.50);
    UPDATE Nota SET Puntaje = 16.00 WHERE AlumnoId = 1 AND EvaluacionId = @EvalId;
    DELETE FROM Nota WHERE AlumnoId = 1 AND EvaluacionId = @EvalId;

    INSERT INTO Asistencia (AlumnoId, CursoSeccionId, Fecha, Estado) VALUES (1, 1, '2026-03-20', 'F');
    UPDATE Asistencia SET Estado = 'J', Observacion = 'Certificado medico'
    WHERE AlumnoId = 1 AND CursoSeccionId = 1 AND Fecha = '2026-03-20';

    -- Debe mostrar 5 filas
    SELECT AuditoriaId, Tabla, ClavePrimaria, UsuarioId, ValorAnterior, ValorNuevo, FechaCambio
    FROM Auditoria
    ORDER BY AuditoriaId;

ROLLBACK TRANSACTION;
GO

-- Prueba B: sin usuario en sesión, el trigger debe rechazar la operación.
EXEC sp_set_session_context @key = N'UsuarioId', @value = NULL;

BEGIN TRY
    INSERT INTO Asistencia (AlumnoId, CursoSeccionId, Fecha, Estado) VALUES (1, 1, '2026-03-21', 'A');
    PRINT 'ERROR: la insercion no debio permitirse.';
END TRY
BEGIN CATCH
    PRINT 'Correcto, se rechazo la operacion: ' + ERROR_MESSAGE();
END CATCH;
GO
