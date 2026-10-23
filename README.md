# Мониторинг Spring Boot

Приложение публикует метрики через Actuator, Prometheus собирает их, а Grafana загружает источник данных, дашборд и алерты из файлов при старте.

Нужны JDK 21, Maven и Docker Engine. Запуск стека:

```bash
mvn clean package -DskipTests
docker compose up --build -d
```

Панели доступны в Grafana на `http://localhost:3000` с учётными данными `admin` / `admin`. Prometheus доступен на `http://localhost:9090`, метрики приложения — на `http://localhost:8080/actuator/prometheus`.

Создайте разные ответы для метрик:

```bash
curl http://localhost:8080/api/success
curl http://localhost:8080/api/not-found
curl http://localhost:8080/api/error
curl -X POST "http://localhost:8080/api/memory?megabytes=32"
curl -X DELETE http://localhost:8080/api/memory
```

Алерт срабатывает при недоступности приложения или превышении 70% heap на одну минуту. В Docker Compose заданы временные учётные данные Grafana для локальной учебной среды.

