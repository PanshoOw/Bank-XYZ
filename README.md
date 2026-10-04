# Bank XYZ - Microservicios seguros y resilientes en la nube

Proyecto desarrollado para la asignatura **Desarrollo Backend III (PBY2203)** de Duoc UC.

La solución implementa una arquitectura distribuida para Bank XYZ basada en Spring Boot y Spring Cloud, incorporando Backend for Frontend (BFF), configuración centralizada, descubrimiento de servicios, OAuth 2.0, tolerancia a fallos con Resilience4j, mensajería asíncrona con Apache Kafka y contenerización mediante Docker y Docker Compose.

---

## 1. Objetivo

El objetivo del proyecto es preparar el ecosistema de microservicios de Bank XYZ para operar como una solución distribuida, segura y resiliente.

La implementación incorpora:

- OAuth 2.0 mediante un Authorization Server independiente.
- JWT para proteger los BFF Web, Mobile y ATM.
- Scopes específicos para cada canal.
- Spring Cloud Config para configuración centralizada.
- Eureka Server para Service Discovery.
- Resilience4j con Circuit Breaker, Retry, Bulkhead y fallback.
- Apache Kafka para procesamiento asíncrono de eventos.
- Docker para contenerizar los componentes.
- Docker Compose para orquestar el ecosistema completo.
- HTTPS en los tres BFF.
- Oracle Autonomous Database como capa de persistencia.

---

## 2. Arquitectura

| Componente | Puerto | Función |
|---|---:|---|
| Backend Bank XYZ | 8080 | API principal, Oracle y Kafka |
| BFF Web | 8081 HTTPS | Backend especializado para Web |
| BFF Mobile | 8082 HTTPS | Backend especializado para Mobile |
| BFF ATM | 8083 HTTPS | Backend especializado para ATM |
| Eureka Discovery Server | 8761 | Registro y descubrimiento de servicios |
| Config Server | 8888 | Configuración centralizada |
| Authorization Server | 9000 | OAuth 2.0 y emisión de JWT |
| Apache Kafka | 9092 / 29092 | Mensajería asíncrona |

Los contenedores se comunican mediante la red:

```text
bankxyz-network
```

Kafka utiliza dos listeners:

```text
Host:   localhost:9092
Docker: kafka:29092
```

---

## 3. Estructura del proyecto

```text
demo/
├── auth-server/
├── bff-web/
├── bff-mobile/
├── bff-atm/
├── config-server/
├── discovery-server/
├── src/
├── Dockerfile
├── docker-compose.yml
├── .env.example
├── .gitignore
├── pom.xml
└── README.md
```

### Backend principal

Contiene la lógica bancaria, persistencia mediante Oracle Autonomous Database y la integración con Apache Kafka.

### BFF Web

Backend optimizado para clientes Web. Incorpora mecanismos de resiliencia para las operaciones de lectura.

### BFF Mobile

Backend especializado para clientes móviles.

### BFF ATM

Backend destinado a operaciones de cajero automático, incluyendo consultas de saldo y retiros.

### Authorization Server

Servidor independiente encargado de emitir tokens OAuth 2.0 mediante JWT.

### Config Server

Centraliza la configuración de los BFF.

### Discovery Server

Implementa Eureka Server para registro y descubrimiento de servicios.

---

## 4. Seguridad OAuth 2.0

Se implementó Spring Authorization Server utilizando el flujo:

```text
client_credentials
```

Se definieron tres clientes:

| Cliente | Scope |
|---|---|
| bankxyz-web | WEB |
| bankxyz-mobile | MOBILE |
| bankxyz-atm | ATM |

Cada BFF funciona como OAuth2 Resource Server.

Las rutas están protegidas según el scope correspondiente:

```text
/api/web/**     -> SCOPE_WEB
/api/mobile/**  -> SCOPE_MOBILE
/api/atm/**     -> SCOPE_ATM
```

Endpoint de obtención de token:

```text
POST http://localhost:9000/oauth2/token
```

Ejemplo de parámetros:

```text
grant_type=client_credentials
scope=WEB
```

Un token perteneciente a un canal diferente es rechazado mediante HTTP `403 Forbidden`.

Los secretos OAuth2 no se almacenan directamente en el repositorio.

---

## 5. Resiliencia con Resilience4j

El BFF Web implementa mecanismos de tolerancia a fallos para operaciones de lectura hacia el Backend.

