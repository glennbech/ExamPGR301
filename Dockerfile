FROM maven:3.6-jdk-11 as bobthebuilder
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn package

FROM adoptopenjdk/openjdk11:alpine-slim
COPY --from=bobthebuilder /app/target/s3rekognition-0.0.1-SNAPSHOT.jar /app/application.jar
ENV AWS_ACCESS_KEY_ID=""
ENV AWS_SECRET_ACCESS_KEY=""
ENV BUCKET_NAME=""
ENTRYPOINT ["java","-jar","/app/application.jar"]