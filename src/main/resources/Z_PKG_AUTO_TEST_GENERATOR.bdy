CREATE OR REPLACE PACKAGE BODY Z_PKG_AUTO_TEST_GENERATOR IS
                                          
    FUNCTION AT_fGetCountIinDataset(nameDataset varchar2, iin varchar2) RETURN NUMBER
      IS
      countIin number;
      BEGIN
        EXECUTE IMMEDIATE 
          'SELECT COUNT(IIN)'
          || ' FROM ' || DBMS_ASSERT.simple_sql_name(nameDataset)
          || ' WHERE IIN = ' || iin
          INTO countIin;
        RETURN countIin;
      EXCEPTION
        WHEN OTHERS THEN
          raise_application_error(-20000,'Произошла ошибка - '||SQLCODE||' -ERROR- '||SQLERRM);
      END;
    FUNCTION AT_fGetCountDublAllDtst(iin varchar2, typeCli varchar2) RETURN NUMBER
      IS
        nameDatasetJur dtstJur := dtstJur('TESTINGIINSAFESJUR', 'TESTINGIINJURCredit', 'TESTINGIINJUR', 'TESTINGIINJURMEMORIAL',
                                          'TESTINGIINJURCOLLECTION', 'TESTINGIINJURPTP', 'TESTINGIINJURLONG', 'TESTINGIINJURCOMMIS',
                                          'TESTINGIINJURDD7', 'TESTINGIINJURNONCASH', 'TESTINGIINJURSOC', 'TESTINGIINJURBLOCKACC',
                                          'TESTINGIINJURCARDCLI', 'TESTINGIINJURBOKS', 'TESTINGIINAKKREDSJUR');
                                          
        nameDatasetFl dtstFl := dtstFl('TESTINGIINFLBOKS', 'TESTINGIINFLCOLLECTION', 'TESTINGIINFLCOMMIS', 'TESTINGIINFLDD7',
                                        'TESTINGIINFLCARDCLI', 'TESTINGIINFL', 'TESTINGIINFLBLOCKACC', 'TESTINGIINFLSOC',
                                        'TESTINGIINFLCREDIT', 'TESTINGIINFLLONG', 'TESTINGIINSAFESFL', 'TESTINGIINFLMEMORIAL',
                                        'TESTINGIINFLDD7NONCASH', 'TESTINGIINFLPTP');                        
        
        cntIin number;
        cntDublIin number := 0;
      BEGIN
        IF typeCli = 'JUR' THEN
          FOR i IN 1 .. nameDatasetJur.count LOOP
            cntIin := AT_fGetCountIinDataset(nameDataset => nameDatasetJur(i), iin => iin);
            cntDublIin := cntDublIin + cntIin;
          END LOOP;
        ELSIF typeCli = 'FL' THEN
          FOR i IN 1 .. nameDatasetFl.count LOOP
            cntIin := AT_fGetCountIinDataset(nameDataset => nameDatasetFl(i), iin => iin);
            cntDublIin := cntDublIin + cntIin;
          END LOOP;
        ELSE
          raise_application_error(-20000,'Произошла ошибка - Передан неверный тип клиента');
        END IF;
        RETURN cntDublIin;
      EXCEPTION
        WHEN OTHERS THEN
          raise_application_error(-20000,'Произошла ошибка - '||SQLCODE||' -ERROR- '||SQLERRM);      
      END;
    PROCEDURE AT_pSetIinJurToDtst(nameDtst varchar2, iin varchar2, typeEntity varchar2, 
                                  typeOrg varchar2, dateReg varchar2, resident varchar2,
                                  jurName varchar2)
      IS
      BEGIN
          EXECUTE IMMEDIATE
            'INSERT INTO ' || DBMS_ASSERT.simple_sql_name(nameDtst) || '(IIN, TYPE_ENTITY, TYPE_ORG, DATE_REG, RESIDENT, NAME_COM)
             VALUES(''' || iin || ''', ''' || typeEntity || ''', ''' || typeOrg || ''', ''' || dateReg || ''', ''' || resident || ''', ''' || jurName || ''')';
          COMMIT;
      EXCEPTION
        WHEN OTHERS THEN
          raise_application_error(-20000,'Произошла ошибка - '||SQLCODE||' -ERROR- '||SQLERRM);
      END;
    PROCEDURE AT_pSetIinFlToDtst(nameDtst varchar2, iinCli varchar2, residentCli varchar2,
                                 sexCli varchar2, birthdayCli varchar2, nameFl varchar2, surnameFl varchar2, 
                                 middleFl varchar2)
      IS
      BEGIN
        EXECUTE IMMEDIATE 'INSERT INTO ' || nameDtst || '(IIN, SEX, BIRTHDAY, RESIDENT, NAME, SURNAME, MIDDLENAME) 
                           VALUES(''' || iinCli || ''', ''' || sexCli || ''', ''' || birthdayCli || ''', ''' || residentCli || ''', ''' || nameFl || ''', ''' || surnameFl || ''', ''' || middleFl || ''')';
            COMMIT;
      EXCEPTION
        WHEN OTHERS THEN
          raise_application_error(-20000,'Произошла ошибка - '||SQLCODE||' -ERROR- '||SQLERRM);
      END;
    FUNCTION AT_fCheckIinInGIin(cliIin varchar2) RETURN NUMBER
      IS
        cntIin number;
      BEGIN
        SELECT COUNT(IIN) INTO cntIin FROM g_iin
        WHERE IIN = cliIin;
        RETURN cntIin;
      EXCEPTION
        WHEN OTHERS THEN
          raise_application_error(-20000,'Произошла ошибка - '||SQLCODE||' -ERROR- '||SQLERRM);
      END;
    FUNCTION AT_fCheckIinInDb(cliIin varchar2) RETURN NUMBER
      IS
        cntIin number;
      BEGIN
        SELECT COUNT(taxcode) INTO cntIin FROM g_clihst
        WHERE taxcode = cliIin;
        RETURN cntIin;
      EXCEPTION
        WHEN OTHERS THEN
          raise_application_error(-20000,'Произошла ошибка - '||SQLCODE||' -ERROR- '||SQLERRM);
      END;
    FUNCTION AT_fCheckIinCli(IIN varchar2) RETURN INTEGER
    IS
    errNum integer;
    BEGIN
      errNum := G_PKGCLI.fChk_BINIIN(sTEXT => IIN, sExpCheckBIN => null);
      RETURN errNum;
    EXCEPTION
        WHEN OTHERS THEN
          raise_application_error(-20000,'Произошла ошибка - '||SQLCODE||' -ERROR- '||SQLERRM);
    END;
    PROCEDURE AT_pAddIinInDtst(iin varchar2, typeCli varchar2, dtstName varchar2, typeEntity varchar2 default null,
                                   typeOrg varchar2 default null, dateReg varchar2 default null, resident varchar2 default null,
                                   jurName varchar2 default null, sex varchar2 default null, birthday varchar2 default null,
                                   nameFl varchar2 default null, surnameFl varchar2 default null, middleFl varchar2 default null)
      IS
        cntDblDtst number;
        cntDblDb number;
        cntDblGiin number;
        cntErrorIin number;
      BEGIN
        /* Сперва проверяем дубли в датасетах ИИН */
        cntDblDtst := AT_fGetCountDublAllDtst(iin => iin, typeCli => typeCli);
        IF cntDblDtst = 0 THEN
          /* Если ИИН нет в датасетах, то проверяем его в БД Колвира */
          cntDblDb := AT_fCheckIinInDb(cliIin => iin);
          IF cntDblDb = 0 THEN
            /* Если ИИН не существует в БД, то проверяем в базе налогоплательщиков g_iin */
            cntDblGiin := AT_fCheckIinInGIin(cliIin => iin);
            IF cntDblGiin = 0 THEN
              /* Если в g_iin ИИН нет, то проверяем ИИН на ошибки перед инсертом в датасет */
              cntErrorIin := AT_fCheckIinCli(IIN => iin);
              IF cntErrorIin = 1 THEN
                /* Если нигде нет дублей и ошибок, то инсертим ИИН в датасет */
                IF typeCli = 'JUR' THEN
                  AT_pSetIinJurToDtst(nameDtst => dtstName, iin => iin, typeEntity => typeEntity, 
                                      typeOrg => typeOrg, dateReg => dateReg, resident => resident,
                                      jurName => jurName);
                ELSIF typeCli = 'FL' THEN
                  AT_pSetIinFlToDtst(nameDtst => dtstName, iinCli => iin, residentCli => resident, sexCli => sex, 
                                     birthdayCli => birthday, nameFl => nameFl, surnameFl => surnameFl,
                                     middleFl => middleFl);
                ELSE
                  raise_application_error(-20000,'Произошла ошибка - Передан неверный тип клиента '||typeCli);
                END IF;
                
              ELSE
                raise_application_error(-20000,'По ИИН - ' || iin || ' вернулся код ошибки ' || cntErrorIin);
              END IF;
            ELSE
              raise_application_error(-20000,'ИИН - ' || iin || ' уже существует в БД G_IIN');
            END IF;
          ELSE
            raise_application_error(-20000,'ИИН - ' || iin || ' уже существует в БД');
          END IF;
        ELSE
          raise_application_error(-20000,'ИИН - ' || iin || ' уже существует в датасетах');
        END IF;
      END;
    FUNCTION AT_fGetCliCodeMoneyAcc(cliType varchar2) RETURN VARCHAR2
      IS
        cliCode varchar2(255);
      BEGIN
        SELECT CLI_CODE INTO cliCode FROM MONEY_ACC
        WHERE VAL = 'KZT' AND TYPE_CLI = cliType;
        RETURN cliCode;
      EXCEPTION
        WHEN OTHERS THEN
          raise_application_error(-20000,'Произошла ошибка - '||SQLCODE||' -ERROR- '||SQLERRM);
      END;
    FUNCTION AT_fGetMoneyAcc(valCode varchar2, typeCli varchar2) RETURN VARCHAR2
      IS
        moneyAcc varchar2(255);
      BEGIN
        SELECT ACC INTO moneyAcc FROM MONEY_ACC
        WHERE VAL = valCode AND TYPE_CLI = typeCli;
        RETURN moneyAcc;
      EXCEPTION
        WHEN OTHERS THEN
          raise_application_error(-20000,'Произошла ошибка - '||SQLCODE||' -ERROR- '||SQLERRM);
      END;
  /* Функция для проверки существования записи по юрику в G_AFF */
  FUNCTION AT_fCheckJurGaff(cliCode varchar2) RETURN NUMBER
    IS
      cliId number;
      cliDepId number;
      cnt number;
    BEGIN
      cliId := Z_PKG_AUTO_TEST.AT_fGetCliIdFromCodeCard(cliCode => cliCode);
      cliDepId := Z_PKG_AUTO_TEST.AT_fGetCliDepIdFromCodeCard(cliCode => cliCode);
      SELECT COUNT(ID) INTO cnt FROM G_AFF g
      WHERE g.OBJDEP_ID = cliDepId AND g.OBJ_ID = cliId;
      RETURN cnt;
    EXCEPTION
        WHEN OTHERS THEN
          raise_application_error(-20000,'Произошла ошибка - '||SQLCODE||' -ERROR- '||SQLERRM);
    END;
  /* Функция для получения dep_id уполномоченного лица юрика */
  FUNCTION AT_fGetDepIdAuthPrsCliJur(cliCode varchar2) RETURN NUMBER
    IS
      prsDepId number;
      cliId number;
      cliDepId number;
    BEGIN
      cliId := Z_PKG_AUTO_TEST.AT_fGetCliIdFromCodeCard(cliCode => cliCode);
      cliDepId := Z_PKG_AUTO_TEST.AT_fGetCliDepIdFromCodeCard(cliCode => cliCode);
      SELECT PRSDEP_ID INTO prsDepId FROM GV_CLIAUTHPRS
      WHERE dep_id = cliDepId AND id = cliId
      AND ROWNUM < 2;
      RETURN prsDepId;
    EXCEPTION
        WHEN OTHERS THEN
          raise_application_error(-20000,'Произошла ошибка - '||SQLCODE||' -ERROR- '||SQLERRM);
    END;
  /* Функция для получения id уполномоченного лица юрика */
  FUNCTION AT_fGetIdAuthPrsCliJur(cliCode varchar2) RETURN NUMBER
    IS
      prsId number;
      cliId number;
      cliDepId number;
    BEGIN
      cliId := Z_PKG_AUTO_TEST.AT_fGetCliIdFromCodeCard(cliCode => cliCode);
      cliDepId := Z_PKG_AUTO_TEST.AT_fGetCliDepIdFromCodeCard(cliCode => cliCode);
      SELECT PRS_ID INTO prsId FROM GV_CLIAUTHPRS
      WHERE dep_id = cliDepId AND id = cliId
      AND ROWNUM < 2;
      RETURN prsId;
    EXCEPTION
        WHEN OTHERS THEN
          raise_application_error(-20000,'Произошла ошибка - '||SQLCODE||' -ERROR- '||SQLERRM);
    END;
  /* Функция возвращает ИИН из датасета */
  FUNCTION AT_fGetIinFromDtst(dtst varchar2, res varchar2) RETURN VARCHAR2
    IS
      result varchar2(30);
    BEGIN
      EXECUTE IMMEDIATE 'SELECT IIN FROM ' || dtst || ' WHERE RESIDENT = ' || res
      || ' AND ROWNUM < 2' INTO result;
      RETURN result;
    EXCEPTION
        WHEN OTHERS THEN
          raise_application_error(-20000,'Произошла ошибка - '||SQLCODE||' -ERROR- '||SQLERRM);
    END;
  /* Генерация мобильного номера телефона */
  FUNCTION AT_fGenNumPhone(opr varchar2) RETURN VARCHAR2
    IS
      rndNum number;
      cntNum number;
      numPhone varchar2(30);
      fullNumPhone varchar2(30);
    BEGIN
      LOOP
        rndNum := Z_PKG_AUTO_TEST.AT_fGetRandomNum(cnt => 7);
        numPhone := opr || TO_CHAR(rndNum);
        cntNum := AT_fCheckNumPhone(numPhone => numPhone);
        IF cntNum = 0 THEN
          EXIT;
        END IF;
      END LOOP;
      fullNumPhone := '+7' || numPhone;
      RETURN fullNumPhone;
    EXCEPTION
        WHEN OTHERS THEN
          raise_application_error(-20000,'Произошла ошибка - '||SQLCODE||' -ERROR- '||SQLERRM);  
    END;
  /* Функция для проверки существования номера в БД */
  FUNCTION AT_fCheckNumPhone(numPhone varchar2) RETURN NUMBER
    IS
      cntNum number;
    BEGIN
      SELECT COUNT(*) INTO cntNum FROM G_CLICONT
      WHERE CONT LIKE '%' || numPhone;
      RETURN cntNum;
    EXCEPTION
        WHEN OTHERS THEN
          raise_application_error(-20000,'Произошла ошибка - '||SQLCODE||' -ERROR- '||SQLERRM);
    END;
  /* Генерация номера удостоверения личности */
  FUNCTION AT_fGetNumUdo RETURN VARCHAR2
    IS
      rndNum number;
      checkNum number;
      numUdo varchar2(30);
    BEGIN
      LOOP
        rndNum := Z_PKG_AUTO_TEST.AT_fGetRandomNum(cnt => 9);
        checkNum := AT_fCheckNumUdo(numUdo => rndNum);
        IF checkNum = 0 THEN
          EXIT;
        END IF;
      END LOOP;
      numUdo := TO_CHAR(rndNum);
      RETURN numUdo;
    EXCEPTION
      WHEN OTHERS THEN
        raise_application_error(-20000,'Произошла ошибка - '||SQLCODE||' -ERROR- '||SQLERRM);
    END;
  /* Функция для проверки существования номера удо в бд */
  FUNCTION AT_fCheckNumUdo(numUdo varchar2) RETURN NUMBER
    IS
      cntNum number;
    BEGIN
      SELECT COUNT(*) INTO cntNum
      FROM G_CLIDOC 
      WHERE PASSNUM = numUdo;
      RETURN cntNum;
    EXCEPTION
      WHEN OTHERS THEN
        raise_application_error(-20000,'Произошла ошибка - '||SQLCODE||' -ERROR- '||SQLERRM);
    END;
  /* Функция для получения данных из датасета по ИИН */
  FUNCTION AT_fGetBirthdateFromDtst(dtst varchar2, iin varchar2, nameColumn varchar2) RETURN VARCHAR2
    IS
      result varchar2(255);
    BEGIN
      EXECUTE IMMEDIATE 'SELECT ' || nameColumn || ' FROM ' || dtst || ' WHERE IIN = ' || iin INTO result;
      RETURN result;
    EXCEPTION
      WHEN OTHERS THEN
        raise_application_error(-20000,'Произошла ошибка - '||SQLCODE||' -ERROR- '||SQLERRM);
    END;
  /* Функция для получения кода карточки клиента */
  FUNCTION AT_fGetCliCode(iin varchar2) RETURN VARCHAR2
    IS
      cliCode varchar2(30);
    BEGIN
      SELECT CODE INTO cliCode
      FROM G_CLI g, G_CLIHST gh
      WHERE g.dep_id = gh.dep_id AND g.id = gh.id
      AND gh.taxcode = iin;
      RETURN cliCode;
    EXCEPTION
      WHEN OTHERS THEN
        raise_application_error(-20000,'Произошла ошибка - '||SQLCODE||' -ERROR- '||SQLERRM);
    END;
  /* Получение bop_id карточки клиента */
  FUNCTION AT_fGetBopIdCliCard(cliCode varchar2) RETURN NUMBER
    IS
      cliId number;
      cliDepId number;
      bopId number;
    BEGIN
        cliId := Z_PKG_AUTO_TEST.AT_fGetCliIdFromCodeCard(cliCode => cliCode);
        cliDepId := Z_PKG_AUTO_TEST.AT_fGetCliDepIdFromCodeCard(cliCode => cliCode);
        SELECT p.BOP_ID INTO bopId FROM G_CLIHST, G_CLI, T_PROCMEM M, T_PROCESS P
        WHERE G_CLIHST.DEP_ID = G_CLI.DEP_ID
        and G_CLIHST.ID = G_CLI.ID
        and P_Operday BETWEEN G_CLIHST.FROMDATE and G_CLIHST.TODATE
        and M.ORD_ID = G_CLI.ORD_ID
        and M.DEP_ID = G_CLI.DEP_ID
        and P.ID = M.ID
        and M.MAINFL = '1'
        and g_cli.dep_id = cliDepId and g_cli.id = cliId;
        RETURN bopId;
    EXCEPTION
      WHEN OTHERS THEN
        raise_application_error(-20000,'Произошла ошибка - '||SQLCODE||' -ERROR- '||SQLERRM);
    END;
  /* Получение proc_id карточки клиента */
  FUNCTION AT_fGetProcIdCliCard(cliCode varchar2) RETURN NUMBER
    IS
      cliId number;
      cliDepId number;
      procId number;
    BEGIN
        cliId := Z_PKG_AUTO_TEST.AT_fGetCliIdFromCodeCard(cliCode => cliCode);
        cliDepId := Z_PKG_AUTO_TEST.AT_fGetCliDepIdFromCodeCard(cliCode => cliCode);
        SELECT p.ID INTO procId FROM G_CLIHST, G_CLI, T_PROCMEM M, T_PROCESS P
        WHERE G_CLIHST.DEP_ID = G_CLI.DEP_ID
        and G_CLIHST.ID = G_CLI.ID
        and P_Operday BETWEEN G_CLIHST.FROMDATE and G_CLIHST.TODATE
        and M.ORD_ID = G_CLI.ORD_ID
        and M.DEP_ID = G_CLI.DEP_ID
        and P.ID = M.ID
        and M.MAINFL = '1'
        and g_cli.dep_id = cliDepId and g_cli.id = cliId;
        RETURN procId;
    EXCEPTION
      WHEN OTHERS THEN
        raise_application_error(-20000,'Произошла ошибка - '||SQLCODE||' -ERROR- '||SQLERRM);
    END;
  /* Функция для получения названия стенда на котором запущены процедуры */
  FUNCTION AT_fGetStandName RETURN VARCHAR2
    IS
      standName varchar2(30);
    BEGIN
      SELECT DB_UNIQUE_NAME INTO standName FROM v$database;
      RETURN standName;
    EXCEPTION
      WHEN OTHERS THEN
        raise_application_error(-20000,'Произошла ошибка - '||SQLCODE||' -ERROR- '||SQLERRM);
    END;
END Z_PKG_AUTO_TEST_GENERATOR;
