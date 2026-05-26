package com.forpets.domain.chat.websocket;

import com.forpets.domain.chat.entity.ChatRoomParticipant;
import com.forpets.domain.chat.exception.ChatErrorCode;
import com.forpets.domain.chat.repository.ChatRoomParticipantRepository;
import com.forpets.domain.member.entity.MemberRole;
import com.forpets.global.security.jwt.JwtTokenProvider;
import com.forpets.global.security.jwt.TokenRedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class StompChannelInterceptor implements ChannelInterceptor {

    private final JwtTokenProvider jwtTokenProvider;
    private final TokenRedisService tokenRedisService;
    private final ChatRoomParticipantRepository chatRoomParticipantRepository;

    // STOMP 명령별 인증 처리
    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }

        StompCommand command = accessor.getCommand();

        switch (command) {
            case CONNECT -> handleConnect(accessor, message);
            case SUBSCRIBE -> handleSubscribe(accessor, message);
            case SEND -> validateSession(accessor, message);
            default -> {
            }
        }

        return message;
    }

    // CONNECT 시 JWT 검증
    private void handleConnect(StompHeaderAccessor accessor, Message<?> message) {
        String authorizationHeader = accessor.getFirstNativeHeader("Authorization");

        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throwUnauthorized(message);
        }

        String token = authorizationHeader.substring(7);

        try {
            if (tokenRedisService.isBlacklisted(token)) {
                throwUnauthorized(message);
            }

            jwtTokenProvider.validateToken(token);

            Long memberId = jwtTokenProvider.getMemberId(token);
            MemberRole role = jwtTokenProvider.getRole(token);
            long remainingTime = jwtTokenProvider.getRemainingTime(token);

            Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
            if (sessionAttributes == null) {
                sessionAttributes = new HashMap<>();
            }

            sessionAttributes.put("memberId", memberId);
            sessionAttributes.put("role", role);
            sessionAttributes.put("token", token);
            sessionAttributes.put("tokenExpiresAt", System.currentTimeMillis() + remainingTime);

            accessor.setSessionAttributes(sessionAttributes);

            log.info("[WebSocket CONNECT] memberId={}, role={}", memberId, role);
        } catch (MessageDeliveryException e) {
            throw e;
        } catch (Exception e) {
            throwUnauthorized(message);
        }
    }

    // 구독 채팅방 권한 확인
    private void handleSubscribe(StompHeaderAccessor accessor, Message<?> message) {
        validateSession(accessor, message);

        String destination = accessor.getDestination();
        if (destination == null || !destination.startsWith("/sub/chat-rooms/")) {
            return;
        }

        Long memberId = getMemberIdFromSession(accessor, message);
        Long chatRoomId = extractChatRoomId(destination, message);

        ChatRoomParticipant participant = chatRoomParticipantRepository
                .findByChatRoomIdAndMemberId(chatRoomId, memberId)
                .orElseThrow(() -> new MessageDeliveryException(
                        message,
                        ChatErrorCode.CHAT_ROOM_ACCESS_DENIED.getMessage()
                ));

        if (participant.isLeft()) {
            throw new MessageDeliveryException(
                    message,
                    ChatErrorCode.CHAT_ROOM_LEFT.getMessage()
            );
        }

        log.info("[WebSocket SUBSCRIBE] memberId={}, chatRoomId={}", memberId, chatRoomId);
    }

    // 세션 토큰 검증
    private void validateSession(StompHeaderAccessor accessor, Message<?> message) {
        Map<String, Object> sessionAttributes = accessor.getSessionAttributes();

        if (sessionAttributes == null) {
            throwUnauthorized(message);
        }

        Long tokenExpiresAt = (Long) sessionAttributes.get("tokenExpiresAt");
        if (tokenExpiresAt == null || System.currentTimeMillis() > tokenExpiresAt) {
            throwUnauthorized(message);
        }

        String token = (String) sessionAttributes.get("token");
        if (token == null || tokenRedisService.isBlacklisted(token)) {
            throwUnauthorized(message);
        }
    }

    private Long getMemberIdFromSession(StompHeaderAccessor accessor, Message<?> message) {
        Map<String, Object> sessionAttributes = accessor.getSessionAttributes();

        if (sessionAttributes == null || sessionAttributes.get("memberId") == null) {
            throwUnauthorized(message);
        }

        return (Long) sessionAttributes.get("memberId");
    }

    private Long extractChatRoomId(String destination, Message<?> message) {
        try {
            String[] parts = destination.split("/");
            return Long.parseLong(parts[3]);
        } catch (Exception e) {
            throw new MessageDeliveryException(message, "잘못된 구독 경로입니다.");
        }
    }

    private void throwUnauthorized(Message<?> message) {
        throw new MessageDeliveryException(
                message,
                ChatErrorCode.CHAT_WEBSOCKET_UNAUTHORIZED.getMessage()
        );
    }
}
