package com.forpets.domain.post.service;

import com.forpets.domain.member.entity.Member;
import com.forpets.domain.member.entity.MemberGender;
import com.forpets.domain.member.entity.Region;
import com.forpets.domain.member.service.MemberService;
import com.forpets.domain.pet.entity.Pet;
import com.forpets.domain.pet.entity.PetGender;
import com.forpets.domain.pet.entity.PetSize;
import com.forpets.domain.pet.entity.PetSpecies;
import com.forpets.domain.pet.service.PetService;
import com.forpets.domain.post.dto.CreatePostRequest;
import com.forpets.domain.post.dto.PostResponseDto;
import com.forpets.domain.post.entity.Post;
import com.forpets.domain.post.entity.PostPet;
import com.forpets.domain.post.entity.PostStatus;
import com.forpets.domain.post.entity.PostTimeSlot;
import com.forpets.domain.post.repository.PostPetRepository;
import com.forpets.domain.post.repository.PostRepository;
import com.forpets.domain.post.repository.PostTimeSlotRepository;
import com.forpets.domain.proposal.repository.ProposalRepository;
import com.forpets.global.cache.support.GracefulDegradationCacheTestConfiguration;
import com.forpets.global.common.CareType;
import com.forpets.global.embed.TimeSlotValidator;
import com.forpets.global.embed.dto.TimeSlotRequest;
import com.forpets.global.embed.entity.TimeSlotInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.Mockito.mock;

@SpringJUnitConfig(classes = {
        PostService.class,
        GracefulDegradationCacheTestConfiguration.class,
        PostServiceCacheEvictGracefulDegradationTest.MockConfig.class
})
class PostServiceCacheEvictGracefulDegradationTest {

    @Autowired
    private PostService postService;

    @Autowired
    private MemberService memberService;

    @Autowired
    private PetService petService;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private PostPetRepository postPetRepository;

    @Autowired
    private PostTimeSlotRepository postTimeSlotRepository;

    @Autowired
    private TimeSlotValidator timeSlotValidator;

    private final Long memberId = 1L;
    private final Long petId = 10L;
    private final Long postId = 300L;

    private Member member;
    private Pet pet;
    private Post post;

    @BeforeEach
    void setUp() {
        member = Member.builder()
                .email("writer@test.com")
                .password("password")
                .nickname("writer")
                .phone("010-0000-0000")
                .gender(MemberGender.MALE)
                .region(Region.GANGNAM)
                .build();
        ReflectionTestUtils.setField(member, "id", memberId);

        pet = Pet.builder()
                .memberId(memberId)
                .name("초코")
                .species(PetSpecies.DOG)
                .breed("푸들")
                .size(PetSize.SMALL)
                .age(3)
                .gender(PetGender.MALE)
                .build();
        ReflectionTestUtils.setField(pet, "id", petId);

        post = Post.builder()
                .memberId(memberId)
                .title("공고")
                .content("내용")
                .careType(CareType.VISIT)
                .budgetAmount(50000)
                .build();
        ReflectionTestUtils.setField(post, "id", postId);
    }

    @Test
    @DisplayName("캐시 EVICT 실패해도 공고 등록은 정상 완료된다")
    void create_completes_when_cache_evict_fails() {
        CreatePostRequest request = new CreatePostRequest(
                "공고", "내용", List.of(petId), CareType.VISIT, 50000,
                List.of(new TimeSlotRequest(
                        LocalDate.now().plusDays(3),
                        LocalTime.of(10, 0),
                        LocalTime.of(12, 0)
                ))
        );

        given(memberService.findById(memberId)).willReturn(member);
        given(petService.findById(petId)).willReturn(pet);
        willDoNothing().given(timeSlotValidator).validate(anyList());
        given(postRepository.save(any(Post.class))).willReturn(post);
        given(postPetRepository.saveAll(anyList())).willReturn(List.of(PostPet.createFrom(postId, pet)));
        given(postTimeSlotRepository.saveAll(anyList())).willReturn(List.of(
                PostTimeSlot.create(postId, TimeSlotInfo.of(
                        LocalDate.now().plusDays(3), LocalTime.of(10, 0), LocalTime.of(12, 0), 1))
        ));

        assertThatCode(() -> {
            PostResponseDto result = postService.create(memberId, request);
            assertThat(result.status()).isEqualTo(PostStatus.OPEN);
            assertThat(result.title()).isEqualTo("공고");
        }).doesNotThrowAnyException();
    }

    @TestConfiguration
    static class MockConfig {
        @Bean
        MemberService memberService() {
            return mock(MemberService.class);
        }

        @Bean
        PetService petService() {
            return mock(PetService.class);
        }

        @Bean
        PostRepository postRepository() {
            return mock(PostRepository.class);
        }

        @Bean
        PostPetRepository postPetRepository() {
            return mock(PostPetRepository.class);
        }

        @Bean
        PostTimeSlotRepository postTimeSlotRepository() {
            return mock(PostTimeSlotRepository.class);
        }

        @Bean
        TimeSlotValidator timeSlotValidator() {
            return mock(TimeSlotValidator.class);
        }

        @Bean
        ProposalRepository proposalRepository() {
            return mock(ProposalRepository.class);
        }

        @Bean
        PostCacheService postCacheService() {
            return mock(PostCacheService.class);
        }
    }
}
