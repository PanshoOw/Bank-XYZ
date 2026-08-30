# Propuesta Técnica - Bank XYZ

## 1. Objetivo

La solución moderniza tres procesos batch legacy del Banco XYZ utilizando Spring Batch:

- Reporte de transacciones diarias.
- Cálculo de intereses mensuales.
- Generación de estados de cuenta anuales.

El diseño busca mantener la consistencia de los datos, controlar registros incorrectos y mejorar el rendimiento mediante procesamiento paralelo.

## 2. Arquitectura propuesta

Cada proceso fue implementado como un Job independiente de Spring Batch y utiliza el flujo:

```text
Reader → Processor → Writer
```

Los archivos CSV son divididos en particiones para permitir su procesamiento concurrente.

La configuración principal utilizada es:

```text
Chunk size: 5
Grid size: 5
Pool size: 3
```

Los cinco segmentos de datos son distribuidos entre tres workers mediante `TaskExecutorPartitionHandler`.

## 3. Procesamiento y validación

Los `ItemProcessor` realizan las transformaciones y validaciones necesarias antes de persistir la información.

Se controlan, entre otros casos:

- Campos obligatorios faltantes.
- Fechas con formatos inválidos.
- Montos inválidos.
- Tipos de datos no reconocidos.
- Valores que deben clasificarse como anomalías.

Los datos válidos y las anomalías que pueden ser procesadas son almacenados en Oracle, mientras que los registros que no pueden continuar son omitidos mediante una política controlada.

## 4. Tolerancia a fallos

Los Steps utilizan configuración `faultTolerant`.

La política personalizada `BankSkipPolicy` permite omitir errores de datos conocidos sin detener completamente el Job.

Además, los `SkipListener` registran los elementos descartados y su causa.

Para errores transitorios de acceso a datos se configuró una política de reintento utilizando:

```java
.retry(TransientDataAccessException.class)
.retryLimit(2)
```

En el Job de intereses mensuales también se contempla `DuplicateKeyException`, debido a una condición de concurrencia detectada durante escrituras paralelas sobre una misma clave.

Los errores correspondientes a datos de negocio son gestionados mediante la política de omisión y no mediante reintentos.

## 5. Escalamiento y paralelismo

Se seleccionó partitioning como estrategia de escalamiento.

Para determinar la cantidad adecuada de threads se ejecutó el Job de transacciones diarias con el mismo volumen de 1000 registros, manteniendo constantes el `chunk size` y el número de particiones.

| Threads | Tiempo particiones | Tiempo total Job |
|---:|---:|---:|
| 1 | 6,977 s | 7,251 s |
| 2 | 4,360 s | 4,593 s |
| 3 | 3,109 s | 3,401 s |

La configuración de tres threads obtuvo el menor tiempo de ejecución.

Respecto a la ejecución con un thread, el tiempo total se redujo aproximadamente un 53 %.

Por este motivo se seleccionó como configuración final:

```properties
app.poolSize=3
```

Esta configuración también permite observar la ejecución concurrente mediante:

```text
batch-worker-1
batch-worker-2
batch-worker-3
```

## 6. Resultados obtenidos

### Transacciones diarias

```text
Registros de entrada: 1000
Registros procesados: 785
Registros omitidos: 215
Registros válidos: 387
Registros con anomalías: 398
```

### Intereses mensuales

```text
Registros de entrada: 1000
Registros omitidos: 363
Cuentas consolidadas: 50
Errores detectados en tasas: 0
Errores detectados en intereses: 0
Errores detectados en saldos finales: 0
```

### Estados de cuenta anuales

```text
Registros de entrada: 1000
Registros procesados: 952
Registros omitidos: 48
Registros válidos: 329
Registros con anomalías: 623
Estados generados: 20
```

## 7. Conclusión

La propuesta permite ejecutar los tres procesos requeridos mediante una arquitectura Spring Batch modular y tolerante a fallos.

El uso de particiones y tres threads permitió mejorar el rendimiento manteniendo los resultados esperados. Además, la combinación de validaciones, políticas de omisión, listeners y reintentos permite continuar el procesamiento frente a registros incorrectos o fallos recuperables sin comprometer la ejecución completa del Job.