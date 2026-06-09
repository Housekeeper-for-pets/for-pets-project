package com.forpets.domain.carelog.service;

import com.forpets.domain.carelog.dto.CareLogResponse;
import com.forpets.domain.carelog.dto.CreateCareLogRequest;
import com.forpets.domain.carelog.entity.CareLog;
import com.forpets.domain.carelog.entity.CareLogImage;
import com.forpets.domain.carelog.exception.CareLogErrorCode;
import com.forpets.domain.carelog.exception.CareLogException;
import com.forpets.domain.carelog.repository.CareLogImageRepository;
import com.forpets.domain.carelog.repository.CareLogRepository;
import com.forpets.domain.notification.broker.NotificationMessageBroker;
import com.forpets.domain.notification.event.NotificationEvent;
import com.forpets.domain.notification.service.NotificationService;
import com.forpets.domain.reservation.entity.Reservation;
import com.forpets.domain.reservation.entity.ReservationStatus;
import com.forpets.domain.reservation.repository.ReservationRepository;
import com.forpets.global.sse.SseEventType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CareLogService {

    private static final String NAME = CareLogService.class.getSimpleName();

    private final CareLogRepository careLogRepository;
    private final CareLogImageRepository careLogImageRepository;
    private final ReservationRepository reservationRepository;

    private final NotificationMessageBroker notificationBroker;

    @Transactional
    public CareLogResponse create(Long sitterMemberId, Long reservationId,
                                  CreateCareLogRequest request) {

        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new CareLogException(CareLogErrorCode.RESERVATION_NOT_FOUND));

        if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new CareLogException(CareLogErrorCode.INVALID_RESERVATION_STATUS);

        }

        log.info("{} => create { reservationId: {}, getSitterMemberId: {}, sitterMemberId: {}}"
                , NAME, reservationId, reservation.getSitterMemberId(), sitterMemberId);
        if (!reservation.getSitterMemberId().equals(sitterMemberId)) {
            throw new CareLogException(CareLogErrorCode.NOT_SITTER_OF_RESERVATION);
        }

        // CareLog 저장
        CareLog careLog = careLogRepository.save(
                CareLog.builder()
                        .reservationId(reservationId)
                        .sitterMemberId(sitterMemberId)
                        .content(request.content())
                        .build()
        );

        // CareLogImage 저장 (있으면)
        List<CareLogImage> images = List.of();
        if (request.imageUrls() != null && !request.imageUrls().isEmpty()) {
            images = request.imageUrls().stream()
                    .map(url -> CareLogImage.builder()
                            .careLogId(careLog.getId())
                            .imageUrl(url)
                            .build())
                    .toList();
            careLogImageRepository.saveAll(images);
        }

        log.info("{} => 케어일지 등록 -> reservationId={}, sitterMemberId={}, imageCount={}", NAME, reservationId, sitterMemberId, images.size());

        // SSE로 보호자에게 실시간 알림
        notificationBroker.publish(
                NotificationEvent.of(
                        reservation.getGuardianId(),
                        sitterMemberId,
                        SseEventType.CARE_LOG,
                        "시터님이 케어 일지를 등록했습니다.",
                        careLog.getId(),
                        "CARE_LOG"
                )
        );
        return CareLogResponse.from(careLog, images);
    }

    @Transactional(readOnly = true)
    public List<CareLogResponse> getByReservation(Long memberId, Long reservationId) {

        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new CareLogException(CareLogErrorCode.RESERVATION_NOT_FOUND));

        log.info("{} => 케어일지 조회 시도 -> { memberId={}, reservationId={}, guardianId={}, sitterMemberId={} }",
                NAME, memberId, reservationId, reservation.getGuardianId(), reservation.getSitterMemberId());
        if (!reservation.getGuardianId().equals(memberId) && !reservation.getSitterMemberId().equals(memberId)) {
            throw new CareLogException(CareLogErrorCode.NOT_RESERVATION_PARTICIPANT);
        }

        // 일지 목록 조회
        List<CareLog> careLogs = careLogRepository
                .findByReservationIdOrderByCreatedAtDesc(reservationId);
        if (careLogs.isEmpty()) return List.of();

        // 이미지 한번에 조회 (N+1 방지)
        List<Long> careLogIds = careLogs.stream()
                .map(CareLog::getId)
                .toList();

        Map<Long, List<CareLogImage>> imageMap = careLogImageRepository
                .findByCareLogIdIn(careLogIds)
                .stream()
                .collect(Collectors.groupingBy(CareLogImage::getCareLogId));

        return careLogs.stream()
                .map(log -> CareLogResponse.from(log, imageMap.getOrDefault(log.getId(), List.of())))
                .toList();
    }
}