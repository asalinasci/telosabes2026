<#
.SYNOPSIS
    Crea (o actualiza) el usuario administrador de telosabes2026 directamente en MariaDB.

.DESCRIPTION
    El administrador NO se puede crear ni modificar desde la aplicacion: su
    contraseña vive unicamente en la base de datos, siempre cifrada con BCrypt.
    Este script genera el hash y ejecuta el INSERT/UPDATE correspondiente.

    Si no se indica -ClaveRoot, el cliente de MariaDB pedira la contraseña de
    root de forma interactiva (recomendado).

    Nota: la primera vez, la tabla "usuarios" la crea Hibernate al arrancar la
    aplicacion. Si aun no existe, ejecuta antes: .\mvnw.cmd spring-boot:run

.EXAMPLE
    .\db\crear-admin.ps1 -Email "admin@telosabes2026.local" -Clave "MiClaveSecreta1!"

.EXAMPLE
    # Cambiar la contraseña del administrador ya existente
    .\db\crear-admin.ps1 -Email "admin@telosabes2026.local" -Clave "OtraClave2!"
#>
param(
    [Parameter(Mandatory = $true)][string]$Email,
    [Parameter(Mandatory = $true)][string]$Clave,
    [string]$Nombre = "Administrador",
    [string]$BaseDatos = "telosabes2026",
    [string]$UsuarioRoot = "root",
    [string]$ClaveRoot,
    [string]$MariaDb = "C:\Program Files\MariaDB 11.6\bin\mariadb.exe"
)

$ErrorActionPreference = "Stop"

function EscaparSql($texto) {
    return $texto.Replace("\", "\\").Replace("'", "\'")
}

if (-not (Test-Path $MariaDb)) {
    throw "No se encuentra el cliente de MariaDB en '$MariaDb'. Indica la ruta con -MariaDb."
}

if ($Clave.Length -lt 8) {
    throw "La contraseña del administrador debe tener al menos 8 caracteres."
}

if ($Clave.Length -gt 72) {
    throw "La contraseña no puede superar los 72 caracteres (limite de BCrypt)."
}

$raizProyecto = Split-Path $PSScriptRoot -Parent

# ------------------------------------------------------------------
#  1. Generar el hash BCrypt con las librerias del propio proyecto
# ------------------------------------------------------------------
$javaExe = "java"
if ($env:JAVA_HOME) { $javaExe = Join-Path $env:JAVA_HOME "bin\java.exe" }

$archivoClasspath = Join-Path $env:TEMP "telosabes-classpath.txt"
$mvnw = Join-Path $raizProyecto "mvnw.cmd"

Write-Host "1/2 Generando el hash BCrypt de la contraseña..." -ForegroundColor Cyan
& $mvnw -B -q -f (Join-Path $raizProyecto "pom.xml") dependency:build-classpath "-Dmdep.outputFile=$archivoClasspath" | Out-Null

if (-not (Test-Path $archivoClasspath)) {
    throw "No se pudo calcular el classpath del proyecto. Ejecuta antes: .\mvnw.cmd compile"
}

$classpath = (Get-Content -Raw $archivoClasspath).Trim()
$hash = (& $javaExe -cp $classpath (Join-Path $PSScriptRoot "GenerarHash.java") $Clave | Select-Object -First 1)

if ($LASTEXITCODE -ne 0 -or -not $hash) {
    throw "No se pudo generar el hash BCrypt."
}

$hash = $hash.Trim()
Write-Host "    $hash"

# ------------------------------------------------------------------
#  2. Escribir en la base de datos
# ------------------------------------------------------------------
$emailNormalizado = $Email.Trim().ToLower()

$sql = @"
USE $BaseDatos;

INSERT INTO usuarios (nombre, email, clave, rol, activo, fecha_registro)
VALUES ('$(EscaparSql $Nombre)', '$emailNormalizado', '$hash', 'ADMIN', 1, NOW())
ON DUPLICATE KEY UPDATE clave = '$hash', rol = 'ADMIN', activo = 1;

SELECT id, nombre, email, rol FROM usuarios WHERE email = '$emailNormalizado';
"@

$archivoSql = Join-Path $env:TEMP "telosabes-crear-admin.sql"
[System.IO.File]::WriteAllText($archivoSql, $sql, (New-Object System.Text.UTF8Encoding($false)))
$rutaSource = $archivoSql.Replace("\", "/")

try {
    Write-Host "2/2 Escribiendo en MariaDB..." -ForegroundColor Cyan

    if ($ClaveRoot) {
        & $MariaDb -u $UsuarioRoot "--password=$ClaveRoot" -e "source $rutaSource"
    } else {
        & $MariaDb -u $UsuarioRoot -p -e "source $rutaSource"
    }

    if ($LASTEXITCODE -ne 0) {
        throw @"
MariaDB devolvio un error.
Si dice que la tabla 'usuarios' no existe, arranca antes la aplicacion:
    .\mvnw.cmd spring-boot:run
"@
    }
} finally {
    Remove-Item $archivoClasspath -ErrorAction SilentlyContinue
}

Write-Host "`nAdministrador listo: $Email" -ForegroundColor Green
Write-Host "Inicia sesion en http://localhost:8080/login" -ForegroundColor Green
