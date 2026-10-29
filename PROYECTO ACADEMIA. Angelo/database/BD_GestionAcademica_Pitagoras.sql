-- =====================================================================================
-- SCRIPT OFICIAL DE BASE DE DATOS: BD_GestionAcademica_Pitagoras
-- Basado en el Modelo Lógico (Figura 18) y Modelo Físico (Figura 19, pág. 43) del Informe UTP
-- Motor: Microsoft SQL Server (Puerto 1433)
-- =====================================================================================

IF NOT EXISTS (SELECT name FROM sys.databases WHERE name = N'BD_GestionAcademica_Pitagoras')
BEGIN
    CREATE DATABASE BD_GestionAcademica_Pitagoras;
END
GO

USE BD_GestionAcademica_Pitagoras;
GO

-- 1. Seguridad y Usuarios (Auth - RF01, RF02, RF03)
IF OBJECT_ID('Rol', 'U') IS NULL
CREATE TABLE Rol (
    RolId INT IDENTITY(1,1) PRIMARY KEY,
    Nombre VARCHAR(50) NOT NULL UNIQUE
);

IF OBJECT_ID('Usuario', 'U') IS NULL
CREATE TABLE Usuario (
    UsuarioId INT IDENTITY(1,1) PRIMARY KEY,
    Username VARCHAR(50) NOT NULL UNIQUE,
    PasswordHash VARCHAR(255) NOT NULL,
    RolId INT NOT NULL REFERENCES Rol(RolId),
    Estado BIT NOT NULL DEFAULT 1,
    DebeCambiarPassword BIT NOT NULL DEFAULT 1,
    IntentosFallidos INT NOT NULL DEFAULT 0,
    BloqueadoHasta DATETIME NULL,
    CreadoEn DATETIME NOT NULL DEFAULT GETDATE()
);

-- 2. Estructura Académica (Academic - RF05, RF07)
IF OBJECT_ID('Turno', 'U') IS NULL
CREATE TABLE Turno (
    TurnoId INT IDENTITY(1,1) PRIMARY KEY,
    Nombre VARCHAR(30) NOT NULL UNIQUE
);

IF OBJECT_ID('Grado', 'U') IS NULL
CREATE TABLE Grado (
    GradoId INT IDENTITY(1,1) PRIMARY KEY,
    Nombre VARCHAR(30) NOT NULL UNIQUE
);

IF OBJECT_ID('Seccion', 'U') IS NULL
CREATE TABLE Seccion (
    SeccionId INT IDENTITY(1,1) PRIMARY KEY,
    GradoId INT NOT NULL REFERENCES Grado(GradoId),
    TurnoId INT NOT NULL REFERENCES Turno(TurnoId),
    Nombre CHAR(2) NOT NULL
);

IF OBJECT_ID('Curso', 'U') IS NULL
CREATE TABLE Curso (
    CursoId INT IDENTITY(1,1) PRIMARY KEY,
    Nombre VARCHAR(100) NOT NULL UNIQUE
);

IF OBJECT_ID('Periodo', 'U') IS NULL
CREATE TABLE Periodo (
    PeriodoId INT IDENTITY(1,1) PRIMARY KEY,
    AnioEscolar INT NOT NULL,
    Bimestre INT NOT NULL CHECK (Bimestre BETWEEN 1 AND 4),
    FechaInicio DATE NOT NULL,
    FechaFin DATE NOT NULL,
    Cerrado BIT NOT NULL DEFAULT 0,
    FechaCierre DATETIME NULL,
    CerradoPorUsuarioId INT NULL REFERENCES Usuario(UsuarioId)
);

-- 3. Docentes, Alumnos y Carga Académica (RF04, RF06, RF11)
IF OBJECT_ID('Profesor', 'U') IS NULL
CREATE TABLE Profesor (
    ProfesorId INT IDENTITY(1,1) PRIMARY KEY,
    UsuarioId INT NOT NULL UNIQUE REFERENCES Usuario(UsuarioId),
    DNI CHAR(8) NOT NULL UNIQUE,
    Nombre VARCHAR(80) NOT NULL,
    Apellido VARCHAR(80) NOT NULL,
    Email VARCHAR(120) NULL
);

