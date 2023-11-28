package com.example.s3rekognition;

import lombok.*;
import org.springframework.beans.factory.annotation.Required;

import java.math.BigDecimal;
import java.math.BigInteger;

@ToString
@Data
@RequiredArgsConstructor
public class MedicalSupplies {
    private String id;
    private String drugName;
    private BigDecimal supplyBalance = BigDecimal.valueOf(0);

    public BigDecimal getSupplyBalance() {
        return supplyBalance;
    }


}
