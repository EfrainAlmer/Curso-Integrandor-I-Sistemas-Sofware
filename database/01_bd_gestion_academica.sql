
USE master;
GO
IF DB_ID(N'BD_GestionAcademica_Pitagoras') IS NULL
    CREATE DATABASE BD_GestionAcademica_Pitagoras;
GO
USE BD_GestionAcademica_Pitagoras;
GO

/* ---------- Seguridad ---------- */
CREATE TABLE Rol (
    RolId  INT IDENTITY(1,1) CONSTRAINT PK_Rol PRIMARY KEY,
    Nombre VARCHAR(30) NOT NULL CONSTRAINT UQ_Rol_Nombre UNIQUE
);

CREATE TABLE Usuario (
    UsuarioId           INT IDENTITY(1,1) CONSTRAINT PK_Usuario PRIMARY KEY,
    Username            VARCHAR(50)  NOT NULL CONSTRAINT UQ_Usuario_Username UNIQUE,
    PasswordHash        VARCHAR(100) NOT NULL,              -- BCrypt costo 12 (RNF02)
    RolId               INT          NOT NULL CONSTRAINT FK_Usuario_Rol REFERENCES Rol(RolId),
    Estado              BIT          NOT NULL CONSTRAINT DF_Usuario_Estado DEFAULT 1,   -- 0 = dado de baja (RF03)
    DebeCambiarPassword BIT          NOT NULL CONSTRAINT DF_Usuario_DebeCambiar DEFAULT 1,
    IntentosFallidos    INT          NOT NULL CONSTRAINT DF_Usuario_Intentos DEFAULT 0,
    BloqueadoHasta      DATETIME2    NULL,
    CreadoEl            DATETIME2    NOT NULL CONSTRAINT DF_Usuario_Creado DEFAULT SYSDATETIME()
);

/* ---------- Estructura del colegio ---------- */
CREATE TABLE Turno (
    TurnoId INT IDENTITY(1,1) CONSTRAINT PK_Turno PRIMARY KEY,
    Nombre  VARCHAR(20) NOT NULL CONSTRAINT UQ_Turno_Nombre UNIQUE
);

CREATE TABLE Grado (
    GradoId INT IDENTITY(1,1) CONSTRAINT PK_Grado PRIMARY KEY,
    Nombre  VARCHAR(50) NOT NULL CONSTRAINT UQ_Grado_Nombre UNIQUE
);

CREATE TABLE Seccion (
    SeccionId INT IDENTITY(1,1) CONSTRAINT PK_Seccion PRIMARY KEY,
    GradoId   INT     NOT NULL CONSTRAINT FK_Seccion_Grado REFERENCES Grado(GradoId),
    TurnoId   INT     NOT NULL CONSTRAINT FK_Seccion_Turno REFERENCES Turno(TurnoId),
    Nombre    CHAR(1) NOT NULL,
    CONSTRAINT UQ_Seccion_Grado_Nombre UNIQUE (GradoId, Nombre)
);

/* ---------- Personas ---------- */
CREATE TABLE Alumno (
    AlumnoId          INT IDENTITY(1,1) CONSTRAINT PK_Alumno PRIMARY KEY,
    SeccionId         INT         NOT NULL CONSTRAINT FK_Alumno_Seccion REFERENCES Seccion(SeccionId),
    UsuarioId         INT         NULL     CONSTRAINT FK_Alumno_Usuario REFERENCES Usuario(UsuarioId), -- cuenta del apoderado
    DNI               CHAR(8)     NOT NULL CONSTRAINT UQ_Alumno_DNI UNIQUE,
    Nombre            VARCHAR(50) NOT NULL,
    Apellido          VARCHAR(50) NOT NULL,
    NombreApoderado   VARCHAR(100) NOT NULL,
    TelefonoApoderado VARCHAR(20) NULL,
    Estado            VARCHAR(20) NOT NULL CONSTRAINT DF_Alumno_Estado DEFAULT 'ACTIVO',
    CONSTRAINT CK_Alumno_Estado CHECK (Estado IN ('ACTIVO','RETIRADO'))   -- baja lógica (RF04)
);
-- Una cuenta de apoderado por alumno; el índice filtrado permite varios alumnos sin cuenta (NULL).
CREATE UNIQUE INDEX UQ_Alumno_Usuario ON Alumno(UsuarioId) WHERE UsuarioId IS NOT NULL;

