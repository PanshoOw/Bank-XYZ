# Bank XYZ - Backend for Frontend (BFF)

Proyecto desarrollado para la asignatura **Desarrollo Backend III (PBY2203)**.

Esta versión corresponde a la continuidad del proyecto Bank XYZ y a la implementación del patrón arquitectónico **Backend for Frontend (BFF)** para tres tipos de clientes:

- Aplicación Web
- Aplicación Móvil
- Cajero Automático (ATM)

Durante esta etapa se incorporaron mecanismos adicionales de seguridad mediante **HTTPS, certificados SSL/TLS, autenticación y autorización mediante JWT**.

---

## Objetivo

El objetivo del proyecto es implementar el patrón Backend for Frontend, permitiendo que cada tipo de cliente disponga de un backend independiente y adaptado a sus necesidades.

El sistema mantiene un backend principal encargado del acceso a los datos del Banco XYZ y tres BFF independientes:

- BFF Web
- BFF Mobile
- BFF ATM

Cada BFF consume las APIs del backend principal, transforma la información y expone únicamente los datos y operaciones requeridos por su canal.

Además, las APIs de los BFF se encuentran protegidas mediante HTTPS y tokens JWT.

---

## Arquitectura

```text
                           Oracle Database
                                  │
                                  ▼
                      Backend principal Bank XYZ
                         http://localhost:8080
                                  │
                  ┌───────────────┼───────────────┐
                  │               │               │
                  ▼               ▼               ▼
              BFF Web         BFF Mobile        BFF ATM
          https://:8081     https://:8082     https://:8083
                  │               │               │
                  ▼               ▼               ▼
                Web             Móvil           Cajero
```

Los BFF no acceden directamente a Oracle.

El acceso a los datos se mantiene centralizado en el backend principal, mientras que cada BFF se encarga de adaptar la respuesta, aplicar la seguridad correspondiente y exponer las operaciones necesarias para su cliente.

---

## Tecnologías utilizadas

- Java 18
- Spring Boot 4.1.1
- Spring MVC
- Spring Batch
- Spring JDBC
- Spring Security
- Spring Security OAuth2 Resource Server
- JSON Web Token (JWT)
- HTTPS / TLS
- Certificados PKCS12
- Oracle Database
- Oracle Wallet
- Maven
- Git / GitHub

---

## Estructura del proyecto

```text
demo/
│
├── src/
│   └── main/java/com/duoc/demo/
│       ├── Config/
│       ├── Controller/
│       ├── Dto/
│       ├── Model/
│       ├── Processor/
│       ├── Reader/
│       ├── Repository/
│       ├── Service/
│       └── Writer/
│
├── bff-web/
│   └── src/main/java/com/duoc/bffweb/
│       ├── config/
│       ├── controller/
│       ├── dto/
│       └── service/
│
├── bff-mobile/
│   └── src/main/java/com/duoc/bffmobile/
│       ├── config/
│       ├── controller/
│       ├── dto/
│       └── service/
│
├── bff-atm/
│   └── src/main/java/com/duoc/bffatm/
│       ├── config/
│       ├── controller/
│       ├── dto/
│       └── service/
│
├── script/
│   └── generar-certificados.ps1
│
├── README.md
├── PROPUESTA_TECNICA.md
└── pom.xml
```

---

# Backend principal

El backend principal se ejecuta mediante HTTP en:

```text
http://localhost:8080
```

Su función es acceder a los datos almacenados en Oracle y proporcionar las APIs utilizadas internamente por los BFF.

## Endpoints

| Método | Endpoint | Descripción |
|---|---|---|
| GET | `/api/cuentas` | Obtiene todas las cuentas |
| GET | `/api/cuentas/{id}` | Obtiene una cuenta por ID |
| POST | `/api/cuentas/{id}/retiro` | Realiza un retiro |

Ejemplo:

```text
GET http://localhost:8080/api/cuentas/101
```

---

# BFF Web

El BFF Web se ejecuta mediante HTTPS en:

```text
https://localhost:8081
```

Está orientado a navegadores y proporciona información completa para interfaces que requieren mayor nivel de detalle.

## Endpoints

| Método | Endpoint | Descripción |
|---|---|---|
| POST | `/auth/token` | Autentica al usuario y genera un JWT |
| GET | `/api/web/cuentas` | Obtiene la lista completa de cuentas |
| GET | `/api/web/cuentas/{id}` | Obtiene el detalle completo de una cuenta |

La respuesta Web incluye:

- ID de cuenta
- Nombre del cliente
- Edad
- Tipo de cuenta
- Saldo inicial
- Tasa de interés
- Interés generado
- Saldo actual
- Estado

Los endpoints `/api/web/**` requieren un JWT con alcance:

```text
WEB
```

---

# BFF Mobile

El BFF Mobile se ejecuta mediante HTTPS en:

```text
https://localhost:8082
```

Está diseñado para reducir el volumen de datos transferidos a dispositivos móviles.

## Endpoints

