# Bank XYZ - Backend for Frontend (BFF)

Proyecto desarrollado para la asignatura **Desarrollo Backend III (PBY2203)**.

Esta versión corresponde a la continuidad del proyecto Bank XYZ, incorporando el patrón arquitectónico **Backend for Frontend (BFF)** para entregar servicios personalizados a tres tipos de clientes:

- Aplicación Web
- Aplicación Móvil
- Cajero Automático (ATM)

## Objetivo

El objetivo del proyecto es implementar el patrón Backend for Frontend (BFF), permitiendo que cada tipo de cliente disponga de un backend adaptado a sus necesidades.

El sistema mantiene un backend principal encargado del acceso a los datos del Banco XYZ y tres BFF independientes que consumen dicho backend mediante HTTP.

Cada BFF transforma la información antes de entregarla al frontend correspondiente.

## Arquitectura

La arquitectura implementada es la siguiente:

```text
                         Oracle Database
                               │
                               ▼
                   Backend principal Bank XYZ
                         Puerto 8080
                               │
                ┌──────────────┼──────────────┐
                │              │              │
                ▼              ▼              ▼
            BFF Web       BFF Mobile       BFF ATM
             8081             8082            8083
                │              │              │
                ▼              ▼              ▼
              Web            Móvil          Cajero
```

Se utilizó la estrategia de crear un backend específico para cada tipo de cliente.

Los BFF no acceden directamente a Oracle. Todos obtienen la información mediante las APIs REST del backend principal.

## Tecnologías utilizadas

- Java 18
- Spring Boot 4.1.1
- Spring MVC
- Spring Batch
- Spring JDBC
- Spring Security
- Oracle Database
- Oracle Wallet
- Maven
- Git / GitHub

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
└── pom.xml
```

## Backend principal

El backend principal se ejecuta en:

```text
http://localhost:8080
```

Su función es acceder a los datos almacenados en Oracle y proporcionar una API general que posteriormente es consumida por los BFF.

### Endpoints principales

| Método | Endpoint | Descripción |
|---|---|---|
| GET | `/api/cuentas` | Obtiene todas las cuentas |
| GET | `/api/cuentas/{id}` | Obtiene una cuenta por ID |
| POST | `/api/cuentas/{id}/retiro` | Realiza un retiro |

Ejemplo:

```text
GET http://localhost:8080/api/cuentas/101
```

## BFF Web

El BFF Web se ejecuta en:

```text
http://localhost:8081
```

Está orientado a navegadores y entrega información más completa de las cuentas.

### Endpoints

| Método | Endpoint | Descripción |
|---|---|---|
| GET | `/api/web/cuentas` | Lista las cuentas |
| GET | `/api/web/cuentas/{id}` | Obtiene el detalle de una cuenta |

Ejemplo:

```text
GET http://localhost:8081/api/web/cuentas/101
```

La respuesta Web contiene información como:

- ID de cuenta
- Nombre del cliente
- Edad
- Tipo de cuenta
- Saldo inicial
- Tasa de interés
- Interés generado
- Saldo actual
- Estado

## BFF Mobile

El BFF Mobile se ejecuta en:

```text
http://localhost:8082
```

Su objetivo es entregar respuestas más ligeras, evitando enviar información innecesaria a dispositivos móviles.

### Endpoints

| Método | Endpoint | Descripción |
|---|---|---|
| GET | `/api/mobile/cuentas` | Lista resumida de cuentas |
| GET | `/api/mobile/cuentas/{id}` | Obtiene información resumida de una cuenta |

Ejemplo:

```text
GET http://localhost:8082/api/mobile/cuentas/101
```

La respuesta Mobile contiene solamente:

- ID de cuenta
- Nombre
- Tipo de cuenta
- Saldo actual
- Estado

De esta forma se reduce la cantidad de información transferida en comparación con el BFF Web.

## BFF ATM

El BFF ATM se ejecuta en:

```text
http://localhost:8083
```

Está orientado a operaciones específicas de un cajero automático.

### Endpoints

| Método | Endpoint | Descripción |
|---|---|---|
| GET | `/api/atm/cuentas/{id}/saldo` | Consulta el saldo disponible |
| POST | `/api/atm/cuentas/{id}/retiro` | Realiza un retiro |

Ejemplo de consulta:

```text
GET http://localhost:8083/api/atm/cuentas/101/saldo
```

Ejemplo de retiro:

```json
{
    "monto": 100
}
```

El cajero recibe únicamente la información necesaria para sus operaciones.

## Personalización por frontend

Cada BFF utiliza el mismo backend principal, pero transforma los datos según las necesidades de su cliente.

```text
Backend principal
        │
        ├── BFF Web
        │     └── Información completa
        │
        ├── BFF Mobile
        │     └── Información resumida
        │
        └── BFF ATM
              └── Saldo y operaciones de retiro