CREATE TABLE Docente (
    DocenteId INT IDENTITY(1,1) CONSTRAINT PK_Docente PRIMARY KEY,
    UsuarioId INT          NOT NULL CONSTRAINT UQ_Docente_Usuario UNIQUE
                           CONSTRAINT FK_Docente_Usuario REFERENCES Usuario(UsuarioId),
    DNI       CHAR(8)      NOT NULL CONSTRAINT UQ_Docente_DNI UNIQUE,
    Nombre    VARCHAR(50)  NOT NULL,
    Apellido  VARCHAR(50)  NOT NULL,
    Email     VARCHAR(100) NULL
);

/* ---------- Académico ---------- */
CREATE TABLE Curso (                                        -- hace de área curricular
    CursoId INT IDENTITY(1,1) CONSTRAINT PK_Curso PRIMARY KEY,
    Nombre  VARCHAR(100) NOT NULL CONSTRAINT UQ_Curso_Nombre UNIQUE
);

CREATE TABLE Competencia (                                  -- los títulos de la grilla de notas salen de aquí
    CursoId INT          NOT NULL CONSTRAINT FK_Competencia_Curso REFERENCES Curso(CursoId),
    Codigo  CHAR(1)      NOT NULL,                          -- '1' a '4' (tipo char por requisito del curso)
    Nombre  VARCHAR(150) NOT NULL,
    CONSTRAINT PK_Competencia PRIMARY KEY (CursoId, Codigo),
    CONSTRAINT CK_Competencia_Codigo CHECK (Codigo LIKE '[1-4]')
);

CREATE TABLE CursoSeccion (                                 -- carga académica (RF06)
    CursoSeccionId INT IDENTITY(1,1) CONSTRAINT PK_CursoSeccion PRIMARY KEY,
    CursoId   INT NOT NULL CONSTRAINT FK_CursoSeccion_Curso   REFERENCES Curso(CursoId),
    SeccionId INT NOT NULL CONSTRAINT FK_CursoSeccion_Seccion REFERENCES Seccion(SeccionId),
    DocenteId INT NOT NULL CONSTRAINT FK_CursoSeccion_Docente REFERENCES Docente(DocenteId),
    CONSTRAINT UQ_CursoSeccion_Curso_Seccion UNIQUE (CursoId, SeccionId)
);

CREATE TABLE Periodo (
    PeriodoId           INT IDENTITY(1,1) CONSTRAINT PK_Periodo PRIMARY KEY,
    AnioLectivo         INT     NOT NULL,
    BimestreNum         TINYINT NOT NULL,
    FechaInicio         DATE    NOT NULL,
    FechaFin            DATE    NOT NULL,
    Cerrado             BIT     NOT NULL CONSTRAINT DF_Periodo_Cerrado DEFAULT 0,
    FechaCierre         DATETIME2 NULL,
    CerradoPorUsuarioId INT     NULL CONSTRAINT FK_Periodo_Usuario REFERENCES Usuario(UsuarioId),
    CONSTRAINT UQ_Periodo_Anio_Bimestre UNIQUE (AnioLectivo, BimestreNum),
    CONSTRAINT CK_Periodo_Bimestre CHECK (BimestreNum BETWEEN 1 AND 4),
    CONSTRAINT CK_Periodo_Fechas CHECK (FechaFin > FechaInicio)
);

CREATE TABLE Evaluacion (                                   -- una por periodo, curso-sección y competencia
    EvaluacionId      INT IDENTITY(1,1) CONSTRAINT PK_Evaluacion PRIMARY KEY,
    PeriodoId         INT     NOT NULL CONSTRAINT FK_Evaluacion_Periodo REFERENCES Periodo(PeriodoId),
    CursoSeccionId    INT     NOT NULL CONSTRAINT FK_Evaluacion_CursoSeccion REFERENCES CursoSeccion(CursoSeccionId),
    CodigoCompetencia CHAR(1) NOT NULL,
    CONSTRAINT UQ_Evaluacion UNIQUE (PeriodoId, CursoSeccionId, CodigoCompetencia),
    CONSTRAINT CK_Evaluacion_Codigo CHECK (CodigoCompetencia LIKE '[1-4]')
);

CREATE TABLE Nota (
    AlumnoId     INT     NOT NULL CONSTRAINT FK_Nota_Alumno REFERENCES Alumno(AlumnoId),
    EvaluacionId INT     NOT NULL CONSTRAINT FK_Nota_Evaluacion REFERENCES Evaluacion(EvaluacionId),
    Calificativo CHAR(2) NOT NULL,                          -- char[] en la clase Nota (requisito del curso)
    Fecha        DATETIME2 NOT NULL CONSTRAINT DF_Nota_Fecha DEFAULT SYSDATETIME(),
    CONSTRAINT PK_Nota PRIMARY KEY (AlumnoId, EvaluacionId),
    CONSTRAINT CK_Nota_Calificativo CHECK (Calificativo IN ('AD','A','B','C'))
);

