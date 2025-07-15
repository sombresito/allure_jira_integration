package ru.iopump.qa.allure.service.generate;


import ru.iopump.qa.allure.service.generate.ErrorExecutingScript;
import ru.iopump.qa.allure.service.generate.ConnectionData;
import ru.iopump.qa.allure.service.generate.GenerateNumbers;
import ru.iopump.qa.allure.service.generate.ChangeDate;
//import javax.management.Query;
import java.util.ArrayList;
import java.util.Arrays;
import ru.iopump.qa.allure.service.generate.CreateDocDos;

public class CreateClientCardJurFL {

    public String CreateClientCardJur(String code_inn, String res, String type_doc, String cli_role, String nameCom,
                                      String name_dep, String dateReg, String BdUrl, String uiLogin, String uiPassword) throws Exception {
        // метод создает карточку юр лица
        // в данном методе также вызывается метод для создания физ лица
        // так как, чтобы создать юрика, необходимо подвязать ему уполномоченное физ лицо
        // в метод передаются:
        // code_inn - ИИН юрика
        // res - резидентство юрика
        // type_doc - тип документа уд личность (применяетс при создании уполн-го физ лица
        // cli_role роль физ лица
        // name - имя физ лица
        // surName - фамилия физ лица
        // nameCom - название юр лица
        // sex - пол физ лица
        // full_name - ФИО физ лица
        // name_dep - код департамента
        // birthday - дата рождения физ лица
        // dateReg - дата регистрации юр лица

        GenerateNumbers GetNum = new GenerateNumbers();
        ChangeDate cd = new ChangeDate();
        ConnectionData con_data = new ConnectionData();
        CreatePKBReport pkb = new CreatePKBReport();
        ArrayList<ArrayList> iin_db_fl = null;
        // получаем системную дату из базы
        String select_od = "SELECT TRUNC(SYSDATE) FROM Dual";
        ArrayList<ArrayList> date_od_lst = ConnectionData.executeQuerySelect(select_od, BdUrl, uiLogin, uiPassword);
        String oper_day = (String) date_od_lst.get(0).get(0);

        //date_from_before = GetPlusDailyDate(-30)
        //date_from = aqConvert.DateTimeToFormatStr(date_from_before, "%d.%m.%Y")
        //date_to_after = self.GetPlusDailyDate(3569)
        //date_to = aqConvert.DateTimeToFormatStr(date_to_after, "%d.%m.%Y")
        String num_phone = GetNum.GenPhoneNumber(BdUrl, uiLogin, uiPassword);

        String num_pass = GetNum.GenDocNum(BdUrl, uiLogin, uiPassword);
        String id_main = GetNum.GetIdRecord(BdUrl, uiLogin, uiPassword);
        String from_date_doc = cd.FutureLastDate(oper_day.replace(".", ""), "year", -5, true);
        String to_date_doc = cd.FutureLastDate(oper_day.replace(".", ""), "year", 5, true);
        String redister_date = String.format("%s-%s-%s", oper_day.substring(6), oper_day.substring(3, 5), oper_day.substring(0, 2));

        String res_country = null;
        String grCountry = null;
        String codeCountry = null;
        String nameCountry = null;
        String codeNatioan = null;
        if (res == "1") {
            grCountry = "Гражданин РК";
            codeCountry = "398";
            nameCountry = "КАЗАХСТАН";
            res_country = "KZ";
            codeNatioan = "1";
        } else if (res == "0") {
            grCountry = "Иностранный гражданин";
            codeCountry = "643";
            nameCountry = "РОССИЯ";
            res_country = "RU";
            codeNatioan = "2";
        }

        // проверяем, что данного ИИНа нет в g_iin (База налогоплательщиков) и g_clihst (База банка)
        // проверка будет происходить в цикле, чтобы каждый раз брался новый ИИН
        // для начала определяем количество уникальных ИИН в датасете
        // чтобы использовать, как конечную итерацию цикла
        String select_count_record = "SELECT DISTINCT COUNT(iin) FROM TESTINGIINMANUALFL";
        ArrayList<ArrayList> lst_count_record = ConnectionData.executeQuerySelect(select_count_record, BdUrl, uiLogin, uiPassword);
        int cnt_record = (int) Double.parseDouble((String) lst_count_record.get(0).get(0));

        ArrayList<String> cnt_prm = new ArrayList<>();
        // запускаем цикл for
        for (int i = 0; i < cnt_record; i++) {
            // получаем тестовый ИИН физ лица из датасета
            String select_iin_db_fl = String.format("SELECT * FROM TESTINGIINMANUALFL WHERE RESIDENT = '%s' and rownum = 1", res);
            iin_db_fl = ConnectionData.executeQuerySelect(select_iin_db_fl, BdUrl, uiLogin, uiPassword);
            // проверяем, что данного ИИН нет в базе налогоплательщиков
            String select_g_iin = String.format("SELECT COUNT(IIN) FROM G_IIN WHERE IIN = '%s'", (String) iin_db_fl.get(0).get(0));
            ArrayList<ArrayList> result_g_iin_lst = ConnectionData.executeQuerySelect(select_g_iin, BdUrl, uiLogin, uiPassword);

            if (result_g_iin_lst.get(0).get(0).equals("0.0")) {
                System.out.println(String.format("Полученный ИИН - %s, не найден в базе налогоплательщиков (G_IIN)", iin_db_fl.get(0).get(0)));
                // если ИИН не найден в базе налогоплательщика, то проверяем
                // есть ли карточка клиента в базе
                String select_g_clihst = String.format("SELECT COUNT(TAXCODE) FROM G_CLIHST WHERE TAXCODE = '%s'", (String) iin_db_fl.get(0).get(0));
                ArrayList<ArrayList> result_g_clihst_lst = ConnectionData.executeQuerySelect(select_g_clihst, BdUrl, uiLogin, uiPassword);
                if (result_g_clihst_lst.get(0).get(0).equals("0.0")) {
                    System.out.println(String.format("Полученный ИИН - %s, не найден в базе банка (G_CLIHST)", iin_db_fl.get(0).get(0)));
                    System.out.println(String.format("ИИН - %s можно использовать для создания карточки клиента", iin_db_fl.get(0).get(0)));
                    break;
                } else {
                    System.out.println(String.format("В БД банка (G_CLIHST) уже есть запись с ИИН - '%s'", iin_db_fl.get(0).get(0)));
                }
            } else {
                // если ИИН есть в базе, то пропускаем данную итерацию
                // параллельно удаляем ИИН из датасета
                System.out.println(String.format("В базе налогоплательщиков уже существует запись с ИИН - '%s'", iin_db_fl.get(0).get(0)));
                String dlt_select = String.format("DELETE FROM TESTINGIINMANUALFL WHERE IIN = '%s'", (String) iin_db_fl.get(0).get(0));
                ConnectionData.executeQueryDML(dlt_select, BdUrl, uiLogin, uiPassword);
            }

        }

        String iin_fl = (String) iin_db_fl.get(0).get(0);
        String sex_fl = (String) iin_db_fl.get(0).get(1);
        String birthdate = (String) iin_db_fl.get(0).get(2);
        String resident = (String) iin_db_fl.get(0).get(3);
        String surname = (String) iin_db_fl.get(0).get(4);
        String name_fl = (String) iin_db_fl.get(0).get(5);
        String middlename = (String) iin_db_fl.get(0).get(6);
        String fio = String.format("%s %s %s", surname, name_fl, middlename);

        if (iin_db_fl.get(0).get(3).equals("0.0")) {
            String sexFL = "M";
        } else if (iin_db_fl.get(0).get(3).equals("1.0")) {
            String sexFL = (String) iin_db_fl.get(0).get(5);
        }
        String selectUpDateNKFL = String.format("insert into g_iin (RNN, IIN, JURFL, RESIDFL, CONSTFL, INDIVIDFL, NAME, FIO, LASTREGEND, REASONDEREG, " +
                "NOTARY_LAWYER, INACTIVE, TAXDEP_ID, CORRECTDT) " +
                "values('', '%s', '0', '%s', '1', '0', '%s', '%s', '', '', '', '0', '5238', '')", iin_fl, resident, fio, fio);
        ConnectionData.executeQueryDML(selectUpDateNKFL, BdUrl, uiLogin, uiPassword);

        //ArrayList lst_prm = new ArrayList<>(Arrays.asList(iin_fl, type_doc, from_date_doc, to_date_doc, num_phone, cli_role, num_pass, "FL", resident, fio, sex_fl, res_country, name_dep, birthdate));
        ArrayList lst_prm = new ArrayList<>(Arrays.asList(iin_fl, type_doc, from_date_doc, to_date_doc, num_phone.replace("+7", ""), "CLI", num_pass, "FL", resident, sex_fl, res_country, fio, name_dep, birthdate, "CODE_CLIENT"));
        ArrayList result = con_data.CallSqlProcedure(null, "CreateClientCardJava", lst_prm, BdUrl, uiLogin, uiPassword);


        String selectUpDateNK = String.format("insert into g_iin (RNN, IIN, JURFL, RESIDFL, CONSTFL, INDIVIDFL, NAME, FIO, LASTREGEND, REASONDEREG, " +
                "NOTARY_LAWYER, INACTIVE, TAXDEP_ID, CORRECTDT) " +
                "values('', '%s', '1', '%s', '0', '0', '%s', '%s', '', '', '', '0', '5238', '')", code_inn, res, nameCom, nameCom);
        ConnectionData.executeQueryDML(selectUpDateNK, BdUrl, uiLogin, uiPassword);

        String select_iin_main = String.format("insert into z_077_ent_gbdul_main(id_gbdul_main, BIN, REQUESTDATE, XML_DATA, REGSTATUSCODE, REGSTATUSNAME, REGDEPARTCODE, REGDEPARTNAME, REGDATE, REGLASTDATE, FULLNAMERU, FULLNAMEKZ, FULLNAMELAT, SHORTNAMERU, SHORTNAMEKZ, SHORTNAMELAT, ORGFORMCODE, ORGFORMNAME, FORMOFLAWCODE, FORMOFLAWNAME, EPRISETYPECODE, EPRISETYPENAME, TAXORGSTATUS, CREATIONMETHODCODE, CREATIONMETHODNAME, PROPERTYTYPECODE, PROPERTYTYPENAME, TYPICALCHARACTER, COMMERCEORG, ENTERPRISESUBJECT, AFFILIATED, INTERNATIONAL, FOREIGNINVEST, ONECITIZENSHIP, BRANCHESEXISTS, ACTIVITYOKEDCODE, ACTIVITYOKEDNAME, ADDRESSZIPCODE, ADDRESSKATO, ADDRESSDISTRICT, ADDRESSREGION, ADDRESSCITY, ADDRESSSTREET, ADDRESSBUILDING, FOUNDRESCOUNT, FOUNDERSCOUNTFL, FOUNDERSCOUNTUL, ORGSIZECODE, ORGSIZENAME, STATCOMOKEDCODE, STATCOMOKEDNAME, ACTIVITYATTRCODE, ACTIVITYATTRNAME) " +
                "values ('%d', '%s', '%s', '01.01.1990', null, null, null, null, null, null, null, null, null, null, null, null, null, null, '20', null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, '050000', '751210000', null, null, null, 'улица Центральная', '1', null, null, null, null, null, null, null, null, null)", Integer.parseInt(id_main), code_inn, oper_day);
        ConnectionData.executeQueryDML(select_iin_main, BdUrl, uiLogin, uiPassword);
        String select_iin_leaders = String.format("insert into z_077_ent_gbdul_leaders(ID_GBDUL_MAIN, ORG_BIN, COUNTRYCODE, COUNTRYNAME, CITIZENCOUNTRYCODE, CITIZENCOUNTRYNAME, NATIOANLITYCODE, NATIONALITYNAME, IIN, SURNAME, NAME, MIDDLENAME) " +
                "values('%d', '%s', '%s', '%s', '%s', '%s', '%s', '%s', '%s', '%s', '%s', '%s')", Integer.parseInt(id_main), code_inn, codeCountry, nameCountry, codeCountry, nameCountry, codeNatioan, grCountry, iin_fl, iin_db_fl.get(0).get(4), iin_db_fl.get(0).get(5), iin_db_fl.get(0).get(6));

        ConnectionData.executeQueryDML(select_iin_leaders, BdUrl, uiLogin, uiPassword);

        String select_iin_founders = String.format("insert into z_077_ent_gbdul_founders(ID_GBDUL_MAIN, ORGBIN, FOUNDERCOUNTRYCODE, FOUNDERSCOUNTRYNAME, FOUNDERSNATIONCODE, FOUNDERSNATIONNAME, FOUNDERSCITIZENCODE, FOUNDERSCITIZENNAME, FOUNDERSIIN, FOUNDERSSURNAME, FOUNDERSNAME, FOUNDERSMIDDLENAME, FOUNDERSREGNUMBER, FOUNDERSREGDATE, FOUNDERSORGFULLNAMERU, FOUNDERSORGFULLNAMEKZ) " +
                "values('%d', '%s', '%s', '%s', '%s', '%s', '%s', '%s', '%s', '%s', '%s', '%s', '', '', '', '')", Integer.parseInt(id_main), code_inn, codeCountry, nameCountry, codeNatioan, grCountry, codeCountry, nameCountry, iin_fl, iin_db_fl.get(0).get(4), iin_db_fl.get(0).get(5), iin_db_fl.get(0).get(6));
        ConnectionData.executeQueryDML(select_iin_founders, BdUrl, uiLogin, uiPassword);
        String select_iin_StatEgovLog = String.format("insert into z_077_StatEgovLog(IBIN, ILANG, XRES, CALLDATE, RES, TYPE, STATUS, BINIIN, CLIN, REGISTERDATE, OKEDCODE, OKEDNAME, SECONDOKEDS, KRPCODE, KRPNAME, KRPBFCODE, KRPBFNAME, KATOCODE,  KATOID,  KATOADDRESS, FIO, IP) " +
                "values('%s','RU','', '%s', '%s', 'Ok',  'true',  '%s',  '%s',  '%s',  '46213', 'Оптовая торговля масличными культурами', '', '105', 'Малые предприятия (<= 5)',  '105', 'Малые предприятия (<= 5)',  '751210000', '268025',  'Г.АЛМАТЫ, АЛАТАУСКИЙ РАЙОН, Микрорайон Карасу, улица Центральная, дом 1', '%s %s %s',  'false')", code_inn, oper_day, res, code_inn, nameCom, redister_date, iin_db_fl.get(0).get(4), iin_db_fl.get(0).get(5), iin_db_fl.get(0).get(6));
        ConnectionData.executeQueryDML(select_iin_StatEgovLog, BdUrl, uiLogin, uiPassword);

        ArrayList result_jur_lst = new ArrayList<>(Arrays.asList(code_inn, type_doc, from_date_doc, to_date_doc, num_phone.replace("+7", ""), cli_role, num_pass, "JUR", res, sex_fl, res_country, nameCom, name_dep, dateReg, "CODE_CLIENT"));
        ArrayList result_jur = con_data.CallSqlProcedure(null, "CreateClientCardJava", result_jur_lst, BdUrl, uiLogin, uiPassword);

        // проверяем, что карточка юр лица создалась
        String check_jur_select = String.format("SELECT COUNT(TAXCODE) FROM G_CLIHST g WHERE TAXCODE = '%s'", code_inn);
        ArrayList<ArrayList> check_jur_result = ConnectionData.executeQuerySelect(check_jur_select, BdUrl, uiLogin, uiPassword);
        if (check_jur_result.get(0).get(0).equals("1.0")) {
            System.out.println(String.format("По ИИН - '%s' создана карточка юр лица", code_inn));
            // удаляем данный ИИН из датасета, чтобы не использовать его второй раз
            String dlt_crd_query = String.format("DELETE FROM TESTINGIINMANUALJUR where IIN = '%s'", code_inn);
            String dlt_iin_fl = String.format("DELETE FROM TESTINGIINMANUALFL where IIN = '%s'", iin_fl);
            ConnectionData.executeQueryDML(dlt_crd_query, BdUrl, uiLogin, uiPassword);
            ConnectionData.executeQueryDML(dlt_iin_fl, BdUrl, uiLogin, uiPassword);

            // создаем документ с образцом подписи/печати юр лица
            // для того, чтобы можно было проводить платежные документы
            // под этим тестовым клиентом
            CreateDocDos doc = new CreateDocDos();
            String cli_code = doc.CreateDoc((String) result_jur.get(0), "696", null,"Образец подписи физического лица/печати юридического лица",
                    "01.01.18", "1234975", "1", "1", "01.01.30", "Черт его знает",
                    "Для автотестов", "0", null, "1", null, ".jpg", "0", BdUrl, uiLogin, uiPassword);
            // Добавляем ПКБ отчет
            pkb.AddCliPkbRep((String) result_jur.get(0), BdUrl, uiLogin, uiPassword);
        } else if (check_jur_result.get(0).get(0).equals("0.0")) {
            throw new ErrorExecutingScript(String.format("По ИИН - '%s' не создана карточка юр лица", code_inn));
        } else {
            throw new ErrorExecutingScript(String.format("По ИИН - '%s' созданы больше одной карточки юр лица", code_inn));
        }
        return (String) result_jur.get(0);

    }
    public String CreateClientCardFl(String code_inn, String res, String type_doc, String cli_role, String name, String surName, String middlename,
                                     String sex, String name_dep, String birthday, String BdUrl, String uiLogin, String uiPassword) throws Exception {
        // метод создает карточку юр лица
        // в данном методе также вызывается метод для создания физ лица
        // так как, чтобы создать юрика, необходимо подвязать ему уполномоченное физ лицо
        // в метод передаются:
        // code_inn - ИИН юрика
        // res - резидентство юрика
        // type_doc - тип документа уд личность (применяетс при создании уполн-го физ лица
        // cli_role роль физ лица
        // name - имя физ лица
        // surName - фамилия физ лица
        // nameCom - название юр лица
        // sex - пол физ лица
        // full_name - ФИО физ лица
        // name_dep - код департамента
        // birthday - дата рождения физ лица
        // dateReg - дата регистрации юр лица

        GenerateNumbers GetNum = new GenerateNumbers();
        ChangeDate cd = new ChangeDate();
        ConnectionData con_data = new ConnectionData();
        CreatePKBReport pkb = new CreatePKBReport();
        ArrayList<ArrayList> iin_db_fl = null;
        // получаем системную дату из базы
        String select_od = "SELECT TRUNC(SYSDATE) FROM Dual";
        ArrayList<ArrayList> date_od_lst = ConnectionData.executeQuerySelect(select_od, BdUrl, uiLogin, uiPassword);
        String oper_day = (String) date_od_lst.get(0).get(0);
        String num_phone = GetNum.GenPhoneNumber(BdUrl, uiLogin, uiPassword);
        String fio = String.format("%s %s %s", surName, name, middlename);

        String num_pass = GetNum.GenDocNum(BdUrl, uiLogin, uiPassword);
        String id_main = GetNum.GetIdRecord(BdUrl, uiLogin, uiPassword);
        String from_date_doc = cd.FutureLastDate(oper_day.replace(".", ""), "year", -5, true);
        String to_date_doc = cd.FutureLastDate(oper_day.replace(".", ""), "year", 5, true);
        String redister_date = String.format("%s-%s-%s", oper_day.substring(6), oper_day.substring(3, 5), oper_day.substring(0, 2));

        String res_country = null;
        if (res == "1") {
            res_country = "KZ";
        } else {
            res_country = "RU";
        }

        String select_count_record = "SELECT DISTINCT COUNT(iin) FROM TESTINGIINMANUALFL";
        ArrayList<ArrayList> lst_count_record = ConnectionData.executeQuerySelect(select_count_record, BdUrl, uiLogin, uiPassword);
        int cnt_record = (int) Double.parseDouble((String) lst_count_record.get(0).get(0));

        ArrayList<String> cnt_prm = new ArrayList<>();

        String selectUpDateNKFL = String.format("insert into g_iin (RNN, IIN, JURFL, RESIDFL, CONSTFL, INDIVIDFL, NAME, FIO, LASTREGEND, REASONDEREG, " +
                "NOTARY_LAWYER, INACTIVE, TAXDEP_ID, CORRECTDT) " +
                "values('', '%s', '0', '%s', '1', '0', '%s', '%s', '', '', '', '0', '5238', '')", code_inn, res, fio, fio);
        ConnectionData.executeQueryDML(selectUpDateNKFL, BdUrl, uiLogin, uiPassword);

        //ArrayList lst_prm = new ArrayList<>(Arrays.asList(iin_fl, type_doc, from_date_doc, to_date_doc, num_phone, cli_role, num_pass, "FL", resident, fio, sex_fl, res_country, name_dep, birthdate));
        ArrayList lst_prm = new ArrayList<>(Arrays.asList(code_inn, type_doc, from_date_doc, to_date_doc, num_phone.replace("+7", ""), cli_role, num_pass, "FL", res, sex, res_country, fio, name_dep, birthday, "CODE_CLIENT"));
        ArrayList result_fl = con_data.CallSqlProcedure(null, "CreateClientCardJava", lst_prm, BdUrl, uiLogin, uiPassword);

        // проверяем, что карточка физ лица создалась
        String check_jur_select = String.format("SELECT COUNT(TAXCODE) FROM G_CLIHST g WHERE TAXCODE = '%s'", code_inn);
        ArrayList<ArrayList> check_jur_result = ConnectionData.executeQuerySelect(check_jur_select, BdUrl, uiLogin, uiPassword);
        if (check_jur_result.get(0).get(0).equals("1.0")) {
            System.out.println(String.format("По ИИН - '%s' создана карточка физ лица", code_inn));
            // удаляем данный ИИН из датасета, чтобы не использовать его второй раз
            String dlt_iin_fl = String.format("DELETE FROM TESTINGIINMANUALFL where IIN = '%s'", code_inn);
            ConnectionData.executeQueryDML(dlt_iin_fl, BdUrl, uiLogin, uiPassword);
            // Добавляем ПКБ отчет
            pkb.AddCliPkbRep((String) result_fl.get(0), BdUrl, uiLogin, uiPassword);
        } else if (check_jur_result.get(0).get(0).equals("0.0")) {
            throw new ErrorExecutingScript(String.format("По ИИН - '%s' не создана карточка физ лица", code_inn));
        } else {
            throw new ErrorExecutingScript(String.format("По ИИН - '%s' созданы больше одной карточки физ лица", code_inn));
        }
        return (String) result_fl.get(0);

    }
    public String CreateClientCardIp(String code_inn, String res, String sex, String type_doc, String cliRole, String name, String surname,
                                     String middlename, String nameIp, String name_dep, String birthDate, String dateReg, String BdUrl, String uiLogin, String uiPassword) throws Exception {
        // метод создает карточку юр лица
        // в данном методе также вызывается метод для создания физ лица
        // так как, чтобы создать юрика, необходимо подвязать ему уполномоченное физ лицо
        // в метод передаются:
        // code_inn - ИИН юрика
        // res - резидентство юрика
        // type_doc - тип документа уд личность (применяетс при создании уполн-го физ лица
        // cli_role роль физ лица
        // name - имя физ лица
        // surName - фамилия физ лица
        // nameCom - название юр лица
        // sex - пол физ лица
        // full_name - ФИО физ лица
        // name_dep - код департамента
        // birthday - дата рождения физ лица
        // dateReg - дата регистрации юр лица

        GenerateNumbers GetNum = new GenerateNumbers();
        ChangeDate cd = new ChangeDate();
        ConnectionData con_data = new ConnectionData();
        CreatePKBReport pkb = new CreatePKBReport();
        // получаем системную дату из базы
        String select_od = "SELECT TRUNC(SYSDATE) FROM Dual";
        ArrayList<ArrayList> date_od_lst = ConnectionData.executeQuerySelect(select_od, BdUrl, uiLogin, uiPassword);
        String oper_day = (String) date_od_lst.get(0).get(0);
        String fio = String.format("%s %s %s", surname, name, middlename);

        String num_phone = GetNum.GenPhoneNumber(BdUrl, uiLogin, uiPassword);

        String num_pass = GetNum.GenDocNum(BdUrl, uiLogin, uiPassword);
        String id_main = GetNum.GetIdRecord(BdUrl, uiLogin, uiPassword);
        String from_date_doc = cd.FutureLastDate(oper_day.replace(".", ""), "year", -5, true);
        String to_date_doc = cd.FutureLastDate(oper_day.replace(".", ""), "year", 5, true);
        String redister_date = String.format("%s-%s-%s", oper_day.substring(6), oper_day.substring(3, 5), oper_day.substring(0, 2));

        String res_country = null;
        String codeCountry = null;
        String nation_code = null;
        String nation = null;
        if (res == "1") {
            codeCountry = "398";
            res_country = "KZ";
            nation_code = "005";
            if (sex == "M") {
                nation = "КАЗАХ";
            } else {
                nation = "КАЗАШКА";
            }
        } else if (res == "0") {
            codeCountry = "643";
            res_country = "RU";
            nation_code = "001";
            if (sex == "M") {
                nation = "РУССКИЙ";
            } else {
                nation = "РУССКАЯ";
            }
        }
        // получаем максимальный id таблицы mca_gbdl и прибавляем 1, чтобы id был уникальный
        // иначе будет ошибка ограничения уникальности при инсерте
        String select_max_id = String.format("SELECT MAX(ID) FROM Z_077_MCA_GBDL_DATA_RESPONCE", code_inn);
        ArrayList<ArrayList> get_max_id = ConnectionData.executeQuerySelect(select_max_id, BdUrl, uiLogin, uiPassword);
        int maxId = (int) Double.parseDouble(String.valueOf(get_max_id.get(0).get(0)));
        System.out.println(maxId);
        // проверяем, что данного ИИНа нет в g_iin (База налогоплательщиков) и g_clihst (База банка)
        String select_check_db = String.format("SELECT COUNT(*) FROM G_CLIHST WHERE TAXCODE = '%s'", code_inn);
        ArrayList<ArrayList> result_db_lst = ConnectionData.executeQuerySelect(select_check_db, BdUrl, uiLogin, uiPassword);

        // если вернулось значение больше 0, то клиент с данным ИИН уже есть в БД
        if (result_db_lst.get(0).get(0).equals("0.0")) {
            // проверяем, что в g_iin нет записи с данным ИИН
            String select_check_g_iin = String.format("SELECT COUNT(*) FROM G_IIN WHERE IIN = '%s'", code_inn);
            ArrayList<ArrayList> result_g_iin_lst = ConnectionData.executeQuerySelect(select_check_g_iin, BdUrl, uiLogin, uiPassword);
            if (result_g_iin_lst.get(0).get(0).equals("0.0")) {

                // добавляем данный ИИН в БД налогоплательщиков G_IIN
                String insertGIin = String.format("insert into g_iin (RNN, IIN, JURFL, RESIDFL, CONSTFL, INDIVIDFL, NAME, FIO, LASTREGEND, REASONDEREG, " +
                        "NOTARY_LAWYER, INACTIVE, TAXDEP_ID, CORRECTDT) " +
                        "values('', '%s', '0', '%s', '1', '1', '%s', '%s', '', '', '0', '0', '5238', '')", code_inn, res, nameIp, fio);
                ConnectionData.executeQueryDML(insertGIin, BdUrl, uiLogin, uiPassword);
                // добавляем запись в StatEgov
                // оттуда берутся данные дл ОКВЭД
                String insert_iin_StatEgovLog = String.format("insert into z_077_StatEgovLog(IBIN, ILANG, XRES, CALLDATE, RES, TYPE, STATUS, BINIIN, CLIN, REGISTERDATE, OKEDCODE, OKEDNAME, SECONDOKEDS, KRPCODE, KRPNAME, KRPBFCODE, KRPBFNAME, KATOCODE,  KATOID,  KATOADDRESS, FIO, IP) " +
                        "values('%s','RU','', '%s', '%s', 'Ok',  'true',  '%s',  '%s',  '%s',  '46213', 'Оптовая торговля масличными культурами', '', '105', 'Малые предприятия (<= 5)',  '105', 'Малые предприятия (<= 5)',  '751210000', '268025',  'Г.АЛМАТЫ, АЛАТАУСКИЙ РАЙОН, Микрорайон Карасу, улица Центральная, дом 1', '%s %s %s',  'false')", code_inn, oper_day, res, code_inn, nameIp, redister_date, surname, name, middlename);
                ConnectionData.executeQueryDML(insert_iin_StatEgovLog, BdUrl, uiLogin, uiPassword);
                // добавляем данные в mca_gbdl
                String insertIinGbdl = String.format("insert into Z_077_MCA_GBDL_DATA_RESPONCE (IIN, XMLMSG, MSGID, MSGCODE, MSGRESOINSEDATE, " +
                                "CLIIIN, CLISURNAME, CLINAME, CLIPATRONYMIC, CLIBIRTHDATE, CLIDEATHDATE, CLIGENDERCODE, " +
                                "CLINATIONALCODE, CLINATIONAL, CLIСITIZENSHIPCODE, CLIСITIZENSHIP, CLILIFESTATUSCODE, CLILIFESTATUS, " +
                                "CLIBPCOUNTRYCODE, CLIBPCOUNTRY, CLIREGSTATUSCODE, CLIREGCOUNTRYCODE, CLIREGCOUNTRY, CLIREGDDISTRICTCODE, " +
                                "CLIREGDISTRICT, CLIREGDREGIONCODE, CLIREGREGION, CLIREGCITY, CLIREGSTREET, CLIREGBUILDING, CLIREGCORPUS, " +
                                "CLIREGFLAT, DOCTYPE, DOCTYPECODE, DOCNUMBER, DOCSERIES, DOCBEGINDATE, DOCENDDATE, DOCISSUEORG, DOCSTATUS, " +
                                "DOCCODE, CLICAPABLESTATUS, CLIMISSINGSTATUS, CLIMISSINGENDDATE, CLIEXCLUDEREASON, CLIDISAPPEAR, " +
                                "CLIADDSTATUSCODE, CLIADDCOUNTRYCODE, CLIADDCOUNTRY, CLIADDDDISTRICTCODE, CLIADDDISTRICT, CLIADDDREGIONCODE, " +
                                "CLIADDREGION, CLIADDCITY, CLIADDSTREET, CLIADDBUILDING, CLIADDCORPUS, CLIADDFLAT, RESPONSERESULT, " +
                                "RESPONSEDATE, ID, CLILATSURNAME, CLILATNAME, TOKEN, PUBLICKEY, XMLKDP, XMLGOVGBD, CLIREGFULLADDR) " +
                                "values('%s', '', '', 'SCSS001', '%s', '%s', '%s', " +
                                "'%s', '%s', '%s', null, '%s', '%s', '%s', '%s', " +
                                "'%s', '0', 'Нормальный', '%s', '%s', '1', '%s', " +
                                "'%s', '1907', 'АЛМАТИНСКАЯ', '1907211', 'ИЛИЙСКИЙ РАЙОН', 'Казциковский, Казцик', " +
                                "'УЛИЦА В.Г.Гиль', '45', null, '2', null, null, null, null, null, null, null, null, null, null, null, " +
                                "null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, " +
                                "'%s', %d, '', '', null, null, null, null, null)", code_inn, oper_day, code_inn,
                        surname, name, middlename, birthDate, sex, nation_code, nation, codeCountry, res_country, codeCountry, res_country,
                        codeCountry, res_country, oper_day, maxId + 1);
                ConnectionData.executeQueryDML(insertIinGbdl, BdUrl, uiLogin, uiPassword);

                // если ИИН нигде не фигурирует, то создаем на него карточку физ лица
                // картчку физ лица необходимо создать перед созданием карточки ИП
                System.out.println(fio);
                ArrayList lstPrmFl = new ArrayList<>(Arrays.asList(code_inn, type_doc, from_date_doc, to_date_doc, num_phone.replace("+7", ""), cliRole, num_pass, "FL", res, sex, res_country, fio, name_dep, birthDate, "CODE_CLIENT"));
                ArrayList createFl = con_data.CallSqlProcedure(null, "CreateClientCardJava", lstPrmFl, BdUrl, uiLogin, uiPassword);

                // создаем карточку ИП
                ArrayList lstPrmIp = new ArrayList<>(Arrays.asList(code_inn, type_doc, from_date_doc, to_date_doc, num_phone.replace("+7", ""), cliRole, num_pass, "PBOYUL", res, sex, res_country, nameIp, name_dep, dateReg, "CODE_CLIENT"));
                ArrayList createIp = con_data.CallSqlProcedure(null, "CreateClientCardJava", lstPrmIp, BdUrl, uiLogin, uiPassword);

                // проверяем, что карточка юр лица создалась
                String check_jur_select = String.format("SELECT COUNT(TAXCODE) FROM G_CLIHST WHERE TAXCODE = '%s' and typefl = '2'", code_inn);
                ArrayList<ArrayList> check_jur_result = ConnectionData.executeQuerySelect(check_jur_select, BdUrl, uiLogin, uiPassword);
                if (check_jur_result.get(0).get(0).equals("1.0")) {
                    System.out.println(String.format("По ИИН - '%s' создана карточка ИП", code_inn));
                    // удаляем данный ИИН из датасета, чтобы не использовать его второй раз
                    String dlt_iin_fl = String.format("DELETE FROM TESTINGIINMANUALFL where IIN = '%s'", code_inn);
                    ConnectionData.executeQueryDML(dlt_iin_fl, BdUrl, uiLogin, uiPassword);
                    // Добавляем ПКБ отчет
                    pkb.AddCliPkbRep((String) createIp.get(0), BdUrl, uiLogin, uiPassword);
                } else if (check_jur_result.get(0).get(0).equals("0.0")) {
                    throw new ErrorExecutingScript(String.format("По ИИН - '%s' не создана карточка юр лица", code_inn));
                } else {
                    throw new ErrorExecutingScript(String.format("По ИИН - '%s' созданы больше одной карточки юр лица", code_inn));
                }
                return (String) createIp.get(0);

            } else {
                throw new ErrorExecutingScript(String.format("ИИН - '%s' уже существует в G_IIN", code_inn));
            }

        } else {
            throw new ErrorExecutingScript(String.format("ИИН - '%s' уже существует в БД", code_inn));
        }
    }
    public String CreateClientCardIpS(String code_inn, String res, String type_doc, String cli_role, String nameCom,
                                      String name_dep, String dateReg, String BdUrl, String uiLogin, String uiPassword) throws Exception {
        // метод создает карточку юр лица
        // в данном методе также вызывается метод для создания физ лица
        // так как, чтобы создать юрика, необходимо подвязать ему уполномоченное физ лицо
        // в метод передаются:
        // code_inn - ИИН юрика
        // res - резидентство юрика
        // type_doc - тип документа уд личность (применяетс при создании уполн-го физ лица
        // cli_role роль физ лица
        // name - имя физ лица
        // surName - фамилия физ лица
        // nameCom - название юр лица
        // sex - пол физ лица
        // full_name - ФИО физ лица
        // name_dep - код департамента
        // birthday - дата рождения физ лица
        // dateReg - дата регистрации юр лица

        GenerateNumbers GetNum = new GenerateNumbers();
        ChangeDate cd = new ChangeDate();
        ConnectionData con_data = new ConnectionData();
        CreatePKBReport pkb = new CreatePKBReport();
        ArrayList<ArrayList> iin_db_fl = null;
        // получаем системную дату из базы
        String select_od = "SELECT TRUNC(SYSDATE) FROM Dual";
        ArrayList<ArrayList> date_od_lst = ConnectionData.executeQuerySelect(select_od, BdUrl, uiLogin, uiPassword);
        String oper_day = (String) date_od_lst.get(0).get(0);

        String num_phone = GetNum.GenPhoneNumber(BdUrl, uiLogin, uiPassword);

        String num_pass = GetNum.GenDocNum(BdUrl, uiLogin, uiPassword);
        String id_main = GetNum.GetIdRecord(BdUrl, uiLogin, uiPassword);
        String from_date_doc = cd.FutureLastDate(oper_day.replace(".", ""), "year", -5, true);
        String to_date_doc = cd.FutureLastDate(oper_day.replace(".", ""), "year", 5, true);
        String redister_date = String.format("%s-%s-%s", oper_day.substring(6), oper_day.substring(3, 5), oper_day.substring(0, 2));

        String res_country = null;
        String grCountry = null;
        String codeCountry = null;
        String nameCountry = null;
        String codeNatioan = null;
        if (res == "1") {
            grCountry = "Гражданин РК";
            codeCountry = "398";
            nameCountry = "КАЗАХСТАН";
            res_country = "KZ";
            codeNatioan = "1";
        } else if (res == "0") {
            grCountry = "Иностранный гражданин";
            codeCountry = "643";
            nameCountry = "РОССИЯ";
            res_country = "RU";
            codeNatioan = "2";
        }

        // проверяем, что данного ИИНа нет в g_iin (База налогоплательщиков) и g_clihst (База банка)
        // проверка будет происходить в цикле, чтобы каждый раз брался новый ИИН
        // для начала определяем количество уникальных ИИН в датасете
        // чтобы использовать, как конечную итерацию цикла
        String select_count_record = "SELECT DISTINCT COUNT(iin) FROM TESTINGIINMANUALFL";
        ArrayList<ArrayList> lst_count_record = ConnectionData.executeQuerySelect(select_count_record, BdUrl, uiLogin, uiPassword);
        int cnt_record = (int) Double.parseDouble((String) lst_count_record.get(0).get(0));

        ArrayList<String> cnt_prm = new ArrayList<>();
        // запускаем цикл for
        for (int i = 0; i < cnt_record; i++) {
            // получаем тестовый ИИН физ лица из датасета
            String select_iin_db_fl = String.format("SELECT * FROM TESTINGIINMANUALFL WHERE RESIDENT = '%s' and rownum = 1", res);
            iin_db_fl = ConnectionData.executeQuerySelect(select_iin_db_fl, BdUrl, uiLogin, uiPassword);
            // проверяем, что данного ИИН нет в базе налогоплательщиков
            String select_g_iin = String.format("SELECT COUNT(IIN) FROM G_IIN WHERE IIN = '%s'", (String) iin_db_fl.get(0).get(0));
            ArrayList<ArrayList> result_g_iin_lst = ConnectionData.executeQuerySelect(select_g_iin, BdUrl, uiLogin, uiPassword);

            if (result_g_iin_lst.get(0).get(0).equals("0.0")) {
                System.out.println(String.format("Полученный ИИН - %s, не найден в базе налогоплательщиков (G_IIN)", iin_db_fl.get(0).get(0)));
                // если ИИН не найден в базе налогоплательщика, то проверяем
                // есть ли карточка клиента в базе
                String select_g_clihst = String.format("SELECT COUNT(TAXCODE) FROM G_CLIHST WHERE TAXCODE = '%s'", (String) iin_db_fl.get(0).get(0));
                ArrayList<ArrayList> result_g_clihst_lst = ConnectionData.executeQuerySelect(select_g_clihst, BdUrl, uiLogin, uiPassword);
                if (result_g_clihst_lst.get(0).get(0).equals("0.0")) {
                    System.out.println(String.format("Полученный ИИН - %s, не найден в базе банка (G_CLIHST)", iin_db_fl.get(0).get(0)));
                    System.out.println(String.format("ИИН - %s можно использовать для создания карточки клиента", iin_db_fl.get(0).get(0)));
                    break;
                } else {
                    System.out.println(String.format("В БД банка (G_CLIHST) уже есть запись с ИИН - '%s'", iin_db_fl.get(0).get(0)));
                }
            } else {
                // если ИИН есть в базе, то пропускаем данную итерацию
                // параллельно удаляем ИИН из датасета
                System.out.println(String.format("В базе налогоплательщиков уже существует запись с ИИН - '%s'", iin_db_fl.get(0).get(0)));
                String dlt_select = String.format("DELETE FROM TESTINGIINMANUALFL WHERE IIN = '%s'", (String) iin_db_fl.get(0).get(0));
                ConnectionData.executeQueryDML(dlt_select, BdUrl, uiLogin, uiPassword);
            }

        }

        String iin_fl = (String) iin_db_fl.get(0).get(0);
        String sex_fl = (String) iin_db_fl.get(0).get(1);
        String birthdate = (String) iin_db_fl.get(0).get(2);
        String resident = (String) iin_db_fl.get(0).get(3);
        String surname = (String) iin_db_fl.get(0).get(4);
        String name_fl = (String) iin_db_fl.get(0).get(5);
        String middlename = (String) iin_db_fl.get(0).get(6);
        String fio = String.format("%s %s %s", surname, name_fl, middlename);

        if (iin_db_fl.get(0).get(3).equals("0.0")) {
            String sexFL = "M";
        } else if (iin_db_fl.get(0).get(3).equals("1.0")) {
            String sexFL = (String) iin_db_fl.get(0).get(5);
        }
        String selectUpDateNKFL = String.format("insert into g_iin (RNN, IIN, JURFL, RESIDFL, CONSTFL, INDIVIDFL, NAME, FIO, LASTREGEND, REASONDEREG, " +
                "NOTARY_LAWYER, INACTIVE, TAXDEP_ID, CORRECTDT) " +
                "values('', '%s', '0', '%s', '1', '0', '%s', '%s', '', '', '', '0', '5238', '')", iin_fl, resident, fio, fio);
        ConnectionData.executeQueryDML(selectUpDateNKFL, BdUrl, uiLogin, uiPassword);

        //ArrayList lst_prm = new ArrayList<>(Arrays.asList(iin_fl, type_doc, from_date_doc, to_date_doc, num_phone, cli_role, num_pass, "FL", resident, fio, sex_fl, res_country, name_dep, birthdate));
        ArrayList lst_prm = new ArrayList<>(Arrays.asList(iin_fl, type_doc, from_date_doc, to_date_doc, num_phone.replace("+7", ""), "CLI", num_pass, "FL", resident, sex_fl, res_country, fio, name_dep, birthdate, "CODE_CLIENT"));
        ArrayList result = con_data.CallSqlProcedure(null, "CreateClientCardJava", lst_prm, BdUrl, uiLogin, uiPassword);


        String selectUpDateNK = String.format("insert into g_iin (RNN, IIN, JURFL, RESIDFL, CONSTFL, INDIVIDFL, NAME, FIO, LASTREGEND, REASONDEREG, " +
                "NOTARY_LAWYER, INACTIVE, TAXDEP_ID, CORRECTDT) " +
                "values('', '%s', '0', '%s', '0', '0', '%s', '%s', '', '', '', '0', '5238', '')", code_inn, res, nameCom.replace("'", "\""), nameCom.replace("'", "\""));
        ConnectionData.executeQueryDML(selectUpDateNK, BdUrl, uiLogin, uiPassword);

        String select_iin_main = String.format("insert into z_077_ent_gbdul_main(id_gbdul_main, BIN, REQUESTDATE, XML_DATA, REGSTATUSCODE, REGSTATUSNAME, REGDEPARTCODE, REGDEPARTNAME, REGDATE, REGLASTDATE, FULLNAMERU, FULLNAMEKZ, FULLNAMELAT, SHORTNAMERU, SHORTNAMEKZ, SHORTNAMELAT, ORGFORMCODE, ORGFORMNAME, FORMOFLAWCODE, FORMOFLAWNAME, EPRISETYPECODE, EPRISETYPENAME, TAXORGSTATUS, CREATIONMETHODCODE, CREATIONMETHODNAME, PROPERTYTYPECODE, PROPERTYTYPENAME, TYPICALCHARACTER, COMMERCEORG, ENTERPRISESUBJECT, AFFILIATED, INTERNATIONAL, FOREIGNINVEST, ONECITIZENSHIP, BRANCHESEXISTS, ACTIVITYOKEDCODE, ACTIVITYOKEDNAME, ADDRESSZIPCODE, ADDRESSKATO, ADDRESSDISTRICT, ADDRESSREGION, ADDRESSCITY, ADDRESSSTREET, ADDRESSBUILDING, FOUNDRESCOUNT, FOUNDERSCOUNTFL, FOUNDERSCOUNTUL, ORGSIZECODE, ORGSIZENAME, STATCOMOKEDCODE, STATCOMOKEDNAME, ACTIVITYATTRCODE, ACTIVITYATTRNAME) " +
                "values ('%d', '%s', '%s', '01.01.1990', null, null, null, null, null, null, null, null, null, null, null, null, null, null, '50', null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, '050000', '751210000', null, null, null, 'улица Центральная', '1', null, null, null, null, null, null, null, null, null)", Integer.parseInt(id_main), code_inn, oper_day);
        ConnectionData.executeQueryDML(select_iin_main, BdUrl, uiLogin, uiPassword);
        String select_iin_leaders = String.format("insert into z_077_ent_gbdul_leaders(ID_GBDUL_MAIN, ORG_BIN, COUNTRYCODE, COUNTRYNAME, CITIZENCOUNTRYCODE, CITIZENCOUNTRYNAME, NATIOANLITYCODE, NATIONALITYNAME, IIN, SURNAME, NAME, MIDDLENAME) " +
                "values('%d', '%s', '%s', '%s', '%s', '%s', '%s', '%s', '%s', '%s', '%s', '%s')", Integer.parseInt(id_main), code_inn, codeCountry, nameCountry, codeCountry, nameCountry, codeNatioan, grCountry, iin_fl, iin_db_fl.get(0).get(4), iin_db_fl.get(0).get(5), iin_db_fl.get(0).get(6));

        ConnectionData.executeQueryDML(select_iin_leaders, BdUrl, uiLogin, uiPassword);

        String select_iin_founders = String.format("insert into z_077_ent_gbdul_founders(ID_GBDUL_MAIN, ORGBIN, FOUNDERCOUNTRYCODE, FOUNDERSCOUNTRYNAME, FOUNDERSNATIONCODE, FOUNDERSNATIONNAME, FOUNDERSCITIZENCODE, FOUNDERSCITIZENNAME, FOUNDERSIIN, FOUNDERSSURNAME, FOUNDERSNAME, FOUNDERSMIDDLENAME, FOUNDERSREGNUMBER, FOUNDERSREGDATE, FOUNDERSORGFULLNAMERU, FOUNDERSORGFULLNAMEKZ) " +
                "values('%d', '%s', '%s', '%s', '%s', '%s', '%s', '%s', '%s', '%s', '%s', '%s', '', '', '', '')", Integer.parseInt(id_main), code_inn, codeCountry, nameCountry, codeNatioan, grCountry, codeCountry, nameCountry, iin_fl, iin_db_fl.get(0).get(4), iin_db_fl.get(0).get(5), iin_db_fl.get(0).get(6));
        ConnectionData.executeQueryDML(select_iin_founders, BdUrl, uiLogin, uiPassword);
        String select_iin_StatEgovLog = String.format("insert into z_077_StatEgovLog(IBIN, ILANG, XRES, CALLDATE, RES, TYPE, STATUS, BINIIN, CLIN, REGISTERDATE, OKEDCODE, OKEDNAME, SECONDOKEDS, KRPCODE, KRPNAME, KRPBFCODE, KRPBFNAME, KATOCODE,  KATOID,  KATOADDRESS, FIO, IP) " +
                "values('%s','RU','', '%s', '%s', 'Ok',  'true',  '%s',  '%s',  '%s',  '46213', 'Оптовая торговля масличными культурами', '', '105', 'Малые предприятия (<= 5)',  '105', 'Малые предприятия (<= 5)',  '751210000', '268025',  'Г.АЛМАТЫ, АЛАТАУСКИЙ РАЙОН, Микрорайон Карасу, улица Центральная, дом 1', '%s %s %s',  'false')", code_inn, oper_day, res, code_inn, nameCom.replace("'", "\""), redister_date, iin_db_fl.get(0).get(4), iin_db_fl.get(0).get(5), iin_db_fl.get(0).get(6));
        ConnectionData.executeQueryDML(select_iin_StatEgovLog, BdUrl, uiLogin, uiPassword);

        ArrayList result_jur_lst = new ArrayList<>(Arrays.asList(code_inn, type_doc, from_date_doc, to_date_doc, num_phone.replace("+7", ""), cli_role, num_pass, "PBOYULS", res, sex_fl, res_country, nameCom, name_dep, dateReg, "CODE_CLIENT"));
        ArrayList result_jur = con_data.CallSqlProcedure(null, "CreateClientCardJava", result_jur_lst, BdUrl, uiLogin, uiPassword);

        // проверяем, что карточка юр лица создалась
        String check_jur_select = String.format("SELECT COUNT(TAXCODE) FROM G_CLIHST g WHERE TAXCODE = '%s'", code_inn);
        ArrayList<ArrayList> check_jur_result = ConnectionData.executeQuerySelect(check_jur_select, BdUrl, uiLogin, uiPassword);
        if (check_jur_result.get(0).get(0).equals("1.0")) {
            System.out.println(String.format("По ИИН - '%s' создана карточка совместного ИП", code_inn));
            // удаляем данный ИИН из датасета, чтобы не использовать его второй раз
            String dlt_crd_query = String.format("DELETE FROM TESTINGIINMANUALJUR where IIN = '%s'", code_inn);
            String dlt_iin_fl = String.format("DELETE FROM TESTINGIINMANUALFL where IIN = '%s'", iin_fl);
            ConnectionData.executeQueryDML(dlt_crd_query, BdUrl, uiLogin, uiPassword);
            ConnectionData.executeQueryDML(dlt_iin_fl, BdUrl, uiLogin, uiPassword);
            // Добавляем ПКБ отчет
            pkb.AddCliPkbRep((String) result_jur.get(0), BdUrl, uiLogin, uiPassword);
        } else if (check_jur_result.get(0).get(0).equals("0.0")) {
            throw new ErrorExecutingScript(String.format("По ИИН - '%s' не создана карточка совместного ИП", code_inn));
        } else {
            throw new ErrorExecutingScript(String.format("По ИИН - '%s' созданы больше одной карточки совместного ИП", code_inn));
        }
        return (String) result_jur.get(0);

    }
}
