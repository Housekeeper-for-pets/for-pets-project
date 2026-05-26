package com.forpets.global.init;


import com.forpets.domain.carerequest.entity.CareRequest;
import com.forpets.domain.carerequest.entity.CareRequestPet;
import com.forpets.domain.carerequest.entity.CareRequestTimeSlot;
import com.forpets.domain.carerequest.repository.CareRequestPetRepository;
import com.forpets.domain.carerequest.repository.CareRequestRepository;
import com.forpets.domain.carerequest.repository.CareRequestTimeSlotRepository;
import com.forpets.domain.member.entity.Member;
import com.forpets.domain.member.entity.MemberGender;
import com.forpets.domain.member.entity.MemberRole;
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
import java.util.*;

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

    // 루프 기반 생성에서 사용할 시드 (동일 데이터 재현 보장)
    private final Random random = new Random(42);

    @Override
    @Transactional
    public void run(String... args) {

        // =============================================
        // 1. Member 5명
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
        Pet pet1 = savePet(member1.getId(), "일피", PetSpecies.ETC, "하얀구피", PetSize.SMALL, 3, PetGender.MALE, "낯가림 있음");
        Pet pet2 = savePet(member1.getId(), "이피", PetSpecies.ETC, "빨간구피", PetSize.SMALL, 2, PetGender.MALE, "조금 뚱뚱함");
        Pet pet3 = savePet(member1.getId(), "삼피", PetSpecies.ETC, "하얀구피", PetSize.SMALL, 3, PetGender.FEMALE, "쌍피로 이름 짓고 싶었음");
        Pet pet4 = savePet(member1.getId(), "사피", PetSpecies.ETC, "하얀구피", PetSize.SMALL, 3, PetGender.FEMALE, null);
        Pet pet5 = savePet(member1.getId(), "오피", PetSpecies.ETC, "하얀구피", PetSize.SMALL, 3, PetGender.FEMALE, null);
        Pet pet6 = savePet(member1.getId(), "육피", PetSpecies.ETC, "하얀구피", PetSize.SMALL, 2, PetGender.MALE, "잘 헤엄침");
        Pet pet7 = savePet(member1.getId(), "칠피", PetSpecies.ETC, "빨간구피", PetSize.SMALL, 3, PetGender.MALE, null);
        Pet pet8 = savePet(member1.getId(), "팔피", PetSpecies.ETC, "하얀구피", PetSize.SMALL, 3, PetGender.MALE, "가끔 탈출");
        Pet pet9 = savePet(member1.getId(), "구피", PetSpecies.ETC, "하얀구피", PetSize.SMALL, 1, PetGender.FEMALE, "빌드업");

        Pet pet10 = savePet(member2.getId(), "타코", PetSpecies.CAT, "코리안숏헤어", PetSize.SMALL, 4, PetGender.MALE, "길냥이에요, 많이 울어요, 귀여워요");

        Pet pet11 = savePet(member3.getId(), "연두", PetSpecies.DOG, "포메라니안", PetSize.SMALL, 1, PetGender.FEMALE, "굉장히 귀여움 겨우 2.5kg");
        Pet pet12 = savePet(member3.getId(), "갈색", PetSpecies.ETC, "제주도 거대 말", PetSize.LARGE, 6, PetGender.MALE, null);

        Pet pet13 = savePet(member5.getId(), "길쭝", PetSpecies.DOG, "리트리버", PetSize.LARGE, 26, PetGender.MALE, "리더 재질");
        Pet pet14 = savePet(member5.getId(), "건젼", PetSpecies.ETC, "긴팔원숭이", PetSize.MEDIUM, 24, PetGender.FEMALE, "시끄러움");
        Pet pet15 = savePet(member5.getId(), "지밍", PetSpecies.DOG, "포메라니안", PetSize.SMALL, 23, PetGender.FEMALE, "귀여움");
        Pet pet16 = savePet(member5.getId(), "안경", PetSpecies.CAT, "스핑크스고양이", PetSize.MEDIUM, 26, PetGender.MALE, "똑똑함");

        // =============================================
        // 3. SitterProfile 2명
        // =============================================
        SitterProfile sitter1 = saveSitter(member2.getId(), "고양이 전문 시터, 자격증 보유", 7,
                PossiblePetType.CAT, PossiblePetSize.MEDIUM, 18000);

        SitterProfile sitter2 = saveSitter(member4.getId(), "뭐든 다 잘 합니다.", 7,
                PossiblePetType.ALL, PossiblePetSize.ALL, 30000);

        sitter1.changeStatus(SitterProfileStatus.RESERVABLE);
        sitter1.approve(1L);
        member2.changeRoleToSitter();

        sitter2.changeStatus(SitterProfileStatus.RESERVABLE);
        sitter2.approve(1L);
        member4.changeRoleToSitter();


        // =============================================
        // 4. SitterSchedule
        // =============================================
        saveSchedule(sitter1.getId(), DayOfWeek.MONDAY, "09:00", "18:00");
        saveSchedule(sitter1.getId(), DayOfWeek.TUESDAY, "09:00", "18:00");
        saveSchedule(sitter1.getId(), DayOfWeek.WEDNESDAY, "09:00", "18:00");
        saveSchedule(sitter1.getId(), DayOfWeek.THURSDAY, "09:00", "18:00");
        saveSchedule(sitter1.getId(), DayOfWeek.FRIDAY, "09:00", "18:00");

        // =============================================
        // 5. Post 2개
        //    - post1: OPEN 상태로 유지, pending proposal 1개 붙어있음
        //    - post2: proposal 수락으로 CONFIRMED reservation 생성 → CLOSED
        // =============================================

        // post1: OPEN 유지, sitter1(지원이)의 pending proposal 1개 (postman 테스트용)
        Post post1 = savePost(member5.getId(), "작은 강아지 봐주실 분 구합니다.",
                "애기가 무서워하지 않도록 여자 시터분이면 좋겠어요!",
                CareType.VISIT, 70000, PostStatus.OPEN);
        savePostPet(post1.getId(), pet15);
        savePostTimeSlot(post1.getId(), "2026-07-15", "14:00", "18:00", 1);

        saveProposal(post1.getId(), sitter1.getId(), member1.getId(), sitter1.getMemberId(),
                75000, "뭐든 열심히 할 수 있습니다!");

        // post2: proposal 2개 → 그 중 sitter1(지원이) 수락으로 confirmed reservation 생성
        Post post2 = savePost(member2.getId(), "고양이 놀아주세요",
                "너무 바빠서 고양이가 무기력해서 잘 놀아주실분",
                CareType.VISIT, 120000, PostStatus.OPEN);
        savePostPet(post2.getId(), pet10);
        savePostTimeSlot(post2.getId(), "2026-06-10", "10:00", "20:00", 1);

        Proposal post2Proposal1 = saveProposal(post2.getId(), sitter1.getId(), member2.getId(), sitter1.getMemberId(),
                110000, "고양이 전문이에요. 잘 놀아드릴게요");
//        Proposal post2Proposal2 = saveProposal(post2.getId(), sitter3.getId(), member2.getId(), sitter3.getMemberId(),
//                115000, "고양이 진짜 진짜 잘 놀아줘요!!");

        // =============================================
        // 6. CareRequest 1개 (PENDING)
        //    - member1(길중) → sitter1(지원)
        //    - postman에서 같은 펫 조합으로 중복 전송 시 차단 테스트용
        // =============================================
        CareRequest careRequest1 = saveCareRequest(member1.getId(), sitter1.getId(), sitter1.getMemberId(),
                CareType.VISIT, "구피들 돌봐주실 수 있나요?", 40000);
        saveCareRequestPet(careRequest1.getId(), pet1);
        saveCareRequestPet(careRequest1.getId(), pet2);
        saveCareRequestTimeSlot(careRequest1.getId(), "2026-06-28", "15:00", "19:00", 1);

        // =============================================
        // 7. Reservation - PENDING 1개
        //    - member1(길중) → sitter1(지원), CareRequest 수락 상태
        //    - 취소 시 careRequest가 다시 PENDING으로 돌아오는지 확인용
        // =============================================

        CareRequest careRequest2 = saveCareRequest(member1.getId(), sitter1.getId(), sitter1.getMemberId(),
                CareType.VISIT, "다른 구피들도 부탁드려요", 45000);
        saveCareRequestPet(careRequest2.getId(), pet4);
        saveCareRequestPet(careRequest2.getId(), pet5);
        saveCareRequestTimeSlot(careRequest2.getId(), "2026-07-20", "10:00", "14:00", 1);
        careRequest2.accept();

        Reservation reservationPending = saveReservation(member1.getId(), member2.getId(), sitter1.getId(),
                CareType.VISIT, ReservationSource.CARE_REQUEST, careRequest2.getId(), 45000);
        saveReservationPet(reservationPending.getId(), pet4);
        saveReservationPet(reservationPending.getId(), pet5);
        saveReservationTimeSlot(reservationPending.getId(), "2026-07-20", "10:00", "14:00", 1);
        saveReservationPayment(reservationPending.getId(), 45000);

        // reservation은 PENDING 유지, Payment는 만들지 않음

        // =============================================
        // 8. Reservation - CONFIRMED 1개
        //    - source: PROPOSAL (post2 + post2Proposal1)
        //    - sitter1(지원)이 수락
        //    - 중복 수락 불가 / 자동 거절 등 연쇄처리 검증용
        // =============================================
        Reservation reservationConfirmed = saveReservation(member2.getId(), member2.getId(), sitter1.getId(),
                CareType.VISIT, ReservationSource.PROPOSAL, post2.getId(), 110000);
        saveReservationPet(reservationConfirmed.getId(), pet10);
        saveReservationTimeSlot(reservationConfirmed.getId(), "2026-06-10", "10:00", "20:00", 1);

        ReservationPayment confirmedRp = saveReservationPayment(reservationConfirmed.getId(), 110000);
        confirmedRp.guardianConfirm();
        confirmedRp.sitterConfirm();

        reservationConfirmed.confirm();

        // confirmAfterPayment 연쇄처리 흉내
        post2.close();
        post2Proposal1.accept();
//        post2Proposal2.reject();
    }

    // ===== Helper Methods (기존과 동일) =====

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