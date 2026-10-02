package com.project.BharatConnect.dto.content;

import java.util.List;
import java.util.UUID;

public record QuizAnswerResponseDto(
        boolean correct,
        UUID correctOptionId,
        String explanation,
        long totalAnswers,
        long correctAnswers,
        List<ContentResponseDto.QuizOptionDto> options
) {}
