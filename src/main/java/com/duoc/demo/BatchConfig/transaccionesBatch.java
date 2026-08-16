package com.duoc.demo.BatchConfig;

import java.time.format.DateTimeParseException;

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

import com.duoc.demo.Legacy.LegacyTransacciones;
import com.duoc.demo.Modern.ModernTransacciones;
import com.duoc.demo.Processor.LegacyTransaccionesProcessor;

@Configuration
public class transaccionesBatch {

    @Bean
    public FlatFileItemReader<LegacyTransacciones> transaccionesReader() {

        return new FlatFileItemReaderBuilder<LegacyTransacciones>()
                .name("legacyTransaccionesReader")
                .resource(new ClassPathResource("data/transacciones.csv"))
                .linesToSkip(1)
                .delimited()
                .delimiter(",")
                .names("id", "fecha", "monto", "tipo")
                .fieldSetMapper(fieldSet -> new LegacyTransacciones(
                        fieldSet.readString("id"),
                        fieldSet.readString("fecha"),
                        fieldSet.readString("monto"),
                        fieldSet.readString("tipo")
                ))
                .build();
    }

    @Bean
    @StepScope
    public LegacyTransaccionesProcessor transaccionesProcessor() {
        return new LegacyTransaccionesProcessor();
    }

    @Bean
    public JdbcBatchItemWriter<ModernTransacciones> transaccionesWriter(
            DataSource dataSource) {

        String sql = """
                MERGE INTO TRANSACCIONES_PROCESADAS destino
                USING (
                    SELECT
                        ? AS ID,
                        ? AS FECHA,
                        ? AS MONTO,
                        ? AS TIPO,
                        ? AS ESTADO,
                        ? AS DETALLE_VALIDACION
                    FROM DUAL
                ) origen
                ON (destino.ID = origen.ID)

                WHEN MATCHED THEN
                    UPDATE SET
                        destino.FECHA = origen.FECHA,
                        destino.MONTO = origen.MONTO,
                        destino.TIPO = origen.TIPO,
                        destino.ESTADO = origen.ESTADO,
                        destino.DETALLE_VALIDACION = origen.DETALLE_VALIDACION,
                        destino.FECHA_PROCESAMIENTO = SYSTIMESTAMP

                WHEN NOT MATCHED THEN
                    INSERT (
                        ID,
                        FECHA,
                        MONTO,
                        TIPO,
                        ESTADO,
                        DETALLE_VALIDACION
                    )
                    VALUES (
                        origen.ID,
                        origen.FECHA,
                        origen.MONTO,
                        origen.TIPO,
                        origen.ESTADO,
                        origen.DETALLE_VALIDACION
                    )
                """;

        return new JdbcBatchItemWriterBuilder<ModernTransacciones>()
                .dataSource(dataSource)
                .sql(sql)
                .itemPreparedStatementSetter((transaccion, ps) -> {
                    ps.setInt(1, transaccion.id());
                    ps.setDate(
                            2,
                            java.sql.Date.valueOf(transaccion.fecha())
                    );
                    ps.setBigDecimal(3, transaccion.monto());
                    ps.setString(4, transaccion.tipo());
                    ps.setString(5, transaccion.estado());
                    ps.setString(6, transaccion.detalleValidacion());
                })
                .build();
    }

    @Bean
    Step stepTransacciones(
            JobRepository jobRepository,
            FlatFileItemReader<LegacyTransacciones> reader,
            LegacyTransaccionesProcessor processor,
            JdbcBatchItemWriter<ModernTransacciones> writer) {

        return new StepBuilder("stepTransacciones", jobRepository)
                .<LegacyTransacciones, ModernTransacciones>chunk(2)
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
}