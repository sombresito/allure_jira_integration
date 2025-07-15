package ru.iopump.qa.allure.service.generate;


import ru.iopump.qa.allure.service.generate.ErrorExecutingScript;
import ru.iopump.qa.allure.service.generate.ConnectionData;
import ru.iopump.qa.allure.service.generate.GenerateNumbers;
import ru.iopump.qa.allure.service.generate.ChangeDate;
//import javax.management.Query;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;

public class CreateDocDos {

    public String CreateDoc(String cliCode, String docType, String docGrp, String longName, String docDate,
                            String docNum, String copyFl, String docPycCnt, String expDate, String docPubl,
                            String docDsc, String notifyFl, String storage, String eCopy, String fullPath,
                            String fileExt, String dbFl, String BdUrl, String uiLogin, String uiPassword) throws SQLException, ErrorExecutingScript, ParseException {
        // метод создает документ и его электронную копию в досье карточки клиента
        // параметры метода
        // cliCode - код карточки клиента
        // docType - тип документа (id документа, который необходимо создать
        // docGrp - id группы документов. Можно передавать null
        // longName - наименование документа. Должно браться из бд, так как есть валидация на название передаваемого документа
        // docDate - дата документа в формате дд.мм.гг
        // docNum - номер документа
        // copyFl - признак копии документа '0'/'1'
        // docPycCnt - кол-во эеземпляров
        // expDate - дата окончания документа в формате дд.мм.гг
        // docPubl - кем выдан
        // docDsc - описание
        // notifyFl - признак уведомления '0'/'1'
        // storage - место хранения (можно передавать null)
        // eCopy - признак создания электронной копии (если передать '1', то создасться электронная копия
        //             если '0', то электронная копия не создасться
        // fullPath - путь до файла. По дефолту null
        // fileExt - расширение файла. По дефолту null
        // dbFl - место хранения. По дефолту '0'

        ConnectionData con_data = new ConnectionData();
        ArrayList result = null;

        ArrayList lst_prm = new ArrayList<>(Arrays.asList(cliCode, docType, docGrp, longName, docDate, docNum, copyFl, docPycCnt, expDate, docPubl, docDsc, notifyFl, storage, eCopy, fullPath, fileExt, dbFl, "nord"));
        result = con_data.CallSqlProcedure("Z_PKG_AUTO_TEST", "pCreDocDosCliJava", lst_prm, BdUrl, uiLogin, uiPassword);

        if (result == null) {
            throw new ErrorExecutingScript(String.format("По коду клиента %s не создан документ с номером - %s", cliCode, docNum));
        } else {
            System.out.println(String.format("Создан документ с порядковым номером %s по коду клиента %s", result.get(0), cliCode));
        }
        return (String) result.get(0);
    }
}
