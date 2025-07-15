package ru.iopump.qa.allure.service; //NOPMD

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import io.qameta.allure.entity.ExecutorInfo;
import lombok.Getter;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import ru.iopump.qa.allure.entity.ReportEntity;
import ru.iopump.qa.allure.helper.AllureReportGenerator;
import ru.iopump.qa.allure.helper.OldReportsFormatConverterHelper;
import ru.iopump.qa.allure.helper.ServeRedirectHelper;
import ru.iopump.qa.allure.properties.AllureProperties;
import ru.iopump.qa.allure.properties.ReportProperties;
import ru.iopump.qa.allure.repo.JpaReportRepository;
import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import javax.transaction.Transactional;
import javax.validation.constraints.NotNull;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileTime;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.StreamSupport;
import static java.util.Optional.ofNullable;
import static org.apache.commons.io.FileUtils.deleteQuietly;
import static ru.iopump.qa.allure.gui.DateTimeResolver.zeroZone;
import static ru.iopump.qa.allure.helper.ExecutorCiPlugin.JSON_FILE_NAME;
import static ru.iopump.qa.allure.helper.Util.join;
import static ru.iopump.qa.allure.service.PathUtil.str;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@Component
@Slf4j
@Transactional
public class JpaReportService {
    private final ReportProperties reportProperties;
    @Getter
    private final Path reportsDir;
    private final AllureProperties cfg;
    private final ObjectMapper objectMapper;
    private final AllureReportGenerator reportGenerator;
    private final ServeRedirectHelper redirection;
    private final JpaReportRepository repository;
    private final ResultService reportUnzipService;
    private final AtomicBoolean init = new AtomicBoolean();

    public JpaReportService(AllureProperties cfg,
                            ObjectMapper objectMapper,
                            JpaReportRepository repository,
                            AllureReportGenerator reportGenerator,
                            ServeRedirectHelper redirection,
                            ReportProperties reportProperties
    ) {
        this.reportProperties = reportProperties;
        this.reportsDir = cfg.reports().dirPath();
        this.cfg = cfg;
        this.objectMapper = objectMapper;
        this.repository = repository;
        this.reportGenerator = reportGenerator;
        this.redirection = redirection;
        this.reportUnzipService = new ResultService(reportsDir);
    }