CREATE TABLE Asistencia (
    AlumnoId       INT     NOT NULL CONSTRAINT FK_Asistencia_Alumno REFERENCES Alumno(AlumnoId),
    CursoSeccionId INT     NOT NULL CONSTRAINT FK_Asistencia_CursoSeccion REFERENCES CursoSeccion(CursoSeccionId),
    Fecha          DATE    NOT NULL,
    Estado         CHAR(1) NOT NULL,                        -- A asistió, F falta, T tardanza
    Justificada    BIT     NOT NULL CONSTRAINT DF_Asistencia_Justificada DEFAULT 0,
    Observacion    VARCHAR(150) NULL,
    CONSTRAINT PK_Asistencia PRIMARY KEY (AlumnoId, CursoSeccionId, Fecha),
    CONSTRAINT CK_Asistencia_Estado CHECK (Estado IN ('A','F','T')),
    CONSTRAINT CK_Asistencia_Justificada CHECK (Justificada = 0 OR Estado = 'F')   -- falta justificada conserva Estado F
);

CREATE TABLE DetalleRendimiento (                           -- se llena al cerrar el periodo (RF07)
    AlumnoId            INT     NOT NULL CONSTRAINT FK_Detalle_Alumno REFERENCES Alumno(AlumnoId),
    CursoSeccionId      INT     NOT NULL CONSTRAINT FK_Detalle_CursoSeccion REFERENCES CursoSeccion(CursoSeccionId),
    PeriodoId           INT     NOT NULL CONSTRAINT FK_Detalle_Periodo REFERENCES Periodo(PeriodoId),
    Calificativo        CHAR(2) NOT NULL,
    TotalAsistencias    INT     NOT NULL CONSTRAINT DF_Detalle_Asis DEFAULT 0,
    TotalFaltas         INT     NOT NULL CONSTRAINT DF_Detalle_Faltas DEFAULT 0,
    TotalTardanzas      INT     NOT NULL CONSTRAINT DF_Detalle_Tard DEFAULT 0,
    TotalJustificadas   INT     NOT NULL CONSTRAINT DF_Detalle_Just DEFAULT 0,
    EstadoAprobacion    VARCHAR(20) NOT NULL,
    UltimaActualizacion DATETIME2 NOT NULL CONSTRAINT DF_Detalle_Act DEFAULT SYSDATETIME(),
    CONSTRAINT PK_DetalleRendimiento PRIMARY KEY (AlumnoId, CursoSeccionId, PeriodoId),
    CONSTRAINT CK_Detalle_Calificativo CHECK (Calificativo IN ('AD','A','B','C'))
);

/* ---------- Auditoría, respaldo y accesos ---------- */
CREATE TABLE Auditoria (
    AuditoriaId   INT IDENTITY(1,1) CONSTRAINT PK_Auditoria PRIMARY KEY,
    Tabla         VARCHAR(50)  NOT NULL,
    ClavePrimaria VARCHAR(100) NOT NULL,
    UsuarioId     INT          NOT NULL CONSTRAINT FK_Auditoria_Usuario REFERENCES Usuario(UsuarioId),
    FechaCambio   DATETIME2    NOT NULL CONSTRAINT DF_Auditoria_Fecha DEFAULT SYSDATETIME(),
    ValorAnterior VARCHAR(200) NULL,
    ValorNuevo    VARCHAR(200) NOT NULL,
    Accion        VARCHAR(30)  NOT NULL,
    CONSTRAINT CK_Auditoria_Accion CHECK (Accion IN ('MODIFICAR','CERRAR_PERIODO','REABRIR_PERIODO','BACKUP','RESTAURAR'))
);

CREATE TABLE CopiaSeguridad (                               -- RF10
    CopiaId   INT IDENTITY(1,1) CONSTRAINT PK_CopiaSeguridad PRIMARY KEY,
    Ruta      VARCHAR(260) NOT NULL,
    Fecha     DATETIME2    NOT NULL CONSTRAINT DF_Copia_Fecha DEFAULT SYSDATETIME(),
    UsuarioId INT          NOT NULL CONSTRAINT FK_Copia_Usuario REFERENCES Usuario(UsuarioId),
    Estado    VARCHAR(20)  NOT NULL,
    CONSTRAINT CK_Copia_Estado CHECK (Estado IN ('EXITOSO','FALLIDO'))
);

