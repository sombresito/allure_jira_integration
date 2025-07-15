package ru.iopump.qa.allure.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import ru.iopump.qa.allure.entity.ReportEntity;
import ru.iopump.qa.allure.properties.AllureProperties;
import ru.iopump.qa.allure.properties.CleanUpProperties;
import ru.iopump.qa.allure.repo.JpaReportRepository;

import javax.annotation.PostConstruct;
import java.io.File;
import java.time.*;
import java.util.Collection;
import java.util.Date;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static org.apache.commons.io.FileUtils.deleteQuietly;

@Configuration
@Lazy(false)
@Slf4j
@RequiredArgsConstructor
@EnableScheduling
public class CleanUpServiceConfiguration implements SchedulingConfigurer {

    private final AllureProperties allureProperties;
    private final CleanUpProperties cleanUpProperties;
    private final JpaReportRepository repository;
    private final ObjectMapper objectMapper;

    private static String print(Collection<Pair<ReportEntity, Boolean>> removedReports) {
        return removedReports.stream().map(pair ->
                format("CleanUpResult(id=%s, path=%s, create=%s, age=%sd, isDeleted=%s)",
                        pair.getKey().getUuid(),
                        pair.getKey().getPath(),
                        pair.getKey().getCreatedDateTime(),
                        Duration.between(
                                pair.getKey().getCreatedDateTime(),
                                LocalDateTime.now()
                        ).toDays(),
                        pair.getValue())
        ).collect(Collectors.joining(", "));
    }

    @PostConstruct
    void init() throws JsonProcessingException {
        final ObjectWriter prettyWriter = objectMapper.writerWithDefaultPrettyPrinter();

        log.info("[ALLURE SERVER CONFIGURATION] CleanUp policy settings:\n{}", prettyWriter.writeValueAsString(cleanUpProperties));
    }

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        taskRegistrar.addTriggerTask(() -> {

            // Логирование параметров CleanUp
            log.info("CleanUp settings: time={}, ageDays={}, dryRun={}",
                    cleanUpProperties.getTime(), cleanUpProperties.getAgeDays(), cleanUpProperties.isNotDryRun());




            if (log.isInfoEnabled()) {
                log.info("CleanUp started ...");
                log.info("CleanUp parameters: " + cleanUpProperties);
            }

            final Collection<ReportEntity> candidatesCleanUp = repository
                    .findAllByCreatedDateTimeIsBefore(cleanUpProperties.getClosestEdgeDate());

            if (log.isDebugEnabled()) {
                log.debug("CleanUp. All reports: " + repository.findAll().stream()
                        .map(e -> e.getUuid() + " " + e.getPath() + " " + e.getCreatedDateTime())
                        .collect(Collectors.joining(", ", "[", "]"))
                );

                log.debug("CleanUp. Candidates to clean up: " + candidatesCleanUp.stream()
                        .map(e -> e.getUuid() + " " + e.getPath() + " " + e.getCreatedDateTime())
                        .collect(Collectors.joining(", ", "[", "]"))
                );
            }

            final Collection<Pair<ReportEntity, Boolean>> processedReports = candidatesCleanUp.stream()
                    .map(report ->
                            cleanUpProperties.getPaths().stream()
                                    // Есть ли среди настроек paths для данного отчета
                                    .filter(path -> report.getPath().equals(path.getPath())).findFirst()
                                    // Если отчет подпадает под правила paths, то найти правило и использовать
                                    .map(path -> {
                                        if (report.getCreatedDateTime().isBefore(path.getEdgeDate()))
                                            // Если отчет создан до крайней даты, то удалять
                                            return delete(report);
                                        else
                                            // Оставить если младше
                                            return Pair.of(report, false);
                                    })
                                    // Если отчет не подпадает под правила paths, то использовать общее правило ageDays
                                    .orElseGet(() -> {
                                        if (report.getCreatedDateTime().isBefore(cleanUpProperties.getEdgeDate()))
                                            // Если отчет создан до крайней даты, то удалять
                                            return delete(report);
                                        else
                                            // Оставить если младше
                                            return Pair.of(report, false);
                                    })
                    ).collect(Collectors.toUnmodifiableList());

            if (log.isInfoEnabled()) log.info("CleanUp finished with results: " + print(processedReports));

        }, triggerContext -> {

            final LocalDate nextDate = Optional.ofNullable(triggerContext.lastScheduledExecutionTime())
                    // Если триггер уже срабатывал, то прибавить день
                    .map(date -> date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate().plusDays(1))
                    // Если триггер не срабатывал, то взять текущий день
                    .orElse(LocalDate.now());

            final LocalTime nextTime = cleanUpProperties.getTime();


            // Следующее срабатывание из даты и времени из настроек
            final LocalDateTime nextDateTime = LocalDateTime.of(nextDate, nextTime);
            log.info("Next CleanUp scheduled for: {}", nextDateTime);


            if (log.isInfoEnabled()) log.info("Next CleanUp scheduled at " + nextDateTime);

            return Date.from(nextDateTime.atZone(ZoneId.systemDefault()).toInstant());
        });
    }


    private Pair<ReportEntity, Boolean> delete(ReportEntity report) {
        final File reportPath = allureProperties.reports().dirPath().resolve(report.getUuid().toString()).toFile();

        if (!reportPath.exists()) {
            log.warn("Report path does not exist: {}", reportPath.getAbsolutePath());
            return Pair.of(report, false);
        }

        if (!reportPath.canWrite()) {
            log.warn("No write permission for report path: {}", reportPath.getAbsolutePath());
            return Pair.of(report, false);
        }

        boolean isDeleted;
        if (cleanUpProperties.isNotDryRun()) {
            try {
                isDeleted = deleteQuietly(reportPath);
                log.info("Successfully deleted report: {}", report.getUuid());
            } catch (Exception e) {
                log.error("Failed to delete report: {}", report.getUuid(), e);
                isDeleted = false;
            }
        } else {
            log.info("DryRun mode: Report {} would have been deleted", report.getUuid());
            isDeleted = true;
        }

        return Pair.of(report, isDeleted);
    }


}
