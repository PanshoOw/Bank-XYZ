package com.duoc.demo.Processor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Set;

import org.springframework.batch.infrastructure.item.ItemProcessor;

import com.duoc.demo.Legacy.LegacyTransacciones;
import com.duoc.demo.Modern.ModernTransacciones;

public class LegacyTransaccionesProcessor
        implements ItemProcessor<LegacyTransacciones, ModernTransacciones> {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private static final String ESTADO_VALIDA = "VALIDA";
    private static final String ESTADO_ANOMALIA = "ANOMALIA";

    private final Set<String> transaccionesProcesadas = new HashSet<>();

    @Override
    public ModernTransacciones process(LegacyTransacciones item) throws Exception {

        Integer id = Integer.valueOf(item.id().trim());
        LocalDate fecha = LocalDate.parse(item.fecha().trim(), FORMATTER);
        BigDecimal monto = new BigDecimal(item.monto().trim());
        String tipo = item.tipo().trim().toLowerCase();

        String estado = ESTADO_VALIDA;
        String detalleValidacion = null;

        // Validar montos
        if (monto.compareTo(BigDecimal.ZERO) < 0) {
            estado = ESTADO_ANOMALIA;
            detalleValidacion = "Monto negativo";

        } else if (monto.compareTo(BigDecimal.ZERO) == 0) {
            estado = ESTADO_ANOMALIA;
            detalleValidacion = "Monto igual a cero";
        }

        // Detectar posibles transacciones duplicadas
        String claveTransaccion =
                fecha + "|" +
                monto.stripTrailingZeros().toPlainString() + "|" +
                tipo;

        if (!transaccionesProcesadas.add(claveTransaccion)) {
            estado = ESTADO_ANOMALIA;

            if (detalleValidacion == null) {
                detalleValidacion = "Posible transaccion duplicada";
            } else {
                detalleValidacion += "; posible transaccion duplicada";
            }
        }

        return new ModernTransacciones(
                id,
                fecha,
                monto,
                tipo,
                estado,
                detalleValidacion
        );
    }
}