CREATE TABLE IntentoAcceso (                                -- bitácora de accesos (RNF04); nunca guarda contraseñas
    IntentoId INT IDENTITY(1,1) CONSTRAINT PK_IntentoAcceso PRIMARY KEY,
    Username  VARCHAR(50) NOT NULL,                         -- sin FK: puede ser un usuario inexistente
    Fecha     DATETIME2   NOT NULL CONSTRAINT DF_Intento_Fecha DEFAULT SYSDATETIME(),
    Resultado VARCHAR(20) NOT NULL,
    CONSTRAINT CK_Intento_Resultado CHECK (Resultado IN ('EXITOSO','FALLIDO','BLOQUEADO'))
);
GO

/* ---------- Índices (RNF05) ----------
   Nota(AlumnoId) y Evaluacion(PeriodoId, CursoSeccionId) ya quedan cubiertos por
   PK_Nota y UQ_Evaluacion, que empiezan con esas mismas columnas. */
CREATE INDEX IX_Nota_Evaluacion        ON Nota(EvaluacionId);
CREATE INDEX IX_Asistencia_Curso_Fecha ON Asistencia(CursoSeccionId, Fecha);
CREATE INDEX IX_Alumno_Seccion         ON Alumno(SeccionId);
CREATE INDEX IX_Auditoria_Fecha        ON Auditoria(FechaCambio);
GO

/* ===================== DATOS SEMILLA ===================== */
INSERT INTO Rol (Nombre) VALUES ('DIRECTOR'), ('DOCENTE'), ('APODERADO');
INSERT INTO Turno (Nombre) VALUES (N'Mañana'), (N'Tarde');
INSERT INTO Grado (Nombre) VALUES (N'1.°'), (N'2.°'), (N'3.°'), (N'4.°'), (N'5.°');

-- Una sección A por grado, turno Mañana
INSERT INTO Seccion (GradoId, TurnoId, Nombre)
SELECT g.GradoId, t.TurnoId, 'A'
FROM Grado g CROSS JOIN Turno t
WHERE t.Nombre = N'Mañana';

-- Usuarios de prueba (solo hashes BCrypt costo 12; las claves están en el README)
INSERT INTO Usuario (Username, PasswordHash, RolId) VALUES
 ('director',   '$2b$12$4mfMY/jJqreGPFKzymAdNun685tZTxh3tAgvJsLT0j56QfGD50MzO',   (SELECT RolId FROM Rol WHERE Nombre = 'DIRECTOR')),
 ('docente1',   '$2b$12$OoB/iu03oHIUjMOoJL0nceXRiKvtvsksAtHTavZwr2mryE1UFqKUW',   (SELECT RolId FROM Rol WHERE Nombre = 'DOCENTE')),
 ('docente2',   '$2b$12$47DeEJCwo0HOzwjc9cZUautoVeOGZinSM2FF5JsNC0mAOoy47gvdK',   (SELECT RolId FROM Rol WHERE Nombre = 'DOCENTE')),
 ('apoderado1', '$2b$12$dGgWh/VZ.txTGS8/S.J0tuke4xXrPI631jFBtwFZDyQruMyaTe0nW', (SELECT RolId FROM Rol WHERE Nombre = 'APODERADO'));

-- Docentes de prueba (datos ficticios)
INSERT INTO Docente (UsuarioId, DNI, Nombre, Apellido, Email) VALUES
 ((SELECT UsuarioId FROM Usuario WHERE Username = 'docente1'), '40000001', 'Rosa',  'Prueba Uno', NULL),
 ((SELECT UsuarioId FROM Usuario WHERE Username = 'docente2'), '40000002', 'Carlos','Prueba Dos', NULL);

-- Cursos y competencias (Currículo Nacional)
INSERT INTO Curso (Nombre) VALUES (N'Matemática'), (N'Comunicación'), (N'Ciencia y Tecnología');

