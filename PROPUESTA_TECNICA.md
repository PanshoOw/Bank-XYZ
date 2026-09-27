# Propuesta Técnica – Bank XYZ

## 1. Contexto

Bank XYZ corresponde a un sistema backend bancario desarrollado de forma incremental para la asignatura **Desarrollo Backend III (PBY2203)**.

En etapas anteriores se implementó una arquitectura basada en **Backend for Frontend (BFF)**, separando las necesidades de los canales Web, Mobile y ATM. Posteriormente se incorporaron componentes de Spring Cloud, seguridad mediante HTTPS y JWT, configuración centralizada y mecanismos de tolerancia a fallos.

Para la Semana 7, la solución evoluciona incorporando una **arquitectura orientada a eventos**, con el propósito de desacoplar procesos derivados de las operaciones bancarias y habilitar procesamiento asíncrono y concurrente.

La actividad solicita definir una arquitectura de eventos, representarla mediante un diagrama, implementar tolerancia a fallos con Resilience4j e integrar Kafka o JMS de manera funcional y escalable. :contentReference[oaicite:1]{index=1}

---

## 2. Objetivo de la propuesta

La propuesta busca fortalecer Bank XYZ mediante una arquitectura que combine:

- Separación de responsabilidades entre canales.
- Configuración centralizada.
- Registro y descubrimiento de servicios.
- Seguridad en las comunicaciones.
- Tolerancia a fallos.
- Procesamiento asíncrono de eventos.
- Capacidad de procesamiento concurrente.
- Preparación para futuras extensiones del sistema.

La solución mantiene Oracle Database como fuente del estado bancario e incorpora Apache Kafka como plataforma de distribución de eventos.

---

## 3. Arquitectura propuesta

La solución se estructura mediante los siguientes componentes principales:

```text
Clientes
   │
   ▼
BFF Web / Mobile / ATM
   │
   ▼
Backend Bank XYZ
   │
   ├──────────────▶ Oracle Database
   │
   └──────────────▶ Apache Kafka
                           │
                           ▼
                     Consumer Group
                           │
                           ▼
                     Auditoría en log
```

Complementariamente se utilizan:

```text
Spring Cloud Config
        +
Eureka Discovery Server
        +
Resilience4j
        +
HTTPS / JWT
```

El diagrama detallado de la solución se encuentra documentado en:

```text
Arquitectura_Eventos_BankXYZ.drawio
Arquitectura_Eventos_BankXYZ.png
```

---

## 4. Decisión arquitectónica: Event-Driven Architecture

Para esta etapa se seleccionó una **arquitectura orientada a eventos (Event-Driven Architecture)** basada en el modelo:

```text
Producer → Topic → Consumer
```

La decisión responde a la necesidad de desacoplar la operación bancaria principal de procesos que pueden ejecutarse posteriormente de forma asíncrona.

En una operación de retiro, el proceso principal mantiene la responsabilidad de:

1. Validar la solicitud.
2. Actualizar el saldo.
3. Persistir el nuevo estado en Oracle.

Una vez finalizada correctamente la operación, se genera un evento que representa el hecho ocurrido.

```text
Retiro realizado
      │
      ▼
RetiroRealizadoEvent
      │
      ▼
Apache Kafka
      │
      ▼
Procesamiento asíncrono
```

Este enfoque evita incorporar directamente nuevas responsabilidades dentro de la lógica transaccional del retiro.

---

## 5. Elección de Apache Kafka

Se seleccionó **Apache Kafka** como plataforma de mensajería asíncrona.

Kafka resulta adecuado para la solución debido a que proporciona:

- Desacoplamiento entre productor y consumidor.
- Persistencia temporal de eventos.
- Organización mediante tópicos.
- Particionamiento.
- Procesamiento concurrente.
- Posibilidad de incorporar nuevos consumidores en futuras etapas.

En el entorno de desarrollo Kafka se ejecuta mediante Docker y utiliza un único broker.

---

## 6. Diseño de tópicos

La arquitectura contempla tres eventos bancarios principales:

| Tópico | Estado |
|---|---|
| `retiro-realizado` | Implementado |
| `deposito-realizado` | Preparado |
| `transferencia-realizada` | Preparado |

Los tres tópicos poseen:

```text
3 particiones
Replication Factor: 1
```

El factor de replicación se mantiene en `1` porque el entorno académico utiliza un único broker Kafka.

