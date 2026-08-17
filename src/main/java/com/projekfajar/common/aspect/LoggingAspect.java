package com.projekfajar.common.aspect;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

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
     * Exception yang lolos sampai ke luar service/controller selalu dicatat,
     * lengkap dengan stacktrace — sekalipun pemanggilnya menelan error itu.
     */
    @AfterThrowing(pointcut = "applicationPackagePointcut() && springBeanPointcut()", throwing = "e")
    public void logAfterThrowing(JoinPoint joinPoint, Throwable e) {
        log.error("Exception in {}.{}(): {}",
                joinPoint.getSignature().getDeclaringType().getSimpleName(),
                joinPoint.getSignature().getName(),
                e.getMessage(), e);
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
