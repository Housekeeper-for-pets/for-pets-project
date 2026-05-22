package com.forpets.domain.chat.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

// 채팅방 생성 결과
public record ChatRoomCreateResponse(
        Long chatRoomId,
        Long opponentId,
        String opponentNickname,

        @JsonProperty("isNew")
        boolean isNew
) {
}
