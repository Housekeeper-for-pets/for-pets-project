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
import com.forpets.domain.member.entity.MemberGender;
import com.forpets.domain.member.entity.MemberRole;
import com.forpets.domain.member.entity.MemberStatus;
import com.forpets.domain.member.entity.Region;
import com.forpets.domain.member.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatMessageWebSocketService 단위 테스트")
class ChatMessageWebSocketServiceTest {

    @InjectMocks
    private ChatMessageWebSocketService chatMessageWebSocketService;

    @Mock private ChatMessageRepository chatMessageRepository;
    @Mock private ChatRoomRepository chatRoomRepository;
    @Mock private ChatRoomParticipantRepository chatRoomParticipantRepository;
    @Mock private MemberRepository memberRepository;

    private static final Long CHAT_ROOM_ID = 10L;
    private static final Long SENDER_ID = 1L;
    private static final Long OPPONENT_ID = 2L;

    private ChatRoom chatRoom;
    private Member sender;
    private Member opponent;
    private ChatRoomParticipant senderParticipant;
    private ChatRoomParticipant opponentParticipant;
    private ChatMessage savedMessage;

    @BeforeEach
    void setUp() {
        chatRoom = ChatRoom.builder().build();
        ReflectionTestUtils.setField(chatRoom, "id", CHAT_ROOM_ID);

        sender = Member.builder()
                .email("sender@example.com")
                .password("pw")
                .nickname("발신자")
                .gender(MemberGender.MALE)
                .region(Region.GANGNAM)
                .build();
        ReflectionTestUtils.setField(sender, "id", SENDER_ID);

        opponent = Member.builder()
                .email("opponent@example.com")
                .password("pw")
                .nickname("수신자")
                .gender(MemberGender.FEMALE)
                .region(Region.GANGNAM)
                .build();
        ReflectionTestUtils.setField(opponent, "id", OPPONENT_ID);

        senderParticipant = ChatRoomParticipant.builder()
                .chatRoomId(CHAT_ROOM_ID)
                .memberId(SENDER_ID)
                .build();

        opponentParticipant = ChatRoomParticipant.builder()
                .chatRoomId(CHAT_ROOM_ID)
                .memberId(OPPONENT_ID)
                .build();

        savedMessage = ChatMessage.builder()
                .chatRoomId(CHAT_ROOM_ID)
                .senderId(SENDER_ID)
                .senderNickname("발신자")
                .messageType(ChatMessageType.TEXT)
                .content("안녕하세요")
                .build();
        ReflectionTestUtils.setField(savedMessage, "id", 100L);
        ReflectionTestUtils.setField(savedMessage, "createdAt", LocalDateTime.now());
    }

    // =============================================
    // saveTextMessage
    // =============================================
    @Nested
    @DisplayName("텍스트 메시지 저장 — saveTextMessage()")
    class SaveTextMessageTest {

        @Test
        @DisplayName("[성공] 정상 메시지 저장 후 broadcast DTO를 반환한다")
        void saveTextMessage_success() {
            ChatMessageRequest request = new ChatMessageRequest("안녕하세요");

            given(chatRoomRepository.findById(CHAT_ROOM_ID)).willReturn(Optional.of(chatRoom));
            given(chatRoomParticipantRepository.findByChatRoomIdAndMemberId(CHAT_ROOM_ID, SENDER_ID))
                    .willReturn(Optional.of(senderParticipant));
            given(memberRepository.findByIdIncludingDeleted(SENDER_ID)).willReturn(Optional.of(sender));
            given(chatRoomParticipantRepository.findAllByChatRoomId(CHAT_ROOM_ID))
                    .willReturn(List.of(senderParticipant, opponentParticipant));
            given(memberRepository.findByIdIncludingDeleted(OPPONENT_ID)).willReturn(Optional.of(opponent));
            given(chatMessageRepository.save(any(ChatMessage.class))).willReturn(savedMessage);

            ChatMessageBroadcast broadcast = chatMessageWebSocketService.saveTextMessage(
                    CHAT_ROOM_ID, SENDER_ID, request);

            assertThat(broadcast.chatRoomId()).isEqualTo(CHAT_ROOM_ID);
            assertThat(broadcast.senderId()).isEqualTo(SENDER_ID);
            assertThat(broadcast.content()).isEqualTo("안녕하세요");
            assertThat(broadcast.messageType()).isEqualTo(ChatMessageType.TEXT);
        }

