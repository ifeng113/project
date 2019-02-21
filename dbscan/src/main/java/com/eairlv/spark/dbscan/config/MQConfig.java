package com.eairlv.spark.dbscan.config;

import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MQConfig {

    public static final String DB_SCAN_INSTRUCTION = "db_scan_instruction";
    public static final String DB_SCAN_RESULT = "db_scan_result";

    @Bean
    Queue instructionQueue(){
        return new Queue(DB_SCAN_INSTRUCTION);
    }

    @Bean
    Queue resultQueue(){
        return new Queue(DB_SCAN_RESULT);
    }

}
