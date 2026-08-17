package com.projekfajar.common.aspect;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.projekfajar.exception.BusinessException;
import com.projekfajar.exception.EmailNotFoundException;
import com.projekfajar.exception.InvalidOtpException;
import com.projekfajar.exception.InvalidPasswordException;
import com.projekfajar.exception.OtpExpiredException;
import com.projekfajar.exception.ProdukNotFoundException;
import com.projekfajar.exception.ResourceNotFoundException;
import com.projekfajar.exception.UnauthorizedAccessException;

import lombok.extern.slf4j.Slf4j;

/**
 * Jaring pengaman logging: melengkapi log manual di controller/service dengan
 * jejak durasi dan exception untuk SEMUA method, termasuk yang lupa diberi log.
 *
 * Sengaja tidak mencetak argumen maupun nilai kembalian. Method di sini
 * mencakup login, ganti password, dan webhook pembayaran — argumennya berisi
 * password mentah, kode OTP, dan payload Xendit, jadi mencetaknya (walau hanya
 * di level debug) akan membocorkan kredensial ke berkas log.
 */
@Aspect
@Component
@Slf4j
public class LoggingAspect {

    /** Di atas ambang ini sebuah pemanggilan dianggap lambat dan naik ke level INFO. */
    @Value("${app.logging.slow-call-ms:1000}")
    private long ambangLambatMs;

    @Pointcut("within(@org.springframework.stereotype.Service *)"
            + " || within(@org.springframework.web.bind.annotation.RestController *)")
    public void springBeanPointcut() {
        // Penanda pointcut saja; implementasinya ada di advice di bawah.
    }

    @Pointcut("within(com.projekfajar..*)")
    public void applicationPackagePointcut() {
        // Penanda pointcut saja; implementasinya ada di advice di bawah.
    }

    /**
     * Exception yang lolos sampai ke luar service/controller dicatat di sini,
     * sekalipun pemanggilnya menelan error itu.
     *
     * Pelanggaran aturan bisnis sengaja dicatat tanpa stacktrace pada level
     * WARN. Penolakan seperti sesi kedaluwarsa, OTP salah, atau stok habis
     * adalah jalur normal yang terjadi setiap hari — mencetak seratusan baris
     * stacktrace untuk tiap kejadian hanya menenggelamkan kesalahan sungguhan
     * yang justru perlu dilihat.
     */
    @AfterThrowing(pointcut = "applicationPackagePointcut() && springBeanPointcut()", throwing = "e")
    public void logAfterThrowing(JoinPoint joinPoint, Throwable e) {
        String kelas = joinPoint.getSignature().getDeclaringType().getSimpleName();
        String method = joinPoint.getSignature().getName();

        if (kegagalanTerduga(e)) {
            log.warn("{}.{}() ditolak: {}", kelas, method, e.getMessage());
            return;
        }

        log.error("Exception in {}.{}(): {}", kelas, method, e.getMessage(), e);
    }

    /**
     * Menandai exception yang sudah punya arti bisnis dan penanganannya
     * sendiri di GlobalExceptionHandler, sehingga bukan gejala kerusakan.
     */
    private boolean kegagalanTerduga(Throwable e) {
        return e instanceof BusinessException
                || e instanceof UnauthorizedAccessException
                || e instanceof ResourceNotFoundException
                || e instanceof ProdukNotFoundException
                || e instanceof EmailNotFoundException
                || e instanceof InvalidOtpException
                || e instanceof InvalidPasswordException
                || e instanceof OtpExpiredException
                || e instanceof IllegalArgumentException;
    }

    /**
     * Durasi setiap pemanggilan. Level debug agar log normal tidak tenggelam;
     * hanya pemanggilan lambat yang dinaikkan ke INFO supaya masalah performa
     * tetap terlihat tanpa perlu menyalakan debug.
     */
    @Around("applicationPackagePointcut() && springBeanPointcut()")
    public Object logAround(ProceedingJoinPoint joinPoint) throws Throwable {
        long mulai = System.currentTimeMillis();
        Object hasil = joinPoint.proceed();
        long durasi = System.currentTimeMillis() - mulai;

        String kelas = joinPoint.getSignature().getDeclaringType().getSimpleName();
        String method = joinPoint.getSignature().getName();

        if (durasi >= ambangLambatMs) {
            log.info("SLOW {}.{}() took {} ms", kelas, method, durasi);
        } else if (log.isDebugEnabled()) {
            log.debug("{}.{}() took {} ms", kelas, method, durasi);
        }

        return hasil;
    }
}
