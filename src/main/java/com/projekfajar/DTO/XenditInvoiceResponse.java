package com.projekfajar.DTO;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class XenditInvoiceResponse {
    
    private String id;
    
    @JsonProperty("external_id")
    private String externalId;
    
    @JsonProperty("user_id")
    private String userId;
    
    private String status;
    
    @JsonProperty("merchant_name")
    private String merchantName;
    
    @JsonProperty("merchant_profile_picture_url")
    private String merchantProfilePictureUrl;
    
    private Double amount;
    
    @JsonProperty("payer_email")
    private String payerEmail;
    
    private String description;
    
    @JsonProperty("expiry_date")
    private String expiryDate;
    
    @JsonProperty("invoice_url")
    private String invoiceUrl;
    
    @JsonProperty("available_banks")
    private Object[] availableBanks;
    
    @JsonProperty("available_retail_outlets")
    private Object[] availableRetailOutlets;
    
    @JsonProperty("available_ewallets")
    private Object[] availableEwallets;
    
    @JsonProperty("should_exclude_credit_card")
    private Boolean shouldExcludeCreditCard;
    
    @JsonProperty("should_send_email")
    private Boolean shouldSendEmail;
    
    @JsonProperty("created")
    private String created;
    
    @JsonProperty("updated")
    private String updated;
    
    @JsonProperty("paid_at")
    private String paidAt;
    
    private String currency;
}
