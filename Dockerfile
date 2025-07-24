# Этап сборки
FROM gradle:7.6-jdk19 AS build
COPY . .
ARG RELEASE_VERSION=5.0.3
RUN gradle -Pversion=docker -i -s --no-daemon bootJar

# Этап выполнения (production)
FROM eclipse-temurin:19-jre-jammy AS production

# Копируем банковские сертификаты в образ
COPY gtb-ssl-descryption.crt /tmp/gtb-ssl-descryption.crt
COPY developer-ssl-descryption.crt /tmp/developer-ssl-descryption.crt

# Добавляем сертификаты в truststore внутри образа
RUN keytool -importcert -trustcacerts -alias gtb-ssl-descryption \
    -file /tmp/gtb-ssl-descryption.crt \
    -keystore $JAVA_HOME/lib/security/cacerts \
    -storepass changeit -noprompt && \
    keytool -importcert -trustcacerts -alias developer-proxy-ca \
    -file /tmp/developer-ssl-descryption.crt \
    -keystore $JAVA_HOME/lib/security/cacerts \
    -storepass changeit -noprompt && \
    rm /tmp/gtb-ssl-descryption.crt /tmp/developer-ssl-descryption.crt

# Копируем собранный jar из стадии build
COPY --from=build /home/gradle/build/libs/allure-server-docker.jar /allure-server-docker.jar

# Открываем порт
EXPOSE ${PORT:-8080}

# Настройки Java
ENV JAVA_OPTS="-Xms256m -Xmx2048m"

# Запуск приложения
ENTRYPOINT ["java", "-Dloader.path=/ext", "-cp", "allure-server-docker.jar", "-Dspring.profiles.active=${PROFILE:default}", "org.springframework.boot.loader.PropertiesLauncher"]
