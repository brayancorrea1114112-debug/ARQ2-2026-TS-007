# Historial de la sesión - StreamHub VOD (ARQ2-2026-TS-007)

Resumen para continuar el trabajo en otro equipo. No contiene claves. No subir este archivo a Git: el proyecto debe estar en inglés.

## Proyecto
- Software Architecture II, Team 007, Caso 7: StreamHub VOD Catalog and Recommendations.
- Repo: https://github.com/brayancorrea1114112-debug/ARQ2-2026-TS-007
- Wave 2: implementar lo diseñado (C4, ADRs y `docs/spec.md`). Si cambia una decisión, se documenta en un ADR y en el spec.
- En la revisión el profesor pregunta por qué se tomó cada decisión: qué pasa si cae el broker o la caché, cómo se garantiza orden e idempotencia, y qué trade-offs se aceptaron.

## Cómo trabajar con Claude en este proyecto
- Responder en español, profesional y directo. No asumir: preguntar si algo es ambiguo.
- **No hacer commits, push ni cambiar el historial.** Los commits los hace el usuario.
- **Docker lo ejecuta el usuario.** Claude no levanta ni reconstruye contenedores: escribe los archivos y da los comandos. Consultas de solo lectura y peticiones de prueba sí.
- No editar `docker-compose.yml` sin que el usuario lo pida.
- No leer ni mostrar archivos `.env` ni escribir claves en el código. El usuario pidió crear los `.env` de esta sesión como excepción.
- Los comandos con `docker` usan el nombre completo del contenedor, con sufijo `-arquitectura2`. `docker compose` usa el nombre del servicio del yml (por ejemplo `kong`).

## Arquitectura implementada
| Pieza | Tecnología | Puerto |
|---|---|---|
| Kong (gateway, JWT, rate limit, CORS, trazas) | Kong 3.7 sin base de datos, `kong/kong.yml` | 8000 y 8001 |
| auth-service | Spring Boot 4.1.2, PostgreSQL | 8081 |
| catalog-service (Home) | Spring Boot 4.1.2, Redis, MongoDB, Cassandra | 8083 |
| telemetry-service (pings y likes) | Spring Boot 4.1.2, RabbitMQ | 8084 |
| recommendation-worker | Spring Boot 3.3.4, RabbitMQ, Cassandra | 8082 |
| Observabilidad | OTel Collector, Jaeger, Prometheus, Loki, Alloy, Grafana | 16686, 9090, 3100, 3000 |
| Frontend | Next.js en `frontend/`, corre fuera de Docker (Node 22) | 3001 |

Decisiones clave:
- Hexagonal en cada servicio (ADR 004).
- JWT HS256 con secreto compartido entre auth, catalog, telemetry y Kong.
- ADR 007: los workers solo escriben en Cassandra. El Home se reconstruye en Redis por Cache-Aside con TTL de 15 min ± 2 min.
- Cuentas de prueba: `ana@example.com` (perfiles `prof_9921` y `prof_9922`) y `luis@example.com` (`prof_9931`).

## Estado actual
- Último commit en GitHub revisado: `51a0d7a`.
- **Los cambios de esta sesión están sin commit** en el equipo original. Hay que subirlos antes de cambiar de equipo o se pierden en el traslado.
- El flujo completo ya se probó con éxito por Kong, antes de estas últimas correcciones: login, perfil, ping, like, Home, Cassandra, RabbitMQ, trazas en Jaeger y logs en Loki.
- Última prueba de código: compila, 103 tests pasan y 4 se saltan (requieren Docker).
- **Las últimas correcciones no se han probado con contenedores.** La reconstrucción se interrumpió por saturación del equipo (7,3 GB de RAM).

## Correcciones hechas (sin commit)
- Métricas en auth y telemetry, plugin `prometheus` en Kong, telemetry añadido a Prometheus y al dashboard.
- Títulos en memoria 60 s en catalog y spans de consultas a Mongo.
- `correlationId` en logs del worker y reintentos alineados con el spec (3 intentos, esperas de 1 s y 5 s).
- Auth responde 503 si cae Postgres y llave foránea `profiles → accounts`.
- Sin claves por defecto de RabbitMQ en el código.
- `docker-compose.yml` usa `env_file`, con un `.env` por servicio.
- `.env.example` por servicio, README, ADR 005 y spec actualizados. ADR del frontend renombrado a 008.

## Qué falta
1. **Decidir las recomendaciones.** Hoy solo sugieren títulos con Like que no se han empezado a ver. Opciones:
   - A (recomendada): catalog expone los géneros y el worker los consulta, con caché en memoria y un ADR nuevo.
   - B: el worker lee directo el Mongo del catálogo. Más rápido, pero rompe "cada servicio con su base".
   - C: dejarlo y documentarlo como limitación.
2. Reconstruir contenedores y repetir la prueba completa. Confirmar métricas (auth, telemetry, Kong) y la traza del Home con Kong y Mongo.
3. Medir de nuevo la latencia del Home (objetivo menor a 20 ms; antes de los cambios, 29-35 ms) y la de las páginas de Next con el equipo sin carga.
4. Corregir el C4 (muestra 2 workers, el código tiene 1) y exportar el PNG desde draw.io. Esperar a la decisión del punto 1.
5. Actualizar el Tech Spec en Google Docs: ADR 007 y 008, C4 nuevo, Kong y frontend.
6. Revisar que los integrantes tengan commits en Wave 2: Juan Jose Rua David solo tiene merges, y Juan José Alcaraz Guisao no aparece con ese nombre.
7. Avisar al equipo que cada uno debe crear sus `.env` desde los `.env.example`.
8. Revisar `frontend/package-lock.json`, que tiene un cambio local que no vino de esta sesión.

Sin hacer, por no ser obligatorio: frontend en contenedor, IP fija `172.27.16.1` en Kong y Next.

## Pasar al equipo corporativo
1. En el equipo original: revisar `git status`, hacer commit y push de los cambios.
2. Antes de instalar, confirmar con la empresa que se permite Docker. Docker Desktop puede requerir licencia en empresas grandes; Docker Engine dentro de WSL evita ese punto. Un proxy o VPN puede bloquear descargas.
3. Clonar en una ruta corta, por ejemplo `C:\dev\ARQ2-2026-TS-007`, fuera de carpetas sincronizadas.
4. Copiar por un medio seguro los 8 `.env`, que no van en Git: `services/*/.env` e `infra/{postgres,mongo,rabbitmq,grafana}/.env`.
5. Configurar `C:\Users\<usuario>\.wslconfig` con `memory=12GB` y ejecutar `wsl --shutdown`.
6. Instalar Node 22 para el frontend.
7. Dentro de la carpeta del repo en WSL:

```bash
docker compose up -d --build
```

Deben quedar 16 contenedores en `Up`, y `mongo-seed` y `cassandra-init` en `Exited (0)`. Las cuentas, el catálogo y las tablas se crean solos.

## Avisos para la demo
- "Continuar viendo" puede tardar hasta unos 17 minutos si el Home ya estaba en caché. Enviar el ping antes de abrir el Home.
- El primer Home tras arrancar tarda más porque abre la conexión con Cassandra. Hacer una petición de calentamiento antes.