IF OBJECT_ID('Alumno', 'U') IS NULL
CREATE TABLE Alumno (
    AlumnoId INT IDENTITY(1,1) PRIMARY KEY,
    SeccionId INT NOT NULL REFERENCES Seccion(SeccionId),
    UsuarioId INT NOT NULL UNIQUE REFERENCES Usuario(UsuarioId),
    DNI CHAR(8) NOT NULL UNIQUE,
    Nombre VARCHAR(80) NOT NULL,
    Apellido VARCHAR(80) NOT NULL,
    NombreApoderado VARCHAR(150) NOT NULL,
    TelefonoApoderado VARCHAR(20) NULL,
    Estado VARCHAR(20) NOT NULL DEFAULT 'Activa'
);

IF OBJECT_ID('CursoSeccion', 'U') IS NULL
CREATE TABLE CursoSeccion (
    CursoSeccionId INT IDENTITY(1,1) PRIMARY KEY,
    CursoId INT NOT NULL REFERENCES Curso(CursoId),
    SeccionId INT NOT NULL REFERENCES Seccion(SeccionId),
    ProfesorId INT NOT NULL REFERENCES Profesor(ProfesorId)
);

IF OBJECT_ID('DetalleCursoSeccion', 'U') IS NULL
CREATE TABLE DetalleCursoSeccion (
    DetalleCursoSeccionId INT IDENTITY(1,1) PRIMARY KEY,
    CursoSeccionId INT NOT NULL REFERENCES CursoSeccion(CursoSeccionId),
    AlumnoId INT NOT NULL REFERENCES Alumno(AlumnoId),
    PeriodoId INT NOT NULL REFERENCES Periodo(PeriodoId),
    Calificativo CHAR(2) NULL CHECK (Calificativo IN ('AD', 'A', 'B', 'C'))
);

-- 4. Evaluación y Asistencia (Grades & Attendance - RF12, RF13, RF14)
IF OBJECT_ID('Evaluacion', 'U') IS NULL
CREATE TABLE Evaluacion (
    EvaluacionId INT IDENTITY(1,1) PRIMARY KEY,
    PeriodoId INT NOT NULL REFERENCES Periodo(PeriodoId),
    CursoSeccionId INT NOT NULL REFERENCES CursoSeccion(CursoSeccionId),
    NombreEvaluacion VARCHAR(120) NOT NULL
);

IF OBJECT_ID('Nota', 'U') IS NULL
CREATE TABLE Nota (
    DetalleCursoSeccionId INT NOT NULL REFERENCES DetalleCursoSeccion(DetalleCursoSeccionId),
    EvaluacionId INT NOT NULL REFERENCES Evaluacion(EvaluacionId),
    Calificativo CHAR(2) NOT NULL CHECK (Calificativo IN ('AD', 'A', 'B', 'C')),
    Fecha DATETIME NOT NULL DEFAULT GETDATE(),
    PRIMARY KEY (DetalleCursoSeccionId, EvaluacionId)
);

IF OBJECT_ID('Asistencia', 'U') IS NULL
CREATE TABLE Asistencia (
    AlumnoId INT NOT NULL REFERENCES Alumno(AlumnoId),
    CursoSeccionId INT NULL REFERENCES CursoSeccion(CursoSeccionId),
    Fecha DATE NOT NULL,
    Estado CHAR(2) NOT NULL CHECK (Estado IN ('A', 'F', 'T', 'FJ')),
    Observacion VARCHAR(255) NULL,
    PRIMARY KEY (AlumnoId, Fecha)
);

-- 5. Bitácora de Auditoría (Audit - RF09)
IF OBJECT_ID('Auditoria', 'U') IS NULL
CREATE TABLE Auditoria (
    AuditoriaId INT IDENTITY(1,1) PRIMARY KEY,
    Tabla VARCHAR(60) NOT NULL,
    ClavePrimaria VARCHAR(100) NOT NULL,
    UsuarioId INT NOT NULL REFERENCES Usuario(UsuarioId),
    FechaCambio DATETIME NOT NULL DEFAULT GETDATE(),
    ValorAnterior VARCHAR(255) NULL,
    ValorNuevo VARCHAR(255) NULL
);
GO
