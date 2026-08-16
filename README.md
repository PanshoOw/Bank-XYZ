# Banco XYZ - Procesamiento Batch con Spring Batch

## Descripción

Proyecto desarrollado para la asignatura **Desarrollo Backend III (PBY2203)**, orientado a la modernización de procesos batch pertenecientes a un sistema legacy del Banco XYZ mediante **Spring Batch**.

La aplicación procesa archivos CSV correspondientes a transacciones diarias, cálculo de intereses mensuales y movimientos anuales de cuentas. Los datos son leídos, transformados, validados y posteriormente persistidos en una base de datos **Oracle Autonomous Database en Oracle Cloud**.

La solución incorpora reglas de validación de negocio, detección de anomalías, tolerancia a errores durante el procesamiento, persistencia mediante JDBC y almacenamiento de la metadata de ejecución de Spring Batch.

---

## Objetivo

Implementar una solución batch capaz de migrar y procesar información proveniente de archivos CSV legacy, asegurando la consistencia e integridad de los datos mediante Spring Batch.

El sistema contempla tres procesos principales:

* **Reporte de transacciones diarias:** procesa transacciones, identifica anomalías y persiste los resultados validados.
* **Cálculo de intereses mensuales:** calcula intereses para cuentas de ahorro y préstamos, determina el saldo final y registra inconsistencias.
* **Generación de estados de cuenta anuales:** procesa movimientos anuales y consolida la información por cuenta y año para fines de auditoría.

---

## Tecnologías utilizadas

* Java 18
* Spring Boot 4.1.0
* Spring Batch
* Spring Batch JDBC
* Spring JDBC
* Oracle JDBC
* Oracle Autonomous Database
* Oracle Wallet
* HikariCP
* Maven
* Git y GitHub

---

## Arquitectura general

Cada proceso sigue el modelo de procesamiento de Spring Batch:

```text
Archivo CSV
    ↓
ItemReader
    ↓
ItemProcessor
    ↓
Validación y transformación
    ↓
ItemWriter
    ↓
Oracle Autonomous Database
```

Los procesos se encuentran separados en Jobs independientes, permitiendo su ejecución y seguimiento individual.

---

## Estructura del proyecto

```text
demo/
│
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/duoc/demo/
│   │   │       ├── BatchConfig/
│   │   │       │   ├── CuentasBatch.java
│   │   │       │   ├── InteresesBatch.java
│   │   │       │   └── transaccionesBatch.java
│   │   │       │
│   │   │       ├── Legacy/
│   │   │       │   ├── LegacyCuentas.java
│   │   │       │   ├── LegacyIntereses.java
│   │   │       │   └── LegacyTransacciones.java
│   │   │       │
│   │   │       ├── Modern/
│   │   │       │   ├── ModernCuentas.java
│   │   │       │   ├── ModernIntereses.java
│   │   │       │   └── ModernTransacciones.java
│   │   │       │
│   │   │       ├── Processor/
│   │   │       │   ├── LegacyCuentasProcessor.java
│   │   │       │   ├── LegacyInteresesProcessor.java
│   │   │       │   └── LegacyTransaccionesProcessor.java
│   │   │       │
│   │   │       ├── BatchJobConfig.java
│   │   │       └── DemoApplication.java
│   │   │
│   │   └── resources/
│   │       ├── data/
│   │       │   ├── cuentas_anuales.csv
│   │       │   ├── intereses.csv
│   │       │   └── transacciones.csv
│   │       │
│   │       └── application.properties
│   │
│   └── test/
│
├── pom.xml
├── mvnw
├── mvnw.cmd
└── README.md
```

---

# Procesos Batch

## 1. Reporte de Transacciones Diarias

### Job

```text
reporteTransaccionesDiariasJob
```

### Step

```text
stepTransacciones
```

### Archivo de entrada

```text
transacciones.csv
```

### Funcionamiento

El proceso:

1. Lee las transacciones desde el archivo CSV.
2. Convierte los datos legacy a tipos Java adecuados.
3. Valida cada transacción.
4. Detecta posibles anomalías.
5. Persiste los resultados en Oracle.

### Validaciones implementadas

Se consideran anomalías:

* Montos negativos.
* Montos iguales a cero.
* Posibles transacciones duplicadas.

Los registros se clasifican mediante:

```text
VALIDA
ANOMALIA
```

Cuando se detecta una anomalía, se almacena además su descripción en `DETALLE_VALIDACION`.

### Tabla de salida

```text
TRANSACCIONES_PROCESADAS
```

La persistencia se realiza mediante `JdbcBatchItemWriter` utilizando una sentencia `MERGE`, lo que permite actualizar registros existentes o insertar registros nuevos.

---

## 2. Cálculo de Intereses Mensuales

### Job

```text
calculoInteresesMensualesJob
```

### Step

```text
stepIntereses
```

### Archivo de entrada

```text
intereses.csv
```

### Funcionamiento

El proceso:

1. Lee la información de las cuentas.
2. Valida el saldo y el tipo de cuenta.
3. Determina la tasa de interés correspondiente.
4. Calcula el interés mensual.
5. Calcula el saldo final.
6. Detecta posibles inconsistencias.
7. Persiste los resultados en Oracle.

