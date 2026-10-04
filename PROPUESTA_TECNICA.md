# PROPUESTA TÃ‰CNICA
## Bank XYZ - Arquitectura de microservicios segura, resiliente y orientada a eventos

**Asignatura:** Desarrollo Backend III - PBY2203
**Proyecto:** Bank XYZ
**Actividad:** Experiencia 3 - Semana 8
**Tema:** Desarrollo de microservicios y resiliencia en la nube con Spring Cloud

---

## 1. IntroducciÃ³n

La presente propuesta tÃ©cnica describe la evoluciÃ³n de la soluciÃ³n Bank XYZ hacia una arquitectura distribuida preparada para operar en un entorno cloud, incorporando mecanismos de seguridad, resiliencia, mensajerÃ­a asÃ­ncrona y contenerizaciÃ³n.

La soluciÃ³n se construye sobre una arquitectura de microservicios desarrollada con Spring Boot y Spring Cloud, complementada mediante:

- Spring Authorization Server.
- OAuth 2.0.
- JSON Web Token (JWT).
- Spring Security.
- Spring Cloud Config.
- Eureka Service Discovery.
- Resilience4j.
- Apache Kafka.
- Docker.
- Docker Compose.
- Oracle Autonomous Database.

La propuesta busca reducir el acoplamiento entre componentes, mejorar la tolerancia a fallos, centralizar aspectos de configuraciÃ³n y seguridad, y permitir el despliegue coordinado de los distintos servicios de Bank XYZ.

---

## 2. Objetivo de la propuesta

El objetivo principal es disponer de una arquitectura backend distribuida que pueda mantener caracterÃ­sticas de seguridad, disponibilidad y escalabilidad ante escenarios propios de un sistema bancario.

Para ello se propone:

1. Centralizar la autenticaciÃ³n mediante un Authorization Server independiente.
2. Proteger los servicios BFF mediante OAuth 2.0 y JWT.
3. Aplicar autorizaciÃ³n diferenciada por tipo de cliente mediante scopes.
4. Mantener una configuraciÃ³n centralizada mediante Spring Cloud Config.
5. Registrar y descubrir servicios mediante Eureka.
6. Incorporar tolerancia a fallos mediante Resilience4j.
7. Procesar eventos bancarios de manera asÃ­ncrona mediante Kafka.
8. Ejecutar los componentes dentro de contenedores Docker.
9. Orquestar la soluciÃ³n completa mediante Docker Compose.
10. Mantener las credenciales y recursos sensibles fuera del cÃ³digo fuente.

---

## 3. Arquitectura propuesta

La soluciÃ³n estÃ¡ compuesta por los siguientes componentes:

| Componente | Puerto | Responsabilidad |
|---|---:|---|
| Backend Bank XYZ | 8080 | LÃ³gica bancaria, persistencia y publicaciÃ³n de eventos |
| BFF Web | 8081 | AtenciÃ³n de solicitudes del canal Web |
| BFF Mobile | 8082 | AtenciÃ³n de solicitudes del canal Mobile |
| BFF ATM | 8083 | Operaciones propias de cajeros automÃ¡ticos |
| Discovery Server | 8761 | Registro y descubrimiento de servicios |
| Config Server | 8888 | ConfiguraciÃ³n centralizada |
| Authorization Server | 9000 | AutenticaciÃ³n OAuth 2.0 y emisiÃ³n de JWT |
| Apache Kafka | 9092 / 29092 | MensajerÃ­a asÃ­ncrona |

La arquitectura general puede representarse de la siguiente forma:

