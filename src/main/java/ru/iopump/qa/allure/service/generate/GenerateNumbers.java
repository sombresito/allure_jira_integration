package ru.iopump.qa.allure.service.generate;


import ru.iopump.qa.allure.service.generate.ConnectionData;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.util.List;

public class GenerateNumbers {

    public String GenPhoneNumber(String BdUrl, String uiLogin, String uiPassword) throws Exception {
        // метод генерирует номер телефона
        // которого нет в БД
        Random random = new Random();
        String str_null = "0";
        String tel_num = null;
        List<String> lst_provider = Arrays.asList("701", "702", "705", "778", "707", "777", "708", "775", "776", "747");
        while (true) {
            int index_provider = random.nextInt(lst_provider.size()); // получаем псевдорандомный индекс списка
            // получаем по индексу элемент списка
            String provider = lst_provider.get(index_provider);
            // генерируем номер телефона состоящий из 7 цифр
            // если рандомное число менее 7 знаков, то генерируем нули спереди
            int num = random.nextInt((9999999 - 1) + 1) + 1;
            int cnt_null = 7;
            int cnt_null_d = cnt_null - Integer.toString(num).length();
            String num_with_null = str_null.repeat(cnt_null_d) + Integer.toString(num);
            // формируем полный номер телефона
            tel_num = String.format("+7%s%s", provider, num_with_null);

            // проверяем, что в БД нет номера, который сгенерировал скрипт
            String check_num_select = String.format("SELECT COUNT(CONT) FROM G_CLICONT WHERE CONT = '%s'", tel_num);
            ArrayList<ArrayList> check_num_tel = ConnectionData.executeQuerySelect(check_num_select, BdUrl, uiLogin, uiPassword);
            if (check_num_tel.get(0).get(0).equals("0.0")) {
                System.out.println(String.format("В БД отсутствует сгенерированный номер телефона - %s", tel_num));
                break;
            }
        }
        return tel_num;
    }
    public String GenDocNum(String BdUrl, String uiLogin, String uiPassword) throws Exception {
        // метод генерирует номер документа удостоверения личности
        // которого нет в БД
        Random random = new Random();
        String str_null = "0";
        String doc_num = null;
        while (true) {
            // генерируем номер телефона состоящий из 7 цифр
            // если рандомное число менее 7 знаков, то генерируем нули спереди
            int num = random.nextInt((999999999 - 1) + 1) + 1;
            int cnt_null = 9;
            int cnt_null_d = cnt_null - Integer.toString(num).length();
            String num_with_null = str_null.repeat(cnt_null_d) + Integer.toString(num);
            // формируем полный номер телефона
            doc_num = String.format("%s", num_with_null);

            // проверяем, что в БД нет номера, который сгенерировал скрипт
            String check_num_select = String.format("SELECT COUNT(*) FROM G_CLIDOC WHERE PASSNUM = '%s'", doc_num);
            ArrayList<ArrayList> check_num_tel = ConnectionData.executeQuerySelect(check_num_select, BdUrl, uiLogin, uiPassword);
            if (check_num_tel.get(0).get(0).equals("0.0")) {
                System.out.println(String.format("В БД отсутствует сгенерированный номер документа - %s", doc_num));
                break;
            }
        }
        return doc_num;
    }
    public String GetIdRecord(String BdUrl, String uiLogin, String uiPassword) throws Exception {
        Random random = new Random();
        ArrayList<ArrayList> check_id_lst = null;
        String get_id = null;
        while (true) { //если сгенерированный номер id существует в таблицах, то будет сгенерирован новый
            //получаем id записи в таблицах, которые используются при создании юр лица

            get_id = Integer.toString(random.nextInt(99999 - 10000) + 10000);
            String select_id = String.format("select COUNT(ID_GBDUL_MAIN) from z_077_ent_gbdul_main where ID_GBDUL_MAIN = '%s' " +
                    "UNION select COUNT(ID_GBDUL_MAIN) from z_077_ent_gbdul_leaders where ID_GBDUL_MAIN = '%s' " +
                    "UNION select COUNT(ID_GBDUL_MAIN) from z_077_ent_gbdul_founders where ID_GBDUL_MAIN = '%s'", get_id, get_id, get_id);
            check_id_lst = ConnectionData.executeQuerySelect(select_id, BdUrl, uiLogin, uiPassword);
            if (check_id_lst.get(0).get(0).equals("0.0") && check_id_lst.size() == 1) {
                break;
            }
        }
        String id_record = (String) check_id_lst.get(0).get(0);
        return get_id;
    }
}
