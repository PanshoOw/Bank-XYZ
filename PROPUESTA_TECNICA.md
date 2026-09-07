# Propuesta Técnica - Implementación Backend for Frontend (BFF)

## 1. Contexto

El proyecto Bank XYZ corresponde a la continuidad del sistema desarrollado previamente para procesar y gestionar información bancaria utilizando Spring Boot, Spring Batch y Oracle Database.

Para la Semana 4 se incorpora el patrón arquitectónico Backend for Frontend (BFF), con el objetivo de adaptar la información y las operaciones según las necesidades de distintos tipos de clientes.

Los clientes considerados son:

- Aplicación Web
- Aplicación Móvil
- Cajero Automático (ATM)

## 2. Estrategia seleccionada

Se seleccionó la estrategia de implementar un backend específico para cada tipo de cliente.

La solución está compuesta por:

- Backend principal Bank XYZ
- BFF Web
- BFF Mobile
- BFF ATM

Cada BFF se implementa como una aplicación Spring Boot independiente y se comunica con el backend principal mediante APIs REST.

La guía de aprendizaje plantea esta estrategia como una forma de separar la lógica específica de cada cliente y permitir que cada frontend reciba únicamente la información que necesita.

## 3. Arquitectura propuesta

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