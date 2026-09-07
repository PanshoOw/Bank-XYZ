package com.duoc.bffmobile.dto;

import java.math.BigDecimal;

public record CuentaBackendResponse(
        Integer cuentaId,
        String nombre,
        BigDecimal saldoInicial,
        Integer edad,
        String tipo,
        BigDecimal tasaInteres,
        BigDecimal interesCalculado,
        BigDecimal saldoFinal,
        String estado
) {
}