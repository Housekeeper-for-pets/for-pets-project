package com.forpets.domain.notification.service;

import com.forpets.domain.notification.dto.NotificationRealtimeMessage;
import com.forpets.domain.notification.entity.Notification;
import com.forpets.domain.notification.exception.NotificationErrorCode;
import com.forpets.domain.notification.exception.NotificationException;
import com.forpets.domain.notification.pubsub.NotificationRedisPublisher;
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
    private final NotificationRedisPublisher notificationRedisPublisher;

    /**
     * 알림 생성 + 실시간 알림 broadcast 발행
     * 핵심: DB 저장은 한 인스턴스만, SSE 전송은 Redis Pub/Sub으로 모든 인스턴스가 시도
     */
    @Transactional
    public Notification notify(
            Long receiverId,
            Long senderId,
            SseEventType type,
            String message,
            Long referenceId,
            String referenceType
    ) {

        //DB 저장
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

        notificationRedisPublisher.publish(NotificationRealtimeMessage.from(notification));

        return notification;
    }

    public void sendRealtime(NotificationRealtimeMessage message) {
        sseEmitterManager.sendToUser(message.receiverId(), message.type(),
                Map.of(
                        "id", message.id(),
                        "type", message.type(),
                        "message", message.message(),
                        "referenceId", message.referenceId() != null ? message.referenceId() : "",
                        "referenceType", message.referenceType() != null ? message.referenceType() : "",
                        "createdAt", message.createdAt().toString()
                ));
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
                .orElseThrow(() -> new NotificationException(NotificationErrorCode.NOTIFICATION_NOT_FOUND));

        if (!notification.getReceiverId().equals(userId)) {
            throw new NotificationException(NotificationErrorCode.NOT_NOTIFICATION_RECEIVER);
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