Durante esta etapa sólo `retiro-realizado` posee un flujo funcional completo de productor y consumidor.

Los otros dos tópicos fueron definidos como preparación para la evolución posterior del sistema.

---

## 7. Evento implementado

El evento utilizado para representar un retiro correctamente procesado corresponde a:

```text
RetiroRealizadoEvent
```

Su contrato contiene:

```text
cuentaId
monto
saldoDisponible
fechaHora
```

El evento no intenta representar el estado completo de la cuenta, sino únicamente la información necesaria para comunicar que el retiro ocurrió correctamente.

La clave utilizada al publicar en Kafka corresponde a:

```text
cuentaId.toString()
```

El uso del identificador de cuenta como clave permite mantener una estrategia de particionado consistente para eventos relacionados con una misma cuenta.

---

## 8. Flujo de procesamiento

El flujo implementado corresponde a:

```text
Solicitud de retiro
        │
        ▼
CuentaController
        │
        ▼
CuentaService
        │
        ├──── Validaciones
        │
        ▼
CuentaRepository
        │
        ▼
Oracle Database
        │
        ▼
Saldo actualizado
        │
        ▼
RetiroRealizadoEvent
        │
        ▼
RetiroEventProducer
        │
        ▼
retiro-realizado
        │
        ▼
RetiroEventConsumer
        │
        ▼
Registro de auditoría
```

La publicación del evento ocurre después de procesar satisfactoriamente la actualización de la cuenta.

Esto evita generar eventos de retiro para operaciones rechazadas por las validaciones de negocio.

---

## 9. Estrategia de consumo y escalabilidad

El tópico `retiro-realizado` posee tres particiones.

El consumidor pertenece al grupo:

```text
auditoria-bankxyz
```

y está configurado con:

```text
concurrency = 3
```

La relación conceptual es:

```text
retiro-realizado
 ┌────┬────┬────┐
 P0   P1   P2
 └────┴────┴────┘
        │
        ▼
auditoria-bankxyz
        │
 ┌──────┼──────┐
 C1     C2     C3
```

Los tres consumidores corresponden a ejecución concurrente del mismo listener y no a tres microservicios independientes.

Esta configuración permite distribuir el trabajo entre las particiones disponibles y demostrar procesamiento paralelo de eventos.

---

## 10. Tolerancia a fallos

Los BFF implementan tolerancia a fallos mediante **Resilience4j Circuit Breaker**.

La configuración utilizada contempla:

```text
Sliding Window Size:       4
Minimum Number of Calls:   2
Failure Rate Threshold:   50 %
Wait Duration Open State: 10 segundos
```

El objetivo es evitar que una dependencia no disponible provoque fallos sin controlar hacia los clientes.

Cuando el backend principal presenta una indisponibilidad, el BFF ejecuta una respuesta fallback.

Ejemplo:

```text
TEMPORALMENTE_NO_DISPONIBLE
```

Durante las pruebas se verificaron ambos estados:

```text
Backend disponible
        ↓
Datos reales
```

y:

```text
Backend no disponible
        ↓
Resilience4j
        ↓
Fallback controlado
```

De esta manera, la resiliencia se mantiene separada de la lógica de negocio bancaria.

---

## 11. Persistencia y Event Sourcing

Oracle Database continúa siendo la **fuente de verdad del estado actual de las cuentas**.

Kafka se utiliza para comunicar hechos ocurridos después de las operaciones bancarias.

Por esta razón, la solución implementada **no corresponde a Event Sourcing completo**.

En esta arquitectura:

```text
Oracle
   │
   └── Estado actual de la cuenta

Kafka
   │
   └── Eventos derivados de operaciones
```

El saldo no se reconstruye leyendo el historial de Kafka.

Esta distinción permite utilizar los beneficios de una arquitectura orientada a eventos sin reemplazar el modelo de persistencia actualmente implementado.

---

## 12. Seguridad y configuración

La incorporación de Kafka mantiene los mecanismos de seguridad desarrollados previamente.

Los BFF utilizan:

- HTTPS.
- JWT.
- Scopes por canal.
- Sesiones stateless.
- Secretos almacenados mediante variables de entorno.

Los canales se mantienen separados mediante:

```text
WEB
MOBILE
ATM
```

Spring Cloud Config centraliza parámetros operativos de los BFF y Eureka proporciona registro y visualización de los servicios disponibles.

