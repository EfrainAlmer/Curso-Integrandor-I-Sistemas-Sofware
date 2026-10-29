# Sistema de Gestión Académica - Colegio Particular Pitágoras

Proyecto Java Swing estructurado bajo arquitectura **MVC + DAO + SOLID + TDD** con enrutamiento dinámico por rol (**Director / Coordinador**, **Docente** y **Alumno / Apoderado**), listo para abrir y ejecutar en **IntelliJ IDEA**.

## Credenciales de Prueba (Demo Rápida)

| Rol | Usuario | Contraseña | Cursos / Características a Probar |
| :--- | :--- | :--- | :--- |
| **Docente 1** | `docente` | `123456` | **Prof. Carlos Mendoza** — Enseña **Matemática** (4 competencias) y **Ciencia y Tecnología** (3 competencias) en `1.° A` a **`5.° A`**. |
| **Docente 2** | `docente2` | `123456` | **Prof. María Quispe** — Enseña **solo 2 cursos diferentes**: **Comunicación** (3 competencias) y **Ciencias Sociales** (3 competencias) en `1.° A` a **`5.° A`**. |
| **Director / Coordinador** | `director` | `123456` | Panel de indicadores (KPIs y barras multicolor AD/A/B/C), Usuarios, Matrícula, Configuración académica, Carga académica, Periodos, Reportes SIAGIE y Auditoría/Backup `.bak`. |
| **Alumno / Apoderado** | `apoderado` | `123456` | Cambio obligatorio de contraseña en el 1.° ingreso (RF02/CU02) y vista de solo lectura (asistencia, insignias de notas y visor de libreta PDF). |

## Cómo Ejecutar en IntelliJ IDEA

1. Abre **IntelliJ IDEA** -> **File > Open...** y selecciona la carpeta `d:\PROYECTO ACADEMIA`.
2. En la parte superior derecha tienes dos configuraciones listas:
   - **`Main (Pitagoras)`**: Inicia la aplicación gráfica en la ventana de Login.
   - **`DocenteTDDTest (Pruebas TDD)`**: Ejecuta las 6 pruebas unitarias TDD en consola.
