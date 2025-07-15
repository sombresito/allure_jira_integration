package ru.iopump.qa.allure.service.generate;


import net.datafaker.Faker;
import ru.iopump.qa.allure.service.generate.ErrorExecutingScript;

import java.sql.SQLSyntaxErrorException;
import java.util.Locale;

public class GenerateNameJur {
    public String GetNameCompany(String res, String type_org) throws ErrorExecutingScript {
        // метод возвращает сгенированное название компании
        // если передается резидент, то имя компании будет на кириллице
        // если не резидент, то на латинице
        // если передается тип JUR, то тип организации будет ТОО
        // если PBOYUL, то тип организации будет ИП
        String fakerNameCom = null;
        String nameCom = null;
        if (res == "1") {
            Faker faker = new Faker(new Locale("ru"));
            if (type_org == "JUR") {
                fakerNameCom = String.valueOf(faker.company().name());
                for (int i = 0; i < fakerNameCom.length(); i++) {
                    if (fakerNameCom.charAt(i) == ' ') {
                        nameCom = String.format("ТОО \"%s\"", fakerNameCom.substring(i + 1, fakerNameCom.length()));
                    }
                }
            } else if (type_org == "PBOYUL") {
                fakerNameCom = String.valueOf(faker.company().name());
                for (int i = 0; i < fakerNameCom.length(); i++) {
                    if (fakerNameCom.charAt(i) == ' ') {
                        nameCom = String.format("ИП \"%s\"", fakerNameCom.substring(i + 1, fakerNameCom.length()));
                    }
                }
            } else {
                throw new ErrorExecutingScript(String.format("Передан неверный тип организации - %s", type_org));
            }
        } else if (res == "0") {
            Faker faker = new Faker(new Locale("en-US"));
            fakerNameCom = String.valueOf(faker.company().name());
            nameCom = fakerNameCom.replace("'", "");
        } else {
            throw new ErrorExecutingScript(String.format("Передан неверный тип резидентства - %s", res));
        }
        return nameCom;
    }
}
