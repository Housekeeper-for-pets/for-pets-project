package com.forpets.global.init;

import com.forpets.domain.carerequest.entity.CareRequest;
import com.forpets.domain.carerequest.entity.CareRequestPet;
import com.forpets.domain.carerequest.entity.CareRequestTimeSlot;
import com.forpets.domain.carerequest.repository.CareRequestPetRepository;
import com.forpets.domain.carerequest.repository.CareRequestRepository;
import com.forpets.domain.carerequest.repository.CareRequestTimeSlotRepository;
import com.forpets.domain.member.entity.Member;
import com.forpets.domain.member.entity.MemberGender;
import com.forpets.domain.member.entity.Region;
import com.forpets.domain.member.repository.MemberRepository;
import com.forpets.domain.pet.entity.Pet;
import com.forpets.domain.pet.entity.PetGender;
import com.forpets.domain.pet.entity.PetSize;
import com.forpets.domain.pet.entity.PetSpecies;
import com.forpets.domain.pet.repository.PetRepository;
import com.forpets.domain.post.entity.Post;
import com.forpets.domain.post.entity.PostPet;
import com.forpets.domain.post.entity.PostStatus;
import com.forpets.domain.post.entity.PostTimeSlot;
import com.forpets.domain.post.repository.PostPetRepository;
import com.forpets.domain.post.repository.PostRepository;
import com.forpets.domain.post.repository.PostTimeSlotRepository;
import com.forpets.domain.proposal.entity.Proposal;
import com.forpets.domain.proposal.repository.ProposalRepository;
import com.forpets.domain.reservation.entity.*;
import com.forpets.domain.reservation.repository.ReservationPaymentRepository;
import com.forpets.domain.reservation.repository.ReservationPetRepository;
import com.forpets.domain.reservation.repository.ReservationRepository;
import com.forpets.domain.reservation.repository.ReservationTimeSlotRepository;
import com.forpets.domain.sitter.entity.*;
import com.forpets.domain.sitter.repository.SitterProfileRepository;
import com.forpets.domain.sitter.repository.SitterScheduleRepository;
import com.forpets.global.common.CareType;
import com.forpets.global.embed.entity.PetSnapshot;
import com.forpets.global.embed.entity.TimeSlotInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;

@Component
@RequiredArgsConstructor
public class PostmanDataInit implements CommandLineRunner {

