#!/bin/sh
set -e

# Penyedia database terkelola (Render, Supabase, Neon) menyerahkan alamat
# koneksi dalam bentuk URI Postgres — postgres://user:sandi@host:port/nama —
# sedangkan driver JDBC menuntut awalan jdbc:postgresql:// dan menolak
# kredensial yang menempel di dalam URL. Konversi ditaruh di sini supaya
# application.properties tidak perlu tahu kekhasan tiap platform.
case "$DB_URL" in
  postgres://*|postgresql://*)
    tanpa_skema="${DB_URL#*://}"

    # Kredensial hanya diambil bila memang ada bagian sebelum tanda @.
    case "$tanpa_skema" in
      *@*)
        kredensial="${tanpa_skema%%@*}"
        host_dan_db="${tanpa_skema#*@}"

        # Penugasan ":=" sengaja dipakai agar nilai yang sudah disetel
        # eksplisit lewat environment tidak tertimpa isi URL.
        : "${DB_USERNAME:=${kredensial%%:*}}"
        case "$kredensial" in
          *:*) : "${DB_PASSWORD:=${kredensial#*:}}" ;;
        esac
        ;;
      *)
        host_dan_db="$tanpa_skema"
        ;;
    esac

    DB_URL="jdbc:postgresql://${host_dan_db}"
    ;;
esac

# Postgres terkelola hanya menerima sambungan terenkripsi. Driver JDBC secara
# bawaan memakai mode "prefer" yang diam-diam turun ke koneksi polos bila
# jabat tangan TLS gagal, jadi mode tegas disisipkan bila belum diminta.
# Database di mesin yang sama dikecualikan karena lazimnya tidak memasang
# sertifikat, dan memaksa TLS di sana hanya membuat koneksi gagal.
case "$DB_URL" in
  *sslmode=*|*localhost*|*127.0.0.1*|*@db:*|*//db:*)
    ;;
  jdbc:postgresql://*\?*)
    DB_URL="${DB_URL}&sslmode=require"
    ;;
  jdbc:postgresql://*)
    DB_URL="${DB_URL}?sslmode=require"
    ;;
esac

export DB_URL DB_USERNAME DB_PASSWORD

exec java $JAVA_OPTS -jar app.jar
