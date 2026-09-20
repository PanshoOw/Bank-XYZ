# Bank XYZ - Microservicios con Spring Cloud

Proyecto desarrollado para la asignatura **Desarrollo Backend III (PBY2203)**.

Esta versión corresponde a la **Experiencia 3 - Semana 6**, incorporando una arquitectura distribuida basada en Spring Cloud sobre el proyecto Bank XYZ desarrollado durante las semanas anteriores.

La solución implementa:

- Configuración centralizada mediante Spring Cloud Config.
- Service Discovery mediante Eureka.
- Tres BFF registrados como microservicios.
- Tolerancia a fallos mediante Resilience4j.
- Circuit Breaker y respuestas Fallback.
- Autenticación y autorización mediante JWT.
- Comunicación HTTPS en los BFF.

---

## Objetivo

El objetivo del proyecto es evolucionar la arquitectura BFF existente hacia un ecosistema de microservicios utilizando Spring Cloud.

La solución permite:

- Centralizar la configuración de los microservicios.
- Registrar y descubrir dinámicamente los servicios disponibles.
- Responder de forma controlada cuando el backend principal no se encuentra disponible.
- Mantener autenticación y autorización independiente para Web, Mobile y ATM.
- Mejorar la resiliencia y mantenibilidad del sistema.

---

## Arquitectura

```text
                           ┌─────────────────────┐
                           │    CONFIG SERVER    │
                           │       :8888         │
                           └──────────┬──────────┘
                                      │
                          Configuración centralizada
                                      │
                   ┌──────────────────┼──────────────────┐
                   │                  │                  │
                   ▼                  ▼                  ▼
               BFF Web           BFF Mobile          BFF ATM
            HTTPS :8081         HTTPS :8082        HTTPS :8083
                   │                  │                  │
                   └──────────────────┼──────────────────┘
                                      │
                            ┌─────────▼─────────┐
                            │   EUREKA SERVER   │
                            │       :8761       │
                            └───────────────────┘

                            BFF Web / Mobile / ATM
                                        │
                                Circuit Breaker
                                        │
                                        ▼
                            Backend principal Bank XYZ
                               http://localhost:8080
                                        │
                                        ▼
                                  Oracle Database
```

Los tres BFF se registran en Eureka y obtienen parte de su configuración desde Config Server.

El backend principal continúa centralizando el acceso a Oracle.

---

## Tecnologías utilizadas

- Java 18
- Spring Boot 4.1.1
- Spring Cloud 2025.1.2
- Spring Cloud Config
- Netflix Eureka
- Resilience4j
- Spring MVC
- Spring JDBC
- Spring Batch
- Spring Security
- OAuth2 Resource Server
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
│       └── Backend principal Bank XYZ
│
├── config-server/
│   ├── src/main/java/
│   └── src/main/resources/
│       └── config-repo/
│           ├── bff-web.yml
│           ├── bff-mobile.yml
│           └── bff-atm.yml
│
├── discovery-server/
│   └── Servidor Eureka
│
├── bff-web/
│   └── src/main/java/com/duoc/bffweb/
│
├── bff-mobile/
│   └── src/main/java/com/duoc/bffmobile/
│
├── bff-atm/
│   └── src/main/java/com/duoc/bffatm/
│
├── script/
│   └── generar-certificados.ps1
│
├── README.md
├── PROPUESTA_TECNICA.md
└── pom.xml
```

---

## Spring Cloud Config Server

El Config Server se ejecuta en:

```text
http://localhost:8888
```

Su función es centralizar la configuración operacional de los BFF.

Cada microservicio se identifica mediante:

```yaml
spring:
  application:
    name: bff-web
```

y obtiene su configuración correspondiente desde Config Server.

Ejemplos:

```text
http://localhost:8888/bff-web/default
http://localhost:8888/bff-mobile/default
http://localhost:8888/bff-atm/default
```

Las configuraciones centralizadas incluyen:

- Puerto del servicio.
- Dirección del backend principal.
- Configuración de Eureka.
- Configuración de Actuator.
- Configuración de Resilience4j.
- Identificación del canal.
- Issuer JWT.
- Scope JWT.
- Duración del token.

Las credenciales, secretos JWT y contraseñas de certificados permanecen fuera del repositorio y se obtienen mediante variables de entorno.

Los BFF requieren que Config Server se encuentre disponible para iniciar correctamente.

---

## Eureka Service Discovery

El servidor Eureka se ejecuta en:

```text
http://localhost:8761
```

Los tres BFF se registran automáticamente como servicios:

```text
BFF-WEB
BFF-MOBILE
BFF-ATM
```

Eureka permite mantener un registro dinámico de las instancias disponibles, evitando depender exclusivamente de ubicaciones configuradas manualmente para el descubrimiento de servicios.

Durante las pruebas se verificó que los tres BFF aparecieran simultáneamente en estado:

```text
UP
```

---

## Tolerancia a fallos con Resilience4j

Los tres BFF implementan tolerancia a fallos mediante **Circuit Breaker**.

Cuando el backend principal se encuentra disponible:

```text
BFF
 │
 ▼
