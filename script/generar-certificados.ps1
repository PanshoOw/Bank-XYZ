$ErrorActionPreference = "Stop"

$Root = Split-Path -Parent $PSScriptRoot

if (-not (Get-Command keytool -ErrorAction SilentlyContinue)) {
    throw "No se encontró keytool en PATH. Verifica que Java esté instalado y configurado correctamente."
}

$Services = @(
    @{
        Name = "bff-web"
        Alias = "bff-web"
        PasswordVariable = "BFF_WEB_SSL_KEYSTORE_PASSWORD"
    },
    @{
        Name = "bff-mobile"
        Alias = "bff-mobile"
        PasswordVariable = "BFF_MOBILE_SSL_KEYSTORE_PASSWORD"
    },
    @{
        Name = "bff-atm"
        Alias = "bff-atm"
        PasswordVariable = "BFF_ATM_SSL_KEYSTORE_PASSWORD"
    }
)

foreach ($Service in $Services) {

    $Password = [Environment]::GetEnvironmentVariable(
        $Service.PasswordVariable,
        "Process"
    )

    if ([string]::IsNullOrWhiteSpace($Password)) {
        throw "La variable de entorno $($Service.PasswordVariable) no está configurada."
    }

    $KeystorePath = Join-Path `
        $Root `
        "$($Service.Name)\src\main\resources\keystore.p12"

    if (Test-Path $KeystorePath) {
        Remove-Item $KeystorePath -Force
    }

    Write-Host "Generando certificado para $($Service.Name)..."

    & keytool -genkeypair `
        -alias $Service.Alias `
        -keyalg RSA `
        -keysize 2048 `
        -storetype PKCS12 `
        -keystore $KeystorePath `
        -validity 365 `
        -storepass $Password `
        -keypass $Password `
        -dname "CN=localhost, OU=BackendIII, O=DuocUC, C=CL" `
        -ext "SAN=dns:localhost,ip:127.0.0.1"
}

Write-Host ""
Write-Host "Certificados SSL generados correctamente para los tres BFF."