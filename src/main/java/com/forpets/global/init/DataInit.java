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
import com.forpets.domain.sitter.entity.PossiblePetSize;
import com.forpets.domain.sitter.entity.PossiblePetType;
import com.forpets.domain.sitter.entity.SitterProfile;
import com.forpets.domain.sitter.entity.SitterSchedule;
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
public class DataInit implements CommandLineRunner {

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
        if (memberRepository.count() > 0) return;

        // =============================================
        // 1. Member 5명
        // =============================================
        Member member1 = saveMember("giljung@test.com", "째길중", "010-1111-1111", MemberGender.MALE, Region.SEOCHO);
        Member member2 = saveMember("jiwon@test.com", "타코맘", "010-2222-2222", MemberGender.FEMALE, Region.DONGJAK);
        Member member3 = saveMember("jimin@test.com", "지민냥", "010-3333-3333", MemberGender.FEMALE, Region.GANGNAM);
        Member member4 = saveMember("gyeongahn@test.com", "앉은경안", "010-4444-4444", MemberGender.MALE, Region.GWANGJIN);
        Member member5 = saveMember("yeongsoo@test.com", "박제로수", "010-5555-5555", MemberGender.MALE, Region.YONGSAN);

        member2.changeRoleToSitter();
        member3.changeRoleToSitter();
        member4.changeRoleToSitter();

        // =============================================
        // 2. Pet 등록
        // =============================================

        // 째길중 — 9마리
        Pet pet1 = savePet(member1.getId(), "일피", PetSpecies.ETC, "하얀구피", PetSize.SMALL, 3, PetGender.MALE, "낯가림 있음");
        Pet pet2 = savePet(member1.getId(), "이피", PetSpecies.ETC, "빨간구피", PetSize.SMALL, 2, PetGender.MALE, "조금 뚱뚱함");
        Pet pet3 = savePet(member1.getId(), "삼피", PetSpecies.ETC, "하얀구피", PetSize.SMALL, 3, PetGender.FEMALE, "쌍피로 이름 짓고 싶었음");
        Pet pet4 = savePet(member1.getId(), "사피", PetSpecies.ETC, "하얀구피", PetSize.SMALL, 3, PetGender.FEMALE, null);
        Pet pet5 = savePet(member1.getId(), "오피", PetSpecies.ETC, "하얀구피", PetSize.SMALL, 3, PetGender.FEMALE, null);
        Pet pet6 = savePet(member1.getId(), "육피", PetSpecies.ETC, "하얀구피", PetSize.SMALL, 2, PetGender.MALE, "잘 헤엄침");
        Pet pet7 = savePet(member1.getId(), "칠피", PetSpecies.ETC, "빨간구피", PetSize.SMALL, 3, PetGender.MALE, null);
        Pet pet8 = savePet(member1.getId(), "팔피", PetSpecies.ETC, "하얀구피", PetSize.SMALL, 3, PetGender.MALE, "가끔 탈출");
        Pet pet9 = savePet(member1.getId(), "구피", PetSpecies.ETC, "하얀구피", PetSize.SMALL, 1, PetGender.FEMALE, "빌드업");

        // 타코맘 — 1마리
        Pet pet10 = savePet(member2.getId(), "타코", PetSpecies.CAT, "코리안숏헤어", PetSize.SMALL, 4, PetGender.MALE, "길냥이에요, 많이 울어요, 귀여워요");

        // 지민냥 — 2마리
        Pet pet11 = savePet(member3.getId(), "연두", PetSpecies.DOG, "포메라니안", PetSize.SMALL, 1, PetGender.FEMALE, "굉장히 귀여움 겨우 2.5kg");
        Pet pet12 = savePet(member3.getId(), "갈색", PetSpecies.ETC, "제주도 거대 말", PetSize.LARGE, 6, PetGender.MALE, null);

