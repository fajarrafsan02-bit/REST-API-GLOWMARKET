package com.projekfajar.controllers;

import com.projekfajar.DTO.AlamatRequest;
import com.projekfajar.DTO.AlamatResponse;
import com.projekfajar.models.User;
import com.projekfajar.repository.UserRepository;
import com.projekfajar.services.AlamatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/alamat")
@RequiredArgsConstructor
public class AlamatController {

        private final AlamatService alamatService;
        private final UserRepository userRepository;

        @PostMapping
        public ResponseEntity<AlamatResponse> createAlamat(
                        @RequestBody AlamatRequest request,
                        Authentication authentication) {
                String email = authentication.getName();
                User user = userRepository.findByEmail(email)
                                .orElseThrow(() -> new RuntimeException("User tidak ditemukan"));
                AlamatResponse response = alamatService.createAlamat(user.getId(), request);
                return ResponseEntity.status(HttpStatus.CREATED).body(response);
        }

        @GetMapping
        public ResponseEntity<List<AlamatResponse>> getAlamatList(Authentication authentication) {
                String email = authentication.getName();
                User user = userRepository.findByEmail(email)
                                .orElseThrow(() -> new RuntimeException("User tidak ditemukan"));
                List<AlamatResponse> responses = alamatService.getAlamatByUser(user.getId());
                return ResponseEntity.ok(responses);
        }

        @GetMapping("/default")
        public ResponseEntity<AlamatResponse> getDefaultAlamat(Authentication authentication) {
                String email = authentication.getName();
                User user = userRepository.findByEmail(email)
                                .orElseThrow(() -> new RuntimeException("User tidak ditemukan"));
                AlamatResponse response = alamatService.getDefaultAlamat(user.getId());
                if (response == null) {
                        return ResponseEntity.notFound().build();
                }
                return ResponseEntity.ok(response);
        }

        @GetMapping("/{alamatId}")
        public ResponseEntity<AlamatResponse> getAlamatById(
                        @PathVariable Long alamatId,
                        Authentication authentication) {
                String email = authentication.getName();
                User user = userRepository.findByEmail(email)
                                .orElseThrow(() -> new RuntimeException("User tidak ditemukan"));
                AlamatResponse response = alamatService.getAlamatById(user.getId(), alamatId);
                return ResponseEntity.ok(response);
        }

        @PutMapping("/{alamatId}")
        public ResponseEntity<AlamatResponse> updateAlamat(
                        @PathVariable Long alamatId,
                        @RequestBody AlamatRequest request,
                        Authentication authentication) {
                String email = authentication.getName();
                User user = userRepository.findByEmail(email)
                                .orElseThrow(() -> new RuntimeException("User tidak ditemukan"));
                AlamatResponse response = alamatService.updateAlamat(user.getId(), alamatId, request);
                return ResponseEntity.ok(response);
        }

        @PutMapping("/{alamatId}/set-default")
        public ResponseEntity<AlamatResponse> setDefaultAlamat(
                        @PathVariable Long alamatId,
                        Authentication authentication) {
                String email = authentication.getName();
                User user = userRepository.findByEmail(email)
                                .orElseThrow(() -> new RuntimeException("User tidak ditemukan"));
                AlamatResponse response = alamatService.setDefaultAlamat(user.getId(), alamatId);
                return ResponseEntity.ok(response);
        }

        @DeleteMapping("/{alamatId}")
        public ResponseEntity<Void> deleteAlamat(
                        @PathVariable Long alamatId,
                        Authentication authentication) {
                String email = authentication.getName();
                User user = userRepository.findByEmail(email)
                                .orElseThrow(() -> new RuntimeException("User tidak ditemukan"));
                alamatService.deleteAlamat(user.getId(), alamatId);
                return ResponseEntity.noContent().build();
        }
}
