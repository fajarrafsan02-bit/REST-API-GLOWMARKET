# Build dan runtime dipisah supaya image akhir tidak membawa Maven, kode
# sumber, maupun cache dependency — hanya JRE dan satu berkas JAR.

FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Salin deskriptor dependency lebih dulu agar layer unduhan dependency
# tetap dipakai ulang selama pom.xml tidak berubah.
COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B clean package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

# Render menyuntikkan PORT dan hanya meneruskan trafik ke port tersebut;
# application.properties sudah membaca ${PORT:8080}.
EXPOSE 8080

# Instance kecil (Render free hanya 512 MB) membuat JVM salah menebak ukuran
# heap dan berujung OOM saat startup. MaxRAMPercentage mengikat heap ke porsi
# memori kontainer, bukan memori host.
#
# SerialGC dipilih karena pada memori sekecil ini GC paralel/G1 justru
# memakan lebih banyak memori untuk struktur internalnya sendiri.
ENV JAVA_OPTS="-XX:MaxRAMPercentage=70 -XX:+UseSerialGC -Xss512k"

# Skrip ini menormalkan format URL database milik platform ke bentuk JDBC
# sebelum menjalankan aplikasi.
COPY entrypoint.sh .
RUN chmod +x entrypoint.sh

ENTRYPOINT ["./entrypoint.sh"]
