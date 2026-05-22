package com.forpets.domain.payment.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.forpets.domain.payment.dto.CreatePaymentRequest;
import com.forpets.domain.payment.dto.ConfirmPaymentRequest;
import com.forpets.domain.payment.dto.ConfirmPaymentResponse;
import com.forpets.domain.payment.dto.FailPaymentRequest;
import com.forpets.domain.payment.dto.PaymentResponseDto;
import com.forpets.domain.payment.dto.PortOneWebhookResponse;
import com.forpets.domain.payment.service.PaymentService;
import com.forpets.domain.payment.service.PaymentWebhookService;
import com.forpets.global.common.ApiResponse;
import com.forpets.global.security.annotation.LoginUser;
import com.forpets.global.security.dto.CurrentMember;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final PaymentWebhookService paymentWebhookService;

    /*
    결제 요청 생성
    실제 PortOne 승인 검증은 PET-24에서 별도 API로 추가한다.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<PaymentResponseDto>> create(
            @LoginUser CurrentMember currentMember,
            @RequestBody @Valid CreatePaymentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(paymentService.create(currentMember.id(), request)));
    }

    /*
    결제 승인 검증
    PortOne V2 결제 단건 조회 결과와 서버에 저장된 결제 금액을 비교한다.
     */
    @PostMapping("/confirm")
    public ResponseEntity<ApiResponse<ConfirmPaymentResponse>> confirm(
            @LoginUser CurrentMember currentMember,
            @RequestBody @Valid ConfirmPaymentRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success(paymentService.confirm(currentMember.id(), request)));
    }

    /*
    결제 실패 처리
    프론트 결제 실패/취소 콜백에서 호출해 진행 중 결제를 FAILED로 변경한다.
     */
    @PostMapping("/fail")
    public ResponseEntity<ApiResponse<PaymentResponseDto>> fail(
            @LoginUser CurrentMember currentMember,
            @RequestBody @Valid FailPaymentRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success(paymentService.fail(currentMember.id(), request)));
    }

    @PostMapping("/webhook/portone")
    public ResponseEntity<ApiResponse<PortOneWebhookResponse>> receivePortOneWebhook(
            @RequestBody JsonNode payload) {
        return ResponseEntity.ok(
                ApiResponse.success(paymentWebhookService.handlePortOneWebhook(payload)));
    }
}
