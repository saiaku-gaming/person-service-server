package com.valhallagame.personserviceserver.job;

import com.valhallagame.personserviceserver.service.PersonService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Profile("development")
public class DebugPersonCleanupJob {

    @Autowired
    private PersonService personService;

    @Scheduled(fixedRate = 60000L, initialDelay = 5000L)
    public void execute() {
        personService.deleteOldDebugPersons();
    }

}