    private final MemberRepository memberRepository;
    private final PetRepository petRepository;
    private final SitterProfileRepository sitterProfileRepository;
    private final SitterScheduleRepository scheduleRepository;
    private final PostRepository postRepository;
    private final PostPetRepository postPetRepository;
    private final PostTimeSlotRepository postTimeSlotRepository;
    private final ProposalRepository proposalRepository;
    private final CareRequestRepository careRequestRepository;
    private final CareRequestPetRepository careRequestPetRepository;
    private final CareRequestTimeSlotRepository careRequestTimeSlotRepository;
    private final ReservationRepository reservationRepository;
    private final ReservationPetRepository reservationPetRepository;
    private final ReservationTimeSlotRepository reservationTimeSlotRepository;
    private final ReservationPaymentRepository reservationPaymentRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {

        // =============================================
        // 1. Member 6명 (admin 포함)
        // =============================================
        Member member1 = saveMember("giljung@test.com", "째길중", "010-1111-1111", MemberGender.MALE, Region.SEOCHO);
        Member member2 = saveMember("jiwon@test.com", "타코맘", "010-2222-2222", MemberGender.FEMALE, Region.DONGJAK);
        Member member3 = saveMember("jimin@test.com", "지민냥", "010-3333-3333", MemberGender.FEMALE, Region.GANGNAM);
        Member member4 = saveMember("seon@test.com", "점선면", "010-4444-4444", MemberGender.MALE, Region.GWANGJIN);
        Member member5 = saveMember("yeongsoo@test.com", "박제로수", "010-5555-5555", MemberGender.MALE, Region.YONGSAN);
        Member adminMember = saveMember("dragonRock@test.com", "소란석", "010-0000-0000", MemberGender.MALE, Region.YONGSAN);

        // =============================================
        // 2. Pet 등록
        // =============================================
        // member1(길중) - 구피 9마리
        Pet pet1 = savePet(member1.getId(), "일피", PetSpecies.ETC, "하얀구피", PetSize.SMALL, 3, PetGender.MALE, "낯가림 있음");
        Pet pet2 = savePet(member1.getId(), "이피", PetSpecies.ETC, "빨간구피", PetSize.SMALL, 2, PetGender.MALE, "조금 뚱뚱함");
        Pet pet3 = savePet(member1.getId(), "삼피", PetSpecies.ETC, "하얀구피", PetSize.SMALL, 3, PetGender.FEMALE, "쌍피로 이름 짓고 싶었음");
        Pet pet4 = savePet(member1.getId(), "사피", PetSpecies.ETC, "하얀구피", PetSize.SMALL, 3, PetGender.FEMALE, null);
        Pet pet5 = savePet(member1.getId(), "오피", PetSpecies.ETC, "하얀구피", PetSize.SMALL, 3, PetGender.FEMALE, null);
        Pet pet6 = savePet(member1.getId(), "육피", PetSpecies.ETC, "하얀구피", PetSize.SMALL, 2, PetGender.MALE, "잘 헤엄침");
        Pet pet7 = savePet(member1.getId(), "칠피", PetSpecies.ETC, "빨간구피", PetSize.SMALL, 3, PetGender.MALE, null);
        Pet pet8 = savePet(member1.getId(), "팔피", PetSpecies.ETC, "하얀구피", PetSize.SMALL, 3, PetGender.MALE, "가끔 탈출");
        Pet pet9 = savePet(member1.getId(), "구피", PetSpecies.ETC, "하얀구피", PetSize.SMALL, 1, PetGender.FEMALE, "빌드업");

        // member2(타코맘) - 고양이
        Pet pet10 = savePet(member2.getId(), "타코", PetSpecies.CAT, "코리안숏헤어", PetSize.SMALL, 4, PetGender.MALE, "길냥이에요, 많이 울어요, 귀여워요");

        // member3(지민냥) - 강아지, 말
        Pet pet11 = savePet(member3.getId(), "연두", PetSpecies.DOG, "포메라니안", PetSize.SMALL, 1, PetGender.FEMALE, "굉장히 귀여움 겨우 2.5kg");
        Pet pet12 = savePet(member3.getId(), "갈색", PetSpecies.ETC, "제주도 거대 말", PetSize.LARGE, 6, PetGender.MALE, null);

        // member5(박제로수) - 다양한 동물들
        Pet pet13 = savePet(member5.getId(), "길쭝", PetSpecies.DOG, "리트리버", PetSize.LARGE, 26, PetGender.MALE, "리더 재질");
        Pet pet14 = savePet(member5.getId(), "건젼", PetSpecies.ETC, "긴팔원숭이", PetSize.MEDIUM, 24, PetGender.FEMALE, "시끄러움");
        Pet pet15 = savePet(member5.getId(), "지밍", PetSpecies.DOG, "포메라니안", PetSize.SMALL, 23, PetGender.FEMALE, "귀여움");
        Pet pet16 = savePet(member5.getId(), "안경", PetSpecies.CAT, "스핑크스고양이", PetSize.MEDIUM, 26, PetGender.MALE, "똑똑함");

        // =============================================
        // 3. SitterProfile 2명 (member2, member4)
        //    - 둘 다 승인 완료 + RESERVABLE 상태
        // =============================================
        SitterProfile sitter1 = saveSitter(member2.getId(), "고양이 전문 시터, 자격증 보유", 7,
                PossiblePetType.CAT, PossiblePetSize.MEDIUM, 18000);
        sitter1.approve(1L);
        sitter1.changeStatus(SitterProfileStatus.RESERVABLE);
        member2.changeRoleToSitter();

        SitterProfile sitter2 = saveSitter(member4.getId(), "뭐든 다 잘 합니다.", 7,
                PossiblePetType.ALL, PossiblePetSize.ALL, 30000);
        sitter2.approve(1L);
        sitter2.changeStatus(SitterProfileStatus.RESERVABLE);
        member4.changeRoleToSitter();

        // =============================================
        // 4. SitterSchedule
        // =============================================
        saveSchedule(sitter1.getId(), DayOfWeek.MONDAY,    "09:00", "18:00");
        saveSchedule(sitter1.getId(), DayOfWeek.TUESDAY,   "09:00", "18:00");
        saveSchedule(sitter1.getId(), DayOfWeek.WEDNESDAY, "09:00", "18:00");
        saveSchedule(sitter1.getId(), DayOfWeek.THURSDAY,  "09:00", "18:00");
        saveSchedule(sitter1.getId(), DayOfWeek.FRIDAY,    "09:00", "18:00");

        saveSchedule(sitter2.getId(), DayOfWeek.MONDAY,    "10:00", "20:00");
        saveSchedule(sitter2.getId(), DayOfWeek.WEDNESDAY, "10:00", "20:00");

        // =============================================
        // 5. CareRequest - PENDING (member1 → sitter1)
        //    - 중복 전송 차단 테스트용
        // =============================================
        CareRequest careRequest1 = saveCareRequest(member1.getId(), sitter1.getId(), sitter1.getMemberId(),
                CareType.VISIT, "구피들 돌봐주실 수 있나요?", 40000);
        saveCareRequestPet(careRequest1.getId(), pet1);
        saveCareRequestPet(careRequest1.getId(), pet2);
        saveCareRequestTimeSlot(careRequest1.getId(), "2026-06-28", "15:00", "19:00", 1);

        // =============================================
        // 6. Reservation - PENDING (member1 ↔ sitter1/member2)
        //    - CareRequest 수락된 상태
        //    - ReservationPayment 미결제 (false/false)
        //    - 시간: 2026-07-20 10:00~14:00 (post1과 겹침 → 자동 cancel 대상)
        // =============================================
        CareRequest careRequest2 = saveCareRequest(member1.getId(), sitter1.getId(), sitter1.getMemberId(),
                CareType.VISIT, "다른 구피들도 부탁드려요", 45000);
        saveCareRequestPet(careRequest2.getId(), pet4);
        saveCareRequestPet(careRequest2.getId(), pet5);
        saveCareRequestTimeSlot(careRequest2.getId(), "2026-07-20", "10:00", "14:00", 1);
        careRequest2.accept();

        Reservation reservation1 = saveReservation(member1.getId(), member2.getId(), sitter1.getId(),
                CareType.VISIT, ReservationSource.CARE_REQUEST, careRequest2.getId(), 45000);
        saveReservationPet(reservation1.getId(), pet4);
        saveReservationPet(reservation1.getId(), pet5);
        saveReservationTimeSlot(reservation1.getId(), "2026-07-20", "10:00", "14:00", 1);
        saveReservationPayment(reservation1.getId(), 45000);

        // =============================================
        // 7. Post1 (by member5) - OPEN
        //    - 시간: 2026-07-20 10:00~14:00 (reservation1과 겹침)
        //    - Proposal 2개 (member4, member2) - 둘 다 PENDING
        //    - member2의 Proposal 수락 시 reservation2 생성 시나리오
        // =============================================
        Post post1 = savePost(member5.getId(), "우리 댕댕이들 봐주실 분",
                "길쭝이랑 지밍이 잘 봐주실 분 구해요",
                CareType.VISIT, 100000, PostStatus.OPEN);
        savePostPet(post1.getId(), pet13);
        savePostPet(post1.getId(), pet15);
        savePostTimeSlot(post1.getId(), "2026-07-20", "10:00", "14:00", 1);

        // member4(sitter2)의 Proposal
        saveProposal(post1.getId(), sitter2.getId(), member5.getId(), sitter2.getMemberId(),
                95000, "대형견도 자신 있습니다!");

        // member2(sitter1)의 Proposal → 수락 대상
        saveProposal(post1.getId(), sitter1.getId(), member5.getId(), sitter1.getMemberId(),
                100000, "고양이 전문이지만 강아지도 잘 봐드려요");

        // =============================================
        // 8. CareRequest - PENDING (member5 → sitter1/member2)
        //    - 다른 시간대, 전체 플로우 후에도 영향 없음 확인용
        // =============================================
        CareRequest careRequest3 = saveCareRequest(member5.getId(), sitter1.getId(), sitter1.getMemberId(),
                CareType.VISIT, "고양이 봐주실 수 있나요?", 50000);
        saveCareRequestPet(careRequest3.getId(), pet16);
        saveCareRequestTimeSlot(careRequest3.getId(), "2026-08-05", "10:00", "14:00", 1);

        // =============================================
        // 9. Post2 (by member1) - OPEN
        //    - 시간: 2026-07-20 10:00~14:00 (reservation1과 겹침)
        //    - member2(sitter1)가 작성한 Proposal 존재
        //    - 시나리오 진행 시 WITHDRAWN 처리 대상
        // =============================================
        Post post2 = savePost(member1.getId(), "구피 수조 관리해주세요",
                "여행 가는 동안 구피들 좀 봐주실 분",
                CareType.VISIT, 80000, PostStatus.OPEN);
        savePostPet(post2.getId(), pet6);
        savePostPet(post2.getId(), pet7);
        savePostTimeSlot(post2.getId(), "2026-07-20", "10:00", "14:00", 1);

        saveProposal(post2.getId(), sitter1.getId(), member1.getId(), sitter1.getMemberId(),
                75000, "물고기 케어도 가능합니다");

        // =============================================
        // 10. Reservation - 다른 시간대 (member5 ↔ sitter1/member2)
        //     - 위 플로우와 무관한 별개 예약 (시간 안 겹침)
        //     - 전체 시나리오 후에도 영향 받지 않음 확인용
        // =============================================
        CareRequest careRequest4 = saveCareRequest(member5.getId(), sitter1.getId(), sitter1.getMemberId(),
                CareType.VISIT, "원숭이 좀 봐주세요", 60000);
        saveCareRequestPet(careRequest4.getId(), pet14);
        saveCareRequestTimeSlot(careRequest4.getId(), "2026-09-10", "10:00", "14:00", 1);
        careRequest4.accept();

        Reservation reservation3 = saveReservation(member5.getId(), member2.getId(), sitter1.getId(),
                CareType.VISIT, ReservationSource.CARE_REQUEST, careRequest4.getId(), 60000);
        saveReservationPet(reservation3.getId(), pet14);
        saveReservationTimeSlot(reservation3.getId(), "2026-09-10", "10:00", "14:00", 1);
        saveReservationPayment(reservation3.getId(), 60000);
    }

