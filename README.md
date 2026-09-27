# Bank XYZ - Microservicios, BFF y Arquitectura Orientada a Eventos

Proyecto desarrollado para la asignatura **Desarrollo Backend III (PBY2203)**.

Esta versión corresponde a la continuidad del proyecto **Bank XYZ**, incorporando una arquitectura distribuida basada en Backend for Frontend (BFF), configuración centralizada, descubrimiento de servicios, seguridad mediante JWT y HTTPS, tolerancia a fallos con Resilience4j y mensajería asíncrona mediante Apache Kafka.

La solución está compuesta por:

- Backend principal Bank XYZ.
- BFF Web.
- BFF Mobile.
- BFF ATM.
- Spring Cloud Config Server.
- Eureka Discovery Server.
- Oracle Autonomous Database.
- Apache Kafka.

Durante la Semana 7 se incorporó una **arquitectura orientada a eventos (Event-Driven Architecture)** para desacoplar el procesamiento de eventos bancarios y permitir su procesamiento asíncrono y concurrente.

Actualmente se encuentra implementado funcionalmente el evento asociado a retiros mediante el tópico Kafka:

```text
retiro-realizado
```

También se encuentran creados y preparados los tópicos:

```text
deposito-realizado
transferencia-realizada
```

Estos dos últimos forman parte de la arquitectura preparada para futuras extensiones del proyecto, pero actualmente no poseen un flujo funcional de productor y consumidor.

---

## Objetivo

El objetivo del proyecto es implementar una arquitectura backend modular, segura, resiliente y preparada para el procesamiento asíncrono de eventos bancarios.

La solución combina los siguientes componentes y patrones:

- **Backend for Frontend (BFF)** para proporcionar APIs adaptadas a Web, Mobile y ATM.
- **Spring Cloud Config** para centralizar configuración operativa de los BFF.
- **Eureka Service Discovery** para registrar y visualizar los servicios BFF.
- **Resilience4j** para implementar Circuit Breaker y respuestas fallback.
- **Apache Kafka** para implementar una arquitectura orientada a eventos.
- **Oracle Database** como fuente del estado actual de las cuentas.
- **JWT y HTTPS** para autenticación, autorización y protección de las comunicaciones con los BFF.

El backend principal mantiene la lógica de negocio y el acceso a Oracle.

Cuando una operación de retiro finaliza correctamente, el backend genera un evento `RetiroRealizadoEvent` y lo publica de manera asíncrona en Apache Kafka.

El tópico `retiro-realizado` dispone de tres particiones y es procesado mediante el grupo de consumidores `auditoria-bankxyz`, configurado con tres consumidores concurrentes.

---

# Arquitectura

La solución implementa una arquitectura distribuida que combina BFF, componentes Spring Cloud, persistencia en Oracle y mensajería asíncrona con Apache Kafka.

El diagrama completo de arquitectura se adjunta junto con la entrega:

```text
Arquitectura_Eventos_BankXYZ.drawio
Arquitectura_Eventos_BankXYZ.png
```

Vista conceptual simplificada:

```text
                      Config Server
                          :8888
                            │
                configuración centralizada
                            │
        ┌───────────────────┼───────────────────┐
        ▼                   ▼                   ▼
    BFF Web            BFF Mobile            BFF ATM
 HTTPS :8081          HTTPS :8082          HTTPS :8083
        │                   │                   │
        └───────────────────┼───────────────────┘
                            │
                            ▼
                   Backend Bank XYZ
                     HTTP :8080
                      /       \
                     /         \
                    ▼           ▼
             Oracle Database   Apache Kafka
                                  :9092
                                    │
                           retiro-realizado
                           P0 | P1 | P2
                                    │
                                    ▼
                           auditoria-bankxyz
                           concurrency = 3
                                    │
                                    ▼
                            Registro en log
```

Por otra parte, los BFF se registran en:

```text
Eureka Discovery Server
http://localhost:8761
```

