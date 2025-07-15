CREATE OR REPLACE PROCEDURE CreateClientCardJava(IIN_CLI IN varchar2, TYPE_DOC IN varchar2, DATE_FROM IN varchar2,
                                             DATE_TO IN varchar2, NUM_PHONE IN varchar2, CLI_ROLE IN varchar2,
                                             PASS_NUM IN varchar2, CLI_TYPE IN varchar2, RES IN varchar2,
                                             SEX IN varchar2, RES_COUNTRY IN varchar2, NAME IN varchar2, DEP IN varchar2,
                                             BIRTHDATE IN varchar2, CODE_CLIENT OUT varchar2)

IS
--sType := substr(sqlerrm, 1, 500);
nRes number;
sType varchar2 (20000);
p_errfl number;
p_errMsg varchar2 (20000);
CLI_CODE varchar2 (20000);
CLI_ID varchar2 (20000);
CLI_DEP_ID varchar2 (20000);
CLI_ORD_ID varchar2 (20000);
JURFL varchar2 (20000);
Pboyulfl varchar2 (20000);
Typefl varchar2 (20000);
nErrFl number;
sErrMsg varchar2 (20000);
nDEnID number;
nIDR number;
nORD_ID number;
cErrMsg varchar2 (20000);
log_id number;
nErr_fl number;
sErr_msg varchar2 (20000);
sectId number(20);
fName varchar2 (20000) := REGEXP_SUBSTR(NAME, '(\S*)', 1, 3);
lName varchar2 (20000) := REGEXP_SUBSTR(NAME, '(\S*)');
mName varchar2 (20000) := REGEXP_SUBSTR(NAME, '(\S*)', 1, 5);
name1 varchar2(40);
name2 varchar2(40);
name3 varchar2(40);
birdate date;
new_date varchar2(40);
g_code varchar2(40);
prm varchar2(2000);
proc_id number;
FIZ_DEP_ID number;
FIZ_ID number;
DEP_ID_IP number;
ID_IP number;
nord number;
jrn_id number;
Cnt number;
cntAff number;
getAuytDepId number;
getAuytId number;
nameStand varchar2(30);

TYPE value_adt_type IS TABLE OF VARCHAR2 (10); -- таблица содержащая типы адресов
  v_tab value_adt_type;
