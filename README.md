# Bank XYZ - Procesamiento Batch

Proyecto desarrollado para la asignatura **Desarrollo Backend III (PBY2203)**.

El objetivo es modernizar procesos batch legacy del Banco XYZ utilizando **Spring Batch**, procesando archivos CSV con información bancaria, aplicando validaciones y transformaciones, manejando registros incorrectos y persistiendo los resultados en una base de datos Oracle.

## Tecnologías utilizadas

- Java 18
- Spring Boot
- Spring Batch
- Spring JDBC
- Oracle Database
- Maven
- Oracle Wallet
- Git / GitHub

## Procesos implementados

El proyecto contiene tres Jobs principales.

### 1. Reporte de Transacciones Diarias

Job:

```text
reporteTransaccionesDiariasJob
```

Procesa las transacciones diarias, valida los registros y genera un resumen de las operaciones procesadas.

Principales resultados:

- 1000 registros de entrada.
- 785 registros procesados.
- 215 registros omitidos debido a errores de datos.
- 387 registros válidos.
- 398 registros con anomalías.
- Generación del resumen diario de transacciones.

### 2. Cálculo de Intereses Mensuales

Job:

```text
calculoInteresesMensualesJob
```

Procesa información de cuentas bancarias y calcula los intereses correspondientes a cuentas de ahorro y préstamos.

Se utilizan las siguientes tasas:

- Ahorro: 1 %
- Préstamo: 2 %

Principales resultados:

- 1000 registros de entrada.
- 363 registros omitidos por campos obligatorios inválidos o faltantes.
- 50 cuentas consolidadas en la base de datos.
- Validación correcta de tasas de interés.
- Validación correcta del interés calculado.
- Validación correcta del saldo final.

### 3. Estados de Cuenta Anuales

Job:

```text
estadosCuentaAnualesJob
```

Procesa los movimientos anuales y genera estados de cuenta consolidados para su utilización en procesos de auditoría.

Principales resultados:

- 1000 registros de entrada.
- 952 registros procesados.
- 48 registros omitidos.
- 329 registros válidos.
- 623 registros clasificados con anomalías.
- Generación de estados de cuenta anuales consolidados.

## Arquitectura del proyecto

El procesamiento utiliza la arquitectura propia de Spring Batch:

```text
CSV
 ↓
ItemReader
 ↓
ItemProcessor
 ↓
ItemWriter
 ↓
Oracle Database
```

Los Jobs se encuentran separados de sus respectivas configuraciones de procesamiento.

Entre los principales componentes del proyecto se encuentran:

```text
Config/
    BatchJobConfig
    TransaccionesConfig
    InteresesConfig
    CuentasConfig
    ExecutorConfig

Reader/
Processor/
Writer/
Listener/
Policy/
Partition/
Decider/
Model/
Exception/
```

### Reader

Lee los registros provenientes de los archivos CSV legacy.

### Processor

Realiza transformaciones, validaciones y clasificación de anomalías antes de persistir los datos.

### Writer

Persiste los resultados procesados utilizando Spring JDBC y Oracle.

## Tolerancia a fallos

Los Steps principales se encuentran configurados mediante:

```java
.faultTolerant()
```

El proyecto implementa una política personalizada de omisión mediante `BankSkipPolicy`.

Entre las situaciones controladas se encuentran:

- Campos obligatorios faltantes.
- Montos inválidos.
- Fechas inválidas.
- Errores de formato.
- Registros con información inconsistente.

Los registros descartados son registrados mediante `SkipListener`, permitiendo identificar el registro y la causa del problema.

También se utiliza un `BankJobDecider` para determinar si cada Job terminó con registros omitidos.

## Política de reintentos

Para errores transitorios relacionados con el acceso a la base de datos se utiliza:

```java
.retry(TransientDataAccessException.class)
.retryLimit(2)
```

En el proceso de intereses también se controla específicamente una posible condición de concurrencia asociada a claves duplicadas durante escrituras paralelas.

Los errores correspondientes a datos de negocio no se reintentan, sino que son gestionados mediante la política de omisión.

## Procesamiento paralelo

El proyecto utiliza **partitioning** de Spring Batch.

