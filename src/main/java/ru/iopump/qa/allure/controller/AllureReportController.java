package ru.iopump.qa.allure.controller; //NOPMD

import com.google.common.base.Preconditions;
import io.qameta.allure.entity.ExecutorInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ru.iopump.qa.allure.entity.ReportEntity;
import ru.iopump.qa.allure.gui.view.ModuleSuitesResponse;
import ru.iopump.qa.allure.gui.view.ModuleTestStatsResponse;
import ru.iopump.qa.allure.model.ReportGenerateRequest;
import ru.iopump.qa.allure.model.ReportResponse;
import ru.iopump.qa.allure.properties.AllureProperties;
import ru.iopump.qa.allure.service.JpaReportService;
import ru.iopump.qa.allure.service.ResultService;
import ru.iopump.qa.allure.service.JiraService;
import ru.iopump.qa.util.StreamUtil;


import javax.servlet.http.HttpServletResponse;
import javax.validation.ConstraintViolationException;
import javax.validation.Valid;
import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;


import ru.iopump.qa.allure.model.TestResultResponse;
import static ru.iopump.qa.allure.helper.Util.url;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.server.ResponseStatusException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;


import org.springframework.core.io.Resource;
import org.springframework.core.io.PathResource;
import org.springframework.http.ResponseEntity;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;

@SuppressWarnings({"unused", "UnusedReturnValue"})
@RequiredArgsConstructor
@RestController
@Slf4j
@Validated
@RequestMapping(path = "/api/report")
public class AllureReportController {
    final static String CACHE = "reports";
    private final JpaReportService reportService;
    private final ResultService resultService;
    private final AllureProperties allureProperties;
    private final JiraService jiraService;

    public String baseUrl() {
        return url(allureProperties);
    }

    @Operation(summary = "Get generated allure reports")
    @GetMapping
    public Collection<ReportResponse> getAllReports(@RequestParam(required = false) String path) {
        return StreamUtil.stream(getAllCached())
                .filter(i -> path == null || i.getPath().startsWith(path))
                .collect(Collectors.toUnmodifiableSet());
    }

    @Cacheable(CACHE) // caching results
    public Collection<ReportResponse> getAllCached() {
        return StreamUtil.stream(reportService.getAll())
                .map(entity -> new ReportResponse(
                        entity.getUuid(),
                        entity.getPath(),
                        entity.generateUrl(baseUrl(), allureProperties.reports().dir()),
                        entity.generateLatestUrl(baseUrl(), allureProperties.reports().path())
                ))
                .collect(Collectors.toUnmodifiableList());
    }

    @Operation(summary = "Generate report")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @CacheEvict(value = {CACHE, AllureResultController.CACHE}, allEntries = true) // update results cache
    public ReportResponse generateReport(@RequestBody @Valid ReportGenerateRequest reportGenerateRequest) throws IOException {

        final ReportEntity reportEntity = reportService.generate(
                reportGenerateRequest.getReportSpec().getPathsAsPath(),
                reportGenerateRequest.getResultsAsPath(resultService.getStoragePath()),
                reportGenerateRequest.isDeleteResults(),
                reportGenerateRequest.getReportSpec().getExecutorInfo(),
                baseUrl()
        );

        String jiraIssueKey = reportGenerateRequest.getJiraIssueKey();
        if (StringUtils.isNotBlank(jiraIssueKey)) {
            Path reportDir = reportService.getReportDirectory(reportEntity.getUuid());
            String reportUrl = reportEntity.generateUrl(baseUrl(), allureProperties.reports().dir());
            jiraService.addReportComment(jiraIssueKey, reportDir, reportUrl);
        }

        return new ReportResponse(
                reportEntity.getUuid(),
                reportEntity.getPath(),
                reportEntity.generateUrl(baseUrl(), allureProperties.reports().dir()),
                reportEntity.generateLatestUrl(baseUrl(), allureProperties.reports().path())
        );
    }


