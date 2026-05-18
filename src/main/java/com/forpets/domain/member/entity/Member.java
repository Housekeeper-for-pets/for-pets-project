package com.forpets.domain.member.entity;

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
@Table(name = "member")
@SQLRestriction("deleted = false")  // 추가
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, length = 50, unique = true)
    private String nickname;

    @Column(length = 30)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Region region;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MemberGender gender;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MemberRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MemberStatus status;

    @Column(nullable = false)
    private boolean deleted = false;  // 추가

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Builder
    private Member(
            String email,
            String password,
            String nickname,
            String phone,
            MemberGender gender,
            Region region
    ) {
        this.email = email;
        this.password = password;
        this.nickname = nickname;
        this.phone = phone;
        this.gender = gender == null ? MemberGender.UNKNOWN : gender;
        this.region = region == null ? Region.UNKNOWN : region;
        this.role = MemberRole.MEMBER;
        this.status = MemberStatus.ACTIVE;
    }

    public void changeRoleToSitter() {
        this.role = MemberRole.SITTER;
    }

    public void restoreRoleToMember() {
        this.role = MemberRole.MEMBER;
    }

    public void updateProfile(String nickname, String phone, MemberGender gender, Region region) {
        this.nickname = nickname;
        this.phone = phone;
        this.gender = gender == null ? MemberGender.UNKNOWN : gender;
        this.region = region == null ? Region.UNKNOWN : region;
    }

    public void changePassword(String encodedPassword) {
        this.password = encodedPassword;
    }

    public void delete() {
        this.deleted = true;
        this.deletedAt = LocalDateTime.now();
    }

    public boolean isDeleted() {
        return this.deleted;
    }

    public boolean isSuspended() {
        return this.status == MemberStatus.SUSPENDED;
    }

    public boolean isActive() {
        return this.status == MemberStatus.ACTIVE && !this.deleted;  // deletedAt → !this.deleted
    }
}
