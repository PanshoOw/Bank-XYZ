package com.duoc.demo;

import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.parameters.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BatchJobConfig {

    @Bean
    public Job transaccionesJob(
            JobRepository jobRepository,
            @Qualifier("stepTransacciones") Step stepTransacciones) {

        return new JobBuilder("reporteTransaccionesDiariasJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(stepTransacciones)
                .build();
    }

    @Bean
    public Job interesesJob(
            JobRepository jobRepository,
            @Qualifier("stepIntereses") Step stepIntereses) {

        return new JobBuilder("calculoInteresesMensualesJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(stepIntereses)
                .build();
    }

        @Bean
        public Job cuentasJob(
                JobRepository jobRepository,
                @Qualifier("stepLimpiarMovimientos") Step stepLimpiarMovimientos,
                @Qualifier("stepCuentas") Step stepCuentas,
                @Qualifier("stepResumenAnual") Step stepResumenAnual) {

        return new JobBuilder("estadosCuentaAnualesJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(stepLimpiarMovimientos)
                .next(stepCuentas)
                .next(stepResumenAnual)
                .build();
        }
}