```text
                       +----------------------+
                       | Authorization Server |
                       |      OAuth 2.0       |
                       |      Puerto 9000     |
                       +----------+-----------+
                                  |
                           JWT + Scopes
                                  |
               +------------------+------------------+
               |                  |                  |
               v                  v                  v
        +-------------+    +-------------+    +-------------+
        |   BFF Web   |    | BFF Mobile  |    |   BFF ATM   |
        | HTTPS 8081  |    | HTTPS 8082  |    | HTTPS 8083  |
        +------+------+    +------+------+    +------+------+
               |                  |                  |
               +------------------+------------------+
                                  |
                                  v
                        +------------------+
                        | Backend Bank XYZ |
                        |    Puerto 8080   |
                        +--------+---------+
                                 |
                    +------------+-------------+
                    |                          |
                    v                          v
        +----------------------+      +------------------+
        | Oracle Autonomous DB |      |   Apache Kafka   |
        +----------------------+      +---------+--------+
                                               |
                                               v
                                     +-------------------+
                                     | Consumer AuditorÃ­a|
                                     +-------------------+
```

De manera complementaria:

```text
Config Server  <---- configuraciÃ³n centralizada
Discovery      <---- registro de servicios
Docker Compose <---- orquestaciÃ³n completa
```

---

## 4. PatrÃ³n Backend for Frontend

Se mantiene el patrÃ³n Backend for Frontend debido a que Bank XYZ dispone de distintos tipos de clientes con necesidades diferentes.

### BFF Web

EstÃ¡ orientado a clientes Web y puede entregar representaciones mÃ¡s completas de los datos.

### BFF Mobile

EstÃ¡ orientado a dispositivos mÃ³viles, permitiendo mantener una interfaz especializada para ese tipo de consumo.

### BFF ATM

EstÃ¡ orientado a operaciones de cajeros automÃ¡ticos, particularmente operaciones sensibles como:

- Consulta de cuentas.
- Consulta de saldo.
- Retiros.

La separaciÃ³n de los BFF permite aplicar polÃ­ticas especÃ­ficas segÃºn el canal sin trasladar esa responsabilidad directamente al Backend principal.

---

## 5. Seguridad con OAuth 2.0

La arquitectura incorpora un Authorization Server independiente implementado mediante Spring Authorization Server.

El flujo seleccionado es:

```text
client_credentials
```

Este flujo resulta apropiado para la comunicaciÃ³n utilizada en el proyecto, donde cada cliente lÃ³gico obtiene un token utilizando sus propias credenciales.

Se definen tres clientes:

```text
bankxyz-web
bankxyz-mobile
bankxyz-atm
```

Cada uno dispone de un scope independiente:

```text
WEB
MOBILE
ATM
```

---

## 6. AutorizaciÃ³n mediante scopes

Cada BFF funciona como OAuth2 Resource Server.

Las rutas se protegen de acuerdo con el canal:

```text
/api/web/**     -> SCOPE_WEB
/api/mobile/**  -> SCOPE_MOBILE
/api/atm/**     -> SCOPE_ATM
```

Esto permite evitar que un token emitido para un determinado cliente sea utilizado sobre servicios pertenecientes a otro canal.

Por ejemplo:

```text
Token WEB -> BFF Web       -> Permitido
Token WEB -> BFF Mobile    -> HTTP 403
Token WEB -> BFF ATM       -> HTTP 403
```

Con esta decisiÃ³n se separan claramente autenticaciÃ³n y autorizaciÃ³n.

El Authorization Server autentica al cliente y emite el token, mientras cada Resource Server verifica el JWT y autoriza el acceso segÃºn el scope correspondiente.

---

## 7. EliminaciÃ³n de autenticaciÃ³n local duplicada

Anteriormente cada BFF gestionaba de manera independiente componentes relacionados con autenticaciÃ³n y generaciÃ³n de tokens.

La propuesta reemplaza dicho enfoque por un servidor de autorizaciÃ³n centralizado.

Como consecuencia se eliminan de los BFF componentes como:

```text
AuthController
AuthRequest
TokenResponse
JwtTokenService
```

Esta decisiÃ³n reduce duplicaciÃ³n de cÃ³digo y centraliza la responsabilidad de emisiÃ³n de credenciales.

Los BFF mantienen Ãºnicamente la responsabilidad de validar los JWT y aplicar autorizaciÃ³n.

---

## 8. ComunicaciÃ³n segura mediante HTTPS

Los BFF Web, Mobile y ATM exponen sus endpoints mediante HTTPS.

