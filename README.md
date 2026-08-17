# GlowMarket — Dokumentasi Backend

Backend REST API untuk toko perhiasan emas GlowMarket. Menangani katalog produk,
keranjang, checkout & pembayaran (Xendit), pengiriman (RajaOngkir), chat pelanggan
real-time, poin loyalitas, pengembalian barang, sampai pembukuan akuntansi
berpasangan (double-entry).

---

## Daftar Isi

1. [Teknologi](#teknologi)
2. [Menjalankan Proyek](#menjalankan-proyek)
3. [Konfigurasi](#konfigurasi)
4. [Struktur Proyek](#struktur-proyek)
5. [Autentikasi & Keamanan](#autentikasi--keamanan)
6. [Daftar Endpoint](#daftar-endpoint)
7. [WebSocket](#websocket)
8. [Database & Migrasi](#database--migrasi)
9. [Alur Bisnis Utama](#alur-bisnis-utama)
10. [Integrasi Pihak Ketiga](#integrasi-pihak-ketiga)
11. [Testing](#testing)
12. [Deployment](#deployment)

---

## Teknologi

| Komponen | Versi | Keterangan |
|---|---|---|
| Java | 21 | |
| Spring Boot | 4.0.0 | Web MVC, Security, Data JPA, WebSocket, Mail, Validation |
| PostgreSQL | — | Database utama |
| Flyway | — | Migrasi skema (`spring.jpa.hibernate.ddl-auto=validate`) |
| JJWT | 0.11.5 | Pembuatan & verifikasi JWT |
| Cloudinary | 1.38.0 | Penyimpanan gambar produk |
| Apache POI | 5.2.5 | Ekspor laporan `.xlsx` |
| SpringDoc OpenAPI | 2.3.0 | Swagger UI |
| Google API Client | 2.8.0 | Verifikasi ID token Login Google |
| Lombok | — | Boilerplate reduction |

---

## Menjalankan Proyek

### Prasyarat

- JDK 21
- PostgreSQL berjalan, database sudah dibuat (default nama: `tokweb`)

### Langkah

```bash
# 1. Siapkan kredensial lokal
cp src/main/resources/application-local.properties.example \
   src/main/resources/application-local.properties
# lalu isi nilainya (lihat bagian Konfigurasi)

# 2. Jalankan
./mvnw spring-boot:run          # Linux/macOS
.\mvnw.cmd spring-boot:run      # Windows
```

Aplikasi jalan di `http://localhost:8080`. Flyway otomatis menjalankan migrasi
saat startup.

### Perintah lain

```bash
./mvnw compile        # kompilasi saja
./mvnw test           # jalankan seluruh test
./mvnw clean package  # build JAR ke target/
```

### Akun admin awal

`DataInitializer` membuat satu akun admin saat pertama kali dijalankan bila belum
ada — lihat `config/DataInitializer.java` untuk email dan kata sandinya. Ganti
kata sandi ini sebelum dipakai selain di lingkungan lokal.

---

## Konfigurasi

### Profil

| Profil | Aktifkan dengan | Isi |
|---|---|---|
| `local` (default) | — | Kredensial lokal, tidak masuk Git |
| `dev` | `SPRING_PROFILES_ACTIVE=local,dev` | SQL log aktif, Swagger terbuka, log DEBUG |
| `prod` | `SPRING_PROFILES_ACTIVE=prod` | Swagger tertutup, log ke berkas dengan rotasi, cookie `Secure`, Flyway tanpa auto-baseline |

### `application-local.properties`

File ini di-`.gitignore`. Salin dari `application-local.properties.example`:

```properties
spring.datasource.password=

spring.mail.username=
spring.mail.password=          # App Password Gmail, bukan kata sandi akun

cloudinary.cloud-name=
cloudinary.api-key=
cloudinary.api-secret=

xendit.api-key=
xendit.callback-token=         # Xendit Dashboard > Settings > Webhooks

jwt.secret=                    # minimal 32 karakter
rajaongkir.api-key=
google.oauth.client-id=        # Google Cloud Console > Credentials
```

### Environment variable penting (produksi)

Semua properti di `application.properties` memakai pola `${ENV_VAR:default}`,
jadi bisa di-override lewat environment variable:

| Variable | Default | Fungsi |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/tokweb` | Koneksi database |
| `DB_USERNAME` / `DB_PASSWORD` | `postgres` / — | Kredensial database |
| `JWT_SECRET` | (fallback konstan) | **Wajib diganti di produksi.** Minimal 32 karakter |
| `JWT_ACCESS_EXPIRATION_MS` | `1800000` (30 menit) | Umur access token |
| `JWT_REFRESH_EXPIRATION_MS` | `2592000000` (30 hari) | Umur refresh token |
| `AUTH_COOKIE_SECURE` | `false` (`true` di prod) | Cookie hanya lewat HTTPS |
| `APP_CORS_ORIGINS` | `http://localhost:5173` | Origin frontend, pisahkan koma |
| `APP_FRONTEND_URL` | `http://localhost:5173` | Basis URL redirect setelah bayar |
| `GOOGLE_OAUTH_CLIENT_ID` | — | Harus sama dengan Client ID di frontend |
| `XENDIT_API_KEY` / `XENDIT_CALLBACK_TOKEN` | — | Kredensial Xendit |
| `RAJAONGKIR_API_KEY` | — | Kosong = fitur ongkir real-time mati, jatuh ke tarif tetap |
| `SWAGGER_ENABLED` / `SWAGGER_PUBLIC` | `false` / `false` | Kontrol akses Swagger |
| `AUTH_MAX_ATTEMPTS` / `AUTH_WINDOW_SECONDS` | `10` / `60` | Rate limit endpoint `/auth/**` |

---

## Struktur Proyek

Kode diorganisasi **per domain bisnis**, bukan per lapisan teknis. Setiap modul
punya `controller/`, `dto/`, `model/`, `repository/`, `service/` sendiri.

```
com.projekfajar
├── akuntansi/      Pembukuan double-entry: akun, jurnal, beban, pembelian, laporan
├── alamat/         Alamat pengiriman pengguna
├── auth/           Login, register, OTP, verifikasi email, refresh token, Google
├── chat/           Chat pelanggan–admin + chatbot otomatis
├── common/         ApiResponse, LoggingAspect
├── config/         Security, WebSocket, CORS, Cloudinary, RajaOngkir, Google, dll.
├── contact/        Form kontak (kirim email ke toko)
├── exception/      Exception khusus + GlobalExceptionHandler
├── keranjang/      Keranjang belanja
├── notification/   Notifikasi in-app (admin & user) + event email pesanan
├── ongkir/         Tarif & estimasi ongkos kirim
├── payment/        Invoice Xendit, webhook, rekonsiliasi terjadwal
├── pengembalian/   Pengajuan & pemrosesan retur barang
├── pesanan/        Pesanan, item, status, riwayat
├── poin/           Poin loyalitas & penukaran voucher
├── produk/         Produk, varian, gambar, upload Cloudinary
├── restock/        Notifikasi "beri tahu saya saat stok ada"
├── review/         Ulasan produk
├── settings/       Pengaturan aplikasi (key–value di database)
├── statistik/      Statistik penjualan & laporan harian
├── terjual/        Agregat produk terjual
├── tracking/       Timeline pelacakan pengiriman
├── user/           Profil pengguna & manajemen pelanggan
├── util/           JwtUtil, SecurityUtils, OtpGenerator, RupiahFormatter
├── voucher/        Voucher diskon (umum & hasil tukar poin)
├── wilayah/        Proxy data provinsi/kota/kecamatan/kelurahan & kode pos
└── wishlist/       Wishlist pengguna
```

### Alasan beberapa keputusan desain

- **Otorisasi dipusatkan di `SecurityConfig`.** Sebelumnya tiap controller
  memeriksa sendiri lewat `isAdmin()`, dan satu controller yang lupa langsung
  menjadi lubang keamanan. Pemeriksaan di controller tetap ada sebagai lapis kedua.
- **JWT di cookie httpOnly**, bukan `localStorage` + header `Authorization`.
  JavaScript tidak bisa membacanya sama sekali, jadi token tidak bisa dicuri lewat XSS.
- **CSRF dinonaktifkan.** API ini stateless berbasis token; mekanisme regenerasi
  token CSRF Spring Security menganggap tiap request sebagai "login baru" sehingga
  cookie `XSRF-TOKEN` terhapus di setiap respons — ini yang dulu membuat pembeli
  tiba-tiba logout sendiri. Pertahanan cukup lewat `SameSite=Lax` pada cookie auth.
- **Flyway, bukan `ddl-auto=update`.** Perubahan skema tercatat sebagai berkas
  bernomor, bukan ditebak Hibernate saat startup.

---

## Autentikasi & Keamanan

### Mekanisme token

| Token | Cookie | Path | Umur | Fungsi |
|---|---|---|---|---|
| Access JWT | `auth_token` | `/` | 30 menit | Dikirim di setiap request API |
| Refresh token | `refresh_token` | `/auth` | 30 hari | Hanya dikirim ke `/auth/*`, dipakai memperbarui access token |

Keduanya `httpOnly`, `SameSite=Lax`, dan `Secure` di produksi. Refresh token
disimpan di database sebagai hash (`refresh_token.token_hash`) sehingga bisa
dicabut saat logout.

### Alur login

**Pembeli (USER):**
```
POST /auth/register       → kode verifikasi 6 digit dikirim ke email
POST /auth/verifikasi-email
POST /auth/login          → cookie auth_token + refresh_token dipasang
```

**Admin (verifikasi dua langkah wajib):**
```
POST /auth/login          → terdeteksi ADMIN, OTP 4 digit dikirim ke email
                            (respons tanpa token)
POST /auth/verifikasi-otp-admin → cookie dipasang
```

**Login Google:**
```
POST /auth/google  { credential: "<ID token dari Google Identity Services>" }
```
ID token diverifikasi ke Google di sisi server (tanda tangan, audience,
kedaluwarsa) — data profil dari browser tidak pernah dipercaya langsung.

- Email yang sudah terdaftar manual **digabung otomatis** ke akun yang ada.
- Akun baru langsung berstatus email terverifikasi.
- **Akun admin tetap wajib OTP**: respons berisi `butuhOtpAdmin: true` dan OTP
  dikirim ke email; Google hanya menggantikan langkah email+password.

### Aturan otorisasi

Dievaluasi dari atas ke bawah, **yang cocok duluan menang**.

| Path | Aturan |
|---|---|
| `/auth/**`, `/uploads/**` | Publik |
| `GET /api/produk`, `GET /api/produk/**` | Publik |
| `/api/produk/**` (verb lain) | ADMIN |
| `/api/reviews/produk/**` | Publik |
| `/api/contact/**` | Publik |
| `/api/wilayah/**` | Publik |
| `/api/settings/public` | Publik |
| `/api/payments/webhook` | Publik (diverifikasi `x-callback-token`) |
| `/ws/**` | Publik (handshake divalidasi terpisah) |
| `/swagger-ui/**`, `/v3/api-docs/**` | Hanya bila `app.swagger.public=true` |
| `/api/admin/list` | Login saja — pembeli butuh ini untuk memulai chat CS |
| `/api/admin/**` | ADMIN |
| `/api/statistik/**` | ADMIN |
| `/api/terjual-produk/**` | ADMIN |
| `GET /api/ongkir`, `GET /api/ongkir/**` | Login |
| `POST /api/ongkir/estimasi` | Login |
| `/api/ongkir/**` (verb lain) | ADMIN |
| `/api/keranjang/**`, `/api/wishlist/**`, `/api/payments/**` | Login |
| Selainnya | Login |

### Perlindungan lain

- **Rate limit** pada `/auth/**` (`AuthRateLimitFilter`): default 10 percobaan
  per 60 detik per IP.
- **401, bukan 403**, untuk request tanpa autentikasi — frontend memakai 401
  sebagai sinyal membersihkan sesi basi.
- **Verifikasi email wajib** sebelum checkout, agar bukti pembayaran dan
  pemberitahuan status pesanan dipastikan sampai.
- **Diskon voucher dihitung di server**, bukan dikirim client, sehingga nominal
  tidak bisa dipalsukan.

---

## Daftar Endpoint

Semua respons memakai bentuk seragam:

```json
{ "success": true, "message": "...", "data": { } }
```

Legenda: **Publik** = tanpa login · **Login** = perlu autentikasi · **ADMIN** = khusus admin

### Auth — `/auth`

| Method | Path | Fungsi | Akses |
|---|---|---|---|
| POST | `/auth/register` | Registrasi pembeli | Publik |
| POST | `/auth/verifikasi-email` | Verifikasi email dengan kode 6 digit | Publik |
| POST | `/auth/kirim-ulang-verifikasi` | Kirim ulang kode verifikasi email | Publik |
| POST | `/auth/login` | Login pembeli (admin dialihkan ke OTP) | Publik |
| POST | `/auth/login-admin` | Login admin tahap 1 | Publik |
| POST | `/auth/verifikasi-otp-admin` | Verifikasi OTP admin | Publik |
| POST | `/auth/kirim-ulang-otp-admin` | Kirim ulang OTP admin | Publik |
| POST | `/auth/google` | Login/daftar dengan akun Google | Publik |
| POST | `/auth/refresh` | Terbitkan access token baru dari cookie refresh | Publik |
| POST | `/auth/logout` | Cabut refresh token, hapus cookie | Publik |

### Produk — `/api/produk`

| Method | Path | Fungsi | Akses |
|---|---|---|---|
| GET | `/api/produk` | Daftar semua produk | Publik |
| GET | `/api/produk/{id}` | Detail produk | Publik |
| GET | `/api/produk/status/{status}` | Filter berdasarkan status | Publik |
| GET | `/api/produk/search?nama=` | Cari produk | Publik |
| GET | `/api/produk/{id}/varian` | Varian aktif produk | Publik |
| POST | `/api/produk` | Tambah produk | ADMIN |
| PUT | `/api/produk/{id}` | Ubah produk | ADMIN |
| DELETE | `/api/produk/{id}` | Hapus produk (soft delete) | ADMIN |
| PATCH | `/api/produk/{id}/status` | Ubah status produk | ADMIN |
| PATCH | `/api/produk/{id}/stock` | Ubah stok | ADMIN |
| POST | `/api/produk/upload-image` | Unggah gambar ke Cloudinary (maks 5 MB) | ADMIN |
| POST | `/api/produk/{id}/varian` | Tambah varian | ADMIN |
| PUT | `/api/produk/varian/{id}` | Ubah varian | ADMIN |
| DELETE | `/api/produk/varian/{id}` | Nonaktifkan varian | ADMIN |

### Keranjang — `/api/keranjang`

| Method | Path | Fungsi | Akses |
|---|---|---|---|
| GET | `/api/keranjang` | Isi keranjang + total | Login |
| POST | `/api/keranjang` | Tambah produk ke keranjang | Login |
| PATCH | `/api/keranjang/{id}` | Ubah jumlah item | Login |
| DELETE | `/api/keranjang/{id}` | Hapus satu item | Login |
| DELETE | `/api/keranjang/clear` | Kosongkan keranjang | Login |

### Wishlist — `/api/wishlist`

| Method | Path | Fungsi | Akses |
|---|---|---|---|
| GET | `/api/wishlist` | Daftar wishlist | Login |
| POST | `/api/wishlist` | Tambah ke wishlist | Login |
| DELETE | `/api/wishlist/{id}` | Hapus dari wishlist | Login |
| GET | `/api/wishlist/check/{produkId}` | Cek apakah produk ada di wishlist | Login |

### Alamat — `/api/alamat`

| Method | Path | Fungsi | Akses |
|---|---|---|---|
| GET | `/api/alamat` | Daftar alamat pengguna | Login |
| GET | `/api/alamat/default` | Alamat utama | Login |
| GET | `/api/alamat/{id}` | Detail alamat | Login |
| POST | `/api/alamat` | Tambah alamat | Login |
| PUT | `/api/alamat/{id}` | Ubah alamat | Login |
| PUT | `/api/alamat/{id}/set-default` | Jadikan alamat utama | Login |
| DELETE | `/api/alamat/{id}` | Hapus alamat | Login |

### Wilayah — `/api/wilayah`

Proxy ke API wilayah pihak ketiga. Dilakukan lewat backend agar form alamat tidak
bergantung pada DNS/jaringan pengguna; bila gagal selalu mengembalikan daftar kosong.

| Method | Path | Fungsi | Akses |
|---|---|---|---|
| GET | `/api/wilayah/provinces` | Daftar provinsi | Publik |
| GET | `/api/wilayah/regencies/{provinceId}` | Kota/kabupaten | Publik |
| GET | `/api/wilayah/districts/{regencyId}` | Kecamatan | Publik |
| GET | `/api/wilayah/villages/{districtId}` | Kelurahan/desa | Publik |
| GET | `/api/wilayah/kode-pos?q=` | Cari kode pos | Publik |

### Ongkir — `/api/ongkir`

| Method | Path | Fungsi | Akses |
|---|---|---|---|
| POST | `/api/ongkir/estimasi` | Estimasi ongkir dari keranjang + alamat | Login |
| GET | `/api/ongkir` | Daftar tarif ongkir | Login |
| POST | `/api/ongkir` | Tambah tarif | ADMIN |
| PUT | `/api/ongkir/{id}` | Ubah tarif | ADMIN |
| DELETE | `/api/ongkir/{id}` | Hapus tarif | ADMIN |
| GET | `/api/admin/ongkir/cari-lokasi?q=` | Cari lokasi RajaOngkir | ADMIN |

### Pesanan — `/api/pesanan`

| Method | Path | Fungsi | Akses |
|---|---|---|---|
| GET | `/api/pesanan` | Semua pesanan (admin) / pesanan sendiri (user) | Login |
| GET | `/api/pesanan/{id}` | Detail pesanan | Login (pemilik/admin) |
| GET | `/api/pesanan/nomor/{nomorPesanan}` | Detail berdasarkan nomor pesanan | Login (pemilik/admin) |
| GET | `/api/pesanan/external/{externalId}` | Detail berdasarkan external id Xendit | Login (pemilik/admin) |
| GET | `/api/pesanan/user/history` | Riwayat pesanan pengguna | Login |
| PUT | `/api/pesanan/{id}/status` | Ubah status + nomor resi | ADMIN |

Status pesanan: `PENDING` → `DIKEMAS` → `DIKIRIM` → `SELESAI`, dengan cabang
`DIBATALKAN` dan `DIKEMBALIKAN`.

### Pembayaran — `/api/payments`

| Method | Path | Fungsi | Akses |
|---|---|---|---|
| GET | `/api/payments/methods` | Metode pembayaran aktif untuk checkout | Login |
| POST | `/api/payments/create-invoice` | Buat invoice Xendit | Login |
| POST | `/api/payments/webhook` | Callback Xendit | Publik (token diverifikasi) |
| GET | `/api/payments/{externalId}` | Detail pembayaran | Login (pemilik/admin) |
| GET | `/api/payments/user/history` | Riwayat pembayaran pengguna | Login |
| GET | `/api/payments/check/{invoiceId}` | Cek status langsung ke Xendit | Login (pemilik/admin) |
| GET/POST | `/api/payments/sync/{externalId}` | Sinkronkan ulang status | Login (pemilik/admin) |
| GET/POST | `/api/payments/sync-by-xendit/{xenditInvoiceId}` | Sinkronkan berdasarkan invoice id | Login (pemilik/admin) |

### Voucher — `/api/vouchers`

| Method | Path | Fungsi | Akses |
|---|---|---|---|
| GET | `/api/vouchers/public` | Voucher umum yang sedang aktif | Login |
| POST | `/api/vouchers/check` | Validasi kode + hitung diskon | Login |
| GET | `/api/admin/vouchers` | Daftar semua voucher | ADMIN |
| POST | `/api/admin/vouchers` | Buat voucher | ADMIN |
| PUT | `/api/admin/vouchers/{id}` | Ubah voucher | ADMIN |
| PATCH | `/api/admin/vouchers/{id}/toggle` | Aktif/nonaktifkan | ADMIN |
| DELETE | `/api/admin/vouchers/{id}` | Hapus voucher | ADMIN |

Jenis voucher: `PERSEN` (dibatasi `maksDiskon`) dan `NOMINAL`. Voucher dengan
`user_id` terisi hanya bisa dipakai pemiliknya (hasil tukar poin).

### Poin Loyalitas — `/api/poin`

| Method | Path | Fungsi | Akses |
|---|---|---|---|
| GET | `/api/poin` | Saldo, riwayat, dan voucher pengguna | Login |
| POST | `/api/poin/tukar` | Tukar poin jadi voucher | Login |

Aturan: **1 poin per Rp 10.000** belanja (dari total pesanan yang lunas),
penukaran minimum & kelipatan **100 poin**, nilai **Rp 100/poin**, voucher
berlaku **30 hari**.

### Ulasan — `/api/reviews`

| Method | Path | Fungsi | Akses |
|---|---|---|---|
| GET | `/api/reviews/produk/{produkId}` | Ulasan sebuah produk | Publik |
| POST | `/api/reviews` | Kirim ulasan | Login |
| GET | `/api/reviews/user` | Ulasan milik pengguna | Login |
| GET | `/api/reviews/check/{produkId}/{pesananId}` | Cek sudah pernah mengulas | Login |

### Pengembalian — `/api/pengembalian`

| Method | Path | Fungsi | Akses |
|---|---|---|---|
| POST | `/api/pengembalian` | Ajukan pengembalian | Login |
| GET | `/api/pengembalian` | Daftar pengajuan sendiri | Login |
| GET | `/api/pengembalian/admin?status=` | Semua pengajuan | ADMIN |
| PATCH | `/api/pengembalian/{id}/setujui` | Setujui | ADMIN |
| PATCH | `/api/pengembalian/{id}/tolak` | Tolak | ADMIN |
| PATCH | `/api/pengembalian/{id}/terima` | Barang diterima, stok dikembalikan | ADMIN |

Status: `DIAJUKAN` → `DISETUJUI`/`DITOLAK` → `DITERIMA`.

### Tracking — `/api/tracking`

| Method | Path | Fungsi | Akses |
|---|---|---|---|
| GET | `/api/tracking/{pesananId}` | Timeline pengiriman | Login (pemilik/admin) |
| POST | `/api/tracking/{pesananId}/lanjutkan` | Majukan satu tahap (simulasi) | ADMIN |

Tahapan: `DIPROSES` → `DALAM_PERJALANAN` → `SAMPAI_KOTA_TUJUAN` →
`OUT_FOR_DELIVERY` → `DITERIMA`.

### Restock — `/api/restock`

| Method | Path | Fungsi | Akses |
|---|---|---|---|
| POST | `/api/restock/notifikasi` | Daftar notifikasi saat stok tersedia | Login |
| GET | `/api/restock/notifikasi` | Daftar langganan sendiri | Login |
| DELETE | `/api/restock/notifikasi/{id}` | Batalkan langganan | Login |

### Chat — `/api/chat`

| Method | Path | Fungsi | Akses |
|---|---|---|---|
| POST | `/api/chat/send` | Kirim pesan (alternatif WebSocket) | Login |
| GET | `/api/chat/history?userId=&adminId=` | Riwayat percakapan | Login |
| POST | `/api/chat/mark-read?senderId=` | Tandai sudah dibaca | Login |
| GET | `/api/chat/unread-count` | Jumlah pesan belum dibaca | Login |
| GET | `/api/chat/conversations` | Daftar percakapan (inbox admin) | ADMIN |
| GET | `/api/chat/user-status/{userId}` | Status online satu pengguna | ADMIN |
| GET | `/api/chat/online-users` | Daftar pengguna yang sedang online | ADMIN |
| GET | `/api/admin/list` | Daftar admin untuk memulai chat | Login |

Bila `chatbot.enabled=true` dan admin sedang offline, balasan otomatis dikirim
oleh `ChatBotService`.

### Pengguna — `/api/user`

| Method | Path | Fungsi | Akses |
|---|---|---|---|
| GET | `/api/user/profile` | Profil sendiri | Login |
| PUT | `/api/user/update-profile` | Ubah profil sendiri | Login |
| GET | `/api/user/profile/admin` | Profil admin | ADMIN |
| GET | `/api/user/customers` | Daftar pelanggan | ADMIN |
| GET | `/api/user/total-pelanggan` | Jumlah pelanggan | ADMIN |
| PUT | `/api/user/customers/{id}/toggle-status` | Aktif/nonaktifkan akun | ADMIN |

### Notifikasi

| Method | Path | Fungsi | Akses |
|---|---|---|---|
| GET | `/api/user/notifications` | Notifikasi pengguna + jumlah belum dibaca | Login |
| GET | `/api/user/notifications/unread-count` | Jumlah belum dibaca | Login |
| PUT | `/api/user/notifications/{id}/read` | Tandai satu dibaca | Login |
| PUT | `/api/user/notifications/mark-all-read` | Tandai semua dibaca | Login |
| GET | `/api/admin/notifications` | Notifikasi admin | ADMIN |
| PUT | `/api/admin/notifications/{id}/read` | Tandai satu dibaca | ADMIN |
| PUT | `/api/admin/notifications/mark-all-read` | Tandai semua dibaca | ADMIN |
| GET | `/api/admin/notifications/check-low-stock` | Pindai stok menipis | ADMIN |

### Statistik — `/api/statistik` (ADMIN)

| Method | Path | Fungsi |
|---|---|---|
| GET | `/api/statistik/penjualan/bulan-ini` | Penjualan bulan berjalan |
| GET | `/api/statistik/produk-terjual/bulan-ini` | Produk terjual bulan berjalan |
| GET | `/api/statistik/pesanan/bulan-ini` | Jumlah pesanan bulan berjalan |
| GET | `/api/statistik/grafik/bulanan?tahun=` | Grafik penjualan bulanan |
| GET | `/api/statistik/grafik/12-bulan-terakhir` | Grafik 12 bulan terakhir |
| GET | `/api/statistik/grafik/tahunan?tahun=` | Grafik tahunan |
| GET | `/api/statistik/grafik/tahunan/export-excel?tahun=` | Unduh grafik tahunan `.xlsx` |
| GET | `/api/statistik/laporan-harian?startDate=&endDate=` | Laporan harian rentang tanggal |
| GET | `/api/terjual-produk` | Agregat produk terjual |
| GET | `/api/terjual-produk/produk/{produkId}` | Agregat satu produk |

### Akuntansi — `/api/admin/akuntansi` (ADMIN)

| Method | Path | Fungsi |
|---|---|---|
| GET | `/api/admin/akuntansi/akun` | Daftar akun (chart of accounts) |
| GET | `/api/admin/akuntansi/saldo-awal` | Info saldo awal |
| POST | `/api/admin/akuntansi/saldo-awal` | Catat saldo awal |
| GET | `/api/admin/akuntansi/beban?mulai=&sampai=` | Daftar beban |
| POST | `/api/admin/akuntansi/beban` | Catat beban |
| DELETE | `/api/admin/akuntansi/beban/{id}?alasan=` | Batalkan beban (jurnal balik) |
| GET | `/api/admin/akuntansi/pembelian?mulai=&sampai=` | Daftar pembelian |
| POST | `/api/admin/akuntansi/pembelian` | Catat pembelian (menambah stok) |
| POST | `/api/admin/akuntansi/pembelian/{id}/lunasi` | Lunasi utang pembelian |
| DELETE | `/api/admin/akuntansi/pembelian/{id}?alasan=` | Batalkan pembelian (stok dikembalikan) |

**Laporan** — semua tersedia dalam versi JSON dan `.xlsx` (tambahkan `/excel`):

| Method | Path | Fungsi |
|---|---|---|
| GET | `/api/admin/akuntansi/laporan/laba-rugi?mulai=&sampai=` | Laba rugi |
| GET | `/api/admin/akuntansi/laporan/neraca?sampai=` | Neraca |
| GET | `/api/admin/akuntansi/laporan/buku-besar?kodeAkun=&mulai=&sampai=` | Buku besar |
| GET | `/api/admin/akuntansi/laporan/jurnal?mulai=&sampai=&sumber=` | Jurnal umum |

### Pengaturan & Kontak

| Method | Path | Fungsi | Akses |
|---|---|---|---|
| GET | `/api/settings/public` | Pengaturan `store.*` untuk frontend | Publik |
| GET | `/api/admin/settings` | Semua pengaturan | ADMIN |
| PUT | `/api/admin/settings` | Ubah pengaturan | ADMIN |
| POST | `/api/contact/send` | Kirim pesan form kontak ke email toko | Publik |

Kunci pengaturan yang dipakai: `store.name`, `store.tagline`, `store.description`,
`store.phone`, `store.email`, `store.address`, `store.whatsapp`, `store.instagram`,
`store.logo`, `chatbot.enabled`, `chatbot.pesan_fallback`, `payment.methods`.

---

## WebSocket

Endpoint STOMP: **`/ws`** (SockJS). Prefix aplikasi `/app`, broker `/topic` dan
`/queue`, prefix user `/user`. Handshake divalidasi `WsHandshakeInterceptor`.

### Topik

| Topik | Isi |
|---|---|
| `/topic/chat/user/{userId}` | Pesan chat masuk untuk pembeli |
| `/topic/chat/admin/{adminId}` | Pesan chat masuk untuk admin |
| `/topic/admin/notifications` | Notifikasi admin (pesanan baru, stok menipis, dll.) |
| `/topic/notifications/user/{userId}` | Notifikasi pengguna |
| `/topic/user.presence` | Perubahan status online/offline |

### Mengirim pesan

Destination `/app/chat.send` (ditangani `@MessageMapping("/chat.send")`).

---

## Database & Migrasi

Skema dikelola **Flyway** di `src/main/resources/db/migration`.
`spring.jpa.hibernate.ddl-auto=validate` — Hibernate hanya memeriksa kecocokan
entity dengan skema, tidak pernah mengubah tabel sendiri.

### Aturan menambah migrasi

1. Buat berkas `V{n}__deskripsi_singkat.sql` dengan nomor berikutnya.
2. **Jangan pernah mengubah berkas migrasi yang sudah dijalankan** — Flyway
   memvalidasi checksum dan akan menolak startup bila berubah.
3. Jalankan aplikasi; migrasi otomatis diterapkan.

### Riwayat migrasi

| Versi | Isi |
|---|---|
| V1 | Skema dasar (users, produk, pesanan, keranjang, dll.) |
| V2 | Perbaikan typo kolom `terferifikasi` → `terverifikasi` |
| V3 | Kolom uang memakai `numeric` |
| V4 | Hapus tabel counter terjual yang usang |
| V5 | Verifikasi email |
| V6 | Penanda pesan chat dari bot |
| V7–V10 | Fondasi akuntansi, transaksi, akun selisih persediaan, pembelian kredit |
| V11–V12 | Integrasi RajaOngkir & kurir terpilih |
| V13 | Deskripsi produk |
| V14 | Voucher & diskon |
| V15 | Varian produk |
| V16 | Pengembalian barang |
| V17 | Tracking pengiriman |
| V18 | Notifikasi restock |
| V19 | Poin loyalitas |
| V20 | Refresh token |
| V21 | Status pesanan `DIKEMBALIKAN` |
| V22 | Galeri gambar produk |
| V23 | Ganti nama toko ke GlowMarket |
| V24 | Login Google (`users.google_id`, `password` nullable) |

### Tabel utama

`users`, `produk`, `produk_variant`, `produk_gambar`, `keranjang`, `wishlist`,
`pesanan`, `pesanan_item`, `payments`, `alamat`, `ongkir`, `review`, `voucher`,
`poin_user`, `riwayat_poin`, `pengembalian`, `tracking_pengiriman`,
`restock_notifikasi`, `chat_messages`, `notification`, `app_settings`,
`login_otp`, `email_verification`, `refresh_token`, `akun`, `jurnal`,
`jurnal_detail`, `pembelian`, `pembelian_item`, `beban`, `produk_terjual`.

---

## Alur Bisnis Utama

### Checkout & pembayaran

```
1. POST /api/payments/create-invoice
   → validasi stok & alamat
   → hitung ongkir + diskon voucher (server-side)
   → buat pesanan berstatus PENDING, stok dikurangi & dikunci
   → buat invoice di Xendit
   → kembalikan invoiceUrl

2. Pembeli membayar di halaman Xendit

3. Xendit memanggil POST /api/payments/webhook (atau frontend memanggil /sync)
   → verifikasi x-callback-token
   → PesananService.markOrderPaid():
       • status PENDING → DIKEMAS
       • catat penjualan ke jurnal akuntansi
       • tambahkan poin loyalitas
       • kirim email & notifikasi "pembayaran lunas"

4. Bila invoice kedaluwarsa → pesanan DIBATALKAN, stok dikembalikan
```

`PaymentReconciliationJob` berjalan tiap 10 menit
(`payment.reconciliation-interval-ms`, default `600000`) untuk menangani kasus
webhook yang tidak sampai.

### Pengembalian barang

```
Pembeli: POST /api/pengembalian            → status DIAJUKAN
Admin:   PATCH /{id}/setujui atau /tolak   → DISETUJUI / DITOLAK
Admin:   PATCH /{id}/terima                → DITERIMA
         → stok dikembalikan, pesanan jadi DIKEMBALIKAN, jurnal balik dicatat
```

### Akuntansi otomatis

Setiap transaksi bisnis menghasilkan jurnal berpasangan secara otomatis.

**Bagan akun yang dipakai:**

| Kode | Akun |
|---|---|
| `1-100` | Kas |
| `1-200` | Persediaan |
| `4-100` | Pendapatan Penjualan |
| `4-200` | Pendapatan Ongkir |
| `4-300` | Potongan Penjualan |
| `5-100` | Harga Pokok Penjualan (HPP) |

**Jurnal per kejadian:**

| Kejadian | Jurnal |
|---|---|
| Penjualan lunas | Kas (D) · Pendapatan (K) · Pendapatan Ongkir (K) · Potongan Penjualan (D, bila ada diskon) — lalu HPP (D) · Persediaan (K) |
| Pembatalan/retur penjualan | Persediaan (D) · HPP (K) |
| Pembelian | Persediaan (D) · Kas/Utang (K) |
| Beban | Beban (D) · Kas (K) |
| Penyesuaian stok | Selisih Persediaan (D/K) · Persediaan (K/D) |
| Pembatalan beban/pembelian | Jurnal balik dari entri asal |

Total debit dan kredit selalu diperiksa seimbang
(`JurnalDetailRepository.selisihDebitKredit()`).

---

## Integrasi Pihak Ketiga

| Layanan | Fungsi | Bila gagal |
|---|---|---|
| **Xendit** | Invoice & pembayaran | Checkout gagal dengan pesan jelas |
| **Cloudinary** | Penyimpanan gambar produk | Upload ditolak |
| **RajaOngkir (Komerce)** | Ongkir real-time | Jatuh ke tarif tetap di tabel `ongkir` |
| **Google Identity** | Login Google | Tombol Google disembunyikan bila Client ID kosong |
| **Gmail SMTP** | OTP, verifikasi email, notifikasi pesanan | Dicatat di log, alur utama tetap jalan |
| **API Wilayah** | Provinsi/kota/kecamatan/kelurahan & kode pos | Kembalikan daftar kosong, form tetap bisa diisi manual |

Catatan konfigurasi SMTP: `spring.mail.properties.mail.smtp.localhost=localhost`
wajib ada — tanpa itu JavaMail melakukan reverse-DNS lookup dua kali yang bisa
menggantung ~9,5 detik per pemanggilan pada jaringan tertentu.

---

## Testing

```bash
./mvnw test                              # semua test
./mvnw test -Dtest=PesananServiceTest    # satu kelas
```

**78 test** di 12 berkas, memakai JUnit 5 + Mockito:

| Berkas | Cakupan |
|---|---|
| `PesananServiceTest` | Pembuatan pesanan, `markOrderPaid`, idempotensi, pembatalan, stok |
| `AuthServiceEmailVerificationTest` | Verifikasi email, kedaluwarsa, batas percobaan |
| `ChatServiceTest`, `ChatBotServiceTest` | Alur chat & balasan otomatis |
| `OngkirCalculationServiceTest` | Perhitungan ongkir |
| `AlamatOngkirResolverTest` | Pencocokan alamat ke tarif |
| `JurnalServiceTest`, `BebanServiceTest`, `PembelianServiceTest`, `PenyesuaianStokServiceTest`, `PostingPenjualanServiceTest` | Akuntansi double-entry |
| `ProjekFajarApplicationTests` | Context loading |

---

## Deployment

### Build

```bash
./mvnw clean package
java -jar target/PROJEK-FAJAR-0.0.1-SNAPSHOT.jar
```

### Checklist produksi

- [ ] `SPRING_PROFILES_ACTIVE=prod`
- [ ] `JWT_SECRET` diisi nilai acak minimal 32 karakter
- [ ] `AUTH_COOKIE_SECURE=true` (aplikasi harus di belakang HTTPS)
- [ ] `APP_CORS_ORIGINS` dan `APP_FRONTEND_URL` diarahkan ke domain produksi
- [ ] Kredensial database, Xendit, Cloudinary, RajaOngkir, Google diisi lewat
      environment variable — bukan berkas properties
- [ ] `XENDIT_CALLBACK_TOKEN` diisi, dan URL webhook didaftarkan di dashboard Xendit
- [ ] Origin frontend terdaftar di **Authorized JavaScript origins** Google Cloud Console
- [ ] Swagger tetap tertutup (`SWAGGER_ENABLED=false`)
- [ ] Kata sandi akun admin bawaan sudah diganti
- [ ] Database sudah punya baseline Flyway yang benar
      (`spring.flyway.baseline-on-migrate=false` di prod)

### Dokumentasi API interaktif

Aktifkan profil `dev`, lalu buka `http://localhost:8080/swagger-ui.html`.
