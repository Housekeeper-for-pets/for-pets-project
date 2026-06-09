package com.forpets.domain.notification.broker;

import com.forpets.domain.notification.event.NotificationEvent;

public interface NotificationMessageBroker {

    void publish(NotificationEvent event);
}