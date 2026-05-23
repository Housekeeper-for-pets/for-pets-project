package com.forpets.domain.chat.controller;

import com.forpets.domain.chat.dto.*;
import com.forpets.domain.chat.service.ChatMessageService;
import com.forpets.domain.chat.service.ChatRoomService;
import com.forpets.global.common.ApiResponse;
import com.forpets.global.security.annotation.LoginUser;
import com.forpets.global.security.dto.CurrentMember;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/chat-rooms")
@RequiredArgsConstructor
public class ChatRoomController {

    private final ChatRoomService chatRoomService;
    private final ChatMessageService chatMessageService;


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

    // 로그인한 회원이 참여 중인 채팅방 목록 조회
    @GetMapping
    public ResponseEntity<ApiResponse<ChatRoomListResponse>> getChatRoomList(
            @LoginUser CurrentMember currentMember,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime cursorLastMessageAt,
            @RequestParam(required = false) Long cursorChatRoomId,
            @RequestParam(defaultValue = "20") int size
    ){
        ChatRoomListResponse response = chatRoomService.getChatRoomList(
                currentMember.id(),
                cursorLastMessageAt,
                cursorChatRoomId,
                size
        );
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // 채팅방 나가기
    @PatchMapping("/{chatRoomId}/leave")
    public ResponseEntity<ApiResponse<ChatRoomLeaveResponse>> leaveChatRoom(
            @LoginUser CurrentMember currentMember,
            @PathVariable Long chatRoomId
    ) {
        ChatRoomLeaveResponse response = chatRoomService.leaveChatRoom(
                currentMember.id(),
                chatRoomId
        );

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // 메세지 목록 조회
    @GetMapping("/{chatRoomId}/messages")
    public ResponseEntity<ApiResponse<ChatMessageListResponse>> getMessages(
            @LoginUser CurrentMember currentMember,
            @PathVariable Long chatRoomId,
            @RequestParam(required = false) Long cursorId,
            @RequestParam(defaultValue = "30") int size
    ){
        ChatMessageListResponse response = chatMessageService.getMessages(
                currentMember.id(),
                chatRoomId,
                cursorId,
                size
        );
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
