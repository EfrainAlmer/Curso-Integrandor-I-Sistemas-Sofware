USE master;
GO
IF EXISTS (SELECT * FROM sys.databases WHERE name = 'BD_GestionAcademica')
BEGIN
    ALTER DATABASE BD_GestionAcademica SET SINGLE_USER WITH ROLLBACK IMMEDIATE;
    DROP DATABASE BD_GestionAcademica;
END
GO
CREATE DATABASE BD_GestionAcademica;
GO
USE BD_GestionAcademica;
GO

-- =========================================================
-- 1. SEGURIDAD, ROLES Y USUARIOS
-- =========================================================
CREATE TABLE Rol (
    RolId INT IDENTITY(1, 1) PRIMARY KEY,
    Nombre VARCHAR(30) NOT NULL UNIQUE
);

CREATE TABLE Usuario (
    UsuarioId INT IDENTITY(1, 1) PRIMARY KEY,
    Username VARCHAR(50) NOT NULL UNIQUE,
    -- VARCHAR(100), no CHAR: un hash BCrypt no mide longitud fija (varía según
    -- el costo del algoritmo); la sal ya viene embebida dentro del propio hash.
    PasswordHash VARCHAR(100) NOT NULL,
    RolId INT NOT NULL,
    Estado BIT NOT NULL DEFAULT 1,
    DebeCambiarPassword BIT NOT NULL DEFAULT 1,     -- RF02: obliga cambio en el primer ingreso
    IntentosFallidos INT NOT NULL DEFAULT 0,        -- RF02: contador de intentos fallidos
    BloqueadoHasta DATETIME NULL,                   -- RF02: NULL = no bloqueado
    CreadoEl DATETIME NOT NULL DEFAULT GETDATE(),
    CONSTRAINT FK_Usuario_Rol FOREIGN KEY (RolId) REFERENCES Rol(RolId)
);

-- =========================================================
-- 2. TABLAS MAESTRAS Y PERSONAL DOCENTE
-- =========================================================
CREATE TABLE Turno (
    TurnoId INT IDENTITY(1, 1) PRIMARY KEY,
    Nombre VARCHAR(20) NOT NULL UNIQUE
);