        // 박제로수 — 4마리
        Pet pet13 = savePet(member5.getId(), "길쭝", PetSpecies.DOG, "리트리버", PetSize.LARGE, 26, PetGender.MALE, "리더 재질");
        Pet pet14 = savePet(member5.getId(), "건젼", PetSpecies.ETC, "긴팔원숭이", PetSize.MEDIUM, 24, PetGender.FEMALE, "시끄러움");
        Pet pet15 = savePet(member5.getId(), "지밍", PetSpecies.DOG, "포메라니안", PetSize.SMALL, 23, PetGender.FEMALE, "귀여움");
        Pet pet16 = savePet(member5.getId(), "안경", PetSpecies.CAT, "스핑크스고양이", PetSize.MEDIUM, 26, PetGender.MALE, "똑똑함");

        // =============================================
        // 3. SitterProfile 3명
        // =============================================
        SitterProfile sitter1 = saveSitter(member2.getId(), "고양이 전문 시터, 자격증 보유", 7,
                PossiblePetType.CAT, PossiblePetSize.MEDIUM, 18000);
        SitterProfile sitter2 = saveSitter(member3.getId(), "소형견 전문 시터입니다", 3,
                PossiblePetType.DOG, PossiblePetSize.SMALL, 15000);
        SitterProfile sitter3 = saveSitter(member4.getId(), "모든 반려동물 케어 가능합니다", 5,
                PossiblePetType.ALL, PossiblePetSize.ALL, 20000);

        // =============================================
        // 4. SitterAvailableTime
        // =============================================

        // sitter1 (타코맘) — 월~금
        saveSchedule(sitter1.getId(), DayOfWeek.MONDAY, "09:00", "18:00");
        saveSchedule(sitter1.getId(), DayOfWeek.TUESDAY, "09:00", "18:00");
        saveSchedule(sitter1.getId(), DayOfWeek.WEDNESDAY, "09:00", "18:00");
        saveSchedule(sitter1.getId(), DayOfWeek.THURSDAY, "09:00", "18:00");
        saveSchedule(sitter1.getId(), DayOfWeek.FRIDAY, "09:00", "18:00");

        // sitter2 (지민냥) — 월수금 + 토
        saveSchedule(sitter2.getId(), DayOfWeek.MONDAY, "10:00", "20:00");
        saveSchedule(sitter2.getId(), DayOfWeek.WEDNESDAY, "10:00", "20:00");
        saveSchedule(sitter2.getId(), DayOfWeek.FRIDAY, "10:00", "20:00");
        saveSchedule(sitter2.getId(), DayOfWeek.SATURDAY, "09:00", "15:00");

        // sitter3 (앉은경안) — 화목토일
        saveSchedule(sitter3.getId(), DayOfWeek.TUESDAY, "08:00", "17:00");
        saveSchedule(sitter3.getId(), DayOfWeek.THURSDAY, "08:00", "17:00");
        saveSchedule(sitter3.getId(), DayOfWeek.SATURDAY, "10:00", "16:00");
        saveSchedule(sitter3.getId(), DayOfWeek.SUNDAY, "10:00", "16:00");

        // =============================================
        // 5. Post 10개 (OPEN 7개, CLOSED 2개)
        // =============================================

        // --- OPEN 공고 7개 ---
        Post post1 = savePost(member1.getId(), "구피 밥 좀 주실분", "출장 중 방문 돌봄 부탁드려요",
                CareType.VISIT, 50000, PostStatus.OPEN);
        savePostPet(post1.getId(), pet1);
        savePostPet(post1.getId(), pet2);
        savePostPet(post1.getId(), pet3);
        savePostPet(post1.getId(), pet4);
        savePostPet(post1.getId(), pet5);
        savePostPet(post1.getId(), pet6);
        savePostPet(post1.getId(), pet7);
        savePostPet(post1.getId(), pet8);
        savePostPet(post1.getId(), pet9);
        savePostTimeSlot(post1.getId(), "2026-06-02", "09:00", "12:00", 1);
        savePostTimeSlot(post1.getId(), "2026-06-02", "14:00", "18:00", 2);
        savePostTimeSlot(post1.getId(), "2026-06-03", "09:00", "13:00", 3);

