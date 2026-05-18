package com.forpets.domain.member.repository;

import com.forpets.domain.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * 회원 조회와 중복 검사를 담당합니다.
 * 탈퇴 회원도 이메일과 닉네임을 재사용할 수 없도록 전체 상태를 대상으로 중복 검사합니다.
 */
public interface MemberRepository extends JpaRepository<Member, Long> {

    Optional<Member> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByNickname(String nickname);

    // 탈퇴 회원 포함 이메일 중복 체크
    @Query(value = "SELECT COUNT(*) FROM member WHERE email = :email", nativeQuery = true)
    int countByEmailIncludingDeleted(@Param("email") String email);

    // 탈퇴 회원 포함 닉네임 중복 체크
    @Query(value = "SELECT COUNT(*) FROM member WHERE nickname = :nickname", nativeQuery = true)
    int countByNicknameIncludingDeleted(@Param("nickname") String nickname);
}