Para el ambiente de desarrollo se utilizan certificados autofirmados almacenados en keystores PKCS12.

```text
bff-web/keystore.p12
bff-mobile/keystore.p12
bff-atm/keystore.p12
```

Estos archivos no forman parte del repositorio debido a que contienen material criptogrÃ¡fico.

Las contraseÃ±as de los keystores se obtienen mediante variables de entorno.

---

## 9. ConfiguraciÃ³n centralizada

Spring Cloud Config continÃºa siendo utilizado para centralizar configuraciones de los BFF.

El Config Server se encuentra disponible mediante:

```text
http://config-server:8888
```

dentro del entorno Docker.

Entre las configuraciones centralizadas se encuentran:

- URL del Backend.
- URL de Eureka.
- ConfiguraciÃ³n OAuth2 Resource Server.
- Issuer URI del Authorization Server.
- ParÃ¡metros de Resilience4j.
- Puertos y configuraciÃ³n especÃ­fica por canal.

La centralizaciÃ³n facilita realizar cambios sin distribuir configuraciones manualmente entre todos los componentes.

---

## 10. Service Discovery

Eureka Server se mantiene como mecanismo de registro y descubrimiento de servicios.

Puerto:

```text
8761
```

Los microservicios pueden registrarse dentro del ecosistema y exponer informaciÃ³n sobre su disponibilidad.

La utilizaciÃ³n de Service Discovery permite mantener una arquitectura preparada para escenarios donde las instancias de servicios puedan cambiar dinÃ¡micamente.

---

## 11. Resiliencia con Resilience4j

Se implementan mecanismos de tolerancia a fallos mediante Resilience4j en el BFF Web para las operaciones de lectura hacia el Backend.

Los patrones utilizados son:

```text
Circuit Breaker
Retry
Bulkhead
Fallback
```

---

## 12. Circuit Breaker

El Circuit Breaker permite detectar fallos repetidos del Backend.

La configuraciÃ³n utiliza una ventana reducida para facilitar la detecciÃ³n del fallo durante las pruebas del proyecto.

Ejemplo conceptual:

```text
Backend disponible
      |
      v
Circuit Breaker CLOSED
      |
      v
Solicitudes normales
```

Ante mÃºltiples fallos:

```text
Backend no disponible
      |
      v
Fallos consecutivos
      |
      v
Circuit Breaker OPEN
      |
      v
No se realizan nuevas llamadas innecesarias
      |
      v
Fallback
```

Cuando el circuito estÃ¡ abierto se evita continuar enviando solicitudes a un servicio que ya se encuentra detectado como indisponible.

---

## 13. Retry

Se utilizan reintentos controlados para las operaciones de lectura.

La configuraciÃ³n definida contempla:

```text
maxAttempts = 3
waitDuration = 300 ms
```

Los reintentos se aplican Ãºnicamente ante errores compatibles con fallos temporales de infraestructura o servidor.

No se reintentan automÃ¡ticamente errores de cliente.

---

## 14. Bulkhead

El patrÃ³n Bulkhead limita el nÃºmero de operaciones concurrentes hacia el Backend.

La configuraciÃ³n utilizada contempla:

```text
maxConcurrentCalls = 5
maxWaitDuration = 0 ms
```

El objetivo es evitar que una saturaciÃ³n de llamadas hacia una dependencia afecte completamente la capacidad de respuesta del BFF.

---

## 15. Estrategia de fallback

Cuando el Backend se encuentra temporalmente no disponible, el BFF Web retorna una respuesta controlada.

Ejemplo:

```text
nombre = Servicio no disponible
estado = TEMPORALMENTE_NO_DISPONIBLE
```

El objetivo es evitar que un fallo interno de infraestructura sea propagado directamente hacia el cliente como una respuesta no controlada.

---

## 16. Tratamiento especial de operaciones con efectos secundarios

No se aplica Retry automÃ¡tico sobre la operaciÃ³n de retiro ATM.

Esta decisiÃ³n es intencional.

Una operaciÃ³n:

```text
POST retiro
```

modifica el estado financiero de una cuenta.

