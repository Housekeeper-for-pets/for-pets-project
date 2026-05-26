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

    // 루프 기반 생성에서 사용할 시드 (동일 데이터 재현 보장)
    private final Random random = new Random(42);

    @Override
    @Transactional
    public void run(String... args) {
        if (memberRepository.count() > 0) return;

        Member adminMember = saveMember("dragonRock@test.com", "소란석", "010-0000-0000", MemberGender.MALE, Region.YONGSAN);
        // =============================================
        // 1. Member 5명 (기존)
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
        // 2. Pet 등록 (기존)
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
        Pet pet16 = savePet(member5.getId(), "안경", PetSpecies.CAT, "스핑크스고양이", PetSize.MEDIUM, 26, PetGender.MALE, "똑똒함");

        // =============================================
        // 3. SitterProfile 3명 (기존)
        // =============================================
        SitterProfile sitter1 = saveSitter(member2.getId(), "고양이 전문 시터, 자격증 보유", 7,
                PossiblePetType.CAT, PossiblePetSize.MEDIUM, 18000);
        SitterProfile sitter2 = saveSitter(member3.getId(), "소형견 전문 시터입니다", 3,
                PossiblePetType.DOG, PossiblePetSize.SMALL, 15000);
        SitterProfile sitter3 = saveSitter(member4.getId(), "모든 반려동물 케어 가능합니다", 5,
                PossiblePetType.ALL, PossiblePetSize.ALL, 20000);

        // =============================================
        // 4. SitterSchedule (기존)
        // =============================================
        saveSchedule(sitter1.getId(), DayOfWeek.MONDAY, "09:00", "18:00");
        saveSchedule(sitter1.getId(), DayOfWeek.TUESDAY, "09:00", "18:00");
        saveSchedule(sitter1.getId(), DayOfWeek.WEDNESDAY, "09:00", "18:00");
        saveSchedule(sitter1.getId(), DayOfWeek.THURSDAY, "09:00", "18:00");
        saveSchedule(sitter1.getId(), DayOfWeek.FRIDAY, "09:00", "18:00");

        saveSchedule(sitter2.getId(), DayOfWeek.MONDAY, "10:00", "20:00");
        saveSchedule(sitter2.getId(), DayOfWeek.WEDNESDAY, "10:00", "20:00");
        saveSchedule(sitter2.getId(), DayOfWeek.FRIDAY, "10:00", "20:00");
        saveSchedule(sitter2.getId(), DayOfWeek.SATURDAY, "09:00", "15:00");

        saveSchedule(sitter3.getId(), DayOfWeek.TUESDAY, "08:00", "17:00");
        saveSchedule(sitter3.getId(), DayOfWeek.THURSDAY, "08:00", "17:00");
        saveSchedule(sitter3.getId(), DayOfWeek.SATURDAY, "10:00", "16:00");
        saveSchedule(sitter3.getId(), DayOfWeek.SUNDAY, "10:00", "16:00");

        // =============================================
        // 5. Post (기존 9개)
        // =============================================

        // --- OPEN 7개 ---
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

        Post post5 = savePost(member5.getId(), "중요한 회의가 있어서 고양이 봐주실 분", "많이 시끄러워서 잠시 보내요...",
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

        // --- CLOSED 2개 ---
        Post post8 = savePost(member1.getId(), "[마감] 구피 밥 주실 분", "제곧내",
                CareType.VISIT, 35000, PostStatus.CLOSED);
        savePostPet(post8.getId(), pet1);
        savePostTimeSlot(post8.getId(), "2026-06-01", "10:00", "16:00", 1);

        Post post9 = savePost(member5.getId(), "[마감] 길쭝이 산책", "좋은 시터분 찾았어요",
                CareType.BOARDING, 55000, PostStatus.CLOSED);
        savePostPet(post9.getId(), pet13);
        savePostTimeSlot(post9.getId(), "2026-06-01", "09:00", "18:00", 1);

        // =============================================
        // 6. Proposal (기존)
        // =============================================
        saveProposal(post1.getId(), sitter1.getId(), member2.getId(), sitter1.getMemberId(),45000, "구피 백마리도 키워봤어요");
        saveProposal(post1.getId(), sitter2.getId(), member3.getId(), sitter2.getMemberId(),50000, "제주도 사람입니다. 더 볼 거 있나요?");
        saveProposal(post1.getId(), sitter3.getId(), member4.getId(), sitter3.getMemberId(),48000, "근처에요");

        saveProposal(post2.getId(), sitter1.getId(), member2.getId(), sitter1.getMemberId(),110000, "강아지 고양이 둘 다 경험 있습니다");
        saveProposal(post2.getId(), sitter3.getId(), member4.getId(), sitter3.getMemberId(),115000, "고양이 진짜 진짜 잘 놀아줘요!!");

        saveProposal(post3.getId(), sitter2.getId(), member3.getId(), sitter2.getMemberId(),28000, "밥 맛있는걸로다가 주겠습니다~");

        Proposal proposal41 = saveProposal(post4.getId(), sitter1.getId(), member2.getId(), sitter1.getMemberId(),60000, "다중 돌봄 경험 있습니다.");
        saveProposal(post4.getId(), sitter2.getId(), member3.getId(), sitter2.getMemberId(),70000, null);

        saveProposal(post5.getId(), sitter3.getId(), member4.getId(), sitter3.getMemberId(),55000, "고양이 전문이라 겁 많은 아이도 잘 다룹니다");

        Proposal proposal71 = saveProposal(post7.getId(), sitter1.getId(), member2.getId(), sitter1.getMemberId(),1000, "와 진짜 무료로도 해드리고 싶어요");
        saveProposal(post7.getId(), sitter3.getId(), member4.getId(), sitter3.getMemberId(),78000, "경마장 알바 경험 있습니다.");

        saveProposal(post10.getId(), sitter2.getId(), member3.getId(), sitter2.getMemberId(),23000, "소형 반려동물 전문입니다");
        Proposal proposal103 = saveProposal(post10.getId(), sitter3.getId(), member4.getId(), sitter3.getMemberId(),24000, "오후 시간 괜찮아요");

        // =============================================
        // 7. CareRequest (기존 4개)
        // =============================================
        CareRequest request1 = saveCareRequest(member1.getId(), sitter3.getId(), sitter3.getMemberId(), CareType.VISIT,
                "구피 여러 마리 한꺼번에 봐주실 수 있나요?", 55000);
        saveCareRequestPet(request1.getId(), pet1);
        saveCareRequestPet(request1.getId(), pet2);
        saveCareRequestPet(request1.getId(), pet6);
        saveCareRequestTimeSlot(request1.getId(), "2026-06-18", "10:00", "14:00", 1);

        CareRequest request2 = saveCareRequest(member5.getId(), sitter1.getId(), sitter1.getMemberId(), CareType.BOARDING,
                "고양이 전문가시라고 해서 연락드려요", 90000);
        saveCareRequestPet(request2.getId(), pet16);
        saveCareRequestTimeSlot(request2.getId(), "2026-06-20", "00:00", "23:59", 1);
        saveCareRequestTimeSlot(request2.getId(), "2026-06-21", "00:00", "23:59", 2);

        CareRequest request3 = saveCareRequest(member2.getId(), sitter3.getId(), sitter3.getMemberId(), CareType.VISIT,
                "하루종일 같이 있어주실 수 있나요?", 180000);
        saveCareRequestPet(request3.getId(), pet10);
        saveCareRequestTimeSlot(request3.getId(), "2026-06-25", "08:00", "20:00", 1);

        CareRequest request4 = saveCareRequest(member3.getId(), sitter3.getId(), sitter3.getMemberId(), CareType.VISIT,
                "소형견인데 괜찮으실까요?", 35000);
        saveCareRequestPet(request4.getId(), pet11);
        saveCareRequestTimeSlot(request4.getId(), "2026-06-28", "15:00", "19:00", 1);

        // =============================================
        // 8. Reservation (기존 5개)
        // =============================================

        // post4 → proposal41 수락
        Reservation reservation1 = saveReservation(member5.getId(), member2.getId(), sitter1.getId(),
                CareType.VISIT, ReservationSource.PROPOSAL, post4.getId(), 60000);
        saveReservationPet(reservation1.getId(), pet13);
        saveReservationPet(reservation1.getId(), pet14);
        saveReservationPet(reservation1.getId(), pet15);
        saveReservationPet(reservation1.getId(), pet16);
        saveReservationTimeSlot(reservation1.getId(), "2026-05-07", "13:00", "17:00", 1);
        saveReservationTimeSlot(reservation1.getId(), "2026-05-08", "13:00", "17:00", 2);
        reservation1.confirm();
        reservation1.complete();
        ReservationPayment rp1 = saveReservationPayment(reservation1.getId(), 60000);
        rp1.sitterConfirm();
        rp1.guardianConfirm();
        reservation1.confirm();

        // post7 → proposal71 수락
        Reservation reservation2 = saveReservation(member3.getId(), member2.getId(), sitter1.getId(),
                CareType.VISIT, ReservationSource.PROPOSAL, post7.getId(), 1000);
        saveReservationPet(reservation2.getId(), pet12);
        saveReservationTimeSlot(reservation2.getId(), "2026-05-18", "09:00", "20:00", 1);
        saveReservationTimeSlot(reservation2.getId(), "2026-05-19", "09:00", "20:00", 2);
        reservation2.confirm();
        ReservationPayment rp2 = saveReservationPayment(reservation2.getId(), 1000);
        rp2.sitterConfirm();
        rp2.guardianConfirm();
        reservation2.confirm();

        // post10 → proposal103 수락
        Reservation reservation3 = saveReservation(member1.getId(), member4.getId(), sitter3.getId(),
                CareType.VISIT, ReservationSource.PROPOSAL, post10.getId(), 24000);
        saveReservationPet(reservation3.getId(), pet2);
        saveReservationPet(reservation3.getId(), pet4);
        saveReservationPet(reservation3.getId(), pet5);
        saveReservationTimeSlot(reservation3.getId(), "2026-06-12", "14:00", "17:00", 1);
        ReservationPayment rp3 = saveReservationPayment(reservation3.getId(), 24000);
        rp3.sitterConfirm();
        rp3.guardianConfirm();
        reservation3.confirm();



        // request2 수락
        Reservation reservation4 = saveReservation(member5.getId(), member2.getId(), sitter1.getId(),
                CareType.BOARDING, ReservationSource.CARE_REQUEST, request2.getId(), 90000);
        saveReservationPet(reservation4.getId(), pet16);
        saveReservationTimeSlot(reservation4.getId(), "2026-06-20", "00:00", "23:59", 1);
        saveReservationTimeSlot(reservation4.getId(), "2026-06-21", "00:00", "23:59", 2);
        saveReservationPayment(reservation4.getId(), 90000);

        // request4 수락
        Reservation reservation5 = saveReservation(member3.getId(), member4.getId(), sitter3.getId(),
                CareType.VISIT, ReservationSource.CARE_REQUEST, request4.getId(), 35000);
        saveReservationPet(reservation5.getId(), pet11);
        saveReservationTimeSlot(reservation5.getId(), "2026-06-28", "15:00", "19:00", 1);
        saveReservationPayment(reservation5.getId(), 35000);

        // =============================================================
        // ★ 여기서부터 신규 대량 데이터 (루프 기반)
        // =============================================================

        // --- 신규 Member 15명 (member6 ~ member20) ---
        Region[] regions = {Region.SEOCHO, Region.DONGJAK, Region.GANGNAM, Region.GWANGJIN, Region.YONGSAN};
        MemberGender[] genders = {MemberGender.MALE, MemberGender.FEMALE};

        List<Member> newMembers = new ArrayList<>();
        for (int i = 6; i <= 20; i++) {
            Member m = saveMember(
                    "member" + i + "@test.com",
                    "멤버" + i,
                    String.format("010-%04d-%04d", i * 100, i),
                    genders[i % 2],
                    regions[i % 5]
            );
            newMembers.add(m);
        }

        // 시터 10명: member6 ~ member15 (newMembers index 0~9)
        for (int i = 0; i < 10; i++) {
            newMembers.get(i).changeRoleToSitter();
        }

        // --- 전체 멤버/펫/시터 통합 관리용 ---
        List<Member> allMembers = new ArrayList<>(List.of(member1, member2, member3, member4, member5));
        allMembers.addAll(newMembers);

        // memberId → List<Pet> 매핑
        Map<Long, List<Pet>> petsByMember = new HashMap<>();
        petsByMember.put(member1.getId(), List.of(pet1, pet2, pet3, pet4, pet5, pet6, pet7, pet8, pet9));
        petsByMember.put(member2.getId(), List.of(pet10));
        petsByMember.put(member3.getId(), List.of(pet11, pet12));
        petsByMember.put(member5.getId(), List.of(pet13, pet14, pet15, pet16));

        // 신규 멤버 Pet 등록 (1~3마리)
        String[][] breedPool = {
                {"DOG", "말티즈"}, {"DOG", "비숑"}, {"DOG", "시바이누"}, {"DOG", "푸들"},
                {"DOG", "웰시코기"}, {"DOG", "치와와"}, {"DOG", "보더콜리"}, {"DOG", "골든리트리버"},
                {"DOG", "진돗개"}, {"DOG", "비글"}, {"DOG", "시츄"}, {"DOG", "요크셔테리어"},
                {"CAT", "러시안블루"}, {"CAT", "브리티쉬숏헤어"}, {"CAT", "페르시안"},
                {"CAT", "먼치킨"}, {"CAT", "샴"}, {"CAT", "벵갈"}, {"CAT", "메인쿤"},
                {"CAT", "스코티쉬폴드"}, {"CAT", "노르웨이숲"}, {"CAT", "아비시니안"}
        };
        PetSize[] sizes = {PetSize.SMALL, PetSize.MEDIUM, PetSize.LARGE};
        PetGender[] petGenders = {PetGender.MALE, PetGender.FEMALE};
        int petCounter = 17; // 기존 pet16까지 사용

        for (Member m : newMembers) {
            int petCount = 1 + random.nextInt(3); // 1~3마리
            List<Pet> pets = new ArrayList<>();
            for (int j = 0; j < petCount; j++) {
                String[] breed = breedPool[(petCounter - 1) % breedPool.length];
                PetSpecies species = breed[0].equals("DOG") ? PetSpecies.DOG : PetSpecies.CAT;
                Pet pet = savePet(
                        m.getId(),
                        "반려동물" + petCounter,
                        species,
                        breed[1],
                        sizes[petCounter % 3],
                        1 + random.nextInt(8),
                        petGenders[petCounter % 2],
                        null
                );
                pets.add(pet);
                petCounter++;
            }
            petsByMember.put(m.getId(), pets);
        }

        // --- 신규 SitterProfile 10명 ---
        String[] sitterIntros = {
                "소형견 위주로 돌봅니다", "고양이 사육사 출신입니다", "대형견도 가능합니다",
                "모든 동물 케어합니다", "소형 고양이 전문", "중형견 경험 많습니다",
                "동물병원 근무 경력 있음", "대형견 전문 시터", "고양이 전문 돌봄",
                "반려동물 트레이너 출신"
        };
        PossiblePetType[] petTypes = {
                PossiblePetType.DOG, PossiblePetType.CAT, PossiblePetType.DOG,
                PossiblePetType.ALL, PossiblePetType.CAT, PossiblePetType.DOG,
                PossiblePetType.ALL, PossiblePetType.DOG, PossiblePetType.CAT,
                PossiblePetType.ALL
        };
        PossiblePetSize[] petSizes = {
                PossiblePetSize.SMALL, PossiblePetSize.MEDIUM, PossiblePetSize.ALL,
                PossiblePetSize.ALL, PossiblePetSize.SMALL, PossiblePetSize.MEDIUM,
                PossiblePetSize.ALL, PossiblePetSize.LARGE, PossiblePetSize.MEDIUM,
                PossiblePetSize.ALL
        };
        int[] prices = {14000, 22000, 19000, 17000, 16000, 18000, 25000, 23000, 15000, 21000};

        List<SitterProfile> allSitters = new ArrayList<>(List.of(sitter1, sitter2, sitter3));
        // sitterMemberId → SitterProfile 매핑
        Map<Long, SitterProfile> sitterByMemberId = new HashMap<>();
        sitterByMemberId.put(member2.getId(), sitter1);
        sitterByMemberId.put(member3.getId(), sitter2);
        sitterByMemberId.put(member4.getId(), sitter3);

        for (int i = 0; i < 10; i++) {
            Member sitterMember = newMembers.get(i);
            SitterProfile sp = saveSitter(
                    sitterMember.getId(), sitterIntros[i],
                    2 + random.nextInt(7), petTypes[i], petSizes[i], prices[i]
            );
            sp.approve(1L);
            sp.changeStatus(SitterProfileStatus.RESERVABLE);
            allSitters.add(sp);
            sitterByMemberId.put(sitterMember.getId(), sp);
        }

        // --- 신규 SitterSchedule (3~5일씩) ---
        DayOfWeek[] allDays = DayOfWeek.values();
        for (int i = 3; i < allSitters.size(); i++) { // index 3부터 = sitter4~sitter13
            SitterProfile sp = allSitters.get(i);
            int dayCount = 3 + random.nextInt(3); // 3~5일
            Set<Integer> usedDays = new HashSet<>();
            for (int d = 0; d < dayCount; d++) {
                int dayIdx;
                do { dayIdx = random.nextInt(7); } while (usedDays.contains(dayIdx));
                usedDays.add(dayIdx);
                int startHour = 8 + random.nextInt(3); // 08~10
                int endHour = startHour + 6 + random.nextInt(4); // +6~9시간
                if (endHour > 20) endHour = 20;
                saveSchedule(sp.getId(), allDays[dayIdx],
                        String.format("%02d:00", startHour),
                        String.format("%02d:00", endHour));
            }
        }

        // =============================================
        // ★ 신규 Post 21개 (총 30개)
        // =============================================
        String[] postTitles = {
                "방문 돌봄 부탁해요", "위탁 돌봄 구합니다", "산책 시켜주세요",
                "하루 돌봄 구해요", "주말 돌봄 급구", "출장이라 맡길 곳 찾아요",
                "놀아주실 분 구해요", "밥 주고 케어해주세요", "돌봐주실 분 급구"
        };
        String[] postContents = {
                "잘 부탁드립니다", "경험 많은 시터분 찾아요", "밥 주고 놀아주시면 돼요",
                "건강 체크도 부탁해요", "소중한 아이 잘 부탁드려요", "급하게 구합니다",
                "방문 돌봄 선호해요", "위탁도 괜찮아요", "연락주세요"
        };

        List<Post> allOpenPosts = new ArrayList<>(List.of(post1, post2, post3, post4, post5, post7, post10));
        List<Post> newOpenPosts = new ArrayList<>();

        // 신규 멤버 중 다양하게 Post 작성 (Post 작성자 분포)
        // member6~20 중 골고루 배분하여 21개 생성
        int[] postWriterIndices = {0, 0, 1, 2, 2, 3, 4, 4, 5, 6, 7, 8, 9, 10, 10, 11, 11, 12, 12, 13, 14};
        LocalDate baseDate = LocalDate.of(2026, 6, 8);

        for (int i = 0; i < 21; i++) {
            Member writer = newMembers.get(postWriterIndices[i]);
            List<Pet> writerPets = petsByMember.get(writer.getId());
            if (writerPets == null || writerPets.isEmpty()) continue;

            CareType careType = (i % 3 == 0) ? CareType.BOARDING : CareType.VISIT;
            int budget = 15000 + random.nextInt(10) * 10000; // 15000 ~ 105000

            Post post = savePost(
                    writer.getId(),
                    writerPets.get(0).getName() + " " + postTitles[i % postTitles.length],
                    postContents[i % postContents.length],
                    careType,
                    budget,
                    PostStatus.OPEN
            );

            // Pet 연결 (1~2마리)
            int petCount = Math.min(1 + random.nextInt(2), writerPets.size());
            for (int p = 0; p < petCount; p++) {
                savePostPet(post.getId(), writerPets.get(p));
            }

            // TimeSlot 연결 (1~3개)
            LocalDate careDate = baseDate.plusDays(i * 2);
            int slotCount = 1 + random.nextInt(3);
            if (careType == CareType.BOARDING) {
                // 위탁: 연속 일자
                for (int s = 0; s < slotCount; s++) {
                    savePostTimeSlot(post.getId(), careDate.plusDays(s).toString(),
                            "00:00", "23:59", s + 1);
                }
            } else {
                // 방문: 같은날 또는 다른날 시간대
                for (int s = 0; s < slotCount; s++) {
                    int startHour = 8 + s * 3;
                    int endHour = startHour + 3 + random.nextInt(3);
                    if (endHour > 20) endHour = 20;
                    savePostTimeSlot(post.getId(), careDate.plusDays(s).toString(),
                            String.format("%02d:00", startHour),
                            String.format("%02d:00", endHour),
                            s + 1);
                }
            }

            newOpenPosts.add(post);
            allOpenPosts.add(post);
        }

        // =============================================
        // ★ 신규 Proposal (~137개, 기존 13개 합쳐 ~150개)
        // =============================================
        String[] proposalMessages = {
                "경험 풍부합니다", "근처에 살아서 바로 갈 수 있어요", null,
                "해당 품종 키워봤어요", "전문적으로 돌봐드릴게요", null,
                "시간 여유 있습니다", "다중 돌봄 가능합니다", "건강 체크도 해드려요",
                null, "소형 동물 전문이에요", "위탁 경험 많습니다"
        };

        // 기존 Proposal의 (postId, sitterProfileId) 조합 추적 → 중복 방지
        Set<String> existingProposalKeys = new HashSet<>();
        existingProposalKeys.add(post1.getId() + "-" + sitter1.getId());
        existingProposalKeys.add(post1.getId() + "-" + sitter2.getId());
        existingProposalKeys.add(post1.getId() + "-" + sitter3.getId());
        existingProposalKeys.add(post2.getId() + "-" + sitter1.getId());
        existingProposalKeys.add(post2.getId() + "-" + sitter3.getId());
        existingProposalKeys.add(post3.getId() + "-" + sitter2.getId());
        existingProposalKeys.add(post4.getId() + "-" + sitter1.getId());
        existingProposalKeys.add(post4.getId() + "-" + sitter2.getId());
        existingProposalKeys.add(post5.getId() + "-" + sitter3.getId());
        existingProposalKeys.add(post7.getId() + "-" + sitter1.getId());
        existingProposalKeys.add(post7.getId() + "-" + sitter3.getId());
        existingProposalKeys.add(post10.getId() + "-" + sitter2.getId());
        existingProposalKeys.add(post10.getId() + "-" + sitter3.getId());

        int proposalCount = 0;
        for (Post post : allOpenPosts) {
            // 이 Post 작성자가 아닌 시터들 중 중복 아닌 것만
            List<SitterProfile> eligibleSitters = new ArrayList<>();
            for (SitterProfile sp : allSitters) {
                Long sitterMemberId = findMemberIdBySitter(sp, sitterByMemberId);
                String key = post.getId() + "-" + sp.getId();
                if (sitterMemberId != null
                        && !sitterMemberId.equals(post.getMemberId())
                        && !existingProposalKeys.contains(key)) {
                    eligibleSitters.add(sp);
                }
            }
            Collections.shuffle(eligibleSitters, random);

            int count = Math.min(4 + random.nextInt(5), eligibleSitters.size()); // 4~8개

            for (int i = 0; i < count; i++) {
                SitterProfile sp = eligibleSitters.get(i);
                Long sitterMemberId = findMemberIdBySitter(sp, sitterByMemberId);

                int proposedPrice = (int) (post.getBudgetAmount() * (0.85 + random.nextDouble() * 0.3));
                saveProposal(
                        post.getId(),
                        sp.getId(),
                        sitterMemberId,
                        post.getMemberId(),
                        proposedPrice,
                        proposalMessages[proposalCount % proposalMessages.length]
                );
                existingProposalKeys.add(post.getId() + "-" + sp.getId());
                proposalCount++;
            }
        }

        // =============================================
        // ★ 신규 CareRequest (56개, 기존 4개 합쳐 60개)
        // =============================================
        String[] careRequestMessages = {
                "돌봄 가능하실까요?", "시간 되시면 부탁드려요", "경험 많으시다고 해서 연락드려요",
                "반려동물 좀 봐주세요", "급하게 부탁드립니다", "위탁 가능하신가요?",
                "산책 좀 시켜주세요", "하루만 봐주세요", "주말에 가능하신가요?",
                "여러 마리인데 괜찮으실까요?"
        };

        int careRequestCount = 0;
        LocalDate crBaseDate = LocalDate.of(2026, 6, 20);

        for (Member requester : allMembers) {
            List<Pet> requesterPets = petsByMember.get(requester.getId());
            if (requesterPets == null || requesterPets.isEmpty()) continue;

            // 각 멤버가 0~5개 CareRequest 전송
            int requestsToMake = random.nextInt(5); // 0~4개
            if (careRequestCount >= 56) break;

            // 자기가 아닌 시터 중 랜덤으로
            List<SitterProfile> eligibleSitters = new ArrayList<>();
            for (SitterProfile sp : allSitters) {
                Long sitterMemberId = findMemberIdBySitter(sp, sitterByMemberId);
                if (sitterMemberId != null && !sitterMemberId.equals(requester.getId())) {
                    eligibleSitters.add(sp);
                }
            }
            Collections.shuffle(eligibleSitters, random);

            for (int i = 0; i < Math.min(requestsToMake, eligibleSitters.size()); i++) {
                if (careRequestCount >= 56) break;

                SitterProfile targetSitter = eligibleSitters.get(i);
                CareType careType = (careRequestCount % 4 == 0) ? CareType.BOARDING : CareType.VISIT;
                int price = 20000 + random.nextInt(8) * 10000;

                CareRequest cr = saveCareRequest(
                        requester.getId(),
                        targetSitter.getId(),
                        targetSitter.getMemberId(),
                        careType,
                        careRequestMessages[careRequestCount % careRequestMessages.length],
                        price
                );

                // Pet 연결 (1~2마리)
                int petCount = Math.min(1 + random.nextInt(2), requesterPets.size());
                for (int p = 0; p < petCount; p++) {
                    saveCareRequestPet(cr.getId(), requesterPets.get(p));
                }

                // TimeSlot 연결
                LocalDate careDate = crBaseDate.plusDays(careRequestCount * 2);
                if (careType == CareType.BOARDING) {
                    int days = 1 + random.nextInt(3);
                    for (int d = 0; d < days; d++) {
                        saveCareRequestTimeSlot(cr.getId(), careDate.plusDays(d).toString(),
                                "00:00", "23:59", d + 1);
                    }
                } else {
                    int startHour = 8 + random.nextInt(4);
                    int endHour = startHour + 3 + random.nextInt(5);
                    if (endHour > 20) endHour = 20;
                    saveCareRequestTimeSlot(cr.getId(), careDate.toString(),
                            String.format("%02d:00", startHour),
                            String.format("%02d:00", endHour),
                            1);
                }

                careRequestCount++;
            }
        }

        // =============================================
        // ★ 신규 Reservation (95개, 기존 5개 합쳐 100개)
        //   PROPOSAL 기반 50개 + CARE_REQUEST 기반 45개
        //   상태 분포: PENDING 40%, CONFIRMED 40%, COMPLETED 15%, CANCELED 5%
        // =============================================

        // --- PROPOSAL 기반 Reservation 50개 ---
        int resCount = 0;
        LocalDate resBaseDate = LocalDate.of(2026, 6, 9);

        for (Post post : newOpenPosts) {
            if (resCount >= 50) break;

            // 이 Post에 달린 시터 중 하나를 수락 → Reservation 생성
            // 실제로는 Post 작성자가 수락하므로 guardianId = post.getMemberId()
            List<SitterProfile> eligibleSitters = new ArrayList<>();
            for (SitterProfile sp : allSitters) {
                Long sitterMemberId = findMemberIdBySitter(sp, sitterByMemberId);
                if (sitterMemberId != null && !sitterMemberId.equals(post.getMemberId())) {
                    eligibleSitters.add(sp);
                }
            }
            if (eligibleSitters.isEmpty()) continue;
            Collections.shuffle(eligibleSitters, random);

            // Post당 1~3개 Reservation 생성
            int resPerPost = Math.min(1 + random.nextInt(3), eligibleSitters.size());
            for (int i = 0; i < resPerPost && resCount < 50; i++) {
                SitterProfile sp = eligibleSitters.get(i);
                Long sitterMemberId = findMemberIdBySitter(sp, sitterByMemberId);
                if (sitterMemberId == null) continue;

                List<Pet> guardianPets = petsByMember.get(post.getMemberId());
                if (guardianPets == null || guardianPets.isEmpty()) continue;

                CareType careType = (resCount % 3 == 0) ? CareType.BOARDING : CareType.VISIT;
                int price = 20000 + random.nextInt(8) * 10000;

                Reservation res = saveReservation(
                        post.getMemberId(), sitterMemberId, sp.getId(),
                        careType, ReservationSource.PROPOSAL, post.getId(), price
                );

                // Pet 연결
                int petCount = Math.min(1 + random.nextInt(2), guardianPets.size());
                for (int p = 0; p < petCount; p++) {
                    saveReservationPet(res.getId(), guardianPets.get(p));
                }

                // TimeSlot 연결
                LocalDate careDate = resBaseDate.plusDays(resCount);
                if (careType == CareType.BOARDING) {
                    int days = 1 + random.nextInt(3);
                    for (int d = 0; d < days; d++) {
                        saveReservationTimeSlot(res.getId(), careDate.plusDays(d).toString(),
                                "00:00", "23:59", d + 1);
                    }
                } else {
                    int startHour = 8 + random.nextInt(4);
                    int endHour = startHour + 3 + random.nextInt(5);
                    if (endHour > 20) endHour = 20;
                    saveReservationTimeSlot(res.getId(), careDate.toString(),
                            String.format("%02d:00", startHour),
                            String.format("%02d:00", endHour),
                            1);
                }

                // 상태 전이
                applyReservationStatus(res, resCount, price);
                resCount++;
            }
        }

        // --- CARE_REQUEST 기반 Reservation 45개 ---
        // 모든 멤버 순회하며 CareRequest를 수락한 것으로 Reservation 생성
        int crResCount = 0;
        for (Member requester : allMembers) {
            if (crResCount >= 45) break;

            List<Pet> requesterPets = petsByMember.get(requester.getId());
            if (requesterPets == null || requesterPets.isEmpty()) continue;

            // 이 멤버가 아닌 시터 중 랜덤으로
            List<SitterProfile> eligibleSitters = new ArrayList<>();
            for (SitterProfile sp : allSitters) {
                Long sitterMemberId = findMemberIdBySitter(sp, sitterByMemberId);
                if (sitterMemberId != null && !sitterMemberId.equals(requester.getId())) {
                    eligibleSitters.add(sp);
                }
            }
            Collections.shuffle(eligibleSitters, random);

            int resPerMember = Math.min(1 + random.nextInt(4), eligibleSitters.size());
            for (int i = 0; i < resPerMember && crResCount < 45; i++) {
                SitterProfile sp = eligibleSitters.get(i);
                Long sitterMemberId = findMemberIdBySitter(sp, sitterByMemberId);
                if (sitterMemberId == null) continue;

                CareType careType = (crResCount % 3 == 0) ? CareType.BOARDING : CareType.VISIT;
                int price = 25000 + random.nextInt(8) * 10000;

                // CareRequest를 먼저 만들고 그것의 ID를 sourceId로 사용
                CareRequest cr = saveCareRequest(
                        requester.getId(), sp.getId(), sp.getMemberId(), careType,
                        "예약용 돌봄 요청", price
                );
                Pet firstPet = requesterPets.get(0);
                saveCareRequestPet(cr.getId(), firstPet);

                LocalDate careDate = resBaseDate.plusDays(50 + crResCount);
                if (careType == CareType.BOARDING) {
                    saveCareRequestTimeSlot(cr.getId(), careDate.toString(), "00:00", "23:59", 1);
                } else {
                    saveCareRequestTimeSlot(cr.getId(), careDate.toString(), "10:00", "18:00", 1);
                }

                Reservation res = saveReservation(
                        requester.getId(), sitterMemberId, sp.getId(),
                        careType, ReservationSource.CARE_REQUEST, cr.getId(), price
                );

                // Pet 연결
                int petCount = Math.min(1 + random.nextInt(2), requesterPets.size());
                for (int p = 0; p < petCount; p++) {
                    saveReservationPet(res.getId(), requesterPets.get(p));
                }

                // TimeSlot 연결
                if (careType == CareType.BOARDING) {
                    int days = 1 + random.nextInt(3);
                    for (int d = 0; d < days; d++) {
                        saveReservationTimeSlot(res.getId(), careDate.plusDays(d).toString(),
                                "00:00", "23:59", d + 1);
                    }
                } else {
                    saveReservationTimeSlot(res.getId(), careDate.toString(), "10:00", "18:00", 1);
                }

                // 상태 전이
                applyReservationStatus(res, resCount + crResCount, price);
                crResCount++;
            }
        }
    }

    // ===== 상태 분포 적용 =====
    private void applyReservationStatus(Reservation res, int index, int price) {
        int mod = index % 20;
        if (mod < 8) {
            // 40% PENDING — 아무것도 안 함
            ReservationPayment rp = saveReservationPayment(res.getId(), price);

            if (mod == 0 || mod == 1 || mod == 4) rp.guardianConfirm();
            else if (mod == 2 || mod == 3 || mod == 5) rp.sitterConfirm();

        } else if (mod < 16) {
            // 40% CONFIRMED
            ReservationPayment rp = saveReservationPayment(res.getId(), price);
            rp.guardianConfirm();
            rp.sitterConfirm();

            res.confirm();
        } else if (mod < 19) {
            // 15% COMPLETED

            ReservationPayment rp = saveReservationPayment(res.getId(), price);
            rp.guardianConfirm();
            rp.sitterConfirm();

            res.confirm();
            res.complete();

        } else {
            // 5% CANCELED
            ReservationPayment rp = saveReservationPayment(res.getId(), price);
            rp.guardianConfirm();
            rp.sitterConfirm();

            res.confirm();

            res.cancel("개인 사정으로 취소합니다", CancelCategory.PERSONAL, CanceledBy.GUARDIAN);

        }
    }

    // ===== sitterByMemberId 역방향 조회 =====
    private Long findMemberIdBySitter(SitterProfile sp, Map<Long, SitterProfile> sitterByMemberId) {
        for (Map.Entry<Long, SitterProfile> entry : sitterByMemberId.entrySet()) {
            if (entry.getValue().getId().equals(sp.getId())) {
                return entry.getKey();
            }
        }
        return null;
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