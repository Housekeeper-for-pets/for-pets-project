package com.forpets.domain.coupon.service.issue;

public enum CouponIssueStrategy {

    NO_LOCK("no-lock"),
    PESSIMISTIC("pessimistic"),
    OPTIMISTIC("optimistic"),
    DISTRIBUTED("distributed");

    private final String metricName;

    CouponIssueStrategy(String metricName) {
        this.metricName = metricName;
    }

    public String metricName() {
        return metricName;
    }
}