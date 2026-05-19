package com.forpets.integration;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.forpets.domain.member.entity.Member;
import com.forpets.domain.member.entity.MemberGender;
import com.forpets.domain.member.entity.Region;
import com.forpets.domain.pet.entity.Pet;
import com.forpets.domain.post.entity.Post;
import com.forpets.domain.reservation.entity.Reservation;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 시나리오 통합 테스트
    단위테스트는 따로 진행할 예정이고,
    가장 오류가 빈번히 발생할 것 같다고 예상한 시나리오들에 대해서 테스트를 진행

     ============================================================
     시나리오 1: 다중 Proposal 수락 및 결제 경쟁 테스트
     목표: Post → 여러 Proposal → 여러 Pending Reservation → 결제 경쟁 → 자동 취소 확인
     추가: Pet 삭제 불가, 회원 탈퇴 불가, 시터 프로필 삭제 불가 확인
     ============================================================
     시나리오 2: Complete 시간 검증 테스트
     목표: CONFIRMED 상태에서 케어 시간 전 Complete 시도 → 실패
     시간 경과 후 Complete 시도 → 성공
     ============================================================
     시나리오 3: PENDING/CONFIRMED 상태별 Proposal 제한 테스트
     목표: PENDING 상태 → Proposal 가능
     CONFIRMED 상태 → Proposal 불가
     취소 시 Post 자동 닫힘 및 재개방 안 됨 확인
     ============================================================

 */
