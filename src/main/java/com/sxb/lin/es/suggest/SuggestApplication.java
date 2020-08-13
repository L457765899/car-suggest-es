package com.sxb.lin.es.suggest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@EnableTransactionManagement
@SpringBootApplication
public class SuggestApplication implements CommandLineRunner, DisposableBean {

    private final Logger logger = LoggerFactory.getLogger(SuggestApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(SuggestApplication.class, args);
    }

    @Override
    public void destroy() throws Exception {
        logger.info("suggest app stop-------------------------------------------------------------");
    }

    @Override
    public void run(String... args) throws Exception {
        logger.info("suggest app start-------------------------------------------------------------");
    }
}
