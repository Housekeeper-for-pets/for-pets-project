package com.forpets.domain.admin.service;

import com.forpets.domain.member.entity.Member;
import com.forpets.domain.member.service.MemberService;
import com.forpets.domain.sitter.dto.admin.AdminSitterResponseDto;
import com.forpets.domain.sitter.entity.SitterApprovalStatus;
import com.forpets.domain.sitter.entity.SitterProfile;
import com.forpets.domain.sitter.repository.SitterProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SitterAdminService {
    private final SitterProfileRepository sitterProfileRepository;
    private final MemberService memberService;

    public List<AdminSitterResponseDto> getPendingSitters() {
        List<SitterProfile> pendingSitters = sitterProfileRepository
                .findAllByApprovalStatus(SitterApprovalStatus.PENDING);

        return pendingSitters.stream()
                .map(sitter -> {
                    Member member = memberService.findById(sitter.getMemberId());
                    return AdminSitterResponseDto.from(sitter, member.getRegion());
                })
                .toList();
    }
}
