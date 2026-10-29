# Proyecto Académico - BD_GestionAcademica_Pitagoras
## Módulo Vista de Usuario (Autor: Omar Bavin)

### Instrucciones de Configuración y Ejecución:

1. **Base de Datos:**
   Cree una base de datos en MySQL con el nombre `BD_GestionAcademica_Pitagoras` y ejecute el siguiente script SQL:
   ```sql
   CREATE DATABASE BD_GestionAcademica_Pitagoras;
   USE BD_GestionAcademica_Pitagoras;

   CREATE TABLE usuario (
       id INT AUTO_INCREMENT PRIMARY KEY,
       nombre VARCHAR(50) NOT NULL,
       apellido VARCHAR(50) NOT NULL,
       correo VARCHAR(100) NOT NULL,
       tipo_usuario VARCHAR(30) NOT NULL,
       estado VARCHAR(20) NOT NULL
   );
   ```

2. **Compilación y Ejecución (Maven):**
   - Abra el proyecto en su IDE favorito (NetBeans, IntelliJ IDEA o Eclipse).
   - Verifique que las dependencias de Maven se hayan descargado correctamente (`mysql-connector-j`).
   - Ejecute la clase principal ubicada en:
     `pe.edu.utp.pitagoras.main.Main`
