package com.projekfajar.alamat.controller;

import com.projekfajar.alamat.dto.AlamatRequest;
import com.projekfajar.alamat.dto.AlamatResponse;
import com.projekfajar.common.ApiResponse;
import com.projekfajar.user.model.User;
import com.projekfajar.alamat.service.AlamatService;
import com.projekfajar.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import lombok.extern.slf4j.Slf4j;

/**
 * Controller ini dulu mengembalikan objek/list mentah, berbeda dari controller
 * lain yang memakai bentuk {success, message, data}. Perbedaan itulah yang
 * memaksa frontend menebak bentuk respons. Sekarang seragam lewat ApiResponse.
 */
@RestController
@RequestMapping("/api/alamat")
@RequiredArgsConstructor
@Slf4j
public class AlamatController {

        private final AlamatService alamatService;
        private final SecurityUtils securityUtils;

        @PostMapping
        public ResponseEntity<ApiResponse<AlamatResponse>> createAlamat(
                        @RequestBody AlamatRequest request,
                        Authentication authentication) {
                User user = securityUtils.getCurrentUser(authentication);
                if (user == null) {
                        log.warn("Create address rejected: unauthenticated request");
                        return unauthorized();
                }

                log.info("Creating address for userId={}", user.getId());
                AlamatResponse response = alamatService.createAlamat(user.getId(), request);
                log.info("Address created: id={}, userId={}", response.getId(), user.getId());
                return ResponseEntity.status(HttpStatus.CREATED)
                                .body(ApiResponse.ok("Alamat berhasil ditambahkan", response));
        }

        @GetMapping
        public ResponseEntity<ApiResponse<List<AlamatResponse>>> getAlamatList(Authentication authentication) {
                User user = securityUtils.getCurrentUser(authentication);
                if (user == null) {
                        log.warn("Address list rejected: unauthenticated request");
                        return unauthorized();
                }

                log.info("Fetching address list for userId={}", user.getId());
                List<AlamatResponse> responses = alamatService.getAlamatByUser(user.getId());
                log.info("Address list returned: userId={}, total={}", user.getId(), responses.size());
                return ResponseEntity.ok(ApiResponse.ok("Daftar alamat berhasil diambil", responses));
        }

        @GetMapping("/default")
        public ResponseEntity<ApiResponse<AlamatResponse>> getDefaultAlamat(Authentication authentication) {
                User user = securityUtils.getCurrentUser(authentication);
                if (user == null) {
                        log.warn("Default address request rejected: unauthenticated request");
                        return unauthorized();
                }

                log.info("Fetching default address for userId={}", user.getId());
                AlamatResponse response = alamatService.getDefaultAlamat(user.getId());
                if (response == null) {
                        log.warn("No default address set for userId={}", user.getId());
                        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                        .body(ApiResponse.error("Belum ada alamat utama"));
                }

                log.debug("Default address found: id={}, userId={}", response.getId(), user.getId());
                return ResponseEntity.ok(ApiResponse.ok(response));
        }

        @GetMapping("/{alamatId}")
        public ResponseEntity<ApiResponse<AlamatResponse>> getAlamatById(
                        @PathVariable Long alamatId,
                        Authentication authentication) {
                User user = securityUtils.getCurrentUser(authentication);
                if (user == null) {
                        log.warn("Address detail rejected: unauthenticated request, alamatId={}", alamatId);
                        return unauthorized();
                }

                log.info("Fetching address id={} for userId={}", alamatId, user.getId());
                AlamatResponse response = alamatService.getAlamatById(user.getId(), alamatId);
                return ResponseEntity.ok(ApiResponse.ok(response));
        }

        @PutMapping("/{alamatId}")
        public ResponseEntity<ApiResponse<AlamatResponse>> updateAlamat(
                        @PathVariable Long alamatId,
                        @RequestBody AlamatRequest request,
                        Authentication authentication) {
                User user = securityUtils.getCurrentUser(authentication);
                if (user == null) {
                        log.warn("Update address rejected: unauthenticated request, alamatId={}", alamatId);
                        return unauthorized();
                }

                log.info("Updating address id={} for userId={}", alamatId, user.getId());
                AlamatResponse response = alamatService.updateAlamat(user.getId(), alamatId, request);
                log.info("Address updated: id={}, userId={}", alamatId, user.getId());
                return ResponseEntity.ok(ApiResponse.ok("Alamat berhasil diperbarui", response));
        }

        @PutMapping("/{alamatId}/set-default")
        public ResponseEntity<ApiResponse<AlamatResponse>> setDefaultAlamat(
                        @PathVariable Long alamatId,
                        Authentication authentication) {
                User user = securityUtils.getCurrentUser(authentication);
                if (user == null) {
                        return unauthorized();
                }

                AlamatResponse response = alamatService.setDefaultAlamat(user.getId(), alamatId);
                return ResponseEntity.ok(ApiResponse.ok("Alamat utama berhasil diubah", response));
        }

        @DeleteMapping("/{alamatId}")
        public ResponseEntity<ApiResponse<Void>> deleteAlamat(
                        @PathVariable Long alamatId,
                        Authentication authentication) {
                User user = securityUtils.getCurrentUser(authentication);
                if (user == null) {
                        return unauthorized();
                }

                alamatService.deleteAlamat(user.getId(), alamatId);
                return ResponseEntity.ok(ApiResponse.ok("Alamat berhasil dihapus", null));
        }

        private <T> ResponseEntity<ApiResponse<T>> unauthorized() {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                .body(ApiResponse.error("Silakan login terlebih dahulu"));
        }
}
