package com.forpets.domain.pet.service;


import com.forpets.domain.pet.dto.CreatePetRequest;
import com.forpets.domain.pet.dto.UpdatePetRequest;
import com.forpets.domain.pet.dto.PetResponseDto;
import com.forpets.domain.pet.entity.Pet;
import com.forpets.domain.pet.entity.PetGender;
import com.forpets.domain.pet.entity.PetSize;
import com.forpets.domain.pet.entity.PetSpecies;
import com.forpets.domain.pet.exception.PetErrorCode;
import com.forpets.domain.pet.exception.PetException;
import com.forpets.domain.pet.repository.PetRepository;
import com.forpets.global.common.AssociationChecker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class PetServiceTest {

    @InjectMocks
    private PetService petService;

    @Mock
    private PetRepository petRepository;

    @Mock
    private AssociationChecker associationChecker;

    private Pet pet;
    private final Long memberId = 1L;
    private final Long otherMemberId = 2L;
    private final Long petId = 1L;

    @BeforeEach
    void setUp() {
        pet = Pet.builder()
                .memberId(memberId)
                .name("타코")
                .species(PetSpecies.CAT)
                .breed("코리안숏헤어")
                .size(PetSize.SMALL)
                .age(4)
                .gender(PetGender.MALE)
                .profileImageUrl("https://example.com/tacocat.jpg")
                .note("시끄러워요")
                .build();
        ReflectionTestUtils.setField(pet, "id", petId);
    }

    // ========================================================
    // 반려동물 등록 — POST /api/pets
    // ========================================================
    @Nested
    @DisplayName("반려동물 등록 — POST /api/pets")
    class CreatePetTest {

        @Test
        @DisplayName("[성공] 로그인된 회원이 반려동물 등록 성공 (name, species 필수)")
        void pet_test_01() {
            // given
            CreatePetRequest request = new CreatePetRequest(
                    "타코", PetSpecies.CAT, "코리안숏헤어", PetSize.SMALL,
                    4, PetGender.MALE, "https://example.com/tacocat.jpg", "시끄러워요"
            );
            given(petRepository.countByMemberId(memberId)).willReturn(0L);
            given(petRepository.save(any(Pet.class))).willReturn(pet);

            // when
            PetResponseDto result = petService.create(memberId, request);

            // then
            assertThat(result.id()).isEqualTo(petId);
            assertThat(result.memberId()).isEqualTo(memberId);
            assertThat(result.name()).isEqualTo("타코");
            assertThat(result.species()).isEqualTo(PetSpecies.CAT);
            then(petRepository).should().save(any(Pet.class));
        }

        @Test
        @DisplayName("[실패] 반려동물 10마리 초과 등록 시 차단")
        void pet_test_02() {
            // given
            CreatePetRequest request = new CreatePetRequest(
                    "타코", PetSpecies.CAT, null, null,
                    null, null, null, null
            );
            given(petRepository.countByMemberId(memberId)).willReturn(10L);

            // when & then
            assertThatThrownBy(() -> petService.create(memberId, request))
                    .isInstanceOf(PetException.class)
                    .satisfies(ex -> assertThat(((PetException) ex).getErrorCode())
                            .isEqualTo(PetErrorCode.PET_LIMIT_EXCEEDED));
        }
    }

    // ========================================================
    // 내 반려동물 목록 조회 — GET /api/pets
    // ========================================================
    @Nested
    @DisplayName("내 반려동물 목록 조회 — GET /api/pets")
    class GetMyPetsTest {

        @Test
        @DisplayName("[성공] 내 반려동물 목록 조회 성공 — 본인 소유 목록만 반환")
        void pet_test_03() {
            // given
            given(petRepository.findAllByMemberId(memberId)).willReturn(List.of(pet));

            // when
            List<PetResponseDto> result = petService.getMyPets(memberId);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).memberId()).isEqualTo(memberId);
            assertThat(result.get(0).name()).isEqualTo("타코");
        }
    }

    // ========================================================
    // 반려동물 상세 조회 — GET /api/pets/{petId}
    // ========================================================
    @Nested
    @DisplayName("반려동물 상세 조회 — GET /api/pets/{petId}")
    class GetByIdTest {

        @Test
        @DisplayName("[성공] 반려동물 상세 조회 성공")
        void pet_test_04() {
            // given
            given(petRepository.findById(petId)).willReturn(Optional.of(pet));

            // when
            PetResponseDto result = petService.getById(memberId, petId);

            // then
            assertThat(result.id()).isEqualTo(petId);
            assertThat(result.name()).isEqualTo("타코");
            assertThat(result.species()).isEqualTo(PetSpecies.CAT);
            assertThat(result.breed()).isEqualTo("코리안숏헤어");
            assertThat(result.size()).isEqualTo(PetSize.SMALL);
            assertThat(result.age()).isEqualTo(4);
            assertThat(result.gender()).isEqualTo(PetGender.MALE);
            assertThat(result.note()).isEqualTo("시끄러워요");
        }

        @Test
        @DisplayName("[실패] 타인의 반려동물 조회 시 차단")
        void pet_test_05() {
            // given
            given(petRepository.findById(petId)).willReturn(Optional.of(pet));

            // when & then
            assertThatThrownBy(() -> petService.getById(otherMemberId, petId))
                    .isInstanceOf(PetException.class)
                    .satisfies(ex -> assertThat(((PetException) ex).getErrorCode())
                            .isEqualTo(PetErrorCode.NOT_PET_OWNER));
        }

        @Test
        @DisplayName("[실패] 존재하지 않는 반려동물 조회")
        void pet_test_06() {
            // given
            given(petRepository.findById(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> petService.getById(memberId, 999L))
                    .isInstanceOf(PetException.class)
                    .satisfies(ex -> assertThat(((PetException) ex).getErrorCode())
                            .isEqualTo(PetErrorCode.PET_NOT_FOUND));
        }
    }

    // ========================================================
    // 반려동물 정보 수정 — PUT /api/pets/{petId}
    // ========================================================
    @Nested
    @DisplayName("반려동물 정보 수정 — PUT /api/pets/{petId}")
    class UpdatePetTest {

        @Test
        @DisplayName("[성공] 반려동물 정보 수정 성공")
        void pet_test_07() {
            // given
            UpdatePetRequest request = new UpdatePetRequest(
                    "야끼", PetSpecies.CAT, "코리안롱헤어", PetSize.SMALL,
                    5, PetGender.MALE, "https://example.com/yakki.jpg", "여전히 시끄러워요"
            );
            given(petRepository.findById(petId)).willReturn(Optional.of(pet));

            // when
            PetResponseDto result = petService.update(memberId, petId, request);

            // then
            assertThat(result.name()).isEqualTo("야끼");
            assertThat(result.breed()).isEqualTo("코리안롱헤어");
            assertThat(result.age()).isEqualTo(5);
            assertThat(result.gender()).isEqualTo(PetGender.MALE);
            assertThat(result.note()).isEqualTo("여전히 시끄러워요");
        }

        @Test
        @DisplayName("[실패] 타인의 반려동물 수정 시 차단")
        void pet_test_08() {
            // given
            UpdatePetRequest request = new UpdatePetRequest(
                    "야끼", PetSpecies.CAT, "코리안롱헤어", PetSize.SMALL,
                    5, PetGender.MALE, "https://example.com/yakki.jpg", "여전히 시끄러워요"
            );
            given(petRepository.findById(petId)).willReturn(Optional.of(pet));

            // when & then
            assertThatThrownBy(() -> petService.update(otherMemberId, petId, request))
                    .isInstanceOf(PetException.class)
                    .satisfies(ex -> assertThat(((PetException) ex).getErrorCode())
                            .isEqualTo(PetErrorCode.NOT_PET_OWNER));
        }

        @Test
        @DisplayName("[실패] 존재하지 않는 반려동물 수정")
        void pet_test_09() {
            // given
            UpdatePetRequest request = new UpdatePetRequest(
                    "야끼", PetSpecies.CAT, "코리안롱헤어", PetSize.SMALL,
                    5, PetGender.MALE, "https://example.com/yakki.jpg", "여전히 시끄러워요"
            );
            given(petRepository.findById(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> petService.update(memberId, 999L, request))
                    .isInstanceOf(PetException.class)
                    .satisfies(ex -> assertThat(((PetException) ex).getErrorCode())
                            .isEqualTo(PetErrorCode.PET_NOT_FOUND));
        }
    }

    // ========================================================
    // 반려동물 삭제 — DELETE /api/pets/{petId}
    // ========================================================
    @Nested
    @DisplayName("반려동물 삭제 — DELETE /api/pets/{petId}")
    class DeletePetTest {

        @Test
        @DisplayName("[성공] 반려동물 삭제 성공 — 연관된 Post/CareRequest/Reservation 없는 경우")
        void pet_test_10() {
            // given
            given(petRepository.findById(petId)).willReturn(Optional.of(pet));
            given(associationChecker.hasPetActiveAssociation(petId)).willReturn(false);

            // when
            petService.delete(memberId, petId);

            // then
            assertThat(pet.isDeleted()).isTrue();
        }

        @Test
        @DisplayName("[실패] 타인의 반려동물 삭제 시 차단")
        void pet_test_11() {
            // given
            given(petRepository.findById(petId)).willReturn(Optional.of(pet));

            // when & then
            assertThatThrownBy(() -> petService.delete(otherMemberId, petId))
                    .isInstanceOf(PetException.class)
                    .satisfies(ex -> assertThat(((PetException) ex).getErrorCode())
                            .isEqualTo(PetErrorCode.NOT_PET_OWNER));
        }

        @Test
        @DisplayName("[실패] 존재하지 않는 반려동물 삭제")
        void pet_test_12() {
            // given
            given(petRepository.findById(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> petService.delete(memberId, 999L))
                    .isInstanceOf(PetException.class)
                    .satisfies(ex -> assertThat(((PetException) ex).getErrorCode())
                            .isEqualTo(PetErrorCode.PET_NOT_FOUND));
        }

        @Test
        @DisplayName("[실패] 연관된 Post/CareRequest/Reservation이 존재하는 반려동물 삭제 시 차단")
        void pet_test_13() {
            // given
            given(petRepository.findById(petId)).willReturn(Optional.of(pet));
            given(associationChecker.hasPetActiveAssociation(petId)).willReturn(true);

            // when & then
            assertThatThrownBy(() -> petService.delete(memberId, petId))
                    .isInstanceOf(PetException.class)
                    .satisfies(ex -> assertThat(((PetException) ex).getErrorCode())
                            .isEqualTo(PetErrorCode.PET_USED_IN_ACTIVE_PROCESS));
        }

    }
}
