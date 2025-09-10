# Stage 1: Build Stage
FROM bellsoft/liberica-runtime-container:jdk-24-stream-musl AS builder

WORKDIR /home/app
ADD . /home/app/spring-boot-3-redis-cache
RUN cd spring-boot-3-redis-cache && chmod +x mvnw && ./mvnw -Dmaven.test.skip=true clean package

# Stage 2: Layer Tool Stage
FROM bellsoft/liberica-runtime-container:jdk-24-cds-slim-musl AS optimizer

WORKDIR /home/app
COPY --from=builder /home/app/spring-boot-3-redis-cache/target/*.jar spring-boot-3-redis-cache.jar
RUN java -Djarmode=tools -jar spring-boot-3-redis-cache.jar extract --layers --launcher

# Stage 3: Final Stage
FROM bellsoft/liberica-runtime-container:jre-24-stream-musl

ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
EXPOSE 8080
COPY --from=optimizer /home/app/spring-boot-3-redis-cache/dependencies/ ./
COPY --from=optimizer /home/app/spring-boot-3-redis-cache/spring-boot-loader/ ./
COPY --from=optimizer /home/app/spring-boot-3-redis-cache/snapshot-dependencies/ ./
COPY --from=optimizer /home/app/spring-boot-3-redis-cache/application/ ./