Configuración utilizada:

```text
Grid size: 5
Chunk size: 5
Pool size: 3
```

El `TaskExecutor` permite ejecutar las particiones utilizando múltiples workers:

```text
batch-worker-1
batch-worker-2
batch-worker-3
```

## Comparación de rendimiento

Se probaron distintas configuraciones del pool de threads utilizando el mismo Job, los mismos 1000 registros y la misma configuración de particiones.

| Threads | Tiempo del Job |
|---:|---:|
| 1 | 7,251 s |
| 2 | 4,593 s |
| 3 | 3,401 s |

La configuración con **3 threads** obtuvo el mejor tiempo en las pruebas realizadas.

En comparación con un solo thread, el tiempo total disminuyó aproximadamente un **53 %**.

Por esta razón se seleccionó como configuración final:

```properties
app.poolSize=3
```

## Base de datos

El proyecto utiliza Oracle Database.

Entre las tablas utilizadas para almacenar los resultados se encuentran:

```text
TRANSACCIONES_PROCESADAS
RESUMEN_TRANSACCIONES_DIARIAS

INTERESES_PROCESADOS

MOVIMIENTOS_ANUALES_PROCESADOS
ESTADOS_CUENTA_ANUALES
```

## Configuración de conexión

La conexión a Oracle utiliza Oracle Wallet.

La contraseña de la base de datos y la ubicación del Wallet se configuran mediante variables de entorno para evitar almacenar credenciales directamente en el repositorio.

Ejemplo en PowerShell:

```powershell
$env:ORACLE_WALLET_DIR="RUTA_DEL_WALLET"
$env:BANKXYZ_DB_PASSWORD="CONTRASEÑA"
```

Configuración principal en `application.properties`:

```properties
spring.datasource.url=jdbc:oracle:thin:@bankxyz_tp
spring.datasource.username=BANKXYZ_APP
spring.datasource.password=${BANKXYZ_DB_PASSWORD}
spring.datasource.driver-class-name=oracle.jdbc.OracleDriver

spring.datasource.hikari.data-source-properties[oracle.net.tns_admin]=${ORACLE_WALLET_DIR}

spring.batch.jdbc.initialize-schema=never
spring.batch.jdbc.isolation-level-for-create=READ_COMMITTED

app.totalRecords=1000
app.poolSize=3
app.maxSkipCount=100
```

## Selección del Job

Para ejecutar un Job específico debe modificarse la propiedad:

```properties
spring.batch.job.name=
```

### Transacciones diarias

```properties
spring.batch.job.name=reporteTransaccionesDiariasJob
```

### Intereses mensuales

```properties
spring.batch.job.name=calculoInteresesMensualesJob
```

### Estados de cuenta anuales

```properties
spring.batch.job.name=estadosCuentaAnualesJob
```

## Ejecución

Desde la carpeta raíz del proyecto ejecutar:

```powershell
.\mvnw.cmd spring-boot:run
```

Spring Batch ejecutará únicamente el Job seleccionado en `application.properties`.

Una ejecución correcta finaliza con un mensaje similar a:

```text
status: [COMPLETED]
```

seguido por:

```text
BUILD SUCCESS
```

## Manejo de credenciales

Las credenciales de Oracle no deben almacenarse directamente dentro del repositorio.

Antes de ejecutar el proyecto se deben configurar las variables de entorno correspondientes al Wallet y a la contraseña de la base de datos.

## Evidencias

Las evidencias de ejecución incluyen:

- Ejecución del Job de transacciones diarias.
- Resultados generados en Oracle.
- Ejecución del Job de intereses mensuales.
- Validación de cálculos de intereses y saldos.
- Ejecución del Job de estados de cuenta anuales.
- Resultados consolidados en Oracle.
- Ejecución paralela mediante tres workers.

## Resultado final

La solución implementa los tres procesos batch solicitados utilizando Spring Batch, incorporando validaciones, tolerancia a fallos, políticas de omisión, reintentos, procesamiento mediante chunks, particionamiento y ejecución multithread.

Las pruebas realizadas muestran que la configuración con tres threads mejora el rendimiento del procesamiento manteniendo la consistencia de los resultados generados.