Se utilizan:

- Circuit Breaker
- Retry
- Bulkhead
- Fallback

El Circuit Breaker detecta fallos repetidos del Backend y cambia a estado `OPEN` para evitar llamadas innecesarias mientras el servicio se encuentra temporalmente no disponible.

El fallback entrega una respuesta controlada utilizando:

```text
estado = TEMPORALMENTE_NO_DISPONIBLE
```

Las operaciones que generan efectos secundarios, como retiros ATM, no utilizan reintentos automáticos para evitar duplicar una transacción.

---

## 6. Mensajería asíncrona con Kafka

Apache Kafka se utiliza para procesar eventos generados por retiros bancarios.

Tópico:

```text
retiro-realizado
```

Consumer group:

```text
auditoria-bankxyz
```

El tópico dispone de tres particiones:

```text
retiro-realizado-0
retiro-realizado-1
retiro-realizado-2
```

El Backend ejecuta tres consumidores dentro del mismo grupo, distribuyendo el procesamiento de las tres particiones.

Flujo:

```text
BFF ATM
   |
   v
Backend
   |
   v
Kafka Producer
   |
   v
retiro-realizado
   |
   v
Consumer auditoria-bankxyz
```

Ejemplo de evento:

```json
{
  "cuentaId": 101,
  "monto": 1,
  "saldoDisponible": 6579,
  "fechaHora": "..."
}
```

La publicación y el consumo se realizan de manera asíncrona.

---

## 7. Docker

Cada componente desarrollado posee su correspondiente `Dockerfile`.

Los servicios son construidos como aplicaciones Spring Boot y ejecutados dentro de contenedores independientes.

También se utilizan archivos `.dockerignore` para evitar copiar al contexto de construcción archivos innecesarios o sensibles.

---

## 8. Docker Compose

El archivo `docker-compose.yml` permite orquestar desde una sola configuración los siguientes componentes:

```text
discovery-server
config-server
auth-server
backend
bff-web
bff-mobile
bff-atm
kafka
```

Todos los servicios se encuentran conectados mediante:

```text
bankxyz-network
```

Para levantar el ecosistema:

```powershell
docker compose up -d --build
```

Para comprobar el estado:

```powershell
docker compose ps
```

Para detenerlo:

```powershell
docker compose down
```

---

## 9. Variables de entorno

El archivo `.env` real no se almacena en Git.

Se incluye como plantilla:

```text
.env.example
```

Variables requeridas:

```text
BANKXYZ_DB_PASSWORD
ORACLE_WALLET_HOST_PATH

OAUTH_WEB_CLIENT_SECRET
OAUTH_MOBILE_CLIENT_SECRET
OAUTH_ATM_CLIENT_SECRET

BFF_WEB_SSL_KEYSTORE_PASSWORD
BFF_MOBILE_SSL_KEYSTORE_PASSWORD
BFF_ATM_SSL_KEYSTORE_PASSWORD
```

Para crear el archivo local:

```powershell
Copy-Item .env.example .env
```

Luego deben reemplazarse los valores `REEMPLAZAR` por credenciales válidas.

El archivo `.env` nunca debe subirse al repositorio.

---

## 10. Oracle Autonomous Database

El Backend utiliza Oracle Autonomous Database.

El Oracle Wallet permanece fuera del repositorio.

Su ubicación se configura mediante:

```text
ORACLE_WALLET_HOST_PATH
```

Docker Compose monta dicha ruta dentro del contenedor del Backend.

---

## 11. HTTPS y keystores

Los tres BFF funcionan mediante HTTPS utilizando keystores PKCS12 locales:

```text
bff-web/keystore.p12
bff-mobile/keystore.p12
bff-atm/keystore.p12
```

Los keystores no se almacenan en Git.

Pueden generarse mediante `keytool`.

Ejemplo:

```powershell
keytool -genkeypair `
  -alias bankxyz `
  -keyalg RSA `
  -keysize 2048 `
  -storetype PKCS12 `
  -keystore keystore.p12 `
  -validity 3650
```

La contraseña debe coincidir con la variable de entorno correspondiente.

Al utilizar certificados autofirmados en desarrollo, `curl` puede requerir:

```text
-k
```

---

## 12. Requisitos de ejecución

- Docker Desktop
- Docker Compose
- Oracle Wallet válido
- Acceso a Oracle Autonomous Database
- Keystores PKCS12 para Web, Mobile y ATM
- Archivo `.env` configurado

