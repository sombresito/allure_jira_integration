package ru.iopump.qa.allure.properties;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

@Getter
@Component
@PropertySource("classpath:application.yaml")

public class ReportProperties {

    @Value("${report_full.idFull}")
    private String yourProperty;

    @Value("${report_full.idPreprod}")
    private String yourPreprodReportProperty;

    @Value("${report_full.idTest}")
    private String yourTestReportProperty;

    @Value("${report_full.idDev}")
    private String yourDevReportProperty;

    @Value("${report_full.idETE}")
    private String yourETEReportProperty;

    @Value("${report_full.idColvir}")
    private String yourColvirReportProperty;



}

