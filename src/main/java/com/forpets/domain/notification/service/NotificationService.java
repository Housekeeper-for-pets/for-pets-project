package com.forpets.domain.notification.service;

import com.forpets.domain.notification.entity.Notification;
import com.forpets.domain.notification.repository.NotificationRepository;
import com.forpets.global.sse.SseEmitterManager;
import com.forpets.global.sse.SseEventType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final SseEmitterManager sseEmitterManager;

    /**
     * 알림 생성 + SSE 전송
     * 핵심: DB 먼저 저장 → SSE는 "있으면 보내고 없으면 말고"
     */
    @Transactional
    public Notification notify(Long receiverId, Long senderId,
                               SseEventType type, String message,
                               Long referenceId, String referenceType) {

        // 1. DB 저장 (영속 보장)
        Notification notification = Notification.builder()
                .receiverId(receiverId)
                .senderId(senderId)
                .type(type)
                .message(message)
                .referenceId(referenceId)
                .referenceType(referenceType)
                .build();

        notificationRepository.save(notification);
        log.info("알림 저장: type={}, receiver={}", type, receiverId);

        // 2. SSE 전송 (연결 안 돼있으면 스킵)
        sseEmitterManager.sendToUser(receiverId, type.name(),
                Map.of(
                        "id", notification.getId(),
                        "type", type.name(),
                        "message", message,
                        "referenceId", referenceId != null ? referenceId : "",
                        "referenceType", referenceType != null ? referenceType : "",
                        "createdAt", notification.getCreatedAt().toString()
                ));

        return notification;
    }

    /**
     * 내 알림 목록 조회
     */
    @Transactional(readOnly = true)
    public List<Notification> getMyNotifications(Long userId) {
        return notificationRepository
                .findByReceiverIdOrderByCreatedAtDesc(userId);
    }

    /**
     * 미읽음 알림만 조회
     */
    @Transactional(readOnly = true)
    public List<Notification> getUnreadNotifications(Long userId) {
        return notificationRepository
                .findByReceiverIdAndIsReadFalseOrderByCreatedAtDesc(userId);
    }

    /**
     * 읽음 처리
     */
    @Transactional
    public void markAsRead(Long notificationId, Long userId) {
        Notification notification = notificationRepository
                .findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("알림 없음"));

        if (!notification.getReceiverId().equals(userId)) {
            throw new IllegalArgumentException("본인 알림만 읽음 처리 가능");
        }

        notification.markAsRead();
    }

    /**
     * 미읽음 개수
     */
    @Transactional(readOnly = true)
    public long getUnreadCount(Long userId) {
        return notificationRepository
                .countByReceiverIdAndIsReadFalse(userId);
    }
}