TYPE value_attr_type IS TABLE OF VARCHAR2 (2000); -- таблица содержащая атрибуты доп информации
  dop_attr value_attr_type;
  value_attr value_attr_type;
  --v_idx NUMBER;

  BEGIN

       IF CLI_TYPE = 'FL' THEN
         JURFL := '0';
         Pboyulfl := '0';
         Typefl := '1';
         sectId := '9';

       ELSIF CLI_TYPE = 'JUR' THEN
         JURFL := '1';
         Pboyulfl := '0';
         Typefl := '0';
         sectId := '7';

       ELSIF CLI_TYPE = 'PBOYUL' THEN
         JURFL := '0';
         Pboyulfl := '1';
         Typefl := '2';
         sectId := '9';

       ELSIF CLI_TYPE = 'PBOYULS' THEN
         JURFL := '0';
         Pboyulfl := '1';
         Typefl := '3';

       ELSIF CLI_TYPE is null THEN
         nRES := 0;
         sType := 'В переменной CLI_TYPE содержится null';
         cErrMsg := 'В переменной CLI_TYPE содержится null '|| chr(13)||
                     'nResult = ' || nRes || chr(13)||
                     'sResMsg = ' || sType ;

         Z_077_PKGSRVLOG.p_log(p_id_log  => log_id
                                ,p_exec_result => cErrMsg);
         dbms_output.put_line('1out -> ' || IIN_CLI || '; 2out-> ' || CLI_TYPE || '; 3out-> ' || NAME || '; 4out-> ' || nRes || '; 5out-> ' || sType);
         RETURN;

       ELSE
         nRES := 0;
         sType := 'В переменной CLI_TYPE содержится неподходящее значение - '|| CLI_TYPE;
         cErrMsg := 'В переменной CLI_TYPE содержится неподходящее значение - '|| CLI_TYPE || chr(13)||
                     'nResult = ' || nRes || chr(13)||
                     'sResMsg = ' || sType ;

         Z_077_PKGSRVLOG.p_log(p_id_log  => log_id
                                ,p_exec_result => cErrMsg);
         dbms_output.put_line('1out -> ' || IIN_CLI || '; 2out-> ' || CLI_TYPE || '; 3out-> ' || NAME || '; 4out-> ' || nRes || '; 5out-> ' || sType);
         RETURN;

       END IF;
       IF CLI_TYPE = 'FL' or CLI_TYPE = 'JUR' or CLI_TYPE = 'PBOYUL' THEN
       Z_077_PKG_CLIENT.pCreCliUniv( sLongname   => NAME,
                              sShortname  => null,
                              sPlname1    => null,
                              sPlname2    => null,
                              sCodeword   => null,
                              sDepCode    => DEP,
                              sRolemask   => CLI_ROLE,
                              sJURFL      => JURFL,
                              sPboyulfl   => Pboyulfl,
                              sTaxcode    => IIN_CLI,
                              sSect_id    => sectId,
                              sTypefl     => Typefl,
                              sBirdate     => BIRTHDATE,
                              sSex         => SEX,
                              sResidfl     => RES,
                              sReg         => RES_COUNTRY,
                              sCit         => RES_COUNTRY,
                              sHDRSTATFL   => null,
                              nErrFl       => nErrFl,
                              sErrMsg      => sErrMsg,
                              nDEnID      => nDEnID,
                              nID         => nIDR,
                              nORD_ID     => nORD_ID);
          commit;
       END IF;
       IF CLI_TYPE = 'FL' THEN
          Z_PKG_AUTO_TEST.p_create_card_fl(n_inn_code => IIN_CLI, n_num_phone => NUM_PHONE);
       ELSIF CLI_TYPE = 'JUR' THEN
          Z_PKG_AUTO_TEST.p_create_card_jur(n_inn_code => IIN_CLI);
       ELSIF CLI_TYPE = 'PBOYUL' or CLI_TYPE = 'PBOYULS' THEN
          Z_PKG_AUTO_TEST.p_create_card_pboul(n_inn_code => IIN_CLI, n_num_phone => NUM_PHONE);
          commit;
       END IF;

       BEGIN
         IF CLI_TYPE = 'FL' THEN
             select g.CODE, g.DEP_ID, g.ID, g.ORD_ID
             INTO CLI_CODE, CLI_DEP_ID, CLI_ID, CLI_ORD_ID
             from g_cli g, g_clihst gh
             where g.id = gh.id and g.dep_id = gh.dep_id
                   and gh.TAXCODE = IIN_CLI and g.PBOYULFL = 0;
         ELSIF CLI_TYPE = 'PBOYUL' or CLI_TYPE = 'PBOYULS' THEN
             select g.CODE, g.DEP_ID, g.ID, g.ORD_ID
             INTO CLI_CODE, CLI_DEP_ID, CLI_ID, CLI_ORD_ID
             from g_cli g, g_clihst gh
             where g.id = gh.id and g.dep_id = gh.dep_id
                   and gh.TAXCODE = IIN_CLI and g.PBOYULFL = 1;

             update g_cli g
             set REGINITDATE = BIRTHDATE
             where g.code = CLI_CODE;
             COMMIT;

             /* Добавление классификаторов */
              c_pkgconnect.popen(); c_pkgsession.doper := trunc(sysdate);
              Z_077_PKG_CLIENT.pProcessCliOPF(nDEP_ID   => CLI_DEP_ID,
                                              nID       => CLI_ID,
                                              sOkved    => 46213,
                                              nOkved_Id => 386,
                                              sOkopf    => '46',
                                              nOkopf_id => 388,
                                              sOkfs     => '30',
                                              nOkfs_id  => 1,
                                              sSpe      => '30.1',
                                              nSpe_id   => 201,
                                              nRes      => nRes,
                                              sType     => sType);

               SELECT gh.PNAME1, gh.PNAME2, gh.PNAME3, g.BIRDATE
               into name1, name2, name3, birdate
               FROM g_clihst gh, g_cli g
               where gh.id = g.id and gh.dep_id = g.dep_id and gh.taxcode = IIN_CLI and g.TYPEFL = '1';

               SELECT g.CODE
               into g_code
               FROM g_clihst gh, g_cli g
               where gh.id = g.id and gh.dep_id = g.dep_id and gh.taxcode = IIN_CLI and g.TYPEFL = '2';
               new_date := TO_CHAR(birdate, 'DD/MM/YYYY');
               update g_cli
               set BIRDATE = new_date
               where code = g_code;
               commit;

               update g_clihst
               set PNAME1 = name1, PNAME2 = name2, PNAME3 = name3
               where taxcode = IIN_CLI and TYPEFL = '2';
               commit;

               Z_077_PKG_CLIENT.pControlCard(nDep_id => CLI_DEP_ID,
                                       nId => CLI_ID);
               COMMIT;

         ELSIF CLI_TYPE = 'JUR' THEN
             select g.CODE, g.DEP_ID, g.ID, g.ORD_ID
             INTO CLI_CODE, CLI_DEP_ID, CLI_ID, CLI_ORD_ID
             from g_cli g, g_clihst gh
             where g.id = gh.id and g.dep_id = gh.dep_id
                   and gh.TAXCODE = IIN_CLI and g.JURFL = 1;
         ELSE
             nRES := 0;
             sType := 'В переменной CLI_TYPE содержится неподходящее значение - '|| CLI_TYPE;
             cErrMsg := 'В переменной CLI_TYPE содержится неподходящее значение - '|| CLI_TYPE || chr(13)||
                         'nResult = ' || nRes || chr(13)||
                         'sResMsg = ' || sType ;
         END IF;

         Z_077_PKGSRVLOG.p_log(p_id_log  => log_id
                                ,p_exec_result => cErrMsg);
         dbms_output.put_line('1out -> ' || IIN_CLI || '; 2out-> ' || CLI_TYPE || '; 3out-> ' || NAME || '; 4out-> ' || nRes || '; 5out-> ' || sType);
       EXCEPTION WHEN NO_DATA_FOUND THEN
         nRES := 0;
         sType := 'Клиент ' || IIN_CLI || ' не найден в клиентской базе банка!';
         cErrMsg := 'PROC Идентификация клиента в базе банка '|| chr(13)||
                     'nResult = ' || nRes || chr(13)||
                     'sResMsg = ' || sType ;

         Z_077_PKGSRVLOG.p_log(p_id_log  => log_id
                                ,p_exec_result => cErrMsg);
         dbms_output.put_line('1out -> ' || IIN_CLI || '; 2out-> ' || CLI_TYPE || '; 3out-> ' || NAME || '; 4out-> ' || nRes || '; 5out-> ' || sType);
         RETURN;
       END;

       IF CLI_TYPE = 'JUR' THEN
         Z_077_PKG_CLIENT.pActualizeYul(nDEP_ID => CLI_DEP_ID,
                                          nID    => CLI_ID,
                                          nORD_ID  => CLI_ORD_ID,
                                          sIin => IIN_CLI,  --ИИН Руководителя
                                          sPhoneNumber => NUM_PHONE, --Мобильный телефона
                                          sProject   => 01, --Источник создания карточки клиента
                                          nRes => nRes,
                                          sType => sType);
       ELSIF CLI_TYPE = 'PBOYUL' or CLI_TYPE = 'PBOYULS' THEN
         Z_077_PKG_CLIENT.pActualizeIp(sIIN          => IIN_CLI,             --ИИН ИП
                                       sPhoneNumber  => NUM_PHONE,             --Номер телефона
                                       sProject      => 01,             --Источник создания карточки
                                       nDEP_ID       => CLI_DEP_ID,               --Подразделение созданной карточки
                                       nID           => CLI_ID,               --Идентификатор созданной карточки
                                       nORD_ID       => CLI_ORD_ID,               --ORD_ID созданной карточки
                                       nRes          => nRes,              --Код результата
                                       sType         => sType);         --Текс результата
         commit;
         dbms_output.put_line('1out -> ' || IIN_CLI || '; 2out-> ' || nRes || '; 3out-> ' || sType);
       END IF;
       IF CLI_TYPE = 'PBOYULS' THEN
           Z_077_PKG_CLIENT.pEntAddAttr(nClidep_id => CLI_DEP_ID,
                                 nCli_id    => CLI_ID,
                                 sAttr      => 'PRTYPE', -- Тип предпринимательства
                                 sValue     => 'крестьянское хозяйство',
                                 nErr_fl    => nRes,
                                 sErr_msg   => sType);
         commit;
         Z_077_PKG_CLIENT.pControlCard(nDep_id => CLI_DEP_ID,
                                       nId => CLI_ID);
         COMMIT;
       END IF;
   Z_077_PKG_CLIENT.pAddRole(nDEP_ID => CLI_DEP_ID,
                                 nID => CLI_ID,
                                 sCliRole => CLI_ROLE,
                                 nRes => nRes,
                                 sType => sType);
      IF CLI_TYPE = 'FL' OR CLI_TYPE = 'PBOYUL' or CLI_TYPE = 'PBOYULS' THEN
          Z_077_PKG_CLIENT.pAddCliDoc(nDEP_ID => CLI_DEP_ID,
                                      nID => CLI_ID,
                                      sDocType => TYPE_DOC,
                                      sPassnum => PASS_NUM, --053710243
                                      dPassdat => DATE_FROM,
                                      sPassorg => 'МВД РК',
                                      dPassfin => DATE_TO,
                                      sPassser => null,
                                      sBasfl   => '1',
                                      sArcfl   => '0',
                                      nRes => nRes,
                                      sType => sType);

       -- Инициализировать коллекцию двумя значениями. 002 - место регистрации адреса, 006 - фактическое место
        v_tab:= value_adt_type('002', '006');
        << ADD_TYPE_ADDR >>

        -- 2 раза запускаем процедуру с разными типами адресов. Создаются 2 адреса с разными типами
        FOR i IN 1 .. 2 LOOP

       Z_077_PKG_CLIENT.pProcessCliAddr2(n_dep_id  => CLI_DEP_ID,
                                         n_id      => CLI_ID,
                                         p_country => 'KZ',
                                         p_city    => 'АЛМАТЫ',
                                         p_district  =>  null,
                                         p_microdistrict  => null,
                                         p_street   => null,
                                         p_home  => null,
                                         p_apartment  => null,
                                         p_index   => null,
                                         p_village  => NULL,
                                         p_adrtype => v_tab(i),
                                         p_okato  => '750000000',
                                         p_strtype => NULL,
                                         p_mdstype => NULL,
                                         p_errfl => p_errfl,
                                         p_errMsg => p_errMsg);


       END LOOP  ADD_TYPE_ADDR;
       END IF;

       dop_attr := value_attr_type('BPLACE', 'LONGNAME_KZ', 'RES_COUNTRY', 'PRSCNT');
       value_attr := value_attr_type(RES_COUNTRY, NAME, RES_COUNTRY, 20);
        << ADD_TYPE_ATTR >>

        -- запускаем процедуру с разными типами атрибутов. Атрибуты добавляются во вкладку Доп информации
        FOR i IN 1 .. 4 LOOP
            Z_077_PKG_CLIENT.pEntAddAttr(nClidep_id => CLI_DEP_ID,
                                         nCli_id    =>   CLI_ID,
                                         sAttr     => dop_attr(i),
                                         sValue   => value_attr(i),
                                         nErr_fl    => nErr_fl,
                                         sErr_msg   => sErr_msg);
            --dbms_output.put_line('1out -> ' || nErr_fl || '; 2out-> ' || sErr_msg);
        END LOOP  ADD_TYPE_ATTR;
         /* Выполняем операцию привязки карточки физ к карточке ИП */
        IF CLI_TYPE = 'PBOYUL' THEN
          select g.DEP_ID, g.ID INTO DEP_ID_IP, ID_IP
             from g_cli g, g_clihst gh
             where g.id = gh.id and g.dep_id = gh.dep_id
                   and gh.TAXCODE = IIN_CLI and g.PBOYULFL = 1;
          /* получаем bop_id и proc_id карточки ИП */
          select p.id INTO proc_id from G_CLI g, G_CLIHST gh, T_PROCMEM M, T_PROCESS P
          where g.dep_id = gh.dep_id and g.id = gh.id and M.ORD_ID = g.ORD_ID
                and M.DEP_ID = g.DEP_ID and P.ID = M.ID and g.id = ID_IP
                and g.dep_id = DEP_ID_IP and p.BOP_ID = 5563;
          /* получаем id и dep_id физической карточки */
          SELECT g.DEP_ID, g.ID INTO FIZ_DEP_ID, FIZ_ID FROM G_CLI g, G_CLIHST gh
          WHERE g.dep_id = gh.dep_id and g.id = gh.id and gh.taxcode = IIN_CLI
                and g.PBOYULFL = 0;
          /* Добавляем уполномоченное лицо */
          c_pkgconnect.popen(); c_pkgsession.doper := trunc(sysdate);
          g_pkgdblcli.pinsupdcliauthprs(p_dep_id       => DEP_ID_IP,  --карточка ипч
                                        p_id           => ID_IP, --карточка ипч
                                        p_nord         => nord,
                                        p_prsdep_id    => FIZ_DEP_ID, --карточка физ лица
                                        p_prs_id       => FIZ_ID, --карточка физ лица
                                        p_arcfl        => '0',
                                        p_arestfl      => '0',
                                        p_begdate      => null,
                                        p_enddate      => null,
                                        p_nsign        => 1,
                                        p_signfl       => '1',
                                        p_place        => 'Руководитель',
                                        p_num          => null,
                                        p_issued       => null,
                                        p_issued_dtl   => null,
                                        p_issdate      => null,
                                        p_deliverplace => null,
                                        p_doctype      => 'X',
                                        p_jrn_id       => jrn_id,
                                        p_reason       => null);
          COMMIT;
          /* Выполняем операцию привязки карточки физ к карточке ИП */
          prm := 'DEP_ID => ' || CLI_DEP_ID || ', ID => ' || CLI_ID || ', CLIFIZ_DEP_ID => ' || FIZ_DEP_ID || ', CLIFIZ_ID => ' || FIZ_ID ;
          c_pkgconnect.popen(); c_pkgsession.doper := trunc(sysdate);
          M_PKGDBLCLI.pRunOperation(
                                    p_ID  => proc_id               -- идентификатор бизнесс-процесса
                                    , p_BOP_ID => 5563              -- идентификатор описание бизнесс процесса из T_BOP_DSCR
                                    , p_Prm => prm -- входные параметры операции
                                    , p_OperCode => 'SEL_FIZ'      -- код операции
                                    );
          COMMIT;
        END IF;
        Z_077_PKG_CLIENT.pControlCard(nDep_id => CLI_DEP_ID,
                                       nId => CLI_ID);
        COMMIT;
        /* Добавляем согласие на кредитную историю */
        Z_PKG_AUTO_TEST.AT_pInsYesKb(cli_code => CLI_CODE);
        /*---------*/
        /* Добавление ОГРН ИП */
        IF CLI_TYPE = 'PBOYUL' THEN
          Z_PKG_AUTO_TEST.AT_pAddCliRegDocBase(cliCode => CLI_CODE);
        END IF;
        /* добавляем контакты, если не добавились */
        SELECT COUNT(*) INTO Cnt FROM G_CLICONT c, g_cli g
        WHERE C.DEP_ID = g.DEP_ID and C.ID = g.ID
        AND g.code = CLI_CODE;
        IF CLI_TYPE <> 'JUR' AND Cnt = 0 THEN
          dbms_output.put_line('1out -> ' || SUBSTR(NUM_PHONE, 3, 3));
          IF SUBSTR(NUM_PHONE, 1, 3) = '747' THEN
             insert into G_CLICONT(DEP_ID, ID, NORD, BASFL, CLICONT_NORD, ARCFL, CONT, ATYP, CORRECTDT, ID_US, PRIM, ID_CONTTYPE, PURPOSE$BK1, PHONE_OP_ID, SEEKCONT)
             values(CLI_DEP_ID, CLI_ID, 1, 1, null, 0, '+7'||NUM_PHONE, 8, null, null, 'Autotests', 82, null, 21, SUBSTR(NUM_PHONE, 4));
          ELSIF SUBSTR(NUM_PHONE, 1, 3) = '701' THEN
             insert into G_CLICONT(DEP_ID, ID, NORD, BASFL, CLICONT_NORD, ARCFL, CONT, ATYP, CORRECTDT, ID_US, PRIM, ID_CONTTYPE, PURPOSE$BK1, PHONE_OP_ID, SEEKCONT)
             values(CLI_DEP_ID, CLI_ID, 1, 1, null, 0, '+7'||NUM_PHONE, 8, null, null, 'Autotests', 82, null, 1, SUBSTR(NUM_PHONE, 4));
          ELSIF SUBSTR(NUM_PHONE, 1, 3) = '702' THEN
             insert into G_CLICONT(DEP_ID, ID, NORD, BASFL, CLICONT_NORD, ARCFL, CONT, ATYP, CORRECTDT, ID_US, PRIM, ID_CONTTYPE, PURPOSE$BK1, PHONE_OP_ID, SEEKCONT)
             values(CLI_DEP_ID, CLI_ID, 1, 1, null, 0, '+7'||NUM_PHONE, 8, null, null, 'Autotests', 82, null, 4, SUBSTR(NUM_PHONE, 4));
          ELSIF SUBSTR(NUM_PHONE, 1, 3) = '705' THEN
             insert into G_CLICONT(DEP_ID, ID, NORD, BASFL, CLICONT_NORD, ARCFL, CONT, ATYP, CORRECTDT, ID_US, PRIM, ID_CONTTYPE, PURPOSE$BK1, PHONE_OP_ID, SEEKCONT)
             values(CLI_DEP_ID, CLI_ID, 1, 1, null, 0, '+7'||NUM_PHONE, 8, null, null, 'Autotests', 82, null, 8, SUBSTR(NUM_PHONE, 4));
          ELSIF SUBSTR(NUM_PHONE, 1, 3) = '778' THEN
             insert into G_CLICONT(DEP_ID, ID, NORD, BASFL, CLICONT_NORD, ARCFL, CONT, ATYP, CORRECTDT, ID_US, PRIM, ID_CONTTYPE, PURPOSE$BK1, PHONE_OP_ID, SEEKCONT)
             values(CLI_DEP_ID, CLI_ID, 1, 1, null, 0, '+7'||NUM_PHONE, 8, null, null, 'Autotests', 82, null, 6, SUBSTR(NUM_PHONE, 4));
          ELSIF SUBSTR(NUM_PHONE, 1, 3) = '707' THEN
             insert into G_CLICONT(DEP_ID, ID, NORD, BASFL, CLICONT_NORD, ARCFL, CONT, ATYP, CORRECTDT, ID_US, PRIM, ID_CONTTYPE, PURPOSE$BK1, PHONE_OP_ID, SEEKCONT)
             values(CLI_DEP_ID, CLI_ID, 1, 1, null, 0, '+7'||NUM_PHONE, 8, null, null, 'Autotests', 82, null, 10, SUBSTR(NUM_PHONE, 4));
          ELSIF SUBSTR(NUM_PHONE, 1, 3) = '777' THEN
             insert into G_CLICONT(DEP_ID, ID, NORD, BASFL, CLICONT_NORD, ARCFL, CONT, ATYP, CORRECTDT, ID_US, PRIM, ID_CONTTYPE, PURPOSE$BK1, PHONE_OP_ID, SEEKCONT)
             values(CLI_DEP_ID, CLI_ID, 1, 1, null, 0, '+7'||NUM_PHONE, 8, null, null, 'Autotests', 82, null, 2, SUBSTR(NUM_PHONE, 4));
          ELSIF SUBSTR(NUM_PHONE, 1, 3) = '708' THEN
             insert into G_CLICONT(DEP_ID, ID, NORD, BASFL, CLICONT_NORD, ARCFL, CONT, ATYP, CORRECTDT, ID_US, PRIM, ID_CONTTYPE, PURPOSE$BK1, PHONE_OP_ID, SEEKCONT)
             values(CLI_DEP_ID, CLI_ID, 1, 1, null, 0, '+7'||NUM_PHONE, 8, null, null, 'Autotests', 82, null, 11, SUBSTR(NUM_PHONE, 4));
          ELSIF SUBSTR(NUM_PHONE, 1, 3) = '775' THEN
             insert into G_CLICONT(DEP_ID, ID, NORD, BASFL, CLICONT_NORD, ARCFL, CONT, ATYP, CORRECTDT, ID_US, PRIM, ID_CONTTYPE, PURPOSE$BK1, PHONE_OP_ID, SEEKCONT)
             values(CLI_DEP_ID, CLI_ID, 1, 1, null, 0, '+7'||NUM_PHONE, 8, null, null, 'Autotests', 82, null, 5, SUBSTR(NUM_PHONE, 4));
          ELSIF SUBSTR(NUM_PHONE, 1, 3) = '776' THEN
             insert into G_CLICONT(DEP_ID, ID, NORD, BASFL, CLICONT_NORD, ARCFL, CONT, ATYP, CORRECTDT, ID_US, PRIM, ID_CONTTYPE, PURPOSE$BK1, PHONE_OP_ID, SEEKCONT)
             values(CLI_DEP_ID, CLI_ID, 1, 1, null, 0, '+7'||NUM_PHONE, 8, null, null, 'Autotests', 82, null, 7, SUBSTR(NUM_PHONE, 4));
          END IF;
          COMMIT;
          /*---------*/
          Z_077_PKG_CLIENT.pControlCard(nDep_id => CLI_DEP_ID,
                                       nId => CLI_ID);
          COMMIT;
        END IF;
        /* Добавление степени влияния юр лицу */
        IF CLI_TYPE = 'JUR' THEN
            /* Проверяем существует ли запись в G_AFF */
            cntAff := Z_PKG_AUTO_TEST_GENERATOR.AT_fCheckJurGaff(cliCode => CLI_CODE);
            EXECUTE IMMEDIATE 'ALTER TRIGGER COLVIR.G_AFF_DTU DISABLE';
            EXECUTE IMMEDIATE 'ALTER TRIGGER COLVIR.Z_077_BENIF_TRIGGER DISABLE';
            /* Если запись есть, то апдейтим ее */
            nameStand := Z_PKG_AUTO_TEST_GENERATOR.AT_fGetStandName;
            IF cntAff > 0 THEN
              IF nameStand <> 'cbs3tp' THEN
                UPDATE G_AFF
                SET rate = '100.0'
                WHERE OBJDEP_ID = CLI_DEP_ID 
                AND OBJ_ID = CLI_ID;
                COMMIT;
              ELSE
                UPDATE G_AFF
                SET rate = '100.0', AFFKND = 'Выгодоприобретатель (ФЛ-бенефициарный собственник, владеющий более 25% в капитале)'
                WHERE OBJDEP_ID = CLI_DEP_ID 
                AND OBJ_ID = CLI_ID;
                COMMIT;
              END IF;
            /* Если нет то инсертим */
            ELSE
              getAuytDepId := Z_PKG_AUTO_TEST_GENERATOR.AT_fGetDepIdAuthPrsCliJur(cliCode => CLI_CODE);
              getAuytId := Z_PKG_AUTO_TEST_GENERATOR.AT_fGetIdAuthPrsCliJur(cliCode => CLI_CODE);
              INSERT INTO G_AFF(OBJDEP_ID, OBJ_ID, SBJDEP_ID, SBJ_ID, AFFILFL, HOLDFL, DOLGFL, SECRET, RATE, AFFKND, HOLDPRC, PLACE, BEGDATE, ENDDATE, ID_US, CORRECTDT, PRIM, UNCONFFL, SBJBNFOWNFL)
              VALUES(CLI_DEP_ID, CLI_ID, getAuytDepId, getAuytId, 1, 1, 0, 0, '100.0', 'Выгодоприобретатель, в том числе крупный участник (более 10% в капитале)', '0.0', '', trunc(sysdate), null, null, null, null, 0, 0);
              COMMIT;
              UPDATE G_AFF
              SET AFFKND = 'Выгодоприобретатель (ФЛ-бенефициарный собственник, владеющий более 25% в капитале)'
              WHERE OBJDEP_ID = CLI_DEP_ID 
              AND OBJ_ID = CLI_ID;
              COMMIT;
            END IF;
            EXECUTE IMMEDIATE 'ALTER TRIGGER COLVIR.G_AFF_DTU ENABLE';
            EXECUTE IMMEDIATE 'ALTER TRIGGER COLVIR.Z_077_BENIF_TRIGGER ENABLE';
            /*---------*/
            Z_077_PKG_CLIENT.pControlCard(nDep_id => CLI_DEP_ID,
                                          nId => CLI_ID);
            COMMIT;
        END IF;
      dbms_output.put_line('1out -> ' || CLI_CODE || '; 2out-> ' || p_errfl || '; 3out-> ' || p_errMsg || '; 4out-> ' || nRes || '; 5out-> ' || sType);
      CODE_CLIENT := CLI_CODE;
      commit;
       END;
/