    // ===== Helper Methods =====

    private Member saveMember(String email, String nickname, String phone,
                              MemberGender gender, Region region) {
        return memberRepository.save(Member.builder()
                .email(email)
                .password(passwordEncoder.encode("test1234!"))
                .nickname(nickname)
                .phone(phone)
                .gender(gender)
                .region(region)
                .build());
    }

    private Pet savePet(Long memberId, String name, PetSpecies species, String breed,
                        PetSize size, Integer age, PetGender gender, String note) {
        return petRepository.save(Pet.builder()
                .memberId(memberId)
                .name(name)
                .species(species)
                .breed(breed)
                .size(size)
                .age(age)
                .gender(gender)
                .note(note)
                .build());
    }

    private SitterProfile saveSitter(Long memberId, String introduction,
                                     int experienceYears, PossiblePetType petType,
                                     PossiblePetSize petSize, int pricePerHour) {
        return sitterProfileRepository.save(SitterProfile.builder()
                .memberId(memberId)
                .introduction(introduction)
                .experienceYears(experienceYears)
                .possiblePetType(petType)
                .possiblePetSize(petSize)
                .pricePerHour(pricePerHour)
                .build());
    }

    private void saveSchedule(Long sitterProfileId, DayOfWeek dayOfWeek,
                              String startTime, String endTime) {
        scheduleRepository.save(SitterSchedule.builder()
                .sitterProfileId(sitterProfileId)
                .dayOfWeek(dayOfWeek)
                .startTime(LocalTime.parse(startTime))
                .endTime(LocalTime.parse(endTime))
                .build());
    }