El backend principal no utiliza Eureka para resolver sus dependencias y los BFF se comunican actualmente con él mediante:

```text
http://localhost:8080
```

---

## Patrón de arquitectura de eventos

El patrón utilizado corresponde a una **arquitectura orientada a eventos**, implementada mediante el modelo:

```text
Producer → Topic → Consumer
```

Apache Kafka actúa como broker de eventos.

En el flujo actualmente implementado:

```text
Retiro bancario
      │
      ▼
CuentaService
      │
      ├── Actualiza saldo en Oracle
      │
      ▼
RetiroRealizadoEvent
      │
      ▼
RetiroEventProducer
      │
      ▼
Kafka
      │
      ▼
retiro-realizado
      │
      ▼
RetiroEventConsumer
      │
      ▼
Auditoría mediante logs
```

La solución **no implementa Event Sourcing completo**.

Oracle Database continúa siendo la fuente del estado actual de las cuentas. Kafka se utiliza para publicar, distribuir y procesar eventos de manera asíncrona, pero el estado bancario no se reconstruye a partir del historial de eventos.

---

# Arquitectura Kafka

Apache Kafka se ejecuta localmente mediante Docker.

El broker se encuentra disponible en:

```text
localhost:9092
```

La configuración está definida en:

```text
docker-compose.yml
```

El entorno utiliza un broker Kafka local de un solo nodo para desarrollo y pruebas.

---

## Tópicos

El backend crea automáticamente los siguientes tópicos:

| Tópico | Particiones | Replication Factor | Estado |
|---|---:|---:|---|
| `retiro-realizado` | 3 | 1 | Implementado |
| `deposito-realizado` | 3 | 1 | Creado / preparado |
| `transferencia-realizada` | 3 | 1 | Creado / preparado |

La configuración se encuentra en:

```text
src/main/java/com/duoc/demo/Config/KafkaTopicsConfiguration.java
```

Cada tópico posee tres particiones:

```text
P0
P1
P2
```

El `Replication Factor` es `1` debido a que el entorno local utiliza un único broker Kafka.

---

## Evento implementado

El evento funcional actualmente implementado es:

```text
RetiroRealizadoEvent
```

Ubicación:

```text
src/main/java/com/duoc/demo/Kafka/event/RetiroRealizadoEvent.java
```

Su estructura es:

```java
public record RetiroRealizadoEvent(
        Integer cuentaId,
        BigDecimal monto,
        BigDecimal saldoDisponible,
        LocalDateTime fechaHora
) {
}
```

Ejemplo del mensaje publicado:

```json
{
  "cuentaId": 101,
  "monto": 1000,
  "saldoDisponible": 150000,
  "fechaHora": "2026-09-26T14:25:36"
}
```

---

## Producer Kafka

El productor encargado de publicar el evento es:

```text
RetiroEventProducer
```

Ubicación:

```text
src/main/java/com/duoc/demo/Kafka/producer/RetiroEventProducer.java
```

El productor utiliza:

```text
KafkaTemplate<String, RetiroRealizadoEvent>
```

La publicación se realiza sobre:

```text
retiro-realizado
```

utilizando como clave Kafka:

```text
cuentaId.toString()
```

De esta forma, los eventos relacionados con una misma cuenta utilizan de manera consistente la misma clave de particionado.

La publicación se realiza de forma asíncrona mediante:

```java
kafkaTemplate.send(...)
```

El producer registra en el log:

- ID de la cuenta.
- Partición utilizada.
- Offset asignado por Kafka.
- Posibles errores de publicación.

---

## Consumer Kafka

El consumidor implementado es:

```text
RetiroEventConsumer
```

Ubicación:

```text
src/main/java/com/duoc/demo/Kafka/consumer/RetiroEventConsumer.java
```

Su listener utiliza:

```text
topic: retiro-realizado
groupId: auditoria-bankxyz
concurrency: 3
```

La configuración:

```java
@KafkaListener(
        topics = TopicNames.RETIRO_REALIZADO,
        groupId = "auditoria-bankxyz",
        concurrency = "3"
)
```

