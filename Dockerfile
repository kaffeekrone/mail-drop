FROM docker.io/maven:3.9.11-eclipse-temurin-25
WORKDIR /app
COPY pom.xml /app/pom.xml
RUN mvn dependency:go-offline
COPY src /app/src
RUN mvn clean package

FROM docker.io/eclipse-temurin:25-jre
WORKDIR /app
COPY --from=0 /app/target/*.jar /app/mail-drop.jar

CMD ["java", "-jar", "mail-drop.jar"]
