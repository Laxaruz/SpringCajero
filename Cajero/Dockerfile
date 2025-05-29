# Usar OpenJDK 17 como imagen base
FROM openjdk:17-jdk-slim

# Información del mantenedor
LABEL maintainer="Laxaruz"
LABEL description="Aplicación Cajero Automático con Spring Boot"
LABEL version="0.0.1-SNAPSHOT"

# Crear directorio de trabajo
WORKDIR /app

# Instalar utilidades necesarias
RUN apt-get update && apt-get install -y \
    curl \
    && rm -rf /var/lib/apt/lists/*

# Copiar el wrapper de Maven y el archivo pom.xml primero (para optimizar cache de Docker)
COPY mvnw ./
COPY mvnw.cmd ./
COPY .mvn .mvn
COPY pom.xml ./

# Dar permisos de ejecución al wrapper de Maven
RUN chmod +x ./mvnw

# Descargar dependencias (esto se cachea si pom.xml no cambia)
RUN ./mvnw dependency:go-offline -B

# Copiar el código fuente
COPY src ./src

# Compilar la aplicación
RUN ./mvnw clean package -DskipTests

# Crear directorio para logs
RUN mkdir -p /app/logs

# Exponer el puerto que usa Spring Boot (por defecto 8080)
EXPOSE 8080

# Variables de entorno por defecto
ENV JAVA_OPTS="-Xmx512m -Xms256m"
ENV SPRING_PROFILES_ACTIVE=docker
ENV MYSQL_URL=jdbc:mysql://host.docker.internal:3306/atmdatabase
ENV MYSQL_USERNAME=root
ENV MYSQL_PASSWORD=

# Comando para ejecutar la aplicación
CMD ["java", "-jar", "/app/target/Cajero-0.0.1-SNAPSHOT.jar"]
