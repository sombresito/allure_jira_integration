package ru.iopump.qa.allure.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TestResultResponse {
    private String name;
    private String status;
    private String duration;
    private List<String> steps;
}
