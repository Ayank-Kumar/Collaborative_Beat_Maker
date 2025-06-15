FROM eclipse-temurin:21-jdk

RUN apt-get update && \
    apt-get install -y \
    libxext6 \
    libxrender1 \
    libxi6 \
    libxtst6 \
    libxrandr2 \
    libgtk2.0-0 \
    libxinerama1 \
    libxcursor1 && \
    rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY out/production/Collaborative_Beat_Maker/. .
CMD ["java", "Client_Side"]
