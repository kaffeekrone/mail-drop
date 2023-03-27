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

A mail-json:

Complete example:

```json
{
  "mail": {
    "subject": "huhu",
    "htmlContent": "<html><body>A<br><img src=\"cid:fancyCid\"/></a><br>B</body></html>",
    "plainTextContent": "content🙉",
    "attachments": [
      {
        "base64Content": "iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAIAAACQkWg2AAADG0lEQVR4XgEQA+/8AHjqPA3/o0wvZ0L1eOAmcvcFRf/qMCbQa/j6yFUzQIezl/HOPFYYJI9imYFYziAsywDIwypVb1lHD062xPXlKXhBA9zU+sZ8yuTVv/lY8b8W/mxdd0vZJMTjvQtIVgViDhIAEgoAJM4CxWxPx9Pr/6Rec70g2X0nrlhaQBWdKU7BDXgF8r6jEWmZ8Yj/cqtGkk0pAACRZ/h8L2mvuz37umjjBXEpm2YfD3pYRx2iC/0F3DOOcuxVieQG9G+vZi5EEjV6FAAaTm1sR8EFQevxAFUfUzdo4vFrBlFYSgD/Jw9/r4VSoN3uSWfULiatOdwbexTjQEQAZ0Oq4k4MRnRlfHRokWfXdhEAHrv3KDrez2UWcq084u62rev3ow2qPirS7QNA7kXGAB9raCrsBVluC9WihEF/AlFVEp4UbNejaHoEVF0PsK992JC7aP61S6VzshnPAwcWYwBKmc4pn2OUDjwCdEFw1TruqQCcGqcGX3mQf+sve+yC/7JiSYgj/0711gx3+mfpD9AAbmcEhfLMHNQBu19sw1+QK05XBvRQtIdo9AoU6xl8yL1v9CbsinqXgc258a4+AkLSADIcXUZi+5DMMRNGLO68yHeyiKrL6uZfMH95R052lTx+HsTDFo1N1nxzBygNRBZhZgALlIKHR+qwcZwfZitbGYR5WQnks/lMPZqqKGV2bzyMbdmqYaEPJamqb98ZGU7toOgA4ieTFb72BZszoTu70RprtcN/PpYr7nNhNubaixhzBQbwfPNkJRjI/9buyZxu96Y/ALmRVB+MCodfLK9qBB3BUD1X6CIYf840YNTYy2umWTb4DbYJnb+OLU4Zy+wQHPVtAQBZ3lfWDclIAjBXqSW5V1xOScB5gnCWfbTMZz5IVFydEV8muBpMKzns6SKNhl6iMJwA9EscHRaxG9Z+TaRhd1Y1AIIrWKgZO/7mpiI9XExVTCTBj4BdZ7bgXSDatI/wWE+AANI1QiziRToqF4/qRyNLJOqHHqbTYqoCFNKrAoiQvJDz4VQ2yHsBzfaqPhMlbL43Q9KPZhZplHsFAAAAAElFTkSuQmCC",
        "mimeType": "image/png",
        "name": "imageFileName.png",
        "contentDisposition": "INLINE",
        "contentId": "fancyCid"
      },
      {
        "base64Content": "iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAIAAACQkWg2AAADG0lEQVR4XgEQA+/8APX7qjfzOPIr4Q7R1jLFQs1kg8anIWpUG0+bHgDhx54Wah/rxG7QjYtsI6eRcmlO0ADD650cAo9H1NRYX6hlOZo3kmU8DEhJLbhUbKDyp5c14SuJ1uhIwrcSLhJlU8+J1oQAryj8BeLQntAgPDMac+pXMd8WWRQLAxS4+GRBLtteSU89wjgf2TkRT35v4w2ojtljAN2QXhFpI2VjVHVtdbHT5uRXVt/pCefNG9p1hM/riKzpMTtI1mRCu3mfpflmndIPNADPp0EAWoK8VYlRlBC6jCI/K4CqrW+CPz3ILujjpuG1ri9KGB9GfFXjaURz9/c/xrMAC30s10nXGNWEWn+CFTZ8pJirW2cpSFoAZ5wHDyHA5Psm1n/anAIqpjs2sDDL+smwAMGY2IiGLvNm5f0/v+JgIYvT2P3rAjf/8N41xT2iLpNw8fsB3u9SU0XX3E5BJQlMQAB12ludBtZsxE0xBUp77yMt7BSrwM6G7HF5wnNAhQuV3362tSDYM+VYVj3UAi75z90AqGtN3UiL9s6BIQlWIgmhLLmoPweXRZ/gV0T5fyY54y33iy/cqqAUNQcpVdpWiwqTAH6e8fc+ovtqmtR08aX38Dm+OfJpvQ1DwQrb/5jJx0M/6yxovhqvXZNaZh2peVfSJQBm3F0mNSmFcybUSCwKFr+/E/UK9iOhlOJJsRQOfhDu3XMkpATmea83zNRVrg13BDkAw4qi3L8W6ZxU1EZBNLRFhk+5fxZXnMaIjuLV7RDtPJ9dfFyugAGSmsaWrXFBL2p8AJbz82mZbGlbHF3ZvokEZWBuOqRpuRR0ksdhFnPp1UjSVGQlo7AdTZOPPQWoLZSkywAfLtCkkWPkzmVy/Kqh+tfStyrOt6FGfqb25Au6+AybYgXdmtVDXxL5LeoTIvczEVsAjggqknORd6ctOSCx5z1Ps6pn/dGKO/xU3sdtW57E0b9LXT9wr/sjUlB8Cq35u7flAKTojw3uESgQs6QaRkMJp+HiGoV8O3EcQ6z4pxyQSkTIU3pw/bqu/Hk5tEYA7aQpxbgxfcjbtInaAAAAAElFTkSuQmCC",
        "mimeType": "image/png",
        "name": "additionalAttachmentFileName.png",
        "contentDisposition": "ATTACHMENT"
      }
    ]
  },
  "recipients": [
    "fun@bla.de"
  ],
  "from": "defaultfromaddress@fancydomain.de",
  "cc": [],
  "bcc": [],
  "replyTo" : []
}
```
Some notes about required fields and combinations 