    @SneakyThrows
    @Operation(summary = "Upload allure-report.zip with generated allure report files")
    @PostMapping(value = "{reportPath}", consumes = {"multipart/form-data"})
    @ResponseStatus(HttpStatus.CREATED)
    @CacheEvict(value = CACHE, allEntries = true) // update results cache
    public ReportResponse uploadReport(
            @PathVariable("reportPath") @NonNull String reportPath,
            @Parameter(description = "File as multipart body. File must be an zip archive and not be empty. Nested type is 'application/zip'",
                    name = "allureResults",
                    example = "allure-result.zip",
                    required = true,
                    content = @Content(mediaType = "application/zip")
            )
            @RequestParam MultipartFile allureReportArchive) {

        final String contentType = allureReportArchive.getContentType();

        // Check Content-Type
        if (StringUtils.isNotBlank(contentType)) {
            Preconditions.checkArgument(StringUtils.equalsAny(contentType, "application/zip", "application/x-zip-compressed"),
                    "Content-Type must be '%s' but '%s'", "application/zip", contentType);
        }

        // Check Extension
        if (allureReportArchive.getOriginalFilename() != null) {
            Preconditions.checkArgument(allureReportArchive.getOriginalFilename().endsWith(".zip"),
                    "File must have '.zip' extension but '%s'", allureReportArchive.getOriginalFilename());
        }

        // Unzip and save
        ReportEntity reportEntity = reportService
                .uploadReport(reportPath, allureReportArchive.getInputStream(), new ExecutorInfo(), baseUrl());
        log.info("File saved to file system '{}'", allureReportArchive); // где сохранен отчет

        return new ReportResponse(
                reportEntity.getUuid(),
                reportEntity.getPath(),
                reportEntity.generateUrl(baseUrl(), allureProperties.reports().dir()),
                reportEntity.generateLatestUrl(baseUrl(), allureProperties.reports().path())
        );
    }

    @Operation(summary = "Clear all history reports")
    @DeleteMapping("/history")
    @CacheEvict(value = CACHE, allEntries = true)
    public Collection<ReportResponse> deleteAllHistory() {
        return reportService.clearAllHistory().stream()
                .map(entity -> new ReportResponse(
                        entity.getUuid(),
                        entity.getPath(),
                        entity.generateUrl(baseUrl(), allureProperties.reports().dir()),
                        entity.generateLatestUrl(baseUrl(), allureProperties.reports().path())
                ))
                .collect(Collectors.toUnmodifiableList());
    }

    @Operation(summary = "Delete all reports or older than date in epoch seconds")
    @DeleteMapping
    @CacheEvict(value = CACHE, allEntries = true)
    public Collection<ReportResponse> deleteAll(@RequestParam(required = false) Long seconds) throws IOException {
        Collection<ReportEntity> deleted;
        if (seconds == null) {
            deleted = reportService.deleteAll();
        } else {
            LocalDateTime boundaryDate = LocalDateTime.ofInstant(Instant.ofEpochSecond(seconds), ZoneId.of("UTC"));
            deleted = reportService.deleteAllOlderThanDate(boundaryDate);
        }
        return deleted.stream()
                .map(entity -> new ReportResponse(
                        entity.getUuid(),
                        entity.getPath(),
                        entity.generateUrl(baseUrl(), allureProperties.reports().dir()),
                        entity.generateLatestUrl(baseUrl(), allureProperties.reports().path())
                ))
                .collect(Collectors.toUnmodifiableList());
    }

    // Получение результатов автотетсов из файла из widgets/suites.json
    @Operation(summary = "Get data from widgets/suites.json")
    @GetMapping("/{uuid}/suites")
    public Map<String, Object> getSuitesDataWithCustomSummaryOrder(@PathVariable UUID uuid) {
        // 1. Получаем отчет по UUID
        ReportEntity reportEntity = reportService.getReportByUUID(uuid);
        // 2. Получаем директорию отчета
        Path reportDir = reportService.getReportDirectory(uuid);
        // 3. Указываем путь к файлу suites.json в widgets
        Path suitesFilePath = reportDir.resolve("widgets").resolve("suites.json");
        // 4. Проверяем, существует ли файл suites.json
        if (!Files.exists(suitesFilePath)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File suites.json not found");
        }
        // 5. Читаем содержимое файла и преобразуем его в объекты
        try {
            String suitesJson = Files.readString(suitesFilePath);
            ObjectMapper objectMapper = new ObjectMapper();
            // Преобразование JSON в список объектов ModuleTestStatsResponse
            ModuleSuitesResponse suitesResponse = objectMapper.readValue(suitesJson, ModuleSuitesResponse.class);
            List<ModuleTestStatsResponse> items = suitesResponse.getItems();
            // 6. Подсчитываем общие значения
            int totalTests = 0;
            int totalFailed = 0;
            int totalBroken = 0;
            int totalSkipped = 0;
            int totalPassed = 0;
            int totalUnknown = 0;
            List<Map<String, Object>> modules = new ArrayList<>();
            for (ModuleTestStatsResponse item : items) {
                totalTests += item.getStatistic().getTotal();
                totalFailed += item.getStatistic().getFailed();
                totalBroken += item.getStatistic().getBroken();
                totalSkipped += item.getStatistic().getSkipped();
                totalPassed += item.getStatistic().getPassed();
                totalUnknown += item.getStatistic().getUnknown();
                // Преобразуем статистику для каждого модуля в более читаемый формат и правильный порядок
                Map<String, Object> moduleStats = new LinkedHashMap<>(); // Используем LinkedHashMap для сохранения порядка ключей
                moduleStats.put("uid", item.getUid());
                moduleStats.put("name", item.getName());
                moduleStats.put("статистика", createOrderedStatsMap(item.getStatistic()));
                modules.add(moduleStats);
            }
            // 7. Создаем сводный ответ с правильным порядком полей
            Map<String, Object> response = new LinkedHashMap<>();  // Используем LinkedHashMap для сохранения порядка ключей
            response.put("modules", modules);
            response.put("summary", createOrderedSummaryMap(totalBroken, totalFailed, totalPassed, totalUnknown, totalSkipped, totalTests));
            return response;
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error reading suites.json", e);
        }
    }


