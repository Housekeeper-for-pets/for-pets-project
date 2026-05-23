package com.forpets.domain.chat.service;

import com.forpets.domain.chat.dto.ChatRoomCreateResponse;
import com.forpets.domain.chat.dto.ChatRoomListItem;
import com.forpets.domain.chat.dto.ChatRoomListResponse;
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
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
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

        // 나가지 않은 채팅방 조회
        List<ChatRoomParticipant> myParticipants =
                chatRoomParticipantRepository.findAllActiveRoomsByMemberId(memberId);

        // 빈 채팅방 응답
        if (myParticipants.isEmpty()) {
            return ChatRoomListResponse.empty();
        }

        // 내 채팅방 참여 목록을 chatRoomId로 찾기
        Map<Long, ChatRoomParticipant> participantMap = myParticipants.stream()
                .collect(Collectors.toMap(ChatRoomParticipant::getChatRoomId, Function.identity()));


        List<Long> chatRoomIds = myParticipants.stream()
                .map(ChatRoomParticipant::getChatRoomId)
                .toList();

        List<ChatRoom> chatRooms = chatRoomRepository.findAllById(chatRoomIds);

        // 마지막 메시지가 있는 채팅방만 목록에 보여준다.
        // 채팅방 생성만 하고 메시지가 없으면 목록에는 표시하지 않는다.
        List<ChatRoom> filteredRooms = chatRooms.stream()
                .filter(chatRoom -> isDisplayableRoom(chatRoom, participantMap.get(chatRoom.getId())))
                .filter(chatRoom -> isBeforeCursor(chatRoom, cursorLastMessageAt, cursorChatRoomId))
                .sorted(Comparator
                        .comparing(ChatRoom::getLastMessageAt, Comparator.reverseOrder())
                        .thenComparing(ChatRoom::getId, Comparator.reverseOrder()))
                .toList();

        boolean hasNext = filteredRooms.size() > effectiveSize;

        List<ChatRoom> pagedRooms = filteredRooms.stream()
                .limit(effectiveSize)
                .toList();

        if (pagedRooms.isEmpty()) {
            return ChatRoomListResponse.empty();
        }

        Map<Long, Member> opponentMap = getOpponentMap(memberId, pagedRooms);
        Map<Long, ChatMessage> lastMessageMap = getLastMessageMap(pagedRooms);

        // ChatRoom 목록, 응답 DTO로 변환
        List<ChatRoomListItem> items = pagedRooms.stream()
                .map(chatRoom -> toChatRoomListItem(
                        memberId,
                        chatRoom,
                        participantMap.get(chatRoom.getId()),
                        opponentMap,
                        lastMessageMap
                ))
                .toList();

        ChatRoom lastRoom = pagedRooms.get(pagedRooms.size() - 1);

        return ChatRoomListResponse.builder()
                .items(items)
                .hasNext(hasNext)
                .nextCursorLastMessageAt(hasNext ? lastRoom.getLastMessageAt() : null)
                .nextCursorChatRoomId(hasNext ? lastRoom.getId() : null)
                .build();
    }

    // 목록에 보여줄 수 있는 채팅방인지 판단
    private boolean isDisplayableRoom(ChatRoom chatRoom, ChatRoomParticipant participant) {
        if (chatRoom.getLastMessageAt() == null) {
            return false;
        }

        if (participant == null || participant.isLeft()) {
            return false;
        }

        if (participant.getVisibleFromAt() == null) {
            return true;
        }

        return chatRoom.getLastMessageAt().isAfter(participant.getVisibleFromAt());
    }

    // 커서가 없으면 전체 대상이고, 커서가 있으면 커서보다 이전 채팅방만 조회 대상
    private boolean isBeforeCursor(
            ChatRoom chatRoom,
            LocalDateTime cursorLastMessageAt,
            Long cursorChatRoomId
    ) {
        if (cursorLastMessageAt == null || cursorChatRoomId == null) {
            return true;
        }

        if (chatRoom.getLastMessageAt().isBefore(cursorLastMessageAt)) {
            return true;
        }

        return chatRoom.getLastMessageAt().isEqual(cursorLastMessageAt)
                && chatRoom.getId() < cursorChatRoomId;
    }

    // 채팅방 목록에 표시할 상대 회원 정보를 조회한다.
    private Map<Long, Member> getOpponentMap(Long memberId, List<ChatRoom> chatRooms) {
        List<Long> opponentIds = chatRooms.stream()
                .map(chatRoom -> resolveOpponentId(memberId, chatRoom))
                .distinct()
                .toList();

        if (opponentIds.isEmpty()) {
            return Map.of();
        }

        return memberRepository.findAllByIdIncludingDeleted(opponentIds)
                .stream()
                .collect(Collectors.toMap(Member::getId, Function.identity()));
    }

    // 채팅방 목록에 표시할 마지막 메시지 정보를 조회한다.
    private Map<Long, ChatMessage> getLastMessageMap(List<ChatRoom> chatRooms) {
        List<Long> lastMessageIds = chatRooms.stream()
                .map(ChatRoom::getLastMessageId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if (lastMessageIds.isEmpty()) {
            return Map.of();
        }

        return chatMessageRepository.findAllById(lastMessageIds)
                .stream()
                .collect(Collectors.toMap(ChatMessage::getId, Function.identity()));
    }

    // 채팅방 Entity를 목록 응답 DTO로 변환한다.
    private ChatRoomListItem toChatRoomListItem(
            Long memberId,
            ChatRoom chatRoom,
            ChatRoomParticipant myParticipant,
            Map<Long, Member> opponentMap,
            Map<Long, ChatMessage> lastMessageMap
    ) {
        Long opponentId = resolveOpponentId(memberId, chatRoom);
        Member opponent = opponentMap.get(opponentId);
        ChatMessage lastMessage = lastMessageMap.get(chatRoom.getLastMessageId());

        long unreadCount = chatMessageRepository.countUnread(
                chatRoom.getId(),
                memberId,
                myParticipant.getLastReadMessageId(),
                myParticipant.getVisibleFromAt()
        );

        return ChatRoomListItem.builder()
                .chatRoomId(chatRoom.getId())
                .opponentId(opponentId)
                .opponentNickname(resolveOpponentNickname(opponent))
                .lastMessage(resolveLastMessageContent(lastMessage))
                .lastMessageType(lastMessage == null ? null : lastMessage.getMessageType())
                .lastMessageAt(chatRoom.getLastMessageAt())
                .unreadCount((int) unreadCount)
                .build();
    }

    // 현재 회원 기준으로 채팅 상대 회원 ID를 계산한다.
    private Long resolveOpponentId(Long memberId, ChatRoom chatRoom) {
        if (chatRoom.getMemberAId().equals(memberId)) {
            return chatRoom.getMemberBId();
        }

        return chatRoom.getMemberAId();
    }


    // 이미지 업로드 없는 버전이므로 마지막 메시지는 TEXT content 그대로 보여준다.
    private String resolveLastMessageContent(ChatMessage lastMessage) {
        if (lastMessage == null) {
            return null;
        }

        return lastMessage.getContent();
    }
}
