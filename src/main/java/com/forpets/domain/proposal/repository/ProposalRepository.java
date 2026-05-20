package com.forpets.domain.proposal.repository;

import com.forpets.domain.post.entity.PostStatus;
import com.forpets.domain.proposal.entity.Proposal;
import com.forpets.domain.proposal.entity.ProposalStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProposalRepository extends JpaRepository<Proposal, Long> {

    boolean existsByPostIdAndSitterProfileId(Long postId, Long sitterProfileId);

    List<Proposal> findAllByPostId(Long postId);

    List<Proposal> findAllByPostIdAndStatus(Long postId, ProposalStatus status);

    List<Proposal> findAllBySitterProfileId(Long sitterProfileId);

    boolean existsByPostIdAndStatus(Long postId, ProposalStatus status);

    boolean existsByPostIdAndStatusIn(Long postId, List<ProposalStatus> statuses);

    List<Proposal> findAllByMemberIdAndStatus(Long memberId, ProposalStatus status);

    List<Proposal> findAllBySitterProfileIdAndStatus(Long sitterProfileId, ProposalStatus status);

    boolean existsBySitterProfileIdAndStatusIn(Long sitterProfileId, List<ProposalStatus> statuses);

}