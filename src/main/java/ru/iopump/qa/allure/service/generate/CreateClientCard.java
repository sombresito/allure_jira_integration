package ru.iopump.qa.allure.service.generate;

import java.text.ParseException;
import java.util.*;
import java.sql.*;
import ru.iopump.qa.allure.service.generate.ConnectionData;
import ru.iopump.qa.allure.service.generate.ErrorExecutingScript;
import ru.iopump.qa.allure.service.generate.CreateClientCardJurFL;
public class CreateClientCard {


    public static void main(String args[]) throws Exception {
        // Пример тестовых значений
        String inn = "190440128562";
        String type_doc = "УЛ";
        String cli_role = "CLI"; // пример URL подключения
        String cli_type = "JUR";
        String resident = "1";
        String nameCom = "Федосеев";
        String nameIp = "";
        String sex = "M";
        String name = "Name";
        String surname = "Surname";
        String middlename = "Middlename";
        String dep_code = "CNT";
        String birthdate = "01042019";
        String dateReg = "01042019";
        String selectedDbUrl = "jdbc"; // пример URL подключения
        String dbUser = "your_username";
        String dbPass = "your_password";

        try {
            String cli_code = CreateClientCard(inn, type_doc, cli_role,cli_type, resident,
                    nameCom, nameIp, sex, name, surname, middlename, dep_code, birthdate, dateReg, selectedDbUrl, dbUser, dbPass);
            System.out.println(cli_code);
        } catch (ErrorExecutingScript error) {
            error.printStackTrace();
        }
    }
    public static String CreateClientCard(String iin, String type_doc, String cli_role,
                                          String cli_type, String resident, String nameCom, String nameIp,
                                          String sex, String name, String surname,
                                          String middlename, String dep_code, String birthdate, String dateReg, String BdUrl,
                                          String uiLogin, String uiPassword) throws Exception {
        // метод создает карточку клиента
        ConnectionData cd = new ConnectionData();
        CreateClientCardJurFL cc = new CreateClientCardJurFL();
        String cli_code = null;

        // проверяем, что  ИИН не существует в базе налогоплательщика
        String select_g_iin = String.format("SELECT COUNT(IIN) FROM G_IIN WHERE IIN = '%s'", iin);
        ArrayList<ArrayList> check_g_iin = ConnectionData.executeQuerySelect(select_g_iin, BdUrl, uiLogin, uiPassword);

        if (check_g_iin.get(0).get(0).equals("0.0")) {
            System.out.println(String.format("ИИН - '%s' не существует в базе налогоплательщиков", iin));
            // проверяем, что карточка с аналогичным ИИН еще не создана в БД
            String check_card_before = String.format("SELECT COUNT(TAXCODE) FROM G_CLIHST WHERE TAXCODE = '%s'", iin);
            ArrayList<ArrayList> check_card_before_query = ConnectionData.executeQuerySelect(check_card_before, BdUrl, uiLogin, uiPassword);
            if (check_card_before_query.get(0).get(0).equals("0.0")) {
                System.out.println(String.format("По ИИН - %s карточка еще не создана в G_CLI", iin));
                // переходим к созданию карточки
                if (cli_type == "JUR") {
                    cli_code = cc.CreateClientCardJur(iin, resident, type_doc, cli_role, nameCom, dep_code, birthdate, BdUrl, uiLogin, uiPassword);
                } else if (cli_type == "FL") {
                    cli_code = cc.CreateClientCardFl(iin, resident, type_doc, cli_role, name, surname, middlename, sex, dep_code, birthdate, BdUrl, uiLogin, uiPassword);
                } else if (cli_type == "PBOYUL") {
                    cli_code = cc.CreateClientCardIp(iin, resident, sex, type_doc, cli_role, name, surname, middlename, nameIp, dep_code, birthdate, dateReg, BdUrl, uiLogin, uiPassword);
                } else if (cli_type == "PBOYULS") {
                    cli_code = cc.CreateClientCardIpS(iin, resident, type_doc, cli_role, nameIp, dep_code, dateReg, BdUrl, uiLogin, uiPassword);
                } else {
                    throw new ErrorExecutingScript(String.format("Передан неверный тип клиента - '%s'", cli_type));
                }
                // проверяем, что карточка создалась
            } else {
                throw new ErrorExecutingScript(String.format("По ИИН - %s уже существует карточка в G_CLIHST", iin));
            }
        } else {
            throw new ErrorExecutingScript(String.format("В базе налогоплательщиков существует ИИН - '%s'", iin));
        }
        return cli_code;
    }
}
