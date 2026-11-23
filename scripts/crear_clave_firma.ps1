# Crea la clave de firma de CarVision (una sola vez en la vida de la app).
#
# Android solo instala una actualización si va firmada con LA MISMA clave que la primera versión. Si se pierde esta
# clave o su contraseña, los clientes no podrán actualizar: tendrían que desinstalar y volver a instalar.
#
# Qué hace:
#   - Te pide una contraseña (no se ve al escribirla).
#   - Crea %USERPROFILE%\.carvision\carvision-release.jks (la clave) y keystore.properties (dónde está y su contraseña).
#   - Todo queda FUERA del proyecto: no va en el .zip del código ni en ninguna copia del proyecto.
#
# Después: haz una copia de la carpeta %USERPROFILE%\.carvision (pendrive y nube privada) y guarda la contraseña en tu
# gestor de contraseñas.
#
# Uso (PowerShell, desde la carpeta del proyecto):  powershell -ExecutionPolicy Bypass -File scripts\crear_clave_firma.ps1

$ErrorActionPreference = "Stop"
$dir = Join-Path $env:USERPROFILE ".carvision"
$jks = Join-Path $dir "carvision-release.jks"
$props = Join-Path $dir "keystore.properties"

if (Test-Path $jks) {
    Write-Host "Ya existe una clave en $jks. No se crea otra (perderías la que ya firma la app)." -ForegroundColor Yellow
    exit 1
}

$keytool = if ($env:JAVA_HOME -and (Test-Path (Join-Path $env:JAVA_HOME "bin\keytool.exe"))) {
    Join-Path $env:JAVA_HOME "bin\keytool.exe"
} else { "keytool" }

function Read-Password([string]$prompt) {
    $s = Read-Host -AsSecureString $prompt
    $b = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($s)
    try { [Runtime.InteropServices.Marshal]::PtrToStringBSTR($b) } finally { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($b) }
}

Write-Host "Clave de firma de CarVision" -ForegroundColor Cyan
Write-Host "Elige una contraseña larga (12 caracteres o más) y guárdala bien: no se puede recuperar."
$p1 = Read-Password "Contraseña"
if ($p1.Length -lt 12) { Write-Host "Demasiado corta: mínimo 12 caracteres." -ForegroundColor Red; exit 1 }
$p2 = Read-Password "Repítela"
if ($p1 -ne $p2) { Write-Host "No coinciden. Vuelve a ejecutar el script." -ForegroundColor Red; exit 1 }

New-Item -ItemType Directory -Force $dir | Out-Null
$env:CV_KS_PASS = $p1
try {
    & $keytool -genkeypair -v -keystore $jks -alias carvision -keyalg RSA -keysize 4096 -validity 36500 `
        -dname "CN=CarVision, O=CarVision, C=ES" -storepass:env CV_KS_PASS -keypass:env CV_KS_PASS | Out-Null
    if ($LASTEXITCODE -ne 0) { throw "keytool terminó con error $LASTEXITCODE" }
} finally {
    Remove-Item Env:\CV_KS_PASS -ErrorAction SilentlyContinue
}

@"
storeFile=carvision-release.jks
storePassword=$p1
keyAlias=carvision
keyPassword=$p1
"@ | Set-Content -Encoding ascii $props

Write-Host ""
Write-Host "Listo. Clave creada en $dir" -ForegroundColor Green
Write-Host "1) Copia esa carpeta a un pendrive y a una nube privada."
Write-Host "2) Guarda la contraseña en tu gestor de contraseñas."
Write-Host "3) Dile a Claude que ya está: compilará la versión 1.0.0 firmada con tu clave."