Backend Bank XYZ
 │
 ▼
Datos reales
```

Si el backend deja de responder:

```text
BFF
 │
 ▼
Circuit Breaker
 │
 ▼
Fallback
 │
 ▼
Respuesta controlada
```

La configuración utiliza parámetros como:

```yaml
slidingWindowSize: 4
minimumNumberOfCalls: 2
failureRateThreshold: 50
waitDurationInOpenState: 10s
```

Los fallos no se propagan directamente al cliente.

En su lugar, los BFF entregan respuestas controladas indicando que el servicio se encuentra temporalmente no disponible.

Durante las pruebas se comprobó el siguiente ciclo:

```text
Backend disponible
        ↓
Datos reales

Backend detenido
        ↓
Fallback

Backend recuperado
        ↓
Datos reales nuevamente
```

---

## Backend principal

El backend principal se ejecuta en:

```text
http://localhost:8080
```

Su función es acceder a Oracle y proporcionar las APIs utilizadas por los BFF.

### Endpoints

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

## BFF Web

Puerto:

```text
https://localhost:8081
```

Entrega información completa de las cuentas.

### Endpoints

| Método | Endpoint | Descripción |
|---|---|---|
| POST | `/auth/token` | Genera un JWT |
| GET | `/api/web/cuentas` | Lista completa |
| GET | `/api/web/cuentas/{id}` | Detalle completo |

Scope requerido:

```text
WEB
```

---

## BFF Mobile

Puerto:

```text
https://localhost:8082
```

Entrega respuestas reducidas para disminuir el volumen de información transferida.

### Endpoints

| Método | Endpoint | Descripción |
|---|---|---|
| POST | `/auth/token` | Genera un JWT |
| GET | `/api/mobile/cuentas` | Lista resumida |
| GET | `/api/mobile/cuentas/{id}` | Detalle resumido |

Scope requerido:

```text
MOBILE
```

---

## BFF ATM

Puerto:

```text
https://localhost:8083
```

Está orientado a consultas de saldo y operaciones de retiro.

### Endpoints

| Método | Endpoint | Descripción |
|---|---|---|
| POST | `/auth/token` | Genera un JWT |
| GET | `/api/atm/cuentas/{id}/saldo` | Consulta saldo |
| POST | `/api/atm/cuentas/{id}/retiro` | Realiza retiro |

Scope requerido:

```text
ATM
```

---

## Seguridad

Los tres BFF mantienen la seguridad implementada mediante:

- HTTPS.
- Certificados SSL/TLS.
- Keystores PKCS12 independientes.
- Credenciales independientes por canal.
- Tokens JWT.
- Firma HMAC SHA-256.
- Autorización mediante scopes.
- Sesiones HTTP stateless.
- Variables de entorno para secretos y contraseñas.

Los permisos son:

| BFF | Scope |
|---|---|
| Web | `WEB` |
| Mobile | `MOBILE` |
| ATM | `ATM` |

Spring Security los procesa como:

```text
SCOPE_WEB
SCOPE_MOBILE
SCOPE_ATM
```

Una solicitud protegida sin JWT válido responde:

```text
HTTP/1.1 401 Unauthorized
```

Una solicitud correctamente autenticada y autorizada puede acceder al recurso correspondiente.

---

## Certificados SSL/TLS

Cada BFF utiliza un archivo local:

```text
keystore.p12
```

Estos archivos contienen claves privadas y se encuentran excluidos del repositorio mediante `.gitignore`.

Pueden regenerarse mediante:

```powershell
Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass

