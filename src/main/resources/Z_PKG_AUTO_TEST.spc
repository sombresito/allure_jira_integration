create or replace package Z_PKG_AUTO_TEST
/*   :  Created Vikhrenko E.S : 21.11.2022 00:00:00 */
 IS
  /* процедура пополнение счета(создание платежного ордера 214/215 с операцией 02016 в задаче SORDPAY) Вихренко Е. С. */
  PROCEDURE p_reset_accPAY(n_acc_code in varchar2, nsummoper IN VARCHAR2);
  /*  процедура пополнение счета(создание приходный мемориальный ордер 711/721 с операцией PO2004) */
  PROCEDURE p_outside_Balance(n_acc_code in varchar2, nsummoper IN VARCHAR2);
  /* функция возвращает код продукта договора РКО по счету клиента */
  function f_get_dcl_code(acc_code in varchar2) return varchar2;
  /* функция наличие проверок/арестов/блокировок */
  function f_get_accPay(p_dep_id IN NUMBER, p_id IN NUMBER) RETURN INTEGER;
  /* функция наличие пс счета привязанного к договору */
  function f_get_ps(ndep_id in t_dea.dep_id%type,
                    nid     in t_dea.id%type,
                    acc     in ledacc_std.code%type) return g_accbln.id%type;
  /* функция создание клиентского счета */
  function p_acc_client(p_cli_id  IN INTEGER,
                        p_cli_dep IN INTEGER,
                        val_code  in VARCHAR2) return varchar2;
  /* функция получение привязанного счета из (ledacc_det) договора */
  function f_get_ps_account(ndep_id in t_dea.dep_id%type,
                            nid     in t_dea.id%type,
                            acc     in g_accbln.code%type)
    return g_accbln.code%type;
  /* функция наличие счета по виду сумм во в вкладке Расчеты по договору */
  function f_payment_attribute(ndep_id in t_dea.dep_id%type,
                               nid     in t_dea.id%type,
                               acc     in g_accbln.code%type,
                               vidsumm in T_ARLDSC.LONGNAME%type)
    return number;
  /* функция наличия счета в атрибутах платежей по договору */
  function f_cliacc_attribute(ndep_id in t_dea.dep_id%type,
                              nid     in t_dea.id%type,
                              acc     in g_accbln.code%type,
                              cliacc  in T_PAYATRTYP.code%type) return number;

  /* процедура удаление таблиц с наименованием dataset */
  PROCEDURE z_026_delete_tables(p_date IN OUT DATE, p_int INTEGER);

  /* процедура создания карточки пользователя Физ. лицо*/
  PROCEDURE p_create_card_fl(n_inn_code in varchar2, n_num_phone in varchar2);

  /* процедура создания карточки пользователя юр. лицо*/
  PROCEDURE p_create_card_jur(n_inn_code in varchar2);

  /* процедура создания карточки пользователя ип*/
  PROCEDURE p_create_card_pboul(n_inn_code in varchar2, n_num_phone in varchar2);

  /*Процедура по созданию документов в состояние введен*/
  PROCEDURE p_crate_doc_ts(n_acc_code in varchar2, sAMOUNT in varchar2, trn_acc in varchar2, sKSO_CODE in varchar2, sCHA_CODE in varchar2, sKNP in varchar2, sCODE_OD in varchar2, sCODE in varchar2);

  /*Процедура по созданию текущих счетов*/
  PROCEDURE p_create_acc(nDepCode in varchar2, nValCode in varchar2, nCliCode in varchar2, sDclCode in varchar2);

  /*Процедура по созданию текущих счетов для Java*/
  PROCEDURE p_create_acc_java(nDepCode in varchar2, nValCode in varchar2, nCliCode in varchar2, sDclCode in varchar2, AccCode out varchar2);

  /* Процедура по созданию документа в досье клиентской карточки */
  PROCEDURE pCreDocDosCli(cliCode in varchar2,
                          docType varchar2,
                          docGrp varchar2,
                          longname varchar2,
                          docDate varchar2,
                          docNum varchar2,
                          copyFl varchar2,
                          docPycCnt integer,
                          expDate varchar2,
                          docPubl varchar2,
                          docDsc varchar2,
                          notifyFl varchar2,
                          storage varchar2,
                          eCopy varchar2,
                          fullPath varchar2 default null,
                          fileExt varchar2 default null,
                          dbFl varchar2 default '0');

  /* Процедура по созданию документа в досье клиентской карточки для java*/
  PROCEDURE pCreDocDosCliJava(cliCode in varchar2,
                          docType varchar2,
                          docGrp varchar2,
                          longname varchar2,
                          docDate varchar2,
                          docNum varchar2,
                          copyFl varchar2,
                          docPycCnt integer,
                          expDate varchar2,
                          docPubl varchar2,
                          docDsc varchar2,
                          notifyFl varchar2,
                          storage varchar2,
                          eCopy varchar2,
                          fullPath varchar2 default null,
                          fileExt varchar2 default null,
                          dbFl varchar2 default '0',
                          nord out varchar2);

  /* Процедура по созданию электронной копии документа */
  PROCEDURE pCreEDocDosCli(fullPath in varchar2, fileExt varchar2, dbFl varchar2, idEDoc out number);

  /* Процедура устанавливающая блокировки на счет */
  PROCEDURE AT_pSetBlockAcc(accCode in varchar2, lLockType integer, lockName varchar2, sReason varchar2, dBegin varchar2, sAmount varchar2, sValCode varchar2);

  /* Процедура устанавливающая блокировки на счет для джавы */
  PROCEDURE AT_pSetBlockAccJava(accCode in varchar2, lLockType number, lockName varchar2, sReason varchar2, dBegin varchar2, sAmount varchar2, sValCode varchar2, lockId out number);

  /* Процедура, которая снимает блокировки со счета */
  PROCEDURE AT_pUndoSetBlockAcc(accCode varchar2, lLockId number, lockName varchar2, sReason varchar2);

  /* Процедура, которая снимает блокировки со счета для джавы*/
  PROCEDURE AT_pUndoSetBlockAccJava(accCode varchar2, lLockId number, lockName varchar2, sReason varchar2, status out varchar2);

  /* Процедура устанавливает/снимает признак администратора у пользователя */
  PROCEDURE AddTakeOffAdmPrm(USER_LOGIN IN VARCHAR2, TYPE_ACT NUMBER);

  /* Процедура для создания пользователя Колвир */
  PROCEDURE CreateUserColvir(LOGIN in varchar2,
                             PASSWORD in varchar2 default null,
                             NAME_USER in varchar2,
                             FIO in varchar2,
                             EMAIL in varchar2 default null,
                             PHONE in varchar2 default null,
                             ARM in C_USR.USE_ID%TYPE,
                             DEP in varchar2,
                             FROMDATE in date,
                             TODATE in date,
                             PRIM in varchar2,
                             VIRTUALFL in char default '0',
                             WEB_TYPE in char default '1',
                             ADM in C_USR.DBAFL%TYPE default 1,
                             GRPFL in C_USR.GROUPFL%TYPE default 0,
                             ARCFL_POS in C_USR.ARCFL%TYPE default 0,
                             ARCFL_USER in char default '0',
                             ARESTFL_POS in C_USR.ARESTFL%TYPE default 0,
                             ARESTFL_USER in char default '0');

  /* Процедура для назначения полномочий тестовому пользователю Колвир */
  PROCEDURE AT_pSetUsrGrn(TEST_USER_ID number, REAL_USER_ID number);
  /* Процедура добавляет ПКБ отчет клиенту*/
  PROCEDURE AT_pSetPkbReport(CLI_CODE in varchar2);
  /* Процедура для назначения конкретного полномочия из датасета*/
  PROCEDURE AT_pSetGrn(id_grn in number, id_grn_2 in number default null, user_pos_id in number, type_grn in varchar2);
  /* Процедура для инсерта полномочий в датасет GRNUSERS*/
  PROCEDURE AT_pInsGrnDtst(ID in number,
                         REAL_LOGIN in varchar2,
                         TYPE_GRN in varchar2,
                         ID_GRN in number,
                         ID_GRN_2 in number);
  /* Процедура создает договор СКС в статус "Введен" */
  PROCEDURE AT_pCreSKSDea(CLI_CODE in varchar2, TYPE_DEA in varchar2, NUM_DEA in varchar2, ID_TRF_CAT in varchar2);
  /* Процедура создает карточный договор и привязывает его к договору СКС */
  PROCEDURE AT_pCreCardDea(DepCode in varchar2, -- код департамента создаваемого карточного договора
                           DEA_CODE_SKS in varchar2, -- код договора договора СКС
                           DCL_CODE in varchar2, -- код продукта карточного договора
                           CRD_ID in number, -- id типа карточки
                           CARDCODE in varchar2, -- код карточки
                           EMBOSSEDNAME in varchar2, -- имя карточки
                           CLI_CODE in varchar2, -- код карточки клиента
                           ACC_CODE in varchar2, -- Номер тек счета клиента
                           VAL_CODE in varchar2, -- Валюта
                           TRF_CODE in varchar2, -- Код тарифной категории
                           CODE in varchar2, -- IDN карточки
                           NOEMBFL in number); -- признак неименной карточки. 0 - нет, 1 - да.
  /* Процедура создает пул неименных карточек */
  PROCEDURE AT_pCreNoPersonalCard(DEP_CODE in varchar2, CODE_PRODUCT_CARD in varchar2, VAL_CODE in varchar2, COUNT_CARD in number);
  /* Процедура по выполнению операций без параметров */
  PROCEDURE AT_pRunOperWOParams(CODE_NUM in varchar2, -- Номер документа по которому необходимо выполнить операцию
                                OPER_CODE in varchar2); -- Код операции. Можно посмотреть в DOP1
  /* Процедура выполняет операции с параметрами */
  PROCEDURE AT_pRunOperWithParams(docNum varchar2, operCode varchar2, params varchar2);
  /* Процедура по добавлению платежных инструкций в карточку клиента */
  PROCEDURE AT_pAddPaymentInstructions(cliId varchar2, depId varchar2, fCode varchar2, fId varchar2, swiftName varchar2);
  /* Процедура для пополнения текущих счетов через приходный кассовый ордер */
  PROCEDURE AT_pRepCurAccCashOrder(AccCode varchar2, Summ varchar2, Iin varchar2, ValCode varchar2);
  /* Процедура добавляет согласие на проверку КБ */
  PROCEDURE AT_pInsYesKb(cli_code varchar2);
  /* Процедура для установки индивидуальной ставки в кредитном договоре */
  PROCEDURE AT_pCreIndividRate(docNum varchar2, value_rate varchar2);
  /* Процедура для создания кредитного договора */
  PROCEDURE AT_pCreCreditContract(depContract varchar2, productCode varchar2, fromdate varchar2, todate varchar2,
                                                  valCode varchar2, amount varchar2, period varchar2, cliCode varchar2, prim varchar2,
                                                  codeContract varchar2, purpose varchar2, trfCode varchar2, individRate varchar2,
                                                  individPrim varchar2, dateDecisionKK varchar2, sourceFinance varchar2,
                                                  signFinance varchar2, noteFinance varchar2, sourceFinanceKM varchar2,
                                                  dateAgreement varchar2, marketRate varchar2, SPPI varchar2,  balanceDebt varchar2,
                                                  creditOfficer varchar2, creditAdmin varchar2, issuingApproval varchar2,
                                                  dateSchedule varchar2, appLoan integer, dateSigning varchar2,
                                                  payDay varchar2 default NULL, payDayOd varchar2 default NULL,
                                                  numDecisionKK varchar2 default NULL, entrepStat varchar2 default NULL,
                                                  lnGuarDamu varchar2 default NULL, gosProg varchar2 default NULL,
                                                  subsidizing varchar2 default NULL, womBusinessSub varchar2 default NULL);
  /* Процедура для проверки ИИН */
  PROCEDURE AT_pCheckIinCli(IIN varchar2);
  /* Процедура для регистрации договора СКС */
  PROCEDURE AT_pRegSksContract(DocNum varchar2);
  /* Процедура для снятия блокировки с контроля лимитов */
  PROCEDURE AT_pUndoBlockLimit(idLim number);
  /* Процедура для установки пути SWIFT */
  PROCEDURE AT_pSetSwiftPath(PathValue varchar2);
  /* Функция для получения айди департамента */
  FUNCTION AT_fGetIdDep(DepCode varchar2) RETURN number;
  /* Функция для получения id договора СКС по номеру договора */
  FUNCTION AT_fGetIDSksDoc(DocNum varchar2) RETURN NUMBER;
  /* Функция возвращает номер договора кредитной линии */
  FUNCTION AT_fGetDocNumCreditLine(CliCode varchar2, DocNumSks varchar2) RETURN VARCHAR2;
  /* Функция возвращает карточный счет клиента */
  FUNCTION AT_fGetCardAccNum(CliCode varchar2) RETURN VARCHAR2;
  /* Функция возвращает Статус лимита */
  FUNCTION AT_fGetStatusLimit(idLim number) RETURN VARCHAR2;
  /* Функция возвращает ID карточного договора */
  FUNCTION AT_fGetIdDoc(docNum varchar2) RETURN NUMBER;
  /* Функция возвращает DEP_ID карточного договора */
  FUNCTION AT_fGetDepIdDoc(docNum varchar2) RETURN NUMBER;
  /* Процедура отправки на акцептование карточного договора */
  PROCEDURE AT_pSendToAcceptCard(docNum varchar2);
  /* Процедура принятия решения по акцепту */
  PROCEDURE AT_pMakeDecisionAcceptCard(docNum varchar2);
  /* Функция для получения id БПС */
  FUNCTION AT_fGetIdBpsAcc(codeBps varchar2) RETURN NUMBER;
  /* Функция для получения id ПС счета */
  FUNCTION AT_fGetIdPsAcc(codePs varchar2) RETURN NUMBER;
  /* Процедура для привязки БПС к ПС */
  PROCEDURE AT_pAddBpsToPs(psCode varchar2, bpsCode varchar2);
  /* Получение id доп соглашения */
  FUNCTION AT_fGetIdAgr(DocNumCredit varchar2) RETURN NUMBER;
  /* Функция для получения dep_id доп соглашения */
  FUNCTION AT_fGetDepIdAgr(DocNumCredit varchar2) RETURN NUMBER;
  /* Процедура для отмены всех операций по доп соглашению */
  PROCEDURE AT_pUndoAllOperAgr(DocNumCredit varchar2);
  /* Процедура для удаления доп соглашения */
  PROCEDURE AT_pDelAgr(DocNumCredit varchar2);
  /* Процедура для создания документа Sordpay */
  PROCEDURE AT_pCreateSordpayDoc(docNum varchar2, typeDoc varchar2, typeOper varchar2, amount varchar2, valCode varchar2,
                               codeAccSend varchar2, BikSend varchar2, codeAccGet varchar2, IinGet varchar2,
                               fioHead varchar2, fioGet varchar2, knp varchar2, codeSend varchar2,
                               codeGet varchar2);
  /* Процедура для выполнения операции Оплатить в Sordpay */
  PROCEDURE AT_pOperPaySordpay(depId number, id number);
  /* Процедура для пополнения счета в SORDPAY */
  PROCEDURE AT_pReplAccSordpay(docNum varchar2, typeDoc varchar2, typeOper varchar2, amount varchar2, valCode varchar2,
                             codeAccSend varchar2, BikSend varchar2, codeAccGet varchar2, IinGet varchar2,
                             fioHead varchar2, fioGet varchar2, knp varchar2, codeSend varchar2,
                             codeGet varchar2);
  /* процедура добавляет опердень в журнал, если его там нет */
  PROCEDURE AT_pSetFutureOperDayToJrn(countDays number);
  /* функция для получения даты платежа из графика платежей */
  FUNCTION AT_fGetPayDateFromGraph(DocNum varchar2) RETURN varchar2;
  /* Процедура для установки/снятия признака автооткрытия пс счета */
  PROCEDURE AT_pUpdAutoFlPsAcc(psAcc varchar2, flagAtoFl varchar2);
  /* функция для получения Менеджера НПК БК */
  FUNCTION AT_fGetAdminManager RETURN varchar2;
  /* Процедура для снятия блокировки в аккаунта WU */
  PROCEDURE AT_pSetUnlockAccWU(numAcc varchar2);
  /* Процедура для ренейма аккаунтов WU */
  PROCEDURE AT_pRenameAccauntWU(codeAcc varchar2);
  /* функция для получения сli_id */
  FUNCTION AT_fGetCliId(DocNumCredit varchar2) RETURN varchar2;
  /* функция для получения сli_dep_id */
  FUNCTION AT_fGetCliDepId(DocNumCredit varchar2) RETURN varchar2;
  /* функция для получения номера счета по договору */
  FUNCTION AT_fGetPayAccNum(DocNumCredit varchar2) RETURN varchar2;
  /* Процедура по сохранению номера счета в договоре */
  PROCEDURE AT_pSavePayAccAtr(DocNumCredit varchar2);
  /* Функция по получению сейфового ячейки */
  FUNCTION AT_fGetFreeSafeBox(depcode varchar2, storage varchar2) RETURN varchar2;
  /* Процедура для установки курсов валют */
  PROCEDURE AT_pSetValRate(valId number, rate number, dateVal varchar2);
   /* Функция по получению иин клиента */
  FUNCTION AT_fGetIinCli(cliCode varchar2) RETURN varchar2;
  /* Функция по получению номера документа */
  FUNCTION AT_fGetCashOrderCode(cliCode varchar2) RETURN varchar2;
  /* Функция возвращающая id кассы */
  FUNCTION AT_fGetCashId(cashCode varchar2) RETURN NUMBER;
  /* Функция возвращающая dep_id кассы */
  FUNCTION AT_fGetCashDepId(cashCode varchar2) RETURN NUMBER;
  /* Функция для получения id позиции по имени поизиции юзера */
  FUNCTION AT_fGetIdUserPosition(userCode varchar2) RETURN NUMBER;
  /* Процедура для добавления тестового юзера в кассиры */
  PROCEDURE AT_pAddTestUserToCash(userName varchar2, codeCash varchar2);
  /* Процедура для добавления тестового юзера в кассиры по id позиции юзера */
  PROCEDURE AT_pAddTestUserIdToCash(userPosId number, codeCash varchar2);
  /* Процедура для апдейта активнго юзера кассы */
  PROCEDURE AT_pUpdUserCash(userName varchar2, codeCash varchar2);
  /* Процедура для апдейта активнго юзера кассы по id позиции юзера*/
  PROCEDURE AT_pUpdUserIdCash(userPosId number, codeCash varchar2);
  /* Функция дл получения статуса кассы */
  FUNCTION AT_fGetStatusCash(codeCash varchar2) RETURN VARCHAR2;
  /* Процедура для выполнения операции открытия кассы */
  PROCEDURE AT_pOpenCash(codeCash varchar2);
  /* Функция для получения id карточки по коду карточки клиента*/
  FUNCTION AT_fGetCliIdFromCodeCard(cliCode varchar2) RETURN NUMBER;
  /* Функция для получения dep_id карточки по коду карточки клиента*/
  FUNCTION AT_fGetCliDepIdFromCodeCard(cliCode varchar2) RETURN NUMBER;
  /* Процедура для добавления рег документа в карточку клиента */
  PROCEDURE AT_pAddCliRegDocBase(cliCode varchar2);
  /* Процедура для отключения/включения проверки Стоп кредит через таблицу принятия решений */
  PROCEDURE AT_pSetStopCreditCheck(nord varchar2, decVal varchar2);
  /* Процедура для создания кассового документа в MADVPAY */
  PROCEDURE AT_pCreateCashDoc(ksoCode varchar2, cshCode varchar2, cshCode2 varchar2, usr2Code varchar2, dscr varchar2,
                              fio varchar2, valCode varchar2, icsId number, icsDepId number);
  /* Функция для получения статуса проверки на признак Стоп Кредит у клиента */
  FUNCTION AT_fGetStopCreditVal(nord integer) RETURN varchar2;
  /* процедура для изменения статуса проверки на признак Стоп Кредит у клиента */
  PROCEDURE AT_pChangeStopCredit(nord number, status varchar2);
  /* процедура активации С - файла АТМ2402*/
  PROCEDURE p_activate_c_file_2402(c_id_code number);
  /* процедура активации С - файла POS2416 */
  PROCEDURE p_activate_c_file_2416(c_id_code number);
  /* процедура обновления признаков договора в задаче GRN */
  PROCEDURE turn_off_doc_sign(C_ID number, C_DEP_ID number, id_par number, in_value varchar2 default NULL);
  /* функция возвращает рандомное число */
  FUNCTION AT_fGetRandomNum(cnt number) RETURN NUMBER;
  /* Функция возвращает longname по переданому ИИН клиента */
  FUNCTION AT_fGetLongnameCli(iinCli varchar2) RETURN VARCHAR2;
  /* функция возвращает номер документа из t_ord */
  FUNCTION AT_fGetDocNum(depId number, idDoc number) RETURN VARCHAR2;
  /* Функция создает документа в Sordpay и возвращает номер созданного документа */
  FUNCTION AT_pCreateIrPtpForCard2(depCode varchar2, orderType varchar2, operCode varchar2, summOper varchar2, valSumm varchar2,
                                    payerAcc varchar2, bik varchar2, payerGet varchar2, iinGet varchar2,
                                    fioHead varchar2, knp varchar2, kod varchar2, kbe varchar2) RETURN VARCHAR2;
  /* Процедура для выполнения операции Регистрация требования */
  PROCEDURE AT_pRegIrPtp(docNum varchar2);
  /* Процедура выполняет операцию Поместить в картотеку */
  PROCEDURE AT_pInputToCard2(docNum varchar2, queue varchar2);
  /* Функция возращает статус документа */
  FUNCTION AT_fGetStatusDoc(docNum varchar2) RETURN VARCHAR2;
  /* Функция для получения id документа из t_ord для Кредитных линий SLLOAN */
  FUNCTION AT_fGetIdDocCL(docNum varchar2) RETURN NUMBER;
  /* Функция для получения dep_id документа из t_ord для Кредитных линий SLLOAN */
  FUNCTION AT_fGetDepIdDocCL(docNum varchar2) RETURN NUMBER;
  /* Функция для получения id индивидуальной ставки договора */
  FUNCTION AT_fGetPcnId(depId integer, docId integer, pcnType varchar2) RETURN NUMBER;
  /* Процедура для изменения индивидуальной ставки договора Кредитных линий */
  PROCEDURE AT_pSetPcnValue(docnum varchar2, pcnTypeVal varchar2, perval varchar2);
  /* Процедура для выполнения операции Поместить в картотеку 2 для ПТП */
  PROCEDURE AT_pInputPtpToCard2(docNum varchar2, -- номер документа в Sordpay
                                queue varchar2); -- приоритет помещения
  /* Функция для получения даты для редактирования графика AHDCONT*/
  FUNCTION AT_fGetPlanDateFromGraphicDog(paymentName varchar2, depId number, dId number) RETURN VARCHAR2;
  /* Процедура для создания договора Гарантии*/
 /* PROCEDURE AT_pCreGrn(P_DEP_ID, P_ID, P_DCLID, P_CLI_DEPID, P_CLI_ID, P_SDOK DEFAULT '0', P_VAL_ID DEFAULT NULL, P_COR_DEPID DEFAULT NULL, P_COR_ID DEFAULT NULL,
                       P_CODE DEFAULT NULL, P_NUMDEAL DEFAULT NULL, P_DORD DEFAULT SYSDATE, P_FROMDATE DEFAULT SYSDATE, P_TODATE DEFAULT NULL, P_PARENT_PROC DEFAULT NULL,
                       P_PARENT_OPR DEFAULT NULL, P_GRNNAZN DEFAULT NULL, P_DEA_DEP_ID DEFAULT NULL, P_DEA_ID DEFAULT NULL, P_SRV_DEP_ID DEFAULT NULL);*/
  /* Функция возвращает ID кода продукта */
  FUNCTION AT_fGetIdProductCode(productCode varchar2) RETURN NUMBER;
  /* Функция возвращает id валюты */
  FUNCTION AT_fGetIdVal(valCode varchar2) RETURN NUMBER;
  /* Функция возвращает id тарифной категории */
  FUNCTION AT_fGetIdTrfCat(trfCode varchar2) RETURN NUMBER;
  /* Функция возвращает longname клиента по коду клиента */
  FUNCTION AT_fGetLongnameCliByCodeCard(cardCode varchar2) RETURN VARCHAR2;
  /* Процедура создает договор аккредитива */
  PROCEDURE AT_pCreAkkred(aDep varchar2, aProductCode varchar2, aValCode varchar2, aSum varchar2, aCliCode varchar2,
                          aDocNumCreLine varchar2, aTrfCat varchar2, aCliAcc varchar2, aTodate varchar2,
                          aCorAgentCode varchar2, aSrvDepCode varchar2, typeDoc varchar2, bnkRole varchar2,
                          execmeth varchar2);
  /* Функция возвращает dep_id аккрудитива */
  FUNCTION AT_fGetDepIdAkkred(docNum varchar2) RETURN NUMBER;
  /* Функция возвращает id аккредитива */
  FUNCTION AT_fGetIdAkkred(docNum varchar2) RETURN NUMBER;
  /* Функция возвращает id типа документа ( не договора, а именно документа ) */
  FUNCTION AT_fGetIdTypeDoc(codeDoc varchar2) RETURN NUMBER;
  /* Процедура добавляет в аккредитив тип документа (вкладка Типы документов) */
  PROCEDURE AT_pAddDocTypeAkkred(docNumAkkred varchar2, typeDoc varchar2);
  /* Процедура выполняет операция Расчет графика */
  PROCEDURE AT_pCalcSheduleAkkred(docNum varchar2);
  /* Процедура выполняет операцию Регистрация аккредитива */
  PROCEDURE AT_pRegAkkred(docNum varchar2, operCode varchar2);
  /* Процедура выполняет операцию Учет ккредитива */
  PROCEDURE AT_pUchetAkkred(docNum varchar2);
  /* Функция возвращает дату платежа из графика аккредитива */
  FUNCTION AT_fGetPayDateFromGraphAkkred(DocNum varchar2) RETURN varchar2;
  /* Функция возвращает референс операции */
  FUNCTION AT_fGetReferOper(docNum varchar2, codeOper varchar2) RETURN VARCHAR2;
  /* Функция возвращает референс операции платежного документа по номеру договора */
  FUNCTION AT_fGetReferOperForPayDoc(docNum varchar2, codeOper varchar2) RETURN VARCHAR2;
  /* Функция возвращает id платежного атрибута */
  FUNCTION AT_fGetIdPayAttr(codeAttr varchar2) RETURN NUMBER;
  /* Функция возвращает nord платежного атрибута */
  FUNCTION AT_fGetNordIdPayAttr(depIdDoc number, idDoc number, idAttr number, accCodeAttr varchar2) RETURN NUMBER;
  /* Функция возвращает longname платежного атрибута */
  FUNCTION AT_fGetLongnamePayAttr(docNum varchar2, codeAttr varchar2, accCodeAttr varchar2) RETURN VARCHAR2;
  /* Функция возвращает класс объекта DDPVCS из кода ошибки "Объект 'DECTBL;Z_077_DSBL_OPR_BLK' уже корректируется разработчиком '***'" */
  FUNCTION AT_fGetErrClassCode(err_message VARCHAR2) RETURN VARCHAR2;
  /* Функция возвращает код объекта DDPVCS из кода ошибки "Объект 'DECTBL;Z_077_DSBL_OPR_BLK' уже корректируется разработчиком '***'" */
  FUNCTION AT_fGetErrObjCode(err_message IN VARCHAR2) RETURN VARCHAR2;
  /* Процедура сбрасывает статус объекта в Colvir DDPVCS по классу и коду объекта, например, objCode = 'Z_PKG_AUTO_TEST', classCode = 'PKG' */
  PROCEDURE AT_pCancelColvirObjStat(classCode varchar2, objCode varchar2);
  /* Процедура по созданию договора для списания товарно-материальных ценностей, задача OUT */
  PROCEDURE AT_pCreateOutDoc(productCode varchar2, depCode varchar2, depOrd varchar2, senderId number,
                             pzOfl varchar2, calcndsFl varchar2, costndsfl varchar2, arestFl varchar2, prim varchar2,
                             typeDocReason number, descDocReason varchar2);
end Z_PKG_AUTO_TEST;
