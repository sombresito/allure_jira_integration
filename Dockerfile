# Используем только JRE-образ
FROM eclipse-temurin:19-jre-jammy

# --- Опционально: импорт ваших сертификатов ---
# Копируем CA и импортируем в truststore
COPY gtb-ssl-descryption.crt /tmp/gtb.crt
COPY developer-ssl-descryption.crt /tmp/dev.crt

RUN keytool -importcert \
    -alias gtb-ssl-descryption \
    -file /tmp/gtb.crt \
    -keystore $JAVA_HOME/lib/security/cacerts \
    -storepass changeit \
    -noprompt && \
  keytool -importcert \
    -alias developer-proxy-ca \
    -file /tmp/dev.crt \
    -keystore $JAVA_HOME/lib/security/cacerts \
    -storepass changeit \
    -noprompt && \
  rm /tmp/gtb.crt /tmp/dev.crt

# Копируем ваш готовый JAR (убедитесь, что он лежит рядом с Dockerfile)
ARG JAR_FILE=allure-server-docker.jar
COPY ${JAR_FILE} /app/${JAR_FILE}

WORKDIR /app

# Публикуем порт
EXPOSE ${PORT:-9443}

# Задаём опции JVM
ENV JAVA_OPTS="-Xms256m -Xmx2048m"

# Запускаем приложение
ENV SPRING_PROFILES_ACTIVE=${PROFILE:-default}

ENTRYPOINT sh -c 'java \
  -Dloader.path=/ext \
  -cp allure-server-docker.jar \
  -Dspring.profiles.active=${PROFILE:-default} \
  org.springframework.boot.loader.PropertiesLauncher'

