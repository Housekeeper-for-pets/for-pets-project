package com.forpets.domain.admin.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
public class AdminController {

    // 1. 승인 대기 시터 프로필 목록 조회
    @GetMapping("/sitters")



    // 2. 시터 프로필 요청 승인
    @PostMapping("/sitters/{sitterProfileId}/approve")



    // 3. 시터 프로필 요청 거절
    @PostMapping("/sitters/{sitterProfileId}/reject")

}