@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ScenarioIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // ============================================================
    // 테스트 계정 정보 — 실제 환경에 맞게 수정
    // ============================================================
    static final String MEMBER_A_EMAIL = "giljung@test.com";
    static final String MEMBER_B_EMAIL = "jiwon@test.com";
    static final String MEMBER_C_EMAIL = "jimin@test.com";
    static final String MEMBER_D_EMAIL = "gyeongahn@test.com";

    static final String PASSWORD = "test1234!";
    static final int PET_ID = 9; // A의 반려동물 ID

    // 시나리오 간 공유되는 상태
    static String tokenA, tokenB, tokenC, tokenD;
    static int postId1, postId2, postId3;
    static int proposalB, proposalC, proposalD;
    static int reservationB, reservationC;

    // ============================================================
    // 헬퍼 메서드
    // ============================================================

    private String login(String email) throws Exception {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("email", email);
        body.put("password", PASSWORD);

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        return response.get("data").get("accessToken").asText();
    }

    private int createPost(String token, String title, String careDate,
                           String startTime, String endTime) throws Exception {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("title", title);
        body.put("content", "통합 테스트용 게시글입니다.");

        ArrayNode petIds = body.putArray("petIds");
        petIds.add(PET_ID);

        body.put("careType", "VISIT");
        body.put("budgetAmount", 50000);

        ArrayNode timeSlots = body.putArray("timeSlots");
        ObjectNode slot = objectMapper.createObjectNode();
        slot.put("careDate", careDate);
        slot.put("startTime", startTime);
        slot.put("endTime", endTime);
        timeSlots.add(slot);

        MvcResult result = mockMvc.perform(post("/api/posts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body.toString()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        return response.get("data").get("id").asInt();
    }

    private int createProposal(String token, int postId, int price, String message) throws Exception {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("proposedPrice", price);
        body.put("message", message);

        MvcResult result = mockMvc.perform(post("/api/posts/" + postId + "/proposals")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body.toString()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        return response.get("data").get("id").asInt();
    }

    private JsonNode acceptProposal(String token, int proposalId) throws Exception {
        MvcResult result = mockMvc.perform(patch("/api/proposals/" + proposalId + "/accept")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private void rejectProposal(String token, int proposalId) throws Exception {
        mockMvc.perform(patch("/api/proposals/" + proposalId + "/reject")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    private void confirmReservation(String token, int reservationId) throws Exception {
        mockMvc.perform(patch("/api/reservations/" + reservationId + "/confirm")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    private void completeReservation(String token, int reservationId) throws Exception {
        mockMvc.perform(patch("/api/reservations/" + reservationId + "/complete")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    private JsonNode getMyReservations(String token) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/reservations/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private JsonNode getReservation(String token, int reservationId) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/reservations/" + reservationId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    // ============================================================
    // 시나리오 1: 다중 Proposal 수락 및 결제 경쟁 테스트
    // 목표: Post → 여러 Proposal → 여러 Pending Reservation → 결제 경쟁 → 자동 취소 확인
    // 추가: Pet 삭제 불가, 회원 탈퇴 불가, 시터 프로필 삭제 불가 확인
    // ============================================================

    @Test
    @Order(1)
    @DisplayName("시나리오1: 다중 Proposal 수락, 결제, 삭제 제한 검증")
    void scenario1_multiProposalAcceptAndPaymentRace() throws Exception {
        // 1-1. A 로그인
        tokenA = login(MEMBER_A_EMAIL);
        Assertions.assertNotNull(tokenA);

        // 1-2. A가 Post 작성
        postId1 = createPost(tokenA, "시나리오1 - 다중 제안 테스트",
                "2026-06-01", "09:00", "18:00");
        Assertions.assertTrue(postId1 > 0);
        System.out.println("✅ Post 생성 완료: postId=" + postId1);

        // 1-3 ~ 1-8. B, C, D 로그인 후 Proposal 제안
        tokenB = login(MEMBER_B_EMAIL);
        proposalB = createProposal(tokenB, postId1, 35000, "경험 많은 시터입니다!");
        System.out.println("✅ B Proposal 생성: proposalId=" + proposalB);

        tokenC = login(MEMBER_C_EMAIL);
        proposalC = createProposal(tokenC, postId1, 40000, "고양이 전문 시터입니다!");
        System.out.println("✅ C Proposal 생성: proposalId=" + proposalC);

        tokenD = login(MEMBER_D_EMAIL);
        proposalD = createProposal(tokenD, postId1, 30000, "저렴하게 해드립니다!");
        System.out.println("✅ D Proposal 생성: proposalId=" + proposalD);

        // 1-9. A가 B의 Proposal 수락 → Reservation 생성
        acceptProposal(tokenA, proposalB);
        // ⚠️ accept 응답에 reservationId가 포함되어 있다면 여기서 추출
        // 아니라면 예약 목록 조회로 확인
        System.out.println("✅ B Proposal 수락 완료");

        // 1-10. A가 C의 Proposal 수락 → Reservation 생성
        acceptProposal(tokenA, proposalC);
        System.out.println("✅ C Proposal 수락 완료");

        // 1-11. A가 D의 Proposal 거절
        rejectProposal(tokenA, proposalD);
        System.out.println("✅ D Proposal 거절 완료");

        // 1-12. A가 본인 Reservation 조회
        JsonNode reservations = getMyReservations(tokenA);
        System.out.println("✅ A의 예약 목록: " + reservations.toPrettyString());

        // ⚠️ 예약 목록에서 reservationId를 추출해야 합니다.
        // 응답 구조에 따라 아래 파싱 로직을 수정하세요.
        JsonNode dataList = reservations.get("data");
        for (JsonNode r : dataList) {
            int sourceId = r.get("sourceId").asInt();
            if (sourceId == proposalB) {
                reservationB = r.get("id").asInt();
            } else if (sourceId == proposalC) {
                reservationC = r.get("id").asInt();
            }
        }
        System.out.println("   reservationB=" + reservationB + ", reservationC=" + reservationC);

        // 1-13. A가 reservationB 결제 (보호자 결제)
        confirmReservation(tokenA, reservationB);
        System.out.println("✅ A가 reservationB 보호자 결제 완료");

        // 1-14. A가 reservationC 결제 (보호자 결제)
        confirmReservation(tokenA, reservationC);
        System.out.println("✅ A가 reservationC 보호자 결제 완료");

        // 1-15. B가 reservationB 결제 (시터 결제) → CONFIRMED
        confirmReservation(tokenB, reservationB);
        System.out.println("✅ B가 reservationB 시터 결제 → CONFIRMED");

        // 1-16. A가 Reservation 조회하여 상태 확인
        JsonNode resB = getReservation(tokenA, reservationB);
        String statusB = resB.get("data").get("status").asText();
        System.out.println("✅ reservationB 상태: " + statusB);
        Assertions.assertEquals("CONFIRMED", statusB,
                "보호자+시터 양쪽 결제 완료 후 CONFIRMED 상태여야 합니다");

        // 1-17. A가 Pet 삭제 시도 (실패해야 함 - 진행중인 예약 존재)
        mockMvc.perform(delete("/api/pets/" + PET_ID)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    Assertions.assertNotEquals(200, status,
                            "진행중인 예약이 있으므로 Pet 삭제는 실패해야 합니다");
                });
        System.out.println("✅ Pet 삭제 시도 → 정상적으로 실패");

        // 1-18. A가 회원 탈퇴 시도 (실패해야 함)
        mockMvc.perform(delete("/api/members/me")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    Assertions.assertNotEquals(200, status,
                            "진행중인 예약이 있으므로 회원 탈퇴는 실패해야 합니다");
                });
        System.out.println("✅ 회원 탈퇴 시도 → 정상적으로 실패");

        // 1-19. B가 시터 프로필 삭제 시도 (실패해야 함)
        mockMvc.perform(delete("/api/sitters/me")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    Assertions.assertNotEquals(200, status,
                            "진행중인 예약이 있으므로 시터 프로필 삭제는 실패해야 합니다");
                });
        System.out.println("✅ 시터 프로필 삭제 시도 → 정상적으로 실패");
    }

    // ============================================================
    // 시나리오 2: Complete 시간 검증 테스트
    // 목표: CONFIRMED 상태에서 케어 시간 전 Complete 시도 → 실패
    //      시간 경과 후 Complete 시도 → 성공
    // ============================================================

    @Test
    @Order(2)
    @DisplayName("시나리오2: Complete 시간 검증 - 케어 종료 전 Complete 불가")
    void scenario2_completeTimeValidation() throws Exception {
        // 2-1. A 로그인
        tokenA = login(MEMBER_A_EMAIL);

        // 2-2. A가 Post 작성 (미래 시간으로 설정 → Complete 불가)
        // ⚠️ careDate를 충분히 먼 미래로 설정하여 "아직 케어 시간 안 됨" 상태를 만듦
        postId2 = createPost(tokenA, "시나리오2 - Complete 시간 검증",
                "2026-07-01", "09:00", "18:00");
        System.out.println("✅ Post 생성 완료: postId=" + postId2);

        // 2-3. B 로그인
        tokenB = login(MEMBER_B_EMAIL);

        // 2-4. B가 Proposal 제안
        int proposal2 = createProposal(tokenB, postId2, 35000, "시간 검증 테스트");
        System.out.println("✅ Proposal 생성: proposalId=" + proposal2);

        // 2-5. A가 Proposal 수락
        acceptProposal(tokenA, proposal2);
        System.out.println("✅ Proposal 수락 완료");

        // 예약 목록에서 방금 생성된 예약 ID 조회
        JsonNode reservations = getMyReservations(tokenA);
        JsonNode dataList = reservations.get("data");
        int reservation2 = -1;

        // ⚠️ 가장 최근 예약을 찾는 로직 - 응답 구조에 따라 수정 필요
        if (dataList != null && dataList.isArray()) {
            for (JsonNode r : dataList) {
                int id = r.get("id").asInt();
                if (id > reservation2) reservation2 = id;
            }
        }
        System.out.println("   reservationId=" + reservation2);
        Assertions.assertTrue(reservation2 > 0, "예약 ID를 찾을 수 없습니다");

        // 2-6. A가 결제 (보호자 결제)
        confirmReservation(tokenA, reservation2);
        System.out.println("✅ 보호자 결제 완료");

        // 2-7. B가 결제 (시터 결제) → CONFIRMED
        confirmReservation(tokenB, reservation2);
        System.out.println("✅ 시터 결제 → CONFIRMED");

        // 2-8. B가 케어 종료 전 Complete 시도 (실패해야 함)
        // careDate가 2026-07-01이므로 현재 시점에서는 Complete 불가
        mockMvc.perform(patch("/api/reservations/" + reservation2 + "/complete")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    Assertions.assertNotEquals(200, status,
                            "케어 시간 종료 전에는 Complete가 실패해야 합니다");
                });
        System.out.println("✅ 케어 종료 전 Complete 시도 → 정상적으로 실패");

        // 2-9. 시간 경과 후 Complete 시도
        // ⚠️ 실제로는 시간이 지나야 가능합니다.
        // 방법 1: careDate를 과거로 설정한 별도 테스트 데이터 사용
        // 방법 2: 테스트 환경에서 시간을 조작 (Clock 주입 등)
        // 여기서는 주석으로 남겨둡니다.
        System.out.println("⏳ 2-9: 시간 경과 후 Complete는 수동 확인 또는 Clock 주입 필요");
        System.out.println("   → 과거 날짜로 Post를 만들어 테스트하거나,");
        System.out.println("   → 실제 endTime 이후에 수동으로 PATCH /api/reservations/" + reservation2 + "/complete 호출");

        // 2-10. 현재 Reservation 상태 확인
        JsonNode res2 = getReservation(tokenA, reservation2);
        String status2 = res2.get("data").get("status").asText();
        System.out.println("✅ 현재 reservation 상태: " + status2);
    }

    // ============================================================
    // 시나리오 3: PENDING/CONFIRMED 상태별 Proposal 제한 테스트
//    목표: PENDING 상태 → Proposal 가능
//    CONFIRMED 상태 → Proposal 불가
//    취소 시 Post 자동 닫힘 및 재개방 안 됨 확인
    // ============================================================

    @Test
    @Order(3)
    @DisplayName("시나리오3: 상태별 Proposal 제한 및 Post 닫힘 검증")
    void scenario3_proposalRestrictionByStatus() throws Exception {
        // 3-1. A 로그인
        tokenA = login(MEMBER_A_EMAIL);

        // 3-2. A가 Post 작성
        postId3 = createPost(tokenA, "시나리오3 - Proposal 제한 테스트",
                "2026-06-15", "10:00", "16:00");
        System.out.println("✅ Post 생성 완료: postId=" + postId3);

        // 3-3 ~ 3-4. B 로그인 후 Proposal 제안
        tokenB = login(MEMBER_B_EMAIL);
        int proposal3B = createProposal(tokenB, postId3, 35000, "첫 번째 시터");
        System.out.println("✅ B Proposal 생성: proposalId=" + proposal3B);

        // 3-5. A가 B의 Proposal 수락 (PENDING Reservation 생성)
        acceptProposal(tokenA, proposal3B);
        System.out.println("✅ B Proposal 수락 → PENDING 예약 생성");

        // 3-6 ~ 3-7. C 로그인 후, PENDING 상태에서 새 Proposal 제안 (성공해야 함)
        tokenC = login(MEMBER_C_EMAIL);
        int proposal3C = createProposal(tokenC, postId3, 38000, "PENDING 중 제안");
        Assertions.assertTrue(proposal3C > 0,
                "PENDING 상태에서는 새 Proposal 제안이 가능해야 합니다");
        System.out.println("✅ PENDING 상태에서 C Proposal 성공: proposalId=" + proposal3C);

        // 예약 ID 조회
        JsonNode reservations = getMyReservations(tokenA);
        JsonNode dataList = reservations.get("data");
        int reservation3 = -1;
        if (dataList != null && dataList.isArray()) {
            for (JsonNode r : dataList) {
                int id = r.get("id").asInt();
                if (id > reservation3) reservation3 = id;
            }
        }
        System.out.println("   reservationId=" + reservation3);

        // 3-8. A가 결제 (보호자 결제)
        confirmReservation(tokenA, reservation3);
        System.out.println("✅ 보호자 결제 완료");

        // 3-9. B가 결제 (시터 결제) → CONFIRMED
        confirmReservation(tokenB, reservation3);
        System.out.println("✅ 시터 결제 → CONFIRMED");

        // 상태 확인
        JsonNode res3 = getReservation(tokenA, reservation3);
        Assertions.assertEquals("CONFIRMED", res3.get("data").get("status").asText());

        // 3-10 ~ 3-11. D 로그인 후, CONFIRMED 상태에서 새 Proposal 시도 (실패해야 함)
        tokenD = login(MEMBER_D_EMAIL);
        mockMvc.perform(post("/api/posts/" + postId3 + "/proposals")
                        .header("Authorization", "Bearer " + tokenD)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"proposedPrice\": 30000, \"message\": \"CONFIRMED 후 제안 시도\"}"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    Assertions.assertNotEquals(201, status,
                            "CONFIRMED 상태에서는 새 Proposal이 실패해야 합니다");
                });
        System.out.println("✅ CONFIRMED 상태에서 D Proposal → 정상적으로 실패");

        // 3-12. A가 Reservation 취소 (사유 포함)
        ObjectNode cancelBody = objectMapper.createObjectNode();
        cancelBody.put("cancelReason", "테스트 취소 사유입니다.");
        cancelBody.put("cancelCategory", "PERSONAL");

        mockMvc.perform(patch("/api/reservations/" + reservation3 + "/cancel")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cancelBody.toString()))
                .andExpect(status().isOk());
        System.out.println("✅ Reservation 취소 완료");

        // 3-13. Post 상태 확인 (CLOSED 되었는지)
        // ⚠️ GET /api/posts/{postId} 가 아직 없으므로 수동 확인 필요
        // 아래는 API가 생긴 후 활성화하세요
        /*
        MvcResult postResult = mockMvc.perform(get("/api/posts/" + postId3)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode postData = objectMapper.readTree(postResult.getResponse().getContentAsString());
        String postStatus = postData.get("data").get("status").asText();
        Assertions.assertEquals("CLOSED", postStatus, "취소 후 Post는 CLOSED 상태여야 합니다");
        */
        System.out.println("⚠️ 3-13: GET /api/posts/{postId} 미구현 → Post 상태 수동 확인 필요");

        // 3-14. 닫힌 Post에 새 Proposal 시도 (실패해야 함)
        // ⚠️ Post가 CLOSED 상태라면 실패해야 함
        mockMvc.perform(post("/api/posts/" + postId3 + "/proposals")
                        .header("Authorization", "Bearer " + tokenD)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"proposedPrice\": 25000, \"message\": \"닫힌 Post에 제안\"}"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    Assertions.assertNotEquals(201, status,
                            "CLOSED 상태의 Post에는 Proposal이 실패해야 합니다");
                });
        System.out.println("✅ CLOSED Post에 Proposal → 정상적으로 실패");

        // 3-15. A가 Post 재개방 시도 (실패해야 함)
        // ⚠️ PUT으로 status를 OPEN으로 변경 시도
        ObjectNode reopenBody = objectMapper.createObjectNode();
        reopenBody.put("title", "시나리오3 - Proposal 제한 테스트");
        reopenBody.put("content", "재개방 시도");
        ArrayNode petIds = reopenBody.putArray("petIds");
        petIds.add(PET_ID);
        reopenBody.put("careType", "VISIT");
        reopenBody.put("budgetAmount", 50000);
        ArrayNode timeSlots = reopenBody.putArray("timeSlots");
        ObjectNode slot = objectMapper.createObjectNode();
        slot.put("careDate", "2026-06-15");
        slot.put("startTime", "10:00");
        slot.put("endTime", "16:00");
        timeSlots.add(slot);

        mockMvc.perform(put("/api/posts/" + postId3)
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reopenBody.toString()))
                .andExpect(result -> {
                    // 재개방이 불가하다면 에러 응답이어야 함
                    // 만약 PUT이 status 필드를 안 받고 내용만 수정하는 거라면
                    // 이 테스트는 별도 재개방 API가 필요할 수 있음
                    System.out.println("   재개방 응답 status: " + result.getResponse().getStatus());
                    System.out.println("   응답: " + result.getResponse().getContentAsString());
                });
        System.out.println("⚠️ 3-15: Post 재개방 제한은 API 구조에 따라 검증 방식 조정 필요");
    }
}