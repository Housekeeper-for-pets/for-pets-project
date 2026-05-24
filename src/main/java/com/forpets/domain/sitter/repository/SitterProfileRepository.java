package com.forpets.domain.sitter.repository;

import com.forpets.domain.sitter.entity.SitterApprovalStatus;
import com.forpets.domain.sitter.entity.SitterProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SitterProfileRepository extends JpaRepository<SitterProfile, Long>, SitterProfileRepositoryCustom {

    @Query(value =
            "SELECT COUNT(*) FROM sitter_profile WHERE member_id = :memberId",
            nativeQuery = true)
    int countByMemberIdIncludingDeleted(@Param("memberId") Long memberId);

    boolean existsByMemberId(Long memberId);

    Optional<SitterProfile> findByMemberId(Long memberId);

    List<SitterProfile> findAllByApprovalStatus(SitterApprovalStatus sitterApprovalStatus);
}
