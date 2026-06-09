package com.forpets.global.config;

import com.forpets.domain.chat.redis.ChatRedisSubscriber;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

@Configuration
@Profile("!test")
@RequiredArgsConstructor
public class ChatPubSubConfig {

    private final ChatRedisSubscriber chatRedisSubscriber;

    @Bean
    public RedisMessageListenerContainer chatMessageListenerContainer(
            RedisConnectionFactory connectionFactory
    ) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        // "chat-room:*" 패턴으로 모든 채팅방 토픽 구독
        container.addMessageListener(chatRedisSubscriber, new PatternTopic("chat-room:*"));
        return container;
    }
}