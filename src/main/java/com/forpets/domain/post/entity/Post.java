package com.forpets.domain.post.entity;

import com.forpets.global.common.CareType;
import com.forpets.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "post")
@SQLRestriction("deleted = false")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Post extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false, length = 100)
    private String region;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CareType careType;

    @Column
    private Integer budgetAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PostStatus status;

    @Column(nullable = false)
    private boolean deleted = false;

    @Column
    private LocalDateTime deletedAt;

    public void delete() {
        this.deleted = true;
        this.deletedAt = LocalDateTime.now();
    }

    @Builder
    private Post(Long memberId, String title, String content,
                 String region, CareType careType, Integer budgetAmount) {
        this.memberId = memberId;
        this.title = title;
        this.content = content;
        this.region = region;
        this.careType = careType;
        this.budgetAmount = budgetAmount;
        this.status = PostStatus.OPEN;
    }

    public void update(String title, String content, String region,
                       CareType careType, Integer budgetAmount) {
        this.title = title;
        this.content = content;
        this.region = region;
        this.careType = careType;
        this.budgetAmount = budgetAmount;
    }

    public void changeStatus(PostStatus status) {
        this.status = status;
    }

    public boolean isOpen() {
        return this.status == PostStatus.OPEN;
    }

    public boolean isOwnedBy(Long memberId) {
        return this.memberId.equals(memberId);
    }
}