Un reintento automÃ¡tico podrÃ­a provocar que, ante ciertas condiciones de red, una misma transacciÃ³n fuese procesada mÃ¡s de una vez.

Por esta razÃ³n los mecanismos de reintento se utilizan sobre operaciones de lectura, mientras las operaciones transaccionales mantienen un tratamiento conservador.

---

## 17. Fallback seguro para retiros

Ante indisponibilidad del Backend, el BFF ATM no informa falsamente que un retiro fue realizado.

La respuesta de fallback utiliza:

```text
montoRetirado = null
saldoDisponible = null
estadoOperacion = NO_VERIFICADA
```

De esta forma se evita presentar al cliente una transacciÃ³n como confirmada cuando el BFF no puede verificar el resultado en el Backend.

---

## 18. Arquitectura orientada a eventos

Para la mensajerÃ­a asÃ­ncrona se seleccionÃ³ Apache Kafka.

Kafka permite desacoplar el procesamiento posterior de determinados eventos respecto de la operaciÃ³n principal.

El evento implementado corresponde a:

```text
retiro-realizado
```

Cuando el Backend confirma correctamente un retiro, genera un evento con informaciÃ³n de la operaciÃ³n.

Ejemplo:

```json
{
  "cuentaId": 101,
  "monto": 1,
  "saldoDisponible": 6579,
  "fechaHora": "2026-10-04T17:17:42..."
}
```

---

## 19. Productor Kafka

El Backend actÃºa como productor del evento.

Flujo:

```text
Solicitud de retiro
        |
        v
Backend procesa transacciÃ³n
        |
        v
Retiro confirmado
        |
        v
RetiroEventProducer
        |
        v
Topic retiro-realizado
```

La publicaciÃ³n ocurre despuÃ©s de que la operaciÃ³n bancaria fue procesada correctamente.

---

## 20. Consumidor Kafka

El Backend contiene consumidores pertenecientes al grupo:

```text
auditoria-bankxyz
```

El consumidor procesa de manera asÃ­ncrona los eventos generados por retiros.

El flujo completo queda definido como:

```text
BFF ATM
   |
   v
Backend
   |
   v
Retiro confirmado
   |
   v
Kafka Producer
   |
   v
Topic retiro-realizado
   |
   v
Kafka Consumer
   |
   v
AuditorÃ­a
```

---

## 21. Escalabilidad mediante particiones

El tÃ³pico `retiro-realizado` dispone de tres particiones:

```text
retiro-realizado-0
retiro-realizado-1
retiro-realizado-2
```

La aplicaciÃ³n utiliza tres consumidores dentro del grupo:

```text
consumer-auditoria-bankxyz-1
consumer-auditoria-bankxyz-2
consumer-auditoria-bankxyz-3
```

Kafka distribuye las particiones entre los consumidores.

Ejemplo:

```text
Consumer 1 -> retiro-realizado-0
Consumer 2 -> retiro-realizado-1
Consumer 3 -> retiro-realizado-2
```

Esta distribuciÃ³n permite demostrar procesamiento paralelo y escalabilidad horizontal dentro del consumer group.

---

## 22. ContenerizaciÃ³n

Los componentes principales disponen de un `Dockerfile` independiente.

Se crean imÃ¡genes para:

```text
bankxyz-backend
bankxyz-discovery-server
bankxyz-config-server
bankxyz-auth-server
bankxyz-bff-web
bankxyz-bff-mobile
bankxyz-bff-atm
```

Apache Kafka utiliza su imagen correspondiente dentro de Docker Compose.

Los Dockerfiles utilizan un proceso de construcciÃ³n multietapa, separando la compilaciÃ³n Maven del runtime final.

---

## 23. Docker Compose

Docker Compose funciona como mecanismo de orquestaciÃ³n local del ecosistema.

Una Ãºnica configuraciÃ³n permite levantar:

```text
Discovery Server
Config Server
Authorization Server
Backend
BFF Web
BFF Mobile
BFF ATM
Kafka
```

Los ocho componentes funcionan de manera coordinada dentro del mismo entorno.

