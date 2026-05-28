package com.forpets.domain.carelog.dto;

import com.forpets.domain.carelog.entity.CareLog;
import com.forpets.domain.carelog.entity.CareLogImage;
import java.time.LocalDateTime;
import java.util.List;

public record CareLogResponse(
        Long id,
        Long reservationId,
        Long sitterMemberId,
        String content,
        List<String> imageUrls,
        LocalDateTime createdAt
) {
    public static CareLogResponse from(CareLog careLog, List<CareLogImage> images) {
        return new CareLogResponse(
                careLog.getId(),
                careLog.getReservationId(),
                careLog.getSitterMemberId(),
                careLog.getContent(),
                images.stream().map(CareLogImage::getImageUrl).toList(),
                careLog.getCreatedAt()
        );
    }
}