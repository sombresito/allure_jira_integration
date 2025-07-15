package ru.iopump.qa.allure.service.generate;


import ru.iopump.qa.allure.service.generate.ErrorExecutingScript;
import ru.iopump.qa.allure.service.generate.ConnectionData;
import java.sql.SQLException;
import java.util.ArrayList;

public class GenerateIINUI {

    public static void main(String args[]) throws Exception {
        // Пример тестовых значений
        String selectedTypeIIN = "JUR";   // или "FL"
        int cntIIN = 5;
        String selectedDbUrl = "jdbc"; // пример URL подключения
        String dbUser = "your_username";
        String dbPass = "your_password";
        String desiredResident = "1";

        GenerateIINUI generator = new GenerateIINUI();
        ArrayList<IINRecord> records = generator.GenerateIINUI(
                "TESTINGIINMANUAL" + selectedTypeIIN,
                selectedTypeIIN,
                cntIIN,
                selectedDbUrl,
                dbUser,
                dbPass,
                desiredResident
        );
        System.out.println(records);
    }


    public ArrayList<IINRecord> GenerateIINUI(String name_dataset, String type_iin, int cnt_iin, String BdUrl, String uiLogin, String uiPassword, String desiredResident) throws Exception {
        ArrayList<IINRecord> generatedRecords = new ArrayList<>();

        // Проверяем наличие датасета
        String select_dtst = String.format("select count(*) from all_all_tables where table_name='%s'", name_dataset.toUpperCase());
        ArrayList<ArrayList> get_cnt_dtst = ConnectionData.executeQuerySelect(select_dtst, BdUrl, uiLogin, uiPassword);

        if (get_cnt_dtst.get(0).get(0).equals("1.0")) {
            System.out.println(String.format("В БД найден датасет %s", name_dataset.toUpperCase()));
            if (type_iin.equals("FL")) {
                generatedRecords = GenerateIinFl.SendIinFlDataset(name_dataset, cnt_iin, BdUrl, uiLogin, uiPassword, desiredResident);
            } else if (type_iin.equals("JUR")) {
                generatedRecords = GenerateIinJur.SendIinJurDataset(name_dataset, cnt_iin, BdUrl, uiLogin, uiPassword, desiredResident);
            } else {
                throw new Exception(String.format("Передан неверный тип ИИН type_iin - %s. Необходимо передавать FL или JUR", type_iin));
            }
        } else {
            // Логика создания датасета и генерации ИИН аналогична выше, с передачей desiredResident
            if (type_iin.equals("FL")) {
                System.out.println(String.format("Создание датасета %s", name_dataset.toUpperCase()));
                String create_dtst = String.format("CREATE TABLE %s(IIN varchar2(255), SEX varchar2(255), BIRTHDAY varchar2(255), RESIDENT varchar2(255), NAME varchar2(255), SURNAME varchar2(255), MIDDLENAME varchar2(255))", name_dataset.toUpperCase());
                ConnectionData.executeQueryDML(create_dtst, BdUrl, uiLogin, uiPassword);
                ArrayList<ArrayList> get_cnt_new_dtst = ConnectionData.executeQuerySelect(
                        String.format("select count(*) from all_all_tables where table_name='%s'", name_dataset.toUpperCase()),
                        BdUrl, uiLogin, uiPassword);
                if (get_cnt_new_dtst.get(0).get(0).equals("1.0")) {
                    System.out.println(String.format("Создан датасет %s", name_dataset.toUpperCase()));
                    generatedRecords = GenerateIinFl.SendIinFlDataset(name_dataset, cnt_iin, BdUrl, uiLogin, uiPassword, desiredResident);
                } else {
                    throw new Exception(String.format("Датасет %s не создан в БД", name_dataset.toUpperCase()));
                }
            } else if (type_iin.equals("JUR")) {
                System.out.println(String.format("Создание датасета %s", name_dataset.toUpperCase()));
                String create_dtst = String.format("CREATE TABLE %s(IIN varchar2(255), TYPE_ENTITY varchar2(255), TYPE_ORG varchar2(255), DATE_REG varchar2(255), RESIDENT varchar2(255), NAME_COM varchar2(255))", name_dataset.toUpperCase());
                ConnectionData.executeQueryDML(create_dtst, BdUrl, uiLogin, uiPassword);
                ArrayList<ArrayList> get_cnt_new_dtst = ConnectionData.executeQuerySelect(
                        String.format("select count(*) from all_all_tables where table_name='%s'", name_dataset.toUpperCase()),
                        BdUrl, uiLogin, uiPassword);
                if (get_cnt_new_dtst.get(0).get(0).equals("1.0")) {
                    System.out.println(String.format("Создан датасет %s", name_dataset.toUpperCase()));
                    generatedRecords = GenerateIinJur.SendIinJurDataset(name_dataset, cnt_iin, BdUrl, uiLogin, uiPassword, desiredResident);
                } else {
                    throw new Exception(String.format("Датасет %s не создан в БД", name_dataset.toUpperCase()));
                }
            } else {
                throw new Exception(String.format("Передан неверный тип ИИН type_iin - %s. Необходимо передавать FL или JUR", type_iin));
            }
        }
        return generatedRecords;
    }


}