    private Post savePost(Long memberId, String title, String content,
                          CareType careType, Integer budgetAmount,
                          PostStatus status) {
        Post post = postRepository.save(Post.builder()
                .memberId(memberId)
                .title(title)
                .content(content)
                .careType(careType)
                .budgetAmount(budgetAmount)
                .build());

        if (status == PostStatus.CLOSED) {
            post.close();
        }

        return post;
    }

    private void savePostPet(Long postId, Pet pet) {
        postPetRepository.save(PostPet.createFrom(postId, pet));
    }

    private void savePostTimeSlot(Long postId, String careDate,
                                  String startTime, String endTime, int sequence) {
        postTimeSlotRepository.save(PostTimeSlot.create(
                postId,
                TimeSlotInfo.of(
                        LocalDate.parse(careDate),
                        LocalTime.parse(startTime),
                        LocalTime.parse(endTime),
                        sequence
                )
        ));
    }

    private Proposal saveProposal(Long postId, Long sitterProfileId, Long memberId, Long sitterMemberId,
                                  Integer proposedPrice, String message) {
        return proposalRepository.save(Proposal.builder()
                .postId(postId)
                .sitterProfileId(sitterProfileId)
                .sitterMemberId(sitterMemberId)
                .memberId(memberId)
                .proposedPrice(proposedPrice)
                .message(message)
                .build());
    }

