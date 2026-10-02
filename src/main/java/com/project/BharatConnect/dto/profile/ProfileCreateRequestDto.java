package com.project.BharatConnect.dto.profile;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ProfileCreateRequestDto {

    @NotBlank
    @Size(max = 100)
    private String displayName;

    @Size(max = 255)
    private String currentStatus;

    @Size(max = 1000)
    private String bio;

    @Size(max = 150)
    private String organization;
}