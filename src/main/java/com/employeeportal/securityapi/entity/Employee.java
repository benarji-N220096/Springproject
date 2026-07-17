package com.employeeportal.securityapi.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "employees")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String department;

    @Column(nullable = false)
    private Double salary;

    // Profile Image Metadata
    private String profileImageName;
    private String profileImagePath;
    private String profileImageType;

    // Resume Metadata
    private String resumeName;
    private String resumePath;
    private String resumeType;
}
