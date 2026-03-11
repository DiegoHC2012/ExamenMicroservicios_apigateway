# ExamenMicroservicios — API Gateway

API Gateway basado en **Spring Cloud Gateway** que enruta al resto de microservicios mediante Eureka, junto con el `docker-compose.yml` que levanta todo el sistema.

| Detalle | Valor |
|---|---|
| Java | 21 |
| Spring Boot | 3.3.5 |
| Spring Cloud | 2024.0.0 |
| Puerto | 8080 |

---

## Estructura de carpetas esperada

```
ExamenMicroServicios/
├── ExamenMicroservicios_apigateway/        ← este repo (contiene docker-compose.yml)
├── ExamenMicroservicios_eureka-server/
├── ExamenMicroservicios_productos-service/
├── ExamenMicroservicios_ordenes-service/
└── ExamenMicroservicios_pagos-service/
```

---

## Estructura del proyecto

```
ExamenMicroservicios_apigateway/
├── src/
│   └── main/
│       ├── java/com/examen/apigateway/
│       │   └── ApigatewayApplication.java
│       └── resources/
│           └── application.yml
├── Dockerfile
├── docker-compose.yml
└── pom.xml
```

---

## Rutas configuradas

| Path | Destino |
|---|---|
| `/productos/**` | `lb://productos-service` |
| `/ordenes/**` | `lb://ordenes-service` |
| `/pagos/**` | `lb://pagos-service` |

---

## Build individual de cada servicio

Ejecutar en cada directorio antes de levantar con Docker Compose si se quiere pre-compilar:

```bash
cd ../ExamenMicroservicios_eureka-server   && mvn -DskipTests clean package
cd ../ExamenMicroservicios_productos-service && mvn -DskipTests clean package
cd ../ExamenMicroservicios_ordenes-service   && mvn -DskipTests clean package
cd ../ExamenMicroservicios_pagos-service     && mvn -DskipTests clean package
cd ../ExamenMicroservicios_apigateway        && mvn -DskipTests clean package
```

---

## Levantar todo el sistema

Desde la raíz de este repo:

```bash
docker compose up --build
```

Para levantar en segundo plano:

```bash
docker compose up --build -d
```

Para detener y limpiar:

```bash
docker compose down -v
```

---

## Verificación

### Eureka Dashboard
```
http://localhost:8761
```

### Ping vía API Gateway

```bash
# productos-service
curl http://localhost:8080/productos/ping

# ordenes-service
curl http://localhost:8080/ordenes/ping

# pagos-service
curl http://localhost:8080/pagos/ping
```

### Ping directo a cada servicio

```bash
curl http://localhost:8081/productos/ping
curl http://localhost:8082/ordenes/ping
curl http://localhost:8083/pagos/ping
```

---

## Actuator

| Endpoint | URL |
|---|---|
| Health gateway | http://localhost:8080/actuator/health |
| Rutas gateway | http://localhost:8080/actuator/gateway/routes |

---

## Servicios Docker Compose

| Servicio | Puerto | Descripción |
|---|---|---|
| `localstack` | 4566 | CloudWatch Logs local (crea log-groups) |
| `init-log-groups` | — | Crea los 3 log-groups en LocalStack |
| `mongodb` | 27017 | Base de datos MongoDB |
| `eureka-server` | 8761 | Servidor de descubrimiento |
| `apigateway` | 8080 | API Gateway (este servicio) |
| `productos-service` | 8081 | CRUD productos |
| `ordenes-service` | 8082 | CRUD ordenes |
| `pagos-service` | 8083 | CRUD pagos |