Estas capacidades permanecen independientes de la arquitectura Kafka.

---

## 13. Decisiones de alcance

Para mantener una implementación proporcional al alcance académico de esta etapa se definieron las siguientes decisiones:

### Auditoría mediante logs

El evento consumido se registra mediante el sistema de logs de la aplicación.

Actualmente no existe una base de datos de auditoría independiente.

### Un solo evento funcional

Se implementó completamente:

```text
retiro-realizado
```

Los eventos de depósito y transferencia permanecen preparados para futuras etapas.

### Broker único

El entorno local utiliza un solo broker Kafka.

Por ello no se busca demostrar alta disponibilidad del cluster, sino:

- creación de tópicos;
- particionado;
- publicación;
- consumo;
- concurrencia.

---

## 14. Limitaciones técnicas

### Consistencia entre Oracle y Kafka

La actualización de Oracle y la publicación del evento Kafka son operaciones independientes.

Actualmente el flujo corresponde conceptualmente a:

```text
Actualizar Oracle
      │
      ▼
Publicar Kafka
```

Esto implica que, ante una falla excepcional ocurrida entre ambas operaciones, podría actualizarse correctamente Oracle sin llegar a publicarse el evento.

Para el alcance actual esta condición se considera aceptable.

En una solución productiva debería utilizarse un patrón especializado para resolver esta consistencia.

---

### Persistencia de auditoría

Los eventos procesados se registran únicamente mediante logs.

Una futura evolución podría almacenarlos en un repositorio específico de auditoría.

---

### Alta disponibilidad Kafka

El entorno posee:

```text
1 broker
Replication Factor = 1
```

Un ambiente productivo debería utilizar múltiples brokers y un factor de replicación mayor.

---

## 15. Evolución propuesta

La arquitectura implementada permite continuar evolucionando sin modificar significativamente la lógica del retiro.

Entre las extensiones posibles se encuentran:

```text
retiro-realizado
        │
        ├── Auditoría
        ├── Notificaciones
        ├── Monitoreo
        ├── Prevención de fraude
        └── Analítica
```

También pueden incorporarse los flujos:

```text
deposito-realizado
transferencia-realizada
```

mediante sus respectivos producers y consumers.

---

### Transactional Outbox

Una evolución relevante para un escenario productivo sería implementar el patrón **Transactional Outbox**.

La idea sería almacenar el evento dentro de la misma transacción que modifica el estado bancario:

```text
Transacción Oracle
   │
   ├── Actualización cuenta
   └── Registro evento pendiente
```

Posteriormente otro proceso publicaría el evento hacia Kafka.

Esto reduciría el riesgo de inconsistencia entre la actualización de Oracle y la publicación del mensaje.

Esta funcionalidad se plantea únicamente como evolución futura y no forma parte de la implementación actual.

---

## 16. Evaluación de la propuesta

La solución implementada permite demostrar:

- Arquitectura orientada a eventos.
- Definición explícita de tópicos.
- Representación de mensajes y eventos.
- Producer Kafka funcional.
- Consumer Kafka funcional.
- Procesamiento asíncrono.
- Tres particiones.
- Tres consumidores concurrentes.
- Tolerancia a fallos mediante Resilience4j.
- Respuesta fallback ante indisponibilidad.
- Integración con la arquitectura BFF existente.

Las evidencias de ejecución y el diagrama permiten verificar estos componentes de forma independiente.

---

## 17. Conclusión

La incorporación de Apache Kafka permite que Bank XYZ evolucione desde una arquitectura basada principalmente en comunicaciones síncronas hacia una solución que combina procesamiento síncrono y asíncrono.

Oracle mantiene la responsabilidad sobre el estado bancario, mientras Kafka distribuye eventos derivados de las operaciones realizadas.

El modelo `Producer → Topic → Consumer` permite desacoplar responsabilidades y facilita futuras extensiones sin incorporar nuevas dependencias directamente en el flujo principal del retiro.

El uso de tres particiones y tres consumidores concurrentes permite demostrar capacidad de procesamiento paralelo, mientras Resilience4j proporciona tolerancia a fallos en las comunicaciones entre los BFF y el backend principal.

La arquitectura resultante mantiene las capacidades implementadas previamente —BFF, seguridad, configuración centralizada y descubrimiento de servicios— e incorpora una base orientada a eventos preparada para continuar evolucionando durante las siguientes etapas del proyecto.