package ru.iopump.qa.allure.service.generate;

import java.sql.SQLSyntaxErrorException;
import java.util.ArrayList;
import java.util.Random;

public class GenerateIinFl {

    public static ArrayList<IINRecord> SendIinFlDataset(String name_dtst, int cnt_iin, String BdUrl, String uiLogin, String uiPassword, String desiredResident)
            throws Exception, SQLSyntaxErrorException {
        ArrayList<IINRecord> generatedRecords = new ArrayList<>();
        // Используем цикл, который продолжается, пока не сгенерировано нужное число записей
        while(generatedRecords.size() < cnt_iin) {
            // Генерируем ИИН физлица
            String test_iin = GenerateIin.generateiinfl();
            System.out.println("----------------------------------------------------------------------");
            System.out.println(String.format("Сгенерирован ИИН - %s", test_iin));

            String res = null;
            String sex = null;
            String birthday = null;
            GenerateNameFl gen_name = new GenerateNameFl();

            // Определяем резиденство, пол и дату рождения по ИИН
            if (test_iin.charAt(6) == '0') {
                res = "0";
                Random random_sex = new Random();
                int index_sex = random_sex.nextInt(2); // 0 или 1
                String sex_string = "MF";
                sex = String.valueOf(sex_string.charAt(index_sex));
                birthday = String.format("%s%s19%s", test_iin.substring(4, 6), test_iin.substring(2, 4), test_iin.substring(0, 2));
            } else if (test_iin.charAt(6) == '3') {
                res = "1";
                sex = "M";
                birthday = String.format("%s%s19%s", test_iin.substring(4, 6), test_iin.substring(2, 4), test_iin.substring(0, 2));
            } else if (test_iin.charAt(6) == '4') {
                res = "1";
                sex = "F";
                birthday = String.format("%s%s19%s", test_iin.substring(4, 6), test_iin.substring(2, 4), test_iin.substring(0, 2));
            } else if (test_iin.charAt(6) == '5') {
                res = "1";
                sex = "M";
                birthday = String.format("%s%s20%s", test_iin.substring(4, 6), test_iin.substring(2, 4), test_iin.substring(0, 2));
            } else if (test_iin.charAt(6) == '6') {
                res = "1";
                sex = "F";
                birthday = String.format("%s%s20%s", test_iin.substring(4, 6), test_iin.substring(2, 4), test_iin.substring(0, 2));
            }

            // Если резидентность не соответствует желаемой, пропускаем запись
            if (!res.equals(desiredResident)) {
                continue;
            }

            // Получаем имя, фамилию и отчество
            String name = gen_name.GetNameFl(sex, res);
            String last_name = gen_name.GetSurnameFl(sex, res);
            String middle_name = gen_name.GetMiddleNameFl(sex, res);

            // Проверяем, существует ли данный ИИН в таблице g_clihst
            String select_check_iin_db = String.format("select COUNT(TAXCODE) from g_clihst where TAXCODE = '%s'", test_iin);
            ArrayList<ArrayList> result_check_iin_db = ConnectionData.executeQuerySelect(select_check_iin_db, BdUrl, uiLogin, uiPassword);
            int cnt_row = (int) Double.parseDouble(String.valueOf(result_check_iin_db.get(0).get(0)));
            if (cnt_row == 0) {
                System.out.println(String.format("Запрос не вернул ни одной строки по ИИН - %s из БД", test_iin));

                // Проверяем наличие ИИН в таблице g_iin
                String select_check_g_iin = String.format("select COUNT(IIN) from g_iin where IIN = '%s'", test_iin);
                ArrayList<ArrayList> result_check_g_iin = ConnectionData.executeQuerySelect(select_check_g_iin, BdUrl, uiLogin, uiPassword);
                int cnt_row_g_iin = (int) Double.parseDouble(String.valueOf(result_check_g_iin.get(0).get(0)));
                if (cnt_row_g_iin == 0) {
                    System.out.println(String.format("Запрос не вернул ни одной строки, из G_IIN, по ИИН - %s", test_iin));

                    // Проверяем, что ИИН не существует в датасете
                    String select_check_dtst = String.format("select COUNT(IIN) from %s where IIN = '%s'", name_dtst.toUpperCase(), test_iin);
                    ArrayList<ArrayList> result_check_dtst = ConnectionData.executeQuerySelect(select_check_dtst, BdUrl, uiLogin, uiPassword);
                    int cnt_row_dtst = (int) Double.parseDouble(String.valueOf(result_check_dtst.get(0).get(0)));
                    if (cnt_row_dtst == 0) {
                        // Вставляем ИИН в датасет
                        String insertQuery = String.format(
                                "insert into %s(IIN, SEX, BIRTHDAY, RESIDENT, NAME, SURNAME, MIDDLENAME) " +
                                        "values('%s', '%s', '%s', '%s', '%s', '%s', '%s')",
                                name_dtst.toUpperCase(), test_iin, sex, birthday, res, name, last_name, middle_name);
                        ConnectionData.executeQueryDML(insertQuery, BdUrl, uiLogin, uiPassword);

                        // Проверяем, что запись добавлена
                        String select_check_iin_dtst = String.format("select COUNT(IIN) from %s where IIN = '%s'", name_dtst.toUpperCase(), test_iin);
                        ArrayList<ArrayList> result_check_iin_dtst = ConnectionData.executeQuerySelect(select_check_iin_dtst, BdUrl, uiLogin, uiPassword);
                        int cnt_check_dtst = (int) Double.parseDouble(String.valueOf(result_check_iin_dtst.get(0).get(0)));
                        if (cnt_check_dtst > 0) {
                            System.out.println(String.format("в датасет %s добавлен ИИН - %s", name_dtst.toUpperCase(), test_iin));
                            IINRecord record = new IINRecord();
                            record.setIin(test_iin);
                            record.setSex(sex);
                            record.setBirthday(birthday);
                            record.setResident(res);
                            record.setName(name);
                            record.setSurname(last_name);
                            record.setMiddleName(middle_name);
                            generatedRecords.add(record);
                        } else {
                            throw new Exception(String.format("в датасет %s не добавлен ИИН - %s", name_dtst.toUpperCase(), test_iin));
                        }
                    } else {
                        System.out.println(String.format("в датасете %s уже существует данный ИИН - %s", name_dtst.toUpperCase(), test_iin));
                    }
                } else {
                    System.out.println(String.format("Сгенерированный ИИН - %s существует в G_IIN", test_iin));
                }
            } else {
                System.out.println(String.format("Сгенерированный ИИН - %s существует в БД", test_iin));
            }
        }
        return generatedRecords;
    }

}