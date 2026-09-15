# Propuesta Técnica
## Implementación Backend for Frontend - Bank XYZ

### 1. Contexto

El proyecto Bank XYZ requiere atender tres tipos de clientes con necesidades diferentes:

- Aplicación Web.
- Aplicación Móvil.
- Cajero Automático (ATM).

Una única respuesta genérica para los tres canales provocaría transferencia innecesaria de información y aumentaría el acoplamiento entre los clientes y el backend.

Por esta razón se implementa el patrón arquitectónico **Backend for Frontend (BFF)**.

---

## 2. Estrategia seleccionada

La estrategia seleccionada consiste en implementar un **BFF independiente para cada tipo de cliente**.

La arquitectura queda compuesta por:

```text
                       Oracle Database
                              │
                              ▼
                     Backend principal
                    http://localhost:8080
                              │
              ┌───────────────┼───────────────┐
              │               │               │
              ▼               ▼               ▼
          BFF Web         BFF Mobile        BFF ATM
      https://:8081     https://:8082     https://:8083
              │               │               │
              ▼               ▼               ▼
             Web             Móvil           Cajero
```

Cada BFF es una aplicación Spring Boot independiente, con su propia configuración, endpoints, DTO, lógica de transformación y configuración de seguridad.

El acceso a Oracle permanece centralizado en el backend principal.

---

## 3. Justificación

Se seleccionó esta estrategia porque cada canal posee requerimientos diferentes.

### Web

El cliente Web dispone de una interfaz con mayor capacidad para mostrar información detallada.

Su BFF entrega:

- Identificación de la cuenta.
- Datos del cliente.
- Tipo de cuenta.
- Saldo inicial.
- Tasa de interés.
- Interés calculado.
- Saldo actual.
- Estado.

### Mobile

El cliente Mobile busca reducir el volumen de información transferida.

Su respuesta contiene solamente:

- ID de cuenta.
- Nombre.
- Tipo de cuenta.
- Saldo actual.
- Estado.

Esto permite disminuir el tamaño de la respuesta y evitar enviar información que la interfaz móvil no necesita.

### ATM

El cajero automático requiere una interfaz reducida y orientada a operaciones concretas.

El BFF ATM expone principalmente:

- Consulta de saldo.
- Retiro de dinero.

De esta manera no se entrega información adicional innecesaria para la operación de un cajero.

---

## 4. Optimización por canal

Las respuestas fueron diseñadas específicamente para las necesidades de cada cliente.

Durante una prueba utilizando la cuenta `101` se obtuvieron los siguientes tamaños:

| Canal | Tamaño |
|---|---:|
| Web | 183 bytes |
| Mobile | 99 bytes |
| ATM | 57 bytes |

Respecto de Web:

- Mobile transfirió aproximadamente un **45,9 % menos información**.
- ATM transfirió aproximadamente un **68,9 % menos información**.

También se obtuvieron los siguientes tiempos durante una ejecución de prueba:

| Canal | Tiempo |
|---|---:|
| Web | 0.381755 s |
| Mobile | 0.111764 s |
| ATM | 0.093577 s |

Los tiempos pueden variar según cada ejecución, por lo que se consideran solamente como referencia.

La principal evidencia de optimización corresponde a la reducción del tamaño de las respuestas mediante DTO específicos por canal.

---

## 5. Seguridad

Cada BFF posee una configuración de seguridad independiente.

La solución implementa:

- HTTPS.
- Certificados SSL/TLS.
- Keystores PKCS12 independientes.
- Autenticación mediante credenciales.
- Tokens JWT.
- Firma HMAC SHA-256.
- Autorización específica por canal.
- APIs sin estado mediante `STATELESS`.
- Variables de entorno para contraseñas y secretos.

Los permisos se diferencian mediante scopes:

| Canal | Scope |
|---|---|
| Web | `WEB` |
| Mobile | `MOBILE` |
| ATM | `ATM` |

Estos scopes se validan como:

```text
SCOPE_WEB
SCOPE_MOBILE
SCOPE_ATM
```

Cada BFF expone:

```text
POST /auth/token
```

para generar un JWT después de validar las credenciales.

Posteriormente las solicitudes protegidas deben incluir:

```text
Authorization: Bearer TOKEN_JWT
```

Una solicitud sin token o con un token inválido es rechazada con:

```text
HTTP 401 Unauthorized
```

Los certificados utilizados durante el desarrollo son autofirmados y destinados exclusivamente al entorno local.

---

## 6. Modularidad y escalabilidad

La separación de los tres BFF permite modificar un canal sin alterar directamente los otros.

Cada aplicación mantiene una estructura organizada mediante:

```text
config/
controller/
dto/
service/
```

Esto permite:

- incorporar nuevos endpoints;
- modificar respuestas de un canal;
- agregar nuevas reglas de autorización;
- evolucionar cada BFF independientemente;
- incorporar nuevos clientes en el futuro.

Por ejemplo, un nuevo canal podría agregarse mediante un nuevo BFF sin modificar las respuestas existentes de Web, Mobile o ATM.

---

## 7. Ventajas de la propuesta

La estrategia seleccionada entrega las siguientes ventajas:

- Respuestas específicas para cada frontend.
- Reducción de información innecesaria.
- Separación de responsabilidades.
- Seguridad diferenciada por canal.
- Mejor mantenibilidad.
- Escalabilidad independiente.
- Menor acoplamiento entre los clientes y el backend principal.

Como desventaja, mantener tres aplicaciones independientes aumenta la cantidad de configuraciones y componentes que deben administrarse.

Sin embargo, para este proyecto la separación resulta adecuada debido a las diferencias existentes entre Web, Mobile y ATM.

---

## 8. Conclusión

La propuesta implementa el patrón Backend for Frontend mediante tres backends independientes orientados a Web, Mobile y ATM.

Cada BFF entrega información optimizada para su cliente y protege sus endpoints mediante HTTPS, certificados SSL/TLS, autenticación, autorización y tokens JWT.

La solución conserva el acceso a datos en el backend principal y separa la lógica específica de cada frontend, obteniendo una arquitectura modular, segura y preparada para futuras extensiones.