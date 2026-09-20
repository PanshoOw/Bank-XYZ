# Propuesta Técnica - Bank XYZ

## Experiencia 3 - Semana 6

Proyecto desarrollado para la asignatura **Desarrollo Backend III (PBY2203)**.

---

## 1. Contexto

Bank XYZ dispone de un backend principal conectado a Oracle Database y tres Backend for Frontend (BFF) independientes:

- BFF Web.
- BFF Mobile.
- BFF ATM.

Durante esta etapa el proyecto evoluciona hacia una arquitectura distribuida utilizando **Spring Cloud**, incorporando configuración centralizada, descubrimiento de servicios y mecanismos de tolerancia a fallos.

---

## 2. Objetivo de la propuesta

El objetivo es mejorar la integración, mantenibilidad y resiliencia de los servicios mediante:

- Spring Cloud Config.
- Eureka Service Discovery.
- Resilience4j.
- Circuit Breaker y Fallback.
- Autenticación y autorización mediante JWT.
- Comunicación HTTPS en los BFF.

---

## 3. Arquitectura propuesta

La solución está compuesta por:

```text
Config Server
     │
     ├── BFF Web
     ├── BFF Mobile
     └── BFF ATM
            │
            ▼
       Backend Bank XYZ
            │
            ▼
       Oracle Database

Los tres BFF se registran además en Eureka Server.
```

Componentes principales:

- **Config Server:** centraliza configuraciones operacionales.
- **Eureka Server:** mantiene el registro dinámico de los microservicios.
- **BFF Web:** entrega información completa para clientes web.
- **BFF Mobile:** entrega información reducida para dispositivos móviles.
- **BFF ATM:** proporciona operaciones específicas para cajeros automáticos.
- **Backend principal:** administra el acceso a Oracle Database.

---

## 4. Configuración centralizada

Se implementó **Spring Cloud Config Server** para evitar mantener toda la configuración operacional dentro de cada microservicio.

Las siguientes propiedades fueron centralizadas:

- Puerto de ejecución.
- URL del backend principal.
- Configuración de Eureka.
- Configuración de Actuator.
- Parámetros de Resilience4j.
- Identificación del canal.
- Issuer JWT.
- Scope JWT.
- Tiempo de expiración de tokens.

Las credenciales, secretos JWT y contraseñas de certificados permanecen fuera del repositorio mediante variables de entorno.

Esto permite modificar configuraciones operacionales sin incorporarlas directamente al código fuente de cada BFF.

---

## 5. Service Discovery

Se implementó **Netflix Eureka** como servidor de descubrimiento de servicios.

Los siguientes microservicios se registran automáticamente:

```text
BFF-WEB
BFF-MOBILE
BFF-ATM
```

Eureka mantiene un registro dinámico de las instancias disponibles.

Esto reduce el acoplamiento asociado a la administración manual de ubicaciones de servicios y facilita futuras ampliaciones del ecosistema.

---

## 6. Tolerancia a fallos

Los tres BFF incorporan **Resilience4j** mediante el patrón Circuit Breaker.

Cuando el backend principal funciona correctamente, los BFF entregan los datos reales.

Si el backend deja de responder:

```text
Solicitud
    ↓
BFF
    ↓
Circuit Breaker
    ↓
Fallback
    ↓
Respuesta controlada
```

El cliente recibe una respuesta indicando que el servicio se encuentra temporalmente no disponible, evitando propagar directamente el fallo del backend.

La configuración utilizada considera:

```yaml
slidingWindowSize: 4
minimumNumberOfCalls: 2
failureRateThreshold: 50
waitDurationInOpenState: 10s
```

También se verificó que, una vez restablecido el backend principal, los BFF vuelvan a entregar información real.

---

## 7. Seguridad

Se mantiene la arquitectura de seguridad implementada previamente.

Cada BFF utiliza:

- HTTPS.
- Certificado SSL/TLS independiente.
- Keystore PKCS12.
- Spring Security.
- JWT.
- Firma HMAC SHA-256.
- Autorización mediante scopes.
- Sesiones stateless.
- Variables de entorno para secretos.

Scopes utilizados:

```text
Web    → WEB
Mobile → MOBILE
ATM    → ATM
```

Una solicitud sin autenticación válida recibe:

```text
401 Unauthorized
```

Mientras que un JWT válido y autorizado permite acceder al recurso correspondiente.

---

## 8. Resiliencia

La incorporación de Circuit Breaker y Fallback permite que una caída del backend principal no genere una interrupción descontrolada en los BFF.

Durante las pruebas se verificó:

```text
Backend disponible
→ Datos reales

Backend no disponible
→ Fallback

Backend restablecido
→ Datos reales nuevamente
```

Esto permite que el sistema maneje fallos de manera controlada y mejore su capacidad de recuperación.

---

## 9. Escalabilidad

La arquitectura separa responsabilidades entre los distintos componentes:

```text
Configuración      → Config Server
Descubrimiento     → Eureka Server
Canal Web          → BFF Web
Canal Mobile       → BFF Mobile
Canal ATM          → BFF ATM
Persistencia       → Backend Bank XYZ / Oracle
Resiliencia        → Resilience4j
Seguridad          → Spring Security + JWT
```

Esta separación permite incorporar nuevos microservicios o nuevas instancias sin modificar significativamente los servicios existentes.

---

## 10. Ventajas

La solución proporciona:

- Configuración centralizada.
- Servicios independientes.
- Descubrimiento dinámico.
- Mejor tolerancia a fallos.
- Respuestas controladas ante indisponibilidad.
- Seguridad específica por canal.
- Mayor capacidad de mantenimiento.
- Arquitectura preparada para futuras ampliaciones.

---

## 11. Consideraciones

Actualmente los BFF consumen el backend principal utilizando una URL configurada centralmente mediante Config Server.

La arquitectura permite que, en futuras evoluciones, el backend principal también sea registrado como servicio en Eureka y pueda ser localizado mediante Service Discovery.

Además, el proyecto incluye dependencias que permiten futuras ampliaciones con mecanismos como Load Balancer, Rate Limiter y Bulkhead.

Estas funcionalidades no forman parte de la implementación activa de esta etapa.

---

## 12. Conclusión

La incorporación de Spring Cloud permite evolucionar Bank XYZ desde una arquitectura basada únicamente en BFF hacia un ecosistema distribuido con configuración centralizada y descubrimiento de servicios.

Config Server permite administrar configuraciones operacionales desde un punto central.

Eureka registra dinámicamente los tres BFF.

Resilience4j permite gestionar fallos mediante Circuit Breaker y Fallback.

Finalmente, la solución mantiene los mecanismos de seguridad mediante HTTPS, JWT y autorización específica por canal.

Con estas modificaciones, Bank XYZ cuenta con una arquitectura más resiliente, modular, configurable y preparada para futuras extensiones.