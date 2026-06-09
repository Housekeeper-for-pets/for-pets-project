package com.forpets.domain.sitter.repository;

import com.forpets.domain.member.entity.MemberGender;
import com.forpets.domain.member.entity.QMember;
import com.forpets.domain.member.entity.Region;
import com.forpets.domain.sitter.dto.profile.SitterPageResponse;
import com.forpets.domain.sitter.dto.profile.SitterResponseDto;
import com.forpets.domain.sitter.dto.profile.SitterSearchCondition;
import com.forpets.domain.sitter.entity.QSitterProfile;
import com.forpets.domain.sitter.entity.SitterApprovalStatus;
import com.forpets.domain.sitter.entity.SitterProfile;
import com.forpets.domain.sitter.entity.SitterSchedule;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 시터 목록 동적 검색 QueryDSL 구현체입니다.
 * Member 테이블과 JOIN하여 region 필드를 함께 조회합니다.
 * API 명세의 캐시 전략(sitters:*) 무효화는 SitterService에서 담당합니다.
 */
@Repository
@RequiredArgsConstructor
public class SitterProfileRepositoryCustomImpl implements SitterProfileRepositoryCustom {

    private final JPAQueryFactory queryFactory;
    private final SitterScheduleRepository sitterScheduleRepository;

    private static final QSitterProfile sitter = QSitterProfile.sitterProfile;
    private static final QMember member = QMember.member;//member쪽 region을 join하기위함

    @Override
    public SitterPageResponse searchSitters(SitterSearchCondition condition, Pageable pageable) {

        // ── 데이터 쿼리 (Tuple로 sitter와 member.region을 한 번에 JOIN 조회하여 N+1 방지) ────────────────
        List<com.querydsl.core.Tuple> results = queryFactory
                .select(sitter, member.region, member.nickname, member.gender)
                .from(sitter)
                .join(member).on(member.id.eq(sitter.memberId))
                .where(
                        sitter.approvalStatus.eq(SitterApprovalStatus.APPROVED),
                        regionEq(condition),
                        possiblePetTypeEq(condition),
                        possiblePetSizeEq(condition),
                        minPriceGoe(condition),
                        maxPriceLoe(condition),
                        genderEq(condition)
                )
                .orderBy(buildOrderSpecifier(pageable))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        // ── 카운트 쿼리 ──────────────────────────────────────────────────────
        Long total = queryFactory
                .select(sitter.count())
                .from(sitter)
                .join(member).on(member.id.eq(sitter.memberId))
                .where(
                        sitter.approvalStatus.eq(SitterApprovalStatus.APPROVED),
                        regionEq(condition),
                        possiblePetTypeEq(condition),
                        possiblePetSizeEq(condition),
                        minPriceGoe(condition),
                        maxPriceLoe(condition),
                        genderEq(condition)
                )
                .fetchOne();

        long totalElements = total == null ? 0L : total;
        int totalPages = (pageable.getPageSize() == 0)
                ? 0
                : (int) Math.ceil((double) totalElements / pageable.getPageSize());

        // sitterProfileId 목록 추출
        List<Long> sitterIds = results.stream()
                .map(t -> t.get(sitter).getId())
                .toList();

        // 스케쥴이 비었을 경우 early return
        if (sitterIds.isEmpty()) {
            return SitterPageResponse.of(List.of(), totalElements, totalPages,
                    pageable.getPageNumber(), pageable.getPageSize());
        }

        // 스케줄 IN절로 한 번에 조회
        Map<Long, List<SitterSchedule>> schedulesMap = sitterScheduleRepository
                .findAllBySitterProfileIdIn(sitterIds)
                .stream()
                .collect(Collectors.groupingBy(SitterSchedule::getSitterProfileId));

        // DTO 변환 시 스케줄 주입
        List<SitterResponseDto> content = results.stream()
                .map(tuple -> {
                    SitterProfile s = tuple.get(sitter);
                    Region region = tuple.get(member.region);
                    String nickname = tuple.get(member.nickname);
                    MemberGender gender = tuple.get(member.gender);
                    List<SitterSchedule> schedules = schedulesMap.getOrDefault(s.getId(), List.of());
                    return SitterResponseDto.from(
                            s,
                            region != null ? region : Region.UNKNOWN,
                            nickname,
                            gender,
                            schedules
                    );
                })
                .toList();

        return SitterPageResponse.of(content, totalElements, totalPages, pageable.getPageNumber(), pageable.getPageSize());
    }

    // ── WHERE 조건 빌더 ────────────────────────────────────────────────────

    private BooleanExpression regionEq(SitterSearchCondition condition) {
        return condition.region() != null ? member.region.eq(condition.region()) : null;
    }

    private BooleanExpression possiblePetTypeEq(SitterSearchCondition condition) {
        if (condition.possiblePetType() == null) return null;
        return sitter.possiblePetType.eq(condition.possiblePetType())
                .or(sitter.possiblePetType.eq(com.forpets.domain.sitter.entity.PossiblePetType.ALL));
    }

    private BooleanExpression possiblePetSizeEq(SitterSearchCondition condition) {
        if (condition.possiblePetSize() == null) return null;
        return sitter.possiblePetSize.eq(condition.possiblePetSize())
                .or(sitter.possiblePetSize.eq(com.forpets.domain.sitter.entity.PossiblePetSize.ALL));
    }

    private BooleanExpression minPriceGoe(SitterSearchCondition condition) {
        return condition.minPrice() != null ? sitter.pricePerHour.goe(condition.minPrice()) : null;
    }

    private BooleanExpression maxPriceLoe(SitterSearchCondition condition) {
        return condition.maxPrice() != null ? sitter.pricePerHour.loe(condition.maxPrice()) : null;
    }

    private BooleanExpression genderEq(SitterSearchCondition condition) {
        return condition.gender() != null ? member.gender.eq(condition.gender()) : null;
    }

    // ── 정렬 빌더 ─────────────────────────────────────────────────────────

    /**
     * Pageable 의 Sort 에서 첫 번째 정렬 조건을 꺼내 QueryDSL OrderSpecifier 로 변환합니다.
     * 화이트리스트(createdAt, pricePerHour, experienceYears, averageRating) 이외 필드는 호출 전에 이미 검증됩니다.
     */
    private OrderSpecifier<?> buildOrderSpecifier(Pageable pageable) {
        if (pageable.getSort().isSorted()) {
            org.springframework.data.domain.Sort.Order order =
                    pageable.getSort().iterator().next();
            boolean isAsc = order.isAscending();

            return switch (order.getProperty()) {
                case "pricePerHour" -> isAsc ? sitter.pricePerHour.asc() : sitter.pricePerHour.desc();
                case "experienceYears" -> isAsc ? sitter.experienceYears.asc() : sitter.experienceYears.desc();
                case "averageRating" -> isAsc ? sitter.averageRating.asc() : sitter.averageRating.desc();
                default -> isAsc ? sitter.createdAt.asc() : sitter.createdAt.desc();
            };
        }
        // 기본 정렬: createdAt DESC
        return sitter.createdAt.desc();
    }
}