La red utilizada es:

```text
bankxyz-network
```

Esto permite que los servicios se comuniquen utilizando los nombres definidos en Docker Compose.

Ejemplos:

```text
backend:8080
config-server:8888
auth-server:9000
kafka:29092
```

---

## 24. ConfiguraciÃ³n de Kafka para Docker

Kafka dispone de listeners separados para conexiones desde el host y desde los contenedores.

```text
HOST   -> localhost:9092
DOCKER -> kafka:29092
```

Esta configuraciÃ³n evita que los servicios dentro de Docker intenten conectarse incorrectamente mediante `localhost`.

El Backend utiliza dentro del entorno Docker:

```text
kafka:29092
```

---

## 25. Persistencia

La persistencia del sistema continÃºa utilizando Oracle Autonomous Database.

El Backend accede a Oracle utilizando un Wallet externo.

El Wallet no se incorpora dentro de:

```text
Git
imagen Docker
cÃ³digo fuente
```

Docker Compose monta el Wallet desde el sistema anfitriÃ³n hacia el contenedor.

Su ubicaciÃ³n se configura mediante:

```text
ORACLE_WALLET_HOST_PATH
```

---

## 26. GestiÃ³n de secretos

La soluciÃ³n evita almacenar credenciales directamente en el cÃ³digo fuente.

Se utiliza un archivo local:

```text
.env
```

que contiene variables como:

```text
BANKXYZ_DB_PASSWORD

OAUTH_WEB_CLIENT_SECRET
OAUTH_MOBILE_CLIENT_SECRET
OAUTH_ATM_CLIENT_SECRET

BFF_WEB_SSL_KEYSTORE_PASSWORD
BFF_MOBILE_SSL_KEYSTORE_PASSWORD
BFF_ATM_SSL_KEYSTORE_PASSWORD
```

El archivo `.env` se encuentra excluido mediante `.gitignore`.

Para documentar la configuraciÃ³n requerida se incorpora:

```text
.env.example
```

sin valores reales.

---

## 27. Recursos excluidos del repositorio

Por seguridad no deben versionarse:

```text
.env
Oracle Wallet
keystore.p12
client secrets
contraseÃ±as
tokens OAuth2
archivos temporales de pruebas
```

Los archivos `.dockerignore` tambiÃ©n evitan incluir recursos sensibles dentro del contexto de construcciÃ³n de las imÃ¡genes.

---

## 28. Estrategia de despliegue

La soluciÃ³n puede desplegarse localmente mediante:

```text
docker compose up -d --build
```

Docker Compose construye y levanta la arquitectura completa.

La secuencia lÃ³gica de inicio considera:

```text
Kafka
Discovery Server
Config Server
Authorization Server
Backend
BFF Web
BFF Mobile
BFF ATM
```

Las dependencias declaradas y las polÃ­ticas de reinicio permiten que los componentes puedan estabilizarse progresivamente durante el arranque del ecosistema.

---

## 29. ValidaciÃ³n funcional

La propuesta fue validada mediante pruebas sobre los distintos componentes.

### OAuth2

Se verificÃ³:

```text
Authorization Server -> entrega JWT
Token WEB -> acceso BFF Web
Token WEB -> rechazo HTTP 403 en BFF Mobile
Token MOBILE -> acceso BFF Mobile
Token ATM -> acceso BFF ATM
```

### Docker

Se verificÃ³ la ejecuciÃ³n en contenedores de:

```text
Discovery Server
Config Server
Authorization Server
Backend
BFF Web
BFF Mobile
BFF ATM
Kafka
```

### Docker Compose

Se verificÃ³ que los ocho servicios fueran levantados desde una Ãºnica configuraciÃ³n y permanecieran en estado operativo.

### Resilience4j

Se verificÃ³:

```text
Backend disponible -> respuesta normal
Backend detenido    -> fallback
Fallos repetidos    -> Circuit Breaker OPEN
```

### Kafka

Se verificÃ³:

```text
3 consumidores
3 particiones
publicaciÃ³n de evento
consumo de evento
```

