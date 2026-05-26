package com.forpets.domain.chat.dto;

import jakarta.validation.constraints.NotNull;

// 채팅 상대 ID 요청
public record ChatRoomCreateRequest(

        @NotNull(message = "상대 회원 ID는 필수입니다.")
        Long opponentId
) {
}
