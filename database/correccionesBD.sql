USE master;
GO

IF EXISTS (SELECT * FROM sys.databases WHERE name = 'BD_GestionAcademica')
    DROP DATABASE BD_GestionAcademica;
GO

CREATE DATABASE BD_GestionAcademica;
GO

USE BD_GestionAcademica;
GO

-- =========================================================
-- 1. SEGURIDAD, ROLES Y USUARIOS
-- =========================================================

CREATE TABLE Rol (
    RolId INT IDENTITY(1,1) PRIMARY KEY,
    Nombre VARCHAR(30) NOT NULL UNIQUE
);

CREATE TABLE Usuario (
    UsuarioId INT IDENTITY(1,1) PRIMARY KEY,
    Username VARCHAR(50) NOT NULL UNIQUE,
    PasswordHash VARCHAR(64) NOT NULL,
    RolId INT NOT NULL,
    Estado BIT DEFAULT 1,
    FechaRegistro DATETIME DEFAULT GETDATE(), -- Cambiado de CreadoEl
    CONSTRAINT FK_Usuario_Rol FOREIGN KEY (RolId) REFERENCES Rol(RolId)
);

-- =========================================================
-- 2. TABLAS MAESTRAS Y PERSONAL
-- =========================================================

CREATE TABLE Turno (
    TurnoId INT IDENTITY(1,1) PRIMARY KEY,
    Nombre VARCHAR(20) NOT NULL UNIQUE
);

