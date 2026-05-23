package com.forpets.domain.chat.websocket;

import com.forpets.domain.chat.dto.ChatMessageBroadcast;
import com.forpets.domain.chat.dto.ChatMessageRequest;
import com.forpets.domain.chat.exception.ChatErrorCode;
import com.forpets.domain.chat.exception.ChatException;
import com.forpets.domain.chat.service.ChatMessageWebSocketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.Map;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatWebSocketHandler {

    private final ChatMessageWebSocketService chatMessageWebSocketService;
    private final SimpMessagingTemplate messagingTemplate;

    // 메시지 저장 후 구독자에게 전송
    @MessageMapping("/chat-rooms/{chatRoomId}/messages")
    public void sendMessage(
            @DestinationVariable Long chatRoomId,
            @Payload ChatMessageRequest request,
            SimpMessageHeaderAccessor headerAccessor
    ) {
        Long senderId = getSenderId(headerAccessor);

        ChatMessageBroadcast broadcast = chatMessageWebSocketService.saveTextMessage(
                chatRoomId,
                senderId,
                request
        );

        messagingTemplate.convertAndSend(
                "/sub/chat-rooms/" + chatRoomId,
                broadcast
        );
    }

    private Long getSenderId(SimpMessageHeaderAccessor headerAccessor) {
        Map<String, Object> sessionAttributes = headerAccessor.getSessionAttributes();

        if (sessionAttributes == null || sessionAttributes.get("memberId") == null) {
            throw new ChatException(ChatErrorCode.CHAT_WEBSOCKET_UNAUTHORIZED);
        }

        return (Long) sessionAttributes.get("memberId");
    }
}
