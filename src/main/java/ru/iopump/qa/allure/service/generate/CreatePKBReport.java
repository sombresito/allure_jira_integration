package ru.iopump.qa.allure.service.generate;

import ru.iopump.qa.allure.service.generate.ErrorExecutingScript;
import ru.iopump.qa.allure.service.generate.ConnectionData;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;

public class CreatePKBReport {
    public void AddCliPkbRep(String cliCode, String BdUrl, String uiLogin, String uiPassword) throws SQLException, ErrorExecutingScript, ParseException {
        // Метод добавляет ПКБ отчет выбранному клиенту
        //Вызываем процедуру, которая добавляет ПКБ отчет
        ConnectionData con_data = new ConnectionData();
        // создаем объект массива
        String object_varray = "create or replace TYPE varrayRepIdPkb IS VARRAY(20000) OF NUMBER;";
        con_data.executeQueryDML(object_varray, BdUrl, uiLogin, uiPassword);
        // создаем таблицу в которой будут храниться id отчетов, которые уже использовались ранее
        // если она уже существует, то не создаем, если нет, то создаем
        String select_table = "select count(*) from ALL_OBJECTS WHERE OBJECT_TYPE = 'TABLE' and OBJECT_NAME = 'AT_TIDPKBREPORT'";
        ArrayList<ArrayList> result_select = con_data.executeQuerySelect(select_table, BdUrl, uiLogin, uiPassword);
        if (result_select.get(0).get(0).equals("0.0")) {
            System.out.println("Таблица AT_TIDPKBREPORT не существует. Создаем");
            String crt_table = "create table AT_tIdPkbReport (id number(10))";
            con_data.executeQueryDML(crt_table, BdUrl, uiLogin, uiPassword);
        } else {
            System.out.println("Таблица AT_TIDPKBREPORT уже создана");
            // создаем клиенту отчет пкб
            ArrayList pkbLst = new ArrayList<>(Arrays.asList(cliCode));
            ArrayList result_jur = con_data.CallSqlProcedure("Z_PKG_AUTO_TEST", "AT_pSetPkbReport", pkbLst, BdUrl, uiLogin, uiPassword);
        }
    }
}

