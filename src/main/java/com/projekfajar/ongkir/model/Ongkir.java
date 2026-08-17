package com.projekfajar.ongkir.model;

import java.math.BigDecimal;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "ongkir")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Ongkir {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 100)
    private String provinsi;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal tarif;

    private Integer estimasiHari;
}