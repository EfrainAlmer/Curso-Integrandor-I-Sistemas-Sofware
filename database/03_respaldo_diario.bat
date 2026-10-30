@echo off
REM ======================================================================
REM  Respaldo diario automatico de BD_GestionAcademica_Pitagoras (RNF07)
REM  Programar en el servidor con el Programador de tareas de Windows:
REM    schtasks /create /tn "Respaldo SGA" /tr "C:\SGA\03_respaldo_diario.bat" /sc daily /st 19:00 /ru SYSTEM
REM  Conserva las ultimas 7 copias y borra las mas antiguas.
REM  Requiere sqlcmd (viene con SQL Server) y una carpeta de respaldo,
REM  idealmente en una unidad externa (ver Tabla 9 del informe).
REM ======================================================================
setlocal
set SERVIDOR=localhost
set BD=BD_GestionAcademica_Pitagoras
set CARPETA=D:\Respaldos\SGA

if not exist "%CARPETA%" mkdir "%CARPETA%"
for /f %%i in ('powershell -NoProfile -Command "Get-Date -Format yyyyMMdd_HHmmss"') do set SELLO=%%i
set ARCHIVO=%CARPETA%\%BD%_%SELLO%.bak

REM -E usa la autenticacion de Windows de la cuenta que ejecuta la tarea
sqlcmd -S %SERVIDOR% -E -b -Q "BACKUP DATABASE [%BD%] TO DISK = N'%ARCHIVO%' WITH INIT, NAME = N'Respaldo diario SGA'"
if errorlevel 1 (
    echo [%date% %time%] ERROR al generar %ARCHIVO% >> "%CARPETA%\respaldo.log"
    exit /b 1
)
sqlcmd -S %SERVIDOR% -E -b -d %BD% -Q "INSERT INTO CopiaSeguridad (Ruta, UsuarioId, Estado) SELECT N'%ARCHIVO%', UsuarioId, 'EXITOSO' FROM Usuario WHERE Username = 'director'"
echo [%date% %time%] OK %ARCHIVO% >> "%CARPETA%\respaldo.log"

REM Conservar solo las 7 copias mas recientes
powershell -NoProfile -Command "Get-ChildItem '%CARPETA%\*.bak' | Sort-Object LastWriteTime -Descending | Select-Object -Skip 7 | Remove-Item -Force"
endlocal
