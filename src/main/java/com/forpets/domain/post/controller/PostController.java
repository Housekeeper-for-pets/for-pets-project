package com.forpets.domain.post.controller;

import com.forpets.domain.post.dto.CreatePostRequest;
import com.forpets.domain.post.dto.PostResponseDto;
import com.forpets.domain.post.dto.UpdatePostRequest;
import com.forpets.domain.post.dto.UpdatePostStatusRequest;
import com.forpets.domain.post.service.PostService;
import com.forpets.global.common.ApiResponse;
import com.forpets.global.security.annotation.LoginUser;
import com.forpets.global.security.dto.CurrentMember;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    @PostMapping
    public ResponseEntity<ApiResponse<PostResponseDto>> create(
            @LoginUser CurrentMember currentMember,
            @RequestBody @Valid CreatePostRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(postService.create(currentMember.id(), request)));
    }

    @PutMapping("/{postId}")
    public ResponseEntity<ApiResponse<PostResponseDto>> update(
            @LoginUser CurrentMember currentMember,
            @PathVariable Long postId,
            @RequestBody @Valid UpdatePostRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success(postService.update(currentMember.id(), postId, request)));
    }

    @PatchMapping("/{postId}/status")
    public ResponseEntity<ApiResponse<PostResponseDto>> updateStatus(
            @LoginUser CurrentMember currentMember,
            @PathVariable Long postId,
            @RequestBody @Valid UpdatePostStatusRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success(postService.updateStatus(currentMember.id(), postId, request)));
    }

    @DeleteMapping("/{postId}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @LoginUser CurrentMember currentMember,
            @PathVariable Long postId) {
        postService.delete(currentMember.id(), postId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
