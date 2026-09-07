package com.duoc.bffatm.dto;

import java.math.BigDecimal;

public record RetiroBackendResponse(
        Integer cuentaId,
        BigDecimal montoRetirado,
        BigDecimal saldoDisponible,
        String mensaje
) {
}