package com.forpets.domain.chat.service;

import com.forpets.domain.chat.dto.ChatRoomCreateResponse;
import com.forpets.domain.chat.entity.ChatRoom;
import com.forpets.domain.chat.entity.ChatRoomParticipant;
import com.forpets.domain.chat.exception.ChatErrorCode;
import com.forpets.domain.chat.exception.ChatException;
import com.forpets.domain.chat.repository.ChatRoomParticipantRepository;
import com.forpets.domain.chat.repository.ChatRoomRepository;
import com.forpets.domain.member.entity.Member;
import com.forpets.domain.member.entity.MemberRole;
import com.forpets.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatRoomService {


    private final MemberRepository memberRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomParticipantRepository chatRoomParticipantRepository;
    private final ChatRoomCreateHelper chatRoomCreateHelper;

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
}
