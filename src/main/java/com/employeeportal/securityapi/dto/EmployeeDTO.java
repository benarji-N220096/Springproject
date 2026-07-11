package com.employeeportal.securityapi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class EmployeeDTO {
    private Long id;

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Email is required")
    @jakarta.validation.constraints.Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Department is required")
    private String department;

    @Positive(message = "Salary must be positive")
    private Double salary;
}
