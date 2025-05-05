# --- Стадия сборки (Build Stage) ---
FROM eclipse-temurin:21-jdk-jammy as builder
# Выбери базовый образ с JDK 17 (или новее, если нужно). Jammy - Ubuntu 22.04

# Рабочая директория внутри контейнера
WORKDIR /app

# Копируем Maven Wrapper (если используешь) для сборки внутри контейнера
COPY .mvn/ .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw

# (Опционально, но ускоряет сборку) Скачиваем зависимости перед копированием исходников
# RUN ./mvnw dependency:go-offline

# Копируем исходный код
COPY src ./src

# Собираем приложение, пропускаем тесты, так как они будут отдельно
# Используй -Pprod, если у тебя есть профиль для продакшена
RUN ls -la && echo "--- POM ---" && cat pom.xml && echo "--- MVNW ---" && cat mvnw || echo "mvnw not found"
RUN ./mvnw package -DskipTests

# --- Финальная стадия (Final Stage) ---
FROM eclipse-temurin:21-jre-jammy
# Используем образ только с JRE для уменьшения размера финального образа

WORKDIR /app

# Копируем собранный JAR из стадии сборки
# Имя JAR файла может отличаться, используй wildcard или точное имя
COPY --from=builder /app/target/*.jar app.jar

# Открываем порт, на котором работает приложение
EXPOSE 8080

# Команда для запуска приложения при старте контейнера
ENTRYPOINT ["java", "-jar", "app.jar"]

# Можно добавить аргументы JVM, если нужно, например:
# ENTRYPOINT ["java", "-Xmx512m", "-jar", "app.jar"]