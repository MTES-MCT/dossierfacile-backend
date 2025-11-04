# API Watermark (dossierfacile-api-watermark)

## Description
REST API dedicated to the FiligraneFacile project.

## Main Features
- Add files that will be processed
- download the watermarked files

## Configuration
Create a file `application-dev.properties` in `dossierfacile-api-watermark/src/main/resources`

```properties
#Path to the folder when the files will be saved during dev
mock.storage.path=../mock-storage
# Port of this API
server.port=8091
# List of storage providers
storage.provider.list=LOCAL
# SQL
#URL of the database
spring.datasource.url=
#Username of the database
spring.datasource.username=
#Password of the database
spring.datasource.password=

#cron to clean the files (every day at midnight)
cron.process.cleanup=0 0 0 * * ?
# Number of days to keep the files watermarked
file.retention.days=1

# RABBIT
#RabbitMQ Configuration (Configuration example with docker compose)
rabbitmq.exchange.name=exchange.pdf.generator
rabbitmq.routing.key=routing.key.pdf.generator.watermark-generic
spring.rabbitmq.username=dev
spring.rabbitmq.password=password
spring.rabbitmq.host=127.0.0.1
```

# LogStash :

For the dev environment the appender Logstash is disabled by default.

# Database :
⚠️ The database is managed by the project dossierfacile-api-tenant.

# Run the application

```shell
    mvn spring-boot:run -D  spring-boot.run.profiles=dev,mockOvh
```