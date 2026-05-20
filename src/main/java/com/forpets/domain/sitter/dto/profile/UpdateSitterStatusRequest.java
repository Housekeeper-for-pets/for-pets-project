package com.forpets.domain.sitter.dto.profile;

import com.forpets.domain.sitter.entity.SitterProfileStatus;

public record UpdateSitterStatusRequest(
        SitterProfileStatus status
) {}
