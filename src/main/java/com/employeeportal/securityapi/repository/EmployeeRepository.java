package com.employeeportal.securityapi.repository;

import com.employeeportal.securityapi.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {
}
