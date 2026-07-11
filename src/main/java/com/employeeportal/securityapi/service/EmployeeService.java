package com.employeeportal.securityapi.service;

import com.employeeportal.securityapi.dto.EmployeeDTO;
import com.employeeportal.securityapi.entity.Employee;
import com.employeeportal.securityapi.repository.EmployeeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class EmployeeService {

    private static final Logger logger = LoggerFactory.getLogger(EmployeeService.class);

    private final EmployeeRepository employeeRepository;
    private final EmailService emailService;

    public EmployeeService(EmployeeRepository employeeRepository, EmailService emailService) {
        this.employeeRepository = employeeRepository;
        this.emailService = emailService;
    }

    public EmployeeDTO createEmployee(EmployeeDTO dto) {
        Employee employee = Employee.builder()
                .name(dto.getName())
                .email(dto.getEmail())
                .department(dto.getDepartment())
                .salary(dto.getSalary())
                .build();
        
        logger.info("Employee creation started on HTTP thread: {}", Thread.currentThread().getName());
        Employee saved = employeeRepository.save(employee);
        logger.info("Employee saved successfully to database.");

        // Async Email processing
        logger.info("Submitting welcome email task to Async Executor...");
        emailService.sendWelcomeEmail(saved);
        logger.info("Async email task submitted. HTTP request will now return response.");

        return mapToDTO(saved);
    }

    public List<EmployeeDTO> getAllEmployees() {
        return employeeRepository.findAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public EmployeeDTO getEmployeeById(Long id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Employee not found"));
        return mapToDTO(employee);
    }

    public EmployeeDTO updateEmployee(Long id, EmployeeDTO dto) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Employee not found"));
        
        employee.setName(dto.getName());
        employee.setEmail(dto.getEmail());
        employee.setDepartment(dto.getDepartment());
        employee.setSalary(dto.getSalary());

        Employee updated = employeeRepository.save(employee);
        return mapToDTO(updated);
    }

    public void deleteEmployee(Long id) {
        employeeRepository.deleteById(id);
    }

    private EmployeeDTO mapToDTO(Employee employee) {
        EmployeeDTO dto = new EmployeeDTO();
        dto.setId(employee.getId());
        dto.setName(employee.getName());
        dto.setEmail(employee.getEmail());
        dto.setDepartment(employee.getDepartment());
        dto.setSalary(employee.getSalary());
        return dto;
    }
}