    @PostConstruct
    protected void initRedirection() {
        repository.findByActiveTrue().forEach(
                e -> redirection.mapRequestTo(join(cfg.reports().path(), e.getPath()), reportsDir.resolve(e.getUuid().toString()).toString())
        );
    }
    public ReportEntity getReportByUUID(UUID uuid) {
        return repository.findById(uuid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Report not found"));
    }

    public long getCount() {
        return repository.count();
    }

    // Метод для получения отчётов с учётом смещения и лимита
    public List<ReportEntity> getReports(int offset, int limit) {
        Pageable pageable = PageRequest.of(offset / limit, limit, Sort.by("createdDateTime").descending());
        return repository.findAll(pageable).getContent();
    }


    // Возвращаем путь к папке отчета по UUID
    public Path getReportDirectory(UUID uuid) {
        return reportsDir.resolve(uuid.toString());
    }

    public Collection<ReportEntity> clearAllHistory() {
        final Collection<ReportEntity> entitiesActive = repository.findByActiveTrue();
        final Collection<ReportEntity> entitiesInactive = repository.deleteByActiveFalse();
        // delete active history
        entitiesActive
                .forEach(e -> deleteQuietly(reportsDir.resolve(e.getUuid().toString()).resolve("history").toFile()));
        // delete active history
        entitiesInactive
                .forEach(e -> deleteQuietly(reportsDir.resolve(e.getUuid().toString()).toFile()));
        return entitiesInactive;
    }

    public void internalDeleteByUUID(UUID uuid) throws IOException {
        repository.deleteById(uuid);
        FileUtils.deleteDirectory(reportsDir.resolve(uuid.toString()).toFile());
    }

    public Collection<ReportEntity> deleteAll() throws IOException {
        var res = getAll();
        repository.deleteAll();
        FileUtils.deleteDirectory(reportsDir.toFile());
        return res;
    }

    public Collection<ReportEntity> deleteAllOlderThanDate(LocalDateTime date) {
        final Collection<ReportEntity> res = repository.findAllByCreatedDateTimeIsBefore(date);
        res.forEach(e -> {
            repository.deleteById(e.getUuid());
            deleteQuietly(reportsDir.resolve(e.getUuid().toString()).toFile());
        });
        return res;
    }

    public Collection<ReportEntity> getAll() {
        return repository.findAll(Sort.by("createdDateTime").descending());
    }

    @SneakyThrows
    public ReportEntity uploadReport(@NonNull String reportPath,
                                     @NonNull InputStream archiveInputStream,
                                     @Nullable ExecutorInfo executorInfo,
                                     String baseUrl) {
        // Новое место назначения и сущность отчета
        final Path destination = reportUnzipService.unzipAndStore(archiveInputStream);
        final UUID uuid = UUID.fromString(destination.getFileName().toString());
        Preconditions.checkArgument(
                Files.list(destination).anyMatch(path -> path.endsWith("index.html")),
                "Uploaded archive is not an Allure Report"
        );


        // Изменяем формат даты в файле index.html, так как пути могут быть разными
        Path indexHtml = destination.resolve("index.html");
        ru.iopump.qa.allure.service.HtmlDateFormatter.reformatDateInHtml(indexHtml);

        // Создать новый рабочий файл из выделения
        final Optional<ReportEntity> prevEntity = repository.findByPathOrderByCreatedDateTimeDesc(reportPath)
                .stream()
                .findFirst();
        // Добавить информацию об исполнителе CI
        var safeExecutorInfo = addExecutionInfo(
                destination,
                executorInfo,
                baseUrl + str(reportsDir.resolve(uuid.toString())) + "/index.html",
                uuid
        );

        log.info("Report '{}' loaded", destination); // лог о загрузке репорта

        // Новый объект отчета
        final ReportEntity newEntity = ReportEntity.builder()
                .uuid(uuid)
                .path(reportPath)
                .createdDateTime(LocalDateTime.now(zeroZone()))
                .url(join(baseUrl, cfg.reports().dir(), uuid.toString()) + "/")
                .level(prevEntity.map(e -> e.getLevel() + 1).orElse(0L))
                .active(true)
                .size(ReportEntity.sizeKB(destination))
                .buildUrl(
                        // Взять Build Url
                        ofNullable(safeExecutorInfo.getBuildUrl())
                                // Or Build Name
                                .or(() -> ofNullable(safeExecutorInfo.getBuildName()))
                                // Or Executor Name
                                .or(() -> ofNullable(safeExecutorInfo.getName()))
                                // Or Executor Type
                                .orElse(safeExecutorInfo.getType())
                )
                .build();

        // Добавить сопоставление запросов
        redirection.mapRequestTo(newEntity.getPath(), reportsDir.resolve(uuid.toString()).toString());
        // Persist
        handleMaxHistory(newEntity);
        repository.saveAndFlush(newEntity);
        // Отключить предыдущий отчет
        prevEntity.ifPresent(e -> e.setActive(false));

        // Определить в какой отчет добавлять данные
        boolean isTestReport = containsKeyword(reportPath, destination, "test");
        boolean isDevReport = containsKeyword(reportPath, destination, "dev");
        boolean isPreprodReport = containsKeyword(reportPath, destination, "preprod");
        boolean isETEReport = containsKeyword(reportPath, destination, "E2E");
        boolean isColvirReport = containsKeyword(reportPath, destination, "Colvir");

        if (isTestReport) {
            integrateNewReportIntoFullReport(destination, reportsDir.resolve(reportProperties.getYourTestReportProperty()));
            updateSummaryReport(reportsDir.resolve(reportProperties.getYourTestReportProperty()), "Test окружения");
        }
        if (isDevReport) {
            integrateNewReportIntoFullReport(destination, reportsDir.resolve(reportProperties.getYourDevReportProperty()));
            updateSummaryReport(reportsDir.resolve(reportProperties.getYourDevReportProperty()), "Dev окружения");
        }
        if (isPreprodReport) {
            integrateNewReportIntoFullReport(destination, reportsDir.resolve(reportProperties.getYourPreprodReportProperty()));
            updateSummaryReport(reportsDir.resolve(reportProperties.getYourPreprodReportProperty()), "Preprod окружения");
        }
        if (isETEReport) {
            integrateNewReportIntoFullReport(destination, reportsDir.resolve(reportProperties.getYourETEReportProperty()));
            updateSummaryReport(reportsDir.resolve(reportProperties.getYourETEReportProperty()), "E2E тестирование");
        }
        if (isColvirReport) {
            integrateNewReportIntoFullReport(destination, reportsDir.resolve(reportProperties.getYourColvirReportProperty()));
            updateSummaryReport(reportsDir.resolve(reportProperties.getYourColvirReportProperty()), "Автоматизация Colvir");
        }
        integrateNewReportIntoFullReport(destination, reportsDir.resolve(reportProperties.getYourProperty()));
        updateSummaryReport(reportsDir.resolve(reportProperties.getYourProperty()), "все окружения");

        integrateNewReportIntoFullReport(destination, reportsDir.resolve(reportProperties.getYourProperty()));
        String refreshMetaTag = "<meta http-equiv=\"refresh\" content=\"600\" id=\"90\">";
        Path mainReportIndexHtml = reportsDir.resolve(reportProperties.getYourProperty()).resolve("index.html");
        Path mainReportTestIndexHtml = reportsDir.resolve(reportProperties.getYourTestReportProperty()).resolve("index.html");
        Path mainReportDevIndexHtml = reportsDir.resolve(reportProperties.getYourDevReportProperty()).resolve("index.html");
        Path mainReportPreprodIndexHtml = reportsDir.resolve(reportProperties.getYourPreprodReportProperty()).resolve("index.html");
        Path mainReportETEIndexHtml = reportsDir.resolve(reportProperties.getYourETEReportProperty()).resolve("index.html");
        Path mainReportColvirIndexHtml = reportsDir.resolve(reportProperties.getYourColvirReportProperty()).resolve("index.html");
        addRefreshMetaTagIfMissing(mainReportIndexHtml, refreshMetaTag);
        addRefreshMetaTagIfMissing(mainReportTestIndexHtml, refreshMetaTag);
        addRefreshMetaTagIfMissing(mainReportDevIndexHtml, refreshMetaTag);
        addRefreshMetaTagIfMissing(mainReportPreprodIndexHtml, refreshMetaTag);
        addRefreshMetaTagIfMissing(mainReportETEIndexHtml, refreshMetaTag);
        addRefreshMetaTagIfMissing(mainReportColvirIndexHtml, refreshMetaTag);
        addReportTypeToTitle(mainReportIndexHtml, "Allure Report All", "All");
        addReportTypeToTitle(mainReportTestIndexHtml, "Allure Report Test", "Test");
        addReportTypeToTitle(mainReportDevIndexHtml, "Allure Report Dev", "Dev");
        addReportTypeToTitle(mainReportPreprodIndexHtml, "Allure Report Preprod", "Preprod");
        addReportTypeToTitle(mainReportETEIndexHtml, "Allure Report E2E", "E2E");
        addReportTypeToTitle(mainReportColvirIndexHtml, "Allure Report Colvir", "Colvir");

        return newEntity;
    }

    public ReportEntity generate(@NonNull String reportPath,
                                 @NonNull List<Path> resultDirs,
                                 boolean clearResults,
                                 @Nullable ExecutorInfo executorInfo,
                                 String baseUrl
    ) throws IOException {
        if (cfg.supportOldFormat() && init.compareAndSet(false, true)) {
            var old = new OldReportsFormatConverterHelper(cfg).convertOldFormat();
            repository.saveAll(old);
            old.forEach(e -> redirection.mapRequestTo(e.getPath(), reportsDir.resolve(e.getUuid().toString()).toString()));
        }
        // Предварительные условия
        Preconditions.checkArgument(!resultDirs.isEmpty());
        resultDirs.forEach(i -> Preconditions.checkArgument(Files.exists(i), "Result '%s' doesn't exist", i));
        // Новое место назначения и сущность отчета
        final UUID uuid = UUID.randomUUID();
        // Найти предыдущий отчет, если он есть
        final Optional<ReportEntity> prevEntity = repository.findByPathOrderByCreatedDateTimeDesc(reportPath)
                .stream()
                .findFirst();
        // Новый каталог uuid
        final Path destination = reportsDir.resolve(uuid.toString());
        // Копировать историю из предыдущего отчета
        final Optional<Path> historyO = prevEntity
                .flatMap(e -> copyHistory(reportsDir.resolve(e.getUuid().toString()), uuid.toString()))
                .or(Optional::empty);
        // Добавить информацию об исполнителе CI
        var safeExecutorInfo = addExecutionInfo(
                resultDirs.get(0),
                executorInfo,
                baseUrl + str(reportsDir.resolve(uuid.toString())) + "/index.html",
                uuid
        );
        try {
            // Добавить историю в результаты, если она существует
            final List<Path> resultDirsToGenerate = historyO
                    .map(history -> (List<Path>) ImmutableList.<Path>builder().addAll(resultDirs).add(history).build())
                    .orElse(resultDirs);
            copyAllureProperties(resultDirsToGenerate);
            // Создать новый отчет с историей
            reportGenerator.generate(destination, resultDirsToGenerate);
            log.info("Report '{}' generated according to results '{}'", destination, resultDirsToGenerate);
        } finally {
            // Удалить историю tmp
            historyO.ifPresent(h -> deleteQuietly(h.toFile()));
            if (clearResults) {
                resultDirs.forEach(r -> deleteQuietly(r.toFile()));
            }
        }
        // Новый объект отчета
        final ReportEntity newEntity = ReportEntity.builder()
                .uuid(uuid)
                .path(reportPath)
                .createdDateTime(LocalDateTime.now(zeroZone()))
                .url(join(baseUrl, cfg.reports().dir(), uuid.toString()) + "/")
                .level(prevEntity.map(e -> e.getLevel() + 1).orElse(0L))
                .active(true)
                .size(ReportEntity.sizeKB(destination))
                .buildUrl(
                        // Взять Build Url
                        ofNullable(safeExecutorInfo.getBuildUrl())
                                // Или название сборки
                                .or(() -> ofNullable(safeExecutorInfo.getBuildName()))
                                // Или имя исполнителя
                                .or(() -> ofNullable(safeExecutorInfo.getName()))
                                // Или тип исполнителя
                                .orElse(safeExecutorInfo.getType())
                )
                .build();
        // Добавить сопоставление запросов
        redirection.mapRequestTo(newEntity.getPath(), reportsDir.resolve(uuid.toString()).toString());
        // Persist
        handleMaxHistory(newEntity);
        repository.saveAndFlush(newEntity);
        // Отключить предыдущий отчет
        prevEntity.ifPresent(e -> e.setActive(false));

        boolean isTestReport = containsKeyword(reportPath, destination, "test");
        boolean isDevReport = containsKeyword(reportPath, destination, "dev");
        boolean isPreprodReport = containsKeyword(reportPath, destination, "preprod");
        boolean isETEReport = containsKeyword(reportPath, destination, "E2E");
        boolean isColvirReport = containsKeyword(reportPath, destination, "Colvir");

        if (isTestReport) {
            integrateNewReportIntoFullReport(destination, reportsDir.resolve(reportProperties.getYourTestReportProperty()));
            updateSummaryReport(reportsDir.resolve(reportProperties.getYourTestReportProperty()), "Test окружения");
        }
        if (isDevReport) {
            integrateNewReportIntoFullReport(destination, reportsDir.resolve(reportProperties.getYourDevReportProperty()));
            updateSummaryReport(reportsDir.resolve(reportProperties.getYourDevReportProperty()), "Dev окружения");
        }
        if (isPreprodReport) {
            integrateNewReportIntoFullReport(destination, reportsDir.resolve(reportProperties.getYourPreprodReportProperty()));
            updateSummaryReport(reportsDir.resolve(reportProperties.getYourPreprodReportProperty()), "Preprod окружения");
        }
        if (isETEReport) {
            integrateNewReportIntoFullReport(destination, reportsDir.resolve(reportProperties.getYourETEReportProperty()));
            updateSummaryReport(reportsDir.resolve(reportProperties.getYourETEReportProperty()), "E2E тестирование");
        }
        if (isColvirReport) {
            integrateNewReportIntoFullReport(destination, reportsDir.resolve(reportProperties.getYourColvirReportProperty()));
            updateSummaryReport(reportsDir.resolve(reportProperties.getYourColvirReportProperty()), "Автоматизация Colvir");
        }
        integrateNewReportIntoFullReport(destination, reportsDir.resolve(reportProperties.getYourProperty()));
        updateSummaryReport(reportsDir.resolve(reportProperties.getYourProperty()), "все окружения");

        integrateNewReportIntoFullReport(destination, reportsDir.resolve(reportProperties.getYourProperty()));
        String refreshMetaTag = "<meta http-equiv=\"refresh\" content=\"600\" id=\"90\">";
        Path mainReportIndexHtml = reportsDir.resolve(reportProperties.getYourProperty()).resolve("index.html");
        Path mainReportTestIndexHtml = reportsDir.resolve(reportProperties.getYourTestReportProperty()).resolve("index.html");
        Path mainReportDevIndexHtml = reportsDir.resolve(reportProperties.getYourDevReportProperty()).resolve("index.html");
        Path mainReportPreprodIndexHtml = reportsDir.resolve(reportProperties.getYourPreprodReportProperty()).resolve("index.html");
        Path mainReportETEIndexHtml = reportsDir.resolve(reportProperties.getYourETEReportProperty()).resolve("index.html");
        Path mainReportColvirIndexHtml = reportsDir.resolve(reportProperties.getYourColvirReportProperty()).resolve("index.html");
        addRefreshMetaTagIfMissing(mainReportIndexHtml, refreshMetaTag);
        addRefreshMetaTagIfMissing(mainReportTestIndexHtml, refreshMetaTag);
        addRefreshMetaTagIfMissing(mainReportDevIndexHtml, refreshMetaTag);
        addRefreshMetaTagIfMissing(mainReportPreprodIndexHtml, refreshMetaTag);
        addRefreshMetaTagIfMissing(mainReportETEIndexHtml, refreshMetaTag);
        addRefreshMetaTagIfMissing(mainReportColvirIndexHtml, refreshMetaTag);
        addReportTypeToTitle(mainReportIndexHtml, "Allure Report All", "All");
        addReportTypeToTitle(mainReportTestIndexHtml, "Allure Report Test", "Test");
        addReportTypeToTitle(mainReportDevIndexHtml, "Allure Report Dev", "Dev");
        addReportTypeToTitle(mainReportPreprodIndexHtml, "Allure Report Preprod", "Preprod");
        addReportTypeToTitle(mainReportETEIndexHtml, "Allure Report E2E", "E2E");
        addReportTypeToTitle(mainReportColvirIndexHtml, "Allure Report Colvir", "Colvir");


        return newEntity;
    }

    ///// PRIVATE /////
    private void handleMaxHistory(ReportEntity created) {
        var max = cfg.reports().historyLevel();
        if (created.getLevel() >= max) { // Проверьте количество отчетов в истории
            // Получить все отсортированные отчеты
            var allReports = repository.findByPathOrderByCreatedDateTimeDesc(created.getPath());
            // Если размер больше максимальной истории
            if (allReports.size() >= max) {
                log.info("Current report count '{}' exceed max history report count '{}'",
                        allReports.size(),
                        max
                );
                // Удалить последнюю после максимальной истории
                long deleted = allReports.stream()
                        .skip(max)
                        .peek(e -> log.info("Report '{}' will be deleted", e))
                        .peek(e -> deleteQuietly(reportsDir.resolve(e.getUuid().toString()).toFile()))
                        .peek(repository::delete)
                        .count();
                // Уровень обновления (безопасность)
                created.setLevel(Math.max(created.getLevel() - deleted, 0));
            }
        }
    }

    @SneakyThrows
    private Optional<Path> copyHistory(Path reportPath, String prevReportWithHistoryUuid) {
        // Каталог истории в каталоге отчета
        final Path sourceHistory = reportPath.resolve("history");
        // Если каталог истории существует
        if (Files.exists(sourceHistory) && Files.isDirectory(sourceHistory)) {
            // Создать каталог истории tmp
            final Path tmpHistory = reportsDir.resolve("history").resolve(prevReportWithHistoryUuid);
            FileUtils.moveDirectoryToDirectory(sourceHistory.toFile(), tmpHistory.toFile(), true);
            log.info("Report '{}' history is '{}'", reportPath, tmpHistory);
            return Optional.of(tmpHistory);
        } else {
            // Или ничего
            return Optional.empty();
        }
    }

    @NotNull
    private ExecutorInfo addExecutionInfo(Path resultPathWithInfo,
                                          ExecutorInfo executor,
                                          String reportUrl,
                                          UUID uuid) throws IOException {
        var executorInfo = ofNullable(executor).orElse(new ExecutorInfo());
        executorInfo.setReportUrl(reportUrl);
        LocalDateTime currentDateTime = LocalDateTime.now();
        LocalDateTime newDateTime = currentDateTime.plusHours(6);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
        newDateTime.format(formatter);
        if (StringUtils.isBlank(executorInfo.getName())) {
            executorInfo.setName("Remote executor");
        }
        if (StringUtils.isBlank(executorInfo.getType())) {
            executorInfo.setType("CI");
        }
        if (StringUtils.isBlank(executorInfo.getReportName())) {
            executorInfo.setName("Allure server generated " + LocalDateTime.now().plusHours(6).format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss")));
        }
        if (StringUtils.isBlank(executorInfo.getReportName())) {
            executorInfo.setReportName(uuid.toString());
        }
        final ObjectWriter writer = objectMapper.writer(new DefaultPrettyPrinter());
        final Path executorPath = resultPathWithInfo.resolve(JSON_FILE_NAME);
        writer.writeValue(executorPath.toFile(), executorInfo);
        log.info("Executor information added to '{}' : {}", executorPath, executorInfo); // лог информации url
        return executorInfo;
    }

    private void mergeReports(Path sourceDir, Path targetDir) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        boolean isTestReport = targetDir.toString().toLowerCase().contains("test");
        boolean isDevReport = targetDir.toString().toLowerCase().contains("dev");
        boolean isPreprodReport = targetDir.toString().toLowerCase().contains("preprod");
        boolean isETEReport = targetDir.toString().toLowerCase().contains("E2E");


        /// Объявление переменной для хранения количества совпадающих строк
        AtomicInteger countMatchingRows = new AtomicInteger(0);
        AtomicInteger countFailed = new AtomicInteger(0);
        AtomicInteger countBroken = new AtomicInteger(0);
        AtomicInteger countSkipped = new AtomicInteger(0);
        AtomicInteger countPassed = new AtomicInteger(0);
        AtomicInteger countUnknown = new AtomicInteger(0);

        Files.walk(sourceDir).forEach(sourcePath -> {
            Path targetPath = targetDir.resolve(sourceDir.relativize(sourcePath));
            try {
                if (Files.isDirectory(sourcePath)) {
                    if (!Files.exists(targetPath)) {
                        Files.createDirectories(targetPath);
                    }
                } else {
                    String grandParentDirName = sourcePath.getParent().getParent().getFileName().toString();
                    String parentDirName = sourcePath.getParent().getFileName().toString();
                    String fileName = sourcePath.getFileName().toString();
                    if (("data".equals(grandParentDirName) && ("attachments".equals(parentDirName) || "test-cases".equals(parentDirName))) || "history".equals(parentDirName) || "widgets".equals(parentDirName) || "data".equals(parentDirName)) {
                        if (!Files.exists(targetPath)) {
                            Files.copy(sourcePath, targetPath);
                        } else {
                            String sourceJson = new String(Files.readAllBytes(sourcePath), StandardCharsets.UTF_8);
                            try {
                                JsonNode sourceNode = objectMapper.readTree(sourceJson);
                                if ("suites.csv".equals(fileName) && "data".equals(parentDirName)) {
                                    try {
                                        if (!Files.exists(targetPath)) {
                                            Files.copy(sourcePath, targetPath);
                                        } else {
                                            FileTime fileTime = Files.getLastModifiedTime(targetPath);
                                            LocalDate lastModifiedDate = fileTime.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                                            LocalDate todayDate = LocalDate.now();

                                            boolean fileWasClearedOrEmpty = false;

                                            if (lastModifiedDate.equals(todayDate)) {
                                                // Чтение файла основного отчета
                                                String existingReportCsv = Files.readString(targetPath);
                                                if (!existingReportCsv.isEmpty()) {
                                                    String[] existingLines = existingReportCsv.split("\\r?\\n");
                                                    List<String> updatedReportLines = new ArrayList<>(Arrays.asList(existingLines));

                                                    // Чтение нового отчета
                                                    String newReportCsv = Files.readString(sourcePath);
                                                    if (!newReportCsv.isEmpty()) {
                                                        newReportCsv = newReportCsv.replaceAll("^\"Status\".*(?:\\r?\\n|\\r)", "");
                                                        String[] newLines = newReportCsv.split("\\r?\\n");

                                                        // Проверка совпадений в колонке "Suite" и удаление совпадающих строк
                                                        Set<String> newSuites = new HashSet<>();
                                                        for (String newLine : newLines) {
                                                            String[] newColumns = newLine.split("\",\"");
                                                            String newSuite = newColumns[5].replace("\"", ""); // Колонка "Suite" - шестая колонка
                                                            newSuites.add(newSuite);
                                                        }

                                                        updatedReportLines.removeIf(existingLine -> {
                                                            String[] existingColumns = existingLine.split("\",\"");
                                                            String existingSuite = existingColumns[5].replace("\"", ""); // Колонка "Suite" - шестая колонка
                                                            if (newSuites.contains(existingSuite)) {
                                                                countMatchingRows.incrementAndGet();
                                                                return true;
                                                            }
                                                            return false;
                                                        });

                                                        // Запись новых строк
                                                        updatedReportLines.addAll(Arrays.asList(newLines));

                                                        // Обновление основного отчета
                                                        Files.writeString(targetPath, String.join("\n", updatedReportLines), StandardOpenOption.TRUNCATE_EXISTING);

                                                        // Подсчет статусов в обновленном отчете
                                                        for (String line : updatedReportLines) {
                                                            String[] columns = line.split("\",\"");
                                                            String status = columns[0].replace("\"", "").toLowerCase();
                                                            switch (status) {
                                                                case "failed":
                                                                    countFailed.incrementAndGet();
                                                                    break;
                                                                case "broken":
                                                                    countBroken.incrementAndGet();
                                                                    break;
                                                                case "skipped":
                                                                    countSkipped.incrementAndGet();
                                                                    break;
                                                                case "passed":
                                                                    countPassed.incrementAndGet();
                                                                    break;
                                                                case "unknown":
                                                                    countUnknown.incrementAndGet();
                                                                    break;
                                                            }
                                                        }

                                                        System.out.println("Number of matching rows: " + countMatchingRows.get());
                                                        System.out.println("Failed: " + countFailed.get());
                                                        System.out.println("Broken: " + countBroken.get());
                                                        System.out.println("Skipped: " + countSkipped.get());
                                                        System.out.println("Passed: " + countPassed.get());
                                                        System.out.println("Unknown: " + countUnknown.get());
                                                    }
                                                }
                                            } else {
                                                Files.write(targetPath, new byte[0], StandardOpenOption.TRUNCATE_EXISTING);
                                                fileWasClearedOrEmpty = true;
                                            }

                                            if (fileWasClearedOrEmpty || Files.size(targetPath) == 0) {
                                                Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
                                            }
                                        }
                                    } catch (IOException e) {
                                        // Используйте здесь более продвинутый механизм логирования
                                        e.printStackTrace();
                                    }
                                }
                                if ("behaviors.json".equals(fileName) && "widgets".equals(parentDirName)) {
                                    try {
                                        FileTime fileTime = Files.getLastModifiedTime(targetPath);
                                        LocalDate lastModifiedDate = fileTime.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                                        LocalDate todayDate = LocalDate.now();
                                        // Проверяем, был ли файл изменен сегодня
                                        boolean fileWasClearedOrEmpty = false;
                                        if (!lastModifiedDate.equals(todayDate)) {
                                            // Если файл был изменен не сегодня, очищаем его содержимое
                                            Files.write(targetPath, new byte[0], StandardOpenOption.TRUNCATE_EXISTING);
                                            fileWasClearedOrEmpty = true;
                                        }
                                        // Чтение данных из нового файла
                                        String newReportJsonStr = new String(Files.readAllBytes(sourcePath), StandardCharsets.UTF_8);
                                        JsonNode newReportJsonNode = objectMapper.readTree(newReportJsonStr);
                                        if (!Files.exists(targetPath)) {
                                            // Если целевого файла нет, просто копируем новый
                                            if (!newReportJsonNode.isEmpty()) {
                                                Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
                                            }
                                        } else {
                                            // Если файл существует, считываем его содержимое
                                            String targetJsonStr = new String(Files.readAllBytes(targetPath), StandardCharsets.UTF_8);
                                            JsonNode targetJsonNode = objectMapper.readTree(targetJsonStr);
                                            // Проверяем, пуст ли файл после очистки или вообще пуст
                                            fileWasClearedOrEmpty = fileWasClearedOrEmpty || targetJsonStr.isEmpty() || targetJsonStr.equals("{}");
                                            if (fileWasClearedOrEmpty) {
                                                // Если файл был пуст, просто используем новые данные
                                                if (!newReportJsonNode.isEmpty()) {
                                                    Files.write(targetPath, newReportJsonStr.getBytes(StandardCharsets.UTF_8), StandardOpenOption.TRUNCATE_EXISTING);
                                                }
                                            } else {
                                                // Продолжаем с обработкой данных
                                                if (targetJsonNode.isEmpty() && !newReportJsonNode.isEmpty()) {
                                                    // Если основной файл пустой, но новый файл содержит данные, записываем новые данные
                                                    Files.write(targetPath, newReportJsonStr.getBytes(StandardCharsets.UTF_8));
                                                } else if (!newReportJsonNode.isEmpty()) {
                                                    // Если оба файла содержат данные
                                                    int oldTotal = targetJsonNode.get("total").asInt();
                                                    int newTotal = newReportJsonNode.get("total").asInt();
                                                    int adjustedOldTotal = oldTotal - countMatchingRows.get();
                                                    int totalSum = adjustedOldTotal + newTotal;
                                                    ((ObjectNode) targetJsonNode).put("total", totalSum);
                                                    // Перезаписываем основной файл с обновленными данными
                                                    Files.write(targetPath, objectMapper.writeValueAsBytes(targetJsonNode));
                                                }
                                            }
                                        }
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }
                                if ("suites.json".equals(fileName) && "data".equals(parentDirName)) {
                                    processFile(sourcePath, targetPath, "suites");
                                }
                                if ("packages.json".equals(fileName) && "data".equals(parentDirName)) {
                                    processFile(sourcePath, targetPath, "packages");
                                }
                                if ("timeline.json".equals(fileName) && "data".equals(parentDirName)) {
                                    processFile(sourcePath, targetPath, "timeline");
                                }
                                if ("behaviors.json".equals(fileName) && "data".equals(parentDirName)) {
                                    processFile(sourcePath, targetPath, "behaviors");
                                }
                                if ("categories.json".equals(fileName) && "data".equals(parentDirName)) {
                                    processFile(sourcePath, targetPath, "categories");
                                }
                                if ("suites.json".equals(fileName) && "widgets".equals(parentDirName)) {
                                    try {
                                        FileTime fileTime = Files.getLastModifiedTime(targetPath);
                                        LocalDate lastModifiedDate = fileTime.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                                        LocalDate todayDate = LocalDate.now();
                                        boolean fileWasClearedOrEmpty = false;

                                        if (!lastModifiedDate.equals(todayDate)) {
                                            Files.write(targetPath, new byte[0], StandardOpenOption.TRUNCATE_EXISTING);
                                            fileWasClearedOrEmpty = true;
                                        }

                                        String newReportJsonSuit = new String(Files.readAllBytes(sourcePath), StandardCharsets.UTF_8);
                                        JsonNode newReportJsonNode = objectMapper.readTree(newReportJsonSuit);

                                        if (!newReportJsonNode.isEmpty()) {
                                            if (Files.exists(targetPath)) {
                                                String targetJsonStr = new String(Files.readAllBytes(targetPath), StandardCharsets.UTF_8);
                                                JsonNode targetJsonNode = objectMapper.readTree(targetJsonStr);
                                                fileWasClearedOrEmpty = fileWasClearedOrEmpty || targetJsonStr.isEmpty() || targetJsonStr.equals("{}");

                                                if (fileWasClearedOrEmpty) {
                                                    Files.write(targetPath, newReportJsonSuit.getBytes(StandardCharsets.UTF_8), StandardOpenOption.TRUNCATE_EXISTING);
                                                } else {
                                                    int countMatchingRowsSuites = 0;
                                                    boolean hasMatchingRows = false;
                                                    List<Integer> indicesToRemove = new ArrayList<>();

                                                    if (targetJsonNode.has("total") && newReportJsonNode.has("total")) {
                                                        // Проверка совпадений по ключу "name" и удаление совпадающих частей
                                                        for (JsonNode newModule : newReportJsonNode.path("items")) {
                                                            String newName = newModule.get("name").asText();
                                                            ArrayNode itemsArray = (ArrayNode) targetJsonNode.get("items");
                                                            for (int i = 0; i < itemsArray.size(); i++) {
                                                                JsonNode existingModule = itemsArray.get(i);
                                                                String existingName = existingModule.get("name").asText();
                                                                if (newName.equals(existingName)) {
                                                                    countMatchingRowsSuites++;
                                                                    indicesToRemove.add(i);
                                                                    hasMatchingRows = true;
                                                                }
                                                            }
                                                        }

                                                        // Удаление совпадающих элементов
                                                        if (hasMatchingRows) {
                                                            ArrayNode itemsArray = (ArrayNode) targetJsonNode.get("items");
                                                            for (int i = indicesToRemove.size() - 1; i >= 0; i--) {
                                                                itemsArray.remove(indicesToRemove.get(i).intValue());
                                                            }

                                                            // Обновление total
                                                            int oldTotal = targetJsonNode.get("total").asInt();
                                                            int adjustedOldTotal = oldTotal - countMatchingRowsSuites;
                                                            if (adjustedOldTotal < 0) adjustedOldTotal = 0;
                                                            ((ObjectNode) targetJsonNode).put("total", adjustedOldTotal);

                                                            // Запись обновленного JSON обратно в файл без совпадающих строк
                                                            Files.write(targetPath, objectMapper.writeValueAsBytes(targetJsonNode), StandardOpenOption.TRUNCATE_EXISTING);

                                                            // Добавление новых данных
                                                            ((ArrayNode) targetJsonNode.path("items")).addAll((ArrayNode) newReportJsonNode.path("items"));

                                                            // Обновление total с учетом новых данных
                                                            int newTotal = newReportJsonNode.get("total").asInt();
                                                            int totalSum = targetJsonNode.get("total").asInt() + newTotal;
                                                            ((ObjectNode) targetJsonNode).put("total", totalSum);

                                                            // Запись обновленного JSON обратно в файл с новыми данными
                                                            Files.write(targetPath, objectMapper.writeValueAsBytes(targetJsonNode), StandardOpenOption.TRUNCATE_EXISTING);
                                                        } else {
                                                            // Если совпадений нет, просто записываем новые данные
                                                            ((ArrayNode) targetJsonNode.path("items")).addAll((ArrayNode) newReportJsonNode.path("items"));

                                                            // Обновление total с учетом новых данных
                                                            int newTotal = newReportJsonNode.get("total").asInt();
                                                            int totalSum = targetJsonNode.get("total").asInt() + newTotal;
                                                            ((ObjectNode) targetJsonNode).put("total", totalSum);

                                                            // Запись обновленного JSON обратно в файл с новыми данными
                                                            Files.write(targetPath, objectMapper.writeValueAsBytes(targetJsonNode), StandardOpenOption.TRUNCATE_EXISTING);
                                                        }
                                                    } else {
                                                        // Если total равно 0, не добавляем данные
                                                        return;
                                                    }
                                                }
                                            } else {
                                                Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
                                            }
                                        }
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }
                                if ("categories.json".equals(fileName) && "widgets".equals(parentDirName)) {
                                    try {
                                        FileTime fileTime = Files.getLastModifiedTime(targetPath);
                                        LocalDate lastModifiedDate = fileTime.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                                        LocalDate todayDate = LocalDate.now();
                                        // Переменная для отслеживания, был ли файл только что очищен или он вообще пуст
                                        boolean fileWasClearedOrEmpty = false;
                                        // Сравнение даты последнего изменения с текущей датой
                                        if (!lastModifiedDate.equals(todayDate)) {
                                            // Если файл был изменен не сегодня, очищаем его содержимое
                                            Files.write(targetPath, new byte[0], StandardOpenOption.TRUNCATE_EXISTING);
                                            fileWasClearedOrEmpty = true;
                                        }
                                        JsonNode sourceNodeCat = objectMapper.readTree(Files.newInputStream(sourcePath));
                                        if (!Files.exists(targetPath) || fileWasClearedOrEmpty) {
                                            Files.write(targetPath, objectMapper.writeValueAsBytes(sourceNodeCat));
                                        } else {
                                            JsonNode targetNode = objectMapper.readTree(Files.newInputStream(targetPath));
                                            if (targetNode.isEmpty()) {
                                                Files.write(targetPath, objectMapper.writeValueAsBytes(sourceNodeCat));
                                            } else {
                                                // Суммирование общих значений
                                                int totalSum = targetNode.get("total").asInt() + sourceNodeCat.get("total").asInt();
                                                ((ObjectNode) targetNode).put("total", totalSum);
                                                // Объединение и суммирование значений для каждого статуса
                                                JsonNode itemsNode = targetNode.path("items");
                                                if (itemsNode.isMissingNode() || !itemsNode.isArray()) {
                                                    // Если секция items отсутствует, просто копируем из нового отчета
                                                    ((ObjectNode) targetNode).set("items", sourceNodeCat.path("items"));
                                                } else {
                                                    // Если секция items существует, объединяем данные
                                                    for (JsonNode sourceItem : sourceNodeCat.path("items")) {
                                                        Optional<JsonNode> matchingItem = StreamSupport.stream(itemsNode.spliterator(), false)
                                                                .filter(item -> item.get("uid").asText().equals(sourceItem.get("uid").asText()))
                                                                .findFirst();
                                                        if (matchingItem.isPresent()) {
                                                            JsonNode item = matchingItem.get();
                                                            for (String key : Arrays.asList("failed", "broken", "skipped", "passed", "unknown")) {
                                                                int sum = item.get("statistic").get(key).asInt() + sourceItem.get("statistic").get(key).asInt();
                                                                ((ObjectNode) item.get("statistic")).put(key, sum);
                                                            }
                                                        } else {
                                                            // Если нет совпадающего элемента, добавляем новый
                                                            ((ArrayNode) itemsNode).add(sourceItem);
                                                        }
                                                    }
                                                }
                                                Files.write(targetPath, objectMapper.writeValueAsBytes(targetNode));
                                            }
                                        }
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }
                                if ("summary.json".equals(fileName) && "widgets".equals(parentDirName)) {
                                    if (!Files.exists(targetPath)) {
                                        Files.copy(sourcePath, targetPath);
                                    } else {
                                        FileTime fileTime = Files.getLastModifiedTime(targetPath);
                                        LocalDate lastModifiedDate = fileTime.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                                        LocalDate todayDate = LocalDate.now();

                                        String newReportJsonStr = new String(Files.readAllBytes(sourcePath), StandardCharsets.UTF_8);
                                        String existingReportJsonStr = new String(Files.readAllBytes(targetPath), StandardCharsets.UTF_8);

                                        ObjectNode newReportJson = (ObjectNode) objectMapper.readTree(newReportJsonStr);
                                        ObjectNode existingReportJson = (ObjectNode) objectMapper.readTree(existingReportJsonStr);

                                        if (!lastModifiedDate.equals(todayDate)) {
                                            // Файл не был изменен сегодня, очищаем его и добавляем новые данные
                                            Files.write(targetPath, newReportJsonStr.getBytes(StandardCharsets.UTF_8), StandardOpenOption.TRUNCATE_EXISTING);
                                        } else {
                                            // Файл был изменен сегодня, заменяем значения total и passed
                                            if (!existingReportJsonStr.isEmpty()) {
                                                // Копирование данных для ключей "time" и "start" без суммирования
                                                if (newReportJson.has("time")) {
                                                    JsonNode newTime = newReportJson.get("time");
                                                    ((ObjectNode) existingReportJson).set("time", newTime);
                                                }

                                                // Замена значений статистики
                                                if (existingReportJson.has("statistic")) {
                                                    ObjectNode existingStatistic = (ObjectNode) existingReportJson.get("statistic");

                                                    // Обновление значений статистики
                                                    existingStatistic.put("failed", countFailed.get());
                                                    existingStatistic.put("broken", countBroken.get());
                                                    existingStatistic.put("skipped", countSkipped.get());
                                                    existingStatistic.put("passed", countPassed.get());
                                                    existingStatistic.put("unknown", countUnknown.get());

                                                    // Обновление значения total
                                                    int newTotal = countFailed.get() + countBroken.get() + countSkipped.get() + countPassed.get() + countUnknown.get();
                                                    existingStatistic.put("total", newTotal);
                                                }

                                                // Запись обновленного JSON обратно в файл
                                                Files.write(targetPath, objectMapper.writeValueAsBytes(existingReportJson), StandardOpenOption.TRUNCATE_EXISTING);
                                            }
                                        }
                                    }
                                }
                                if (("severity.json".equals(fileName) || "status-chart.json".equals(fileName) || "duration.json".equals(fileName)) && "widgets".equals(parentDirName)) {
                                    try {
                                        FileTime fileTime = Files.getLastModifiedTime(targetPath);
                                        LocalDate lastModifiedDate = fileTime.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                                        LocalDate todayDate = LocalDate.now();
                                        boolean fileWasClearedOrEmpty = false;

                                        if (!lastModifiedDate.equals(todayDate)) {
                                            // Файл не изменялся сегодня, очищаем его и записываем новые данные
                                            Files.write(targetPath, new byte[0], StandardOpenOption.TRUNCATE_EXISTING);
                                            fileWasClearedOrEmpty = true;
                                        }

                                        String newReportJson = new String(Files.readAllBytes(sourcePath), StandardCharsets.UTF_8);
                                        JsonNode newReportJsonNode = objectMapper.readTree(newReportJson);

                                        if (!newReportJsonNode.isEmpty()) {
                                            if (Files.exists(targetPath)) {
                                                String targetJsonStr = new String(Files.readAllBytes(targetPath), StandardCharsets.UTF_8);
                                                JsonNode targetJsonNode = objectMapper.readTree(targetJsonStr);
                                                fileWasClearedOrEmpty = fileWasClearedOrEmpty || targetJsonStr.isEmpty() || targetJsonStr.equals("[]");

                                                if (fileWasClearedOrEmpty) {
                                                    // Если файл был очищен, записываем новые данные
                                                    Files.write(targetPath, newReportJson.getBytes(StandardCharsets.UTF_8), StandardOpenOption.TRUNCATE_EXISTING);
                                                } else {
                                                    ArrayNode targetItemsArray = (ArrayNode) targetJsonNode;
                                                    ArrayNode newItemsArray = (ArrayNode) newReportJsonNode;

                                                    boolean hasMatchingRows = false;

                                                    // Проверка совпадений по ключу "name" и удаление совпадающих частей
                                                    for (JsonNode newItem : newItemsArray) {
                                                        String newName = newItem.get("name").asText();
                                                        for (Iterator<JsonNode> it = targetItemsArray.elements(); it.hasNext(); ) {
                                                            JsonNode existingItem = it.next();
                                                            String existingName = existingItem.get("name").asText();
                                                            if (newName.equals(existingName)) {
                                                                it.remove();
                                                                hasMatchingRows = true;
                                                            }
                                                        }
                                                    }

                                                    if (hasMatchingRows) {
                                                        // Добавление новых данных, если есть совпадения
                                                        targetItemsArray.addAll(newItemsArray);

                                                        // Запись обновленного JSON обратно в файл
                                                        Files.write(targetPath, objectMapper.writeValueAsBytes(targetItemsArray), StandardOpenOption.TRUNCATE_EXISTING);
                                                    } else {
                                                        // Если совпадений нет, добавляем новые данные с учетом скобок и запятых
                                                        String correctedJson = objectMapper.writeValueAsString(newItemsArray);
                                                        String existingContent = targetJsonStr;

                                                        // Удаляем скобки [] у новых данных, если они есть
                                                        if (correctedJson.startsWith("[") && correctedJson.endsWith("]")) {
                                                            correctedJson = correctedJson.substring(1, correctedJson.length() - 1);
                                                        }

                                                        // Проверяем, содержит ли существующее содержимое данные внутри [] скобок
                                                        if (existingContent.startsWith("[") && existingContent.endsWith("]")) {
                                                            if (existingContent.trim().length() <= 2) {
                                                                existingContent = "";
                                                            } else {
                                                                existingContent = existingContent.substring(0, existingContent.length() - 1) + ",";
                                                            }
                                                        }

                                                        // Проверяем, не пусты ли новые данные после удаления скобок
                                                        if (!correctedJson.trim().isEmpty()) {
                                                            if (!existingContent.startsWith("[")) {
                                                                existingContent = "[" + existingContent;
                                                            }

                                                            if (existingContent.endsWith("]")) {
                                                                existingContent = existingContent.substring(0, existingContent.length() - 1);
                                                            }

                                                            // Добавляем новые данные
                                                            existingContent += correctedJson;

                                                            // Проверяем, содержит ли существующее содержимое закрывающую скобку
                                                            if (!existingContent.endsWith("]")) {
                                                                existingContent += "]";
                                                            }

                                                            // Запись обновленного содержимого в файл
                                                            Files.write(targetPath, existingContent.getBytes(StandardCharsets.UTF_8), StandardOpenOption.TRUNCATE_EXISTING);
                                                        }
                                                    }
                                                }
                                            } else {
                                                Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
                                            }
                                        }
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }

                                else {
                                    if (!"timeline.json".equals(fileName) && !"behaviors.json".equals(fileName) && !"categories.json".equals(fileName) && !"environment.json".equals(fileName)
                                            && !"suites.json".equals(fileName) && !"packages.json".equals(fileName) && !"summary.json".equals(fileName) && !"suites.csv".equals(fileName)
                                            && !"severity.json".equals(fileName)) {
                                        FileTime fileTime = Files.getLastModifiedTime(targetPath);
                                        LocalDate lastModifiedDate = fileTime.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                                        LocalDate todayDate = LocalDate.now();

                                        if (!lastModifiedDate.equals(todayDate)) {
                                            // Файл не был изменен сегодня, очищаем его и добавляем новые данные
                                            String correctedJson = objectMapper.writeValueAsString(sourceNode);
                                            Files.write(targetPath, correctedJson.getBytes(StandardCharsets.UTF_8), StandardOpenOption.TRUNCATE_EXISTING);
                                        } else {
                                            // Файл был изменен сегодня, добавляем новые данные без изменений в основной файл
                                            String correctedJson = objectMapper.writeValueAsString(sourceNode);
                                            String existingContent = Files.readString(targetPath, StandardCharsets.UTF_8);

                                            // Проверяем, пустой ли файл
                                            if (Files.size(targetPath) == 0) {
                                                // Если файл пустой, просто добавляем новые данные
                                                Files.write(targetPath, correctedJson.getBytes(StandardCharsets.UTF_8));
                                            } else {
                                                // Файл не пустой, продолжаем с логикой добавления данных
                                                // Удаляем скобки [ ] у новых данных, если они есть
                                                if (correctedJson.startsWith("[") && correctedJson.endsWith("]")) {
                                                    correctedJson = correctedJson.substring(1, correctedJson.length() - 1);
                                                }
                                                // Проверяем, содержит ли существующее содержимое данные внутри [ ] скобок
                                                if (existingContent.startsWith("[") && existingContent.endsWith("]")) {
                                                    // Удаляем скобки из существующего содержимого, если они пустые
                                                    if (existingContent.trim().length() <= 2) {
                                                        existingContent = "";
                                                    } else {
                                                        // Если данные присутствуют, добавляем запятую перед новыми данными
                                                        existingContent = existingContent.substring(0, existingContent.length() - 1) + ",";
                                                    }
                                                }

                                                // Проверяем, не пусты ли новые данные после удаления скобок
                                                if (!correctedJson.trim().isEmpty()) {
                                                    // Проверяем, содержит ли существующее содержимое открывающую скобку
                                                    if (!existingContent.startsWith("[")) {
                                                        existingContent = "[" + existingContent;
                                                    }
                                                    // Проверяем, заканчивается ли существующее содержимое на запятую и закрывающую скобку
                                                    if (existingContent.endsWith("]")) {
                                                        // Удаляем закрывающую скобку
                                                        existingContent = existingContent.substring(0, existingContent.length() - 1);
                                                    }
                                                    // Добавляем новые данные
                                                    existingContent += correctedJson;
                                                    // Проверяем, содержит ли существующее содержимое закрывающую скобку
                                                    if (!existingContent.endsWith("]")) {
                                                        existingContent += "]";
                                                    }
                                                    Files.write(targetPath, existingContent.getBytes(StandardCharsets.UTF_8));
                                                }
                                            }
                                        }
                                    }


                                }
                            } catch (JsonProcessingException e) {
                                log.error("Invalid JSON format in file: " + sourcePath, e);
                            }
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        // Использование переменной countMatchingRows в других условиях
        System.out.println("Total number of matching rows in all files: " + countMatchingRows.get());
    }

    public void integrateNewReportIntoFullReport(Path newReportPath, Path fullReportPath) {
        try {
            log.info("Merging reports from '{}' into '{}'", newReportPath, fullReportPath);
            // распаковка директории
            mergeReports(newReportPath, fullReportPath);
        } catch (IOException e) {
            log.error("Failed to merge reports", e);
        }
    }

    public void processFile(Path sourcePath, Path targetPath, String reportPrefix) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();

        if (!Files.exists(targetPath)) {
            Files.copy(sourcePath, targetPath);
        } else {
            FileTime fileTime = Files.getLastModifiedTime(targetPath);
            LocalDate lastModifiedDate = fileTime.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            LocalDate todayDate = LocalDate.now();

            String newReportJsonStr = Files.readString(sourcePath);
            JsonNode newReportJsonNode = objectMapper.readTree(newReportJsonStr);

            if (!lastModifiedDate.equals(todayDate)) {
                // Файл не был изменен сегодня, очищаем его и добавляем новые данные
                Files.writeString(targetPath, newReportJsonStr, StandardOpenOption.TRUNCATE_EXISTING);
            } else {
                // Файл был изменен сегодня, проверяем совпадения и обновляем данные
                String existingReportJsonStr = Files.readString(targetPath);
                JsonNode existingReportJsonNode = objectMapper.readTree(existingReportJsonStr);

                boolean dataUpdated = false;

                if (!existingReportJsonNode.isEmpty()) {
                    // Ищем совпадения по ключу "name"
                    for (JsonNode newChildNode : newReportJsonNode.path("children")) {
                        String newName = newChildNode.get("name").asText();

                        for (Iterator<JsonNode> it = existingReportJsonNode.path("children").elements(); it.hasNext(); ) {
                            JsonNode existingChildNode = it.next();
                            String existingName = existingChildNode.get("name").asText();

                            if (newName.equals(existingName)) {
                                // Удаляем совпадающую часть данных
                                it.remove();
                                dataUpdated = true;
                            }
                        }
                    }
                }

                // Если данные были обновлены, записываем новый отчет в основной файл
                if (dataUpdated) {
                    // Добавляем новые данные
                    ((ArrayNode) existingReportJsonNode.path("children")).addAll((ArrayNode) newReportJsonNode.path("children"));

                    // Записываем обновленные данные в основной файл
                    Files.writeString(targetPath, objectMapper.writeValueAsString(existingReportJsonNode), StandardOpenOption.TRUNCATE_EXISTING);
                } else {
                    // Если совпадений нет, добавляем новые данные без изменений в основной файл
                    if (!newReportJsonStr.isEmpty()) {
                        // Удаляем часть данных начинающуюся с {"uid":"...
                        newReportJsonStr = newReportJsonStr.replaceAll("\\{\"uid\":\"[^\"]*\",\"name\":\"" + reportPrefix + "\",\"children\":\\[", "");

                        if (existingReportJsonStr.isEmpty()) {
                            Files.writeString(targetPath, newReportJsonStr);
                        } else {
                            if (existingReportJsonStr.endsWith("]}")) {
                                existingReportJsonStr = existingReportJsonStr.substring(0, existingReportJsonStr.length() - 2);
                            }

                            // Проверка, пуст ли массив children в основном отчете
                            boolean hasChildren = existingReportJsonNode.path("children").size() > 0;

                            if (!newReportJsonStr.trim().startsWith("]") && hasChildren) {
                                newReportJsonStr = "," + newReportJsonStr;
                            }

                            String updatedReportJsonStr = existingReportJsonStr + newReportJsonStr;

                            if (!updatedReportJsonStr.endsWith("]}")) {
                                updatedReportJsonStr += "]}";
                            }

                            Files.writeString(targetPath, updatedReportJsonStr, StandardOpenOption.TRUNCATE_EXISTING);
                        }
                    }
                }
            }
        }
    }


    private boolean isTestReport(String url, Path destination) {
        // Проверка наличия слова "test" в URL
        if (url.toLowerCase().contains("test")) {
            return true;
        }
        // Проверка наличия слова "test" в имени папки
        if (destination.getFileName().toString().toLowerCase().contains("test")) {
            return true;
        }
        return false;
    }

    private boolean containsKeyword(String url, Path destination, String keyword) {
        // Проверка наличия ключевого слова в URL
        if (url.toLowerCase().contains(keyword.toLowerCase())) {
            return true;
        }
        // Проверка наличия ключевого слова в имени папки
        if (destination.getFileName().toString().toLowerCase().contains(keyword.toLowerCase())) {
            return true;
        }
        return false;
    }

    private void addRefreshMetaTagIfMissing(Path filePath, String metaTag) {
        try {
            String fileContent = new String(Files.readAllBytes(filePath));
            if (!fileContent.contains(metaTag)) {
                try (BufferedWriter writer = Files.newBufferedWriter(filePath, StandardOpenOption.APPEND)) {
                    writer.write(metaTag);
                }
            }
        } catch (IOException e) {
            System.err.println("Ошибка при добавлении мета-тега обновления страницы: " + e.getMessage());
        }
    }

    private void addReportTypeToTitle(Path filePath, String reportType, String keyword) {
        try {
            String fileContent = new String(Files.readAllBytes(filePath));
            String titleTag = "<title>Allure Report</title>";
            String newTitleTag = "<title>" + reportType + "</title>";
            if (!fileContent.contains(newTitleTag)) {
                fileContent = fileContent.replace(titleTag, newTitleTag);
                Files.write(filePath, fileContent.getBytes(StandardCharsets.UTF_8), StandardOpenOption.TRUNCATE_EXISTING);
            }
        } catch (IOException e) {
            System.err.println("Ошибка при добавлении типа отчета в заголовок: " + e.getMessage());
        }
    }

    public void updateSummaryReport(Path reportDir, String reportType) {
        try {
            Path summaryFilePath = reportDir.resolve("widgets/summary.json");
            if (Files.exists(summaryFilePath)) {
                String content = new String(Files.readAllBytes(summaryFilePath));
                ObjectMapper mapper = new ObjectMapper();
                ObjectNode summaryJson = (ObjectNode) mapper.readTree(content);

                String reportName = summaryJson.get("reportName").asText();
                String expectedSuffix = "Allure Report " + reportType;
                if (!reportName.endsWith(expectedSuffix)) {
                    if (reportName.endsWith("Allure Report")) {
                        reportName += " " + reportType;
                    } else if (!reportName.endsWith("Allure Report Test окружения") && !reportName.endsWith("Allure Report Dev окружения")
                            && !reportName.endsWith("Allure Report Preprod окружения") && !reportName.endsWith("Allure Report все окружения")
                            && !reportName.endsWith("Allure Report E2E тестирование")) {
                        reportName += "Allure Report " + reportType;
                    }
                    summaryJson.put("reportName", reportName);
                    Files.write(summaryFilePath, mapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(summaryJson), StandardOpenOption.TRUNCATE_EXISTING);
                }
            }
        } catch (Exception e) {
            System.err.println("Error updating summary report: " + e.getMessage());
        }
    }

    private void copyAllureProperties(Collection<Path> dirs) {
        try (InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream("allure.properties")) {
            if (is == null) {
                log.warn("allure.properties resource not found");
                return;
            }
            byte[] data = is.readAllBytes();
            for (Path dir : dirs) {
                Files.write(dir.resolve("allure.properties"), data, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            }
        } catch (IOException e) {
            log.warn("Failed to copy allure.properties", e);
        }
    }


}
