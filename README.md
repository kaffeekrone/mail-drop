# Mail Drop

A little service that queues your mails via rabbitmq and sends them to a configured smtp server.

- smtp settings are fully configurable
- configurable rabbitmq entities
- retry logic within rabbitmq
- callback queue
- easily scalable
- build complex mail messages with inline attachments and all that stuff
 

## In detail

There are 3 main parts in here "queue", "retry-queue" and "callback-queue".
A consumer listening on "queue", 
if a mail can be sent successfully a message will be written to the callback-queue with `{"success" : true}`.
If the message could not be sent it will be put into the retry-queue. 
The retry-queue is special, it has a `dead-letter-exchange` which is this original queue again(!) and a message ttl configured. 
When the ttl is reached it gets send to the configured dead letter exchange -> back to the original queue


## Build

Just build it with maven

```
mvn clean install
```

## Howto

### Maven

```xml
<dependency>
    <groupId>de.kaffeekrone</groupId>
    <artifactId>mail-drop</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

### Docker

```
docker run -d -p 5672:5672 -p 15672:15672 --name rabbitmq rabbitmq:3.8.9-management
docker run -d --name mailcatcher -p 1080:1080 -p 1025:1025 schickling/mailcatcher
RABBITMQ_IP=$(docker inspect -f '{{range.NetworkSettings.Networks}}{{.IPAddress}}{{end}}' rabbitmq)
SMTP_HOST=$(docker inspect -f '{{range.NetworkSettings.Networks}}{{.IPAddress}}{{end}}' mailcatcher)
docker run --rm -e SPRING_MAIL_HOST=$SMTP_HOST -e SPRING_RABBITMQ_HOST=$RABBITMQ_IP ghcr.io/kaffeekrone/mail-drop:main
```
