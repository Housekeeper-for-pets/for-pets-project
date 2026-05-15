package com.forpets.domain.member.repository;

import com.forpets.domain.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 회원 조회와 중복 검사를 담당합니다.
 * 탈퇴 회원도 이메일과 닉네임을 재사용할 수 없도록 전체 상태를 대상으로 중복 검사합니다.
 */
public interface MemberRepository extends JpaRepository<Member, Long> {

    Optional<Member> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByNickname(String nickname);
}