    @Operation(summary = "Get raw data/suites.json as-is")
    @GetMapping(path = "/{uuid}/suites/json", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Resource> getSuitesJson(@PathVariable UUID uuid) {

        // Путь к data/suites.json
        Path jsonFile = reportService.getReportDirectory(uuid)
                .resolve("data")
                .resolve("suites.json");

        if (!Files.exists(jsonFile)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File suites.json not found");
        }

        try {
            Resource resource = new PathResource(jsonFile);

            // Опционально выводим в лог полный путь — помогает отлавливать ошибку
            log.debug("Serving {}", jsonFile.toAbsolutePath());

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    // contentLength полезно для больших файлов и прогресс-баров в curl
                    .contentLength(Files.size(jsonFile))
                    .body(resource);
        } catch (IOException e) {
            // Потенциально сюда попадает любая IO-проблема
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Cannot read suites.json", e
            );
        }
    }



    // Вспомогательный метод для создания упорядоченной статистики с правильным порядком ключей для каждого модуля
    private Map<String, Object> createOrderedStatsMap(ModuleTestStatsResponse.Statistic statistic) {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("количество сломанных тестов", statistic.getBroken());
        stats.put("количество упавших тестов", statistic.getFailed());
        stats.put("количество успешно пройденных тестов", statistic.getPassed());
        stats.put("количество невыполненых тестов из-за предидущих операций", statistic.getUnknown());
        stats.put("количество пропущенных тестов", statistic.getSkipped());
        stats.put("общее количество тестов", statistic.getTotal());  // "общее количество тестов" в самом низу
        return stats;
    }

    // Вспомогательный метод для создания упорядоченной сводной статистики с правильным порядком ключей
    private Map<String, Object> createOrderedSummaryMap(int totalBroken, int totalFailed, int totalPassed, int totalUnknown, int totalSkipped, int totalTests) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("количество сломанных тестов", totalBroken);
        summary.put("количество упавших тестов", totalFailed);
        summary.put("количество успешно пройденных тестов", totalPassed);
        summary.put("количество невыполненых тестов из-за предидущих операций", totalUnknown);
        summary.put("количество пропущенных тестов", totalSkipped);
        summary.put("общее количество тестов", totalTests);  // "общее количество тестов" в самом низу
        return summary;
    }


    @Operation(summary = "Aggregate all test-cases into one JSON file")
    @GetMapping(path = "/{uuid}/test-cases/aggregate", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Resource> aggregateTestCases(@PathVariable UUID uuid) {

        // 1. Где лежит отчёт и папка с кейсами
        Path reportDir      = reportService.getReportDirectory(uuid);
        Path testCasesDir   = reportDir.resolve("data").resolve("test-cases");
        Path aggregatedPath = reportDir.resolve("data").resolve("aggregated-test-cases.json");

        if (!Files.isDirectory(testCasesDir))
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Каталог data/test-cases не найден: " + testCasesDir);

        ObjectMapper mapper = new ObjectMapper();
        ArrayNode allCases  = mapper.createArrayNode();

        // 2. Читаем каждый *.json и кладём в общий массив
        try (Stream<Path> paths = Files.list(testCasesDir)) {
            paths.filter(p -> p.toString().endsWith(".json"))
                    .forEach(p -> {
                        try {
                            JsonNode node = mapper.readTree(p.toFile());
                            allCases.add(node);
                        } catch (IOException e) {
                            throw new UncheckedIOException(
                                    "Ошибка чтения файла " + p.getFileName(), e);
                        }
                    });
        } catch (IOException e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR, "Ошибка обхода каталога test-cases", e);
        }

        // 3. Записываем результат в файл
        try {
            mapper.writerWithDefaultPrettyPrinter().writeValue(aggregatedPath.toFile(), allCases);
        } catch (IOException e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR, "Не удалось сохранить aggregated-test-cases.json", e);
        }

        // 4. Отдаём клиенту
        try {
            Resource res = new PathResource(aggregatedPath);
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .contentLength(Files.size(aggregatedPath))
                    .body(res);
        } catch (IOException e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR, "Не удалось отдать aggregated-test-cases.json", e);
        }
    }




}
