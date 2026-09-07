package com.duoc.bffatm.dto;

import java.math.BigDecimal;

public record SaldoAtmResponse(
        Integer cuentaId,
        BigDecimal saldoDisponible,
        String estado
) {
}