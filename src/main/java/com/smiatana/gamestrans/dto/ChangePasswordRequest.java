package com.smiatana.gamestrans.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChangePasswordRequest {
    @NotBlank
    @Size(min = 6)
    String oldPassword;
    @NotBlank
    @Size(min = 6)
    String newPassword;
}
