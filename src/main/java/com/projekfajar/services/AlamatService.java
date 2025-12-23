package com.projekfajar.services;

import com.projekfajar.DTO.AlamatRequest;
import com.projekfajar.DTO.AlamatResponse;
import com.projekfajar.models.Alamat;
import com.projekfajar.models.User;
import com.projekfajar.repository.AlamatRepository;
import com.projekfajar.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AlamatService {
    
    private final AlamatRepository alamatRepository;
    private final UserRepository userRepository;
    
    @Transactional
    public AlamatResponse createAlamat(Long userId, AlamatRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User tidak ditemukan"));
        
        // If this is set as default, unset other defaults
        if (Boolean.TRUE.equals(request.getIsDefault())) {
            alamatRepository.findByUserId(userId).forEach(alamat -> {
                alamat.setIsDefault(false);
                alamatRepository.save(alamat);
            });
        }
        
        Alamat alamat = Alamat.builder()
                .user(user)
                .namaLengkap(request.getNamaLengkap())
                .nomorTelepon(request.getNomorTelepon() != null ? request.getNomorTelepon() : user.getNoHp())
                .alamatLengkap(request.getAlamatLengkap())
                .provinsi(request.getProvinsi())
                .kota(request.getKota())
                .kecamatan(request.getKecamatan())
                .kelurahan(request.getKelurahan())
                .kodePos(request.getKodePos())
                .isDefault(request.getIsDefault() != null ? request.getIsDefault() : false)
                .catatan(request.getCatatan())
                .createdAt(LocalDateTime.now())
                .build();
        
        Alamat saved = alamatRepository.save(alamat);
        return convertToResponse(saved);
    }
    
    @Transactional
    public AlamatResponse updateAlamat(Long userId, Long alamatId, AlamatRequest request) {
        Alamat alamat = alamatRepository.findByIdAndUserId(alamatId, userId)
                .orElseThrow(() -> new RuntimeException("Alamat tidak ditemukan"));
        
        // If this is set as default, unset other defaults
        if (Boolean.TRUE.equals(request.getIsDefault()) && !alamat.getIsDefault()) {
            alamatRepository.findByUserId(userId).forEach(a -> {
                if (!a.getId().equals(alamatId)) {
                    a.setIsDefault(false);
                    alamatRepository.save(a);
                }
            });
        }
        
        alamat.setNamaLengkap(request.getNamaLengkap());
        alamat.setNomorTelepon(request.getNomorTelepon());
        alamat.setAlamatLengkap(request.getAlamatLengkap());
        alamat.setProvinsi(request.getProvinsi());
        alamat.setKota(request.getKota());
        alamat.setKecamatan(request.getKecamatan());
        alamat.setKelurahan(request.getKelurahan());
        alamat.setKodePos(request.getKodePos());
        alamat.setIsDefault(request.getIsDefault() != null ? request.getIsDefault() : false);
        alamat.setCatatan(request.getCatatan());
        alamat.setUpdatedAt(LocalDateTime.now());
        
        Alamat updated = alamatRepository.save(alamat);
        return convertToResponse(updated);
    }
    
    @Transactional
    public void deleteAlamat(Long userId, Long alamatId) {
        Alamat alamat = alamatRepository.findByIdAndUserId(alamatId, userId)
                .orElseThrow(() -> new RuntimeException("Alamat tidak ditemukan"));
        alamatRepository.delete(alamat);
    }
    
    @Transactional(readOnly = true)
    public List<AlamatResponse> getAlamatByUser(Long userId) {
        return alamatRepository.findByUserId(userId).stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public AlamatResponse getDefaultAlamat(Long userId) {
        Alamat alamat = alamatRepository.findByUserIdAndIsDefaultTrue(userId)
                .orElse(null);
        return alamat != null ? convertToResponse(alamat) : null;
    }
    
    @Transactional(readOnly = true)
    public AlamatResponse getAlamatById(Long userId, Long alamatId) {
        Alamat alamat = alamatRepository.findByIdAndUserId(alamatId, userId)
                .orElseThrow(() -> new RuntimeException("Alamat tidak ditemukan"));
        return convertToResponse(alamat);
    }
    
    @Transactional
    public AlamatResponse setDefaultAlamat(Long userId, Long alamatId) {
        // Unset all defaults
        alamatRepository.findByUserId(userId).forEach(alamat -> {
            alamat.setIsDefault(false);
            alamatRepository.save(alamat);
        });
        
        // Set new default
        Alamat alamat = alamatRepository.findByIdAndUserId(alamatId, userId)
                .orElseThrow(() -> new RuntimeException("Alamat tidak ditemukan"));
        alamat.setIsDefault(true);
        alamat.setUpdatedAt(LocalDateTime.now());
        
        Alamat updated = alamatRepository.save(alamat);
        return convertToResponse(updated);
    }
    
    private AlamatResponse convertToResponse(Alamat alamat) {
        return AlamatResponse.builder()
                .id(alamat.getId())
                .userId(alamat.getUser().getId())
                .namaLengkap(alamat.getNamaLengkap())
                .nomorTelepon(alamat.getNomorTelepon())
                .alamatLengkap(alamat.getAlamatLengkap())
                .provinsi(alamat.getProvinsi())
                .kota(alamat.getKota())
                .kecamatan(alamat.getKecamatan())
                .kelurahan(alamat.getKelurahan())
                .kodePos(alamat.getKodePos())
                .isDefault(alamat.getIsDefault())
                .catatan(alamat.getCatatan())
                .createdAt(alamat.getCreatedAt())
                .build();
    }
}
