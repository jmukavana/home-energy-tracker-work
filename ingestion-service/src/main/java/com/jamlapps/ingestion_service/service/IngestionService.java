package com.jamlapps.ingestion_service.service;

import com.jamlapps.ingestion_service.dto.EnergyUsageDto;
import com.jamlapps.kafka.event.EnergyUsageEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@Slf4j
public class IngestionService {

    private final KafkaTemplate<String, EnergyUsageEvent> kafkaTemplate;

    public IngestionService(KafkaTemplate<String, EnergyUsageEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void ingestEnergyUsage(EnergyUsageDto input) {
        EnergyUsageEvent event = EnergyUsageEvent.newBuilder()
                .setEventId(UUID.randomUUID().toString())
                .setDeviceId(input.deviceId())
                .setEnergyConsumed(input.energyConsumed())
                .setTimestampEpochMillis(input.timestamp().toEpochMilli())
                .build();

        kafkaTemplate.send("energy-usage-v2", event);
        log.info("Ingested Energy Usage Event: {}", event);
    }
}
