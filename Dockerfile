FROM docker.io/maven:3.8.1-jdk-11
WORKDIR /app
COPY pom.xml /app/pom.xml
RUN mvn dependency:go-offline
COPY src /app/src
RUN mvn clean package

FROM docker.io/adoptopenjdk/openjdk11:alpine-jre
WORKDIR /app
COPY --from=0 /app/target/*.jar /app/mail-drop.jar

CMD ["java", "-jar", "mail-drop.jar"]