        @Test
        @DisplayName("[성공] 퇴장한 상대방에게 메시지 전송 시 상대방을 자동 재입장시킨다")
        void saveTextMessage_opponentRejoin() {
            ChatMessageRequest request = new ChatMessageRequest("다시 왔어요");

            opponentParticipant.leave(LocalDateTime.now());

            given(chatRoomRepository.findById(CHAT_ROOM_ID)).willReturn(Optional.of(chatRoom));
            given(chatRoomParticipantRepository.findByChatRoomIdAndMemberId(CHAT_ROOM_ID, SENDER_ID))
                    .willReturn(Optional.of(senderParticipant));
            given(memberRepository.findByIdIncludingDeleted(SENDER_ID)).willReturn(Optional.of(sender));
            given(chatRoomParticipantRepository.findAllByChatRoomId(CHAT_ROOM_ID))
                    .willReturn(List.of(senderParticipant, opponentParticipant));
            given(memberRepository.findByIdIncludingDeleted(OPPONENT_ID)).willReturn(Optional.of(opponent));
            given(chatMessageRepository.save(any(ChatMessage.class))).willReturn(savedMessage);

            chatMessageWebSocketService.saveTextMessage(CHAT_ROOM_ID, SENDER_ID, request);

            assertThat(opponentParticipant.isLeft()).isFalse();
            then(chatRoomParticipantRepository).should().save(opponentParticipant);
        }

        @Test
        @DisplayName("[실패] 존재하지 않는 채팅방이면 CHAT_ROOM_NOT_FOUND 예외를 던진다")
        void saveTextMessage_chatRoomNotFound() {
            ChatMessageRequest request = new ChatMessageRequest("안녕하세요");

            given(chatRoomRepository.findById(CHAT_ROOM_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() ->
                    chatMessageWebSocketService.saveTextMessage(CHAT_ROOM_ID, SENDER_ID, request))
                    .isInstanceOf(ChatException.class)
                    .satisfies(ex -> assertThat(((ChatException) ex).getErrorCode())
                            .isEqualTo(ChatErrorCode.CHAT_ROOM_NOT_FOUND));
        }

        @Test
        @DisplayName("[실패] 채팅방 참여자가 아니면 CHAT_ROOM_ACCESS_DENIED 예외를 던진다")
        void saveTextMessage_accessDenied() {
            ChatMessageRequest request = new ChatMessageRequest("안녕하세요");

            given(chatRoomRepository.findById(CHAT_ROOM_ID)).willReturn(Optional.of(chatRoom));
            given(chatRoomParticipantRepository.findByChatRoomIdAndMemberId(CHAT_ROOM_ID, SENDER_ID))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() ->
                    chatMessageWebSocketService.saveTextMessage(CHAT_ROOM_ID, SENDER_ID, request))
                    .isInstanceOf(ChatException.class)
                    .satisfies(ex -> assertThat(((ChatException) ex).getErrorCode())
                            .isEqualTo(ChatErrorCode.CHAT_ROOM_ACCESS_DENIED));
        }

        @Test
        @DisplayName("[실패] 내용이 null이면 CHAT_EMPTY_MESSAGE 예외를 던진다")
        void saveTextMessage_nullContent() {
            ChatMessageRequest request = new ChatMessageRequest(null);

            given(chatRoomRepository.findById(CHAT_ROOM_ID)).willReturn(Optional.of(chatRoom));
            given(chatRoomParticipantRepository.findByChatRoomIdAndMemberId(CHAT_ROOM_ID, SENDER_ID))
                    .willReturn(Optional.of(senderParticipant));
            given(memberRepository.findByIdIncludingDeleted(SENDER_ID)).willReturn(Optional.of(sender));
            given(chatRoomParticipantRepository.findAllByChatRoomId(CHAT_ROOM_ID))
                    .willReturn(List.of(senderParticipant, opponentParticipant));
            given(memberRepository.findByIdIncludingDeleted(OPPONENT_ID)).willReturn(Optional.of(opponent));

            assertThatThrownBy(() ->
                    chatMessageWebSocketService.saveTextMessage(CHAT_ROOM_ID, SENDER_ID, request))
                    .isInstanceOf(ChatException.class)
                    .satisfies(ex -> assertThat(((ChatException) ex).getErrorCode())
                            .isEqualTo(ChatErrorCode.CHAT_EMPTY_MESSAGE));
        }

        @Test
        @DisplayName("[실패] 공백만 있는 내용이면 CHAT_EMPTY_MESSAGE 예외를 던진다")
        void saveTextMessage_blankContent() {
            ChatMessageRequest request = new ChatMessageRequest("   ");

            given(chatRoomRepository.findById(CHAT_ROOM_ID)).willReturn(Optional.of(chatRoom));
            given(chatRoomParticipantRepository.findByChatRoomIdAndMemberId(CHAT_ROOM_ID, SENDER_ID))
                    .willReturn(Optional.of(senderParticipant));
            given(memberRepository.findByIdIncludingDeleted(SENDER_ID)).willReturn(Optional.of(sender));
            given(chatRoomParticipantRepository.findAllByChatRoomId(CHAT_ROOM_ID))
                    .willReturn(List.of(senderParticipant, opponentParticipant));
            given(memberRepository.findByIdIncludingDeleted(OPPONENT_ID)).willReturn(Optional.of(opponent));

            assertThatThrownBy(() ->
                    chatMessageWebSocketService.saveTextMessage(CHAT_ROOM_ID, SENDER_ID, request))
                    .isInstanceOf(ChatException.class)
                    .satisfies(ex -> assertThat(((ChatException) ex).getErrorCode())
                            .isEqualTo(ChatErrorCode.CHAT_EMPTY_MESSAGE));
        }

