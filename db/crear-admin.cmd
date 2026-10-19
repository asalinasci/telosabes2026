@echo off
REM ------------------------------------------------------------------
REM  Lanzador de crear-admin.ps1
REM  PowerShell bloquea los .ps1 por directiva de ejecucion, asi que
REM  este .cmd los ejecuta con -ExecutionPolicy Bypass solo esta vez.
REM
REM  Uso:
REM    db\crear-admin.cmd -Email "admin@telosabes2026.local" -Clave "MiClave1!"
REM ------------------------------------------------------------------
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0crear-admin.ps1" %*