permite ejecutar tres consumidores concurrentes para un tópico que posee tres particiones.

El consumidor registra:

```text
key
partición
offset
contenido del evento
```

Ejemplo conceptual del log:

```text
AUDITORIA KAFKA |
key=101 |
particion=1 |
offset=5 |
evento={...}
```

Los tres consumers corresponden a procesamiento concurrente del mismo listener y no a tres microservicios diferentes.

---

# Flujo de un retiro

La secuencia funcional implementada es:

1. El cliente realiza una solicitud de retiro.
2. El BFF ATM puede reenviar la operación al backend principal.
3. `CuentaController` recibe el `POST /api/cuentas/{id}/retiro`.
4. `CuentaService` valida el monto y el estado de la cuenta.
5. Se comprueba que la cuenta permita retiros.
6. Se valida que exista saldo suficiente.
7. `CuentaRepository` actualiza el saldo en Oracle.
8. El backend consulta nuevamente la cuenta actualizada.
9. Se crea un `RetiroRealizadoEvent`.
10. `RetiroEventProducer` publica el evento en Kafka.
11. Kafka almacena el evento en una partición de `retiro-realizado`.
12. `RetiroEventConsumer` procesa el evento de manera asíncrona.
13. El resultado se registra en el log de auditoría.

El evento sólo se genera después de que la operación de retiro ha sido procesada correctamente por el backend.

---

# Resilience4j

Los BFF implementan tolerancia a fallos mediante **Resilience4j Circuit Breaker**.

La configuración central utiliza:

```yaml
resilience4j:
  circuitbreaker:
    instances:
      circuitBreaker:
        slidingWindowSize: 4
        minimumNumberOfCalls: 2
        failureRateThreshold: 50
        waitDurationInOpenState: 10s
```

Valores utilizados:

| Parámetro | Valor |
|---|---:|
| Sliding Window Size | 4 |
| Minimum Number of Calls | 2 |
| Failure Rate Threshold | 50 % |
| Wait Duration in Open State | 10 segundos |

Cuando el backend principal no se encuentra disponible, el BFF evita propagar directamente el error al cliente y ejecuta una respuesta fallback.

En el BFF Web, por ejemplo, el estado de la respuesta controlada corresponde a:

```text
TEMPORALMENTE_NO_DISPONIBLE
```

Durante las pruebas se verificaron ambos escenarios:

```text
Backend disponible
        ↓
BFF obtiene datos reales
```

y:

```text
Backend no disponible
        ↓
Resilience4j
        ↓
Fallback controlado
```

---

# Spring Cloud Config Server

El Config Server se ejecuta en:

```text
http://localhost:8888
```

Se encuentra en:

```text
config-server/
```

Utiliza un repositorio de configuración `native` ubicado en:

```text
config-server/src/main/resources/config-repo/
```

Actualmente contiene:

```text
bff-web.yml
bff-mobile.yml
bff-atm.yml
```

Estas configuraciones centralizan, entre otros elementos:

- Puertos de los BFF.
- Configuración de Eureka.
- Actuator.
- Configuración de Resilience4j.
- Identificación del canal BFF.

---

# Eureka Discovery Server

Eureka se ejecuta en:

```text
http://localhost:8761
```

Se encuentra en:

```text
discovery-server/
```

Los BFF se registran como:

```text
bff-web
bff-mobile
bff-atm
```

Eureka permite verificar visualmente qué servicios se encuentran registrados y disponibles.

---

# Backend principal

El backend principal se ejecuta mediante HTTP:

```text
http://localhost:8080
```

Sus principales responsabilidades son:

- Acceso a Oracle Database.
- Gestión de cuentas.
- Consulta de información bancaria.
- Validación de retiros.
- Actualización de saldos.
- Publicación de eventos Kafka.
- Consumo del evento de retiro para auditoría.

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

Cuerpo de ejemplo para retiro:

```json
{
  "monto": 1000
}
```

