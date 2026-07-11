package com.employeeportal.securityapi.service;

import com.employeeportal.securityapi.entity.Employee;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);
    
    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * Reusable method for sending a simple email.
     */
    public void sendEmail(String to, String subject, String body) {
        try {
            logger.info("Preparing to send email to: {}", to);
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            // Optionally, you can set the "From" address if required by your SMTP provider:
            // message.setFrom("your.email@example.com");

            mailSender.send(message);
            logger.info("Email successfully sent to: {}", to);
        } catch (Exception e) {
            logger.error("Failed to send email to: {}. Error: {}", to, e.getMessage());
            // In a real application, you might want to save failed emails to a database table to retry later
        }
    }

    /**
     * Asynchronously sends an admin report.
     */
    @Async("asyncExecutor")
    public void sendAdminReportAsync(long totalEmployees) {
        logger.info("Async admin report email started on thread: {}", Thread.currentThread().getName());
        
        String subject = "Daily Employee Report";
        String body = "The total number of employees in the system is: " + totalEmployees;
        
        // Use a placeholder admin email since we don't have a specific admin email configured
        sendEmail("admin@employeeportal.com", subject, body);
        
        logger.info("Async admin report email task completed on thread: {}", Thread.currentThread().getName());
    }

    /**
     * Asynchronous method to send a welcome email to a new employee.
     * The @Async annotation tells Spring to execute this method in a separate thread.
     * It uses the "asyncExecutor" bean we configured in AsyncConfig.
     */
    @Async("asyncExecutor")
    public void sendWelcomeEmail(Employee employee) {
        logger.info("Async email started on thread: {}", Thread.currentThread().getName());
        
        String subject = "Welcome to the Employee Portal!";
        String body = String.format("Hello %s,\n\n" +
                "Welcome to the Employee Portal! We are excited to have you in the %s department.\n" +
                "Your Employee ID is: %d\n\n" +
                "Best Regards,\n" +
                "Admin Team",
                employee.getName(), employee.getDepartment(), employee.getId());

        sendEmail(employee.getEmail(), subject, body);
        
        logger.info("Async email task completed on thread: {}", Thread.currentThread().getName());
    }
}
