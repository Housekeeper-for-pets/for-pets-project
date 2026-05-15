package com.forpets.domain.sitter.dto;

import com.forpets.domain.sitter.entity.SitterProfileStatus;

public record UpdateSitterStatusRequest(
        SitterProfileStatus status
) {}
