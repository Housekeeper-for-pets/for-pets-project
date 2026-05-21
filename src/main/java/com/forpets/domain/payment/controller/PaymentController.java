package com.forpets.domain.payment.controller;

import com.forpets.domain.payment.dto.CreatePaymentRequest;
import com.forpets.domain.payment.dto.PaymentResponseDto;
import com.forpets.domain.payment.service.PaymentService;
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
}