INSERT INTO Competencia (CursoId, Codigo, Nombre)
SELECT c.CursoId, v.Codigo, v.Nombre
FROM (VALUES
 (N'Matemática',         '1', N'Resuelve problemas de cantidad'),
 (N'Matemática',         '2', N'Resuelve problemas de regularidad, equivalencia y cambio'),
 (N'Matemática',         '3', N'Resuelve problemas de forma, movimiento y localización'),
 (N'Matemática',         '4', N'Resuelve problemas de gestión de datos e incertidumbre'),
 (N'Comunicación',       '1', N'Se comunica oralmente en su lengua materna'),
 (N'Comunicación',       '2', N'Lee diversos tipos de textos escritos'),
 (N'Comunicación',       '3', N'Escribe diversos tipos de textos'),
 (N'Ciencia y Tecnología','1', N'Indaga mediante métodos científicos'),
 (N'Ciencia y Tecnología','2', N'Explica el mundo físico basándose en conocimientos'),
 (N'Ciencia y Tecnología','3', N'Diseña y construye soluciones tecnológicas')
) AS v(Curso, Codigo, Nombre)
JOIN Curso c ON c.Nombre = v.Curso;

-- Carga académica de prueba: 1.° A
INSERT INTO CursoSeccion (CursoId, SeccionId, DocenteId)
SELECT c.CursoId,
       (SELECT s.SeccionId FROM Seccion s JOIN Grado g ON g.GradoId = s.GradoId
        WHERE g.Nombre = N'1.°' AND s.Nombre = 'A'),
       CASE WHEN c.Nombre = N'Ciencia y Tecnología'
            THEN (SELECT DocenteId FROM Docente WHERE DNI = '40000002')
            ELSE (SELECT DocenteId FROM Docente WHERE DNI = '40000001') END
FROM Curso c;

-- Los 4 bimestres de 2026 (fechas referenciales: ajústalas desde la app, RF05)
INSERT INTO Periodo (AnioLectivo, BimestreNum, FechaInicio, FechaFin) VALUES
 (2026, 1, '2026-03-16', '2026-05-15'),
 (2026, 2, '2026-05-25', '2026-07-31'),
 (2026, 3, '2026-08-10', '2026-10-09'),
 (2026, 4, '2026-10-19', '2026-12-18');

-- Evaluaciones: una por periodo, curso-sección y competencia
INSERT INTO Evaluacion (PeriodoId, CursoSeccionId, CodigoCompetencia)
SELECT p.PeriodoId, cs.CursoSeccionId, k.Codigo
FROM Periodo p
CROSS JOIN CursoSeccion cs
JOIN Competencia k ON k.CursoId = cs.CursoId;

-- Alumnos de prueba en 1.° A (datos ficticios); el primero tiene la cuenta apoderado1
INSERT INTO Alumno (SeccionId, UsuarioId, DNI, Nombre, Apellido, NombreApoderado, TelefonoApoderado)
SELECT (SELECT s.SeccionId FROM Seccion s JOIN Grado g ON g.GradoId = s.GradoId
        WHERE g.Nombre = N'1.°' AND s.Nombre = 'A'),
       v.UsuarioId, v.DNI, v.Nombre, v.Apellido, v.Apoderado, v.Telefono
FROM (VALUES
 ((SELECT UsuarioId FROM Usuario WHERE Username = 'apoderado1'), '70000001', 'Luis',   'Alumno Prueba Uno',    'Apoderado Prueba Uno',    '900000001'),
 (CAST(NULL AS INT), '70000002', 'Ana',    'Alumno Prueba Dos',    'Apoderado Prueba Dos',    '900000002'),
 (CAST(NULL AS INT), '70000003', 'Mateo',  'Alumno Prueba Tres',   'Apoderado Prueba Tres',   '900000003'),
 (CAST(NULL AS INT), '70000004', 'Lucía',  'Alumno Prueba Cuatro', 'Apoderado Prueba Cuatro', '900000004'),
 (CAST(NULL AS INT), '70000005', 'Diego',  'Alumno Prueba Cinco',  'Apoderado Prueba Cinco',  '900000005')
) AS v(UsuarioId, DNI, Nombre, Apellido, Apoderado, Telefono);
GO

/* ---------- Verificación rápida ---------- */
SELECT 'Rol' AS Tabla, COUNT(*) AS Filas FROM Rol
UNION ALL SELECT 'Usuario', COUNT(*) FROM Usuario
UNION ALL SELECT 'Seccion', COUNT(*) FROM Seccion
UNION ALL SELECT 'Competencia', COUNT(*) FROM Competencia
UNION ALL SELECT 'CursoSeccion', COUNT(*) FROM CursoSeccion
UNION ALL SELECT 'Periodo', COUNT(*) FROM Periodo
UNION ALL SELECT 'Evaluacion', COUNT(*) FROM Evaluacion
UNION ALL SELECT 'Alumno', COUNT(*) FROM Alumno;
GO