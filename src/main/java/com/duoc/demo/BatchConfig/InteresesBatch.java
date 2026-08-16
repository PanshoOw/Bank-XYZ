package com.duoc.demo.BatchConfig;

import javax.sql.DataSource;

import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.database.JdbcBatchItemWriter;
import org.springframework.batch.infrastructure.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.infrastructure.item.file.FlatFileItemReader;
import org.springframework.batch.infrastructure.item.file.FlatFileParseException;
import org.springframework.batch.infrastructure.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import com.duoc.demo.Legacy.LegacyIntereses;
import com.duoc.demo.Modern.ModernIntereses;
import com.duoc.demo.Processor.LegacyInteresesProcessor;

@Configuration
public class InteresesBatch {

    @Bean
    public FlatFileItemReader<LegacyIntereses> interesesReader() {

        return new FlatFileItemReaderBuilder<LegacyIntereses>()
                .name("legacyInteresesReader")
                .resource(new ClassPathResource("data/intereses.csv"))
                .linesToSkip(1)
                .delimited()
                .delimiter(",")
                .names("cuentaId", "nombre", "saldo", "edad", "tipo")
                .fieldSetMapper(fieldSet -> new LegacyIntereses(
                        fieldSet.readString("cuentaId"),
                        fieldSet.readString("nombre"),
                        fieldSet.readString("saldo"),
                        fieldSet.readString("edad"),
                        fieldSet.readString("tipo")
                ))
                .build();
    }

    @Bean
    @StepScope
    public LegacyInteresesProcessor interesesProcessor() {
        return new LegacyInteresesProcessor();
    }

    @Bean
    public JdbcBatchItemWriter<ModernIntereses> interesesWriter(
            DataSource dataSource) {

        String sql = """
                MERGE INTO INTERESES_PROCESADOS destino
                USING (
                    SELECT
                        ? AS CUENTA_ID,
                        ? AS NOMBRE,
                        ? AS SALDO_INICIAL,
                        ? AS EDAD,
                        ? AS TIPO,
                        ? AS TASA_INTERES,
                        ? AS INTERES_CALCULADO,
                        ? AS SALDO_FINAL,
                        ? AS ESTADO,
                        ? AS DETALLE_VALIDACION
                    FROM DUAL
                ) origen
                ON (destino.CUENTA_ID = origen.CUENTA_ID)

                WHEN MATCHED THEN
                    UPDATE SET
                        destino.NOMBRE = origen.NOMBRE,
                        destino.SALDO_INICIAL = origen.SALDO_INICIAL,
                        destino.EDAD = origen.EDAD,
                        destino.TIPO = origen.TIPO,
                        destino.TASA_INTERES = origen.TASA_INTERES,
                        destino.INTERES_CALCULADO = origen.INTERES_CALCULADO,
                        destino.SALDO_FINAL = origen.SALDO_FINAL,
                        destino.ESTADO = origen.ESTADO,
                        destino.DETALLE_VALIDACION = origen.DETALLE_VALIDACION,
                        destino.FECHA_PROCESAMIENTO = SYSTIMESTAMP

                WHEN NOT MATCHED THEN
                    INSERT (
                        CUENTA_ID,
                        NOMBRE,
                        SALDO_INICIAL,
                        EDAD,
                        TIPO,
                        TASA_INTERES,
                        INTERES_CALCULADO,
                        SALDO_FINAL,
                        ESTADO,
                        DETALLE_VALIDACION
                    )
                    VALUES (
                        origen.CUENTA_ID,
                        origen.NOMBRE,
                        origen.SALDO_INICIAL,
                        origen.EDAD,
                        origen.TIPO,
                        origen.TASA_INTERES,
                        origen.INTERES_CALCULADO,
                        origen.SALDO_FINAL,
                        origen.ESTADO,
                        origen.DETALLE_VALIDACION
                    )
                """;

        return new JdbcBatchItemWriterBuilder<ModernIntereses>()
                .dataSource(dataSource)
                .sql(sql)
                .itemPreparedStatementSetter((interes, ps) -> {
                    ps.setInt(1, interes.cuentaId());
                    ps.setString(2, interes.nombre());
                    ps.setBigDecimal(3, interes.saldoInicial());
                    ps.setInt(4, interes.edad());
                    ps.setString(5, interes.tipo());
                    ps.setBigDecimal(6, interes.tasaInteres());
                    ps.setBigDecimal(7, interes.interesCalculado());
                    ps.setBigDecimal(8, interes.saldoFinal());
                    ps.setString(9, interes.estado());
                    ps.setString(10, interes.detalleValidacion());
                })
                .build();
    }

    @Bean
    Step stepIntereses(
            JobRepository jobRepository,
            FlatFileItemReader<LegacyIntereses> reader,
            LegacyInteresesProcessor processor,
            JdbcBatchItemWriter<ModernIntereses> writer) {

        return new StepBuilder("stepIntereses", jobRepository)
                .<LegacyIntereses, ModernIntereses>chunk(2)
                .reader(reader)
                .processor(processor)
                .writer(writer)
                .faultTolerant()
                .skip(FlatFileParseException.class)
                .skip(NumberFormatException.class)
                .skipLimit(10)
                .build();
    }
}