### Tasas utilizadas

Como decisión técnica del proyecto se definieron las siguientes tasas mensuales:

```text
Cuenta de ahorro:  1 %
Préstamo:          2 %
```

### Fórmulas

```text
INTERES_CALCULADO = SALDO_INICIAL × TASA_INTERES
```

```text
SALDO_FINAL = SALDO_INICIAL + INTERES_CALCULADO
```

### Validaciones implementadas

Se consideran anomalías:

* Saldo negativo.
* Saldo igual a cero.
* Tipo de cuenta no soportado.
* Posibles registros duplicados.

Los tipos contemplados para el cálculo son:

```text
ahorro
prestamo
```

Los tipos diferentes se conservan en la base de datos, pero se clasifican como anomalía para mantener trazabilidad.

### Tabla de salida

```text
INTERESES_PROCESADOS
```

La persistencia utiliza `MERGE` para permitir la reejecución del proceso sin generar duplicados por clave primaria.

---

## 3. Generación de Estados de Cuenta Anuales

### Job

```text
estadosCuentaAnualesJob
```

Este Job posee tres Steps:

```text
estadosCuentaAnualesJob
        │
        ├── stepLimpiarMovimientos
        │
        ├── stepCuentas
        │
        └── stepResumenAnual
```

### Archivo de entrada

```text
cuentas_anuales.csv
```

### Step 1 - Limpieza de movimientos

```text
stepLimpiarMovimientos
```

Elimina la carga anterior de movimientos procesados antes de realizar una nueva importación completa del archivo anual.

Esto evita duplicar movimientos al reejecutar el Job.

### Step 2 - Procesamiento de movimientos

```text
stepCuentas
```

Lee los movimientos anuales, transforma los datos y aplica validaciones según el tipo de transacción.

Los tipos contemplados son:

```text
deposito
retiro
compra
```

Las reglas utilizadas son:

* Un depósito debe tener un monto mayor que cero.
* Un retiro debe representar un egreso mediante un monto negativo.
* Una compra debe representar un egreso mediante un monto negativo.
* Los tipos desconocidos son clasificados como anomalía.

Los movimientos procesados son almacenados en:

```text
MOVIMIENTOS_ANUALES_PROCESADOS
```

### Step 3 - Generación del resumen anual

```text
stepResumenAnual
```

Este Step agrupa los movimientos por:

```text
CUENTA_ID + ANIO
```

y genera información consolidada para auditoría.

Se calculan:

* Total de depósitos.
* Total de retiros.
* Total de compras.
* Cantidad total de movimientos.
* Saldo anual.
* Cantidad de anomalías.

Los resultados son almacenados en:

```text
ESTADOS_CUENTA_ANUALES
```

La generación del resumen utiliza una sentencia `MERGE`, permitiendo actualizar un estado anual existente cuando el Job vuelve a ejecutarse.

---

# Manejo de errores

Los Steps orientados a procesamiento mediante chunks utilizan tolerancia a fallos de Spring Batch.

Se configuraron:

```java
.faultTolerant()
.skip(...)
.skipLimit(10)
```

El sistema diferencia dos categorías de problemas.

## Anomalías de negocio

Son datos que pueden ser interpretados, pero incumplen las reglas definidas.

Ejemplos:

```text
Monto igual a cero
Monto negativo cuando no corresponde
Tipo de cuenta no soportado
Tipo de transacción no soportado
Registro posiblemente duplicado
```

Estos registros se conservan en Oracle utilizando:

```text
ESTADO = ANOMALIA
```

junto con el motivo almacenado en:

```text
DETALLE_VALIDACION
```

## Errores técnicos de datos

Errores como:

* Valores numéricos con formato inválido.
* Fechas con formato incorrecto.
* Líneas CSV que no pueden ser interpretadas.

pueden ser omitidos mediante las políticas de `skip` configuradas en los Steps.

El límite establecido es de:

```text
10 registros
```

Si se supera ese límite, el Step finaliza con error.

Los errores graves de infraestructura o persistencia no son ignorados.

---

# Persistencia en Oracle

La aplicación utiliza **Oracle Autonomous Database** como base de datos relacional.

Se creó un usuario específico para la aplicación:

```text
BANKXYZ_APP
```

La conexión se realiza mediante Oracle JDBC utilizando un **Oracle Wallet**.

Las principales tablas de negocio son:

```text
TRANSACCIONES_PROCESADAS
INTERESES_PROCESADOS
MOVIMIENTOS_ANUALES_PROCESADOS
ESTADOS_CUENTA_ANUALES
```

Spring Batch utiliza además sus propias tablas de metadata:

```text
BATCH_JOB_INSTANCE
BATCH_JOB_EXECUTION
BATCH_JOB_EXECUTION_PARAMS
BATCH_JOB_EXECUTION_CONTEXT
BATCH_STEP_EXECUTION
BATCH_STEP_EXECUTION_CONTEXT
```

Estas permiten registrar y consultar las ejecuciones de Jobs y Steps.

---

# Reejecución de Jobs

