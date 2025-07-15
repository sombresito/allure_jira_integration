package ru.iopump.qa.allure.controller;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import ru.iopump.qa.allure.entity.ReportEntity;
import ru.iopump.qa.allure.repo.JpaReportRepository;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/reports")
public class ReportsController {

    private final JpaReportRepository repo;

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)   // <-- здесь
    public List<ReportEntity> list(@RequestParam(required = false) String path) {
        if (StringUtils.isBlank(path)) {
            return repo.findAll(Sort.by(Sort.Direction.DESC, "createdDateTime"));
        }
        //return repo.findByPathStartsWithIgnoreCaseOrderByCreatedDateTimeDesc(path);
        return repo.findByPathContainingIgnoreCaseOrderByCreatedDateTimeDesc(path);

    }

    @DeleteMapping("/{uuid}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID uuid) {
        repo.deleteById(uuid);
    }
}

