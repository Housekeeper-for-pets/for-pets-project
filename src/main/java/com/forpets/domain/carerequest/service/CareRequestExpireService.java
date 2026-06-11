package com.forpets.domain.carerequest.service;

import com.forpets.domain.carerequest.entity.CareRequest;
import com.forpets.domain.carerequest.entity.CareRequestStatus;
import com.forpets.domain.carerequest.repository.CareRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/*
호출 흐름
 CareRequestExpireScheduler
 -> CareRequestLockService.executeWithCareRequestLock(...)
 -> CareRequestExpireService.expireOne(careRequestId)   :여기서부터 Transactional

 내부 흐름:
 1) careRequest 재조회
 2) 상태 가드 - PENDING/ACCEPTED 가 아니면 skip
 3) careRequest.expire()   : PENDING/ACCEPTED -> EXPIRED

 ACCEPTED 인 경우엔 대응하는 Reservation 이 이미 만들어진 상태인데,
 timeslot 이 지났다면 그 Reservation 도 정상 흐름에서 만료/완료/취소 되었을 것이고
 더 이상 작업 가능한 게 없으므로 CareRequest 만 EXPIRED 로 마킹한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CareRequestExpireService {

    private final CareRequestRepository careRequestRepository;

    @Transactional
    public void expireOne(Long careRequestId) {
        CareRequest careRequest = careRequestRepository.findById(careRequestId).orElse(null);
        if (careRequest == null) {
            log.warn("[CareRequestExpireService] careRequest 가 존재하지 않음 careRequestId={}", careRequestId);
            return;
        }

        CareRequestStatus status = careRequest.getStatus();
        if (status != CareRequestStatus.PENDING && status != CareRequestStatus.ACCEPTED) {
            log.info("[CareRequestExpireService] PENDING/ACCEPTED 가 아님 -> skip careRequestId={}, status={}",
                    careRequestId, status);
            return;
        }

        careRequest.expire();
        log.info("[CareRequestExpireService] 만료 처리 완료 careRequestId={}, 이전 status={}",
                careRequestId, status);
    }
}
