package com.duoc.bffatm.dto;

import java.math.BigDecimal;

public record RetiroAtmResponse(
        Integer cuentaId,
        BigDecimal montoRetirado,
        BigDecimal saldoDisponible,
        String mensaje
) {
}