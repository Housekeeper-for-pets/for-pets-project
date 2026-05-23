package com.forpets.domain.chat.service;

import com.forpets.domain.chat.dto.ChatMessageBroadcast;
import com.forpets.domain.chat.dto.ChatMessageRequest;
import com.forpets.domain.chat.entity.ChatMessage;
import com.forpets.domain.chat.entity.ChatMessageType;
import com.forpets.domain.chat.entity.ChatRoom;
import com.forpets.domain.chat.entity.ChatRoomParticipant;
import com.forpets.domain.chat.exception.ChatErrorCode;
import com.forpets.domain.chat.exception.ChatException;
import com.forpets.domain.chat.repository.ChatMessageRepository;
import com.forpets.domain.chat.repository.ChatRoomParticipantRepository;
import com.forpets.domain.chat.repository.ChatRoomRepository;
import com.forpets.domain.member.entity.Member;
import com.forpets.domain.member.entity.MemberRole;
import com.forpets.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatMessageWebSocketService {

    private static final int MAX_TEXT_LENGTH = 1000;

    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomParticipantRepository chatRoomParticipantRepository;
    private final MemberRepository memberRepository;

    // TEXT 메시지 저장
    @Transactional
    public ChatMessageBroadcast saveTextMessage(
            Long chatRoomId,
            Long senderId,
            ChatMessageRequest request
    ) {
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new ChatException(ChatErrorCode.CHAT_ROOM_NOT_FOUND));

        ChatRoomParticipant senderParticipant = chatRoomParticipantRepository
                .findByChatRoomIdAndMemberId(chatRoomId, senderId)
                .orElseThrow(() -> new ChatException(ChatErrorCode.CHAT_ROOM_ACCESS_DENIED));

        if (senderParticipant.isLeft()) {
            throw new ChatException(ChatErrorCode.CHAT_ROOM_LEFT);
        }

        Member sender = memberRepository.findByIdIncludingDeleted(senderId)
                .orElseThrow(() -> new ChatException(ChatErrorCode.CHAT_MEMBER_NOT_FOUND));
        validateChatAvailableMember(sender);

        ChatRoomParticipant opponentParticipant = findOpponentParticipant(chatRoomId, senderId);

        Member opponent = memberRepository.findByIdIncludingDeleted(opponentParticipant.getMemberId())
                .orElseThrow(() -> new ChatException(ChatErrorCode.CHAT_OPPONENT_NOT_FOUND));
        validateChatAvailableMember(opponent);

        String content = validateTextContent(request.content());

        ChatMessage message = chatMessageRepository.save(ChatMessage.builder()
                .chatRoomId(chatRoomId)
                .senderId(senderId)
                .senderNickname(sender.getNickname())
                .messageType(ChatMessageType.TEXT)
                .content(content)
                .build());

        LocalDateTime createdAt = message.getCreatedAt();
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }

        chatRoom.updateLastMessage(message.getId(), createdAt);

        if (opponentParticipant.isLeft()) {
            opponentParticipant.rejoin();
        }

        log.info("[WebSocket TEXT] chatRoomId={}, senderId={}, messageId={}",
                chatRoomId,
                senderId,
                message.getId()
        );

        return ChatMessageBroadcast.builder()
                .chatRoomId(chatRoomId)
                .messageId(message.getId())
                .messageType(ChatMessageType.TEXT)
                .senderId(senderId)
                .senderNickname(sender.getNickname())
                .content(message.getContent())
                .createdAt(createdAt)
                .build();
    }

    private ChatRoomParticipant findOpponentParticipant(Long chatRoomId, Long senderId) {
        List<ChatRoomParticipant> participants =
                chatRoomParticipantRepository.findAllByChatRoomId(chatRoomId);

        return participants.stream()
                .filter(participant -> !participant.getMemberId().equals(senderId))
                .findFirst()
                .orElseThrow(() -> new ChatException(ChatErrorCode.CHAT_ROOM_NOT_FOUND));
    }

    private void validateChatAvailableMember(Member member) {
        if (member.getRole() == MemberRole.ADMIN) {
            throw new ChatException(ChatErrorCode.CHAT_ADMIN_NOT_ALLOWED);
        }

        if (!member.isActive()) {
            throw new ChatException(ChatErrorCode.CHAT_MEMBER_NOT_ACTIVE);
        }
    }

    private String validateTextContent(String content) {
        if (content == null) {
            throw new ChatException(ChatErrorCode.CHAT_EMPTY_MESSAGE);
        }

        String trimmedContent = content.trim();

        if (trimmedContent.isEmpty()) {
            throw new ChatException(ChatErrorCode.CHAT_EMPTY_MESSAGE);
        }

        if (trimmedContent.length() > MAX_TEXT_LENGTH) {
            throw new ChatException(ChatErrorCode.CHAT_MESSAGE_TOO_LONG);
        }

        return trimmedContent;
    }
}
