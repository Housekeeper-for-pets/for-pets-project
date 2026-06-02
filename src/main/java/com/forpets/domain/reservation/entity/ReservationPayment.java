package com.forpets.domain.reservation.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "reservation_payment")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReservationPayment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reservation_id", nullable = false, unique = true)
    private Long reservationId;

    @Column(nullable = false)
    private boolean guardianPaid;

    @Column(nullable = false)
    private int guardianPrice;

    @Column(nullable = false)
    private boolean sitterPaid;

    @Column(nullable = false)
    private int sitterPrice;

    /*
    ReservationPayment 는 confirm/refund/expire 흐름에서 양쪽 결제 플래그가 동시 변경될 수 있어
    Lock 으로 1차 직렬화 + @Version 으로 stale write 마지막 방어선.
     */
    @Version
    @Column(nullable = false)
    private Long version;

    public static ReservationPayment create(Long reservationId, int guardianPrice, int sitterPrice) {
        ReservationPayment payment = new ReservationPayment();
        payment.reservationId = reservationId;
        payment.guardianPaid = false;
        payment.sitterPaid = false;
        payment.guardianPrice = guardianPrice;
        payment.sitterPrice = sitterPrice;
        return payment;
    }

    public void guardianConfirm() {
        this.guardianPaid = true;
    }

    public void sitterConfirm() {
        this.sitterPaid = true;
    }

    public void guardianRefund() {
        this.guardianPaid = false;
    }

    public void sitterRefund() {
        this.sitterPaid = false;
    }

    public boolean isBothPaid() {
        return this.guardianPaid && this.sitterPaid;
    }
}
