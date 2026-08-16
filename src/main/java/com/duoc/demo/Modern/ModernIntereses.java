package com.duoc.demo.Modern;

import java.math.BigDecimal;

public record ModernIntereses(
    Integer cuentaId,
    String nombre,
    BigDecimal saldoInicial,
    Integer edad,
    String tipo,
    BigDecimal tasaInteres,
    BigDecimal interesCalculado,
    BigDecimal saldoFinal,
    String estado,
    String detalleValidacion
) {
}