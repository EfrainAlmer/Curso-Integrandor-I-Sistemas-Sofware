USE master;

GO
    IF EXISTS (
        SELECT
            *
        FROM
            sys.databases
        WHERE
            name = 'BD_GestionAcademica'
    ) DROP DATABASE BD_GestionAcademica;

GO
    CREATE DATABASE BD_GestionAcademica;

GO
    USE BD_GestionAcademica;

GO
    -- =========================================================
    -- 1. SEGURIDAD, ROLES Y USUARIOS
    -- =========================================================
    -- Rol: Define los 4 perfiles del sistema (Administrador, Director, Docente, Estudiante/Padre).
    CREATE TABLE Rol (
        RolId INT IDENTITY(1, 1) PRIMARY KEY,
        Nombre VARCHAR(30) NOT NULL UNIQUE
    );

-- Usuario: Credenciales de acceso centralizadas para los 4 perfiles con clave en SHA-256.
CREATE TABLE Usuario (
    UsuarioId INT IDENTITY(1, 1) PRIMARY KEY,
    Username VARCHAR(50) NOT NULL UNIQUE,
    PasswordHash VARCHAR(64) NOT NULL,
    RolId INT NOT NULL,
    Estado BIT DEFAULT 1,
    CreadoEl DATETIME DEFAULT GETDATE(),
    CONSTRAINT FK_Usuario_Rol FOREIGN KEY (RolId) REFERENCES Rol(RolId)
);

-- =========================================================
-- 2. TABLAS MAESTRAS Y PERSONAL
-- =========================================================
-- Turno: Define los horarios de atención ('Mañana', 'Tarde').
CREATE TABLE Turno (
    TurnoId INT IDENTITY(1, 1) PRIMARY KEY,
    Nombre VARCHAR(20) NOT NULL UNIQUE
);

-- Grado: Niveles académicos (Ej: '1ro Secundaria').
CREATE TABLE Grado (
    GradoId INT IDENTITY(1, 1) PRIMARY KEY,
    Nombre VARCHAR(50) NOT NULL UNIQUE
);

-- Profesor: Ficha personal del docente vinculada a su cuenta de usuario.
CREATE TABLE Profesor (
    ProfesorId INT IDENTITY(1, 1) PRIMARY KEY,
    UsuarioId INT NULL UNIQUE,
    DNI CHAR(8) NOT NULL UNIQUE,
    Nombre VARCHAR(50) NOT NULL,
    Apellido VARCHAR(50) NOT NULL,
    Email VARCHAR(100),
    CONSTRAINT FK_Profesor_Usuario FOREIGN KEY (UsuarioId) REFERENCES Usuario(UsuarioId)
);

-- Curso: Catálogo general de las 5 materias impartidas en el colegio.
CREATE TABLE Curso (
    CursoId INT IDENTITY(1, 1) PRIMARY KEY,
    Nombre VARCHAR(100) NOT NULL UNIQUE
);

-- Periodo: División del año escolar en 4 bimestres.
CREATE TABLE Periodo (
    PeriodoId INT IDENTITY(1, 1) PRIMARY KEY,
    AnioLectivo INT NOT NULL,
    BimestreNum INT NOT NULL,
    CONSTRAINT CK_Bimestre CHECK (
        BimestreNum BETWEEN 1
        AND 4
    ),
    CONSTRAINT UQ_Periodo_Anio UNIQUE (AnioLectivo, BimestreNum)
);

-- =========================================================
-- 3. ESTRUCTURA ESCOLAR Y ESTUDIANTES
-- =========================================================
-- Seccion: Define los 4 salones (Mañana: A, B | Tarde: C, D).
CREATE TABLE Seccion (
    SeccionId INT IDENTITY(1, 1) PRIMARY KEY,
    GradoId INT NOT NULL,
    TurnoId INT NOT NULL,
    Nombre CHAR(1) NOT NULL,
    CONSTRAINT FK_Seccion_Grado FOREIGN KEY (GradoId) REFERENCES Grado(GradoId),
    CONSTRAINT FK_Seccion_Turno FOREIGN KEY (TurnoId) REFERENCES Turno(TurnoId),
    CONSTRAINT UQ_Seccion_Grado_Nombre UNIQUE (GradoId, Nombre)
);

