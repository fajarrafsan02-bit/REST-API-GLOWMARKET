package com.projekfajar.alamat.service;

import com.projekfajar.exception.ResourceNotFoundException;
import com.projekfajar.alamat.dto.AlamatRequest;
import com.projekfajar.alamat.dto.AlamatResponse;
import com.projekfajar.alamat.model.Alamat;
import com.projekfajar.user.model.User;
import com.projekfajar.alamat.repository.AlamatRepository;
import com.projekfajar.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AlamatService {
    
    
    private final AlamatRepository alamatRepository;
    private final UserRepository userRepository;
    
    @Transactional
    public AlamatResponse createAlamat(Long userId, AlamatRequest request) {
        log.info("Creating new address for userId: {}, recipient: {}", userId, request.getNamaLengkap());
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("Create address failed: User {} not found", userId);
                    return new ResourceNotFoundException("User tidak ditemukan");
                });
        
        // If this is set as default, unset other defaults
        if (Boolean.TRUE.equals(request.getIsDefault())) {
            log.info("Unsetting existing default address for userId: {}", userId);
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
        log.info("Address created successfully with id: {} for userId: {}", saved.getId(), userId);
        return convertToResponse(saved);
    }
    
    @Transactional
    public AlamatResponse updateAlamat(Long userId, Long alamatId, AlamatRequest request) {
        log.info("Updating address id: {} for userId: {}", alamatId, userId);
        Alamat alamat = alamatRepository.findByIdAndUserId(alamatId, userId)
                .orElseThrow(() -> {
                    log.warn("Update address failed: Address id {} not found for userId {}", alamatId, userId);
                    return new ResourceNotFoundException("Alamat tidak ditemukan");
                });
        
        // If this is set as default, unset other defaults
        if (Boolean.TRUE.equals(request.getIsDefault()) && !alamat.getIsDefault()) {
            log.info("Unsetting other default addresses for userId: {}", userId);
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
        log.info("Address id: {} updated successfully for userId: {}", updated.getId(), userId);
        return convertToResponse(updated);
    }
    
    @Transactional
    public void deleteAlamat(Long userId, Long alamatId) {
        log.info("Deleting address id: {} for userId: {}", alamatId, userId);
        Alamat alamat = alamatRepository.findByIdAndUserId(alamatId, userId)
                .orElseThrow(() -> {
                    log.warn("Delete address failed: Address id {} not found for userId {}", alamatId, userId);
                    return new ResourceNotFoundException("Alamat tidak ditemukan");
                });
        alamatRepository.delete(alamat);
        log.info("Address id: {} deleted successfully for userId: {}", alamatId, userId);
    }
    
    @Transactional(readOnly = true)
    public List<AlamatResponse> getAlamatByUser(Long userId) {
        log.info("Fetching all addresses for userId: {}", userId);
        return alamatRepository.findByUserId(userId).stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public AlamatResponse getDefaultAlamat(Long userId) {
        log.info("Fetching default address for userId: {}", userId);
        Alamat alamat = alamatRepository.findByUserIdAndIsDefaultTrue(userId)
                .orElse(null);
        return alamat != null ? convertToResponse(alamat) : null;
    }
    
    @Transactional(readOnly = true)
    public AlamatResponse getAlamatById(Long userId, Long alamatId) {
        log.info("Fetching address id: {} for userId: {}", alamatId, userId);
        Alamat alamat = alamatRepository.findByIdAndUserId(alamatId, userId)
                .orElseThrow(() -> {
                    log.warn("Fetch address failed: Address id {} not found for userId {}", alamatId, userId);
                    return new ResourceNotFoundException("Alamat tidak ditemukan");
                });
        return convertToResponse(alamat);
    }
    
    @Transactional
    public AlamatResponse setDefaultAlamat(Long userId, Long alamatId) {
        log.info("Setting address id: {} as default for userId: {}", alamatId, userId);
        // Unset all defaults
        alamatRepository.findByUserId(userId).forEach(alamat -> {
            alamat.setIsDefault(false);
            alamatRepository.save(alamat);
        });
        
        // Set new default
        Alamat alamat = alamatRepository.findByIdAndUserId(alamatId, userId)
                .orElseThrow(() -> {
                    log.warn("Set default address failed: Address id {} not found for userId {}", alamatId, userId);
                    return new ResourceNotFoundException("Alamat tidak ditemukan");
                });
        alamat.setIsDefault(true);
        alamat.setUpdatedAt(LocalDateTime.now());
        
        Alamat updated = alamatRepository.save(alamat);
        log.info("Address id: {} successfully set as default for userId: {}", alamatId, userId);
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
