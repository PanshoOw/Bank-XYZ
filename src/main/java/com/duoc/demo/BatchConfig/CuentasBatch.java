package com.duoc.demo.BatchConfig;

import java.time.format.DateTimeParseException;

import javax.sql.DataSource;

import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.database.JdbcBatchItemWriter;
import org.springframework.batch.infrastructure.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.infrastructure.item.file.FlatFileItemReader;
import org.springframework.batch.infrastructure.item.file.FlatFileParseException;
import org.springframework.batch.infrastructure.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import com.duoc.demo.Legacy.LegacyCuentas;
import com.duoc.demo.Modern.ModernCuentas;
import com.duoc.demo.Processor.LegacyCuentasProcessor;

@Configuration
public class CuentasBatch {

    @Bean
    public FlatFileItemReader<LegacyCuentas> cuentasReader() {

        return new FlatFileItemReaderBuilder<LegacyCuentas>()
                .name("legacyCuentasReader")
                .resource(new ClassPathResource("data/cuentas_anuales.csv"))
                .linesToSkip(1)
                .delimited()
                .delimiter(",")
                .names(
                        "cuentaId",
                        "fecha",
                        "transaccion",
                        "monto",
                        "descripcion"
                )
                .fieldSetMapper(fieldSet -> new LegacyCuentas(
                        fieldSet.readString("cuentaId"),
                        fieldSet.readString("fecha"),
                        fieldSet.readString("transaccion"),
                        fieldSet.readString("monto"),
                        fieldSet.readString("descripcion")
                ))
                .build();
    }

    @Bean
    public LegacyCuentasProcessor cuentasProcessor() {
        return new LegacyCuentasProcessor();
    }

    @Bean
    public JdbcBatchItemWriter<ModernCuentas> cuentasWriter(
            DataSource dataSource) {

        String sql = """
                INSERT INTO MOVIMIENTOS_ANUALES_PROCESADOS
                    (
                        CUENTA_ID,
                        FECHA,
                        TRANSACCION,
                        MONTO,
                        DESCRIPCION,
                        ESTADO,
                        DETALLE_VALIDACION
                    )
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;

        return new JdbcBatchItemWriterBuilder<ModernCuentas>()
                .dataSource(dataSource)
                .sql(sql)
                .itemPreparedStatementSetter((cuenta, ps) -> {
                    ps.setInt(1, cuenta.cuentaId());
                    ps.setDate(
                            2,
                            java.sql.Date.valueOf(cuenta.fecha())
                    );
                    ps.setString(3, cuenta.transaccion());
                    ps.setBigDecimal(4, cuenta.monto());
                    ps.setString(5, cuenta.descripcion());
                    ps.setString(6, cuenta.estado());
                    ps.setString(7, cuenta.detalleValidacion());
                })
                .build();
    }

    @Bean
    Step stepCuentas(
            JobRepository jobRepository,
            FlatFileItemReader<LegacyCuentas> reader,
            LegacyCuentasProcessor processor,
            JdbcBatchItemWriter<ModernCuentas> writer) {

        return new StepBuilder("stepCuentas", jobRepository)
                .<LegacyCuentas, ModernCuentas>chunk(2)
                .reader(reader)
                .processor(processor)
                .writer(writer)
                .faultTolerant()
                .skip(FlatFileParseException.class)
                .skip(NumberFormatException.class)
                .skip(DateTimeParseException.class)
                .skipLimit(10)
                .build();
    }

    @Bean
    Step stepResumenAnual(
            JobRepository jobRepository,
            DataSource dataSource,
            PlatformTransactionManager transactionManager) {

        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

        String sql = """
                MERGE INTO ESTADOS_CUENTA_ANUALES destino
                USING (
                    SELECT
                        CUENTA_ID,
                        EXTRACT(YEAR FROM FECHA) AS ANIO,

                        SUM(
                            CASE
                                WHEN ESTADO = 'VALIDA'
                                AND TRANSACCION = 'deposito'
                                THEN MONTO
                                ELSE 0
                            END
                        ) AS TOTAL_DEPOSITOS,

                        SUM(
                            CASE
                                WHEN ESTADO = 'VALIDA'
                                AND TRANSACCION = 'retiro'
                                THEN ABS(MONTO)
                                ELSE 0
                            END
                        ) AS TOTAL_RETIROS,

                        SUM(
                            CASE
                                WHEN ESTADO = 'VALIDA'
                                AND TRANSACCION = 'compra'
                                THEN ABS(MONTO)
                                ELSE 0
                            END
                        ) AS TOTAL_COMPRAS,

                        COUNT(*) AS CANTIDAD_MOVIMIENTOS,

                        SUM(
                            CASE
                                WHEN ESTADO = 'VALIDA'
                                THEN MONTO
                                ELSE 0
                            END
                        ) AS SALDO_ANUAL,

                        SUM(
                            CASE
                                WHEN ESTADO = 'ANOMALIA'
                                THEN 1
                                ELSE 0
                            END
                        ) AS CANTIDAD_ANOMALIAS

                    FROM MOVIMIENTOS_ANUALES_PROCESADOS
                    GROUP BY
                        CUENTA_ID,
                        EXTRACT(YEAR FROM FECHA)
                ) origen

                ON (
                    destino.CUENTA_ID = origen.CUENTA_ID
                    AND destino.ANIO = origen.ANIO
                )

                WHEN MATCHED THEN
                    UPDATE SET
                        destino.TOTAL_DEPOSITOS = origen.TOTAL_DEPOSITOS,
                        destino.TOTAL_RETIROS = origen.TOTAL_RETIROS,
                        destino.TOTAL_COMPRAS = origen.TOTAL_COMPRAS,
                        destino.CANTIDAD_MOVIMIENTOS = origen.CANTIDAD_MOVIMIENTOS,
                        destino.SALDO_ANUAL = origen.SALDO_ANUAL,
                        destino.CANTIDAD_ANOMALIAS = origen.CANTIDAD_ANOMALIAS,
                        destino.FECHA_GENERACION = SYSTIMESTAMP

                WHEN NOT MATCHED THEN
                    INSERT (
                        CUENTA_ID,
                        ANIO,
                        TOTAL_DEPOSITOS,
                        TOTAL_RETIROS,
                        TOTAL_COMPRAS,
                        CANTIDAD_MOVIMIENTOS,
                        SALDO_ANUAL,
                        CANTIDAD_ANOMALIAS
                    )
                    VALUES (
                        origen.CUENTA_ID,
                        origen.ANIO,
                        origen.TOTAL_DEPOSITOS,
                        origen.TOTAL_RETIROS,
                        origen.TOTAL_COMPRAS,
                        origen.CANTIDAD_MOVIMIENTOS,
                        origen.SALDO_ANUAL,
                        origen.CANTIDAD_ANOMALIAS
                    )
                """;

        return new StepBuilder("stepResumenAnual", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    jdbcTemplate.update(sql);
                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }

    @Bean
    Step stepLimpiarMovimientos(
            JobRepository jobRepository,
            DataSource dataSource,
            PlatformTransactionManager transactionManager) {

        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

        return new StepBuilder("stepLimpiarMovimientos", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    jdbcTemplate.update(
                            "DELETE FROM MOVIMIENTOS_ANUALES_PROCESADOS"
                    );
                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }
}