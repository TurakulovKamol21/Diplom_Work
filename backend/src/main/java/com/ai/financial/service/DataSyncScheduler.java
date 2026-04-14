package com.ai.financial.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class DataSyncScheduler {

    private final CbuIntegrationService cbuService;

    public DataSyncScheduler(CbuIntegrationService cbuService) {
        this.cbuService = cbuService;
    }

    // Run every day at midnight
    @Scheduled(cron = "0 0 0 * * ?")
    public void scheduleDailySync() {
        System.out.println("Starting scheduled CBU data sync...");
        cbuService.fetchAndSaveLatestRates();
        System.out.println("Scheduled CBU data sync completed.");
    }
}
