package com.forpets.domain.post.controller;

import com.forpets.domain.member.entity.Region;
import com.forpets.domain.post.dto.CreatePostRequest;
import com.forpets.domain.post.dto.PostPageResponse;
import com.forpets.domain.post.dto.PostResponseDto;
import com.forpets.domain.post.dto.PostSearchCondition;
import com.forpets.domain.post.dto.UpdatePostRequest;
import com.forpets.domain.post.entity.Post;
import com.forpets.domain.post.entity.PostStatus;
import com.forpets.global.common.CareType;
import com.forpets.domain.post.service.PostService;
import com.forpets.global.common.ApiResponse;
import com.forpets.global.security.annotation.LoginUser;
import com.forpets.global.security.dto.CurrentMember;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    /*
    1. 공고 생성
     */
    @PostMapping
    public ResponseEntity<ApiResponse<PostResponseDto>> create(
            @LoginUser CurrentMember currentMember,
            @RequestBody @Valid CreatePostRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(postService.create(currentMember.id(), request)));
    }

    /*
    2. 공고 목록 조회
    - 캐시 전략: Cache-Control public, max-age=300
    - 정렬 화이트리스트: createdAt(기본), updatedAt, budgetAmount
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PostPageResponse>> search(
            @RequestParam(required = false) Region region,
            @RequestParam(required = false) CareType careType,
            @RequestParam(required = false) PostStatus status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sort
    ) {
        PostSearchCondition condition = new PostSearchCondition(
                region, careType, status, keyword
        );

        PostPageResponse response = postService.searchPosts(condition, page, size, sort);

        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=300")
                .body(ApiResponse.success(response));
    }

    /*
    3. 공고 상세 조회
     */
    @GetMapping("/{postId}")
    public ResponseEntity<ApiResponse<PostResponseDto>> getPost(@PathVariable Long postId) {
        return ResponseEntity.ok(ApiResponse.success(postService.getPost(postId)));
    }

    /*
    4. 공고 업데이트
    업데이트 하면 Pet이 ID 기준으로 최신 상태로 자동 업데이트 됨
     */
    @PutMapping("/{postId}")
    public ResponseEntity<ApiResponse<PostResponseDto>> update(
            @LoginUser CurrentMember currentMember,
            @PathVariable Long postId,
            @RequestBody @Valid UpdatePostRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success(postService.update(currentMember.id(), postId, request)));
    }

    /*
    5. 공고를 Close 상태로 전환
    어차피 CLOSE -> OPEN 은 정책상 불가능하도록 막아뒀으니까
    update status 관련 로직, request dto 제외하고 close 로 통일

    수정 시 모든 들어온 Proposal을 REJECT 처리해야함
     */
    @PatchMapping("/{postId}/close")
    public ResponseEntity<ApiResponse<PostResponseDto>> close(
            @LoginUser CurrentMember currentMember,
            @PathVariable Long postId) {
        return ResponseEntity.ok(
                ApiResponse.success(postService.closePost(currentMember.id(), postId)));
    }

    /*
    6. 공고 삭제
    삭제 할 때도 모든 Proposal을 REJECT 처리 하고 해야함
     */
    @DeleteMapping("/{postId}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @LoginUser CurrentMember currentMember,
            @PathVariable Long postId) {
        postService.delete(currentMember.id(), postId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /*
    test 용 공고 목록 조회
     */
//    @GetMapping("/test")
//    public ResponseEntity<ApiResponse<List<PostResponseDto>>> getTest(){
//        return ResponseEntity.ok(ApiResponse.success(postService.getTest()));
//    }
}
