# Sistema de Gestión Académica - Colegio Particular Pitágoras

Aplicación de escritorio en **Java 17 + Swing (FlatLaf)** con **SQL Server**, organizada en
**MVC + DAO + SOLID**, con pruebas **TDD** (JUnit 5 + Mockito). Perfiles: **Director/Coordinador**,
**Docente** y **Alumno/Apoderado**.

## 1. Puesta en marcha

1. **Base de datos** (SQL Server Management Studio), en este orden:
   1. `database/01_bd_gestion_academica.sql`: crea la BD (si ya existe, la **elimina y la vuelve a crear**) con datos de prueba.
   2. `database/02_usuario_aplicacion.sql`: crea el login `sga_app` (mínimo privilegio). Cambie `CAMBIAR_CLAVE`.
   3. (Servidor) `database/03_respaldo_diario.bat`: respaldo automático diario, que conserva 7 copias (RNF07). Ver cabecera del archivo.
2. **Configuración**: los datos de la libreta (DRE, UGEL, código modular) se ponen en `config.properties`
   con las claves `colegio.*` (vea el ejemplo). Copie `src/main/resources/config.example.properties` como `config.properties` y
   complete usuario y clave. Ese archivo **no se sube a Git**.
3. **Ejecutar**: abra el proyecto Maven en NetBeans/IntelliJ y ejecute `pe.edu.pitagoras.sga.Main`.
   - Pruebas: `mvn test` · Javadoc: `mvn javadoc:javadoc` · JAR ejecutable: `mvn package`.

## 2. Datos de prueba (todos ficticios)

| Perfil | Usuario | Clave |
|---|---|---|
| Director / Coordinador | `director`, `coordinador` | `Pitagoras2026` |
| Docente (10) | `docente01` … `docente10` | `Pitagoras2026` |
| Apoderado (200) | `apo` + DNI del alumno, p. ej. `apo70000001` | `Pitagoras2026` |
| Apoderado (primer ingreso, RF02) | `apo70000002` | `Pitagoras2026` (pide cambiarla) |

- 5 grados × 4 secciones (A y B mañana, C y D tarde) = **20 secciones**, **10 alumnos por sección** (200).
- 5 áreas: Matemática (4 competencias), Comunicación, Ciencia y Tecnología, Ciencias Sociales e Inglés (3 cada una).
- Cada área tiene 2 docentes: uno dicta en A-B y el otro en C-D (`docente01`: Matemática A-B, `docente02`: Matemática C-D, …).
- Bimestres 1 a 3 **cerrados** con notas y conclusiones descriptivas; bimestre 4 **abierto** con notas parciales.
- Asistencia de ejemplo en los 4 bimestres (dos semanas por bimestre).
- **Especialidad**: cada docente tiene de 1 a 3 áreas (tabla `DocenteArea`); algunos dictan dos
  (`docente01`: Matemática y Ciencia y Tecnología). Carga académica solo ofrece docentes de esa área.

## 3. Arquitectura

```
pe.edu.pitagoras.sga
├── Main                      arranque (FlatLaf, conexión, ventana de login)
├── config/                   AppConfig, ConexionBD (HikariCP), Aplicacion (raíz de composición)
├── model/                    auth, academico, evaluacion, gestion (records y reglas: ReglaCalificativo)
├── dao/interfaces/           contratos IxxxDAO (el service solo conoce interfaces → DIP)
├── dao/impl/                 JDBC con PreparedStatement y transacciones (JdbcSupport)
├── service/                  reglas de negocio y permisos por rol (Permisos, RNF03)
├── controller/               un controlador por módulo; guarda la sesión
├── view/                     Swing: auth, principal, docente, apoderado, director, comun (Tema)
└── util/                     BCryptUtil, GeneradorClave, Validaciones, PdfReportes, ExcelReportes
```

Flujo: **Vista → Controller → Service → DAO (interfaz) → SQL Server**. Las vistas nunca tocan JDBC;
las consultas corren en segundo plano (`Tarea`, SwingWorker) para que la ventana no se congele.