---

# BFF Web

El BFF Web se ejecuta en:

```text
https://localhost:8081
```

Está orientado a clientes Web y entrega información completa de las cuentas.

## Endpoints

| Método | Endpoint | Descripción |
|---|---|---|
| POST | `/auth/token` | Genera un JWT |
| GET | `/api/web/cuentas` | Lista completa de cuentas |
| GET | `/api/web/cuentas/{id}` | Obtiene una cuenta |

Los endpoints protegidos requieren:

```text
SCOPE_WEB
```

---

# BFF Mobile

El BFF Mobile se ejecuta en:

```text
https://localhost:8082
```

Está diseñado para entregar una representación reducida de los datos.

## Endpoints

| Método | Endpoint | Descripción |
|---|---|---|
| POST | `/auth/token` | Genera un JWT |
| GET | `/api/mobile/cuentas` | Lista resumida de cuentas |
| GET | `/api/mobile/cuentas/{id}` | Obtiene una cuenta resumida |

Los endpoints protegidos requieren:

```text
SCOPE_MOBILE
```

---

# BFF ATM

El BFF ATM se ejecuta en:

```text
https://localhost:8083
```

Está orientado a consultas de saldo y operaciones de retiro.

## Endpoints

| Método | Endpoint | Descripción |
|---|---|---|
| POST | `/auth/token` | Genera un JWT |
| GET | `/api/atm/cuentas/{id}/saldo` | Consulta saldo |
| POST | `/api/atm/cuentas/{id}/retiro` | Realiza un retiro |

Los endpoints protegidos requieren:

```text
SCOPE_ATM
```

Ejemplo:

```json
{
  "monto": 1000
}
```

---

# Seguridad

Los tres BFF utilizan una configuración de seguridad independiente.

La solución utiliza:

- HTTPS.
- Certificados SSL/TLS.
- Keystores PKCS12.
- Autenticación mediante usuario y contraseña.
- Tokens JWT.
- Firma HMAC SHA-256.
- Autorización mediante scopes.
- Sesiones stateless.
- Variables de entorno para credenciales y secretos.

Scopes utilizados:

| BFF | Scope |
|---|---|
| Web | `WEB` |
| Mobile | `MOBILE` |
| ATM | `ATM` |

Spring Security los interpreta como:

```text
SCOPE_WEB
SCOPE_MOBILE
SCOPE_ATM
```

Los tokens poseen una duración de:

```text
1800 segundos
```

equivalentes a 30 minutos.

Los JWT secrets deben contener al menos:

```text
256 bits
```

---

# Certificados SSL/TLS

Cada BFF utiliza un:

```text
keystore.p12
```

Los keystores contienen claves privadas y no deben almacenarse en Git.

Se encuentran excluidos mediante `.gitignore`.

Para generarlos puede utilizarse:

```powershell
Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass

.\script\generar-certificados.ps1
```

Antes de ejecutar el script deben estar declaradas:

```powershell
$env:BFF_WEB_SSL_KEYSTORE_PASSWORD='CONTRASEÑA_SSL_WEB'
$env:BFF_MOBILE_SSL_KEYSTORE_PASSWORD='CONTRASEÑA_SSL_MOBILE'
$env:BFF_ATM_SSL_KEYSTORE_PASSWORD='CONTRASEÑA_SSL_ATM'
```

Los certificados del entorno de desarrollo son autofirmados para:

```text
localhost
127.0.0.1
```

Por esta razón, en pruebas locales con `curl` se utiliza:

```text
-k
```

---

# Variables de entorno

Los valores reales de contraseñas y secretos **no deben escribirse en el repositorio**.

## Backend principal

```powershell
$env:ORACLE_WALLET_DIR='RUTA_DEL_ORACLE_WALLET'
$env:BANKXYZ_DB_PASSWORD='CONTRASEÑA_ORACLE'
```

Para pruebas de APIs puede deshabilitarse la ejecución automática del job Batch:

