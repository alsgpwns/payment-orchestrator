package com.hjmin.payment.pgmock.config;

import com.hjmin.payment.pgmock.store.PgTxStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StoreConfig {
    @Bean
    public PgTxStore pgTxStore() {
        return new PgTxStore();
    }
}