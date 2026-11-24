package com.urbanvibe.model;

import java.time.LocalDate;

public class PromoCode {

    private String code;
    private int percentOff;
    private LocalDate expiryDate;
    private int maxUses;
    private int usedCount;

    public PromoCode(String code, int percentOff, LocalDate expiryDate, int maxUses, int usedCount) {
        this.code = code;
        this.percentOff = percentOff;
        this.expiryDate = expiryDate;
        this.maxUses = maxUses;
        this.usedCount = usedCount;
    }

    public String getCode() { return code; }
    public int getPercentOff() { return percentOff; }
    public LocalDate getExpiryDate() { return expiryDate; }
    public int getMaxUses() { return maxUses; }
    public int getUsedCount() { return usedCount; }

    public boolean isExpired() {
        return expiryDate.isBefore(LocalDate.now());
    }

    public boolean isExhausted() {
        return usedCount >= maxUses;
    }
}