-- Alumno: Registro del estudiante (vinculado a su aula, estado operativo y cuenta para el Padre).
CREATE TABLE Alumno (
    AlumnoId INT IDENTITY(1, 1) PRIMARY KEY,
    SeccionId INT NOT NULL,
    UsuarioId INT NULL UNIQUE,
    -- Cuenta de acceso para el estudiante/padre
    DNI CHAR(8) NOT NULL UNIQUE,
    Nombre VARCHAR(50) NOT NULL,
    Apellido VARCHAR(50) NOT NULL,
    Estado VARCHAR(20) DEFAULT 'Activo',
    -- 'Activo', 'Retirado'
    CONSTRAINT FK_Alumno_Seccion FOREIGN KEY (SeccionId) REFERENCES Seccion(SeccionId),
    CONSTRAINT FK_Alumno_Usuario FOREIGN KEY (UsuarioId) REFERENCES Usuario(UsuarioId),
    CONSTRAINT CK_Alumno_Estado CHECK (Estado IN ('Activo', 'Retirado'))
);

-- =========================================================
-- 4. CARGA ACADÉMICA Y OPERACIONES DIRECTAS
-- =========================================================
-- CursoSeccion: Asignación de docente, curso y sección por año lectivo.
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

-- Evaluacion: Control de los exámenes programados por bimestre.
CREATE TABLE Evaluacion (
    EvaluacionId INT IDENTITY(1, 1) PRIMARY KEY,
    PeriodoId INT NOT NULL,
    CursoSeccionId INT NOT NULL,
    NumEval INT NOT NULL,
    CONSTRAINT FK_Evaluacion_Periodo FOREIGN KEY (PeriodoId) REFERENCES Periodo(PeriodoId),
    CONSTRAINT FK_Evaluacion_CursoSeccion FOREIGN KEY (CursoSeccionId) REFERENCES CursoSeccion(CursoSeccionId),
    CONSTRAINT CK_NumEval CHECK (NumEval IN (1, 2)),
    CONSTRAINT UQ_Evaluacion_Periodo_Curso_Num UNIQUE (PeriodoId, CursoSeccionId, NumEval)
);

-- Nota: Calificación de 0 a 20 vinculada directamente al Alumno.
CREATE TABLE Nota (
    AlumnoId INT NOT NULL,
    EvaluacionId INT NOT NULL,
    Puntaje DECIMAL(4, 2) NOT NULL,
    Fecha DATETIME DEFAULT GETDATE(),
    CONSTRAINT PK_Nota PRIMARY KEY (AlumnoId, EvaluacionId),
    CONSTRAINT FK_Nota_Alumno FOREIGN KEY (AlumnoId) REFERENCES Alumno(AlumnoId),
    CONSTRAINT FK_Nota_Evaluacion FOREIGN KEY (EvaluacionId) REFERENCES Evaluacion(EvaluacionId),
    CONSTRAINT CK_Puntaje CHECK (
        Puntaje BETWEEN 0
        AND 20
    )
);

-- Asistencia: Registro diario ('A', 'F', 'T') vinculado directamente al Alumno y su Curso/Sección.
CREATE TABLE Asistencia (
    AlumnoId INT NOT NULL,
    CursoSeccionId INT NOT NULL,
    Fecha DATE NOT NULL,
    Estado CHAR(1) NOT NULL,
    CONSTRAINT PK_Asistencia PRIMARY KEY (AlumnoId, CursoSeccionId, Fecha),
    CONSTRAINT FK_Asistencia_Alumno FOREIGN KEY (AlumnoId) REFERENCES Alumno(AlumnoId),
    CONSTRAINT FK_Asistencia_CursoSeccion FOREIGN KEY (CursoSeccionId) REFERENCES CursoSeccion(CursoSeccionId),
    CONSTRAINT CK_Asistencia_Estado CHECK (Estado IN ('A', 'F', 'T'))
);

GO
    -- =========================================================
    -- 5. CARGA INICIAL DE DATOS (SEMMBRADO COMPLETO)
    -- =========================================================
    -- A. Insertar los 4 Roles
INSERT INTO
    Rol (Nombre)
VALUES
    ('Administrador'),
    ('Director'),
    ('Docente'),
    ('Estudiante');

-- B. Insertar Usuarios de Prueba (Claves en SHA-256)
-- 'admin123' -> Soporte / Mantenedor
INSERT INTO
    Usuario (Username, PasswordHash, RolId)
VALUES
    (
        'admin',
        '8c6976e5b5410415bde908bd4dee15dfb167a9c873fc4bb8a81f6f2ab448a918',
        1
    );

-- 'director123' -> Director
INSERT INTO
    Usuario (Username, PasswordHash, RolId)
VALUES
    (
        'director',
        'e0300486c35905d5e5c703e7e2d93e1b7829281a95e7441585ee5e74b3f86e3f',
        2
    );

-- 'docente123' -> Profesor
INSERT INTO
    Usuario (Username, PasswordHash, RolId)
