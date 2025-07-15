package ru.iopump.qa.allure.service.generate;

import java.sql.SQLSyntaxErrorException;
import java.util.ArrayList;
import ru.iopump.qa.allure.service.generate.ErrorExecutingScript;
import ru.iopump.qa.allure.service.generate.GenerateNameJur;

public class GenerateIinJur {

    public static ArrayList<IINRecord> SendIinJurDataset(String name_dtst, int cnt_iin, String BdUrl, String uiLogin, String uiPassword, String desiredResident)
            throws Exception, SQLSyntaxErrorException {
        ArrayList<IINRecord> generatedRecords = new ArrayList<>();

        // Продолжаем генерировать записи, пока не достигнем нужного количества
        while (generatedRecords.size() < cnt_iin) {
            // Получаем сгенерированный ИИН для юрлица
            String test_iin = GenerateIin.generateiinjur();
            System.out.println("");
            System.out.println("----------------------------------------------------------------------");
            if (test_iin.equals("000000000000")) {
                System.out.println("Вернулся ИИН, состоящий из нулей, пропускаем");
                continue;
            } else {
                System.out.println(String.format("Сгенерирован ИИН - %s", test_iin));
            }

            // Формируем дату регистрации на основе ИИН
            String date_reg = String.format("01%s20%s", test_iin.substring(2, 4), test_iin.substring(0, 2));

            String type_entity = null;
            String res = null;
            String org = null;
            String type_org = null;

            // Определяем тип юридического лица по символу в ИИН
            if (test_iin.charAt(4) == '4') {
                type_entity = "JUR res";
                org = "JUR";
                res = "1";
            } else if (test_iin.charAt(4) == '5') {
                type_entity = "JUR nores";
                org = "JUR";
                res = "0";
            } else if (test_iin.charAt(4) == '6') {
                type_entity = "PBOYUL share";
                org = "PBOYUL";
                res = "1";
            } else {
                throw new Exception(String.format("В ИИН по 4 индексу находится символ %c, которого нет в наборе '456'. ИИН - %s", test_iin.charAt(4), test_iin));
            }
            if (test_iin.charAt(5) == '0') {
                type_org = "GO";
            } else if (test_iin.charAt(5) == '1') {
                type_org = "Filial";
            } else if (test_iin.charAt(5) == '2') {
                type_org = "Repres office";
            } else if (test_iin.charAt(5) == '4') {
                type_org = "Farmer";
            } else {
                throw new Exception(String.format("В ИИН по 5 индексу находится символ %c, которого нет в наборе '0124'. ИИН - %s", test_iin.charAt(5), test_iin));
            }

            // Если сгенерированная резидентность не соответствует требуемой, пропускаем эту итерацию
            if (!res.equals(desiredResident)) {
                System.out.println("Пропускаем, так как резидентность " + res + " не соответствует требуемой " + desiredResident);
                continue;
            }

            // Получаем наименование компании
            GenerateNameJur genName = new GenerateNameJur();
            String name_company = genName.GetNameCompany(res, org);

            // Проверяем, существует ли ИИН в таблице g_clihst
            String select_check_iin_db = String.format("select COUNT(TAXCODE) from g_clihst where TAXCODE = '%s'", test_iin);
            ArrayList<ArrayList> result_check_iin_db = ConnectionData.executeQuerySelect(select_check_iin_db, BdUrl, uiLogin, uiPassword);
            int cnt_row = (int) Double.parseDouble(String.valueOf(result_check_iin_db.get(0).get(0)));
            if (cnt_row == 0) {
                System.out.println(String.format("Запрос не вернул ни одной строки по ИИН в G_CLI - %s", test_iin));

                // Проверяем наличие ИИН в таблице g_iin
                String select_check_g_iin = String.format("select COUNT(IIN) from g_iin where IIN = '%s'", test_iin);
                ArrayList<ArrayList> result_check_g_iin = ConnectionData.executeQuerySelect(select_check_g_iin, BdUrl, uiLogin, uiPassword);
                int cnt_row_g_iin = (int) Double.parseDouble(String.valueOf(result_check_g_iin.get(0).get(0)));

                if (cnt_row_g_iin == 0) {
                    System.out.println(String.format("Запрос не вернул ни одной строки, из G_IIN, по ИИН - %s", test_iin));

                    // Проверяем, что ИИН еще не добавлен в датасет
                    String select_check_dtst = String.format("select COUNT(IIN) from %s where IIN = '%s'", name_dtst.toUpperCase(), test_iin);
                    ArrayList<ArrayList> result_check_dtst = ConnectionData.executeQuerySelect(select_check_dtst, BdUrl, uiLogin, uiPassword);
                    int cnt_row_dtst = (int) Double.parseDouble(String.valueOf(result_check_dtst.get(0).get(0)));

                    if (cnt_row_dtst == 0) {
                        // Вставляем ИИН в датасет
                        String insertQuery = String.format(
                                "insert into %s(IIN, TYPE_ENTITY, TYPE_ORG, DATE_REG, RESIDENT, NAME_COM) " +
                                        "values('%s', '%s', '%s', '%s', '%s', '%s')",
                                name_dtst.toUpperCase(), test_iin, type_entity, type_org, date_reg, res, name_company);
                        ConnectionData.executeQueryDML(insertQuery, BdUrl, uiLogin, uiPassword);

                        // Проверяем, что запись добавлена
                        String select_check_iin_dtst = String.format("select COUNT(IIN) from %s where IIN = '%s'", name_dtst.toUpperCase(), test_iin);
                        ArrayList<ArrayList> result_check_iin_dtst = ConnectionData.executeQuerySelect(select_check_iin_dtst, BdUrl, uiLogin, uiPassword);
                        int cnt_check_dtst = (int) Double.parseDouble(String.valueOf(result_check_iin_dtst.get(0).get(0)));

                        if (cnt_check_dtst > 0) {
                            System.out.println(String.format("В датасет %s добавлен ИИН - %s", name_dtst.toUpperCase(), test_iin));
                            System.out.println("----------------------------------------------------------------------");
                            // Создаем запись и добавляем в список
                            IINRecord record = new IINRecord();
                            record.setIin(test_iin);
                            record.setTypeEntity(type_entity);
                            record.setTypeOrg(type_org);
                            record.setDateReg(date_reg);
                            record.setResident(res);
                            record.setNameCom(name_company);
                            generatedRecords.add(record);
                        } else {
                            throw new Exception(String.format("В датасет %s не добавлен ИИН - %s", name_dtst.toUpperCase(), test_iin));
                        }
                    } else {
                        System.out.println(String.format("В датасете %s уже существует данный ИИН - %s", name_dtst.toUpperCase(), test_iin));
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