```powershell
$env:SPRING_BATCH_JOB_ENABLED='false'
```

La conexión Oracle utiliza el alias:

```text
bankxyz_tp
```

y el usuario:

```text
BANKXYZ_APP
```

---

## BFF Web

```powershell
$env:BFF_WEB_USERNAME='USUARIO_WEB'
$env:BFF_WEB_PASSWORD='CONTRASEÑA_WEB'
$env:BFF_WEB_SSL_KEYSTORE_PASSWORD='CONTRASEÑA_SSL_WEB'
$env:BFF_WEB_JWT_SECRET='SECRETO_JWT_BASE64'
```

---

## BFF Mobile

```powershell
$env:BFF_MOBILE_USERNAME='USUARIO_MOBILE'
$env:BFF_MOBILE_PASSWORD='CONTRASEÑA_MOBILE'
$env:BFF_MOBILE_SSL_KEYSTORE_PASSWORD='CONTRASEÑA_SSL_MOBILE'
$env:BFF_MOBILE_JWT_SECRET='SECRETO_JWT_BASE64'
```

---

## BFF ATM

```powershell
$env:BFF_ATM_USERNAME='USUARIO_ATM'
$env:BFF_ATM_PASSWORD='CONTRASEÑA_ATM'
$env:BFF_ATM_SSL_KEYSTORE_PASSWORD='CONTRASEÑA_SSL_ATM'
$env:BFF_ATM_JWT_SECRET='SECRETO_JWT_BASE64'
```

---

## Generación de un JWT secret

Ejemplo mediante PowerShell:

```powershell
$jwtBytes = New-Object byte[] 32
$rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$rng.GetBytes($jwtBytes)
$rng.Dispose()

[Convert]::ToBase64String($jwtBytes)
```

El valor obtenido debe almacenarse de forma privada y posteriormente declararse como variable de entorno.

---

# Estructura del proyecto

```text
demo/
│
├── src/
│   └── main/
│       ├── java/com/duoc/demo/
│       │   ├── Config/
│       │   │   └── KafkaTopicsConfiguration.java
│       │   ├── Controller/
│       │   ├── Dto/
│       │   ├── Kafka/
│       │   │   ├── consumer/
│       │   │   │   └── RetiroEventConsumer.java
│       │   │   ├── event/
│       │   │   │   └── RetiroRealizadoEvent.java
│       │   │   ├── producer/
│       │   │   │   └── RetiroEventProducer.java
│       │   │   └── TopicNames.java
│       │   ├── Model/
│       │   ├── Processor/
│       │   ├── Reader/
│       │   ├── Repository/
│       │   ├── Service/
│       │   └── Writer/
│       │
│       └── resources/
│           └── application.properties
│
├── config-server/
│   ├── pom.xml
│   └── src/main/resources/
│       ├── application.yml
│       └── config-repo/
│           ├── bff-web.yml
│           ├── bff-mobile.yml
│           └── bff-atm.yml
│
├── discovery-server/
│   ├── pom.xml
│   └── src/main/resources/application.yml
│
├── bff-web/
│   ├── pom.xml
│   └── src/main/
│
├── bff-mobile/
│   ├── pom.xml
│   └── src/main/
│
├── bff-atm/
│   ├── pom.xml
│   └── src/main/
│
├── script/
│   └── generar-certificados.ps1
│
├── docker-compose.yml
├── README.md
├── PROPUESTA_TECNICA.md
├── pom.xml
├── mvnw
└── mvnw.cmd
```

---

# Tecnologías utilizadas

- Java 18.
- Spring Boot 4.x.
- Spring MVC.
- Spring JDBC.
- Spring Batch.
- Spring Security.
- Spring Security OAuth2 Resource Server.
- Spring Cloud Config.
- Netflix Eureka.
- Resilience4j.
- Apache Kafka.
- Spring for Apache Kafka.
- Oracle Database.
- Oracle Wallet.
- JWT.
- HTTPS / TLS.
- PKCS12.
- Docker / Docker Compose.
- Maven.
- Git.
- GitHub.