| Método | Endpoint | Descripción |
|---|---|---|
| POST | `/auth/token` | Autentica al usuario y genera un JWT |
| GET | `/api/mobile/cuentas` | Obtiene una lista resumida de cuentas |
| GET | `/api/mobile/cuentas/{id}` | Obtiene información resumida de una cuenta |

La respuesta Mobile contiene únicamente:

- ID de cuenta
- Nombre
- Tipo de cuenta
- Saldo actual
- Estado

Los endpoints `/api/mobile/**` requieren un JWT con alcance:

```text
MOBILE
```

---

# BFF ATM

El BFF ATM se ejecuta mediante HTTPS en:

```text
https://localhost:8083
```

Está orientado a operaciones críticas de cajeros automáticos y entrega únicamente la información necesaria para dichas operaciones.

## Endpoints

| Método | Endpoint | Descripción |
|---|---|---|
| POST | `/auth/token` | Autentica al usuario y genera un JWT |
| GET | `/api/atm/cuentas/{id}/saldo` | Consulta el saldo disponible |
| POST | `/api/atm/cuentas/{id}/retiro` | Realiza un retiro |

Los endpoints `/api/atm/**` requieren un JWT con alcance:

```text
ATM
```

Ejemplo de cuerpo para retiro:

```json
{
    "monto": 100
}
```

---

# Personalización y optimización por canal

Cada BFF consume el mismo backend principal, pero adapta la información según las necesidades de su cliente.

```text
Backend principal
        │
        ├── BFF Web
        │     └── Información completa
        │
        ├── BFF Mobile
        │     └── Información esencial y resumida
        │
        └── BFF ATM
              └── Saldo y operaciones de retiro
```

Durante las pruebas realizadas sobre la cuenta `101` se obtuvieron los siguientes tamaños de respuesta:

| Canal | Tamaño de respuesta |
|---|---:|
| Web | 183 bytes |
| Mobile | 99 bytes |
| ATM | 57 bytes |

En esta prueba, Mobile transfirió aproximadamente un **45,9 % menos información que Web**, mientras que ATM transfirió aproximadamente un **68,9 % menos**.

Los tiempos obtenidos en una ejecución de prueba fueron:

| Canal | Tiempo |
|---|---:|
| Web | 0.381755 s |
| Mobile | 0.111764 s |
| ATM | 0.093577 s |

Los tiempos pueden variar entre ejecuciones, por lo que se utilizan únicamente como referencia. La reducción del tamaño de las respuestas demuestra de manera directa la personalización y optimización de datos según el canal.

---

# Seguridad

Los tres BFF implementan una configuración de seguridad independiente.

Se utilizan:

- HTTPS.
- Certificados SSL/TLS autofirmados para el entorno local.
- Keystores PKCS12 independientes por BFF.
- Autenticación mediante usuario y contraseña para solicitar tokens.
- Tokens JWT firmados mediante HMAC SHA-256.
- Autorización mediante scopes específicos por canal.
- Sesiones HTTP stateless.
- Contraseñas y secretos obtenidos desde variables de entorno.

Los scopes utilizados son:

| BFF | Scope requerido |
|---|---|
| Web | `WEB` |
| Mobile | `MOBILE` |
| ATM | `ATM` |

Spring Security interpreta estos scopes como autoridades:

```text
SCOPE_WEB
SCOPE_MOBILE
SCOPE_ATM
```

Una solicitud sin token o con un token inválido responde:

```text
HTTP/1.1 401 Unauthorized
```

Los tokens generados tienen una duración configurada de:

```text
1800 segundos
```

equivalentes a 30 minutos.

---

# Certificados SSL/TLS

Cada BFF utiliza su propio archivo:

```text
keystore.p12
```

Estos archivos contienen claves privadas y no deben almacenarse en el repositorio Git.

Por esta razón se encuentran excluidos mediante `.gitignore`.

Los certificados pueden generarse ejecutando:

```powershell
Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass

.\script\generar-certificados.ps1
```

Antes de ejecutar el script deben encontrarse configuradas las contraseñas:

```powershell
$env:BFF_WEB_SSL_KEYSTORE_PASSWORD='CONTRASEÑA_SSL_WEB'
$env:BFF_MOBILE_SSL_KEYSTORE_PASSWORD='CONTRASEÑA_SSL_MOBILE'
$env:BFF_ATM_SSL_KEYSTORE_PASSWORD='CONTRASEÑA_SSL_ATM'
```

Los certificados utilizados en desarrollo son autofirmados y están configurados para:

```text
localhost
127.0.0.1
```

Al utilizar `curl` durante las pruebas locales se emplea la opción `-k` debido a que los certificados no pertenecen a una autoridad certificadora pública.

---

# Variables de entorno

## Backend principal

```powershell
$env:ORACLE_WALLET_DIR='RUTA_DEL_ORACLE_WALLET'
$env:BANKXYZ_DB_PASSWORD='CONTRASEÑA_ORACLE'
$env:SPRING_BATCH_JOB_ENABLED='false'
```

