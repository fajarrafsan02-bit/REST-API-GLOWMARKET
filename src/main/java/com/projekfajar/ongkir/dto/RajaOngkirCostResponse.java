package com.projekfajar.ongkir.dto;

import java.math.BigDecimal;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

/** Bentuk respons POST /calculate/domestic-cost — diverifikasi dari dokumentasi resmi RajaOngkir/Komerce. */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RajaOngkirCostResponse {
    private Meta meta;
    private List<Tarif> data;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Meta {
        private String message;
        private Integer code;
        private String status;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Tarif {
        private String name;
        private String code;
        private String service;
        private String description;
        private BigDecimal cost;
        private String etd;
    }
}
