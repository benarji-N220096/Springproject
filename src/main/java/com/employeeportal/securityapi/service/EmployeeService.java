package com.employeeportal.securityapi.service;

import com.employeeportal.securityapi.dto.EmployeeDTO;
import com.employeeportal.securityapi.entity.Employee;
import com.employeeportal.securityapi.repository.EmployeeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class EmployeeService {

    private static final Logger logger = LoggerFactory.getLogger(EmployeeService.class);

    private final EmployeeRepository employeeRepository;
    private final EmailService emailService;
    private final FileStorageService fileStorageService;

    public EmployeeService(EmployeeRepository employeeRepository, EmailService emailService, FileStorageService fileStorageService) {
        this.employeeRepository = employeeRepository;
        this.emailService = emailService;
        this.fileStorageService = fileStorageService;
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

    @Cacheable(value = "employees", key = "#id")
    public EmployeeDTO getEmployeeById(Long id) {
        logger.info("Fetching employee with ID: {} from Database (Cache Miss if this prints)", id);
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Employee not found"));
        return mapToDTO(employee);
    }

    @Cacheable(value = "allEmployees")
    public List<EmployeeDTO> getAllEmployees() {
        logger.info("Fetching all employees from Database (Cache Miss if this prints)");
        return employeeRepository.findAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @CachePut(value = "employees", key = "#id")
    @CacheEvict(value = "allEmployees", allEntries = true)
    public EmployeeDTO updateEmployee(Long id, EmployeeDTO dto) {
        logger.info("Updating employee with ID: {} and updating Redis Cache", id);
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Employee not found"));
        
        employee.setName(dto.getName());
        employee.setEmail(dto.getEmail());
        employee.setDepartment(dto.getDepartment());
        employee.setSalary(dto.getSalary());

        Employee updated = employeeRepository.save(employee);
        return mapToDTO(updated);
    }

    @CacheEvict(value = {"employees", "allEmployees"}, allEntries = true)
    public void deleteEmployee(Long id) {
        logger.info("Deleting employee with ID: {} and evicting from Redis Cache", id);
        
        // Also delete associated files
        Employee employee = employeeRepository.findById(id).orElse(null);
        if (employee != null) {
            fileStorageService.deleteFile(employee.getProfileImageName());
            fileStorageService.deleteFile(employee.getResumeName());
            employeeRepository.deleteById(id);
        }
    }

    @CachePut(value = "employees", key = "#id")
    @CacheEvict(value = "allEmployees", allEntries = true)
    public EmployeeDTO uploadProfileImage(Long id, MultipartFile file) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        logger.info("Uploading Profile Image for Employee ID: {}", id);
        String fileName = fileStorageService.storeFile(file, "jpg,jpeg,png");
        
        // Delete old image if exists
        if (employee.getProfileImageName() != null) {
            fileStorageService.deleteFile(employee.getProfileImageName());
        }

        employee.setProfileImageName(fileName);
        employee.setProfileImageType(file.getContentType());
        // Storing basic path representation
        employee.setProfileImagePath("/employees/" + id + "/profile-image"); 
        
        Employee saved = employeeRepository.save(employee);
        logger.info("Saved Profile Image metadata to database for Employee ID: {}", id);
        return mapToDTO(saved);
    }

    @CachePut(value = "employees", key = "#id")
    @CacheEvict(value = "allEmployees", allEntries = true)
    public EmployeeDTO uploadResume(Long id, MultipartFile file) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        logger.info("Uploading Resume for Employee ID: {}", id);
        String fileName = fileStorageService.storeFile(file, "pdf");
        
        // Delete old resume if exists
        if (employee.getResumeName() != null) {
            fileStorageService.deleteFile(employee.getResumeName());
        }

        employee.setResumeName(fileName);
        employee.setResumeType(file.getContentType());
        employee.setResumePath("/employees/" + id + "/resume"); 
        
        Employee saved = employeeRepository.save(employee);
        logger.info("Saved Resume metadata to database for Employee ID: {}", id);
        return mapToDTO(saved);
    }

    private EmployeeDTO mapToDTO(Employee employee) {
        EmployeeDTO dto = new EmployeeDTO();
        dto.setId(employee.getId());
        dto.setName(employee.getName());
        dto.setEmail(employee.getEmail());
        dto.setDepartment(employee.getDepartment());
        dto.setSalary(employee.getSalary());
        dto.setProfileImageName(employee.getProfileImageName());
        dto.setResumeName(employee.getResumeName());
        return dto;
    }
}
