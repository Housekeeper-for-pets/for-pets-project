package com.forpets.global.init;

/**
 * 더미데이터 삽입 목표 건수 공유 상수.
 * 시터(SitterDummyDataInserter)·공고(PostDummyDataInserter) 인서터가 같은 값을 참조한다.
 * 건수를 바꿀 때는 이 한 곳만 수정하면 된다. (예: 50_000 → 100_000)
 */
final class DummyDataConstants {

    static final int TARGET_COUNT = 50_000;

    private DummyDataConstants() {
    }
}
