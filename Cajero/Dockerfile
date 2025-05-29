# Etapa de compilación
FROM maven:3.8-amazoncorretto-17 AS builder
WORKDIR /app

# Configurar codificación UTF-8
ENV MAVEN_OPTS="-Dfile.encoding=UTF-8 -Dproject.build.sourceEncoding=UTF-8"

# Copiar archivos de configuración Maven primero (para cache de capas)
COPY pom.xml .

# Descargar dependencias (esto se cachea si pom.xml no cambia)
RUN mvn dependency:go-offline -B

# Copiar código fuente
COPY src ./src

# Compilar la aplicación
RUN mvn package -DskipTests -Dfile.encoding=UTF-8

# Etapa de ejecución
FROM amazoncorretto:17-alpine-jdk
VOLUME /tmp
COPY --from=builder /app/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]