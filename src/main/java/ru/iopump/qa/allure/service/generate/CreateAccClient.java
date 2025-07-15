package ru.iopump.qa.allure.service.generate;

import java.text.ParseException;
import java.util.*;
import java.sql.*;
import ru.iopump.qa.allure.service.generate.ConnectionData;
import ru.iopump.qa.allure.service.generate.ErrorExecutingScript;

public class CreateAccClient {
    public String CreateCurrentAcc(String dep_code, String val_code, String cli_code, String product_code, String BdUrl,  String uiLogin, String uiPassword) throws SQLException, ErrorExecutingScript, ParseException {
        // метод создает текущий счет клиента
        // метод приннимает код департамента, код валюты, код клиента и код продукта dearko
        ConnectionData cd = new ConnectionData();
        ArrayList lst_prm = new ArrayList<>(Arrays.asList(dep_code, val_code, cli_code, product_code, "acc_code"));
        // вызываем метод для вызова процедуры
        ArrayList<String> acc_code_lst = cd.CallSqlProcedure("Z_PKG_AUTO_TEST", "p_create_acc_java", lst_prm, BdUrl, uiLogin, uiPassword);
        String acc_code = acc_code_lst.get(0);
        if (acc_code == null) {
            throw new SQLSyntaxErrorException(String.format("Текущий счет не создался для клиента - %s", cli_code));
        } else {
            System.out.println(String.format("Для клиента с кодом - %s, создан счет с номером - %s", cli_code, acc_code));
        }
        return acc_code;
    }
}

