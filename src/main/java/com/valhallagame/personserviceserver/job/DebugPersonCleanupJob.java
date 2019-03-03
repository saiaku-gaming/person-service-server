package com.valhallagame.personserviceserver.job;

import com.valhallagame.personserviceserver.service.PersonService;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@Profile("development")
public class DebugPersonCleanupJob {

    @Autowired
    private PersonService personService;

    @Value("${spring.application.name}")
    private String appName;

    @Scheduled(fixedRate = 60000L, initialDelay = 5000L)
    public void execute() {
        MDC.put("service_name", appName);
        MDC.put("request_id", UUID.randomUUID().toString());

        try {
            personService.deleteOldDebugPersons();
        } finally {
            MDC.clear();
        }
    }

}