        @Test
        @DisplayName("[실패] 1000자를 초과하는 내용이면 CHAT_MESSAGE_TOO_LONG 예외를 던진다")
        void saveTextMessage_tooLong() {
            String longContent = "a".repeat(1001);
            ChatMessageRequest request = new ChatMessageRequest(longContent);

            given(chatRoomRepository.findById(CHAT_ROOM_ID)).willReturn(Optional.of(chatRoom));
            given(chatRoomParticipantRepository.findByChatRoomIdAndMemberId(CHAT_ROOM_ID, SENDER_ID))
                    .willReturn(Optional.of(senderParticipant));
            given(memberRepository.findByIdIncludingDeleted(SENDER_ID)).willReturn(Optional.of(sender));
            given(chatRoomParticipantRepository.findAllByChatRoomId(CHAT_ROOM_ID))
                    .willReturn(List.of(senderParticipant, opponentParticipant));
            given(memberRepository.findByIdIncludingDeleted(OPPONENT_ID)).willReturn(Optional.of(opponent));

            assertThatThrownBy(() ->
                    chatMessageWebSocketService.saveTextMessage(CHAT_ROOM_ID, SENDER_ID, request))
                    .isInstanceOf(ChatException.class)
                    .satisfies(ex -> assertThat(((ChatException) ex).getErrorCode())
                            .isEqualTo(ChatErrorCode.CHAT_MESSAGE_TOO_LONG));
        }

        @Test
        @DisplayName("[실패] ADMIN 역할이면 CHAT_ADMIN_NOT_ALLOWED 예외를 던진다")
        void saveTextMessage_adminNotAllowed() {
            ChatMessageRequest request = new ChatMessageRequest("관리자 메시지");

            Member admin = Member.builder()
                    .email("admin@example.com")
                    .password("pw")
                    .nickname("관리자")
                    .gender(MemberGender.UNKNOWN)
                    .region(Region.UNKNOWN)
                    .build();
            ReflectionTestUtils.setField(admin, "id", SENDER_ID);
            ReflectionTestUtils.setField(admin, "role", MemberRole.ADMIN);

            given(chatRoomRepository.findById(CHAT_ROOM_ID)).willReturn(Optional.of(chatRoom));
            given(chatRoomParticipantRepository.findByChatRoomIdAndMemberId(CHAT_ROOM_ID, SENDER_ID))
                    .willReturn(Optional.of(senderParticipant));
            given(memberRepository.findByIdIncludingDeleted(SENDER_ID)).willReturn(Optional.of(admin));

            assertThatThrownBy(() ->
                    chatMessageWebSocketService.saveTextMessage(CHAT_ROOM_ID, SENDER_ID, request))
                    .isInstanceOf(ChatException.class)
                    .satisfies(ex -> assertThat(((ChatException) ex).getErrorCode())
                            .isEqualTo(ChatErrorCode.CHAT_ADMIN_NOT_ALLOWED));
        }

        @Test
        @DisplayName("[실패] 비활성 회원이면 CHAT_MEMBER_NOT_ACTIVE 예외를 던진다")
        void saveTextMessage_memberNotActive() {
            ChatMessageRequest request = new ChatMessageRequest("안녕하세요");

            Member inactiveMember = Member.builder()
                    .email("inactive@example.com")
                    .password("pw")
                    .nickname("비활성")
                    .gender(MemberGender.UNKNOWN)
                    .region(Region.UNKNOWN)
                    .build();
            ReflectionTestUtils.setField(inactiveMember, "id", SENDER_ID);
            inactiveMember.delete(); // deleted = true → isActive() = false

            given(chatRoomRepository.findById(CHAT_ROOM_ID)).willReturn(Optional.of(chatRoom));
            given(chatRoomParticipantRepository.findByChatRoomIdAndMemberId(CHAT_ROOM_ID, SENDER_ID))
                    .willReturn(Optional.of(senderParticipant));
            given(memberRepository.findByIdIncludingDeleted(SENDER_ID)).willReturn(Optional.of(inactiveMember));

            assertThatThrownBy(() ->
                    chatMessageWebSocketService.saveTextMessage(CHAT_ROOM_ID, SENDER_ID, request))
                    .isInstanceOf(ChatException.class)
                    .satisfies(ex -> assertThat(((ChatException) ex).getErrorCode())
                            .isEqualTo(ChatErrorCode.CHAT_MEMBER_NOT_ACTIVE));
        }
    }
}