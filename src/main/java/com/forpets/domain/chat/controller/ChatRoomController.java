package com.forpets.domain.chat.controller;

import com.forpets.domain.chat.dto.ChatRoomCreateRequest;
import com.forpets.domain.chat.dto.ChatRoomCreateResponse;
import com.forpets.domain.chat.service.ChatRoomService;
import com.forpets.global.common.ApiResponse;
import com.forpets.global.security.annotation.LoginUser;
import com.forpets.global.security.dto.CurrentMember;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chat-rooms")
@RequiredArgsConstructor
public class ChatRoomController {

    private final ChatRoomService chatRoomService;


    // 채팅방 생성 또는 조회
    @PostMapping
    public ResponseEntity<ApiResponse<ChatRoomCreateResponse>> createOrGetChatRoom(
            @LoginUser CurrentMember currentMember,
            @RequestBody @Valid ChatRoomCreateRequest request
    ) {
        ChatRoomCreateResponse response = chatRoomService.createOrGetChatRoom(
                currentMember.id(),
                request.opponentId()
        );
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
