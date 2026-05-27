package com.forpets.domain.chat.service;

import com.forpets.domain.chat.dto.ChatMessageItem;
import com.forpets.domain.chat.dto.ChatMessageListResponse;
import com.forpets.domain.chat.entity.ChatMessage;
import com.forpets.domain.chat.entity.ChatRoomParticipant;
import com.forpets.domain.chat.exception.ChatErrorCode;
import com.forpets.domain.chat.exception.ChatException;
import com.forpets.domain.chat.repository.ChatMessageRepository;
import com.forpets.domain.chat.repository.ChatRoomParticipantRepository;
import com.forpets.domain.chat.repository.ChatRoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatMessageService {

    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomParticipantRepository chatRoomParticipantRepository;
    private final ChatRoomRepository chatRoomRepository;

    // 메세지 목록 조회
    @Transactional
    public ChatMessageListResponse getMessages(
            Long memberId,
            Long chatRoomId,
            Long cursorId,
            int size
    ) {
        int effectiveSize = Math.min(Math.max(size, 1), 50);

        chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new ChatException(ChatErrorCode.CHAT_ROOM_NOT_FOUND));

        ChatRoomParticipant myParticipant = chatRoomParticipantRepository
                .findByChatRoomIdAndMemberId(chatRoomId, memberId)
                .orElseThrow(() -> new ChatException(ChatErrorCode.CHAT_ROOM_ACCESS_DENIED));

        if (myParticipant.isLeft()) {
            throw new ChatException(ChatErrorCode.CHAT_ROOM_LEFT);
        }

        ChatRoomParticipant opponentParticipant = findOpponentParticipant(chatRoomId, memberId);

        // DB에서는 최신 메시지부터 조회
        List<ChatMessage> messages = findMessages(
                chatRoomId,
                cursorId,
                myParticipant.getVisibleFromAt(),
                effectiveSize + 1
        );

        // 다음 페이지 여부
        boolean hasNext = messages.size() > effectiveSize;

        List<ChatMessage> pagedMessages = new ArrayList<>(
                messages.subList(0, Math.min(effectiveSize, messages.size()))
        );

        Long nextCursorId = resolveNextCursorId(hasNext, pagedMessages);

        // 조회한 메시지 중 가장 큰 ID를 현재 회원의 마지막 읽음 위치로 저장한다.
        updateReadPosition(myParticipant, pagedMessages);

        // 응답은 오래된 메시지, 최신 메시지 순서로 내려준다.
        Collections.reverse(pagedMessages);

        List<ChatMessageItem> items = pagedMessages.stream()
                .map(message -> toMessageItem(memberId, opponentParticipant, message))
                .toList();

        return ChatMessageListResponse.builder()
                .items(items)
                .hasNext(hasNext)
                .nextCursorId(nextCursorId)
                .build();
    }

    // cursorId가 없으면 최신 메시지를 조회하고, cursorId가 있으면 그 이전 메시지를 조회한다.
    private List<ChatMessage> findMessages(
            Long chatRoomId,
            Long cursorId,
            LocalDateTime visibleFromAt,
            int limit
    ) {
        PageRequest pageRequest = PageRequest.of(0, limit);

        if (cursorId == null) {
            return chatMessageRepository.findLatestMessages(
                    chatRoomId,
                    visibleFromAt,
                    pageRequest
            );
        }

        return chatMessageRepository.findMessagesBeforeCursor(
                chatRoomId,
                cursorId,
                visibleFromAt,
                pageRequest
        );
    }

    // 채팅방 참여자 중 현재 회원이 아닌 상대방 참여자를 찾는다.
    private ChatRoomParticipant findOpponentParticipant(Long chatRoomId, Long memberId) {
        return chatRoomParticipantRepository.findAllByChatRoomId(chatRoomId)
                .stream()
                .filter(participant -> !participant.getMemberId().equals(memberId))
                .findFirst()
                .orElseThrow(() -> new ChatException(ChatErrorCode.CHAT_ROOM_NOT_FOUND));
    }

    // 다음 페이지가 있으면 현재 페이지의 가장 오래된 메시지 ID를 다음 커서로 사용한다.
    private Long resolveNextCursorId(boolean hasNext, List<ChatMessage> pagedMessages) {
        if (!hasNext || pagedMessages.isEmpty()) {
            return null;
        }

        return pagedMessages.get(pagedMessages.size() - 1).getId();
    }

    // 메시지를 조회한 사용자의 읽음 위치를 갱신한다.
    private void updateReadPosition(
            ChatRoomParticipant myParticipant,
            List<ChatMessage> pagedMessages
    ) {
        if (pagedMessages.isEmpty()) {
            return;
        }

        Long maxMessageId = pagedMessages.stream()
                .map(ChatMessage::getId)
                .max(Long::compareTo)
                .orElse(null);

        myParticipant.updateLastRead(maxMessageId, LocalDateTime.now());
    }

    // ChatMessage Entity를 메시지 조회 응답 DTO로 변환한다.
    private ChatMessageItem toMessageItem(
            Long memberId,
            ChatRoomParticipant opponentParticipant,
            ChatMessage message
    ) {
        boolean isMine = message.getSenderId().equals(memberId);

        return ChatMessageItem.builder()
                .messageId(message.getId())
                .messageType(message.getMessageType())
                .senderId(message.getSenderId())
                .senderNickname(message.getSenderNickname())
                .content(message.getContent())
                .createdAt(message.getCreatedAt())
                .isMine(isMine)
                .isReadByOpponent(resolveIsReadByOpponent(isMine, opponentParticipant, message))
                .build();
    }

    // 내가 보낸 메시지이고, 상대방의 마지막 읽은 메시지 ID가 현재 메시지 ID 이상이면 읽은 것으로 판단한다.
    private boolean resolveIsReadByOpponent(
            boolean isMine,
            ChatRoomParticipant opponentParticipant,
            ChatMessage message
    ) {
        if (!isMine) {
            return false;
        }

        Long opponentLastReadMessageId = opponentParticipant.getLastReadMessageId();

        return opponentLastReadMessageId != null
                && opponentLastReadMessageId >= message.getId();
    }
}