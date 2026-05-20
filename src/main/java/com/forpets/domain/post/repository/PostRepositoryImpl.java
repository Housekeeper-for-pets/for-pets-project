package com.forpets.domain.post.repository;

import com.forpets.domain.member.entity.QMember;
import com.forpets.domain.member.entity.Region;
import com.forpets.domain.post.dto.PostPageResponse;
import com.forpets.domain.post.dto.PostResponseDto;
import com.forpets.domain.post.dto.PostSearchCondition;
import com.forpets.domain.post.entity.Post;
import com.forpets.domain.post.entity.PostPet;
import com.forpets.domain.post.entity.PostTimeSlot;
import com.forpets.domain.post.entity.QPost;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.forpets.domain.member.entity.QMember.member;
import static com.forpets.domain.post.entity.QPost.post;

@Repository
@RequiredArgsConstructor
public class PostRepositoryImpl implements PostRepositoryCustom {

    private final JPAQueryFactory queryFactory;
    private final PostPetRepository postPetRepository;
    private final PostTimeSlotRepository postTimeSlotRepository;

    @Override
    public PostPageResponse searchPosts(PostSearchCondition condition, Pageable pageable) {

        // 1. 메인 쿼리: post + member.region을 Tuple로 한 번에 조회 (N+1 해결 - memberRegion)
        List<Tuple> tuples = queryFactory
                .select(post, member.region)
                .from(post)
                .leftJoin(member).on(post.memberId.eq(member.id))
                .where(
                        eqRegion(condition),
                        eqCareType(condition),
                        eqStatus(condition),
                        containsKeyword(condition)
                )
                .orderBy(getOrderSpecifiers(pageable))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        // 2. count 쿼리
        JPAQuery<Long> countQuery = queryFactory
                .select(post.count())
                .from(post)
                .leftJoin(member).on(post.memberId.eq(member.id))
                .where(
                        eqRegion(condition),
                        eqCareType(condition),
                        eqStatus(condition),
                        containsKeyword(condition)
                );

        long totalElements = countQuery.fetchOne() != null ? countQuery.fetchOne() : 0L;
        int totalPages = (int) Math.ceil((double) totalElements / pageable.getPageSize());

        // 3. postId 목록 추출
        List<Long> postIds = tuples.stream()
                .map(t -> t.get(post).getId())
                .toList();

        if (postIds.isEmpty()) {
            return PostPageResponse.of(
                    List.of(),
                    totalElements,
                    totalPages,
                    pageable.getPageNumber(),
                    pageable.getPageSize()
            );
        }

        // 4. pets, timeSlots를 IN절로 한 번에 조회 (N+1 해결 - pets, timeSlots)
        Map<Long, List<PostPet>> petsMap = postPetRepository.findAllByPostIdIn(postIds)
                .stream()
                .collect(Collectors.groupingBy(PostPet::getPostId));

        Map<Long, List<PostTimeSlot>> timeSlotsMap = postTimeSlotRepository
                .findAllByPostIdInOrderByTimeSlotInfoSequence(postIds)
                .stream()
                .collect(Collectors.groupingBy(PostTimeSlot::getPostId));

        // 5. DTO 변환 (추가 쿼리 없음)
        List<PostResponseDto> content = tuples.stream()
                .map(t -> {
                    Post p = t.get(post);
                    Region memberRegion = t.get(member.region);
                    List<PostPet> pets = petsMap.getOrDefault(p.getId(), List.of());
                    List<PostTimeSlot> timeSlots = timeSlotsMap.getOrDefault(p.getId(), List.of());
                    return PostResponseDto.from(p, memberRegion, pets, timeSlots);
                })
                .toList();

        return PostPageResponse.of(
                content,
                totalElements,
                totalPages,
                pageable.getPageNumber(),
                pageable.getPageSize()
        );
    }

    private BooleanExpression eqRegion(PostSearchCondition condition) {
        if (condition.region() == null) return null;
        return member.region.eq(condition.region());
    }

    private BooleanExpression eqCareType(PostSearchCondition condition) {
        if (condition.careType() == null) return null;
        return post.careType.eq(condition.careType());
    }

    private BooleanExpression eqStatus(PostSearchCondition condition) {
        if (condition.status() == null) return null;
        return post.status.eq(condition.status());
    }

    private BooleanExpression containsKeyword(PostSearchCondition condition) {
        if (condition.keyword() == null || condition.keyword().trim().isEmpty()) return null;
        return post.title.containsIgnoreCase(condition.keyword())
                .or(post.content.containsIgnoreCase(condition.keyword()));
    }

    private OrderSpecifier<?>[] getOrderSpecifiers(Pageable pageable) {
        List<OrderSpecifier<?>> orderSpecifiers = new ArrayList<>();

        if (pageable.getSort().isUnsorted()) {
            return new OrderSpecifier[]{new OrderSpecifier<>(Order.DESC, post.createdAt)};
        }

        for (Sort.Order order : pageable.getSort()) {
            Order direction = order.getDirection().isAscending() ? Order.ASC : Order.DESC;
            switch (order.getProperty()) {
                case "createdAt" -> orderSpecifiers.add(new OrderSpecifier<>(direction, post.createdAt));
                case "updatedAt" -> orderSpecifiers.add(new OrderSpecifier<>(direction, post.updatedAt));
                case "budgetAmount" -> orderSpecifiers.add(new OrderSpecifier<>(direction, post.budgetAmount));
            }
        }
        return orderSpecifiers.toArray(new OrderSpecifier[0]);
    }
}