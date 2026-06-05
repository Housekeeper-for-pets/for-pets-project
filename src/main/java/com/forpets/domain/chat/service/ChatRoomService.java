package com.forpets.domain.chat.service;

import com.forpets.domain.chat.dto.*;
import com.forpets.domain.chat.entity.ChatMessage;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatRoomService {


    private final MemberRepository memberRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomParticipantRepository chatRoomParticipantRepository;
    private final ChatRoomCreateHelper chatRoomCreateHelper;
    private final ChatMessageRepository chatMessageRepository;

    @Transactional
    public ChatRoomCreateResponse createOrGetChatRoom(Long currentMemberId, Long opponentId) {
        if (currentMemberId.equals(opponentId)){
            throw new ChatException(ChatErrorCode.CHAT_INVALID_OPPONENT);
        }

        Member currentMember = memberRepository.findByIdIncludingDeleted(currentMemberId)
                .orElseThrow(() -> new ChatException(ChatErrorCode.CHAT_MEMBER_NOT_FOUND));

        validateChatAvailableMember(currentMember);

        Member opponent = memberRepository.findByIdIncludingDeleted(opponentId)
                .orElseThrow(()-> new ChatException(ChatErrorCode.CHAT_OPPONENT_NOT_FOUND));

        validateChatAvailableMember(opponent);

        String roomKey = ChatRoom.generateRoomKey(currentMemberId, opponentId);

        Optional<ChatRoom> existingRoom = chatRoomRepository.findByRoomKey(roomKey);
        if (existingRoom.isPresent()){
            ChatRoom chatRoom = existingRoom.get();

            rejoinIfLeft(chatRoom.getId(), currentMemberId);

            return new ChatRoomCreateResponse(
                    chatRoom.getId(),
                    opponent.getId(),
                    resolveOpponentNickname(opponent),
                    false
            );
        }

        Long memberAId = ChatRoom.resolveMemberAId(currentMemberId, opponentId);
        Long memberBId = ChatRoom.resolveMemberBId(currentMemberId, opponentId);

        try {
            ChatRoom newChatRoom = chatRoomCreateHelper.createChatRoomWithParticipants(
                    memberAId,
                    memberBId,
                    roomKey
            );

            return new ChatRoomCreateResponse(
                    newChatRoom.getId(),
                    opponent.getId(),
                    resolveOpponentNickname(opponent),
                    true
            );
        } catch (DataIntegrityViolationException e) {
            // 동시에 같은 채팅방 생성 요청이 들어오면 충돌
            // 이미 생성된 채팅방을 다시 조회해서 반환
            ChatRoom fallbackRoom = chatRoomRepository.findByRoomKey(roomKey)
                    .orElseThrow(() -> new ChatException(ChatErrorCode.CHAT_ROOM_NOT_FOUND));

            rejoinIfLeft(fallbackRoom.getId(), currentMemberId);

            return new ChatRoomCreateResponse(
                    fallbackRoom.getId(),
                    opponent.getId(),
                    resolveOpponentNickname(opponent),
                    false
            );
        }
    }

    // ADMIN, 탈퇴 회원, 정지 회원은 채팅 못해요
    private void validateChatAvailableMember(Member member) {
        if (member.getRole() == MemberRole.ADMIN) {
            throw new ChatException(ChatErrorCode.CHAT_ADMIN_NOT_ALLOWED);
        }

        if (!member.isActive()) {
            throw new ChatException(ChatErrorCode.CHAT_MEMBER_NOT_ACTIVE);
        }
    }

    // 나간 사용자 재입장
    private void rejoinIfLeft(Long chatRoomId, Long memberId) {
        ChatRoomParticipant participant = chatRoomParticipantRepository
                .findByChatRoomIdAndMemberId(chatRoomId, memberId)
                .orElseThrow(() -> new ChatException(ChatErrorCode.CHAT_ROOM_ACCESS_DENIED));

        if (participant.isLeft()) {
            participant.rejoin();
        }
    }

    // 채팅창에서 탈퇴한 회원 표시
    private String resolveOpponentNickname(Member opponent) {
        if (opponent == null || opponent.isDeleted()) {
            return "탈퇴한 회원";
        }

        return opponent.getNickname();
    }

    // 로그인한 회원이 참여 중인 채팅방 목록을 조회한다.
    public ChatRoomListResponse getChatRoomList(
            Long memberId,
            LocalDateTime cursorLastMessageAt,
            Long cursorChatRoomId,
            int size
    ) {
        int effectiveSize = Math.min(Math.max(size, 1), 50);

        // DB에서 정렬·페이징·unreadCount 집계를 한 번에 처리
        // size + 1개를 가져와서 다음 페이지 존재 여부를 판단
        List<ChatRoomSummary> summaries = chatRoomParticipantRepository.findChatRoomSummaries(
                memberId,
                cursorLastMessageAt,
                cursorChatRoomId,
                PageRequest.of(0, effectiveSize + 1)
        );

        if (summaries.isEmpty()) {
            return ChatRoomListResponse.empty();
        }

        boolean hasNext = summaries.size() > effectiveSize;
        List<ChatRoomSummary> pagedSummaries = summaries.subList(0, Math.min(effectiveSize, summaries.size()));

        // 상대방 회원 정보를 한 번에 조회
        List<Long> opponentIds = pagedSummaries.stream()
                .map(s -> resolveOpponentIdFromSummary(memberId, s))
                .distinct()
                .toList();

        Map<Long, Member> opponentMap = memberRepository.findAllByIdIncludingDeleted(opponentIds)
                .stream()
                .collect(Collectors.toMap(Member::getId, Function.identity()));

        // 마지막 메시지 내용을 한 번에 조회
        List<Long> lastMessageIds = pagedSummaries.stream()
                .map(ChatRoomSummary::lastMessageId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<Long, ChatMessage> lastMessageMap = chatMessageRepository.findAllById(lastMessageIds)
                .stream()
                .collect(Collectors.toMap(ChatMessage::getId, Function.identity()));

        List<ChatRoomListItem> items = pagedSummaries.stream()
                .map(s -> toChatRoomListItem(memberId, s, opponentMap, lastMessageMap))
                .toList();

        ChatRoomSummary lastSummary = pagedSummaries.get(pagedSummaries.size() - 1);

        return ChatRoomListResponse.builder()
                .items(items)
                .hasNext(hasNext)
                .nextCursorLastMessageAt(hasNext ? lastSummary.lastMessageAt() : null)
                .nextCursorChatRoomId(hasNext ? lastSummary.chatRoomId() : null)
                .build();
    }

    // ChatRoomSummary 기준으로 상대방 ID를 계산
    private Long resolveOpponentIdFromSummary(Long memberId, ChatRoomSummary summary) {
        return summary.memberAId().equals(memberId)
                ? summary.memberBId()
                : summary.memberAId();
    }

    // ChatRoomSummary를 목록 응답 DTO로 변환
    private ChatRoomListItem toChatRoomListItem(
            Long memberId,
            ChatRoomSummary summary,
            Map<Long, Member> opponentMap,
            Map<Long, ChatMessage> lastMessageMap
    ) {
        Long opponentId = resolveOpponentIdFromSummary(memberId, summary);
        Member opponent = opponentMap.get(opponentId);
        ChatMessage lastMessage = lastMessageMap.get(summary.lastMessageId());

        return ChatRoomListItem.builder()
                .chatRoomId(summary.chatRoomId())
                .opponentId(opponentId)
                .opponentNickname(resolveOpponentNickname(opponent))
                .lastMessage(resolveLastMessageContent(lastMessage))
                .lastMessageType(lastMessage == null ? null : lastMessage.getMessageType())
                .lastMessageAt(summary.lastMessageAt())
                .unreadCount((int) summary.unreadCount())
                .build();
    }

    // 이미지 업로드 없는 버전이므로 마지막 메시지는 TEXT content 그대로 보여준다.
    private String resolveLastMessageContent(ChatMessage lastMessage) {
        if (lastMessage == null) {
            return null;
        }

        return lastMessage.getContent();
    }

    // 채팅방 나가기
    @Transactional
    public ChatRoomLeaveResponse leaveChatRoom(Long memberId, Long chatRoomId) {
        ChatRoomParticipant participant = chatRoomParticipantRepository
                .findByChatRoomIdAndMemberId(chatRoomId, memberId)
                .orElseThrow(() -> new ChatException(ChatErrorCode.CHAT_ROOM_ACCESS_DENIED));

        // 이미 나감
        if (participant.isLeft()) {
            return new ChatRoomLeaveResponse(
                    chatRoomId,
                    participant.isLeft(),
                    participant.getLeftAt(),
                    participant.getVisibleFromAt()
            );
        }

        // 찐 나가기
        LocalDateTime now = LocalDateTime.now();
        participant.leave(now);

        return new ChatRoomLeaveResponse(
                chatRoomId,
                participant.isLeft(),
                participant.getLeftAt(),
                participant.getVisibleFromAt()
        );
    }
}