```

Esto evita que todos los frontends dependan de una única respuesta general.

## Seguridad

Se implementó autenticación HTTP Basic con Spring Security.

Cada BFF utiliza un usuario y rol independiente:

| BFF | Rol |
|---|---|
| Web | `ROLE_WEB` |
| Mobile | `ROLE_MOBILE` |
| ATM | `ROLE_ATM` |

Sin credenciales válidas, los endpoints protegidos responden:

```text
HTTP/1.1 401 Unauthorized
```

Las contraseñas no se encuentran almacenadas directamente en el código fuente. Se configuran mediante variables de entorno.

## Variables de entorno

### Backend principal

Antes de ejecutar Bank XYZ se deben configurar:

```powershell
$env:ORACLE_WALLET_DIR='RUTA_DEL_ORACLE_WALLET'
$env:BANKXYZ_DB_PASSWORD='CONTRASEÑA_ORACLE'
$env:SPRING_BATCH_JOB_ENABLED='false'
```

`ORACLE_WALLET_DIR` debe apuntar a la carpeta descomprimida del Oracle Wallet.

### BFF Web

```powershell
$env:BFF_WEB_USERNAME='web_user'
$env:BFF_WEB_PASSWORD='CONTRASEÑA_WEB'
```

### BFF Mobile

```powershell
$env:BFF_MOBILE_USERNAME='mobile_user'
$env:BFF_MOBILE_PASSWORD='CONTRASEÑA_MOBILE'
```

### BFF ATM

```powershell
$env:BFF_ATM_USERNAME='atm_user'
$env:BFF_ATM_PASSWORD='CONTRASEÑA_ATM'
```

## Ejecución

Los servicios deben ejecutarse en terminales independientes.

### 1. Backend principal

Desde la raíz del proyecto:

```powershell
.\mvnw.cmd spring-boot:run
```

Puerto:

```text
8080
```

### 2. BFF Web

```powershell
.\mvnw.cmd -f bff-web\pom.xml spring-boot:run
```

Puerto:

```text
8081
```

### 3. BFF Mobile

```powershell
.\mvnw.cmd -f bff-mobile\pom.xml spring-boot:run
```

Puerto:

```text
8082
```

### 4. BFF ATM

```powershell
.\mvnw.cmd -f bff-atm\pom.xml spring-boot:run
```

Puerto:

```text
8083
```

El backend principal debe estar activo para que los BFF puedan obtener la información de las cuentas.

## Ejemplos de pruebas

### Web

Sin autenticación:

```powershell
curl.exe -i http://localhost:8081/api/web/cuentas/101
```

Respuesta esperada:

```text
401 Unauthorized
```

Con autenticación:

```powershell
curl.exe -i -u "web_user:CONTRASEÑA" http://localhost:8081/api/web/cuentas/101
```

Respuesta esperada:

```text
200 OK
```

### Mobile

```powershell
curl.exe -i -u "mobile_user:CONTRASEÑA" http://localhost:8082/api/mobile/cuentas/101
```

### ATM - Consulta de saldo

```powershell
curl.exe -i -u "atm_user:CONTRASEÑA" http://localhost:8083/api/atm/cuentas/101/saldo
```

### ATM - Retiro

```powershell
curl.exe -i `
    -u "atm_user:CONTRASEÑA" `
    -X POST `
    "http://localhost:8083/api/atm/cuentas/101/retiro" `
    -H "Content-Type: application/json" `
    --data-raw '{\"monto\":100}'
```

## Conclusión

La implementación permite aplicar el patrón Backend for Frontend en Bank XYZ mediante tres backends especializados.

Cada frontend dispone de endpoints, formato de respuesta y seguridad propios, mientras que el acceso a los datos permanece centralizado en el backend principal.

Con esto se evita sobrecargar a los clientes con información innecesaria y se mantiene separada la lógica específica de Web, Mobile y ATM.