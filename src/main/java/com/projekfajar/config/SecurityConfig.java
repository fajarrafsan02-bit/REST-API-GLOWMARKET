package com.projekfajar.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final JwtAuthenticationFilter jwtAuthFilter;
    private final AuthRateLimitFilter authRateLimitFilter;

    /** Origin frontend yang diizinkan — dikonfigurasi lewat properti, bukan hardcode di 17 controller. */
    @Value("${app.cors.allowed-origins:http://localhost:5173}")
    private List<String> allowedOrigins;

    /** Swagger dibuka tanpa login hanya bila diizinkan (default: hanya profil dev). */
    @Value("${app.swagger.public:false}")
    private boolean swaggerPublic;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                /*
                 * CSRF berbasis token (double-submit cookie) sempat dicoba saat token
                 * pindah ke cookie, tapi ternyata tidak cocok dengan cara aplikasi ini
                 * autentikasi: SecurityContextHolder dibangun ulang dari nol di SETIAP
                 * request lewat JwtAuthenticationFilter (stateless, tidak ada sesi
                 * server), dan mekanisme regenerasi token CSRF milik Spring Security
                 * ternyata menganggap ini sebagai "login baru" pada setiap request —
                 * cookie XSRF-TOKEN jadi dihapus di respons setiap request terautentikasi,
                 * sehingga token itu tidak pernah bisa dipakai lagi di request berikutnya.
                 * Ini yang jadi biang pembeli tiba-tiba "logout" sendiri.
                 * Pertahanan CSRF di sini cukup lewat SameSite=Lax pada cookie auth
                 * (lihat AuthCookieService) — itu sudah menahan browser mengirim cookie
                 * pada request lintas-situs yang mengubah data (POST/PUT/DELETE/PATCH),
                 * yang justru itulah serangan CSRF yang sebenarnya. Ini juga sesuai
                 * rekomendasi umum: API stateless berbasis token tidak butuh proteksi
                 * CSRF tambahan seperti aplikasi berbasis sesi.
                 */
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/**", "/uploads/**").permitAll()
                        // Dipanggil platform hosting untuk memeriksa proses masih hidup
                        .requestMatchers("/health").permitAll()
                        // Katalog boleh dibaca publik, tapi semua perubahan produk
                        // (tambah/ubah/hapus/stok/upload gambar) hanya untuk admin.
                        .requestMatchers(HttpMethod.GET, "/api/produk", "/api/produk/**").permitAll()
                        .requestMatchers("/api/produk/**").hasRole("ADMIN")
                        .requestMatchers("/api/reviews/produk/**").permitAll()
                        .requestMatchers("/api/contact/**").permitAll() // Public endpoint for contact form
                        .requestMatchers("/api/wilayah/**").permitAll() // Proxy publik data wilayah + kode pos
                        .requestMatchers("/api/settings/public").permitAll() // Public store settings
                        .requestMatchers("/api/payments/webhook").permitAll()
                        .requestMatchers("/ws/**").permitAll()
                        // Swagger hanya terbuka bila diaktifkan (profil dev)
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html")
                        .access((authentication, ctx) -> new AuthorizationDecision(swaggerPublic))

                        /*
                         * Otorisasi admin dipusatkan di sini. Sebelumnya setiap controller
                         * memeriksa sendiri lewat isAdmin(), sehingga satu controller yang
                         * lupa (seperti Produk) langsung menjadi lubang keamanan.
                         * Pemeriksaan di controller tetap ada sebagai lapis kedua.
                         */
                        // Daftar admin dipakai pembeli untuk memulai chat, jadi cukup
                        // wajib login — bukan publik seperti sebelumnya. Aturan ini
                        // harus di atas /api/admin/** karena yang cocok duluan menang.
                        .requestMatchers("/api/admin/list").authenticated()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/statistik/**").hasRole("ADMIN")
                        .requestMatchers("/api/terjual-produk/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/ongkir", "/api/ongkir/**").authenticated()
                        // Estimasi dipakai halaman Keranjang oleh pembeli biasa — harus
                        // ditulis sebelum baris ADMIN-only di bawah karena yang cocok duluan menang.
                        .requestMatchers(HttpMethod.POST, "/api/ongkir/estimasi").authenticated()
                        .requestMatchers("/api/ongkir/**").hasRole("ADMIN")

                        .requestMatchers("/api/keranjang/**", "/api/wishlist/**", "/api/payments/**").authenticated()
                        .anyRequest().authenticated())
                // Request tanpa autentikasi (token hilang/kedaluwarsa/tidak valid)
                // harus balas 401, bukan 403. Frontend memakai 401 sebagai sinyal
                // untuk membersihkan sesi basi; tanpa ini pengguna terjebak dengan
                // token lama yang selalu ditolak.
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint(this::writeUnauthorized))
                .addFilterBefore(authRateLimitFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    private void writeUnauthorized(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException authException) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(
                "{\"success\":false,\"message\":\"Sesi tidak valid atau sudah berakhir, silakan login kembali\"}");
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(allowedOrigins);

        // Izinkan semua method HTTP
        configuration.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH", "HEAD"));

        // Izinkan semua header
        configuration.setAllowedHeaders(Arrays.asList("*"));

        // Izinkan credentials (cookies, authorization headers)
        configuration.setAllowCredentials(true);

        // Max age preflight request
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