.\script\generar-certificados.ps1
```

Antes deben configurarse:

```powershell
$env:BFF_WEB_SSL_KEYSTORE_PASSWORD='CONTRASEÑA_SSL_WEB'
$env:BFF_MOBILE_SSL_KEYSTORE_PASSWORD='CONTRASEÑA_SSL_MOBILE'
$env:BFF_ATM_SSL_KEYSTORE_PASSWORD='CONTRASEÑA_SSL_ATM'
```

Los certificados son autofirmados y se utilizan exclusivamente en el entorno local.

---

## Variables de entorno

### Backend principal

```powershell
$env:ORACLE_WALLET_DIR='RUTA_DEL_WALLET'
$env:BANKXYZ_DB_PASSWORD='CONTRASEÑA_BANKXYZ_APP'
$env:SPRING_BATCH_JOB_ENABLED='false'
```

### Web

```powershell
$env:BFF_WEB_USERNAME='web_user'
$env:BFF_WEB_PASSWORD='CONTRASEÑA_WEB'
$env:BFF_WEB_SSL_KEYSTORE_PASSWORD='CONTRASEÑA_SSL_WEB'
$env:BFF_WEB_JWT_SECRET='SECRETO_JWT_BASE64'
```

### Mobile

```powershell
$env:BFF_MOBILE_USERNAME='mobile_user'
$env:BFF_MOBILE_PASSWORD='CONTRASEÑA_MOBILE'
$env:BFF_MOBILE_SSL_KEYSTORE_PASSWORD='CONTRASEÑA_SSL_MOBILE'
$env:BFF_MOBILE_JWT_SECRET='SECRETO_JWT_BASE64'
```

### ATM

```powershell
$env:BFF_ATM_USERNAME='atm_user'
$env:BFF_ATM_PASSWORD='CONTRASEÑA_ATM'
$env:BFF_ATM_SSL_KEYSTORE_PASSWORD='CONTRASEÑA_SSL_ATM'
$env:BFF_ATM_JWT_SECRET='SECRETO_JWT_BASE64'
```

Los secretos JWT deben tener al menos 256 bits.

---

## Orden de ejecución

Los componentes deben ejecutarse en terminales independientes.

### 1. Config Server

```powershell
.\mvnw.cmd -f .\config-server\pom.xml spring-boot:run
```

Puerto:

```text
8888
```

### 2. Eureka Discovery Server

```powershell
.\mvnw.cmd -f .\discovery-server\pom.xml spring-boot:run
```

Puerto:

```text
8761
```

### 3. Backend principal

Después de configurar Oracle:

```powershell
.\mvnw.cmd -f .\pom.xml spring-boot:run
```

Puerto:

```text
8080
```

### 4. BFF Web

```powershell
.\mvnw.cmd -f .\bff-web\pom.xml spring-boot:run
```

Puerto HTTPS:

```text
8081
```

### 5. BFF Mobile

```powershell
.\mvnw.cmd -f .\bff-mobile\pom.xml spring-boot:run
```

Puerto HTTPS:

```text
8082
```

### 6. BFF ATM

```powershell
.\mvnw.cmd -f .\bff-atm\pom.xml spring-boot:run
```

Puerto HTTPS:

```text
8083
```

---

## Autenticación JWT

Cada BFF dispone de:

```text
POST /auth/token
```

Ejemplo de solicitud:

```json
{
    "username": "USUARIO",
    "password": "CONTRASEÑA"
}
```

La respuesta contiene:

```json
{
    "accessToken": "TOKEN_JWT",
    "tokenType": "Bearer",
    "expiresIn": 1800
}
```

El token se utiliza mediante:

```text
Authorization: Bearer TOKEN_JWT
```

---

## Evidencias verificadas

Durante las pruebas de Semana 6 se comprobó:

- Config Server funcionando correctamente.
- Configuración centralizada consumida por los BFF.
- Eureka Discovery Server funcionando.
- BFF Web registrado en Eureka.
- BFF Mobile registrado en Eureka.
- BFF ATM registrado en Eureka.
- Los tres microservicios simultáneamente en estado `UP`.
- Funcionamiento normal con el backend disponible.
- Circuit Breaker y Fallback en BFF Web.
- Circuit Breaker y Fallback en BFF Mobile.
- Circuit Breaker y Fallback en BFF ATM.
- Recuperación automática al volver a levantar el backend.
- Rechazo de acceso sin autenticación mediante `401 Unauthorized`.
- Acceso correcto utilizando JWT válido.
- Comunicación HTTPS en los tres BFF.

---

## Conclusión

La solución Bank XYZ fue extendida mediante Spring Cloud para incorporar características propias de una arquitectura distribuida.

Spring Cloud Config permite centralizar la configuración operacional de los BFF, mientras que Eureka mantiene el registro de los microservicios disponibles.

Resilience4j proporciona tolerancia a fallos mediante Circuit Breaker y Fallback, permitiendo responder de manera controlada cuando el backend principal no se encuentra disponible y recuperar el funcionamiento normal una vez restablecido.

Los tres BFF conservan además su configuración de seguridad mediante HTTPS, JWT y autorización específica por canal.

De esta forma, el proyecto evoluciona hacia una arquitectura más modular, resiliente, configurable y preparada para futuras ampliaciones.