**SOLID**: S (un service por responsabilidad), O/L (DAO intercambiables por interfaz, por ejemplo mocks en pruebas),
I (interfaces pequeñas por agregado), D (los services reciben interfaces por constructor; solo `Aplicacion` crea las implementaciones).

## 4. Librerías (rúbrica)

| Librería | Uso |
|---|---|
| Google Guava | `Preconditions`, `ImmutableList` en services y modelo |
| Apache POI | Exportación a Excel: nómina, acta de notas y consolidado SIAGIE (RF19) |
| Apache Commons Lang | `StringUtils`, `Validate` en validaciones y configuración |
| Logback (SLF4J) | Bitácora técnica en `logs/sga.log` (sin contraseñas ni datos de menores) |
| OpenPDF | Libreta, nómina, acta, asistencia y consolidado en PDF (RF17, RF18) |
| HikariCP / BCrypt / FlatLaf / MigLayout | Pool (RNF05), hash de claves (RNF02), interfaz |

## 5. Seguridad

- Contraseñas con **BCrypt costo 12** y sal aleatoria; nunca en texto plano ni en logs (RNF02).
- Bloqueo de 15 minutos tras 3 intentos fallidos; cambio obligatorio en el primer ingreso (RF02).
- Permisos verificados **en cada service** según el rol; el apoderado solo ve a su alumno (RNF03).
- Solo `PreparedStatement` (sin concatenar datos del usuario); entradas validadas en `Validaciones`.
- Login de BD con mínimo privilegio; credenciales fuera del código y de Git.
- Bitácora de auditoría (usuario, fecha, valor anterior y nuevo) en notas, asistencia, periodos y cuentas (RF09).

## 6. Trazabilidad de requerimientos

| RF | Dónde |
|---|---|
| RF01 Autenticación por perfil | `LoginFrame`, `AuthService`, menú por rol en `FrmPrincipal` |
| RF02 Cambio de clave, primer ingreso, bloqueo | `CambiarClaveDialog`, `AuthService` |
| RF03 Gestión de usuarios | Usuarios: `UsuarioService` (crear, editar, activar/desactivar, restablecer); cuenta del apoderado al inscribir |
| RF04 Estudiantes e inscripción | Inscripción de alumnos: `AlumnoService` |
| RF05 Estructura académica | Configuración académica y Periodos: `AcademicoService` |
| RF06 Carga académica | Carga académica: `AcademicoService.asignarCarga` |
| RF07 Cierre/reapertura de periodo | Periodos: `AcademicoService.cerrarPeriodo/reabrirPeriodo` |
| RF08 Panel de indicadores | Panel de indicadores: `ReporteService.indicadores` (por grado, sección y área; asistencia; avance; alertas C) |
| RF09 Bitácora | `JdbcSupport.auditar` + Auditoría y respaldo |
| RF10 Copias de seguridad | `RespaldoDAOImpl` + `03_respaldo_diario.bat` |
| RF11 Carga del docente | Mi carga académica |
| RF12 Asistencia en lote | Asistencia (docente) |
| RF13 Calificativos AD/A/B/C | Calificaciones (docente) |
| RF14 Calificativo anual = bimestre 4 | `ReglaCalificativo.calificativoAnual` |
| RF15-RF17 Consultas del apoderado | Calificaciones, Asistencia, Libreta de notas (formato del Informe de progreso del MINEDU: competencias por bimestre, nivel final, conclusiones descriptivas, asistencia por bimestre y situación final) |
| RF18 Reportes PDF con vista previa e impresión | Reportes (docente) y Reportes y exportación (dirección) |
| RF19 Excel y consolidado bimestral | `ExcelReportes`, consolidado por sección |

## 7. Pruebas

`src/test/java`: 97 pruebas (`AuthServiceTest`, `DocenteServiceTest`, `DireccionServicesTest`,
`ConsultasTest`, `ReglaCalificativoTest`, `BCryptUtilTest`, `ValidacionesTest`). Los DAO se simulan con
Mockito, así las pruebas no necesitan SQL Server.

## 8. Manual de usuario

Ver `docs/Manual_de_usuario.md` (RNF11).