---

# Ejecución del proyecto

Para probar la arquitectura completa deben iniciarse los componentes en un orden controlado.

Cada aplicación debe ejecutarse en una terminal independiente.

Los siguientes comandos asumen que la terminal se encuentra ubicada en la raíz:

```text
demo/
```

---

## 1. Iniciar Apache Kafka

Con Docker Desktop activo:

```powershell
docker compose up -d
```

Comprobar:

```powershell
docker ps
```

Debe existir el contenedor:

```text
kafka
```

Si el contenedor ya existe pero se encuentra detenido:

```powershell
docker start kafka
```

Kafka debe quedar disponible en:

```text
localhost:9092
```

---

## 2. Iniciar Config Server

```powershell
.\mvnw.cmd -f .\config-server\pom.xml spring-boot:run
```

Disponible en:

```text
http://localhost:8888
```

---

## 3. Iniciar Eureka

En una nueva terminal:

```powershell
.\mvnw.cmd -f .\discovery-server\pom.xml spring-boot:run
```

Disponible en:

```text
http://localhost:8761
```

---

## 4. Iniciar backend principal

Declarar primero las variables requeridas:

```powershell
$env:ORACLE_WALLET_DIR='RUTA_DEL_WALLET'
$env:BANKXYZ_DB_PASSWORD='CONTRASEÑA_ORACLE'
$env:SPRING_BATCH_JOB_ENABLED='false'
```

Ejecutar:

```powershell
.\mvnw.cmd spring-boot:run
```

El backend queda disponible en:

```text
http://localhost:8080
```

Prueba:

```powershell
curl.exe http://localhost:8080/api/cuentas/101
```

---

## 5. Iniciar BFF Web

Declarar sus variables:

```powershell
$env:BFF_WEB_USERNAME='USUARIO_WEB'
$env:BFF_WEB_PASSWORD='CONTRASEÑA_WEB'
$env:BFF_WEB_SSL_KEYSTORE_PASSWORD='CONTRASEÑA_SSL_WEB'
$env:BFF_WEB_JWT_SECRET='SECRETO_JWT_BASE64'
```

Ejecutar:

```powershell
.\mvnw.cmd -f .\bff-web\pom.xml spring-boot:run
```

Disponible en:

```text
https://localhost:8081
```

---

## 6. Iniciar BFF Mobile

```powershell
$env:BFF_MOBILE_USERNAME='USUARIO_MOBILE'
$env:BFF_MOBILE_PASSWORD='CONTRASEÑA_MOBILE'
$env:BFF_MOBILE_SSL_KEYSTORE_PASSWORD='CONTRASEÑA_SSL_MOBILE'
$env:BFF_MOBILE_JWT_SECRET='SECRETO_JWT_BASE64'
```

Ejecutar:

```powershell
.\mvnw.cmd -f .\bff-mobile\pom.xml spring-boot:run
```

Disponible en:

```text
https://localhost:8082
```

---

## 7. Iniciar BFF ATM

```powershell
$env:BFF_ATM_USERNAME='USUARIO_ATM'
$env:BFF_ATM_PASSWORD='CONTRASEÑA_ATM'
$env:BFF_ATM_SSL_KEYSTORE_PASSWORD='CONTRASEÑA_SSL_ATM'
$env:BFF_ATM_JWT_SECRET='SECRETO_JWT_BASE64'
```

Ejecutar:

```powershell
.\mvnw.cmd -f .\bff-atm\pom.xml spring-boot:run
```

Disponible en:

```text
https://localhost:8083
```

---

# Comprobación de Kafka

## Visualizar tópicos

```powershell
docker exec kafka /opt/kafka/bin/kafka-topics.sh `
    --bootstrap-server localhost:9092 `
    --describe
```

Deben visualizarse:

```text
retiro-realizado
deposito-realizado
transferencia-realizada
```

con tres particiones cada uno.

---

