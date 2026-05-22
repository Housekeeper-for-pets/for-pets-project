package com.forpets.domain.member.repository;

import com.forpets.domain.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

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

    // 탈퇴 회원 포함 단건 조회
    @Query(value = "SELECT * FROM member where id = :id", nativeQuery = true)
    Optional<Member> findByIdIncludingDeleted(@Param("id") Long id);

    // 탈퇴 회원 포함 다건 조회
    @Query(value = "SELECT * FROM member WHERE id IN (:ids)", nativeQuery = true)
    List<Member> findAllByIdIncludingDeleted(@Param("ids") List<Long> ids);
}