    private CareRequest saveCareRequest(Long memberId, Long sitterProfileId, Long sitterMemberId,
                                        CareType careType, String message, int requestPrice) {
        return careRequestRepository.save(CareRequest.builder()
                .memberId(memberId)
                .sitterProfileId(sitterProfileId)
                .sitterMemberId(sitterMemberId)
                .careType(careType)
                .message(message)
                .requestPrice(requestPrice)
                .build());
    }

    private void saveCareRequestPet(Long careRequestId, Pet pet) {
        careRequestPetRepository.save(CareRequestPet.createFrom(careRequestId, pet));
    }

    private void saveCareRequestTimeSlot(Long careRequestId, String careDate,
                                         String startTime, String endTime, int sequence) {
        careRequestTimeSlotRepository.save(CareRequestTimeSlot.create(
                careRequestId,
                TimeSlotInfo.of(
                        LocalDate.parse(careDate),
                        LocalTime.parse(startTime),
                        LocalTime.parse(endTime),
                        sequence
                )
        ));
    }

    private Reservation saveReservation(Long guardianId, Long sitterMemberId, Long sitterProfileId,
                                        CareType careType, ReservationSource source, Long sourceId, int price) {
        return reservationRepository.save(Reservation.builder()
                .guardianId(guardianId)
                .sitterMemberId(sitterMemberId)
                .sitterProfileId(sitterProfileId)
                .careType(careType)
                .source(source)
                .sourceId(sourceId)
                .build());
    }

    private void saveReservationPet(Long reservationId, Pet pet) {
        PetSnapshot snapshot = PetSnapshot.from(pet);
        reservationPetRepository.save(
                ReservationPet.createFrom(reservationId, pet.getId(), snapshot)
        );
    }

    private void saveReservationTimeSlot(Long reservationId, String careDate,
                                         String startTime, String endTime, int sequence) {
        reservationTimeSlotRepository.save(ReservationTimeSlot.create(
                reservationId,
                TimeSlotInfo.of(
                        LocalDate.parse(careDate),
                        LocalTime.parse(startTime),
                        LocalTime.parse(endTime),
                        sequence
                )
        ));
    }

    private ReservationPayment saveReservationPayment(Long reservationId, int price) {
        return reservationPaymentRepository.save(
                ReservationPayment.create(reservationId, price, (int) (price * 0.2))
        );
    }
}