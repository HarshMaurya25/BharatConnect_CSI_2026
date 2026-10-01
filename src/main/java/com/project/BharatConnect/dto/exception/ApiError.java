package com.project.BharatConnect.dto.exception;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ApiError {
    private String keyError;
    private String valueError;
    private LocalDateTime timeStamp;
}
