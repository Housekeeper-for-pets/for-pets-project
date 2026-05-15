package com.forpets.domain.post.entity;

import com.forpets.global.common.CareType;
import com.forpets.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "post")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Post extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "author_id", nullable = false)
    private Long authorId;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false, length = 100)
    private String region;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CareType careType;

    private Integer budgetAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PostStatus status;

    @Column(nullable = false)
    private int viewCount;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Builder
    private Post(Long authorId, String title, String content, String region,
                 CareType careType, Integer budgetAmount) {
        this.authorId = authorId;
        this.title = title;
        this.content = content;
        this.region = region;
        this.careType = careType;
        this.budgetAmount = budgetAmount;
        this.status = PostStatus.OPEN;
        this.viewCount = 0;
    }

    public void update(String title, String content, String region, Integer budgetAmount) {
        this.title = title;
        this.content = content;
        this.region = region;
        this.budgetAmount = budgetAmount;
    }

    public void close() {
        this.status = PostStatus.CLOSED;
    }

    public void delete() {
        this.deletedAt = LocalDateTime.now();
    }

    public boolean isDeleted() {
        return this.deletedAt != null;
    }

    public void increaseViewCount() {
        this.viewCount++;
    }
}