        Post post2 = savePost(member2.getId(), "고양이 놀아주세요", "너무 바빠서 고양이가 무기력해서 잘 놀아주실분",
                CareType.VISIT, 120000, PostStatus.OPEN);
        savePostPet(post2.getId(), pet10);
        savePostTimeSlot(post2.getId(), "2026-06-10", "10:00", "20:00", 1);

        Post post3 = savePost(member1.getId(), "구피 맡아주세요", "구피 한 마리가 임신중이라 잠시 떨궈둘 생각입니다...",
                CareType.BOARDING, 30000, PostStatus.OPEN);
        savePostPet(post3.getId(), pet3);
        savePostTimeSlot(post3.getId(), "2026-06-05", "00:00", "23:59", 1);
        savePostTimeSlot(post3.getId(), "2026-06-06", "00:00", "23:59", 2);
        savePostTimeSlot(post3.getId(), "2026-06-07", "00:00", "23:59", 3);

        Post post4 = savePost(member5.getId(), "4조 팀원들 잠시만 봐주세요", "잘 좀 부탁드립니다...",
                CareType.VISIT, 40000, PostStatus.OPEN);
        savePostPet(post4.getId(), pet13);
        savePostPet(post4.getId(), pet14);
        savePostPet(post4.getId(), pet15);
        savePostPet(post4.getId(), pet16);
        savePostTimeSlot(post4.getId(), "2026-06-07", "13:00", "17:00", 1);
        savePostTimeSlot(post4.getId(), "2026-06-08", "13:00", "17:00", 2);

        Post post5 = savePost(member5.getId(), "중요한 회의가 있어서 고양이 봐주실 분", "많이 시끄러워서 잠시 보내요... 집에서 돌봐주실 분 구합니다",
                CareType.VISIT, 60000, PostStatus.OPEN);
        savePostPet(post5.getId(), pet14);
        savePostTimeSlot(post5.getId(), "2026-06-15", "09:00", "18:00", 1);
        savePostTimeSlot(post5.getId(), "2026-06-16", "09:00", "18:00", 2);

        Post post7 = savePost(member3.getId(), "말 산책 시켜주세요", "타면 안 돼요... 힘들어해요",
                CareType.VISIT, 80000, PostStatus.OPEN);
        savePostPet(post7.getId(), pet12);
        savePostTimeSlot(post7.getId(), "2026-06-21", "09:00", "20:00", 1);
        savePostTimeSlot(post7.getId(), "2026-06-22", "09:00", "20:00", 2);

        Post post10 = savePost(member1.getId(), "일피만 좀 봐주세요", "일피가 아파서 병원 다녀오는 동안 다른 애들 좀...",
                CareType.VISIT, 25000, PostStatus.OPEN);
        savePostPet(post10.getId(), pet2);
        savePostPet(post10.getId(), pet4);
        savePostPet(post10.getId(), pet5);
        savePostTimeSlot(post10.getId(), "2026-06-12", "14:00", "17:00", 1);

        // --- CLOSED 공고 2개 ---
        Post post8 = savePost(member1.getId(), "[마감] 구피 밥 주실 분", "제곧내",
                CareType.VISIT, 35000, PostStatus.CLOSED);
        savePostPet(post8.getId(), pet1);
        savePostTimeSlot(post8.getId(), "2026-06-01", "10:00", "16:00", 1);

        Post post9 = savePost(member5.getId(), "[마감] 길쭝이 산책", "좋은 시터분 찾았어요",
                CareType.BOARDING, 55000, PostStatus.CLOSED);
        savePostPet(post9.getId(), pet13);
        savePostTimeSlot(post9.getId(), "2026-06-01", "09:00", "18:00", 1);

