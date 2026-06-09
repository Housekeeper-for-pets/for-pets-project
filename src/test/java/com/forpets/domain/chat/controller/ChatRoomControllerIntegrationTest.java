package com.forpets.domain.chat.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.forpets.domain.chat.dto.ChatRoomCreateRequest;
import com.forpets.domain.chat.entity.ChatRoom;
import com.forpets.domain.chat.entity.ChatRoomParticipant;
import com.forpets.domain.chat.repository.ChatRoomParticipantRepository;
import com.forpets.domain.chat.repository.ChatRoomRepository;
import com.forpets.domain.member.entity.Member;
import com.forpets.domain.member.entity.MemberGender;
import com.forpets.domain.member.entity.Region;
import com.forpets.domain.member.repository.MemberRepository;
import com.forpets.global.security.jwt.JwtTokenProvider;
import com.forpets.global.security.jwt.TokenRedisService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("ChatRoom 통합 테스트")
class ChatRoomControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private MemberRepository memberRepository;
    @Autowired private ChatRoomRepository chatRoomRepository;
    @Autowired private ChatRoomParticipantRepository chatRoomParticipantRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private TokenRedisService tokenRedisService;

    private Member memberA;
    private Member memberB;
    private Member memberC;

    @BeforeEach
    void setUp() {
        given(tokenRedisService.isBlacklisted(anyString())).willReturn(false);

        memberA = memberRepository.save(Member.builder()
                .email("chat-a@example.com")
                .password(passwordEncoder.encode("password123!"))
                .nickname("채팅유저A")
                .gender(MemberGender.MALE)
                .region(Region.GANGNAM)
                .build());

        memberB = memberRepository.save(Member.builder()
                .email("chat-b@example.com")
                .password(passwordEncoder.encode("password123!"))
                .nickname("채팅유저B")
                .gender(MemberGender.FEMALE)
                .region(Region.SEOCHO)
                .build());

        memberC = memberRepository.save(Member.builder()
                .email("chat-c@example.com")
                .password(passwordEncoder.encode("password123!"))
                .nickname("채팅유저C")
                .gender(MemberGender.UNKNOWN)
                .region(Region.DONGJAK)
                .build());
    }

    private String bearerToken(Member member) {
        return jwtTokenProvider.createAccessToken(member.getId(), member.getRole());
    }

    // =============================================
    // 채팅방 생성/조회
    // =============================================
    @Nested
    @DisplayName("채팅방 생성 또는 조회 — POST /api/chat-rooms")
    class CreateOrGetChatRoomTest {

        @Test
        @DisplayName("[성공] 채팅방이 없으면 새로 생성하고 200을 반환한다")
        void createChatRoom_success() throws Exception {
            ChatRoomCreateRequest request = new ChatRoomCreateRequest(memberB.getId());

            mockMvc.perform(post("/api/chat-rooms")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken(memberA))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.chatRoomId").isNumber());
        }

        @Test
        @DisplayName("[성공] 이미 채팅방이 있으면 기존 채팅방을 반환한다")
        void getChatRoom_existingRoom() throws Exception {
            // 채팅방 미리 생성
            String roomKey = ChatRoom.generateRoomKey(memberA.getId(), memberB.getId());
            ChatRoom existingRoom = chatRoomRepository.save(ChatRoom.builder()
                    .memberAId(ChatRoom.resolveMemberAId(memberA.getId(), memberB.getId()))
                    .memberBId(ChatRoom.resolveMemberBId(memberA.getId(), memberB.getId()))
                    .roomKey(roomKey)
                    .build());
            chatRoomParticipantRepository.save(ChatRoomParticipant.builder()
                    .chatRoomId(existingRoom.getId())
                    .memberId(memberA.getId())
                    .build());
            chatRoomParticipantRepository.save(ChatRoomParticipant.builder()
                    .chatRoomId(existingRoom.getId())
                    .memberId(memberB.getId())
                    .build());

            ChatRoomCreateRequest request = new ChatRoomCreateRequest(memberB.getId());

            mockMvc.perform(post("/api/chat-rooms")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken(memberA))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.chatRoomId").value(existingRoom.getId()));
        }

        @Test
        @DisplayName("[실패] 미인증 요청이면 401을 반환한다")
        void createChatRoom_unauthorized() throws Exception {
            ChatRoomCreateRequest request = new ChatRoomCreateRequest(memberB.getId());

            mockMvc.perform(post("/api/chat-rooms")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }
    }

    // =============================================
    // 채팅방 목록 조회
    // =============================================
    @Nested
    @DisplayName("채팅방 목록 조회 — GET /api/chat-rooms")
    class GetChatRoomListTest {

        @Test
        @DisplayName("[성공] 참여 중인 채팅방 목록을 반환한다")
        void getChatRoomList_success() throws Exception {
            // 채팅방 2개 생성
            ChatRoom room1 = chatRoomRepository.save(ChatRoom.builder()
                    .memberAId(ChatRoom.resolveMemberAId(memberA.getId(), memberB.getId()))
                    .memberBId(ChatRoom.resolveMemberBId(memberA.getId(), memberB.getId()))
                    .roomKey(ChatRoom.generateRoomKey(memberA.getId(), memberB.getId()))
                    .build());
            chatRoomParticipantRepository.save(ChatRoomParticipant.builder()
                    .chatRoomId(room1.getId()).memberId(memberA.getId()).build());
            chatRoomParticipantRepository.save(ChatRoomParticipant.builder()
                    .chatRoomId(room1.getId()).memberId(memberB.getId()).build());

            ChatRoom room2 = chatRoomRepository.save(ChatRoom.builder()
                    .memberAId(ChatRoom.resolveMemberAId(memberA.getId(), memberC.getId()))
                    .memberBId(ChatRoom.resolveMemberBId(memberA.getId(), memberC.getId()))
                    .roomKey(ChatRoom.generateRoomKey(memberA.getId(), memberC.getId()))
                    .build());
            chatRoomParticipantRepository.save(ChatRoomParticipant.builder()
                    .chatRoomId(room2.getId()).memberId(memberA.getId()).build());
            chatRoomParticipantRepository.save(ChatRoomParticipant.builder()
                    .chatRoomId(room2.getId()).memberId(memberC.getId()).build());

            mockMvc.perform(get("/api/chat-rooms")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken(memberA)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.items").isArray());
        }

        @Test
        @DisplayName("[실패] 미인증 요청이면 401을 반환한다")
        void getChatRoomList_unauthorized() throws Exception {
            mockMvc.perform(get("/api/chat-rooms"))
                    .andExpect(status().isUnauthorized());
        }
    }

    // =============================================
    // 채팅방 나가기
    // =============================================
    @Nested
    @DisplayName("채팅방 나가기 — PATCH /api/chat-rooms/{chatRoomId}/leave")
    class LeaveChatRoomTest {

        private ChatRoom chatRoom;
        private ChatRoomParticipant participantA;

        @BeforeEach
        void setUp() {
            chatRoom = chatRoomRepository.save(ChatRoom.builder()
                    .memberAId(ChatRoom.resolveMemberAId(memberA.getId(), memberB.getId()))
                    .memberBId(ChatRoom.resolveMemberBId(memberA.getId(), memberB.getId()))
                    .roomKey(ChatRoom.generateRoomKey(memberA.getId(), memberB.getId()))
                    .build());
            participantA = chatRoomParticipantRepository.save(ChatRoomParticipant.builder()
                    .chatRoomId(chatRoom.getId()).memberId(memberA.getId()).build());
            chatRoomParticipantRepository.save(ChatRoomParticipant.builder()
                    .chatRoomId(chatRoom.getId()).memberId(memberB.getId()).build());
        }

        @Test
        @DisplayName("[성공] 채팅방 나가기 성공 시 200과 isLeft=true를 반환한다")
        void leaveChatRoom_success() throws Exception {
            mockMvc.perform(patch("/api/chat-rooms/{chatRoomId}/leave", chatRoom.getId())
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken(memberA)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.isLeft").value(true));
        }

        @Test
        @DisplayName("[실패] 참여하지 않은 채팅방 나가기 시 403을 반환한다")
        void leaveChatRoom_accessDenied() throws Exception {
            // memberC는 해당 채팅방 참여자가 아님
            mockMvc.perform(patch("/api/chat-rooms/{chatRoomId}/leave", chatRoom.getId())
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken(memberC)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.error.code").value("CHAT_ROOM_ACCESS_DENIED"));
        }

        @Test
        @DisplayName("[실패] 미인증 요청이면 401을 반환한다")
        void leaveChatRoom_unauthorized() throws Exception {
            mockMvc.perform(patch("/api/chat-rooms/{chatRoomId}/leave", chatRoom.getId()))
                    .andExpect(status().isUnauthorized());
        }
    }
}