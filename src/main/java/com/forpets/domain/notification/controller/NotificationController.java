package com.forpets.domain.notification.controller;

import com.forpets.domain.notification.entity.Notification;
import com.forpets.domain.notification.service.NotificationService;
import com.forpets.global.common.ApiResponse;
import com.forpets.global.sse.SseEmitterManager;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final SseEmitterManager sseEmitterManager;

    /**
     * SSE 구독
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@RequestParam Long userId) {
        return sseEmitterManager.connect(userId);
    }

    /**
     * 알림 목록 조회
     */
    @GetMapping
    public ApiResponse<List<Notification>> getNotifications(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "false") boolean unreadOnly) {

        List<Notification> notifications = unreadOnly
                ? notificationService.getUnreadNotifications(userId)
                : notificationService.getMyNotifications(userId);

        return ApiResponse.success(notifications);
    }

    /**
     * 읽음 처리
     */
    @PatchMapping("/{id}/read")
    public ApiResponse<String> markAsRead(
            @PathVariable Long id,
            @RequestParam Long userId) {

        notificationService.markAsRead(id, userId);
        return ApiResponse.success("읽음 처리 완료");
    }

    /**
     * 미읽음 개수
     */
    @GetMapping("/unread-count")
    public ApiResponse<Map<String, Long>> getUnreadCount(
            @RequestParam Long userId) {

        long count = notificationService.getUnreadCount(userId);
        return ApiResponse.success(Map.of("count", count));
    }
}