CREATE TABLE Grado (
    GradoId INT IDENTITY(1,1) PRIMARY KEY,
    Nombre VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE Profesor (
    ProfesorId INT IDENTITY(1,1) PRIMARY KEY,
    UsuarioId INT NULL,
    DNI CHAR(8) NOT NULL UNIQUE,
    Nombre VARCHAR(50) NOT NULL,
    Apellido VARCHAR(50) NOT NULL,
    Email VARCHAR(100),
    CONSTRAINT FK_Profesor_Usuario FOREIGN KEY (UsuarioId) REFERENCES Usuario(UsuarioId)
);
CREATE UNIQUE INDEX UQ_Profesor_UsuarioId ON Profesor(UsuarioId) WHERE UsuarioId IS NOT NULL;

CREATE TABLE Curso (
    CursoId INT IDENTITY(1,1) PRIMARY KEY,
    Nombre VARCHAR(100) NOT NULL UNIQUE
);

-- Periodo: ÚNICO LUGAR donde se define el Año Lectivo y Bimestre (Tipos de datos en INT).
CREATE TABLE Periodo (
    PeriodoId INT IDENTITY(1,1) PRIMARY KEY,
    AnioLectivo INT NOT NULL, 
    BimestreNum INT NOT NULL,
    CONSTRAINT CK_Bimestre CHECK (BimestreNum BETWEEN 1 AND 4),
    CONSTRAINT UQ_Periodo_Anio UNIQUE (AnioLectivo, BimestreNum)
);

-- =========================================================
-- 3. ESTRUCTURA ESCOLAR Y ESTUDIANTES
-- =========================================================

CREATE TABLE Seccion (
    SeccionId INT IDENTITY(1,1) PRIMARY KEY,
    GradoId INT NOT NULL,
    TurnoId INT NOT NULL,
    Nombre CHAR(1) NOT NULL,
    CONSTRAINT FK_Seccion_Grado FOREIGN KEY (GradoId) REFERENCES Grado(GradoId),
    CONSTRAINT FK_Seccion_Turno FOREIGN KEY (TurnoId) REFERENCES Turno(TurnoId),
    CONSTRAINT UQ_Seccion_Grado_Nombre UNIQUE (GradoId, Nombre)
);

CREATE TABLE Alumno (
    AlumnoId INT IDENTITY(1,1) PRIMARY KEY,
    SeccionId INT NOT NULL,
    UsuarioId INT NULL,
    DNI CHAR(8) NOT NULL UNIQUE,
    Nombre VARCHAR(50) NOT NULL,
    Apellido VARCHAR(50) NOT NULL,
    Estado VARCHAR(20) DEFAULT 'Activo',
    CONSTRAINT FK_Alumno_Seccion FOREIGN KEY (SeccionId) REFERENCES Seccion(SeccionId),
    CONSTRAINT FK_Alumno_Usuario FOREIGN KEY (UsuarioId) REFERENCES Usuario(UsuarioId),
    CONSTRAINT CK_Alumno_Estado CHECK (Estado IN ('Activo', 'Retirado'))
);

CREATE UNIQUE INDEX UQ_Alumno_UsuarioId ON Alumno(UsuarioId) WHERE UsuarioId IS NOT NULL;

-- =========================================================
-- 4. CARGA ACADÉMICA Y EVALUACIONES
-- =========================================================

-- CursoSeccion: Asignación de docente y salón (Se eliminó AnioLectivo para evitar duplicidad).
CREATE TABLE CursoSeccion (
    CursoSeccionId INT IDENTITY(1,1) PRIMARY KEY,
    CursoId INT NOT NULL,
    SeccionId INT NOT NULL,
    ProfesorId INT NOT NULL,
    CONSTRAINT FK_CursoSeccion_Curso FOREIGN KEY (CursoId) REFERENCES Curso(CursoId),
    CONSTRAINT FK_CursoSeccion_Seccion FOREIGN KEY (SeccionId) REFERENCES Seccion(SeccionId),
    CONSTRAINT FK_CursoSeccion_Profesor FOREIGN KEY (ProfesorId) REFERENCES Profesor(ProfesorId),
    CONSTRAINT UQ_CursoSeccion UNIQUE (CursoId, SeccionId)
);

-- Evaluacion: Vincula el curso/sección con el Periodo (que trae el Año Lectivo).
CREATE TABLE Evaluacion (
    EvaluacionId INT IDENTITY(1,1) PRIMARY KEY,
    PeriodoId INT NOT NULL,
    CursoSeccionId INT NOT NULL,
    NumEval INT NOT NULL,
    NombreEvaluacion VARCHAR(50) DEFAULT 'Examen Parcial',
    CONSTRAINT FK_Evaluacion_Periodo FOREIGN KEY (PeriodoId) REFERENCES Periodo(PeriodoId),
    CONSTRAINT FK_Evaluacion_CursoSeccion FOREIGN KEY (CursoSeccionId) REFERENCES CursoSeccion(CursoSeccionId),
    CONSTRAINT CK_NumEval CHECK (NumEval BETWEEN 1 AND 4),
    CONSTRAINT UQ_Evaluacion_Periodo_Curso_Num UNIQUE (PeriodoId, CursoSeccionId, NumEval)
);


-- Nota: Registro de calificaciones puntuales por evaluación.
CREATE TABLE Nota (
    AlumnoId INT NOT NULL,
    EvaluacionId INT NOT NULL,
    Calificacion DECIMAL(4,2) NOT NULL,
    FechaRegistro DATETIME DEFAULT GETDATE(),
    CONSTRAINT PK_Nota PRIMARY KEY (AlumnoId, EvaluacionId),
    CONSTRAINT FK_Nota_Alumno FOREIGN KEY (AlumnoId) REFERENCES Alumno(AlumnoId),
    CONSTRAINT FK_Nota_Evaluacion FOREIGN KEY (EvaluacionId) REFERENCES Evaluacion(EvaluacionId),
    CONSTRAINT CK_Calificacion CHECK (Calificacion BETWEEN 0 AND 20)
);

-- Asistencia: Con marca de tiempo exacto (FechaHora DATETIME)
CREATE TABLE Asistencia (
    AlumnoId INT NOT NULL,
    CursoSeccionId INT NOT NULL,
    FechaHora DATETIME NOT NULL DEFAULT GETDATE(), -- Registra fecha y hora exacta
    Estado CHAR(1) NOT NULL, -- 'A' = Asistió, 'F' = Falta, 'T' = Tardanza, 'J' = Justificado
    Observacion VARCHAR(150) NULL, -- Para justificaciones de tardanza/inasistencia
    CONSTRAINT PK_Asistencia PRIMARY KEY (AlumnoId, CursoSeccionId, FechaHora),
    CONSTRAINT FK_Asistencia_Alumno FOREIGN KEY (AlumnoId) REFERENCES Alumno(AlumnoId),
    CONSTRAINT FK_Asistencia_CursoSeccion FOREIGN KEY (CursoSeccionId) REFERENCES CursoSeccion(CursoSeccionId),
    CONSTRAINT CK_Asistencia_Estado CHECK (Estado IN ('A', 'F', 'T', 'J'))
);

-- =========================================================
-- 5. ENTIDAD DÉBIL PARA REPORTES (DetalleRendimiento)
-- =========================================================

CREATE TABLE DetalleRendimiento (
    AlumnoId INT NOT NULL,
    CursoSeccionId INT NOT NULL,
    PeriodoId INT NOT NULL,
    PromedioBimestre DECIMAL(4,2) DEFAULT 0.00,
    TotalAsistencias INT DEFAULT 0,
    TotalFaltas INT DEFAULT 0,
    TotalTardanzas INT DEFAULT 0,
    EstadoAprobacion VARCHAR(20) DEFAULT 'En Proceso',
    UltimaActualizacion DATETIME DEFAULT GETDATE(),

    CONSTRAINT PK_DetalleRendimiento PRIMARY KEY (AlumnoId, CursoSeccionId, PeriodoId),
    CONSTRAINT FK_DetalleRendimiento_Alumno FOREIGN KEY (AlumnoId) 
        REFERENCES Alumno(AlumnoId) ON DELETE CASCADE,
    CONSTRAINT FK_DetalleRendimiento_CursoSeccion FOREIGN KEY (CursoSeccionId) 
        REFERENCES CursoSeccion(CursoSeccionId),
    CONSTRAINT FK_DetalleRendimiento_Periodo FOREIGN KEY (PeriodoId) 
        REFERENCES Periodo(PeriodoId)
);
GO



-- =========================================================
-- 6. PROCEDIMIENTOS ALMACENADOS
-- =========================================================

-- Stored Procedure: Autenticación para el Login
CREATE PROCEDURE sp_AutenticarUsuario 
    @Username VARCHAR(50),
    @PasswordHash VARCHAR(64) 
AS 
BEGIN
    SET NOCOUNT ON;

    SELECT
        u.UsuarioId,
        u.Username,
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
      AND u.PasswordHash = @PasswordHash
      AND u.Estado = 1;
END;
GO

-- Stored Procedure: Actualizar Tabla Débil (Consolidados)
CREATE PROCEDURE sp_ActualizarReporteRendimiento
    @AlumnoId INT,
    @CursoSeccionId INT,
    @PeriodoId INT
AS
BEGIN
    SET NOCOUNT ON;

    -- Cálculo del promedio
    DECLARE @Promedio DECIMAL(4,2);
    SELECT @Promedio = ISNULL(ROUND(AVG(n.Calificacion), 2), 0.00)
    FROM Nota n
    INNER JOIN Evaluacion e ON n.EvaluacionId = e.EvaluacionId
    WHERE n.AlumnoId = @AlumnoId 
      AND e.CursoSeccionId = @CursoSeccionId 
      AND e.PeriodoId = @PeriodoId;

    -- Conteo de asistencias, faltas y tardanzas
    DECLARE @Asistencias INT, @Faltas INT, @Tardanzas INT;
    SELECT 
        @Asistencias = ISNULL(SUM(CASE WHEN Estado = 'A' THEN 1 ELSE 0 END), 0),
        @Faltas      = ISNULL(SUM(CASE WHEN Estado = 'F' THEN 1 ELSE 0 END), 0),
        @Tardanzas   = ISNULL(SUM(CASE WHEN Estado = 'T' THEN 1 ELSE 0 END), 0)
    FROM Asistencia
    WHERE AlumnoId = @AlumnoId AND CursoSeccionId = @CursoSeccionId;

    DECLARE @Estado VARCHAR(20) = CASE WHEN @Promedio >= 10.5 THEN 'Aprobado' ELSE 'Desaprobado' END;

    MERGE DetalleRendimiento AS Target
    USING (SELECT @AlumnoId AS AlumnoId, @CursoSeccionId AS CursoSeccionId, @PeriodoId AS PeriodoId) AS Source
    ON (Target.AlumnoId = Source.AlumnoId AND Target.CursoSeccionId = Source.CursoSeccionId AND Target.PeriodoId = Source.PeriodoId)
    WHEN MATCHED THEN
        UPDATE SET 
            PromedioBimestre = @Promedio,
            TotalAsistencias = @Asistencias,
            TotalFaltas = @Faltas,
            TotalTardanzas = @Tardanzas,
            EstadoAprobacion = @Estado,
            UltimaActualizacion = GETDATE()
    WHEN NOT MATCHED THEN
        INSERT (AlumnoId, CursoSeccionId, PeriodoId, PromedioBimestre, TotalAsistencias, TotalFaltas, TotalTardanzas, EstadoAprobacion)
        VALUES (@AlumnoId, @CursoSeccionId, @PeriodoId, @Promedio, @Asistencias, @Faltas, @Tardanzas, @Estado);
END;
GO




-- =========================================================
-- 7. DATOS DE PRUEBA (SEEDS)
-- =========================================================
INSERT INTO Rol (Nombre) VALUES ('Administrador'), ('Director'), ('Docente'), ('Estudiante');

INSERT INTO Usuario (Username, PasswordHash, RolId) VALUES 
('admin', '240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9', 1),
('director', '9e4d7bba246abe731743986c4dc50897b68b1d0249a066abb3530fcbaa33dab3', 2),
('jgarcia', '9228105f4ea40ae4b70fde55ee2caf4495c1f188333c41ef566417ab04819f56', 3),
('alumno1', 'c1042ecc51482cef39f2e89e1273a35074db7f873f1ac6050efd546a9bceefc0', 4);

INSERT INTO Turno (Nombre) VALUES ('Mañana'), ('Tarde');
INSERT INTO Grado (Nombre) VALUES ('1ro Secundaria');
INSERT INTO Curso (Nombre) VALUES ('Matemáticas'), ('Comunicación'), ('Ciencia y Tecnología'), ('Historia y Geografía'), ('Inglés');

INSERT INTO Periodo (AnioLectivo, BimestreNum) VALUES 
(2026, 1), (2026, 2), (2026, 3), (2026, 4);

INSERT INTO Seccion (GradoId, TurnoId, Nombre) VALUES 
(1, 1, 'A'), (1, 1, 'B'), (1, 2, 'C'), (1, 2, 'D');

INSERT INTO Profesor (UsuarioId, DNI, Nombre, Apellido, Email) VALUES 
(3, '45879632', 'Juan', 'García', 'jgarcia@colegio.edu.pe');

INSERT INTO Alumno (SeccionId, UsuarioId, DNI, Nombre, Apellido) VALUES
(1, 4, '71000001', 'Carlos', 'Mendoza'),
(1, NULL, '71000002', 'Ana', 'Rojas'),
(1, NULL, '71000003', 'Luis', 'Torres'),
(1, NULL, '71000004', 'Maria', 'Sanchez'),
(1, NULL, '71000005', 'Pedro', 'Gomez');

INSERT INTO Alumno (SeccionId, DNI, Nombre, Apellido) VALUES
(2, '72000001', 'Jorge', 'Castro'),
(2, '72000002', 'Lucia', 'Vargas'),
(2, '72000003', 'Diego', 'Flores'),
(2, '72000004', 'Elena', 'Morales'),
(2, '72000005', 'Raul', 'Gutierrez');

INSERT INTO Alumno (SeccionId, DNI, Nombre, Apellido) VALUES
(3, '73000001', 'Sofia', 'Espinoza'),
(3, '73000002', 'Mateo', 'Arias'),
(3, '73000003', 'Camila', 'Campos'),
(3, '73000004', 'Gabriel', 'Reyes'),
(3, '73000005', 'Valeria', 'Perez');

INSERT INTO Alumno (SeccionId, DNI, Nombre, Apellido) VALUES
(4, '74000001', 'Hugo', 'Navarro'),
(4, '74000002', 'Paula', 'Cordero'),
(4, '74000003', 'Daniel', 'Salazar'),
(4, '74000004', 'Andrea', 'Paredes'),
(4, '74000005', 'Marcos', 'Delgado');
GO