- subject is mandatory
- you either have to supply htmlContent or plainTextContent or both
- all empty arrays can be omitted
- the from address can be omitted if you configured a default from address
- attachments can be omitted
- attachments need a `mimeType` a `name` and `base64Content`
- `replyTo` can be omitted
  
When supplying an inline attachment `"contentDisposition": "INLINE"` 
- you need a html part as well
- have a contentId
- reference it in the html part, but there is no validation for that!

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
    <version>XXX</version>
</dependency>
```

### Docker

be aware that docker commands are always environment dependent, especially networking, so take this as a rough example
```
# setup rabbitmq & mailcatcher as the receiving end
docker run -d -p 5672:5672 -p 15672:15672 --name rabbitmq docker.io/rabbitmq:3.8.9-management
docker run -d --name mailcatcher -p 1080:1080 -p 1025:1025 docker.io/schickling/mailcatcher
RABBITMQ_IP=$(docker inspect -f '{{range.NetworkSettings.Networks}}{{.IPAddress}}{{end}}' rabbitmq)
SMTP_HOST=$(docker inspect -f '{{range.NetworkSettings.Networks}}{{.IPAddress}}{{end}}' mailcatcher)
# start mail-drop
docker run --rm -e SPRING_MAIL_HOST=$SMTP_HOST -e SPRING_RABBITMQ_HOST=$RABBITMQ_IP ghcr.io/kaffeekrone/mail-drop:main
# send test mail
docker exec rabbitmq rabbitmqadmin publish exchange='mail-drop' routing_key='mail-drop' payload='{"mail":{"subject":"huhu","plainTextContent":"content🙉"},"recipients":["fun@bla.de"],"from":"defaultfromaddress@fancydomain.de"}'
```
