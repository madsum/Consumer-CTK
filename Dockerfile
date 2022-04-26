FROM openjdk:17-alpine
RUN mkdir -p /ctk
COPY target/dcsa_ctk_consumer-*.jar /ctk/dcsa_ctk_consumer.jar
WORKDIR /ctk/
ENTRYPOINT java -jar dcsa_ctk_consumer.jar