## Escuchar eventos de retiro manualmente

```powershell
docker exec -it kafka /opt/kafka/bin/kafka-console-consumer.sh `
    --bootstrap-server localhost:9092 `
    --topic retiro-realizado `
    --property print.key=true `
    --property key.separator=" | "
```

---

## Consultar el grupo de consumidores

```powershell
docker exec kafka /opt/kafka/bin/kafka-consumer-groups.sh `
    --bootstrap-server localhost:9092 `
    --describe `
    --group auditoria-bankxyz
```

Esto permite observar el estado del grupo y las asignaciones sobre las particiones.

---

# Prueba de publicación de evento

Con el backend principal y Kafka disponibles:

```powershell
$body = @{
    monto = 1000
} | ConvertTo-Json

Invoke-RestMethod `
    -Method Post `
    -Uri "http://localhost:8080/api/cuentas/101/retiro" `
    -ContentType "application/json" `
    -Body $body
```

Si la operación se procesa correctamente:

1. Oracle actualiza el saldo.
2. El backend responde con el resultado del retiro.
3. `RetiroEventProducer` publica un evento.
4. Kafka asigna una partición y un offset.
5. `RetiroEventConsumer` procesa el mensaje.
6. La auditoría aparece en el log.

---

# Autenticación JWT

Cada BFF expone:

```text
POST /auth/token
```

Ejemplo con BFF Web.

Primero deben encontrarse declaradas:

```powershell
$env:BFF_WEB_USERNAME='USUARIO_WEB'
$env:BFF_WEB_PASSWORD='CONTRASEÑA_WEB'
```

Crear la solicitud:

```powershell
$authBody = @{
    username = $env:BFF_WEB_USERNAME
    password = $env:BFF_WEB_PASSWORD
} | ConvertTo-Json -Compress

Set-Content `
    -Path .\auth-web.json `
    -Value $authBody `
    -Encoding utf8
```

Solicitar token:

```powershell
$authResponse = curl.exe -sk `
    -X POST `
    -H "Content-Type: application/json" `
    --data-binary "@auth-web.json" `
    https://localhost:8081/auth/token |
    ConvertFrom-Json
```

Obtener el JWT:

```powershell
$token = $authResponse.accessToken
```

Consultar:

```powershell
curl.exe -k `
    -H "Authorization: Bearer $token" `
    https://localhost:8081/api/web/cuentas/101
```

El archivo temporal:

```text
auth-web.json
```

no debe incluirse en el repositorio ni en la entrega final si contiene credenciales.

---

# Prueba de Resilience4j

## Escenario normal

Con el backend `:8080` disponible:

```powershell
curl.exe -k -i `
    -H "Authorization: Bearer $token" `
    https://localhost:8081/api/web/cuentas/101
```

El BFF devuelve los datos reales de la cuenta.

---

## Escenario de fallo

Detener únicamente el backend principal:

```text
Ctrl + C
```

Mantener activo el BFF Web y repetir:

```powershell
curl.exe -k -i `
    -H "Authorization: Bearer $token" `
    https://localhost:8081/api/web/cuentas/101
```

El BFF devuelve una respuesta controlada mediante fallback, cuyo estado incluye:

```text
TEMPORALMENTE_NO_DISPONIBLE
```

Esto permite demostrar que una caída de la dependencia no provoca una respuesta sin controlar en el cliente.

---

# Evidencias de ejecución - Semana 7

Durante las pruebas se generaron las siguientes evidencias:

```text
1. Kafka - Tópicos bancarios con tres particiones.png

2. Kafka - Evento de retiro publicado y recibido.png

3. Kafka - Producer y Consumer procesando retiro.png

4. Kafka - Escalabilidad con tres consumidores y particiones.png

5. Resilience4j - Funcionamiento normal con backend disponible.png

6. Resilience4j - Fallback ante caída del backend.png
```

Las evidencias permiten demostrar:

