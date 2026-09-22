FROM docker.io/maven:3.9.16-eclipse-temurin-25@sha256:dd8e01b3be719853578c07b57ff8d9bbbbfe746f802226f05b19689420815221
WORKDIR /app
COPY pom.xml /app/pom.xml
RUN mvn dependency:go-offline
COPY src /app/src
RUN mvn clean package

FROM docker.io/eclipse-temurin:25-jre@sha256:bb036ed6cfdc57e3da7c22634d15f1b840d2caf76183861c80e81ca4b5104abb
WORKDIR /app
COPY --from=0 /app/target/*.jar /app/mail-drop.jar

CMD ["java", "-jar", "mail-drop.jar"]