        // =============================================
        // 6. Proposal (Post 기반)
        // =============================================
        saveProposal(post1.getId(), sitter1.getId(), member2.getId(), 45000, "구피 백마리도 키워봤어요");
        saveProposal(post1.getId(), sitter2.getId(), member3.getId(), 50000, "제주도 사람입니다. 더 볼 거 있나요?");
        saveProposal(post1.getId(), sitter3.getId(), member4.getId(), 48000, "근처에요");

        saveProposal(post2.getId(), sitter1.getId(), member2.getId(), 110000, "강아지 고양이 둘 다 경험 있습니다");
        saveProposal(post2.getId(), sitter3.getId(), member4.getId(), 115000, "고양이 진짜 진짜 잘 놀아줘요!!");

        saveProposal(post3.getId(), sitter2.getId(), member3.getId(), 28000, "밥 맛있는걸로다가 주겠습니다~");

        Proposal proposal41 = saveProposal(post4.getId(), sitter1.getId(), member2.getId(), 60000, "다중 돌봄 경험 있습니다.");
        saveProposal(post4.getId(), sitter2.getId(), member3.getId(), 70000, null);

        saveProposal(post5.getId(), sitter3.getId(), member4.getId(), 55000, "고양이 전문이라 겁 많은 아이도 잘 다룹니다");

        Proposal proposal71 = saveProposal(post7.getId(), sitter1.getId(), member2.getId(), 1000, "와 진짜 무료로도 해드리고 싶어요");
        saveProposal(post7.getId(), sitter3.getId(), member4.getId(), 78000, "경마장 알바 경험 있습니다.");

        saveProposal(post10.getId(), sitter2.getId(), member3.getId(), 23000, "소형 반려동물 전문입니다");
        Proposal proposal103 = saveProposal(post10.getId(), sitter3.getId(), member4.getId(), 24000, "오후 시간 괜찮아요");

        // =============================================
        // 7. CareRequest (시터 프로필 기반 직접 요청)
        // =============================================

        // 째길중 → sitter3 에게 구피 돌봄 요청
        CareRequest request1 = saveCareRequest(member1.getId(), sitter3.getId(), CareType.VISIT,
                "구피 여러 마리 한꺼번에 봐주실 수 있나요?", 55000);
        saveCareRequestPet(request1.getId(), pet1);
        saveCareRequestPet(request1.getId(), pet2);
        saveCareRequestPet(request1.getId(), pet6);
        saveCareRequestTimeSlot(request1.getId(), "2026-06-18", "10:00", "14:00", 1);

        // 박제로수 → sitter1 에게 고양이 돌봄 요청
        CareRequest request2 = saveCareRequest(member5.getId(), sitter1.getId(), CareType.BOARDING,
                "고양이 전문가시라고 해서 연락드려요", 90000);
        saveCareRequestPet(request2.getId(), pet16);
        saveCareRequestTimeSlot(request2.getId(), "2026-06-20", "00:00", "23:59", 1);
        saveCareRequestTimeSlot(request2.getId(), "2026-06-21", "00:00", "23:59", 2);

        // 타코맘 → sitter3 에게 타코 돌봄 요청
        CareRequest request3 = saveCareRequest(member2.getId(), sitter3.getId(), CareType.VISIT,
                "하루종일 같이 있어주실 수 있나요?", 180000);
        saveCareRequestPet(request3.getId(), pet10);
        saveCareRequestTimeSlot(request3.getId(), "2026-06-25", "08:00", "20:00", 1);

        // 지민냥 → sitter3 에게 연두 돌봄 요청
        CareRequest request4 = saveCareRequest(member3.getId(), sitter3.getId(), CareType.VISIT,
                "소형견인데 괜찮으실까요?", 35000);
        saveCareRequestPet(request4.getId(), pet11);
        saveCareRequestTimeSlot(request4.getId(), "2026-06-28", "15:00", "19:00", 1);

        // =============================================
        // 8. Reservation (POST 기반)
        // =============================================

