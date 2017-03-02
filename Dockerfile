FROM java:8-alpine
MAINTAINER Your Name <you@example.com>

ADD target/uberjar/train-race.jar /train-race/app.jar

EXPOSE 3000

CMD ["java", "-jar", "/train-race/app.jar"]
