package com.employeeportal.securityapi.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class SchedulingConfig {
    // Enabling scheduling in a separate configuration class keeps our main 
    // application class clean and modular.
}
