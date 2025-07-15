CREATE OR REPLACE PACKAGE Z_PKG_AUTO_TEST_GENERATOR IS
    /* Массив с названиями датасетов ИИН Юриков */
    TYPE dtstJur IS VARRAY(100) OF VARCHAR2(100);
    /* Массив с названиями датасетов ИИН физиков */
    TYPE dtstFl IS VARRAY(100) OF VARCHAR2(100);
    /* Функция возвращает количество дублей передаваемой ИИН в конкретном датасете */
    FUNCTION AT_fGetCountIinDataset(nameDataset varchar2, iin varchar2) RETURN NUMBER;
    /* Функция проверяет каждый датасет на наличие дубля передаваемого ИИН */
    FUNCTION AT_fGetCountDublAllDtst(iin varchar2, typeCli varchar2) RETURN NUMBER;
    /* Процедура инсертит ИИН в датасет юриков, если нет дублей по всем датасетам */
    PROCEDURE AT_pSetIinJurToDtst(nameDtst varchar2, iin varchar2, typeEntity varchar2, 
                                  typeOrg varchar2, dateReg varchar2, resident varchar2,
                                  jurName varchar2);
    /* Процедура инсертит ИИН в датасет физиков, если нет дублей по всем датасетам */
    PROCEDURE AT_pSetIinFlToDtst(nameDtst varchar2, iinCli varchar2, residentCli varchar2,
                                 sexCli varchar2, birthdayCli varchar2, nameFl varchar2, surnameFl varchar2, 
                                 middleFl varchar2);
    /* Функция возвращает количество дублей передаваемого ИИН в таблице
    налогоплательщиков g_iin */
    FUNCTION AT_fCheckIinInGIin(cliIin varchar2) RETURN NUMBER;
    /* Функция проверяет передаваемый ИИН на существование в БД */
    FUNCTION AT_fCheckIinInDb(cliIin varchar2) RETURN NUMBER;
    /* Функция проверяет ИИН на наличие ошибок */
    FUNCTION AT_fCheckIinCli(IIN varchar2) RETURN INTEGER;
    /* Процедура производит все проверки добавляет ИИН в датасет */
    PROCEDURE AT_pAddIinInDtst(iin varchar2, typeCli varchar2, dtstName varchar2, typeEntity varchar2 default null,
                                   typeOrg varchar2 default null, dateReg varchar2 default null, resident varchar2 default null,
                                   jurName varchar2 default null, sex varchar2 default null, birthday varchar2 default null,
                                   nameFl varchar2 default null, surnameFl varchar2 default null, middleFl varchar2 default null);
    /* Функция возвращает код тестового клиента на которого будут создаваться тестовые счета для пополнения */
    FUNCTION AT_fGetCliCodeMoneyAcc(cliType varchar2) RETURN VARCHAR2;
    /* Функция возвращает номер тестового счета для пополнения */
    FUNCTION AT_fGetMoneyAcc(valCode varchar2, typeCli varchar2) RETURN VARCHAR2;
    /* Функция для проверки существования записи по юрику в G_AFF */
    FUNCTION AT_fCheckJurGaff(cliCode varchar2) RETURN NUMBER;
    /* Функция для получения dep_id уполномоченного лица юрика */
  FUNCTION AT_fGetDepIdAuthPrsCliJur(cliCode varchar2) RETURN NUMBER;
  /* Функция для получения id уполномоченного лица юрика */
  FUNCTION AT_fGetIdAuthPrsCliJur(cliCode varchar2) RETURN NUMBER;
  /* Функция возвращает ИИН из датасета */
  FUNCTION AT_fGetIinFromDtst(dtst varchar2, res varchar2) RETURN VARCHAR2;
  /* Генерация мобильного номера телефона */
  FUNCTION AT_fGenNumPhone(opr varchar2) RETURN VARCHAR2;
  /* Функция для проверки существования номера в БД */
  FUNCTION AT_fCheckNumPhone(numPhone varchar2) RETURN NUMBER;
  /* Генерация номера удостоверения личности */
  FUNCTION AT_fGetNumUdo RETURN VARCHAR2;
  /* Функция для проверки существования номера удо в бд */
  FUNCTION AT_fCheckNumUdo(numUdo varchar2) RETURN NUMBER;
  /* Функция для получения данных из датасета по ИИН */
  FUNCTION AT_fGetBirthdateFromDtst(dtst varchar2, iin varchar2, nameColumn varchar2) RETURN VARCHAR2;
  /* Функция для получения кода карточки клиента */
  FUNCTION AT_fGetCliCode(iin varchar2) RETURN VARCHAR2;
  /* Получение bop_id карточки клиента */
  FUNCTION AT_fGetBopIdCliCard(cliCode varchar2) RETURN NUMBER;
  /* Получение proc_id карточки клиента */
  FUNCTION AT_fGetProcIdCliCard(cliCode varchar2) RETURN NUMBER;
  /* Функция для получения названия стенда на котором запущены процедуры */
  FUNCTION AT_fGetStandName RETURN VARCHAR2;
END;