El Oracle Wallet debe contener el alias de conexión utilizado por el proyecto.

Actualmente la conexión utiliza:

```text
bankxyz_tp
```

## BFF Web

```powershell
$env:BFF_WEB_USERNAME='web_user'
$env:BFF_WEB_PASSWORD='CONTRASEÑA_WEB'
$env:BFF_WEB_SSL_KEYSTORE_PASSWORD='CONTRASEÑA_SSL_WEB'
$env:BFF_WEB_JWT_SECRET='SECRETO_JWT_BASE64'
```

## BFF Mobile

```powershell
$env:BFF_MOBILE_USERNAME='mobile_user'
$env:BFF_MOBILE_PASSWORD='CONTRASEÑA_MOBILE'
$env:BFF_MOBILE_SSL_KEYSTORE_PASSWORD='CONTRASEÑA_SSL_MOBILE'
$env:BFF_MOBILE_JWT_SECRET='SECRETO_JWT_BASE64'
```

## BFF ATM

```powershell
$env:BFF_ATM_USERNAME='atm_user'
$env:BFF_ATM_PASSWORD='CONTRASEÑA_ATM'
$env:BFF_ATM_SSL_KEYSTORE_PASSWORD='CONTRASEÑA_SSL_ATM'
$env:BFF_ATM_JWT_SECRET='SECRETO_JWT_BASE64'
```

Los secretos JWT deben tener al menos 256 bits.

Ejemplo para generar uno temporalmente desde PowerShell:

```powershell
$jwtBytes = New-Object byte[] 32
$rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$rng.GetBytes($jwtBytes)
$rng.Dispose()

$env:BFF_WEB_JWT_SECRET = [Convert]::ToBase64String($jwtBytes)
```

El mismo procedimiento puede utilizarse para Mobile y ATM cambiando el nombre de la variable correspondiente.

---

# Ejecución

Los cuatro servicios deben ejecutarse en terminales independientes.

## Backend principal

```powershell
.\mvnw.cmd spring-boot:run
```

Disponible en:

```text
http://localhost:8080
```

## BFF Web

```powershell
.\mvnw.cmd -f .\bff-web\pom.xml spring-boot:run
```

Disponible en:

```text
https://localhost:8081
```

## BFF Mobile

```powershell
.\mvnw.cmd -f .\bff-mobile\pom.xml spring-boot:run
```

Disponible en:

```text
https://localhost:8082
```

## BFF ATM

```powershell
.\mvnw.cmd -f .\bff-atm\pom.xml spring-boot:run
```

Disponible en:

```text
https://localhost:8083
```

El backend principal debe encontrarse activo para que los BFF puedan consumir sus APIs.

---

# Autenticación mediante JWT

Cada BFF dispone de:

```text
POST /auth/token
```

El endpoint recibe:

```json
{
    "username": "USUARIO",
    "password": "CONTRASEÑA"
}
```

y devuelve:

```json
{
    "accessToken": "TOKEN_JWT",
    "tokenType": "Bearer",
    "expiresIn": 1800
}
```

Posteriormente el token debe enviarse mediante:

```text
Authorization: Bearer TOKEN_JWT
```

---

# Ejemplo de prueba

Una solicitud protegida sin JWT:

```powershell
curl.exe -k -i "https://localhost:8081/api/web/cuentas/101"
```

Respuesta esperada:

```text
HTTP/1.1 401
```

Con un JWT previamente obtenido:

```powershell
curl.exe -k -i `
    -H "Authorization: Bearer $token" `
    "https://localhost:8081/api/web/cuentas/101"
```

Respuesta esperada:

```text
HTTP/1.1 200
```

El mismo mecanismo se aplica a Mobile y ATM utilizando sus respectivos puertos, endpoints y tokens.

---

# Evidencias de ejecución

Durante las pruebas se verificó:

- Acceso rechazado sin JWT.
- Acceso autorizado mediante JWT válido.
- Rechazo de tokens inválidos.
- Ejecución de los tres BFF mediante HTTPS.
- Consulta Web con información completa.
- Consulta Mobile con información resumida.
- Consulta de saldo mediante ATM.
- Retiro mediante ATM.
- Comparación del tamaño de las respuestas de Web, Mobile y ATM.

Estas evidencias se adjuntan junto con el proyecto para demostrar el correcto funcionamiento de las APIs.

---

# Conclusión

La solución implementa el patrón Backend for Frontend mediante tres aplicaciones Spring Boot independientes para Web, Mobile y ATM.

Cada canal dispone de endpoints y respuestas adaptados a sus necesidades, reduciendo la transferencia de información innecesaria y evitando que los clientes dependan de una respuesta genérica.

La seguridad de los BFF fue reforzada mediante HTTPS, certificados independientes, autenticación mediante credenciales, autorización específica por canal y tokens JWT.

De esta forma, el proyecto mantiene la lógica de acceso a datos centralizada en el backend principal y separa las responsabilidades específicas de cada frontend, favoreciendo la modularidad, escalabilidad, seguridad y mantenibilidad de la solución.