- Creación de los tópicos Kafka.
- Tres particiones por tópico.
- Publicación de un evento real de retiro.
- Recepción y procesamiento asíncrono.
- Funcionamiento del producer.
- Funcionamiento del consumer.
- Uso del grupo `auditoria-bankxyz`.
- Procesamiento concurrente mediante `concurrency = 3`.
- Funcionamiento normal del BFF.
- Respuesta fallback ante indisponibilidad del backend.

---

# Evidencias de semanas anteriores

También se realizaron pruebas relacionadas con la arquitectura BFF y seguridad:

- Acceso rechazado sin JWT.
- Acceso autorizado mediante JWT válido.
- Rechazo de tokens inválidos.
- Ejecución de BFF mediante HTTPS.
- Consulta Web con información completa.
- Consulta Mobile con información resumida.
- Consulta de saldo mediante ATM.
- Retiro mediante ATM.
- Registro de BFF en Eureka.
- Configuración centralizada mediante Config Server.

---

# Personalización por canal

Cada BFF consume el mismo backend principal pero adapta la respuesta según el canal.

```text
Backend principal
        │
        ├── BFF Web
        │     └── Información completa
        │
        ├── BFF Mobile
        │     └── Información reducida
        │
        └── BFF ATM
              └── Saldo y retiro
```

Esto evita enviar información innecesaria a cada tipo de cliente y mantiene separadas las responsabilidades de cada canal.

---

# Consideraciones de seguridad

No deben almacenarse en Git ni incluirse en la entrega:

```text
Contraseñas reales
JWT secrets
Oracle Wallet
keystore.p12
.env
auth-web.json con credenciales
```

El archivo `.gitignore` se encuentra configurado para excluir:

- `target/`
- archivos `.env`
- Oracle Wallet.
- certificados y keystores locales.
- logs.
- archivos generados.

---

# Consideraciones del entorno Kafka

La configuración de Kafka utilizada corresponde a un entorno local de desarrollo.

El broker posee:

```text
1 nodo
Replication Factor = 1
```

Por lo tanto, la configuración actual permite demostrar particionado y consumo concurrente, pero no busca representar alta disponibilidad real entre múltiples brokers.

Para un entorno productivo deberían considerarse múltiples brokers y un factor de replicación superior.

---

# Alcance actual

Actualmente se encuentra implementado funcionalmente:

```text
retiro-realizado
```

Los tópicos:

```text
deposito-realizado
transferencia-realizada
```

se encuentran creados como parte de la arquitectura, pero sus flujos de producer y consumer quedan preparados para extensiones posteriores del proyecto.

La auditoría de eventos actualmente se realiza mediante el log de la aplicación y no mediante una base de datos de auditoría independiente.

---

# Conclusión

La solución Bank XYZ evoluciona desde una arquitectura basada exclusivamente en Backend for Frontend hacia una arquitectura distribuida que incorpora seguridad, descubrimiento de servicios, configuración centralizada, tolerancia a fallos y procesamiento asíncrono de eventos.

Los tres BFF permiten mantener interfaces específicas para Web, Mobile y ATM, protegidas mediante HTTPS y JWT.

Spring Cloud Config centraliza configuración operativa de los BFF, mientras que Eureka permite registrar y visualizar los servicios disponibles.

Resilience4j proporciona tolerancia a fallos mediante Circuit Breaker y fallback, permitiendo que los BFF respondan de forma controlada ante la indisponibilidad del backend principal.

Apache Kafka incorpora un modelo orientado a eventos basado en Producer, Topic y Consumer. El flujo `retiro-realizado` publica eventos después de procesar correctamente una operación de retiro y los distribuye mediante tres particiones a un grupo configurado con tres consumidores concurrentes.

Oracle Database continúa siendo la fuente del estado actual de las cuentas, mientras que Kafka permite desacoplar el procesamiento posterior de los eventos.

De esta forma, el proyecto integra los principales conceptos trabajados durante las etapas del curso y establece una base modular y extensible para continuar incorporando nuevas operaciones y eventos durante las siguientes semanas.