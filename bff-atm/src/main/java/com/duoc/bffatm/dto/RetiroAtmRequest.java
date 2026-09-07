package com.duoc.bffatm.dto;

import java.math.BigDecimal;

public record RetiroAtmRequest(
        BigDecimal monto
) {
}