package ru.iopump.qa.allure.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.*;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.io.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/analysis")
public class AnalyseController {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /*  analysis/analysis.json  */
    private static final Path GLOBAL_STORAGE = Paths.get("analysis", "analysis.json");

    /*  …/allure/reports/{uuid}/widgets/analysis.json  */
    private static final Path REPORTS_ROOT   = Paths.get("allure", "reports");

    /* ---------- GLOBAL ---------- */

    @GetMapping("/latest")
    public List<AnalysisItem> latest() throws IOException {
        return readJson(GLOBAL_STORAGE);
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public void uploadJson(@RequestBody List<AnalysisItem> items) throws IOException {
        writeJson(GLOBAL_STORAGE, items);
    }

    /* ---------- REPORT-SCOPED ---------- */

    @GetMapping("/report/{uuid}")
    public List<AnalysisItem> report(@PathVariable String uuid) throws IOException {
        Path file = REPORTS_ROOT.resolve(uuid).resolve("widgets/analysis.json");
        return readJson(file);
    }

    @PostMapping(path = "/report/{uuid}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public void uploadJsonForReport(@PathVariable String uuid,
                                    @RequestBody List<AnalysisItem> items) throws IOException {
        Path dst = REPORTS_ROOT.resolve(uuid).resolve("widgets/analysis.json");
        writeJson(dst, items);
    }

    /* ---------- util ---------- */

    private List<AnalysisItem> readJson(Path file) throws IOException {
        if (Files.notExists(file)) {
            Files.createDirectories(file.getParent());
            Files.writeString(file, "[]");               // создаём пустой JSON
            return new ArrayList<>();
        }
        try (Reader r = Files.newBufferedReader(file)) {
            return MAPPER.readValue(r, new TypeReference<List<AnalysisItem>>() {});
        }
    }

    private void writeJson(Path file, List<AnalysisItem> items) throws IOException {
        Files.createDirectories(file.getParent());
        try (Writer w = Files.newBufferedWriter(file)) {
            MAPPER.writeValue(w, items);
        }
    }

    /* ---------- DTO ---------- */
    @Data @NoArgsConstructor @AllArgsConstructor
    public static class AnalysisItem {
        private String rule;
        private String message;
    }
}