---

## 30. Flujo funcional completo de retiro

El flujo final de una operaciÃ³n de retiro es:

```text
Cliente ATM
    |
    | Solicita token
    v
Authorization Server
    |
    | JWT SCOPE_ATM
    v
BFF ATM
    |
    | POST retiro
    v
Backend Bank XYZ
    |
    +------> Oracle Autonomous Database
    |             |
    |             v
    |        Actualiza saldo
    |
    v
Retiro confirmado
    |
    v
Kafka Producer
    |
    v
Topic retiro-realizado
    |
    v
Kafka Consumer
    |
    v
AuditorÃ­a asÃ­ncrona
```

De esta manera se combinan:

- Seguridad.
- Persistencia.
- SeparaciÃ³n por canal.
- MensajerÃ­a asÃ­ncrona.
- Escalabilidad.
- ContenerizaciÃ³n.

---

## 31. Beneficios de la soluciÃ³n

La arquitectura propuesta entrega los siguientes beneficios:

### Seguridad

OAuth 2.0 permite separar la emisiÃ³n de credenciales de los servicios que protegen recursos.

Los scopes limitan el acceso segÃºn el tipo de cliente.

### Desacoplamiento

Kafka permite que operaciones posteriores al retiro puedan ejecutarse de manera asÃ­ncrona.

### Resiliencia

Resilience4j permite responder de manera controlada ante fallos temporales del Backend.

### Escalabilidad

Las particiones Kafka permiten distribuir procesamiento entre mÃºltiples consumidores.

### Mantenibilidad

La separaciÃ³n entre BFF, Backend, Authorization Server, Config Server y Discovery Server reduce responsabilidades mezcladas.

### Portabilidad

Docker permite ejecutar cada servicio dentro de un entorno independiente y reproducible.

### OrquestaciÃ³n

Docker Compose simplifica el levantamiento del ecosistema completo.

### ProtecciÃ³n de secretos

Las credenciales permanecen externas al cÃ³digo fuente mediante variables de entorno.

---

## 32. Consideraciones tÃ©cnicas

La soluciÃ³n corresponde a un entorno acadÃ©mico y de desarrollo.

Para un escenario productivo real serÃ­a recomendable complementar la arquitectura con:

- Certificados TLS emitidos por una autoridad certificadora.
- GestiÃ³n centralizada de secretos.
- ReplicaciÃ³n de Kafka.
- Alta disponibilidad del Authorization Server.
- Observabilidad centralizada.
- MÃ©tricas y alertas.
- Trazabilidad distribuida.
- Estrategias de despliegue en Kubernetes o servicios cloud equivalentes.

Estas mejoras no son necesarias para demostrar los objetivos funcionales actuales, pero constituyen una evoluciÃ³n natural de la arquitectura.

---

## 33. ConclusiÃ³n

La propuesta tÃ©cnica implementada permite evolucionar Bank XYZ desde una soluciÃ³n compuesta Ãºnicamente por APIs hacia un ecosistema distribuido que integra seguridad, resiliencia, descubrimiento de servicios, configuraciÃ³n centralizada, mensajerÃ­a asÃ­ncrona y contenerizaciÃ³n.

OAuth 2.0 centraliza la autenticaciÃ³n y permite aplicar autorizaciÃ³n especÃ­fica mediante scopes.

Resilience4j permite mantener respuestas controladas frente a fallos del Backend y evitar llamadas innecesarias cuando el Circuit Breaker se encuentra abierto.

Apache Kafka incorpora una arquitectura orientada a eventos, permitiendo desacoplar el procesamiento de auditorÃ­a de la transacciÃ³n bancaria principal y distribuir mensajes entre mÃºltiples consumidores.

Finalmente, Docker y Docker Compose permiten ejecutar y orquestar todos los componentes de manera reproducible dentro de un mismo entorno.

El resultado es una arquitectura Bank XYZ mÃ¡s segura, mantenible, escalable y tolerante a fallos, preparada conceptualmente para su evoluciÃ³n hacia entornos cloud.
