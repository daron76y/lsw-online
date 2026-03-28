FROM maven:3.9.6-eclipse-temurin-21
WORKDIR /workspace

COPY pom.xml .
COPY lsw-core/pom.xml lsw-core/pom.xml
COPY lsw-user-service/pom.xml lsw-user-service/pom.xml
COPY lsw-party-service/pom.xml lsw-party-service/pom.xml
COPY lsw-campaign-service/pom.xml lsw-campaign-service/pom.xml
COPY lsw-battle-service/pom.xml lsw-battle-service/pom.xml
COPY lsw-pvp-service/pom.xml lsw-pvp-service/pom.xml
COPY lsw-gateway/pom.xml lsw-gateway/pom.xml
COPY lsw-client/pom.xml lsw-client/pom.xml

RUN mvn dependency:go-offline -pl lsw-gateway -am -q

COPY lsw-gateway/src lsw-gateway/src
RUN mvn package -pl lsw-gateway -DskipTests -q

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "lsw-gateway/target/lsw-gateway-1.0.0.jar"]