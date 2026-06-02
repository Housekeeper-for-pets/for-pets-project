package com.forpets.domain.notification.broker;

import com.forpets.domain.notification.event.NotificationEvent;
import com.forpets.domain.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(
        name = "spring.notifications.broker",
        havingValue = "direct",
        matchIfMissing = true    // 설정 없으면 기본값으로 사용
)
@RequiredArgsConstructor
public class DirectNotificationBroker implements NotificationMessageBroker {

    private static final String NAME = DirectNotificationBroker.class.getSimpleName();

    private final NotificationService notificationService;

    @Override
    public void publish(NotificationEvent event) {
        log.info("{} => 알림 직접 발행: type={}, receiver={}",
                NAME, event.getType(), event.getReceiverId());

        notificationService.notify(
                event.getReceiverId(),
                event.getSenderId(),
                event.getType(),
                event.getMessage(),
                event.getReferenceId(),
                event.getReferenceType()
        );
    }
}
