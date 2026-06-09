package com.forpets.domain.chat.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

@Builder
public record ChatRoomListResponse(
        List<ChatRoomListItem> items,

        @JsonProperty("hasNext")
        boolean hasNext,

        // 다음 페이지 조회 시간
        LocalDateTime nextCursorLastMessageAt,
        // 다음 페이지 채팅방 ID, 같은 시간 건너뛰기 방지
        Long nextCursorChatRoomId
) {
    public static ChatRoomListResponse empty(){
        return new ChatRoomListResponse(
                List.of(),
                false,
                null,
                null
        );
    }
}