Para iniciar:

```powershell
docker compose up -d --build
```

Comprobar:

```powershell
docker compose ps
```

Los ocho componentes principales deben aparecer en estado `Up`.

---

## 13. Ejemplo OAuth2

Solicitar token Web:

```powershell
curl.exe -u "bankxyz-web:CLIENT_SECRET" `
  -X POST `
  -H "Content-Type: application/x-www-form-urlencoded" `
  -d "grant_type=client_credentials&scope=WEB" `
  http://localhost:9000/oauth2/token
```

Utilizar token:

```powershell
curl.exe -sk `
  -H "Authorization: Bearer TOKEN" `
  https://localhost:8081/api/web/cuentas/101
```

---

## 14. Ejemplo de retiro ATM

Endpoint:

```text
POST https://localhost:8083/api/atm/cuentas/101/retiro
```

Headers:

```text
Authorization: Bearer <TOKEN_ATM>
Content-Type: application/json
```

Body:

```json
{
  "monto": 1
}
```

Ejemplo de respuesta:

```json
{
  "cuentaId": 101,
  "montoRetirado": 1,
  "saldoDisponible": 6579,
  "estadoOperacion": "CONFIRMADA",
  "mensaje": "Retiro realizado correctamente"
}
```

Después del retiro, el Backend publica un evento `retiro-realizado` y uno de los consumidores del grupo `auditoria-bankxyz` lo procesa.

---

## 15. Compilación Maven

El proyecto principal puede compilarse mediante:

```powershell
.\mvnw.cmd clean package -DskipTests
```

Los módulos adicionales:

```powershell
.\mvnw.cmd -f .\discovery-server\pom.xml clean package -DskipTests
.\mvnw.cmd -f .\config-server\pom.xml clean package -DskipTests
.\mvnw.cmd -f .\auth-server\pom.xml clean package -DskipTests
.\mvnw.cmd -f .\bff-web\pom.xml clean package -DskipTests
.\mvnw.cmd -f .\bff-mobile\pom.xml clean package -DskipTests
.\mvnw.cmd -f .\bff-atm\pom.xml clean package -DskipTests
```

---

## 16. Evidencias funcionales

La entrega incluye evidencias de:

- Emisión de tokens OAuth2.
- Acceso autorizado mediante scope WEB.
- Rechazo HTTP 403 al utilizar un token en un canal incorrecto.
- Acceso autorizado mediante scope MOBILE.
- Acceso autorizado mediante scope ATM.
- Ejecución de Discovery Server en Docker.
- Ejecución de Config Server en Docker.
- Ejecución de Authorization Server en Docker.
- Ejecución de BFF Web en Docker.
- Ejecución de BFF Mobile en Docker.
- Ejecución de BFF ATM en Docker.
- Backend conectado a Oracle y Kafka.
- Ecosistema interconectado mediante contenedores.
- Orquestación completa con Docker Compose.
- Circuit Breaker, Retry, Bulkhead y fallback.
- Circuit Breaker en estado OPEN ante indisponibilidad del Backend.
- Tres consumidores Kafka distribuidos entre tres particiones.
- Publicación y consumo real de un evento `retiro-realizado`.

---

## 17. Tecnologías utilizadas

- Java
- Spring Boot
- Spring Cloud
- Spring Security
- Spring Authorization Server
- OAuth 2.0
- JWT
- Resilience4j
- Apache Kafka
- Eureka
- Spring Cloud Config
- Oracle Autonomous Database
- Docker
- Docker Compose
- Maven

---

## 18. Seguridad

El repositorio no debe contener:

```text
.env
Oracle Wallet
contraseñas
client secrets
tokens OAuth2
keystore.p12
```

Las credenciales se gestionan mediante variables de entorno y recursos externos al repositorio.

---

## 19. Estado final

La solución fue validada mediante:

- Compilación exitosa de los siete proyectos Maven.
- Ejecución completa mediante Docker Compose.
- OAuth2 funcional.
- JWT y scopes funcionales.
- HTTPS funcional en los BFF.
- Comunicación con Oracle.
- Circuit Breaker funcional.
- Retry y Bulkhead configurados.
- Fallback funcional ante caída del Backend.
- Kafka operativo con tres particiones y tres consumidores.
- Publicación y consumo asíncrono de eventos.