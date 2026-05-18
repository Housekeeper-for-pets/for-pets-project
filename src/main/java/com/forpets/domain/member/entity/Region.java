package com.forpets.domain.member.entity;


import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Region {
    UNKNOWN("없음"),

    // 강남권
    GANGNAM("강남구"),
    SEOCHO("서초구"),
    SONGPA("송파구"),
    GANGDONG("강동구"),

    // 강북권
    GANGBUK("강북구"),
    SEONGBUK("성북구"),
    DOBONG("도봉구"),
    NOWON("노원구"),

    // 도심권
    JONGNO("종로구"),
    JUNG("중구"),
    YONGSAN("용산구"),

    // 서북권
    EUNPYEONG("은평구"),
    SEODAEMUN("서대문구"),
    MAPO("마포구"),

    // 서남권
    GANGSEO("강서구"),
    YANGCHEON("양천구"),
    GURO("구로구"),
    GEUMCHEON("금천구"),
    YEONGDEUNGPO("영등포구"),
    DONGJAK("동작구"),
    GWANAK("관악구"),

    // 동북권
    DONGDAEMUN("동대문구"),
    JUNGNANG("중랑구"),
    SEONGDONG("성동구"),
    GWANGJIN("광진구");

    private final String description;
}