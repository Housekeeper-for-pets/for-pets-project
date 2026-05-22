package com.forpets.domain.chat.exception;

import com.forpets.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ChatErrorCode implements ErrorCode {


    CHAT_MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "CHAT_MEMBER_NOT_FOUND", "회원을 찾을 수 없습니다."),
    CHAT_OPPONENT_NOT_FOUND(HttpStatus.NOT_FOUND, "CHAT_OPPONENT_NOT_FOUND", "상대 회원을 찾을 수 없습니다."),
    CHAT_ROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "CHAT_ROOM_NOT_FOUND", "채팅방을 찾을 수 없습니다."),
    CHAT_INVALID_OPPONENT(HttpStatus.BAD_REQUEST, "CHAT_INVALID_OPPONENT", "자기 자신과 채팅할 수 없습니다."),
    CHAT_EMPTY_MESSAGE(HttpStatus.BAD_REQUEST, "CHAT_EMPTY_MESSAGE", "텍스트 메시지가 비어 있습니다."),
    CHAT_MESSAGE_TOO_LONG(HttpStatus.BAD_REQUEST, "CHAT_MESSAGE_TOO_LONG", "텍스트 메시지 길이를 초과했습니다."),
    CHAT_ADMIN_NOT_ALLOWED(HttpStatus.FORBIDDEN, "CHAT_ADMIN_NOT_ALLOWED", "ADMIN은 채팅에 참여할 수 없습니다."),
    CHAT_MEMBER_NOT_ACTIVE(HttpStatus.FORBIDDEN, "CHAT_MEMBER_NOT_ACTIVE", "탈퇴 또는 정지된 회원과는 채팅할 수 없습니다."),
    CHAT_ROOM_ACCESS_DENIED(HttpStatus.FORBIDDEN, "CHAT_ROOM_ACCESS_DENIED", "채팅방 참여자가 아닙니다."),
    CHAT_ROOM_LEFT(HttpStatus.FORBIDDEN, "CHAT_ROOM_LEFT", "채팅방을 나간 상태입니다. 채팅 시작하기를 통해 재입장해 주세요."),
    CHAT_WEBSOCKET_UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "CHAT_WEBSOCKET_UNAUTHORIZED", "WebSocket 인증에 실패했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}