        // post4 → proposal41 수락 (박제로수 팀원들 돌봄)
        Reservation reservation1 = saveReservation(member5.getId(), member2.getId(), sitter1.getId(),
                CareType.VISIT, ReservationSource.PROPOSAL, post4.getId(), 60000);
        saveReservationPet(reservation1.getId(), pet13);
        saveReservationPet(reservation1.getId(), pet14);
        saveReservationPet(reservation1.getId(), pet15);
        saveReservationPet(reservation1.getId(), pet16);
        saveReservationTimeSlot(reservation1.getId(), "2026-06-07", "13:00", "17:00", 1);
        saveReservationTimeSlot(reservation1.getId(), "2026-06-08", "13:00", "17:00", 2);
        reservation1.confirm();
        saveReservationPayment(reservation1.getId(), 60000);

        // post7 → proposal71 수락 (말 산책)
        Reservation reservation2 = saveReservation(member3.getId(), member2.getId(), sitter1.getId(),
                CareType.VISIT, ReservationSource.PROPOSAL, post7.getId(), 1000);
        saveReservationPet(reservation2.getId(), pet12);
        saveReservationTimeSlot(reservation2.getId(), "2026-06-21", "09:00", "20:00", 1);
        saveReservationTimeSlot(reservation2.getId(), "2026-06-22", "09:00", "20:00", 2);
        reservation2.confirm();
        reservation2.complete();
        saveReservationPayment(reservation2.getId(), 1000);

        // post10 → proposal103 수락 (구피 일부 돌봄)
        Reservation reservation3 = saveReservation(member1.getId(), member4.getId(), sitter3.getId(),
                CareType.VISIT, ReservationSource.PROPOSAL, post10.getId(), 24000);
        saveReservationPet(reservation3.getId(), pet2);
        saveReservationPet(reservation3.getId(), pet4);
        saveReservationPet(reservation3.getId(), pet5);
        saveReservationTimeSlot(reservation3.getId(), "2026-06-12", "14:00", "17:00", 1);
        reservation3.confirm();
        saveReservationPayment(reservation3.getId(), 24000);

        // =============================================
        // 9. Reservation (CARE_REQUEST 기반)
        // =============================================

        // request2 수락 (박제로수 → sitter1, 고양이 위탁)
        Reservation reservation4 = saveReservation(member5.getId(), member2.getId(), sitter1.getId(),
                CareType.BOARDING, ReservationSource.CARE_REQUEST, request2.getId(), 90000);
        saveReservationPet(reservation4.getId(), pet16);
        saveReservationTimeSlot(reservation4.getId(), "2026-06-20", "00:00", "23:59", 1);
        saveReservationTimeSlot(reservation4.getId(), "2026-06-21", "00:00", "23:59", 2);
        reservation4.confirm();
        saveReservationPayment(reservation4.getId(), 90000);

        // request4 수락 (지민냥 → sitter3, 연두 돌봄)
        Reservation reservation5 = saveReservation(member3.getId(), member4.getId(), sitter3.getId(),
                CareType.VISIT, ReservationSource.CARE_REQUEST, request4.getId(), 35000);
        saveReservationPet(reservation5.getId(), pet11);
        saveReservationTimeSlot(reservation5.getId(), "2026-06-28", "15:00", "19:00", 1);
        saveReservationPayment(reservation5.getId(), 35000);
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

    private Proposal saveProposal(Long postId, Long sitterProfileId, Long memberId,
                                  Integer proposedPrice, String message) {
        return proposalRepository.save(Proposal.builder()
                .postId(postId)
                .sitterProfileId(sitterProfileId)
                .memberId(memberId)
                .proposedPrice(proposedPrice)
                .message(message)
                .build());
    }

    private CareRequest saveCareRequest(Long memberId, Long sitterProfileId,
                                        CareType careType, String message, int requestPrice) {
        return careRequestRepository.save(CareRequest.builder()
                .memberId(memberId)
                .sitterProfileId(sitterProfileId)
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

    private void saveReservationPayment(Long reservationId, int price) {
        reservationPaymentRepository.save(
                ReservationPayment.create(reservationId, price, (int) (price * 0.2))
        );
    }
}