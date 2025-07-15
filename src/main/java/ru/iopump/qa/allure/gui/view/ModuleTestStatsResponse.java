package ru.iopump.qa.allure.gui.view;

import lombok.Data;

@Data
public class ModuleTestStatsResponse {
    private String uid;
    private String name;
    private Statistic statistic;

    @Data
    public static class Statistic {
        private int failed;
        private int broken;
        private int skipped;
        private int passed;
        private int unknown;
        private int total;
    }
}