VALUES
    (
        'jgarcia',
        '5e884898da28047151d0e56f8dc6292773603d0d6aabbdd62a11ef721d1542d8',
        3
    );

-- 'alumno123' -> Estudiante / Padre de familia
INSERT INTO
    Usuario (Username, PasswordHash, RolId)
VALUES
    (
        'alumno1',
        '3d0a68d0f1998522d0d0ebbd706037e96b3a32f917a10271d53347528373a00f',
        4
    );

-- C. Insertar Turnos, Grado y los 5 Cursos
INSERT INTO
    Turno (Nombre)
VALUES
    ('Mañana'),
    ('Tarde');

INSERT INTO
    Grado (Nombre)
VALUES
    ('1ro Secundaria');

INSERT INTO
    Curso (Nombre)
VALUES
    ('Matemáticas'),
    ('Comunicación'),
    ('Ciencia y Tecnología'),
    ('Historia y Geografía'),
    ('Inglés');

-- D. Insertar 4 Secciones (2 Mañana, 2 Tarde)
INSERT INTO
    Seccion (GradoId, TurnoId, Nombre)
VALUES
    (1, 1, 'A'),
    -- Turno Mañana
    (1, 1, 'B'),
    -- Turno Mañana
    (1, 2, 'C'),
    -- Turno Tarde
    (1, 2, 'D');

-- Turno Tarde
-- E. Datos del Profesor
INSERT INTO
    Profesor (UsuarioId, DNI, Nombre, Apellido, Email)
VALUES
    (
        3,
        '45879632',
        'Juan',
        'García',
        'jgarcia@colegio.edu.pe'
    );

-- F. Poblar 5 Alumnos por cada salón (Total 20 alumnos)
-- Sección A
INSERT INTO
    Alumno (SeccionId, UsuarioId, DNI, Nombre, Apellido)
VALUES
    (1, 4, '71000001', 'Carlos', 'Mendoza'),
    (1, NULL, '71000002', 'Ana', 'Rojas'),
    (1, NULL, '71000003', 'Luis', 'Torres'),
    (1, NULL, '71000004', 'Maria', 'Sanchez'),
    (1, NULL, '71000005', 'Pedro', 'Gomez');

-- Sección B
INSERT INTO
    Alumno (SeccionId, DNI, Nombre, Apellido)
VALUES
    (2, '72000001', 'Jorge', 'Castro'),
    (2, '72000002', 'Lucia', 'Vargas'),
    (2, '72000003', 'Diego', 'Flores'),
    (2, '72000004', 'Elena', 'Morales'),
    (2, '72000005', 'Raul', 'Gutierrez');

-- Sección C
INSERT INTO
    Alumno (SeccionId, DNI, Nombre, Apellido)
VALUES
    (3, '73000001', 'Sofia', 'Espinoza'),
    (3, '73000002', 'Mateo', 'Arias'),
    (3, '73000003', 'Camila', 'Campos'),
    (3, '73000004', 'Gabriel', 'Reyes'),
    (3, '73000005', 'Valeria', 'Perez');

-- Sección D
INSERT INTO
    Alumno (SeccionId, DNI, Nombre, Apellido)
VALUES
    (4, '74000001', 'Hugo', 'Navarro'),
    (4, '74000002', 'Paula', 'Cordero'),
    (4, '74000003', 'Daniel', 'Salazar'),
    (4, '74000004', 'Andrea', 'Paredes'),
    (4, '74000005', 'Marcos', 'Delgado');

GO
    -- =========================================================
    -- 6. STORED PROCEDURE ACTUALIZADO PARA EL LOGIN JAVA
    -- =========================================================
    CREATE PROCEDURE sp_AutenticarUsuario @Username VARCHAR(50),
    @PasswordHash VARCHAR(64) AS BEGIN
SET
    NOCOUNT ON;

SELECT
    u.UsuarioId,
    u.Username,
    r.Nombre AS Rol,
    CASE
        WHEN r.Nombre = 'Docente' THEN ISNULL(p.Nombre + ' ' + p.Apellido, 'Docente')
        WHEN r.Nombre = 'Estudiante' THEN ISNULL(a.Nombre + ' ' + a.Apellido, 'Estudiante')
        ELSE u.Username
    END AS NombreCompleto
FROM
    Usuario u
    INNER JOIN Rol r ON u.RolId = r.RolId
    LEFT JOIN Profesor p ON u.UsuarioId = p.UsuarioId
    LEFT JOIN Alumno a ON u.UsuarioId = a.UsuarioId
WHERE
    u.Username = @Username
    AND u.PasswordHash = @PasswordHash
    AND u.Estado = 1;

END;

GO