# Runtime stage - simple approach
FROM amazoncorretto:21

WORKDIR /app

# Copy pre-built jar (build locally before docker-compose up)
COPY build/libs/*.jar app.jar

# Expose port (optional, for health checks)
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
