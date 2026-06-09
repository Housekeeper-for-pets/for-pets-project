package com.forpets.domain.chat.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.forpets.domain.chat.dto.ChatMessageBroadcast;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatRedisSubscriber implements MessageListener {

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    // Redis에서 메시지 수신 → WebSocket 구독자에게 전달
    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String payload = new String(message.getBody());
            String channel = new String(message.getChannel());

            ChatMessageBroadcast broadcast = objectMapper.readValue(payload, ChatMessageBroadcast.class);

            // "chat-room:1" → "/sub/chat-rooms/1"
            String chatRoomId = channel.replace("chat-room:", "");
            messagingTemplate.convertAndSend("/sub/chat-rooms/" + chatRoomId, broadcast);

            log.debug("[ChatRedis] SUBSCRIBE channel={}, messageId={}", channel, broadcast.messageId());
        } catch (Exception e) {
            log.error("[ChatRedis] 메시지 처리 실패: channel={}", new String(message.getChannel()), e);
        }
    }
}