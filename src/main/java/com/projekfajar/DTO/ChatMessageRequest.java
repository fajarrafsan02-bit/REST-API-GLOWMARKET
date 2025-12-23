package com.projekfajar.DTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageRequest {
    
    @NotNull(message = "Receiver ID tidak boleh kosong")
    private Long receiverId;
    
    @NotBlank(message = "Pesan tidak boleh kosong")
    private String message;
}