CREATE TABLE Grado (
    GradoId INT IDENTITY(1, 1) PRIMARY KEY,
    Nombre VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE Profesor (
    ProfesorId INT IDENTITY(1, 1) PRIMARY KEY,
    UsuarioId INT NULL,
    DNI CHAR(8) NOT NULL UNIQUE,
    Nombre VARCHAR(50) NOT NULL,
    Apellido VARCHAR(50) NOT NULL,
    Email VARCHAR(100) NULL,
    CONSTRAINT FK_Profesor_Usuario FOREIGN KEY (UsuarioId) REFERENCES Usuario(UsuarioId)
);

CREATE UNIQUE INDEX UQ_Profesor_UsuarioId ON Profesor(UsuarioId) WHERE UsuarioId IS NOT NULL;

CREATE TABLE Curso (
    CursoId INT IDENTITY(1, 1) PRIMARY KEY,
    Nombre VARCHAR(100) NOT NULL UNIQUE
);

-- Periodo: único lugar donde se define el año lectivo y el bimestre.
-- FechaInicio/FechaFin permiten filtrar asistencia por bimestre.
CREATE TABLE Periodo (
    PeriodoId INT IDENTITY(1, 1) PRIMARY KEY,
    AnioLectivo INT NOT NULL,
    BimestreNum INT NOT NULL,
    FechaInicio DATE NOT NULL,
    FechaFin DATE NOT NULL,
    Cerrado BIT NOT NULL DEFAULT 0,                 -- RF07: bloquea edición de notas/asistencia
    FechaCierre DATETIME NULL,
    CerradoPorUsuarioId INT NULL,                   -- RF07: deja registro de quién cerró/reabrió
    CONSTRAINT CK_Bimestre CHECK (BimestreNum BETWEEN 1 AND 4),
    CONSTRAINT CK_Periodo_Fechas CHECK (FechaFin > FechaInicio),
    CONSTRAINT UQ_Periodo_Anio UNIQUE (AnioLectivo, BimestreNum),
    CONSTRAINT FK_Periodo_Usuario FOREIGN KEY (CerradoPorUsuarioId) REFERENCES Usuario(UsuarioId)
);

-- =========================================================
-- 3. ESTRUCTURA ESCOLAR Y ESTUDIANTES
-- =========================================================
CREATE TABLE Seccion (
    SeccionId INT IDENTITY(1, 1) PRIMARY KEY,
    GradoId INT NOT NULL,
    TurnoId INT NOT NULL,
    Nombre CHAR(1) NOT NULL,
    CONSTRAINT FK_Seccion_Grado FOREIGN KEY (GradoId) REFERENCES Grado(GradoId),
    CONSTRAINT FK_Seccion_Turno FOREIGN KEY (TurnoId) REFERENCES Turno(TurnoId),
    CONSTRAINT UQ_Seccion_Grado_Nombre UNIQUE (GradoId, Nombre)
);

CREATE TABLE Alumno (
    AlumnoId INT IDENTITY(1, 1) PRIMARY KEY,
    SeccionId INT NOT NULL,
    UsuarioId INT NULL,
    DNI CHAR(8) NOT NULL UNIQUE,
    Nombre VARCHAR(50) NOT NULL,
    Apellido VARCHAR(50) NOT NULL,
    NombreApoderado VARCHAR(100) NULL,
    TelefonoApoderado VARCHAR(20) NULL,
    Estado VARCHAR(20) NOT NULL DEFAULT 'Activo',
    CONSTRAINT FK_Alumno_Seccion FOREIGN KEY (SeccionId) REFERENCES Seccion(SeccionId),
    CONSTRAINT FK_Alumno_Usuario FOREIGN KEY (UsuarioId) REFERENCES Usuario(UsuarioId),
    CONSTRAINT CK_Alumno_Estado CHECK (Estado IN ('Activo', 'Retirado'))
);

CREATE UNIQUE INDEX UQ_Alumno_UsuarioId ON Alumno(UsuarioId) WHERE UsuarioId IS NOT NULL;

-- =========================================================
-- 4. CARGA ACADÉMICA Y EVALUACIONES
-- =========================================================
CREATE TABLE CursoSeccion (
    CursoSeccionId INT IDENTITY(1, 1) PRIMARY KEY,
    CursoId INT NOT NULL,
    SeccionId INT NOT NULL,
    ProfesorId INT NOT NULL,
    AnioLectivo INT NOT NULL,
    CONSTRAINT FK_CursoSeccion_Curso FOREIGN KEY (CursoId) REFERENCES Curso(CursoId),
    CONSTRAINT FK_CursoSeccion_Seccion FOREIGN KEY (SeccionId) REFERENCES Seccion(SeccionId),
    CONSTRAINT FK_CursoSeccion_Profesor FOREIGN KEY (ProfesorId) REFERENCES Profesor(ProfesorId),
    CONSTRAINT UQ_CursoSeccion UNIQUE (CursoId, SeccionId, AnioLectivo)
);

CREATE TABLE Evaluacion (
    EvaluacionId INT IDENTITY(1, 1) PRIMARY KEY,
    PeriodoId INT NOT NULL,
    CursoSeccionId INT NOT NULL,
    NumEval INT NOT NULL,
    NombreEvaluacion VARCHAR(50) NOT NULL DEFAULT 'Examen Parcial',
    CONSTRAINT FK_Evaluacion_Periodo FOREIGN KEY (PeriodoId) REFERENCES Periodo(PeriodoId),
    CONSTRAINT FK_Evaluacion_CursoSeccion FOREIGN KEY (CursoSeccionId) REFERENCES CursoSeccion(CursoSeccionId),
    CONSTRAINT CK_NumEval CHECK (NumEval IN (1, 2)),
    CONSTRAINT UQ_Evaluacion_Periodo_Curso_Num UNIQUE (PeriodoId, CursoSeccionId, NumEval)
);

CREATE TABLE Nota (
    AlumnoId INT NOT NULL,
    EvaluacionId INT NOT NULL,
    Puntaje DECIMAL(4, 2) NOT NULL,
    Fecha DATETIME NOT NULL DEFAULT GETDATE(),
    CONSTRAINT PK_Nota PRIMARY KEY (AlumnoId, EvaluacionId),
    CONSTRAINT FK_Nota_Alumno FOREIGN KEY (AlumnoId) REFERENCES Alumno(AlumnoId),
    CONSTRAINT FK_Nota_Evaluacion FOREIGN KEY (EvaluacionId) REFERENCES Evaluacion(EvaluacionId),
    CONSTRAINT CK_Puntaje CHECK (Puntaje BETWEEN 0.00 AND 20.00)
);

CREATE TABLE Asistencia (
    AlumnoId INT NOT NULL,
    CursoSeccionId INT NOT NULL,
    Fecha DATE NOT NULL,
    Estado CHAR(1) NOT NULL,                        -- A = Asistió, F = Falta, T = Tardanza, J = Justificado
    Observacion VARCHAR(150) NULL,                  -- justificación de tardanza/inasistencia
    CONSTRAINT PK_Asistencia PRIMARY KEY (AlumnoId, CursoSeccionId, Fecha),
    CONSTRAINT FK_Asistencia_Alumno FOREIGN KEY (AlumnoId) REFERENCES Alumno(AlumnoId),
    CONSTRAINT FK_Asistencia_CursoSeccion FOREIGN KEY (CursoSeccionId) REFERENCES CursoSeccion(CursoSeccionId),
    CONSTRAINT CK_Asistencia_Estado CHECK (Estado IN ('A', 'F', 'T', 'J'))
);

-- =========================================================
-- 5. CONSOLIDADO PARA REPORTES (DetalleRendimiento)
-- =========================================================
-- Tabla de resumen: una fila por alumno, curso y bimestre.
-- Se llena/actualiza con sp_ActualizarReporteRendimiento.
CREATE TABLE DetalleRendimiento (
    AlumnoId INT NOT NULL,
    CursoSeccionId INT NOT NULL,
    PeriodoId INT NOT NULL,
    PromedioBimestre DECIMAL(4, 2) NOT NULL DEFAULT 0.00,
    TotalAsistencias INT NOT NULL DEFAULT 0,
    TotalFaltas INT NOT NULL DEFAULT 0,
    TotalTardanzas INT NOT NULL DEFAULT 0,
    TotalJustificadas INT NOT NULL DEFAULT 0,
    EstadoAprobacion VARCHAR(20) NOT NULL DEFAULT 'En Proceso',
    UltimaActualizacion DATETIME NOT NULL DEFAULT GETDATE(),
    CONSTRAINT PK_DetalleRendimiento PRIMARY KEY (AlumnoId, CursoSeccionId, PeriodoId),
    CONSTRAINT FK_DetalleRendimiento_Alumno FOREIGN KEY (AlumnoId) REFERENCES Alumno(AlumnoId),
    CONSTRAINT FK_DetalleRendimiento_CursoSeccion FOREIGN KEY (CursoSeccionId) REFERENCES CursoSeccion(CursoSeccionId),
    CONSTRAINT FK_DetalleRendimiento_Periodo FOREIGN KEY (PeriodoId) REFERENCES Periodo(PeriodoId),
    CONSTRAINT CK_DetalleRendimiento_Estado CHECK (EstadoAprobacion IN ('En Proceso', 'Aprobado', 'Desaprobado'))
);

-- =========================================================
-- 6. AUDITORÍA (RF09)
-- =========================================================
-- Registro genérico de cambios sobre Nota y Asistencia: quién, cuándo,
-- qué fila y qué valor tenía antes/después. "ClavePrimaria" guarda una
-- descripción legible de la fila afectada (p. ej. 'AlumnoId=5;EvaluacionId=12').
CREATE TABLE Auditoria (
    AuditoriaId INT IDENTITY(1, 1) PRIMARY KEY,
    Tabla VARCHAR(20) NOT NULL,
    ClavePrimaria VARCHAR(100) NOT NULL,
    UsuarioId INT NOT NULL,
    FechaCambio DATETIME NOT NULL DEFAULT GETDATE(),
    ValorAnterior VARCHAR(20) NULL,
    ValorNuevo VARCHAR(20) NOT NULL,
    CONSTRAINT FK_Auditoria_Usuario FOREIGN KEY (UsuarioId) REFERENCES Usuario(UsuarioId),
    CONSTRAINT CK_Auditoria_Tabla CHECK (Tabla IN ('Nota', 'Asistencia'))
);
GO

-- =========================================================
-- 7. STORED PROCEDURES DE AUTENTICACIÓN
-- =========================================================
-- Devuelve los datos para que Java verifique con
-- BCrypt.checkpw(passwordPlano, hashGuardado) y aplique las reglas de
-- bloqueo / cambio obligatorio de contraseña.
CREATE PROCEDURE sp_ObtenerUsuarioParaLogin
    @Username VARCHAR(50)
AS
BEGIN
    SET NOCOUNT ON;

    SELECT
        u.UsuarioId,
        u.Username,
        u.PasswordHash,
        u.Estado,
        u.DebeCambiarPassword,
        u.IntentosFallidos,
        u.BloqueadoHasta,
        r.Nombre AS Rol,
        CASE
            WHEN r.Nombre = 'Docente' THEN ISNULL(p.Nombre + ' ' + p.Apellido, 'Docente')
            WHEN r.Nombre = 'Estudiante' THEN ISNULL(a.Nombre + ' ' + a.Apellido, 'Estudiante')
            ELSE u.Username
        END AS NombreCompleto
    FROM Usuario u
    INNER JOIN Rol r ON u.RolId = r.RolId
    LEFT JOIN Profesor p ON u.UsuarioId = p.UsuarioId
    LEFT JOIN Alumno a ON u.UsuarioId = a.UsuarioId
    WHERE u.Username = @Username
      AND u.Estado = 1;
END;
GO

-- Se llama cuando BCrypt.checkpw(...) da false en Java.
-- Bloquea la cuenta 15 minutos al llegar a 3 intentos fallidos (RF02).
CREATE PROCEDURE sp_RegistrarIntentoFallido
    @UsuarioId INT
AS
BEGIN
    SET NOCOUNT ON;

    UPDATE Usuario
    SET IntentosFallidos = IntentosFallidos + 1,
        BloqueadoHasta = CASE
            WHEN IntentosFallidos + 1 >= 3 THEN DATEADD(MINUTE, 15, GETDATE())
            ELSE BloqueadoHasta
        END
    WHERE UsuarioId = @UsuarioId;
END;
GO

-- Se llama cuando BCrypt.checkpw(...) da true en Java: limpia el contador.
CREATE PROCEDURE sp_RegistrarLoginExitoso
    @UsuarioId INT
AS
BEGIN
    SET NOCOUNT ON;

    UPDATE Usuario
    SET IntentosFallidos = 0,
        BloqueadoHasta = NULL
    WHERE UsuarioId = @UsuarioId;
END;
GO

-- =========================================================
-- 8. STORED PROCEDURE DE REPORTES
-- =========================================================
-- Recalcula el consolidado de UN alumno en UN curso y UN bimestre.
--  * Promedio: solo notas de evaluaciones de ese curso y periodo.
--  * Asistencia: solo registros dentro de FechaInicio..FechaFin del periodo
--    (corrige el error de contar todo el año en cada bimestre).
--  * Si no hay notas, el estado queda 'En Proceso'.
--  * Nota mínima aprobatoria: 10.5
CREATE PROCEDURE sp_ActualizarReporteRendimiento
    @AlumnoId INT,
    @CursoSeccionId INT,
    @PeriodoId INT
AS
BEGIN
    SET NOCOUNT ON;

    DECLARE @FechaInicio DATE, @FechaFin DATE;

    SELECT @FechaInicio = FechaInicio, @FechaFin = FechaFin
    FROM Periodo
    WHERE PeriodoId = @PeriodoId;

    IF @FechaInicio IS NULL
        THROW 50001, 'El periodo indicado no existe.', 1;

    -- Promedio y cantidad de notas del bimestre
    DECLARE @Promedio DECIMAL(4, 2), @CantNotas INT;

    SELECT
        @CantNotas = COUNT(n.Puntaje),
        @Promedio  = CAST(ISNULL(ROUND(AVG(n.Puntaje), 2), 0) AS DECIMAL(4, 2))
    FROM Nota n
    INNER JOIN Evaluacion e ON n.EvaluacionId = e.EvaluacionId
    WHERE n.AlumnoId = @AlumnoId
      AND e.CursoSeccionId = @CursoSeccionId
      AND e.PeriodoId = @PeriodoId;

    -- Asistencia del bimestre (filtrada por fechas del periodo)
    DECLARE @Asistencias INT, @Faltas INT, @Tardanzas INT, @Justificadas INT;

    SELECT
        @Asistencias  = ISNULL(SUM(CASE WHEN Estado = 'A' THEN 1 ELSE 0 END), 0),
        @Faltas       = ISNULL(SUM(CASE WHEN Estado = 'F' THEN 1 ELSE 0 END), 0),
        @Tardanzas    = ISNULL(SUM(CASE WHEN Estado = 'T' THEN 1 ELSE 0 END), 0),
        @Justificadas = ISNULL(SUM(CASE WHEN Estado = 'J' THEN 1 ELSE 0 END), 0)
    FROM Asistencia
    WHERE AlumnoId = @AlumnoId
      AND CursoSeccionId = @CursoSeccionId
      AND Fecha BETWEEN @FechaInicio AND @FechaFin;

    DECLARE @Estado VARCHAR(20) =
        CASE
            WHEN @CantNotas = 0 THEN 'En Proceso'
            WHEN @Promedio >= 10.5 THEN 'Aprobado'
            ELSE 'Desaprobado'
        END;

    BEGIN TRANSACTION;

        UPDATE DetalleRendimiento WITH (UPDLOCK, SERIALIZABLE)
        SET PromedioBimestre    = @Promedio,
            TotalAsistencias    = @Asistencias,
            TotalFaltas         = @Faltas,
            TotalTardanzas      = @Tardanzas,
            TotalJustificadas   = @Justificadas,
            EstadoAprobacion    = @Estado,
            UltimaActualizacion = GETDATE()
        WHERE AlumnoId = @AlumnoId
          AND CursoSeccionId = @CursoSeccionId
          AND PeriodoId = @PeriodoId;

        IF @@ROWCOUNT = 0
            INSERT INTO DetalleRendimiento
                (AlumnoId, CursoSeccionId, PeriodoId, PromedioBimestre,
                 TotalAsistencias, TotalFaltas, TotalTardanzas, TotalJustificadas,
                 EstadoAprobacion)
            VALUES
                (@AlumnoId, @CursoSeccionId, @PeriodoId, @Promedio,
                 @Asistencias, @Faltas, @Tardanzas, @Justificadas, @Estado);

    COMMIT TRANSACTION;
END;
GO

-- =========================================================
-- 9. SEMBRADO DE DATOS (CATÁLOGOS Y CARGA ACADÉMICA 2026)
-- =========================================================

-- A. Roles
INSERT INTO Rol (Nombre) VALUES
    ('Administrador'), ('Director'), ('Docente'), ('Estudiante');

-- B. Usuarios de prueba — hashes BCrypt reales (costo 12)
-- admin / admin123
INSERT INTO Usuario (Username, PasswordHash, RolId) VALUES
    ('admin', '$2b$12$8sAEdDptTetEqXf9Qz.RoeHF2Es7MB/Jr04LhvlvpFQ66.tFJKXBK', 1);
-- director / director123
INSERT INTO Usuario (Username, PasswordHash, RolId) VALUES
    ('director', '$2b$12$1rjkXEevbKuuzHpM61vRquEvXAciUjP3CI6gZaRcfCa738THP1pl6', 2);
-- jgarcia / docente123
INSERT INTO Usuario (Username, PasswordHash, RolId) VALUES
    ('jgarcia', '$2b$12$NOQHr28UdpIvBY4pK.XZMef2a6vIth7JH90ErORvBV5GK.kVldtOe', 3);
-- alumno1 / alumno123
INSERT INTO Usuario (Username, PasswordHash, RolId) VALUES
    ('alumno1', '$2b$12$PvTAwdZeVMDx.oi.T6plou1iA3GMk4.Duodtfnv/jJUXkS/nYnqzG', 4);

-- C. Turnos, Grados (1.° a 6.°, según RF05) y Cursos
INSERT INTO Turno (Nombre) VALUES ('Mañana'), ('Tarde');

INSERT INTO Grado (Nombre) VALUES
    ('1ro Secundaria'), ('2do Secundaria'), ('3ro Secundaria'),
    ('4to Secundaria'), ('5to Secundaria'), ('6to Secundaria');

INSERT INTO Curso (Nombre) VALUES
    ('Matemáticas'), ('Comunicación'), ('Ciencia y Tecnología'),
    ('Historia y Geografía'), ('Inglés');

-- D. Secciones de 1er grado (2 Mañana, 2 Tarde)
INSERT INTO Seccion (GradoId, TurnoId, Nombre) VALUES
    (1, 1, 'A'), (1, 1, 'B'), (1, 2, 'C'), (1, 2, 'D');

-- E. Docente
INSERT INTO Profesor (UsuarioId, DNI, Nombre, Apellido, Email) VALUES
    (3, '45879632', 'Juan', 'García', 'jgarcia@escuela.edu.pe');

-- F. 20 alumnos (5 por sección)
-- Sección A (dos primeros con datos de apoderado de ejemplo)
INSERT INTO Alumno (SeccionId, UsuarioId, DNI, Nombre, Apellido, NombreApoderado, TelefonoApoderado) VALUES
    (1, 4, '71000001', 'Carlos', 'Mendoza', 'Roberto Mendoza', '987654321'),
    (1, NULL, '71000002', 'Ana', 'Rojas', 'Maria Rojas', '987654322');
INSERT INTO Alumno (SeccionId, DNI, Nombre, Apellido) VALUES
    (1, '71000003', 'Luis', 'Torres'),
    (1, '71000004', 'Maria', 'Sanchez'),
    (1, '71000005', 'Pedro', 'Gomez');

-- Sección B
INSERT INTO Alumno (SeccionId, DNI, Nombre, Apellido) VALUES
    (2, '72000001', 'Jorge', 'Castro'),
    (2, '72000002', 'Lucia', 'Vargas'),
    (2, '72000003', 'Diego', 'Flores'),
    (2, '72000004', 'Elena', 'Morales'),
    (2, '72000005', 'Raul', 'Gutierrez');

-- Sección C
INSERT INTO Alumno (SeccionId, DNI, Nombre, Apellido) VALUES
    (3, '73000001', 'Sofia', 'Espinoza'),
    (3, '73000002', 'Mateo', 'Arias'),
    (3, '73000003', 'Camila', 'Campos'),
    (3, '73000004', 'Gabriel', 'Reyes'),
    (3, '73000005', 'Valeria', 'Perez');

-- Sección D
INSERT INTO Alumno (SeccionId, DNI, Nombre, Apellido) VALUES
    (4, '74000001', 'Hugo', 'Navarro'),
    (4, '74000002', 'Paula', 'Cordero'),
    (4, '74000003', 'Daniel', 'Salazar'),
    (4, '74000004', 'Andrea', 'Paredes'),
    (4, '74000005', 'Marcos', 'Delgado');

-- G. Periodos 2026 (4 bimestres, con fechas de ejemplo; ajústalas al calendario real)
INSERT INTO Periodo (AnioLectivo, BimestreNum, FechaInicio, FechaFin) VALUES
    (2026, 1, '2026-03-16', '2026-05-15'),
    (2026, 2, '2026-05-18', '2026-07-24'),
    (2026, 3, '2026-08-03', '2026-10-09'),
    (2026, 4, '2026-10-12', '2026-12-18');

-- H. Carga académica de prueba
-- Juan García (ProfesorId 1) dicta Matemáticas (CursoId 1) en 1ro A (SeccionId 1), año 2026
INSERT INTO CursoSeccion (CursoId, SeccionId, ProfesorId, AnioLectivo) VALUES
    (1, 1, 1, 2026);
GO
