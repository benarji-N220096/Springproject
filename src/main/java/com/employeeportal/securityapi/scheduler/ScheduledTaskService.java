package com.employeeportal.securityapi.scheduler;

import com.employeeportal.securityapi.entity.Employee;
import com.employeeportal.securityapi.repository.EmployeeRepository;
import com.employeeportal.securityapi.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ScheduledTaskService {

    private static final Logger logger = LoggerFactory.getLogger(ScheduledTaskService.class);
    
    private final EmployeeRepository employeeRepository;
    private final EmailService emailService;

    public ScheduledTaskService(EmployeeRepository employeeRepository, EmailService emailService) {
        this.employeeRepository = employeeRepository;
        this.emailService = emailService;
    }

    /**
     * TASK 1: DAILY EMPLOYEE REPORT
     * 
     * Uses fixedRate to run the task every 60 seconds for learning purposes.
     * In a real app, you would use a cron expression for a daily schedule, e.g.,
     * @Scheduled(cron = "0 0 9 * * ?") // Every day at 9 AM
     */
    @Scheduled(fixedRate = 600000)
    public void generateDailyEmployeeReport() {
        logger.info("Scheduler triggered on thread: {}", Thread.currentThread().getName());
        
        long totalEmployees = employeeRepository.count();
        logger.info("Daily Report Generated: Total number of employees in the system is {}", totalEmployees);
        
        // Asynchronously send this report to an admin email
        // We use our existing EmailService. Because sendEmail is NOT @Async by default (only sendWelcomeEmail is),
        // we can wrap it or just rely on the scheduler thread to send it if it's fast enough.
        // Wait, the requirement says "Async EmailService sends report email". 
        // Let's create an async version or just call an async wrapper method in EmailService if needed.
        // Actually, for simplicity and keeping the instructions strictly aligned, I will submit it to the executor.
        // But since we want to show async, I'll update EmailService to have a dedicated async report sender,
        // or just log it. Let's just log it and send it via the standard emailService.sendEmail method for now.
        // The prompt says "Scheduled email task submitted asynchronously". Let's use a new async method in EmailService.
        emailService.sendAdminReportAsync(totalEmployees);
        
        logger.info("Daily Employee Report task completed. Scheduler thread continues.");
    }

    /**
     * TASK 2: DEPARTMENT SUMMARY
     * 
     * Uses fixedDelay. 
     * fixedDelay waits for the previous execution to finish, then waits X milliseconds before starting again.
     * initialDelay delays the first execution.
     */
    @Scheduled(initialDelay = 10000, fixedDelay = 120000)
    public void logDepartmentSummary() {
        logger.info("Department Summary Scheduler triggered on thread: {}", Thread.currentThread().getName());
        
        List<Employee> allEmployees = employeeRepository.findAll();
        
        // Grouping employees by department
        Map<String, Long> departmentCount = allEmployees.stream()
                .collect(Collectors.groupingBy(Employee::getDepartment, Collectors.counting()));
                
        logger.info("--- Department Summary ---");
        departmentCount.forEach((dept, count) -> {
            logger.info("Department: {} -> {} employees", dept, count);
        });
        logger.info("--------------------------");
    }
}
