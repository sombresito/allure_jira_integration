package ru.iopump.qa.allure.gui.view;

import lombok.Data;
import ru.iopump.qa.allure.gui.view.ModuleTestStatsResponse;

import java.util.List;

@Data
public class ModuleSuitesResponse {
    private int total;
    private List<ModuleTestStatsResponse> items;
}
