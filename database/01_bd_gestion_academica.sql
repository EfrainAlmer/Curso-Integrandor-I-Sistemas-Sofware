/* =====================================================================
   BD_GestionAcademica_Pitagoras - Sistema de Gestión Académica
   Ejecutar completo en SSMS (F5). Si la base ya existe, se elimina y se
   vuelve a crear con los datos de prueba. Luego ejecute 02_usuario_aplicacion.sql.
   ===================================================================== */
USE master;
GO
IF DB_ID(N'BD_GestionAcademica_Pitagoras') IS NOT NULL
BEGIN
    ALTER DATABASE BD_GestionAcademica_Pitagoras SET SINGLE_USER WITH ROLLBACK IMMEDIATE;
    DROP DATABASE BD_GestionAcademica_Pitagoras;
END
GO
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

CREATE TABLE DocenteArea (                                  -- especialidad: áreas que puede dictar (1 a 3)
    DocenteId INT NOT NULL CONSTRAINT FK_DocenteArea_Docente REFERENCES Docente(DocenteId),
    CursoId   INT NOT NULL CONSTRAINT FK_DocenteArea_Curso   REFERENCES Curso(CursoId),
    CONSTRAINT PK_DocenteArea PRIMARY KEY (DocenteId, CursoId)
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

CREATE TABLE ConclusionDescriptiva (                        -- logros, dificultades y recomendaciones (libreta)
    AlumnoId     INT          NOT NULL CONSTRAINT FK_Conclusion_Alumno REFERENCES Alumno(AlumnoId),
    EvaluacionId INT          NOT NULL CONSTRAINT FK_Conclusion_Evaluacion REFERENCES Evaluacion(EvaluacionId),
    Texto        VARCHAR(500) NOT NULL,
    Fecha        DATETIME2    NOT NULL CONSTRAINT DF_Conclusion_Fecha DEFAULT SYSDATETIME(),
    CONSTRAINT PK_ConclusionDescriptiva PRIMARY KEY (AlumnoId, EvaluacionId)
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
    ValorAnterior VARCHAR(300) NULL,
    ValorNuevo    VARCHAR(300) NOT NULL,
    Accion        VARCHAR(30)  NOT NULL,
    CONSTRAINT CK_Auditoria_Accion CHECK (Accion IN ('REGISTRAR','MODIFICAR','CERRAR_PERIODO','REABRIR_PERIODO',
        'BACKUP','RESTAURAR','CREAR_USUARIO','ESTADO_USUARIO','RESTABLECER_CLAVE','INSCRIBIR','ACTUALIZAR_ALUMNO',
        'ASIGNAR_CARGA','CONFIGURAR','ACTUALIZAR_DOCENTE','CONCLUSION'))
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
-- Todos los datos son ficticios. Clave de todas las cuentas de prueba: Pitagoras2026 (ver README).
INSERT INTO Rol (Nombre) VALUES ('DIRECTOR'), ('DOCENTE'), ('APODERADO');
INSERT INTO Turno (Nombre) VALUES (N'Mañana'), (N'Tarde');
INSERT INTO Grado (Nombre) VALUES (N'1.°'), (N'2.°'), (N'3.°'), (N'4.°'), (N'5.°');

-- 20 secciones: A y B en la mañana, C y D en la tarde
INSERT INTO Seccion (GradoId, TurnoId, Nombre)
SELECT g.GradoId, t.TurnoId, v.Letra
FROM Grado g
CROSS JOIN (VALUES ('A', N'Mañana'), ('B', N'Mañana'), ('C', N'Tarde'), ('D', N'Tarde')) AS v(Letra, Turno)
JOIN Turno t ON t.Nombre = v.Turno;

-- Cuentas (hash BCrypt costo 12; cada una con su propia sal)
INSERT INTO Usuario (Username, PasswordHash, RolId, DebeCambiarPassword)
SELECT v.Username, v.Hash, r.RolId, v.DebeCambiar
FROM (VALUES
 ('director', '$2b$12$4eHmK4XQqVWYfnxPnQVTo.A1kh3IrdQzbEPfC9YmlPUtfAvCPunKW', 'DIRECTOR', 0),
 ('coordinador', '$2b$12$158xtF2AIJ7q3um33u6NTOZQ5u.rSDQfINmqnXHwGYsVCUzoee3Z2', 'DIRECTOR', 0),
 ('docente01', '$2b$12$UeFAA93TpaREByVH1xdL9ecl2dQdOSYetyREJQ4dO4ADjpoCNTFb.', 'DOCENTE', 0),
 ('docente02', '$2b$12$4temfs0UknRZYB3bcpprQuDUFVicdYgB.QsXdYhx7.uD88o0SqLqW', 'DOCENTE', 0),
 ('docente03', '$2b$12$MZ2igfzCWJH1zynY/EmzGeIhGLJOe3I9C.k0/Dfnd4Gywujx5mHRe', 'DOCENTE', 0),
 ('docente04', '$2b$12$rf7VsWWJkt6ADxrcyuCRfeuG8aYCGhORflso17dENTk.p6E0JzcpW', 'DOCENTE', 0),
 ('docente05', '$2b$12$z9R0Y1akUgrmPGxIakxna.GHARg9w4qwpw25WURk51xhhpjkVFPNK', 'DOCENTE', 0),
 ('docente06', '$2b$12$RozbdDLCYwfticJBeuC8pO6UC6IiidBOanyWRyUZXFuhwD/qjRC5W', 'DOCENTE', 0),
 ('docente07', '$2b$12$FrNlcrkAw3FWuNL7uoKBe.8dtSMSoEbIF5xVeJweWDaau2PEFbbFS', 'DOCENTE', 0),
 ('docente08', '$2b$12$IoJjwW3FQ2MhRk1dArb1i.aBuPWzzit.Lclmt72tGb1BCj1.QNf82', 'DOCENTE', 0),
 ('docente09', '$2b$12$Z6Q0ctZb2u1HPvD.kW7OR.5VSa.UiB6KAHhBfxhQM.STpDg4bU0we', 'DOCENTE', 0),
 ('docente10', '$2b$12$hvUwJF3lZOlGowh9oxP0ceMZB8tLlvlVOPbm2/5sBzMuSvqK7pVwC', 'DOCENTE', 0),
 ('apo70000001', '$2b$12$48tCfoIBFwU6hr2HlXn1K.SS.CYfMC9c8qoDWqyE0h6IQKBQOMX6y', 'APODERADO', 0),
 ('apo70000002', '$2b$12$UawwlAOOLVxKAYM6aqnffu2CiZiHog.IKcYfI0fjfVmppjyE12bIG', 'APODERADO', 1),
 ('apo70000003', '$2b$12$oo7iFjhAwf2v.yppFS8/NeHf/Ii3u1dQ2g8nplqlAhvZ3TVy89/K2', 'APODERADO', 0),
 ('apo70000004', '$2b$12$ztmgBIPhwp0tlICEUDdD0eK.0ss.f1zfVTXbuCKh1HmuqHkDgssVi', 'APODERADO', 0),
 ('apo70000005', '$2b$12$CXK6xsAPO4VooiwP0xuSgueCo.0Kce3tcML.YVkF5Q3QD4ySl.Lvy', 'APODERADO', 0),
 ('apo70000006', '$2b$12$NgO/AWlEYpCPwBCsghOU6..T40H.kgJywFapx9SWbhPpTzFAGIT4e', 'APODERADO', 0),
 ('apo70000007', '$2b$12$3thhPzWVU/ff0d8RKj8J1.BCzrcNFQhBex3excSKSLlgqS/k0h1sq', 'APODERADO', 0),
 ('apo70000008', '$2b$12$PSpquZsYS7x65mt68RPL5.4prpl5epADImqLnARuXWW0yYpf.8pLa', 'APODERADO', 0),
 ('apo70000009', '$2b$12$IYyQ43YG/3GnMfn//MB.s.wqISSfsOSHbEJamWUvxkpZq7k8EXmL2', 'APODERADO', 0),
 ('apo70000010', '$2b$12$aJstUSo2ssMOU/WIxHDAie8kUeTNigiGZIZ36IWo9.vhMVF..y65C', 'APODERADO', 0),
 ('apo70000011', '$2b$12$JBA9/4ZvC9BpeHJoxfKyz.VXTTXs3Zl9Vz.VxfOI9a4j7qJqwjehm', 'APODERADO', 0),
 ('apo70000012', '$2b$12$/JJI7/mAPzqjMWS6LzVQMueGF5O.MLn0oGt59xoL8T/T.PaqCcnDK', 'APODERADO', 0),
 ('apo70000013', '$2b$12$/E3QfbBOVTTwJvXZf3LcDu/101kDK7pf6ANxVcEPVLDm7FdVX4Yky', 'APODERADO', 0),
 ('apo70000014', '$2b$12$q9Syfz/l7b7JjS7cbwLguuUcKeiJRnefNhXCaO6RMCVC6v.8CHDaO', 'APODERADO', 0),
 ('apo70000015', '$2b$12$2dJYWz2YLP3fx2ml3z61/.iwM5pFkmrREM1ba8uWAzS1vLkaN2RNS', 'APODERADO', 0),
 ('apo70000016', '$2b$12$zr0gtuEwgANwlsQDihX6deQuRrT/goT29.EuEoEsuNQMFRj2/yRDm', 'APODERADO', 0),
 ('apo70000017', '$2b$12$vZadr5eFQp/hRduhTPr5x.tekqUSKXd2Ej0ZguBGzVXyBYx6w7sbS', 'APODERADO', 0),
 ('apo70000018', '$2b$12$55A7AgbdStiJ.rxee84DBuli8FORuYQe03v6v7s2Y4Ntwdvi1ut.y', 'APODERADO', 0),
 ('apo70000019', '$2b$12$OXgJYT1CbNe/9HHcxEt3U.5rI.Q9NQ54AI5MwOmsMgOL9cM3NN.ci', 'APODERADO', 0),
 ('apo70000020', '$2b$12$3OEhEqqtAhtMOamfIl7JMufilLpVAN5pGwVjGYBmMarH7woszKnIS', 'APODERADO', 0),
 ('apo70000021', '$2b$12$MtaET8zVrRURxIuZL/j3BOcDDv9jsNwvOMONgNelulw/KwyAFkqlG', 'APODERADO', 0),
 ('apo70000022', '$2b$12$1O.6U1ybrm5KDzKUP0KSFuR6tlZE0GaA7BFrHKgh6eIuTle5PzeHa', 'APODERADO', 0),
 ('apo70000023', '$2b$12$giody9E1GjdQ.L39p/OxdOIEYdU8JFRwReS.lG.KgQeN9xRtHQo8a', 'APODERADO', 0),
 ('apo70000024', '$2b$12$b0LhrjxVwJ8kuBbP/ddkPuTStVEhjsbeRQawD2HYxLHauXUqD9kkC', 'APODERADO', 0),
 ('apo70000025', '$2b$12$e.lWecAAscQzZPN1BXcDBO1qcbwF/K/b6wkKM8NjV/ZEbsCZedorO', 'APODERADO', 0),
 ('apo70000026', '$2b$12$W1KCGKCk7KAH0537yCi9e.BSynXpbkp/OTkJDmfQPmAUh/S7U8GoG', 'APODERADO', 0),
 ('apo70000027', '$2b$12$QLJDriQS.s2AEbpGq5SYyuWp3MyBOAk34DLirfe3QyiM9pTkebUK2', 'APODERADO', 0),
 ('apo70000028', '$2b$12$xSPqRv7enx6TC2sdfIlfu.xEabwIdMTweegVWczE3CMx04OOZixrO', 'APODERADO', 0),
 ('apo70000029', '$2b$12$EB5R3jNrdlMQvQfx7vcV4eJb0zG2KPoSfjLhIlnf.Vv39ubgtvY6.', 'APODERADO', 0),
 ('apo70000030', '$2b$12$8VznVi33PzWg5JUH610J0uik3BNgKHNhjrxrFZ3GopdpndLgo9XEG', 'APODERADO', 0),
 ('apo70000031', '$2b$12$6B9clVInuWDIsn/Z3yJwl.S2s34lLBaPfIL3jRTevRv8v5SZvzu5e', 'APODERADO', 0),
 ('apo70000032', '$2b$12$sHB5E/nSc525C0pCGtAHIe69J9eux2g8H.kVhe.JPoerdlXUg27eu', 'APODERADO', 0),
 ('apo70000033', '$2b$12$90/GqwEfwaclPuF/NvG7TOihoYuwXNC/K/0u6AZ2ARA4Kg0MfaeCO', 'APODERADO', 0),
 ('apo70000034', '$2b$12$BKZ7P8/W6G4Zi4Yi.hKzLO92hE2lGVvE1FdeDuaqPZjgdSJRvCZau', 'APODERADO', 0),
 ('apo70000035', '$2b$12$M5HLW2DC2go7FulY3TB4IuYQZUoTLQsHbiQOPUZj2jOtrUkXLBIw2', 'APODERADO', 0),
 ('apo70000036', '$2b$12$dMM6NqBAC6dcI5sup2OK6OaHH3Ore86/J5Mh8zUS49jZ9VLXoO8je', 'APODERADO', 0),
 ('apo70000037', '$2b$12$IKzwpZlH.5tDE09kx/Kh3OshoA5TrWT5MTFKqF1ekofRtbbdsn6rG', 'APODERADO', 0),
 ('apo70000038', '$2b$12$fZPNQXdLHOIHDoKOjYcTJOFTfQr.yfv1MI/RfF3uSbJfTuknFijo.', 'APODERADO', 0),
 ('apo70000039', '$2b$12$MOjp/jtvV818xaj39/PW9u/2HKDUfbzSSR92mouQvT0t9Gi/5Ix4q', 'APODERADO', 0),
 ('apo70000040', '$2b$12$5Bj9oPLGrwPtRtOg/NuK7.pJkxTPeEsfiWAq5zfAq19B5etDIROoq', 'APODERADO', 0),
 ('apo70000041', '$2b$12$ItvfBoNjaXed13I.p5FieudVu9XGXClBV8Y27U8MRGlT9UT8kRDUS', 'APODERADO', 0),
 ('apo70000042', '$2b$12$UjMLyTovUt5Cb7RoMT0bQuG6rd5O/wcXphNUrGMPqJ5slkvD845JW', 'APODERADO', 0),
 ('apo70000043', '$2b$12$hBB8HARIRNUANx1z0ezKwuP0LbXBg8OiKd.nYZ8OB.jtqSRj8/rUC', 'APODERADO', 0),
 ('apo70000044', '$2b$12$e/vMNsnz2rd2yXgFN8UaW.0.mzNSvJ0bWEADynjQLyKfgml.Sznea', 'APODERADO', 0),
 ('apo70000045', '$2b$12$cKcAKnij6iMb5Lg4vg.peO7F2G0WrmcKe.jNnCGSHX4GwO9nxNO7.', 'APODERADO', 0),
 ('apo70000046', '$2b$12$2X7zR4x70rMJnYov518goOioh1bpIhftjJSIBB4K0UST0/G1kcCra', 'APODERADO', 0),
 ('apo70000047', '$2b$12$R.pab6HSEkjDbY/qYJxcT.BH1cysOHkyxvoqqIK.GBEx2W2MS6c5q', 'APODERADO', 0),
 ('apo70000048', '$2b$12$vi.7TbODKk522TBDcwVBzeysS78w/tI7/NvDZXKZzT9kOAqJtuSzW', 'APODERADO', 0),
 ('apo70000049', '$2b$12$6Evgcm7kt1OSt9btL3VkVehvHDbyJAP1zkUMAcm8jIgCbFOVaCgBq', 'APODERADO', 0),
 ('apo70000050', '$2b$12$n.s7WRCGlTx/kwQfL/6YTehjtKTx60ap8b3DUCcZHfMIxo0Pd4bHy', 'APODERADO', 0),
 ('apo70000051', '$2b$12$D87QH2eFTvokqdppMklsxuJ1AY6Rq.6Wn.zec7dkAYVHRqXDbanz6', 'APODERADO', 0),
 ('apo70000052', '$2b$12$htftHgD66lRnXONEgVJbDuzMTCLqSQGKa.0ZUbS/rncmKEeiyhqVK', 'APODERADO', 0),
 ('apo70000053', '$2b$12$hhTVBee.Furb4Zi.1yN6H.twzCvM0ox69OIcNe30RG0FLnNwAPYLW', 'APODERADO', 0),
 ('apo70000054', '$2b$12$CQ.SgJHgaupjcbZAdePLUOe6ngesHIMPfCWqdzRgU7G0zLVU/yKJy', 'APODERADO', 0),
 ('apo70000055', '$2b$12$PF3EyyS0ynFKXsbVyPfGPuN.iVMLXgFo/.UnwmVsp8JIzvqWWKoCe', 'APODERADO', 0),
 ('apo70000056', '$2b$12$dTahBtlU5X9iE5yVxXQQL.8KV/NF/pFMZOmSRON8QqWY09jDKLhMC', 'APODERADO', 0),
 ('apo70000057', '$2b$12$JqZJw.J215A4OjKbJ/H28.uw4P1pPedpSuz3.epNNdQKZsoIF8SYi', 'APODERADO', 0),
 ('apo70000058', '$2b$12$ocWXbrRPGBUxVXR.Usj3HeLYhyhlCNJtEaNzWXi1h9ztSIcODflPC', 'APODERADO', 0),
 ('apo70000059', '$2b$12$seVyH39pJRx3N1DhDm5Z8eLbI69mRmDnPZiZadfOaawbSNXeiTk/K', 'APODERADO', 0),
 ('apo70000060', '$2b$12$laZNOtmI7GGuQz6mCPUHFurx5oSVjioMZUjhsaLH0mQAGPzGCBVna', 'APODERADO', 0),
 ('apo70000061', '$2b$12$AfnaR7JKhfCYetyKTfgTEuQz6fiUUQkMN.VxmqH1ORbMQOUp7qPZK', 'APODERADO', 0),
 ('apo70000062', '$2b$12$Y9ECCGw98umQDbfOwfZnju02xdgQPbfAA6kyKTWhmtU2BqOivUB9y', 'APODERADO', 0),
 ('apo70000063', '$2b$12$Od3in2zsIakobvkyXG3IQesisDr2TiSDngX6J/QjymDnwrQZMyfFi', 'APODERADO', 0),
 ('apo70000064', '$2b$12$V5/vJ6TQSeQYxBfQAhxst.YYLHp50KUITi1jc/d0ZIRfPn.klxM6u', 'APODERADO', 0),
 ('apo70000065', '$2b$12$TtGziwHtShFtjVJXiSa7quPRaoZZPPR1MUJWRSsYDLcBOdH4BNnBa', 'APODERADO', 0),
 ('apo70000066', '$2b$12$PRLZus8Z8TuQkBcuNm8QgukAKvBehVFzMJNqhTAxbnjyxivaWQuOm', 'APODERADO', 0),
 ('apo70000067', '$2b$12$0wkyAZt9LkeXDjyuImSJp.9WeryDZVaOj6ygF.sIfqcrLGuYYj6ta', 'APODERADO', 0),
 ('apo70000068', '$2b$12$rrtFQ9K6Kh39JzZDb9xDo.kUMZ12uZGLmTXI5wW16ykg3dhskC32u', 'APODERADO', 0),
 ('apo70000069', '$2b$12$c1IaGqPZ229HCqw9KuqjO.OMvza3I.d0wJMbTj0.V59IdwVxDyuzm', 'APODERADO', 0),
 ('apo70000070', '$2b$12$ZTAJOecVI8xn2/V6rYnq9uPzxZRE89.sBQIvHsAFGwT79QsZQ2L.K', 'APODERADO', 0),
 ('apo70000071', '$2b$12$FBIsI8ClgI6sXMQwTOKJoOGZcdQlc0aZTaL3zPM51FfGkuoy72k7O', 'APODERADO', 0),
 ('apo70000072', '$2b$12$xsVu//3K9hDHqwzbG3tDlOZFi38QAjhk9vZ2170Kkrl3CJuv3FN1m', 'APODERADO', 0),
 ('apo70000073', '$2b$12$3IdLEFazj5sDP4fE/HFMZeutCHNfJUYekbz899ep910eekqNCVO4i', 'APODERADO', 0),
 ('apo70000074', '$2b$12$2oJEpXmElyuLANFHPpXnxuj1/yZJm5kPNJHjy8wwYp2cJihh2XTw6', 'APODERADO', 0),
 ('apo70000075', '$2b$12$18wvZMYxm0AGDtcxKO6Ey.CAP8PATdokKFsRZDyqPajgTC.Y4q896', 'APODERADO', 0),
 ('apo70000076', '$2b$12$AtvBejmWHzKXZE/L7oIkzuuiaeoKCRh5iz7S5yKv5fg3l51NLTxDO', 'APODERADO', 0),
 ('apo70000077', '$2b$12$RIQw4st949haWfgQxGkX5OI6av/RkNUE39PCTslQc2d/t.8YnYxJK', 'APODERADO', 0),
 ('apo70000078', '$2b$12$fIx0hv4WfG8siHrxaS6H.uNUEIAihhuiPtP15duim5mvdLTy1Xbbq', 'APODERADO', 0),
 ('apo70000079', '$2b$12$a/TPnWdrx1VW2QyWTJCBNOC652NuJMK.7caxDaE.x/JtSzaW6Hn6q', 'APODERADO', 0),
 ('apo70000080', '$2b$12$4sGNqjSU2R5.zhuBnGjVDOZoy06JC.VNcF.Hne74vjhGKcVdUos9y', 'APODERADO', 0),
 ('apo70000081', '$2b$12$AZ1Vb.X/ZKr03V3QJL8nx.aClMA3iFFtN6DhewraOar0EJKPWkfT2', 'APODERADO', 0),
 ('apo70000082', '$2b$12$c5NtYnFKKQuf.b3lxIRtYO1zAecTp//Tvj2aZMPtW3uAGTnXXbQnK', 'APODERADO', 0),
 ('apo70000083', '$2b$12$DFXuTcLx9Mi8TRlw4avI.enxWikbf6P4/n7oeuID/sFvrKq1Ch.kO', 'APODERADO', 0),
 ('apo70000084', '$2b$12$6yFEZjPpS.Y7b1SiuEQmw.o84H/ILUCO8XPTL9V5eMh5OPcNAMJ7K', 'APODERADO', 0),
 ('apo70000085', '$2b$12$i/7S.81Oo62NeCbDKc01cONY.8YqjZ99TUsuW5dH.9GbMiEHAFwcy', 'APODERADO', 0),
 ('apo70000086', '$2b$12$zZAVnPFVvWY/L9JzCkxrVumZWICYZdqFZqXB7BWLrbnv9DZQGjWqK', 'APODERADO', 0),
 ('apo70000087', '$2b$12$GQxa8BoMdIftOytFOTbSv.Fosm/YuTRbRXBejo0gPWrEMhxRI3bBy', 'APODERADO', 0),
 ('apo70000088', '$2b$12$bGGa1JrNbzj52RcIGvrXHOs5iwwoW6bil/RX0EwV8z8m75YVJxaMy', 'APODERADO', 0),
 ('apo70000089', '$2b$12$xFyif/8wNB8O.iD1vXvFM.6535EEe8jE8aXV8dlR.CHnbFRE4Akxu', 'APODERADO', 0),
 ('apo70000090', '$2b$12$IeYVnfUdJXHLb5wiXldaVO7HVFFLEdOqcZm7/jBOTNLLUTMHQ8.S.', 'APODERADO', 0),
 ('apo70000091', '$2b$12$ApnRQV18NPi50j9evsDRDO.wQ8Zw.xOYF0xSU3xm/ZlDai3yXxppi', 'APODERADO', 0),
 ('apo70000092', '$2b$12$PL9aLW9.AtseBInyOE3QOO0RqpkamhcA14VVUHqIZKiGBjn7WVAjG', 'APODERADO', 0),
 ('apo70000093', '$2b$12$yIBYAlZsBWVbEddQBKrxuOVmZN3yZEmCWl2MOJkQeK3EEZ8HPlrJq', 'APODERADO', 0),
 ('apo70000094', '$2b$12$4D.cbXdhz6Re0B6Fly8xze7kDP0hX8tpQKjIUklFgszdzg63RLnhO', 'APODERADO', 0),
 ('apo70000095', '$2b$12$UegI8uFeKYv8iAP.NfjRO.CbeqcLBE1EN16PdG/NNc76SJW.TBq/a', 'APODERADO', 0),
 ('apo70000096', '$2b$12$Ss5u4dzE8.450tsADfSzMOh1dtRgIGsnVosPV9D1d9GEoeYwdZPbO', 'APODERADO', 0),
 ('apo70000097', '$2b$12$eWvcjCp/6AugEPXvTxMt.uRdg8dpWa3DRlEC.NKg44B8YSPD6sgwS', 'APODERADO', 0),
 ('apo70000098', '$2b$12$29if3l4IBW4Y0Gwxj9Zm9e6CIScGofXPMyOpQOYvDc2kE40GzGh1e', 'APODERADO', 0),
 ('apo70000099', '$2b$12$k8wYGdy4CWE6UN..Hp31zOlGStzMtDxb/ZCnpb91x9yUWhV5QHdbW', 'APODERADO', 0),
 ('apo70000100', '$2b$12$FLqet/20X0JslhDeUvYnS.q6rbAeOZA3CSYEz17QSPsij.SRMfGNm', 'APODERADO', 0),
 ('apo70000101', '$2b$12$QCc0relm4AP0wRG7EDlGVeNHiZpo4QfDmrkZU6G1bfTuJ2z7mIpdy', 'APODERADO', 0),
 ('apo70000102', '$2b$12$iDg0ZhM.cV6s3ENKGe.9zOQbFPGYcdB1QrBq72TMosNaXGr0yLYZy', 'APODERADO', 0),
 ('apo70000103', '$2b$12$ZjrYU5M9vg/SzSHN1PfHhuOnFST8rIn2IpMsD8ZZo8mYJTf0yCo6m', 'APODERADO', 0),
 ('apo70000104', '$2b$12$zQ8lxO9feSu11ZbvXTxKtu3f6fcml2FisgBwDTlPk1TJNCAocAu7y', 'APODERADO', 0),
 ('apo70000105', '$2b$12$0qmoMtbe8mL/QcqYidfBUOdJ9xcB1CDZJ.LNAKp/YbsMizZwkZ.Em', 'APODERADO', 0),
 ('apo70000106', '$2b$12$AGM.GnUTOlqkEcqjRaL71uesZwCKsB.nG5d5fxCo6KHLDbFUKoJ4e', 'APODERADO', 0),
 ('apo70000107', '$2b$12$jmVZVpw6g9oV2yERxCwNnerA1Ihju2U7QjaEHDNZpZ4.d99ee1R2S', 'APODERADO', 0),
 ('apo70000108', '$2b$12$GbIyZBR42I4VH8feTauW7.ZY0/7b7DXUHw1NGDya5QgX716C6aJx6', 'APODERADO', 0),
 ('apo70000109', '$2b$12$Ts3SWSAzVV2D8C3cusylServIY9iK6Zv92Dn9wWF0x5PdFSmBySCO', 'APODERADO', 0),
 ('apo70000110', '$2b$12$9hzSwshxAhYUH87nFclrPuD2rQpcVE0u39lHT5Yte8i2.qTAb5yDS', 'APODERADO', 0),
 ('apo70000111', '$2b$12$2hOFQIxHLKrFoOQZ6BKTweUn0JHxGHSVRj3jr/Sem4KgvvUCPRQ2O', 'APODERADO', 0),
 ('apo70000112', '$2b$12$WdOeEYlilk4ZRyrkFDrxT.JSJhjEHvYpLmz9HYDVnZFvVl.Ng4ZMe', 'APODERADO', 0),
 ('apo70000113', '$2b$12$Tkau9oo1KZA0y20zIhmCKO9fAv4QfmNJMWglKjBjtUmv7ZSXO1fEG', 'APODERADO', 0),
 ('apo70000114', '$2b$12$oWjFQDXPq1n6ZkI3KiClrO1KCC0Vw3uop/xcTdp4/siM40MMogESm', 'APODERADO', 0),
 ('apo70000115', '$2b$12$Og248RlBwMKmuqKWUD9DVulJHnIDPcSaxmznWKcxvMpjdVSx12kKC', 'APODERADO', 0),
 ('apo70000116', '$2b$12$S2BcM7rm164vN0I0XxM1Fe54d1ewKfxq0iUOcXEJ0QPyGh.wJJpzS', 'APODERADO', 0),
 ('apo70000117', '$2b$12$HdBLR0lZ4co4jh.Mxtbn9uwBmhCuOKW2yVBZ4B4m5M1jvodV7YkMS', 'APODERADO', 0),
 ('apo70000118', '$2b$12$oapjrbzBF1BVvdMvvfrna.RSWtGWoUZkvMxuUHGR1YwhKQqZWcMFK', 'APODERADO', 0),
 ('apo70000119', '$2b$12$P/3zRNRhyCRzOVVpaVMYqu5pfeyna/PYLF/edgBxBrb/6wE9OAQn2', 'APODERADO', 0),
 ('apo70000120', '$2b$12$8p6GoldgQRlNwCW6itlfKuxPXuhqF1iWCrk4eEmrFr8T8ByQ51R5G', 'APODERADO', 0),
 ('apo70000121', '$2b$12$454fnX8ZHluK0.H8SzAF8etzouHykpzvrLGs9UKHaIQTD6PspB1Zm', 'APODERADO', 0),
 ('apo70000122', '$2b$12$JvBXBxwyZnJTqe3JmgrNeemvdDrtZP12ngxKkAmDt4MGdiaeb3HX.', 'APODERADO', 0),
 ('apo70000123', '$2b$12$qFILBRS8g6uOGGlbgdNs6uU.BhZ33MDwUZypZWv9dUre0yj0jYeG2', 'APODERADO', 0),
 ('apo70000124', '$2b$12$xePzBoWp3v18vKMn4YxLJ.Xe5rtq2Ux8DB/1WGuG419Q3ZcO1u.Yy', 'APODERADO', 0),
 ('apo70000125', '$2b$12$0itzV0ksdSj.R/lSik0pd.o9XxgOqz/o82OUJzC63F5pM499Mt7pe', 'APODERADO', 0),
 ('apo70000126', '$2b$12$xZ87tVkEZ7PcZoFVofHTQ.lQtcShVP1ePH2490v5FBjauujAv5NEq', 'APODERADO', 0),
 ('apo70000127', '$2b$12$HGrQZVP/3JwUV90kjS/n0uWsm3Aj.gLIscga7AA2SjlLzDrxvoo2u', 'APODERADO', 0),
 ('apo70000128', '$2b$12$ROEuMMEQwMcul3Ps3t.ZrOe288lHleFHUoFCQsWYAiZsmBXtpc8XO', 'APODERADO', 0),
 ('apo70000129', '$2b$12$Pr.uPAs5bA2UJn2XEuPL4eSiHTID4HbF4By3POezohpYAtzXugRsy', 'APODERADO', 0),
 ('apo70000130', '$2b$12$k5g9l.Jbq6/UR8jW4RyVru5SN9fFA5GKfgZswpMagwGU56UOw.RN6', 'APODERADO', 0),
 ('apo70000131', '$2b$12$WGbDk9MWjDF26Y6AcxW3xeN4kpHNUN87PAXuCTrg5xMfpNqEFeBrS', 'APODERADO', 0),
 ('apo70000132', '$2b$12$o0Ez2xGweT.iLOF4Gx7WlujDiDS2S4HNL0NjL28Oo1wXMtv6h4iGi', 'APODERADO', 0),
 ('apo70000133', '$2b$12$g00ESOqGhRnA6b/q7.s8..sULLMxCX4gzjQFgPuP3rbyhyNQH6Yta', 'APODERADO', 0),
 ('apo70000134', '$2b$12$iTgkU7Bg2d4s68KSiC.i8O/MszbFE6.mYMO9iVSRmmkdOlYgLXsSC', 'APODERADO', 0),
 ('apo70000135', '$2b$12$blStGLxiT.LgGh2cFVsH7OwzURHWeK47kROH0xQMslmA81SfDbJQ2', 'APODERADO', 0),
 ('apo70000136', '$2b$12$TqqhFAgeLllNsX0HNe1tqe7XUQsNLLeOh1A21.JKlxlbCAFehU43W', 'APODERADO', 0),
 ('apo70000137', '$2b$12$6BX6IS6tZ.kLVtz7dEs0V.buJeyjFOwxotbbFYs1X/tiS1gLEP5di', 'APODERADO', 0),
 ('apo70000138', '$2b$12$slwSveRlmyNAlN0MCUGZmukqNBULZHVpI4rp8KCP4MnyraHyC4/4e', 'APODERADO', 0),
 ('apo70000139', '$2b$12$N99i6t/MOJW9o0E3aagOSuqOugaTXK1GAVtJtKUDXvtbkpnfA/Hri', 'APODERADO', 0),
 ('apo70000140', '$2b$12$msg10sGDaKFig4yawziDO.LV64PrExlSGmGZwpu8n4LlG7.dVHo9W', 'APODERADO', 0),
 ('apo70000141', '$2b$12$jpfl6e63Rkt4SBVdbtNVOO8Q4l3.zhksrmU3bESk0DljoXlquSx0S', 'APODERADO', 0),
 ('apo70000142', '$2b$12$pl5vlQSFvFpTrRrd3slbieEjlFXtbIH9.X4aFcljk1y/SlsrUJ/MW', 'APODERADO', 0),
 ('apo70000143', '$2b$12$O7jxRqY1RK/DkCCzLRx8Y.a42DbjgAMYVUMpQm3.N9/atR3cxI3im', 'APODERADO', 0),
 ('apo70000144', '$2b$12$6rHJlX6nuCFyD/ojkoAQAe.F5Pa4nSytDkbsLwzJ/mwXICs2vUbFG', 'APODERADO', 0),
 ('apo70000145', '$2b$12$j8WA3TkdsVjbkyR0wy3qE.55WqpDLwB4esz9ufMez4a9xfvx6ocby', 'APODERADO', 0),
 ('apo70000146', '$2b$12$KYFBHWYTn8FfnijwvG4TpuHV3u1Z2ftRIRPbAIBknLBylvAP5kK3K', 'APODERADO', 0),
 ('apo70000147', '$2b$12$LzMPe1pObgtA.o/1Fvu5BOs8KGFdAQH1w4rDoYNA/sDUdSU5iUZmS', 'APODERADO', 0),
 ('apo70000148', '$2b$12$XAIu0n/yULvT8uDc9hkAKOhh6zL8TQgw/Z/.XgCr10yGwrB7tTqiK', 'APODERADO', 0),
 ('apo70000149', '$2b$12$PZjn/RYcIECXenzpuhGxHOBccGL2/Xv8fSSwToj0BKnf4AAuub7RC', 'APODERADO', 0),
 ('apo70000150', '$2b$12$TtwkQByrd.iabB6td0qPZuuXxvDznf1zSFVKyivWZucqqewsAwNV.', 'APODERADO', 0),
 ('apo70000151', '$2b$12$df4pMDtToC8IszlbMoVbuOUQ40eru7OZ5IQPqEi0.azkOYgPxFSi.', 'APODERADO', 0),
 ('apo70000152', '$2b$12$x2hb2LRjJVhw960oMr8qQuQk69W6IeCN0XHxHThRnUa2XuKGlkmnq', 'APODERADO', 0),
 ('apo70000153', '$2b$12$rgp41ftDmAldw9hShBxrE.SdKTRdltacVp3Hh3790MZdGgHmvyDAW', 'APODERADO', 0),
 ('apo70000154', '$2b$12$lqxIhbvE3HtxDpC4kr1fUOxnSKbt.iaK.LC8VHbBY8yyKhFtqgn1i', 'APODERADO', 0),
 ('apo70000155', '$2b$12$8SRygzUCcnQWoL49MBN3Yux0/46dhU5pGt0M9lJRl/FRHUHlA2pWe', 'APODERADO', 0),
 ('apo70000156', '$2b$12$Kyxjq3Nx4PG248w0r0RbxezSq7v2ALhabjxQz7/KCJplI/yZGLcSe', 'APODERADO', 0),
 ('apo70000157', '$2b$12$cAd/pYzfrfEveatiTy1Q8upvDt9bd7ZXxhzKiGayNVCVgj.3G7KgW', 'APODERADO', 0),
 ('apo70000158', '$2b$12$lJizYvmqNPy2.e/IYP9Vr.G.e1jHR1wAqRnTH2jU1LIvNbeHRTfRm', 'APODERADO', 0),
 ('apo70000159', '$2b$12$82bZMjFhBHBeCZvDiTe3MO7ohOgUnoGy0F9RP5zk3.e4wHvn1Yxty', 'APODERADO', 0),
 ('apo70000160', '$2b$12$odWvEayGHggNS.Rw2/5PJO5aoiO5ShVOJz.dTmypIyEaoBHDbeGPG', 'APODERADO', 0),
 ('apo70000161', '$2b$12$.ms55Z94qnJRizBL/f4snuhJaVnFYoqCj6JiCIGpC8nLfOrBaZfc2', 'APODERADO', 0),
 ('apo70000162', '$2b$12$LlqZKiz8TUytFsAqN3Z28OxiMSEW72EJD6p4NVKSSXrb3hLuIebfK', 'APODERADO', 0),
 ('apo70000163', '$2b$12$4FIRfudMcz28mFG5dzdKZOL3DOnWbDYGUpQp1Uyyk5gZHgMqEdFYW', 'APODERADO', 0),
 ('apo70000164', '$2b$12$VZ93MoDTAeWCHK9giZSChuHi/ifMwVPZpFTRC.qB6YY2yBIHWPfAK', 'APODERADO', 0),
 ('apo70000165', '$2b$12$gsFt7CxfZ8ZM.kcb27R9EO12kNOZggvZpQgHh6qIqjeZJL7OhFxe2', 'APODERADO', 0),
 ('apo70000166', '$2b$12$E9LB/ylYJhh0rM.OQHgy3upGjG0ZN6yygGHsywXGksov9OUZxouVW', 'APODERADO', 0),
 ('apo70000167', '$2b$12$MiD/84jC4r1Pap9HXIHfjO7E1qQYHL5DvJhC7.JM2q1CKbmyl.AT2', 'APODERADO', 0),
 ('apo70000168', '$2b$12$DDEImKL9odo18hrJPQooqu41EiEvnWxZfDNg258H5mpu0jwTMNn5O', 'APODERADO', 0),
 ('apo70000169', '$2b$12$B8qVhdx8UTDl93.PDUJwq.RCvBkqgyay2FRvctPViart8YVwBS7ma', 'APODERADO', 0),
 ('apo70000170', '$2b$12$2HN/dlVPmLHRbdG9e5zoR.Uv5g8JV8zfhnCiEkiSB37LrC9eknjM6', 'APODERADO', 0),
 ('apo70000171', '$2b$12$A.tp/U2D3RGD8BpTodviYeI1pIRs/GZI4KuE4Vf1NnEm7FNwEPPEK', 'APODERADO', 0),
 ('apo70000172', '$2b$12$jcVIEZZvprLR2omhcw76/ueeSMRAPsDbSOoKLiJ9MfziYCi6.Uvt2', 'APODERADO', 0),
 ('apo70000173', '$2b$12$x79LqXCB0/pMqXn31GMXPeJs54LHb7PSDEzvlXyj.MfdltWQhQtg2', 'APODERADO', 0),
 ('apo70000174', '$2b$12$4TmWS1dHxigXM6ElwXOB/.xkzAV3ZYzJb.isPW8NlbcRbpM5.W9OC', 'APODERADO', 0),
 ('apo70000175', '$2b$12$wKvnPdUO3RzusAVdsqVUGuqv3S6MhshO68P/cas.R9Cyc5249stWC', 'APODERADO', 0),
 ('apo70000176', '$2b$12$NMWZgM14WH3z5zpOLxqU9Oy7vqPNdWaxY0fJxVh6t4jCK.Jryv.C2', 'APODERADO', 0),
 ('apo70000177', '$2b$12$39QihiV4EqmRMH94AgQU7ud0ZuZw6GEJQ0oYq8QZBbDMIam7a7JTu', 'APODERADO', 0),
 ('apo70000178', '$2b$12$pYETN5CtgIxJFJfqQeTRpuYR/.vksiJxzM1.6Ls9yTg/VDY1UwIsu', 'APODERADO', 0),
 ('apo70000179', '$2b$12$ljIB733XxAdwij93GT2SIOz.KUxM50CjupyJV58G66uesV16GT20a', 'APODERADO', 0),
 ('apo70000180', '$2b$12$qu198Cf6liMqkkmly2HvluBOkPRC133MOkTNeD1JHqL4SkFtoo0I.', 'APODERADO', 0),
 ('apo70000181', '$2b$12$j89cS3N2tNCzICAHpgalne2a61iyDt/6CEj2st23yov2jVKvC7mca', 'APODERADO', 0),
 ('apo70000182', '$2b$12$pkLmR4HeEBK0bnndt2TzSuDhjnRGmMpcjpTXtNuMYPfOSig33YB2W', 'APODERADO', 0),
 ('apo70000183', '$2b$12$EAj6ql.fElFME.IK3Zaw/OmGwWPLrJh2j7DHG4B1WBkAsVXUaya66', 'APODERADO', 0),
 ('apo70000184', '$2b$12$m2KMTyVscXDhhntS7eghwej7T5YXinCNY6unkc4ZeM80EWacAY1RS', 'APODERADO', 0),
 ('apo70000185', '$2b$12$xtMYv3gwrRqTpLfDjaWA5uY9sOYjjtQt9feto6WDQ45TBusecAMf6', 'APODERADO', 0),
 ('apo70000186', '$2b$12$by4yOgDrr9rVfqjo.byolOUo5WVqScC26495V.2OeYjTmlTgtjqNi', 'APODERADO', 0),
 ('apo70000187', '$2b$12$xbAFL60eO14TCdQs8bqLXuY4dIjjtjiD3YXHuo3Pk3UwYqUQqu3zy', 'APODERADO', 0),
 ('apo70000188', '$2b$12$9HWca2V7H9wztd/QfZwfe.zBGHQ9XWQBEaAHEK/jENtwguYGqilTO', 'APODERADO', 0),
 ('apo70000189', '$2b$12$COtzFEQYD3x914E3XGsK/u4OktiwtGAVYpr6MoLTWh5YZ0Hkhkckq', 'APODERADO', 0),
 ('apo70000190', '$2b$12$ZFKSe4QPj4IzfmRId7QQfOzYQ.bowLPnEbQ.ILly9ONEwHIe4BxA2', 'APODERADO', 0),
 ('apo70000191', '$2b$12$sxa/RmLZd2HgR2UfhYbLQOGJyBxZi5.eQPVnrXjzgj2i0y1ykpkAy', 'APODERADO', 0),
 ('apo70000192', '$2b$12$x1wql66MoCuYypHmOzp7F.2flsNYfUYFXZo6tCCQPhurszuhTiJJS', 'APODERADO', 0),
 ('apo70000193', '$2b$12$tp1hv76mD7jxYMg0d7USzOGXPiu2Op8o6ZkLcLDCkpzoVh2sNF0WO', 'APODERADO', 0),
 ('apo70000194', '$2b$12$TIiMeFJqogcPhd5B.xOxVORa/cl3HwYTRXH109H..Po4tWwLJXqYq', 'APODERADO', 0),
 ('apo70000195', '$2b$12$.zWUDjM/pkLEjYOp2H8lxOVMAkMnEFcEAjSgmGuM3P6To6zuSxNQi', 'APODERADO', 0),
 ('apo70000196', '$2b$12$C2BJn85Vxn0w4/OH4Bf9F.Bi3e4Qt2SLy489I3pwqPBJJV9RkQFFS', 'APODERADO', 0),
 ('apo70000197', '$2b$12$c0wQNMD8y2fvFYrqcqWBM.ZCtPj1Drk07azqCbePC1mZUJt2xwyyG', 'APODERADO', 0),
 ('apo70000198', '$2b$12$/.5n14rnBFx4ptGoPe2hKuqAjG7oZR2pVTY2BkGHYmEJZjUK6Ug56', 'APODERADO', 0),
 ('apo70000199', '$2b$12$0NzZGiApWGvIHEYyZ8usxukd0ojO8RBGgIVYW1/isDA1RUxL4V1MS', 'APODERADO', 0),
 ('apo70000200', '$2b$12$OfHTD3KhAguZ7uAOPSLzNe7im6BZI/t6okg4VelgaeFrFdbjuoMGO', 'APODERADO', 0)
) AS v(Username, Hash, Rol, DebeCambiar)
JOIN Rol r ON r.Nombre = v.Rol;

INSERT INTO Docente (UsuarioId, DNI, Nombre, Apellido, Email)
SELECT u.UsuarioId, v.DNI, v.Nombre, v.Apellido, v.Email
FROM (VALUES
 ('docente01', '40000001', N'Carlos', N'Mendoza Ríos', 'docente01@pitagoras.edu.pe'),
 ('docente02', '40000002', N'Rosa', N'Huamán Salas', 'docente02@pitagoras.edu.pe'),
 ('docente03', '40000003', N'María', N'Quispe Torres', 'docente03@pitagoras.edu.pe'),
 ('docente04', '40000004', N'Jorge', N'Paredes León', 'docente04@pitagoras.edu.pe'),
 ('docente05', '40000005', N'Lucía', N'Flores Vega', 'docente05@pitagoras.edu.pe'),
 ('docente06', '40000006', N'Miguel', N'Castro Rojas', 'docente06@pitagoras.edu.pe'),
 ('docente07', '40000007', N'Elena', N'Ramos Chávez', 'docente07@pitagoras.edu.pe'),
 ('docente08', '40000008', N'Pedro', N'Vargas Soto', 'docente08@pitagoras.edu.pe'),
 ('docente09', '40000009', N'Carmen', N'Gutiérrez Silva', 'docente09@pitagoras.edu.pe'),
 ('docente10', '40000010', N'Raúl', N'Espinoza Medina', 'docente10@pitagoras.edu.pe')
) AS v(Username, DNI, Nombre, Apellido, Email)
JOIN Usuario u ON u.Username = v.Username;

INSERT INTO Curso (Nombre) VALUES (N'Matemática'), (N'Comunicación'), (N'Ciencia y Tecnología'), (N'Ciencias Sociales'), (N'Inglés');

INSERT INTO Competencia (CursoId, Codigo, Nombre)
SELECT c.CursoId, v.Codigo, v.Nombre
FROM (VALUES
 (N'Matemática', '1', N'Resuelve problemas de cantidad'),
 (N'Matemática', '2', N'Resuelve problemas de regularidad, equivalencia y cambio'),
 (N'Matemática', '3', N'Resuelve problemas de forma, movimiento y localización'),
 (N'Matemática', '4', N'Resuelve problemas de gestión de datos e incertidumbre'),
 (N'Comunicación', '1', N'Se comunica oralmente en su lengua materna'),
 (N'Comunicación', '2', N'Lee diversos tipos de textos escritos'),
 (N'Comunicación', '3', N'Escribe diversos tipos de textos'),
 (N'Ciencia y Tecnología', '1', N'Indaga mediante métodos científicos'),
 (N'Ciencia y Tecnología', '2', N'Explica el mundo físico basándose en conocimientos'),
 (N'Ciencia y Tecnología', '3', N'Diseña y construye soluciones tecnológicas'),
 (N'Ciencias Sociales', '1', N'Construye interpretaciones históricas'),
 (N'Ciencias Sociales', '2', N'Gestiona responsablemente el espacio y el ambiente'),
 (N'Ciencias Sociales', '3', N'Gestiona responsablemente los recursos económicos'),
 (N'Inglés', '1', N'Se comunica oralmente en inglés'),
 (N'Inglés', '2', N'Lee diversos tipos de textos en inglés'),
 (N'Inglés', '3', N'Escribe diversos tipos de textos en inglés')
) AS v(Curso, Codigo, Nombre)
JOIN Curso c ON c.Nombre = v.Curso;

-- Carga académica: cada curso tiene 2 docentes; uno dicta en las secciones A y B, el otro en C y D
INSERT INTO CursoSeccion (CursoId, SeccionId, DocenteId)
SELECT c.CursoId, s.SeccionId, d.DocenteId
FROM Seccion s
CROSS JOIN Curso c
JOIN (VALUES
 (N'Matemática', 'AB', '40000001'),
 (N'Matemática', 'CD', '40000002'),
 (N'Comunicación', 'AB', '40000003'),
 (N'Comunicación', 'CD', '40000004'),
 (N'Ciencia y Tecnología', 'AB', '40000005'),
 (N'Ciencia y Tecnología', 'CD', '40000006'),
 (N'Ciencias Sociales', 'AB', '40000007'),
 (N'Ciencias Sociales', 'CD', '40000008'),
 (N'Inglés', 'AB', '40000009'),
 (N'Inglés', 'CD', '40000010')
) AS v(Curso, Letras, DNI) ON v.Curso = c.Nombre AND CHARINDEX(s.Nombre, v.Letras) > 0
JOIN Docente d ON d.DNI = v.DNI;

-- Especialidad de cada docente (1 a 3 áreas). Algunos dictan dos áreas, para probar el filtro de Carga académica.
INSERT INTO DocenteArea (DocenteId, CursoId)
SELECT d.DocenteId, c.CursoId
FROM (VALUES
 ('40000001', N'Matemática'), ('40000001', N'Ciencia y Tecnología'),
 ('40000002', N'Matemática'),
 ('40000003', N'Comunicación'), ('40000003', N'Inglés'),
 ('40000004', N'Comunicación'),
 ('40000005', N'Ciencia y Tecnología'), ('40000005', N'Matemática'),
 ('40000006', N'Ciencia y Tecnología'),
 ('40000007', N'Ciencias Sociales'), ('40000007', N'Comunicación'),
 ('40000008', N'Ciencias Sociales'),
 ('40000009', N'Inglés'), ('40000009', N'Comunicación'),
 ('40000010', N'Inglés')
) AS v(DNI, Curso)
JOIN Docente d ON d.DNI = v.DNI
JOIN Curso c   ON c.Nombre = v.Curso;

-- Año 2026: bimestres 1 a 3 cerrados, bimestre 4 abierto
INSERT INTO Periodo (AnioLectivo, BimestreNum, FechaInicio, FechaFin, Cerrado, FechaCierre, CerradoPorUsuarioId) VALUES
 (2026, 1, '2026-03-16', '2026-05-15', 1, '2026-05-18T08:00:00', (SELECT UsuarioId FROM Usuario WHERE Username = 'director')),
 (2026, 2, '2026-05-25', '2026-07-31', 1, '2026-08-03T08:00:00', (SELECT UsuarioId FROM Usuario WHERE Username = 'director')),
 (2026, 3, '2026-08-10', '2026-10-09', 1, '2026-10-12T08:00:00', (SELECT UsuarioId FROM Usuario WHERE Username = 'director')),
 (2026, 4, '2026-10-19', '2026-12-18', 0, NULL, NULL);

-- Evaluaciones: una por periodo, curso-sección y competencia
INSERT INTO Evaluacion (PeriodoId, CursoSeccionId, CodigoCompetencia)
SELECT p.PeriodoId, cs.CursoSeccionId, k.Codigo
FROM Periodo p
CROSS JOIN CursoSeccion cs
JOIN Competencia k ON k.CursoId = cs.CursoId;

-- 200 alumnos: 10 por sección, cada uno con la cuenta de su apoderado (RF03)
INSERT INTO Alumno (SeccionId, UsuarioId, DNI, Nombre, Apellido, NombreApoderado, TelefonoApoderado)
SELECT s.SeccionId, u.UsuarioId, v.DNI, v.Nombre, v.Apellido, v.Apoderado, v.Telefono
FROM (VALUES
 (N'1.°', 'A', 'apo70000001', '70000001', N'Abigail', N'Cruz Arias', N'Marta Cruz Silva', '939968745'),
 (N'1.°', 'A', 'apo70000002', '70000002', N'Samuel', N'Mamani Paredes', N'Pilar Mamani Soto', '988721495'),
 (N'1.°', 'A', 'apo70000003', '70000003', N'Ana', N'Ccahuana Flores', N'Marta Ccahuana Vargas', '923160265'),
 (N'1.°', 'A', 'apo70000004', '70000004', N'Micaela', N'Medina Gutiérrez', N'Víctor Medina Salazar', '943757517'),
 (N'1.°', 'A', 'apo70000005', '70000005', N'Fernanda', N'Navarro Salazar', N'Rosa Navarro Ortiz', '955686986'),
 (N'1.°', 'A', 'apo70000006', '70000006', N'Ariana', N'Vargas Sánchez', N'Julio Vargas Silva', '996542925'),
 (N'1.°', 'A', 'apo70000007', '70000007', N'Daniel', N'Vargas Torres', N'Patricia Vargas Espinoza', '971884106'),
 (N'1.°', 'A', 'apo70000008', '70000008', N'Renata', N'Silva Condori', N'Pilar Silva Vega', '925629378'),
 (N'1.°', 'A', 'apo70000009', '70000009', N'Nicole', N'Silva Díaz', N'César Silva Lozano', '980417695'),
 (N'1.°', 'A', 'apo70000010', '70000010', N'Alessandra', N'Condori Lozano', N'Pilar Condori Cruz', '979523069'),
 (N'1.°', 'B', 'apo70000011', '70000011', N'Daniel', N'Castillo Soto', N'Julio Castillo Campos', '916561596'),
 (N'1.°', 'B', 'apo70000012', '70000012', N'Alonso', N'Silva Medina', N'Marta Silva Reyes', '993524993'),
 (N'1.°', 'B', 'apo70000013', '70000013', N'Sofía', N'Díaz Silva', N'Gloria Díaz Vega', '926376341'),
 (N'1.°', 'B', 'apo70000014', '70000014', N'Iker', N'Medina Díaz', N'Alberto Medina Mamani', '963966306'),
 (N'1.°', 'B', 'apo70000015', '70000015', N'Santiago', N'Mamani Navarro', N'Pilar Mamani Navarro', '963948553'),
 (N'1.°', 'B', 'apo70000016', '70000016', N'Antonella', N'Campos Chávez', N'Juan Campos Rojas', '930111687'),
 (N'1.°', 'B', 'apo70000017', '70000017', N'Samuel', N'Sánchez Navarro', N'Pilar Sánchez Espinoza', '936821744'),
 (N'1.°', 'B', 'apo70000018', '70000018', N'Micaela', N'Sánchez Cruz', N'Pilar Sánchez Navarro', '968281295'),
 (N'1.°', 'B', 'apo70000019', '70000019', N'Mateo', N'Campos Vargas', N'Óscar Campos Sánchez', '923203533'),
 (N'1.°', 'B', 'apo70000020', '70000020', N'Abigail', N'Chávez Sánchez', N'Teresa Chávez Paredes', '974309920'),
 (N'1.°', 'C', 'apo70000021', '70000021', N'Bruno', N'Silva Díaz', N'Patricia Silva Quispe', '936116972'),
 (N'1.°', 'C', 'apo70000022', '70000022', N'Leonardo', N'Huamán Sánchez', N'Juan Huamán Flores', '910028718'),
 (N'1.°', 'C', 'apo70000023', '70000023', N'Ariana', N'Lozano Campos', N'Rosa Lozano Ccahuana', '936681370'),
 (N'1.°', 'C', 'apo70000024', '70000024', N'Nicole', N'Rojas Soto', N'Alberto Rojas Medina', '995283083'),
 (N'1.°', 'C', 'apo70000025', '70000025', N'Ana', N'Gutiérrez Medina', N'Pilar Gutiérrez Reyes', '928101218'),
 (N'1.°', 'C', 'apo70000026', '70000026', N'Matías', N'Condori Huamán', N'Nancy Condori Flores', '966073339'),
 (N'1.°', 'C', 'apo70000027', '70000027', N'Thiago', N'Navarro Lozano', N'Nancy Navarro Cruz', '965302958'),
 (N'1.°', 'C', 'apo70000028', '70000028', N'Sofía', N'Cruz Navarro', N'Víctor Cruz Salazar', '911896747'),
 (N'1.°', 'C', 'apo70000029', '70000029', N'Rodrigo', N'Silva Flores', N'José Silva Campos', '956575946'),
 (N'1.°', 'C', 'apo70000030', '70000030', N'Luis', N'Torres Cruz', N'Pilar Torres Gutiérrez', '950758128'),
 (N'1.°', 'D', 'apo70000031', '70000031', N'Camila', N'Díaz Sánchez', N'Teresa Díaz Huamán', '989357819'),
 (N'1.°', 'D', 'apo70000032', '70000032', N'Leonardo', N'Ccahuana Gutiérrez', N'Hugo Ccahuana Ccahuana', '941316553'),
 (N'1.°', 'D', 'apo70000033', '70000033', N'Diego', N'Mamani Salazar', N'José Mamani Ramos', '996869801'),
 (N'1.°', 'D', 'apo70000034', '70000034', N'Mateo', N'Arias Mamani', N'José Arias Sánchez', '996013797'),
 (N'1.°', 'D', 'apo70000035', '70000035', N'Ana', N'Huamán Arias', N'Víctor Huamán Navarro', '979370789'),
 (N'1.°', 'D', 'apo70000036', '70000036', N'Thiago', N'Mendoza Medina', N'Teresa Mendoza Salazar', '946615522'),
 (N'1.°', 'D', 'apo70000037', '70000037', N'Alessandra', N'Vega Flores', N'César Vega Vargas', '935125395'),
 (N'1.°', 'D', 'apo70000038', '70000038', N'Daniela', N'Sánchez Díaz', N'Silvia Sánchez Vargas', '972773682'),
 (N'1.°', 'D', 'apo70000039', '70000039', N'Abigail', N'Díaz Sánchez', N'Marta Díaz Cruz', '980423899'),
 (N'1.°', 'D', 'apo70000040', '70000040', N'Sebastián', N'Vega Ramos', N'Gloria Vega Torres', '922498300'),
 (N'2.°', 'A', 'apo70000041', '70000041', N'Nicolás', N'Paredes Mamani', N'Teresa Paredes Torres', '957237950'),
 (N'2.°', 'A', 'apo70000042', '70000042', N'Fabricio', N'Mamani Medina', N'Víctor Mamani Quispe', '993320274'),
 (N'2.°', 'A', 'apo70000043', '70000043', N'Gabriel', N'Soto Condori', N'Víctor Soto Condori', '949553980'),
 (N'2.°', 'A', 'apo70000044', '70000044', N'Gabriel', N'Huamán Quispe', N'Alberto Huamán Quispe', '968526026'),
 (N'2.°', 'A', 'apo70000045', '70000045', N'Lucía', N'Soto Espinoza', N'Teresa Soto Arias', '994568783'),
 (N'2.°', 'A', 'apo70000046', '70000046', N'Valentina', N'Chávez Condori', N'Rosa Chávez Vargas', '952225137'),
 (N'2.°', 'A', 'apo70000047', '70000047', N'Daniela', N'Flores Torres', N'Silvia Flores Vega', '944499597'),
 (N'2.°', 'A', 'apo70000048', '70000048', N'Ana', N'Rojas Quispe', N'Gloria Rojas Torres', '951596580'),
 (N'2.°', 'A', 'apo70000049', '70000049', N'Ariana', N'Condori Lozano', N'Patricia Condori Gutiérrez', '971209284'),
 (N'2.°', 'A', 'apo70000050', '70000050', N'Samuel', N'Rojas Lozano', N'César Rojas Gutiérrez', '991575609'),
 (N'2.°', 'B', 'apo70000051', '70000051', N'Luana', N'Paredes Medina', N'Marta Paredes Medina', '960217143'),
 (N'2.°', 'B', 'apo70000052', '70000052', N'Bruno', N'Salazar Vargas', N'Patricia Salazar Rojas', '968618432'),
 (N'2.°', 'B', 'apo70000053', '70000053', N'Santiago', N'Reyes Gutiérrez', N'Nancy Reyes Ortiz', '926416872'),
 (N'2.°', 'B', 'apo70000054', '70000054', N'Alessandra', N'Gutiérrez Condori', N'Juan Gutiérrez Mamani', '955967403'),
 (N'2.°', 'B', 'apo70000055', '70000055', N'Lucía', N'Ortiz Quispe', N'Teresa Ortiz Ccahuana', '926466530'),
 (N'2.°', 'B', 'apo70000056', '70000056', N'Sofía', N'Cruz Espinoza', N'Nancy Cruz Torres', '967011675'),
 (N'2.°', 'B', 'apo70000057', '70000057', N'Fabricio', N'Paredes Salazar', N'César Paredes Castillo', '966503709'),
 (N'2.°', 'B', 'apo70000058', '70000058', N'Thiago', N'Mendoza Mamani', N'Juan Mendoza Medina', '982656183'),
 (N'2.°', 'B', 'apo70000059', '70000059', N'Rodrigo', N'Silva Chávez', N'Julio Silva Castillo', '913135594'),
 (N'2.°', 'B', 'apo70000060', '70000060', N'Ariana', N'Díaz Paredes', N'Alberto Díaz Gutiérrez', '928610287'),
 (N'2.°', 'C', 'apo70000061', '70000061', N'Daniel', N'Flores Campos', N'Teresa Flores Mamani', '915101456'),
 (N'2.°', 'C', 'apo70000062', '70000062', N'Alonso', N'Campos Chávez', N'Óscar Campos Arias', '979853472'),
 (N'2.°', 'C', 'apo70000063', '70000063', N'Nicolás', N'Rojas Medina', N'Juan Rojas Arias', '942588937'),
 (N'2.°', 'C', 'apo70000064', '70000064', N'Mariana', N'Medina Cruz', N'Julio Medina Lozano', '963562849'),
 (N'2.°', 'C', 'apo70000065', '70000065', N'Fabricio', N'Navarro Castillo', N'Alberto Navarro Gutiérrez', '936974998'),
 (N'2.°', 'C', 'apo70000066', '70000066', N'Luana', N'Arias Sánchez', N'Pilar Arias Silva', '987924088'),
 (N'2.°', 'C', 'apo70000067', '70000067', N'Ximena', N'Arias Paredes', N'Julio Arias Díaz', '910712938'),
 (N'2.°', 'C', 'apo70000068', '70000068', N'Alonso', N'Reyes Paredes', N'José Reyes Arias', '933162324'),
 (N'2.°', 'C', 'apo70000069', '70000069', N'Fernanda', N'Mamani Ortiz', N'Alberto Mamani Huamán', '923522643'),
 (N'2.°', 'C', 'apo70000070', '70000070', N'Luis', N'Mamani Ramos', N'Nancy Mamani Condori', '915991890'),
 (N'2.°', 'D', 'apo70000071', '70000071', N'Rodrigo', N'Mamani Cruz', N'Teresa Mamani Cruz', '924082320'),
 (N'2.°', 'D', 'apo70000072', '70000072', N'Renata', N'Gutiérrez Mamani', N'Alberto Gutiérrez Vargas', '915581926'),
 (N'2.°', 'D', 'apo70000073', '70000073', N'Adrián', N'Mendoza Silva', N'Nancy Mendoza Vargas', '982633979'),
 (N'2.°', 'D', 'apo70000074', '70000074', N'Sofía', N'Salazar Ramos', N'Juan Salazar Vega', '991329384'),
 (N'2.°', 'D', 'apo70000075', '70000075', N'Leonardo', N'Espinoza Navarro', N'Teresa Espinoza Ortiz', '979281900'),
 (N'2.°', 'D', 'apo70000076', '70000076', N'Valentina', N'Mamani Huamán', N'Teresa Mamani Torres', '925662869'),
 (N'2.°', 'D', 'apo70000077', '70000077', N'Luis', N'Campos Espinoza', N'Rosa Campos Espinoza', '950620832'),
 (N'2.°', 'D', 'apo70000078', '70000078', N'Sofía', N'Ccahuana Castillo', N'Julio Ccahuana Quispe', '953794068'),
 (N'2.°', 'D', 'apo70000079', '70000079', N'Valeria', N'Salazar Chávez', N'Nancy Salazar Arias', '925777156'),
 (N'2.°', 'D', 'apo70000080', '70000080', N'Micaela', N'Quispe Mamani', N'Gloria Quispe Campos', '958140861'),
 (N'3.°', 'A', 'apo70000081', '70000081', N'Iker', N'Lozano Condori', N'Patricia Lozano Flores', '919265701'),
 (N'3.°', 'A', 'apo70000082', '70000082', N'Diego', N'Navarro Díaz', N'Nancy Navarro Chávez', '998866676'),
 (N'3.°', 'A', 'apo70000083', '70000083', N'Valentina', N'Paredes Cruz', N'Óscar Paredes Gutiérrez', '998187901'),
 (N'3.°', 'A', 'apo70000084', '70000084', N'Rodrigo', N'Gutiérrez Mendoza', N'Víctor Gutiérrez Navarro', '972143747'),
 (N'3.°', 'A', 'apo70000085', '70000085', N'Fernanda', N'Soto Navarro', N'Patricia Soto Paredes', '989170107'),
 (N'3.°', 'A', 'apo70000086', '70000086', N'Sofía', N'Sánchez Ortiz', N'Julio Sánchez Paredes', '945920556'),
 (N'3.°', 'A', 'apo70000087', '70000087', N'Camila', N'Gutiérrez Chávez', N'Hugo Gutiérrez Chávez', '929510197'),
 (N'3.°', 'A', 'apo70000088', '70000088', N'Bruno', N'Castillo Gutiérrez', N'Julio Castillo Mamani', '926540483'),
 (N'3.°', 'A', 'apo70000089', '70000089', N'Kiara', N'Sánchez Arias', N'César Sánchez Salazar', '912915317'),
 (N'3.°', 'A', 'apo70000090', '70000090', N'Nicolás', N'Medina Paredes', N'Óscar Medina Quispe', '976395419'),
 (N'3.°', 'B', 'apo70000091', '70000091', N'Gabriel', N'Cruz Ramos', N'Silvia Cruz Espinoza', '997003335'),
 (N'3.°', 'B', 'apo70000092', '70000092', N'Renata', N'Ortiz Mendoza', N'Juan Ortiz Chávez', '985017167'),
 (N'3.°', 'B', 'apo70000093', '70000093', N'Thiago', N'Gutiérrez Condori', N'Silvia Gutiérrez Campos', '973879958'),
 (N'3.°', 'B', 'apo70000094', '70000094', N'Matías', N'Gutiérrez Torres', N'José Gutiérrez Soto', '939510600'),
 (N'3.°', 'B', 'apo70000095', '70000095', N'Bruno', N'Gutiérrez Torres', N'Víctor Gutiérrez Navarro', '971355838'),
 (N'3.°', 'B', 'apo70000096', '70000096', N'Samuel', N'Arias Espinoza', N'Rosa Arias Mamani', '935186307'),
 (N'3.°', 'B', 'apo70000097', '70000097', N'Luis', N'Quispe Huamán', N'Óscar Quispe Castillo', '972205072'),
 (N'3.°', 'B', 'apo70000098', '70000098', N'Ximena', N'Reyes Vargas', N'Rosa Reyes Condori', '965974034'),
 (N'3.°', 'B', 'apo70000099', '70000099', N'Daniel', N'Mamani Huamán', N'Marta Mamani Torres', '946356183'),
 (N'3.°', 'B', 'apo70000100', '70000100', N'Mateo', N'Mendoza Mamani', N'Gloria Mendoza Lozano', '973185648'),
 (N'3.°', 'C', 'apo70000101', '70000101', N'Nicolás', N'Gutiérrez Espinoza', N'Óscar Gutiérrez Ccahuana', '974383227'),
 (N'3.°', 'C', 'apo70000102', '70000102', N'Valentina', N'Campos Chávez', N'José Campos Arias', '975767327'),
 (N'3.°', 'C', 'apo70000103', '70000103', N'Micaela', N'Navarro Espinoza', N'Alberto Navarro Campos', '972561166'),
 (N'3.°', 'C', 'apo70000104', '70000104', N'Daniela', N'Cruz Campos', N'César Cruz Vargas', '939298954'),
 (N'3.°', 'C', 'apo70000105', '70000105', N'Leonardo', N'Vega Flores', N'Nancy Vega Díaz', '992733156'),
 (N'3.°', 'C', 'apo70000106', '70000106', N'Abigail', N'Cruz Ccahuana', N'Óscar Cruz Díaz', '952672583'),
 (N'3.°', 'C', 'apo70000107', '70000107', N'Fabricio', N'Mendoza Flores', N'Teresa Mendoza Soto', '944559740'),
 (N'3.°', 'C', 'apo70000108', '70000108', N'Alonso', N'Cruz Vega', N'César Cruz Arias', '924275967'),
 (N'3.°', 'C', 'apo70000109', '70000109', N'Adrián', N'Torres Sánchez', N'Gloria Torres Ramos', '967577805'),
 (N'3.°', 'C', 'apo70000110', '70000110', N'Alonso', N'Huamán Cruz', N'Víctor Huamán Navarro', '946330439'),
 (N'3.°', 'D', 'apo70000111', '70000111', N'Matías', N'Huamán Mamani', N'Óscar Huamán Espinoza', '997359092'),
 (N'3.°', 'D', 'apo70000112', '70000112', N'Nicole', N'Ortiz Mamani', N'Víctor Ortiz Huamán', '991439583'),
 (N'3.°', 'D', 'apo70000113', '70000113', N'Thiago', N'Ortiz Huamán', N'Julio Ortiz Navarro', '998178629'),
 (N'3.°', 'D', 'apo70000114', '70000114', N'Mateo', N'Chávez Navarro', N'Nancy Chávez Campos', '934023137'),
 (N'3.°', 'D', 'apo70000115', '70000115', N'Ariana', N'Reyes Espinoza', N'Julio Reyes Sánchez', '932588066'),
 (N'3.°', 'D', 'apo70000116', '70000116', N'Lucía', N'Soto Mendoza', N'Julio Soto Paredes', '996063778'),
 (N'3.°', 'D', 'apo70000117', '70000117', N'Gabriel', N'Torres Vargas', N'Gloria Torres Cruz', '958876296'),
 (N'3.°', 'D', 'apo70000118', '70000118', N'Diego', N'Sánchez Medina', N'Silvia Sánchez Huamán', '953554102'),
 (N'3.°', 'D', 'apo70000119', '70000119', N'Abigail', N'Rojas Díaz', N'Juan Rojas Rojas', '922704519'),
 (N'3.°', 'D', 'apo70000120', '70000120', N'Daniela', N'Gutiérrez Soto', N'Juan Gutiérrez Mendoza', '977546866'),
 (N'4.°', 'A', 'apo70000121', '70000121', N'Valeria', N'Paredes Medina', N'Silvia Paredes Ramos', '932906819'),
 (N'4.°', 'A', 'apo70000122', '70000122', N'Micaela', N'Ramos Espinoza', N'Teresa Ramos Mamani', '966636259'),
 (N'4.°', 'A', 'apo70000123', '70000123', N'Iker', N'Vega Espinoza', N'Víctor Vega Vargas', '994469432'),
 (N'4.°', 'A', 'apo70000124', '70000124', N'Gabriel', N'Ccahuana Díaz', N'Rosa Ccahuana Silva', '938327121'),
 (N'4.°', 'A', 'apo70000125', '70000125', N'Sebastián', N'Soto Flores', N'Patricia Soto Reyes', '993508297'),
 (N'4.°', 'A', 'apo70000126', '70000126', N'Sebastián', N'Mendoza Ramos', N'Silvia Mendoza Ramos', '967431352'),
 (N'4.°', 'A', 'apo70000127', '70000127', N'Sofía', N'Arias Silva', N'Alberto Arias Vargas', '959019939'),
 (N'4.°', 'A', 'apo70000128', '70000128', N'Micaela', N'Cruz Sánchez', N'Alberto Cruz Campos', '957537639'),
 (N'4.°', 'A', 'apo70000129', '70000129', N'Sofía', N'Quispe Navarro', N'Julio Quispe Mamani', '939875714'),
 (N'4.°', 'A', 'apo70000130', '70000130', N'Iker', N'Salazar Ccahuana', N'José Salazar Lozano', '999928734'),
 (N'4.°', 'B', 'apo70000131', '70000131', N'Bruno', N'Medina Flores', N'Hugo Medina Sánchez', '956431565'),
 (N'4.°', 'B', 'apo70000132', '70000132', N'Alonso', N'Campos Cruz', N'Marta Campos Campos', '947059680'),
 (N'4.°', 'B', 'apo70000133', '70000133', N'Valeria', N'Condori Rojas', N'Rosa Condori Cruz', '970725226'),
 (N'4.°', 'B', 'apo70000134', '70000134', N'Rodrigo', N'Navarro Arias', N'Hugo Navarro Ccahuana', '974240714'),
 (N'4.°', 'B', 'apo70000135', '70000135', N'Thiago', N'Silva Gutiérrez', N'Julio Silva Torres', '924113414'),
 (N'4.°', 'B', 'apo70000136', '70000136', N'Ximena', N'Mamani Vega', N'Alberto Mamani Torres', '916199453'),
 (N'4.°', 'B', 'apo70000137', '70000137', N'Diego', N'Soto Ccahuana', N'Marta Soto Ortiz', '954951322'),
 (N'4.°', 'B', 'apo70000138', '70000138', N'Santiago', N'Díaz Castillo', N'Óscar Díaz Reyes', '972903586'),
 (N'4.°', 'B', 'apo70000139', '70000139', N'Rodrigo', N'Castillo Cruz', N'Óscar Castillo Paredes', '915541091'),
 (N'4.°', 'B', 'apo70000140', '70000140', N'Nicole', N'Mendoza Sánchez', N'Víctor Mendoza Soto', '925262863'),
 (N'4.°', 'C', 'apo70000141', '70000141', N'Daniel', N'Salazar Díaz', N'Juan Salazar Díaz', '939335391'),
 (N'4.°', 'C', 'apo70000142', '70000142', N'Lucía', N'Cruz Sánchez', N'Julio Cruz Vargas', '942351297'),
 (N'4.°', 'C', 'apo70000143', '70000143', N'Nicolás', N'Chávez Soto', N'Silvia Chávez Vega', '987892505'),
 (N'4.°', 'C', 'apo70000144', '70000144', N'Nicole', N'Vega Cruz', N'Teresa Vega Cruz', '926396948'),
 (N'4.°', 'C', 'apo70000145', '70000145', N'Adrián', N'Condori Flores', N'Teresa Condori Sánchez', '964411918'),
 (N'4.°', 'C', 'apo70000146', '70000146', N'Sebastián', N'Navarro Paredes', N'César Navarro Torres', '942791500'),
 (N'4.°', 'C', 'apo70000147', '70000147', N'Daniela', N'Soto Navarro', N'Teresa Soto Cruz', '945754149'),
 (N'4.°', 'C', 'apo70000148', '70000148', N'Diego', N'Chávez Mendoza', N'José Chávez Ramos', '994675808'),
 (N'4.°', 'C', 'apo70000149', '70000149', N'Gabriel', N'Quispe Silva', N'Hugo Quispe Mamani', '999525199'),
 (N'4.°', 'C', 'apo70000150', '70000150', N'Luana', N'Campos Cruz', N'José Campos Lozano', '935501820'),
 (N'4.°', 'D', 'apo70000151', '70000151', N'Matías', N'Rojas Vargas', N'Rosa Rojas Mendoza', '997492235'),
 (N'4.°', 'D', 'apo70000152', '70000152', N'Isabella', N'Torres Vargas', N'José Torres Díaz', '944808258'),
 (N'4.°', 'D', 'apo70000153', '70000153', N'Ariana', N'Mendoza Mamani', N'Gloria Mendoza Díaz', '917143479'),
 (N'4.°', 'D', 'apo70000154', '70000154', N'Joaquín', N'Mamani Salazar', N'Hugo Mamani Flores', '942519232'),
 (N'4.°', 'D', 'apo70000155', '70000155', N'Alessandra', N'Salazar Mamani', N'Rosa Salazar Sánchez', '993554858'),
 (N'4.°', 'D', 'apo70000156', '70000156', N'Mariana', N'Mendoza Mamani', N'Víctor Mendoza Cruz', '955243717'),
 (N'4.°', 'D', 'apo70000157', '70000157', N'Ximena', N'Sánchez Flores', N'Juan Sánchez Espinoza', '919974432'),
 (N'4.°', 'D', 'apo70000158', '70000158', N'Thiago', N'Condori Vega', N'Gloria Condori Campos', '973667615'),
 (N'4.°', 'D', 'apo70000159', '70000159', N'Thiago', N'Medina Navarro', N'Óscar Medina Mendoza', '976434800'),
 (N'4.°', 'D', 'apo70000160', '70000160', N'Thiago', N'Torres Campos', N'César Torres Flores', '976842956'),
 (N'5.°', 'A', 'apo70000161', '70000161', N'Daniel', N'Gutiérrez Cruz', N'Patricia Gutiérrez Flores', '989357622'),
 (N'5.°', 'A', 'apo70000162', '70000162', N'Daniela', N'Castillo Quispe', N'César Castillo Silva', '942850936'),
 (N'5.°', 'A', 'apo70000163', '70000163', N'Samuel', N'Medina Condori', N'Teresa Medina Vargas', '981303284'),
 (N'5.°', 'A', 'apo70000164', '70000164', N'Ana', N'Gutiérrez Lozano', N'Teresa Gutiérrez Reyes', '971496342'),
 (N'5.°', 'A', 'apo70000165', '70000165', N'Joaquín', N'Medina Díaz', N'Teresa Medina Ccahuana', '977459442'),
 (N'5.°', 'A', 'apo70000166', '70000166', N'Matías', N'Quispe Flores', N'Julio Quispe Torres', '992274868'),
 (N'5.°', 'A', 'apo70000167', '70000167', N'Thiago', N'Paredes Chávez', N'Juan Paredes Sánchez', '991673863'),
 (N'5.°', 'A', 'apo70000168', '70000168', N'Sofía', N'Salazar Arias', N'Juan Salazar Sánchez', '988820916'),
 (N'5.°', 'A', 'apo70000169', '70000169', N'Mariana', N'Campos Gutiérrez', N'Hugo Campos Navarro', '919161246'),
 (N'5.°', 'A', 'apo70000170', '70000170', N'Bruno', N'Sánchez Soto', N'Hugo Sánchez Ortiz', '949916108'),
 (N'5.°', 'B', 'apo70000171', '70000171', N'Nicole', N'Paredes Díaz', N'Nancy Paredes Vega', '952030237'),
 (N'5.°', 'B', 'apo70000172', '70000172', N'Sebastián', N'Ccahuana Mamani', N'Juan Ccahuana Ramos', '938632146'),
 (N'5.°', 'B', 'apo70000173', '70000173', N'Santiago', N'Reyes Soto', N'César Reyes Castillo', '999213390'),
 (N'5.°', 'B', 'apo70000174', '70000174', N'Camila', N'Castillo Salazar', N'Julio Castillo Ccahuana', '954994952'),
 (N'5.°', 'B', 'apo70000175', '70000175', N'Rodrigo', N'Ramos Ccahuana', N'Nancy Ramos Lozano', '982870673'),
 (N'5.°', 'B', 'apo70000176', '70000176', N'Rodrigo', N'Reyes Chávez', N'Patricia Reyes Vega', '970687035'),
 (N'5.°', 'B', 'apo70000177', '70000177', N'Abigail', N'Vargas Rojas', N'Víctor Vargas Vargas', '932794727'),
 (N'5.°', 'B', 'apo70000178', '70000178', N'Daniel', N'Huamán Sánchez', N'César Huamán Arias', '976692237'),
 (N'5.°', 'B', 'apo70000179', '70000179', N'Samuel', N'Vargas Ortiz', N'José Vargas Navarro', '990077821'),
 (N'5.°', 'B', 'apo70000180', '70000180', N'Gabriel', N'Vargas Campos', N'Silvia Vargas Castillo', '994481670'),
 (N'5.°', 'C', 'apo70000181', '70000181', N'Alonso', N'Vargas Navarro', N'Patricia Vargas Campos', '957073289'),
 (N'5.°', 'C', 'apo70000182', '70000182', N'Alonso', N'Castillo Torres', N'Silvia Castillo Cruz', '967614636'),
 (N'5.°', 'C', 'apo70000183', '70000183', N'Fernanda', N'Vega Navarro', N'Gloria Vega Mamani', '988464085'),
 (N'5.°', 'C', 'apo70000184', '70000184', N'Ana', N'Quispe Castillo', N'Víctor Quispe Mendoza', '975837758'),
 (N'5.°', 'C', 'apo70000185', '70000185', N'Matías', N'Campos Mendoza', N'Silvia Campos Cruz', '935435654'),
 (N'5.°', 'C', 'apo70000186', '70000186', N'Ariana', N'Flores Quispe', N'Marta Flores Ramos', '963357483'),
 (N'5.°', 'C', 'apo70000187', '70000187', N'Nicole', N'Espinoza Arias', N'Marta Espinoza Paredes', '941541992'),
 (N'5.°', 'C', 'apo70000188', '70000188', N'Ana', N'Quispe Silva', N'Rosa Quispe Soto', '955869501'),
 (N'5.°', 'C', 'apo70000189', '70000189', N'Iker', N'Arias Paredes', N'Marta Arias Arias', '924157186'),
 (N'5.°', 'C', 'apo70000190', '70000190', N'Adrián', N'Vega Navarro', N'Pilar Vega Gutiérrez', '989785147'),
 (N'5.°', 'D', 'apo70000191', '70000191', N'Valeria', N'Espinoza Castillo', N'Julio Espinoza Sánchez', '972580453'),
 (N'5.°', 'D', 'apo70000192', '70000192', N'Sebastián', N'Lozano Mamani', N'Silvia Lozano Soto', '927574622'),
 (N'5.°', 'D', 'apo70000193', '70000193', N'Luana', N'Paredes Silva', N'Rosa Paredes Sánchez', '976823433'),
 (N'5.°', 'D', 'apo70000194', '70000194', N'Micaela', N'Espinoza Ortiz', N'Marta Espinoza Rojas', '964714183'),
 (N'5.°', 'D', 'apo70000195', '70000195', N'Valeria', N'Chávez Reyes', N'Gloria Chávez Paredes', '924626460'),
 (N'5.°', 'D', 'apo70000196', '70000196', N'Ximena', N'Torres Vega', N'Julio Torres Ccahuana', '991387268'),
 (N'5.°', 'D', 'apo70000197', '70000197', N'Bruno', N'Soto Navarro', N'Silvia Soto Navarro', '923735094'),
 (N'5.°', 'D', 'apo70000198', '70000198', N'Fabricio', N'Lozano Soto', N'Rosa Lozano Paredes', '995060041'),
 (N'5.°', 'D', 'apo70000199', '70000199', N'Nicole', N'Chávez Gutiérrez', N'Silvia Chávez Vega', '992536111'),
 (N'5.°', 'D', 'apo70000200', '70000200', N'Santiago', N'Castillo Ortiz', N'Teresa Castillo Cruz', '966245341')
) AS v(Grado, Letra, Username, DNI, Nombre, Apellido, Apoderado, Telefono)
JOIN Grado g ON g.Nombre = v.Grado
JOIN Seccion s ON s.GradoId = g.GradoId AND s.Nombre = v.Letra
JOIN Usuario u ON u.Username = v.Username;
GO

/* ---------- Actividad de ejemplo (para que reportes y panel tengan datos) ---------- */
-- Calificativos: bimestres 1 a 3 completos; en el bimestre 4 (abierto) falta registrar un tercio de las cargas.
-- El valor sale de un hash fijo, así el script siempre genera los mismos datos.
-- Los alumnos cuyo Id es múltiplo de 9 tienen un rendimiento más bajo (aparecen en las alertas del panel).
INSERT INTO Nota (AlumnoId, EvaluacionId, Calificativo, Fecha)
SELECT a.AlumnoId, e.EvaluacionId,
       CASE WHEN x.r < 1 THEN 'C' WHEN x.r < 7 THEN 'B' WHEN x.r < 16 THEN 'A' ELSE 'AD' END,
       CAST(DATEADD(DAY, -5, p.FechaFin) AS DATETIME2)
FROM Evaluacion e
JOIN CursoSeccion cs ON cs.CursoSeccionId = e.CursoSeccionId
JOIN Periodo p       ON p.PeriodoId = e.PeriodoId
JOIN Alumno a        ON a.SeccionId = cs.SeccionId
CROSS APPLY (SELECT ABS(CHECKSUM(CONCAT(a.AlumnoId, '|', e.EvaluacionId))) % 20
                    - CASE WHEN a.AlumnoId % 9 = 0 THEN 7 ELSE 0 END AS r) AS x
WHERE p.BimestreNum <= 3
   OR (p.BimestreNum = 4 AND cs.CursoSeccionId % 3 <> 0);

-- Asistencia: dos semanas de clases en cada bimestre (en el 4.°, lo que va del bimestre)
INSERT INTO Asistencia (AlumnoId, CursoSeccionId, Fecha, Estado, Justificada)
SELECT a.AlumnoId, cs.CursoSeccionId, d.Fecha,
       CASE WHEN x.r < 2 THEN 'F' WHEN x.r < 5 THEN 'T' ELSE 'A' END,
       CASE WHEN x.r = 0 THEN 1 ELSE 0 END
FROM CursoSeccion cs
JOIN Alumno a ON a.SeccionId = cs.SeccionId
CROSS JOIN (VALUES
  (CAST('2026-04-13' AS DATE)), ('2026-04-14'), ('2026-04-15'), ('2026-04-16'), ('2026-04-17'),
  ('2026-04-20'), ('2026-04-21'), ('2026-04-22'), ('2026-04-23'), ('2026-04-24'),
  ('2026-06-15'), ('2026-06-16'), ('2026-06-17'), ('2026-06-18'), ('2026-06-19'),
  ('2026-06-22'), ('2026-06-23'), ('2026-06-24'), ('2026-06-25'), ('2026-06-26'),
  ('2026-09-28'), ('2026-09-29'), ('2026-09-30'), ('2026-10-01'), ('2026-10-02'),
  ('2026-10-05'), ('2026-10-06'), ('2026-10-07'), ('2026-10-08'), ('2026-10-09'),
  ('2026-10-19'), ('2026-10-20'), ('2026-10-21'), ('2026-10-22'), ('2026-10-23'),
  ('2026-10-26'), ('2026-10-27'), ('2026-10-28'), ('2026-10-29')
) AS d(Fecha)
CROSS APPLY (SELECT ABS(CHECKSUM(CONCAT(a.AlumnoId, '|', cs.CursoSeccionId, '|', d.Fecha))) % 50
                    - CASE WHEN a.AlumnoId % 9 = 0 THEN 4 ELSE 0 END AS r) AS x;

-- Consolidado de los bimestres cerrados (lo mismo que hace la app al cerrar un periodo, RF07):
-- calificativo del área = promedio redondeado de puntos (AD=4, A=3, B=2, C=1); si hay alguna C
-- y la suma no pasa de 7, el área queda en C.
WITH Puntos AS (
    SELECT n.AlumnoId, e.CursoSeccionId, e.PeriodoId,
           SUM(CASE n.Calificativo WHEN 'AD' THEN 4 WHEN 'A' THEN 3 WHEN 'B' THEN 2 ELSE 1 END) AS Suma,
           COUNT(*) AS Cantidad,
           MAX(CASE WHEN n.Calificativo = 'C' THEN 1 ELSE 0 END) AS TieneC
    FROM Nota n
    JOIN Evaluacion e ON e.EvaluacionId = n.EvaluacionId
    JOIN Periodo p    ON p.PeriodoId = e.PeriodoId
    WHERE p.Cerrado = 1
    GROUP BY n.AlumnoId, e.CursoSeccionId, e.PeriodoId
), Area AS (
    SELECT AlumnoId, CursoSeccionId, PeriodoId,
           CASE WHEN TieneC = 1 AND Suma <= 7 THEN 'C'
                ELSE CASE CAST(FLOOR(CAST(Suma AS DECIMAL(9,4)) / Cantidad + 0.5) AS INT)
                          WHEN 4 THEN 'AD' WHEN 3 THEN 'A' WHEN 2 THEN 'B' ELSE 'C' END
           END AS Calificativo
    FROM Puntos
)
INSERT INTO DetalleRendimiento (AlumnoId, CursoSeccionId, PeriodoId, Calificativo,
        TotalAsistencias, TotalFaltas, TotalTardanzas, TotalJustificadas, EstadoAprobacion)
SELECT ar.AlumnoId, ar.CursoSeccionId, ar.PeriodoId, ar.Calificativo,
       ISNULL(SUM(CASE WHEN s.Estado = 'A' THEN 1 ELSE 0 END), 0),
       ISNULL(SUM(CASE WHEN s.Estado = 'F' AND s.Justificada = 0 THEN 1 ELSE 0 END), 0),
       ISNULL(SUM(CASE WHEN s.Estado = 'T' THEN 1 ELSE 0 END), 0),
       ISNULL(SUM(CASE WHEN s.Estado = 'F' AND s.Justificada = 1 THEN 1 ELSE 0 END), 0),
       CASE ar.Calificativo WHEN 'C' THEN 'EN INICIO' WHEN 'B' THEN 'EN PROCESO' ELSE 'LOGRADO' END
FROM Area ar
JOIN Periodo p ON p.PeriodoId = ar.PeriodoId
LEFT JOIN Asistencia s ON s.AlumnoId = ar.AlumnoId AND s.CursoSeccionId = ar.CursoSeccionId
                      AND s.Fecha BETWEEN p.FechaInicio AND p.FechaFin
GROUP BY ar.AlumnoId, ar.CursoSeccionId, ar.PeriodoId, ar.Calificativo;

-- Conclusiones descriptivas de los bimestres cerrados (texto según el nivel alcanzado)
INSERT INTO ConclusionDescriptiva (AlumnoId, EvaluacionId, Texto, Fecha)
SELECT n.AlumnoId, n.EvaluacionId,
       LEFT(CASE RTRIM(n.Calificativo)
            WHEN 'AD' THEN CONCAT(N'Supera lo esperado en "', k.Nombre, N'". Se recomienda proponerle retos de mayor complejidad.')
            WHEN 'A'  THEN CONCAT(N'Logra lo esperado en "', k.Nombre, N'". Se recomienda mantener la práctica constante.')
            WHEN 'B'  THEN CONCAT(N'Está en proceso de lograr "', k.Nombre, N'". Requiere acompañamiento y práctica guiada.')
            ELSE           CONCAT(N'Muestra dificultades en "', k.Nombre, N'". Necesita refuerzo en clase y apoyo en casa.')
       END, 500),
       n.Fecha
FROM Nota n
JOIN Evaluacion e   ON e.EvaluacionId = n.EvaluacionId
JOIN Periodo p      ON p.PeriodoId = e.PeriodoId
JOIN CursoSeccion cs ON cs.CursoSeccionId = e.CursoSeccionId
JOIN Competencia k  ON k.CursoId = cs.CursoId AND k.Codigo = e.CodigoCompetencia
WHERE p.Cerrado = 1;

-- Bitácora: cierres de los bimestres 1 a 3
INSERT INTO Auditoria (Tabla, ClavePrimaria, UsuarioId, FechaCambio, ValorAnterior, ValorNuevo, Accion)
SELECT 'Periodo', CONCAT('PeriodoId=', p.PeriodoId), p.CerradoPorUsuarioId, p.FechaCierre,
       'ABIERTO', CONCAT('CERRADO (', p.AnioLectivo, ' - Bimestre ', p.BimestreNum, ')'), 'CERRAR_PERIODO'
FROM Periodo p
WHERE p.Cerrado = 1;
GO

/* ---------- Verificación rápida ---------- */
SELECT 'Usuario' AS Tabla, COUNT(*) AS Filas FROM Usuario
UNION ALL SELECT 'Docente', COUNT(*) FROM Docente
UNION ALL SELECT 'Seccion', COUNT(*) FROM Seccion
UNION ALL SELECT 'Alumno', COUNT(*) FROM Alumno
UNION ALL SELECT 'CursoSeccion', COUNT(*) FROM CursoSeccion
UNION ALL SELECT 'Evaluacion', COUNT(*) FROM Evaluacion
UNION ALL SELECT 'Nota', COUNT(*) FROM Nota
UNION ALL SELECT 'Asistencia', COUNT(*) FROM Asistencia
UNION ALL SELECT 'DetalleRendimiento', COUNT(*) FROM DetalleRendimiento
UNION ALL SELECT 'DocenteArea', COUNT(*) FROM DocenteArea
UNION ALL SELECT 'ConclusionDescriptiva', COUNT(*) FROM ConclusionDescriptiva;
GO
