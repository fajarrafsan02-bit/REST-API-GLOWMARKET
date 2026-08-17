package com.projekfajar.chat.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.projekfajar.ongkir.model.Ongkir;
import com.projekfajar.ongkir.repository.OngkirRepository;
import com.projekfajar.pesanan.model.OrderStatus;
import com.projekfajar.pesanan.model.Pesanan;
import com.projekfajar.pesanan.repository.PesananRepository;
import com.projekfajar.produk.model.Produk;
import com.projekfajar.produk.repository.ProdukRepository;
import com.projekfajar.settings.service.SettingService;
import com.projekfajar.user.model.User;

/**
 * Menguji bahwa bot menjawab dari data nyata dan tidak pernah menebak.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ChatBotServiceTest {

    @Mock
    private PesananRepository pesananRepository;
    @Mock
    private OngkirRepository ongkirRepository;
    @Mock
    private ProdukRepository produkRepository;
    @Mock
    private SettingService settingService;

    @InjectMocks
    private ChatBotService chatBotService;

    private User pelanggan;

    @BeforeEach
    void setUp() {
        pelanggan = User.builder()
                .id(1L)
                .namaLengkap("Budi")
                .email("budi@test.com")
                .build();

        when(pesananRepository.findByUserId(1L)).thenReturn(List.of());
        when(produkRepository.findByNamaContainingIgnoreCaseAndDeletedFalse(anyString()))
                .thenReturn(List.of());
        when(settingService.getValue("store.name")).thenReturn("H.Alimin");
    }

    @Test
    @DisplayName("Status pesanan DIKIRIM menyebutkan nomor resi")
    void statusPesananMenyebutResi() {
        Pesanan pesanan = Pesanan.builder()
                .id(1L)
                .nomorPesanan("ORD-123")
                .status(OrderStatus.DIKIRIM)
                .nomorResi("RESI-XYZ")
                .createdAt(LocalDateTime.now())
                .build();
        when(pesananRepository.findByUserId(1L)).thenReturn(List.of(pesanan));

        String jawaban = chatBotService.susunBalasan(pelanggan, "pesanan saya sampai mana?").orElseThrow();

        assertThat(jawaban).contains("ORD-123", "sedang dikirim", "RESI-XYZ");
    }

    @Test
    @DisplayName("Pelanggan tanpa pesanan dijawab apa adanya, tanpa mengarang")
    void tanpaPesanan() {
        String jawaban = chatBotService.susunBalasan(pelanggan, "cek resi dong").orElseThrow();

        assertThat(jawaban).contains("belum menemukan pesanan");
    }

    @Test
    @DisplayName("Ongkir provinsi dikenal menyebutkan tarif dan estimasi")
    void ongkirProvinsiDikenal() {
        when(ongkirRepository.findByProvinsi("Jawa Timur")).thenReturn(Optional.of(
                Ongkir.builder()
                        .provinsi("Jawa Timur")
                        .tarif(new BigDecimal("25000"))
                        .estimasiHari(3)
                        .build()));

        String jawaban = chatBotService
                .susunBalasan(pelanggan, "ongkir ke jawa timur berapa?").orElseThrow();

        assertThat(jawaban).contains("Jawa Timur", "25.000", "3 hari");
    }

    @Test
    @DisplayName("Pertanyaan ongkir tanpa menyebut provinsi meminta kejelasan")
    void ongkirTanpaProvinsi() {
        String jawaban = chatBotService.susunBalasan(pelanggan, "ongkir berapa ya?").orElseThrow();

        assertThat(jawaban).contains("provinsi");
    }

    @Test
    @DisplayName("Tarif nol dijawab sebagai gratis ongkir")
    void ongkirGratis() {
        when(ongkirRepository.findByProvinsi("Bali")).thenReturn(Optional.of(
                Ongkir.builder()
                        .provinsi("Bali")
                        .tarif(BigDecimal.ZERO)
                        .estimasiHari(2)
                        .build()));

        String jawaban = chatBotService.susunBalasan(pelanggan, "kirim ke bali kena ongkir?").orElseThrow();

        assertThat(jawaban).contains("gratis ongkir");
    }

    @Test
    @DisplayName("Info toko diambil dari pengaturan admin")
    void infoToko() {
        when(settingService.getValue("store.address")).thenReturn("Jl. Horas No. 37");
        when(settingService.getValue("store.phone")).thenReturn("0899884724");

        String jawaban = chatBotService.susunBalasan(pelanggan, "alamat tokonya di mana?").orElseThrow();

        assertThat(jawaban).contains("H.Alimin", "Jl. Horas No. 37", "0899884724");
    }

    @Test
    @DisplayName("Produk ditemukan dijawab dengan harga dan stok")
    void produkDitemukan() {
        when(produkRepository.findByNamaContainingIgnoreCaseAndDeletedFalse("cincin"))
                .thenReturn(List.of(Produk.builder()
                        .id(1L)
                        .nama("Cincin Emas Elegan")
                        .harga(new BigDecimal("3850000"))
                        .stock(12)
                        .build()));

        String jawaban = chatBotService.susunBalasan(pelanggan, "harga cincin berapa?").orElseThrow();

        assertThat(jawaban).contains("Cincin Emas Elegan", "3.850.000", "stok 12");
    }

    @Test
    @DisplayName("Produk stok habis dinyatakan habis, bukan disembunyikan")
    void produkStokHabis() {
        when(produkRepository.findByNamaContainingIgnoreCaseAndDeletedFalse("kalung"))
                .thenReturn(List.of(Produk.builder()
                        .id(2L)
                        .nama("Kalung Emas")
                        .harga(new BigDecimal("7850000"))
                        .stock(0)
                        .build()));

        String jawaban = chatBotService.susunBalasan(pelanggan, "stok kalung masih ada?").orElseThrow();

        assertThat(jawaban).contains("stok habis");
    }

    @Test
    @DisplayName("Produk tidak dikenali meminta nama lebih jelas, tidak mengarang")
    void produkTidakDitemukan() {
        String jawaban = chatBotService.susunBalasan(pelanggan, "harga barang itu berapa?").orElseThrow();

        assertThat(jawaban).contains("sebutkan nama produknya");
    }

    @Test
    @DisplayName("Sapaan dibalas beserta daftar kemampuan")
    void sapaan() {
        String jawaban = chatBotService.susunBalasan(pelanggan, "halo").orElseThrow();

        assertThat(jawaban).contains("Budi", "ongkir");
    }

    @Test
    @DisplayName("Pertanyaan di luar jangkauan dikembalikan kosong agar diteruskan ke admin")
    void diLuarJangkauan() {
        Optional<String> jawaban = chatBotService.susunBalasan(
                pelanggan, "apakah emasnya bisa ditukar dengan motor bekas?");

        assertThat(jawaban).isEmpty();
    }
}