Los tres Jobs utilizan:

```java
RunIdIncrementer
```

Esto permite generar nuevas instancias de ejecución mediante el parámetro:

```text
run.id
```

Las tablas de transacciones e intereses utilizan sentencias Oracle `MERGE`, evitando duplicados al ejecutar nuevamente los procesos.

El Job anual limpia previamente los movimientos importados y luego vuelve a generar el resumen consolidado.

---

# Configuración de Oracle Wallet

Por seguridad, el Wallet de Oracle **no debe almacenarse dentro del repositorio Git**.

La ruta del Wallet se proporciona mediante una variable de entorno:

```text
ORACLE_WALLET_DIR
```

También se utiliza una variable de entorno para la contraseña del usuario de Oracle:

```text
BANKXYZ_DB_PASSWORD
```

Ejemplo en PowerShell:

```powershell
$env:ORACLE_WALLET_DIR="C:\ruta\al\Wallet_BANKXYZ"

$env:BANKXYZ_DB_PASSWORD="contraseña_del_usuario"
```

---

# Configuración de la aplicación

El archivo:

```text
src/main/resources/application.properties
```

utiliza una configuración similar a la siguiente:

```properties
spring.application.name=demo

# Oracle Autonomous Database
spring.datasource.url=jdbc:oracle:thin:@bankxyz_tp
spring.datasource.username=BANKXYZ_APP
spring.datasource.password=${BANKXYZ_DB_PASSWORD}
spring.datasource.driver-class-name=oracle.jdbc.OracleDriver

# Oracle Wallet
spring.datasource.hikari.data-source-properties[oracle.net.tns_admin]=${ORACLE_WALLET_DIR}

# Spring Batch
spring.batch.job.enabled=true
spring.batch.job.name=estadosCuentaAnualesJob
spring.batch.jdbc.initialize-schema=never
spring.batch.jdbc.isolation-level-for-create=READ_COMMITTED
```

El servicio Oracle utilizado es:

```text
bankxyz_tp
```

---

# Selección del Job

Para ejecutar un proceso específico debe modificarse:

```properties
spring.batch.job.name=
```

## Transacciones diarias

```properties
spring.batch.job.name=reporteTransaccionesDiariasJob
```

## Intereses mensuales

```properties
spring.batch.job.name=calculoInteresesMensualesJob
```

## Estados de cuenta anuales

```properties
spring.batch.job.name=estadosCuentaAnualesJob
```

---

# Requisitos para ejecutar el proyecto

Antes de iniciar la aplicación se requiere:

* Java 18.
* Maven o Maven Wrapper incluido en el proyecto.
* Acceso a Oracle Autonomous Database.
* Oracle Wallet descargado y descomprimido.
* Usuario `BANKXYZ_APP` habilitado.
* Variables de entorno configuradas.
* Tablas de negocio creadas en Oracle.
* Tablas de metadata de Spring Batch creadas.

---

# Compilación

Desde la raíz del proyecto:

```powershell
.\mvnw.cmd clean compile
```

Una compilación correcta debe finalizar con:

```text
BUILD SUCCESS
```

---

# Ejecución

Después de seleccionar el Job correspondiente en `application.properties`, ejecutar:

```powershell
.\mvnw.cmd spring-boot:run
```

Spring Batch ejecutará únicamente el Job seleccionado.

---

# Archivos de entrada

Los archivos CSV se encuentran en:

```text
src/main/resources/data/
```

y corresponden a:

```text
transacciones.csv
intereses.csv
cuentas_anuales.csv
```

Estos archivos representan la información proveniente del sistema legacy del Banco XYZ.

---

# Seguridad

El proyecto evita almacenar credenciales directamente en el código fuente.

Los siguientes datos deben mantenerse fuera del repositorio:

* Contraseña de Oracle.
* Oracle Wallet.
* Certificados y archivos criptográficos.
* Credenciales personales.

La configuración sensible se proporciona mediante variables de entorno.

---

# Resultados

La implementación permite:

* Procesar archivos CSV mediante Spring Batch.
* Ejecutar Jobs independientes para cada proceso solicitado.
* Transformar información legacy.
* Aplicar reglas de negocio.
* Detectar anomalías.
* Tolerar determinados errores de datos mediante políticas de `skip`.
* Calcular intereses y saldos.
* Generar estados de cuenta anuales consolidados.
* Persistir los resultados en Oracle Cloud.
* Registrar la metadata de Jobs y Steps.
* Reejecutar los procesos evitando duplicación de información.

---

# Evidencias de ejecución

Se generaron evidencias correspondientes a:

1. Ejecución del Job de transacciones diarias.
2. Resultado de transacciones procesadas en Oracle.
3. Ejecución del Job de intereses mensuales.
4. Resultado del cálculo de intereses en Oracle.
5. Ejecución del Job de estados de cuenta anuales.
6. Estados de cuenta anuales consolidados en Oracle.
7. Anomalía detectada durante el procesamiento de movimientos anuales.

---

## Autores

Proyecto desarrollado como actividad académica para la asignatura:

**Desarrollo Backend III - PBY2203**

**Duoc UC**
