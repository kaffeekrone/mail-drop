FROM docker.io/maven:3.10.0-eclipse-temurin-25@sha256:a4734fd7909a9c2beddcacfa1229bd308a349f000bea1ec8db3287c8a16ca2a3 AS build
WORKDIR /app
COPY pom.xml /app/pom.xml
RUN mvn -B -ntp dependency:go-offline
COPY src /app/src
RUN mvn -B -ntp clean package

FROM gcr.io/distroless/java25-debian13:nonroot@sha256:9a868974dd82da359adf8c659d9f9a8d5249b138a733def74adbf00f64975db3
WORKDIR /app
COPY --from=build /app/target/*.jar /app/mail-drop.jar

CMD ["/app/mail-drop.jar"]
