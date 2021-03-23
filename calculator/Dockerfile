FROM java:openjdk-8-jre-alpine


RUN mkdir App
WORKDIR App
COPY ./target/calculator-1.0-SNAPSHOT.jar .



CMD ["java", \
     "-Dcom.sun.management.jmxremote", \
     "-Dcom.sun.management.jmxremote.port=9010", \
     "-Dcom.sun.management.jmxremote.rmi.port=9010", \
     "-Dcom.sun.management.jmxremote.authenticate=false", \
     "-Dcom.sun.management.jmxremote.ssl=false", \
     "-Djava.rmi.server.hostname=EPBGSOFL0071.budapest.epam.com", \
     "-jar", \
     "calculator-1.0-SNAPSHOT.jar"]

EXPOSE 9010