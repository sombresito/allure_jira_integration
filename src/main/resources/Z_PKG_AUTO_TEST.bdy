CREATE OR REPLACE PACKAGE BODY Z_PKG_AUTO_TEST IS

PROCEDURE p_reset_accPAY(n_acc_code in varchar2, nsummoper in varchar2) IS


bImport      boolean;
s_errtxt     varchar2(2000);
nDepId       T_ORD.DEP_ID%TYPE;
nOrdId       T_ORD.ID%TYPE;
sIdnDt       G_CLIHST.TAXCODE%type;
sNamePay     G_CLIHST.LONGNAME%type;
sRuk         varchar2(250);
sBuch        varchar2(250);
sResidDt     varchar2(1);
sSectDt      varchar2(1);
sIdnCt       G_CLIHST.TAXCODE%type;
sNameBen     G_CLIHST.LONGNAME%type;
sResidCt     varchar2(1);
sSectCt      varchar2(1);
sKsoCode     varchar2(10);
sChaCode     varchar2(20):='02016';
dValDate     date;
sTxtDscr     varchar2(500);
RESOURCE_BUSY exception;
pragma exception_init (RESOURCE_BUSY,-54);
cUsrPos      VARCHAR2(50);
nTusId       C_USR.ID%TYPE;

s_response   VARCHAR2(2000);
sdOper       varchar2(20);
dOper        date;

val_code     varchar2(20) := null;
trn_acc      varchar2(30) := null;
BEGIN
  c_pkgconnect.popen();

  begin
    select ID, CODE INTO nTusId, cUsrPos from C_USR c where c.code = 'COLVIR';
  exception
  when others then
    cUsrPos := NULL;
  end;
            dbms_output.put_line('1 ');

  COLVIR.c_pkgconnect.popen(cUsrPos=>cUsrPos);
  COLVIR.c_pkgconnect.pOpenLink(cUserName => cUsrPos, iBss => 1, WNDS_FL => 1);
  dValDate := p_operday ;
  C_PkgSession.dOper := dValDate;

      select v.code into val_code
        from g_accbln a
        join g_accblnhst ah on a.dep_id = ah.dep_id and a.id = ah.id and p_operday between ah.fromdate and ah.todate
        join t_val_std    v on nvl(ah.val_id,1) = v.id
       where a.code = n_acc_code;

      select a.code into trn_acc
        from g_accbln a
        join g_accblnhst ah on a.dep_id = ah.dep_id and a.id = ah.id
                               and p_operday between ah.fromdate and ah.todate and ah.arcfl = '0'
        join ledacc_std  l on a.cha_id = l.id and substr(l.code,1,4) = '2204'

        join t_val_std    v on nvl(ah.val_id,1) = v.id
       where -1*T_PkgAccBal.fAccBal(a.dep_id, a.id, p_operday, 0, p_natval, 0, 0) > 1000000
         and a.code != n_acc_code
         and v.code = val_code
         and rownum < 2;

  if val_code = 'KZT' then
    sKsoCode := '214';
  else
    sKsoCode := '215';
  end if;
            dbms_output.put_line('sKsoCode '||sKsoCode);
  BEGIN
     SELECT ga.DEP_ID,
            gc.TAXCODE,
            gc.LONGNAME,
            G_PKGCLI.FRUK(gc.dep_id, gc.id) ruk,
            nvl(G_PKGCLI.FBUCH(gc.dep_id, gc.id), '-') buch,
            decode(G_PKGCLI.FGETCLIRESIDFL(gc.dep_id, gc.id), 0, 2, G_PKGCLI.FGETCLIRESIDFL(gc.dep_id, gc.id)) resid,
            G_PKGCLI.FGETCLISECT_ID(gc.dep_id, gc.id) sect
       INTO nDepId,
            sIdnDt,
            sNamePay,
            sRuk,
            sBuch,
            sResidDt,
            sSectDt
     FROM G_ACCBLN ga,
          G_ACCBLNHST gh,
          G_CLIHST gc
    WHERE ga.DEP_ID=gh.DEP_ID AND ga.ID=gh.ID and dValDate BETWEEN gh.FROMDATE and gh.TODATE and gh.arcfl = 0
      AND gh.CLIDEP_ID=gc.DEP_ID and gh.CLI_ID=gc.ID AND dValDate BETWEEN gc.FROMDATE and gc.TODATE and gc.arcfl = 0
      AND ga.CODE = trn_acc;
    EXCEPTION WHEN NO_DATA_FOUND THEN
      dbms_output.put_line('err1 ');
      dbms_output.put_line('err = '||sqlerrm||' '||DBMS_UTILITY.FORMAT_ERROR_BACKTRACE);
  END;


            dbms_output.put_line('nDepId '||nDepId);
            dbms_output.put_line('sIdnDt '||sIdnDt);
            dbms_output.put_line('sNamePay '||sNamePay);
            dbms_output.put_line('sRuk '||sRuk);
            dbms_output.put_line('sBuch '||sBuch);
            dbms_output.put_line('sResidDt '||sResidDt);
            dbms_output.put_line('sSectDt '||sSectDt);
      begin
         SELECT gc.TAXCODE,
                gc.LONGNAME,
                decode(G_PKGCLI.FGETCLIRESIDFL(gc.dep_id, gc.id), 0, 2, G_PKGCLI.FGETCLIRESIDFL(gc.dep_id, gc.id)) resid,
                G_PKGCLI.FGETCLISECT_ID(gc.dep_id, gc.id) sect
           INTO sIdnCt,
                sNameBen,
                sResidCt,
                sSectCt
         FROM G_ACCBLN ga,G_ACCBLNHST gh,G_CLIHST gc
        WHERE ga.DEP_ID=gh.DEP_ID AND ga.ID=gh.ID and dValDate BETWEEN gh.FROMDATE and gh.TODATE and gh.arcfl = 0
          AND gh.CLIDEP_ID=gc.DEP_ID and gh.CLI_ID=gc.ID AND dValDate BETWEEN gc.FROMDATE and gc.TODATE and gc.arcfl = 0
          AND ga.CODE = n_acc_code;
        exception when no_data_found then
          null;
          dbms_output.put_line('err2 ');
          dbms_output.put_line('err = '||sqlerrm||' '||DBMS_UTILITY.FORMAT_ERROR_BACKTRACE);
      end;

            dbms_output.put_line('sIdnCt '||sIdnCt);
            dbms_output.put_line('sNameBen '||sNameBen);
            dbms_output.put_line('sResidCt '||sResidCt);
            dbms_output.put_line('sSectCt '||sSectCt);
        begin
          S_BsPay.pSave(
              nDEP_ID     => nDepId,
              nID         => nOrdId,
              sKSO_CODE   => '315',
              sCHA_CODE   => '02016',
              sAMOUNT     => nsummoper,
              sVAL_CODE   => val_code,
              dDORD       => dValDate,
              dDVAL       => dValDate,
              sCODE_ACL   => trn_acc,
              sCODE_BCR   => 'KCJBKZKX',
              sCODE_ACR   => n_acc_code,
              sRNN_CR     => sIdnCt,
              sCODE_BC    => null,
              sTXT_HEAD   => null,
              sTXT_BUCH   => sBuch,
              sTXT_DSCR   => 'Перевод денежных средств для автотестирования',
              sTXT_BEN    =>  null,
              nNOCMSFL    => 0,
              sKNP        => '190',
              sCODE_OD    => '19',
              sCODE_BE    => sResidCt||sSectCt,
              sPRIM       => 'Примечание',
              sCODE       => to_char(sysdate,'YYMMDDHHMISS'),
              nFLZO       => 0,
              iVrfFl      => null,
              sLIMFL      => null,
              iPrintFl    => null,
              sSPEEDFL    => 0,
             sSOST       => null,
              sREFER      => null,
              p_parentProc=> null,
              p_parentOpr => null,
              sRnnCli     => sIdnDt,
              sTxtPay     => sNamePay,
              sVOper      =>'01',
              SGCVP       => null,
              nDEPUSR_ID  => nDepId);
          update S_ORDPAY set ALTERCODE = 'AUTOTEST' where ID = nOrdId and DEP_ID = nDepId;
        exception
          when others then
            rollback;
            s_errtxt := substr('pSave: '||SQLERRM||' '||DBMS_UTILITY.FORMAT_ERROR_BACKTRACE, 1, 400);
            dbms_output.put_line('s_errtxt '||s_errtxt);
        end;
            dbms_output.put_line('5 ');
     if s_errtxt is null then
          begin
              colvir.T_PkgRunOprUtl.pRunOprByMainOrd(nDepId, nOrdId,'PAY');
              commit;
          exception
           when others then
 s_errtxt := substr('pSave: '||SQLERRM||' '||DBMS_UTILITY.FORMAT_ERROR_BACKTRACE, 1, 400);
            dbms_output.put_line('s_errtxt '||s_errtxt);
                    rollback;
          end;
      end if;
            dbms_output.put_line('1');
exception
when others then
    rollback;
    dbms_output.put_line('err = '||sqlerrm||' '||DBMS_UTILITY.FORMAT_ERROR_BACKTRACE);
end;



PROCEDURE p_outside_Balance(n_acc_code in varchar2, nsummoper in varchar2) IS
    n_ord_Depid NUMBER;
    n_ord_id    NUMBER;
    n_val_code  varchar(5);
    n_skso_code NUMBER;

  begin
    FOR rec_ac IN (select g.dep_id,
                          g.code,
                          ag.taxcode,
                          a.bin_iin,
                          t_pkgval.fGetValCodeAccId(g.dep_id, g.id) as val
                     from g_accbln g, g_accblnhst gg, g_cli a, g_clihst ag
                    where g.code = n_acc_code
                      and g.id = gg.id
                      and g.dep_id = gg.dep_id
                      and p_operday between gg.fromdate and gg.todate
                      and gg.cli_id = a.id
                      and gg.clidep_id = a.dep_id
                      and a.id = ag.id
                      and a.dep_id = ag.dep_id
                      and p_operday between ag.fromdate and ag.todate) loop

      n_ord_Depid := rec_ac.dep_id;
      n_ord_id    := null;
      n_val_code  := rec_ac.val;

      if n_val_code = 'KZT' then
        n_skso_code := '711';
      else
        n_skso_code := '721';
      end if;

      BEGIN
        S_BSNBL.psave(NDEP_ID   => n_ord_Depid,
                      NID       => n_ord_id,
                      SKSO_CODE => n_skso_code,
                      DDDOK     => to_date(p_operday, 'dd.mm.yy'),
                      DDVAL     => to_date(p_operday, 'dd.mm.yy'),
                      SACC_CODE => n_acc_code,
                      SACR_CODE => '',
                      SAMOUNT   => nsummoper,
                      STXT_DSCR => 'От...Талканова Е..',
                      SORD_CODE => '',
                      SCHA_CODE => 'PO2004',
                      STXT_HEAD => '',
                      STXT_BUCH => '',
                      SPRIM     => '',
                      FRATE     => '',
                      SFIXRATE  => '1',
                      NFLZO     => 0);
        IF n_ord_id IS NOT NULL THEN
          BEGIN
            t_pkgrunoprutl.pRunOprByMainOrd(n_ord_Depid,
                                            n_ord_id,
                                            'MEMPOST');
          EXCEPTION
            WHEN OTHERS THEN
              ROLLBACK;
          END;
        END IF;
        COMMIT;
      END;
    END LOOP;
  END;

  function f_get_dcl_code(acc_code in varchar2) return varchar2 is
    vDCL_CODE varchar2(5);
  begin
    select nvl((select case
                        when g.code is not null then
                         dc.code
                        when g.code is null then
                         '0'
                        else
                         dc.code
                      END as code
                 from g_accbln g, s_deaacc d, t_dea t, t_deacls_std dc
                where g.id = d.acc_id
                  AND g.dep_id = d.acc_dep_id
                  and d.DEP_ID = t.DEP_ID
                  and d.ID = t.ID
                  AND dc.ID = t.dcl_id
                  and g.code = acc_code),
               1201)
      into vDCL_CODE
      from dual;
    return vDCL_CODE;
  exception
    when others then
      return(0);
  end;

  function f_get_accPay(p_dep_id IN NUMBER, p_id IN NUMBER) RETURN INTEGER IS
    i_incasso_count INTEGER := 0;
    i_crd2_cnt      INTEGER := 0;
    i_lock_count    INTEGER := 0;
    i_nondebfl_cnt  INTEGER := 0;

  BEGIN
    /*Инкассовые*/
    SELECT COUNT(1)
      INTO i_incasso_count
      FROM t_process p, t_procmem m, s_inicash ic
     WHERE ic.dep_id = m.dep_id
       AND ic.id = m.ord_id
       AND m.mainfl = 1
       AND p.id = m.id
       AND ic.dep_id = p_dep_id
       AND ic.acc_id = p_id
       AND (p.bop_id + 0, p.nstat) = ANY
     (SELECT st.id, st.nord
              FROM t_bop_stat st, t_bop_pos p
             WHERE p.tas_id = 10801
               AND p.mainfl = 1
               AND st.id = p.id
               AND st.forkfl = 0
               AND st.code IN ('$ANY', 'CARD2', 'CRD2'));
    /*Картотека 2*/
    SELECT COUNT(1)
      INTO i_crd2_cnt
      FROM t_bop_stat_std s, t_process p, t_procmem m, s_crd2 c2
     WHERE c2.dep_id = m.dep_id
       AND c2.id = m.ord_id
       AND m.mainfl = '1'
       AND p.id = m.id
       AND s.id = p.bop_id
       AND s.nord = p.nstat
       AND c2.dep_id = p_dep_id
       AND c2.acc_id = p_id
       AND s.code IN ('CARD2');
    /*Аресты*/
    SELECT COUNT(1)
      INTO i_lock_count
      FROM g_lock g
     WHERE g.acc_id = p_id
       AND g.dep_id = p_dep_id
       AND NVL(g.todate, p_maxdate) >= TRUNC(SYSDATE);
    /*Блокировки по счету*/
    BEGIN
      SELECT DECODE(nondebfl, 0, 0, 1) as nondebfl
        INTO i_nondebfl_cnt
        FROM g_accblnhst
       WHERE id = p_id
         AND dep_id = p_dep_id
         AND (prim NOT LIKE ('%Z_026_PKG_CMS_SLLOAN%') OR prim IS NULL)
         AND TRUNC(SYSDATE) BETWEEN fromdate AND todate;
    EXCEPTION
      WHEN no_data_found THEN
        i_nondebfl_cnt := 0;
    END;
    IF (i_incasso_count + i_crd2_cnt + i_lock_count + i_nondebfl_cnt) > 0 THEN
      RETURN(1);
    ELSE
      RETURN(0);
    END IF;
  END;

  function f_get_ps(ndep_id in t_dea.dep_id%type,
                    nid     in t_dea.id%type,
                    acc     in ledacc_std.code%type) return g_accbln.id%type is
    l_acc g_accbln.id%type;
  begin
    select l.code
      into l_acc
      from ledacc_det d, g_accbln g, ledacc_std l
     where d.pk1 = to_char(ndep_id)
       and d.pk2 = to_char(nid)
       and d.sgn_id = T_ASGN.fCode2Id('DEA')
       and d.dep_id = g.dep_id
       and d.acc_id = g.id
       and g.cha_id = l.id
       and substr(l.code, 1, 4) = acc
       and rownum = 1;
    return l_acc;
  exception
    when others then
      return(0);
      Raise_Application_Error(-20000,
                              Localize('Ошибка при получений пс счета',
                                       'Z_PKG_AUTO_TEST',
                                       'FGET_PS_LOAN'));
  end;

  function p_acc_client(p_cli_id  IN INTEGER,
                        p_cli_dep IN INTEGER,
                        val_code  in VARCHAR2) return varchar2 as
    pragma autonomous_transaction;
    acc_code    g_accbln.code%type;
    n_ord_Depid NUMBER;
    n_ord_id    NUMBER;

  begin
    FOR rec_ac IN (SELECT A.DEP_ID,
                          a.code,
                          b.pname1  lastName,
                          b.pname2  firstName,
                          b.RESIDFL
                     FROM g_cli a, g_clihst b
                    WHERE a.id = p_cli_id
                      AND a.dep_id = NVL(p_cli_dep, 2)
                      AND a.id = b.id
                      AND a.dep_id = b.dep_id
                      AND p_operday BETWEEN b.fromdate AND b.todate) loop

      n_ord_Depid := rec_ac.dep_id;
      n_ord_id    := null;
      BEGIN
        s_pkgdea.psavedea(iddep        => p_cli_dep,
                          iid          => n_ord_id,
                          d_ord        => p_operday,
                          ccode        => rec_ac.CODE,
                          idclidep     => p_cli_dep,
                          idcli        => p_cli_id,
                          dfrom        => p_operday,
                          idtrf        => 3689,
                          iddcl        => 2682,
                          iddepacc     => NULL,
                          idacc        => NULL,
                          ccmsfl       => '0',
                          nbal_dep_id  => 2,
                          nsrv_dep_id  => 2,
                          nsell_dep_id => 2);
        t_pkgdeaprm.pSetPrm(n_ord_id, p_cli_dep, 'Z_026_COM', '0');

        IF n_ord_id IS NOT NULL THEN
          BEGIN
            t_pkgrunoprutl.pRunOprByMainOrd(p_cli_dep,
                                            n_ord_id,
                                            'REG',
                                            'VAL_CODE =>''' ||
                                            upper(val_code) || '''');
          EXCEPTION
            WHEN OTHERS THEN
              ROLLBACK;
          END;
        END IF;
        COMMIT;

        IF T_PKGRUNOPRUTL.fStatCodeByOrd(p_cli_dep, n_ord_id) = 'REG' then
          select code
            into acc_code
            from s_deaacc s, g_accbln g
           where s.acc_id = g.id
             and s.acc_dep_id = g.dep_id
             and s.id = n_ord_id
             and s.dep_id = p_cli_dep;
        end if;
      END;
    END LOOP;
    return acc_code;
  END;

  function f_get_ps_account(ndep_id in t_dea.dep_id%type,
                            nid     in t_dea.id%type,
                            acc     in g_accbln.code%type)
    return g_accbln.code%type is
    l_acc g_accbln.code%type;
  begin
    select g.code
      into l_acc
      from ledacc_det d, g_accbln g, ledacc_std l
     where d.pk1 = to_char(ndep_id)
       and d.pk2 = to_char(nid)
       and d.sgn_id = T_ASGN.fCode2Id('DEA')
       and d.dep_id = g.dep_id
       and d.acc_id = g.id
       and g.cha_id = l.id
       and substr(l.code, 1, 4) = acc
       and rownum = 1;
    return l_acc;
  exception
    when others then
      return(0);
      Raise_Application_Error(-20000,
                              Localize('Ошибка при получений счета',
                                       'Z_PKG_AUTO_TEST',
                                       'f_get_ps_account'));
  end;

  function f_payment_attribute(ndep_id in t_dea.dep_id%type,
                               nid     in t_dea.id%type,
                               acc     in g_accbln.code%type,
                               vidsumm in T_ARLDSC.LONGNAME%type)
    return number as
    n_cnt number;
  begin
    select count(1)
      into n_cnt
      from T_ARLDEA o, T_ARLCLC c, T_ARLDSC a, T_DEAPAYATR p
     where o.CLC_ID = c.ID
       and c.ARL_ID = a.ID
       and p.DEP_ID(+) = o.DEP_ID
       and p.ID(+) = o.ORD_ID
       and p.NORD(+) = o.PAY_NORD
       and a.NOPAYFL = '0'
       and o.DEP_ID = ndep_id
       and o.ORD_ID = nid
       and a.LONGNAME = vidsumm
       and p.code_acc = acc
       and rownum = 1;
    if n_cnt > 0 then
      return 1;
    else
      return 0;
    end if;
  end;

  function f_cliacc_attribute(ndep_id in t_dea.dep_id%type,
                              nid     in t_dea.id%type,
                              acc     in g_accbln.code%type,
                              cliacc  in T_PAYATRTYP.code%type) return number as
    n_cnt number;
  begin
    select count(1)
      into n_cnt
      from T_DEAPAYATR a, T_PAYATRTYP p
     where a.TYP_ID = p.ID
       and a.DEP_ID = ndep_id
       and a.ID = nid
       and a.code_acc = acc
       and p.CODE = cliacc
       and rownum = 1;
    if n_cnt > 0 then
      return 1;
    else
      return 0;
    end if;
  end;



  procedure z_026_delete_tables(p_date IN OUT DATE, p_int INTEGER) is
    n_table   varchar2(50);
    n_dateset varchar2(50);

  begin
    for rec_del in (select table_name
                      from ALL_TABLES
                     where table_name like upper('%DATASET%')) loop

      n_table := rec_del.table_name;

      if n_table is not null then
        select v.code as n_dateset
          into n_dateset
          from c_vcs v
         where v.code = n_table;

        if n_dateset is not null then
          begin
            update c_vcs v
               set v.usr_id = null
             where v.code = n_table
               and v.USR_ID is not null;
            commit;
          end;

          BEGIN
            EXECUTE IMMEDIATE ('DROP TABLE ' || n_table);
          END;
        else
          BEGIN
            EXECUTE IMMEDIATE ('DROP TABLE ' || n_table);
          END;
        end if;
      end if;
    end loop;
  end;



PROCEDURE p_create_card_fl(n_inn_code in varchar2, n_num_phone in varchar2) IS

  pDEP_ID  number;
  pID  number;
  pORD_ID  number;
  pRes  number;
  pType  varchar2(2000);
BEGIN
  Z_077_PKG_CLIENT.pCheckCli(sIIN       => n_inn_code,
                           sPhoneNumber => n_num_phone,
                           sCliJURCode  => null,
                           sEmpPosition => null,
                           sProject     => 01,
                           nDEP_ID      => pDEP_ID,
                           nID          => pID,
                           nORD_ID      => pORD_ID,
                           nRes         => pRes,
                           sType        => pType);
     dbms_output.put_line('1out -> ' || pDEP_ID || '; 2out-> ' || pID || '; 3out-> ' || pORD_ID || '; 4out-> ' || pRes || '; 5out-> ' || pType);
   commit;
END;

PROCEDURE p_create_card_jur(n_inn_code in varchar2) IS
  pDEP_ID  number;
  pID  number;
  pORD_ID  number;
  pRes  number;
  pType  varchar2(2000);
BEGIN
Z_077_PKG_CLIENT.pCheckCliYul(sBin     => n_inn_code,
                              sIin     => n_inn_code,
                              sProject => 01 ,
                              nDEP_ID  => pDEP_ID ,
                              nID      => pID,
                              nORD_ID  => pORD_ID,
                              nRes     => pRes,
                              sType    => pType);
    dbms_output.put_line('1out -> ' || pDEP_ID || '; 2out-> ' || pID || '; 3out-> ' || pORD_ID || '; 4out-> ' || pRes || '; 5out-> ' || pType);
  commit;
END;

PROCEDURE p_create_card_pboul(n_inn_code in varchar2, n_num_phone in varchar2) IS
  pDEP_ID  number;
  pID  number;
  pORD_ID  number;
  pRes  number;
  pType  varchar2(2000);
BEGIN
Z_077_PKG_CLIENT.pCheckCliIp(sIIN      => n_inn_code,
                           sPhoneNumber => n_num_phone,
                           sProject      => 01,
                           nDEP_ID      => pDEP_ID,
                           nID          => pID,
                           nORD_ID      => pORD_ID,
                           nRes         => pRes,
                           sType        => pType);
     dbms_output.put_line('1out -> ' || pDEP_ID || '; 2out-> ' || pID || '; 3out-> ' || pORD_ID || '; 4out-> ' || pRes || '; 5out-> ' || pType);
   commit;
END;

PROCEDURE p_crate_doc_ts(n_acc_code in varchar2, sAMOUNT in varchar2, trn_acc in varchar2, sKSO_CODE in varchar2, sCHA_CODE in varchar2, sKNP in varchar2, sCODE_OD in varchar2, sCODE in varchar2) IS

bImport      boolean;
s_errtxt     varchar2(2000);
nDepId       T_ORD.DEP_ID%TYPE;
nOrdId       T_ORD.ID%TYPE;
sIdnDt       G_CLIHST.TAXCODE%type;
sNamePay     G_CLIHST.LONGNAME%type;
sRuk         varchar2(250);
sBuch        varchar2(250);
sResidDt     varchar2(1);
sSectDt      varchar2(1);
sIdnCt       G_CLIHST.TAXCODE%type;
sNameBen     G_CLIHST.LONGNAME%type;
sResidCt     varchar2(1);
sSectCt      varchar2(1);
dValDate     date;
sTxtDscr     varchar2(500);
RESOURCE_BUSY exception;
pragma exception_init (RESOURCE_BUSY,-54);
cUsrPos      VARCHAR2(50);
nTusId       C_USR.ID%TYPE;
s_response   VARCHAR2(2000);
sdOper       varchar2(20);
dOper        date;
val_code     varchar2(20) := null;

BEGIN
  c_pkgconnect.popen();
  begin
    select ID, CODE INTO nTusId, cUsrPos from C_USR c where c.code = 'COLVIR';
  exception
  when others then
    cUsrPos := NULL;
  end;
            dbms_output.put_line('1 ');

  COLVIR.c_pkgconnect.popen(cUsrPos=>cUsrPos);
  COLVIR.c_pkgconnect.pOpenLink(cUserName => cUsrPos, iBss => 1, WNDS_FL => 1);
  dValDate := p_operday ; --to_date('15112022','ddmmyyyy') ;
  C_PkgSession.dOper := dValDate; -- Поставить необходимую дату опердня!!!

  BEGIN
     SELECT ga.DEP_ID,
            gc.TAXCODE,
            gc.LONGNAME,
            G_PKGCLI.FRUK(gc.dep_id, gc.id) ruk,
            nvl(G_PKGCLI.FBUCH(gc.dep_id, gc.id), '-') buch,
            decode(G_PKGCLI.FGETCLIRESIDFL(gc.dep_id, gc.id), 0, 2, G_PKGCLI.FGETCLIRESIDFL(gc.dep_id, gc.id)) resid,
            G_PKGCLI.FGETCLISECT_ID(gc.dep_id, gc.id) sect
       INTO nDepId,
            sIdnDt,
            sNamePay,
            sRuk,
            sBuch,
            sResidDt,
            sSectDt
     FROM G_ACCBLN ga,
          G_ACCBLNHST gh,
          G_CLIHST gc
    WHERE ga.DEP_ID=gh.DEP_ID AND ga.ID=gh.ID and dValDate BETWEEN gh.FROMDATE and gh.TODATE and gh.arcfl = 0
      AND gh.CLIDEP_ID=gc.DEP_ID and gh.CLI_ID=gc.ID AND dValDate BETWEEN gc.FROMDATE and gc.TODATE and gc.arcfl = 0
      AND ga.CODE = trn_acc;
    EXCEPTION WHEN NO_DATA_FOUND THEN
      dbms_output.put_line('err1 ');
      dbms_output.put_line('err = '||sqlerrm||' '||DBMS_UTILITY.FORMAT_ERROR_BACKTRACE);
  END;


      begin
         SELECT gc.TAXCODE,
                gc.LONGNAME,
                decode(G_PKGCLI.FGETCLIRESIDFL(gc.dep_id, gc.id), 0, 2, G_PKGCLI.FGETCLIRESIDFL(gc.dep_id, gc.id)) resid,
                G_PKGCLI.FGETCLISECT_ID(gc.dep_id, gc.id) sect
           INTO sIdnCt,
                sNameBen,
                sResidCt,
                sSectCt
         FROM G_ACCBLN ga,G_ACCBLNHST gh,G_CLIHST gc
        WHERE ga.DEP_ID=gh.DEP_ID AND ga.ID=gh.ID and dValDate BETWEEN gh.FROMDATE and gh.TODATE and gh.arcfl = 0
          AND gh.CLIDEP_ID=gc.DEP_ID and gh.CLI_ID=gc.ID AND dValDate BETWEEN gc.FROMDATE and gc.TODATE and gc.arcfl = 0
          AND ga.CODE = n_acc_code;
        exception when no_data_found then
          null;
          dbms_output.put_line('err2 ');
          dbms_output.put_line('err = '||sqlerrm||' '||DBMS_UTILITY.FORMAT_ERROR_BACKTRACE);
      end;

        begin
          S_BsPay.pSave(
              nDEP_ID     => nDepId,
              nID         => nOrdId,
              sKSO_CODE   => sKSO_CODE,
              sCHA_CODE   => sCHA_CODE,
              sAMOUNT     => sAMOUNT,
              sVAL_CODE   => val_code,
              dDORD       => dValDate,
              dDVAL       => dValDate,
              sCODE_ACL   => trn_acc,
              sCODE_BCR   => 'KCJBKZKX',
              sCODE_ACR   => n_acc_code,
              sRNN_CR     => sIdnCt,
              sCODE_BC    => null,
              sTXT_HEAD   => null,
              sTXT_BUCH   => sBuch,
              sTXT_DSCR   => 'автотестирование',
              sTXT_BEN    =>  null,
              nNOCMSFL    => 0,
              sKNP        => sKNP,
              sCODE_OD    => sCODE_OD,
              sCODE_BE    => sResidCt||sSectCt,
              sPRIM       => 'Примечание',
              sCODE       => sCODE,--to_char(sysdate,'YYMMDDHHMISS'),
              nFLZO       => 0,
              iVrfFl      => null,
              sLIMFL      => null,
              iPrintFl    => null,
              sSPEEDFL    => 0,
             sSOST       => null,
              sREFER      => null,
              p_parentProc=> null,
              p_parentOpr => null,
              sRnnCli     => sIdnDt,
              sTxtPay     => sNamePay,
              sVOper      =>'01',
              SGCVP       => null,
              nDEPUSR_ID  => nDepId);
          update S_ORDPAY set ALTERCODE = 'AUTOTEST' where ID = nOrdId and DEP_ID = nDepId;
          commit;
        exception
          when others then
            rollback;
            s_errtxt := substr('pSave: '||SQLERRM||' '||DBMS_UTILITY.FORMAT_ERROR_BACKTRACE, 1, 400);
            dbms_output.put_line('s_errtxt '||s_errtxt);
            commit;
        end;
            dbms_output.put_line('5 ');

end;

PROCEDURE p_create_acc(nDepCode in varchar2, nValCode in varchar2, nCliCode in varchar2, sDclCode in varchar2) IS
  pProject  number;
  pStatus  number;
  pErrMesg  varchar2(2000);
  pErrCode varchar2(2000);
  pCODE  varchar2(2000);
  pDepAcc  varchar2(2000);
BEGIN
Z077_PKG_STARDEPO.pDeaRkoOpen(sDEP_CODE => nDepCode,
                               sVAL_CODE => nValCode,
                               sCLI_CODE => nCliCode,
                               sCMS_ACC  => null,
                               sPROJECT  => pProject,
                               nSTATUS   => pStatus,
                               sErrMesg  => pErrMesg,
                               nErrCode  => pErrCode,
                               sCODE     => pCODE,
                               sDepAcc   => pDepAcc,
                               sDclCode  => sDclCode);
     dbms_output.put_line('1out -> ' || pProject || '; 2out-> ' || pStatus || '; 3out-> ' || pErrMesg || '; 4out-> ' || pErrCode || '; 5out->' || pCODE || '; 6out->' || pDepAcc);
   commit;
END;

PROCEDURE p_create_acc_java(nDepCode in varchar2, nValCode in varchar2, nCliCode in varchar2, sDclCode in varchar2, AccCode out varchar2) IS
  /*Процедура предназначена для использования в языке Java*/
  pProject  number;
  pStatus  number;
  pErrMesg  varchar2(2000);
  pErrCode varchar2(2000);
  pCODE  varchar2(2000);
  pDepAcc  varchar2(2000);
BEGIN
Z077_PKG_STARDEPO.pDeaRkoOpen(sDEP_CODE => nDepCode,
                               sVAL_CODE => nValCode,
                               sCLI_CODE => nCliCode,
                               sCMS_ACC  => null,
                               sPROJECT  => pProject,
                               nSTATUS   => pStatus,
                               sErrMesg  => pErrMesg,
                               nErrCode  => pErrCode,
                               sCODE     => pCODE,
                               sDepAcc   => pDepAcc,
                               sDclCode  => sDclCode);
     dbms_output.put_line('1out -> ' || pProject || '; 2out-> ' || pStatus || '; 3out-> ' || pErrMesg || '; 4out-> ' || pErrCode || '; 5out->' || pCODE || '; 6out->' || pDepAcc);
   commit;
   AccCode := pDepAcc;
END;

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
                        dbFl varchar2 default '0')
  /* Процедура создает документ в досье клиента*/
  /* cliCode - код клиента
     docType - тип документа (id типа документа)
     docGrp - id группы документов
     longname - наименование документа
     docDate - дата документа
     docNum - номер документа
     copyFl - признак копии документа '0'/'1'
     docPycCnt - кол-во эеземпляром
     expDate - дата окончания документа
     docPubl - кем выдан
     docDsc - описание
     notifyFl - признак уведомления '0'/'1'
     storage - место хранения (можно передавать null)
     eCopy - признак создания электронной копии (если передать '1', то создасться электронная копия
             если '0', то электронная копия не создасться
     fullPath - путь до файла. По дефолту null
     fileExt - расширение файла. По дефолту null
     dbFl - место хранения. По дефолту '0' */
     IS
     pNORD number;
     pNVER number;
     pREAL_NORD number;
     DEP_CLI_ID number(10);
     CLI_ID number(10);
     nRES number := 0;
     sType varchar2 (2000);
     cErrMsg varchar2 (2000);
     DOS_ID number(10);
     idEDoc number;
     pID number;
     DEP_ID_CLIENT number(10);
     ID_CLIENT number(10);
     BEGIN
         BEGIN
             /*получаем по коду карточки клиента его id и dep_id**/
             SELECT DISTINCT DEP_ID, ID
             INTO DEP_CLI_ID, CLI_ID
             FROM G_CLI
             WHERE CODE = cliCode;
         EXCEPTION
             WHEN NO_DATA_FOUND THEN
                 nRES := 1;
                 sType := 'В БД отсутствует карточка клиента по коду - '|| cliCode;
                 dbms_output.put_line('1out -> ' || nRES || '; 2out-> ' || sType);
                 RETURN;
             WHEN OTHERS THEN
                 nRES := 1;
                 sType := 'При получении ID карточки клиента возникла ошибка по карточке клиента - '|| cliCode;
                 dbms_output.put_line('1out -> ' || nRES || '; 2out-> ' || sType);
                 RETURN;
         END;
         /* получаем по коду клиента id досье клиента */
         BEGIN
             DEP_ID_CLIENT := DEP_CLI_ID;
             ID_CLIENT := CLI_ID;
             SELECT t.ID
             INTO DOS_ID
             FROM T_DOCDOS t
             LEFT JOIN T_DOCDOCDSC c ON t.DDD_ID = c.ID
             WHERE t.CLI_ID = ID_CLIENT and t.CLI_DEP_ID = DEP_ID_CLIENT and c.CLIFL = '1';
         EXCEPTION
             WHEN NO_DATA_FOUND THEN
                 nRES := 1;
                 sType := 'В БД отсутствует досье клиента по коду - '|| cliCode;
                 dbms_output.put_line('1out -> ' || nRES || '; 2out-> ' || sType);
                 RETURN;
             /*WHEN OTHERS THEN
                 nRES := 1;
                 sType := 'При получении ID досье возникла ошибка по карточке клиента - '|| cliCode;
                 dbms_output.put_line('1out -> ' || nRES || '; 2out-> ' || sType);
                 RETURN;*/
         END;
         EXECUTE IMMEDIATE 'ALTER TRIGGER T_DOCLST_DTU DISABLE';
         EXECUTE IMMEDIATE 'ALTER TRIGGER T_DOCCLIVER_DTU DISABLE';
         /* создаем необходимый документ в досье клиента */
         BEGIN
             T_PKGDOCDOS.pNewDocLst(
                                    pID             => DOS_ID         -- Досье
                                  , pNORD           => pNORD      -- Порядковый номер
                                  , pNVER           => pNVER   -- Номер версии
                                  , pDOCTYPE        => docType         -- Тип документа
                                  , pDRF_ID         => docGrp    -- группа документов
                                  , pLONGNAME       => longname   -- Наименование
                                  , pDOCDATE        => docDate -- Дата документа
                                  , pDOCNUM         => docNum  -- Номер документа
                                  , pCOPYFL         => copyFl  -- Признак копии
                                  , pDOCCPYCNT      => docPycCnt -- Кол-во экземпляров
                                  , pEXPDATE        => expDate -- Дата окончания
                                  , pDOCPUBL        => docPubl -- Кем выдан
                                  , pDOCDSC         => docDsc -- Описание
                                  , pNOTIFYFL       => notifyFl  -- Признак уведомления
                                  , pSTORAGE        => storage    -- Место хранения
                                  , pREAL_NORD      => pREAL_NORD      -- Порядковый номер реальный. не из клиентского досье
                                    );
              COMMIT;
          END;
          /* если в eCopy передан '1', то создаем электронную копию документа */
          BEGIN
              Z_PKG_AUTO_TEST.pCreEDocDosCli(fullPath => fullPath,
                                             fileExt => fileExt,
                                             dbFl => dbFl,
                                             idEDoc => idEDoc);
              /* апдейтим id созданного электронного документа и добавляем его в документ для связки */
              UPDATE T_DOCCLIVER r
              SET DEC_ID = idEDoc
              WHERE r.ID = DOS_ID and r.NORD = pNORD and r.NVER = pNVER;
              COMMIT;
          EXCEPTION WHEN OTHERS THEN
              nRES := 1;
              sType := 'Возникла ошибка при апдейте документа в карточке клиента - '|| cliCode;
              dbms_output.put_line('1out -> ' || nRES || '; 2out-> ' || sType);
              ROLLBACK;
              RETURN;
          END;
          EXECUTE IMMEDIATE 'ALTER TRIGGER T_DOCLST_DTU ENABLE';
          EXECUTE IMMEDIATE 'ALTER TRIGGER T_DOCCLIVER_DTU ENABLE';
          IF nRES = 0 THEN
              sType := 'Создан документ с порядковым номером - '|| pNORD || '. В досье - ' || DOS_ID;
              dbms_output.put_line('1out -> ' || nRES || '; 2out-> ' || sType || '; 3out-> ' || pNORD);
          END IF;
    END;

PROCEDURE pCreEDocDosCli(fullPath in varchar2, fileExt varchar2, dbFl varchar2, idEDoc out number)
  IS
  E_ID number;
  nRES number := 0;
  sType varchar2 (2000);
  cErrMsg varchar2 (2000);
  BEGIN
      EXECUTE IMMEDIATE 'ALTER TRIGGER T_DOCECPY_DTU DISABLE';
      BEGIN
          INSERT INTO T_DOCECPY(FULLPATH, DOCBLB, FILEEXT, ID_US, CORRECTDT, DBFL)
                        values(fullPath, EMPTY_BLOB(), fileExt, '1', null, dbFl)
                        RETURNING ID INTO E_ID;
          COMMIT;
      EXCEPTION WHEN NO_DATA_FOUND THEN
          nRES := 1;
          sType := 'Электронный документ не создался. В ID вернулся null';
          dbms_output.put_line('1out -> ' || nRES || '; 2out-> ' || sType);
          RETURN;
      END;
      EXECUTE IMMEDIATE 'ALTER TRIGGER T_DOCECPY_DTU ENABLE';
      IF nRES = 0 THEN
          sType := 'Создан электронный документ с ID - ' || E_ID;
          /*dbms_output.put_line('1out -> ' || nRES || '; 2out-> ' || sType);*/
      END IF;
      idEDoc := E_ID;
  END;

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
                        nord out varchar2)
  /* Процедура создает документ в досье клиента*/
  /* cliCode - код клиента
     docType - тип документа (id типа документа)
     docGrp - id группы документов
     longname - наименование документа
     docDate - дата документа
     docNum - номер документа
     copyFl - признак копии документа '0'/'1'
     docPycCnt - кол-во эеземпляром
     expDate - дата окончания документа
     docPubl - кем выдан
     docDsc - описание
     notifyFl - признак уведомления '0'/'1'
     storage - место хранения (можно передавать null)
     eCopy - признак создания электронной копии (если передать '1', то создасться электронная копия
             если '0', то электронная копия не создасться
     fullPath - путь до файла. По дефолту null
     fileExt - расширение файла. По дефолту null
     dbFl - место хранения. По дефолту '0' */
     IS
     pNORD number;
     pNVER number;
     pREAL_NORD number;
     DEP_CLI_ID number(10);
     CLI_ID number(10);
     nRES number := 0;
     sType varchar2 (2000);
     cErrMsg varchar2 (2000);
     DOS_ID number(10);
     idEDoc number;
     pID number;
     DEP_ID_CLIENT number(10);
     ID_CLIENT number(10);
     BEGIN
         BEGIN
             /*получаем по коду карточки клиента его id и dep_id**/
             SELECT DISTINCT DEP_ID, ID
             INTO DEP_CLI_ID, CLI_ID
             FROM G_CLI
             WHERE CODE = cliCode;
         EXCEPTION
             WHEN NO_DATA_FOUND THEN
                 nRES := 1;
                 sType := 'В БД отсутствует карточка клиента по коду - '|| cliCode;
                 dbms_output.put_line('1out -> ' || nRES || '; 2out-> ' || sType);
                 RETURN;
             WHEN OTHERS THEN
                 nRES := 1;
                 sType := 'При получении ID карточки клиента возникла ошибка по карточке клиента - '|| cliCode;
                 dbms_output.put_line('1out -> ' || nRES || '; 2out-> ' || sType);
                 RETURN;
         END;
         /* получаем по коду клиента id досье клиента */
         BEGIN
             DEP_ID_CLIENT := DEP_CLI_ID;
             ID_CLIENT := CLI_ID;
             SELECT t.ID
             INTO DOS_ID
             FROM T_DOCDOS t
             LEFT JOIN T_DOCDOCDSC c ON t.DDD_ID = c.ID
             WHERE t.CLI_ID = ID_CLIENT and t.CLI_DEP_ID = DEP_ID_CLIENT and c.CLIFL = '1';
         EXCEPTION
             WHEN NO_DATA_FOUND THEN
                 nRES := 1;
                 sType := 'В БД отсутствует досье клиента по коду - '|| cliCode;
                 dbms_output.put_line('1out -> ' || nRES || '; 2out-> ' || sType);
                 RETURN;
             /*WHEN OTHERS THEN
                 nRES := 1;
                 sType := 'При получении ID досье возникла ошибка по карточке клиента - '|| cliCode;
                 dbms_output.put_line('1out -> ' || nRES || '; 2out-> ' || sType);
                 RETURN;*/
         END;
         EXECUTE IMMEDIATE 'ALTER TRIGGER T_DOCLST_DTU DISABLE';
         EXECUTE IMMEDIATE 'ALTER TRIGGER T_DOCCLIVER_DTU DISABLE';
         /* создаем необходимый документ в досье клиента */
         BEGIN
             T_PKGDOCDOS.pNewDocLst(
                                    pID             => DOS_ID         -- Досье
                                  , pNORD           => pNORD      -- Порядковый номер
                                  , pNVER           => pNVER   -- Номер версии
                                  , pDOCTYPE        => docType         -- Тип документа
                                  , pDRF_ID         => docGrp    -- группа документов
                                  , pLONGNAME       => longname   -- Наименование
                                  , pDOCDATE        => docDate -- Дата документа
                                  , pDOCNUM         => docNum  -- Номер документа
                                  , pCOPYFL         => copyFl  -- Признак копии
                                  , pDOCCPYCNT      => docPycCnt -- Кол-во экземпляров
                                  , pEXPDATE        => expDate -- Дата окончания
                                  , pDOCPUBL        => docPubl -- Кем выдан
                                  , pDOCDSC         => docDsc -- Описание
                                  , pNOTIFYFL       => notifyFl  -- Признак уведомления
                                  , pSTORAGE        => storage    -- Место хранения
                                  , pREAL_NORD      => pREAL_NORD      -- Порядковый номер реальный. не из клиентского досье
                                    );
              COMMIT;
          END;
          /* если в eCopy передан '1', то создаем электронную копию документа */
          BEGIN
              Z_PKG_AUTO_TEST.pCreEDocDosCli(fullPath => fullPath,
                                             fileExt => fileExt,
                                             dbFl => dbFl,
                                             idEDoc => idEDoc);
              /* апдейтим id созданного электронного документа и добавляем его в документ для связки */
              UPDATE T_DOCCLIVER r
              SET DEC_ID = idEDoc
              WHERE r.ID = DOS_ID and r.NORD = pNORD and r.NVER = pNVER;
              COMMIT;
          EXCEPTION WHEN OTHERS THEN
              nRES := 1;
              sType := 'Возникла ошибка при апдейте документа в карточке клиента - '|| cliCode;
              dbms_output.put_line('1out -> ' || nRES || '; 2out-> ' || sType);
              ROLLBACK;
              RETURN;
          END;
          EXECUTE IMMEDIATE 'ALTER TRIGGER T_DOCLST_DTU ENABLE';
          EXECUTE IMMEDIATE 'ALTER TRIGGER T_DOCCLIVER_DTU ENABLE';
          IF nRES = 0 THEN
              nord := pNORD;
              sType := 'Создан документ с порядковым номером - '|| pNORD || '. В досье - ' || DOS_ID;
              dbms_output.put_line('1out -> ' || nRES || '; 2out-> ' || sType || '; 2out-> ' || pNORD);
          END IF;
    END;

  PROCEDURE AT_pSetBlockAcc(accCode in varchar2, lLockType integer, lockName varchar2, sReason varchar2, dBegin varchar2, sAmount varchar2, sValCode varchar2)
    IS
    DEP_ACC number(10);
    ID_ACC number(10);
    DEP_ID_ACC number(10);
    ACC_ID number(10);
    nRES number(10) := 0;
    sType varchar2(2000);
    ORD number(10);
    CVALUE number(10);
    BEGIN
        /* по номеру счета определяем его dep_id и id */
        BEGIN
            SELECT DEP_ID, ID, ORD_ID
            INTO DEP_ACC, ID_ACC, ORD
            FROM G_ACCBLN
            WHERE CODE = accCode;
        EXCEPTION
            WHEN NO_DATA_FOUND THEN
                nRES := 1;
                sType := 'В БД отсутствует счет с номером - '|| accCode;
                dbms_output.put_line('1out -> ' || nRES || '; 2out-> ' || sType);
                RETURN;
        END;
        /* вызываем процедуру для приостановки счета */
        BEGIN
            DEP_ID_ACC := DEP_ACC;
            ACC_ID := ID_ACC;
            c_pkgconnect.popen(); c_pkgsession.doper := P_OPERDAY;
            G_BLNACC.PSETPAUSEACC(IDDEP => DEP_ID_ACC,
                                  IDACC => ACC_ID,
                                  ILOCKTYPE => lLockType,
                                  ILOCKNAME => lockName,
                                  SREASON => sReason,
                                  DBEGIN => dBegin,
                                  SLORDNUM => NULL,
                                  SLORGCODE => NULL,
                                  SLORGNAME => NULL,
                                  DLORDDATE => NULL,
                                  SREFER => NULL,
                                  DEND => NULL,
                                  SAMOUNT => sAmount,
                                  SVALCODE => sValCode);
            COMMIT;
        END;
        /* производим поиск id только что созданной блокировки для возврата из процедуры */
        BEGIN
            SELECT D.CVALUE INTO CVALUE FROM T_OPERJRN J,T_OPERDET D,T_SCEN_STD S
            WHERE J.ORD_ID=ORD AND J.DEP_ID=DEP_ACC AND J.UNDOFL='0'
                  AND D.ID=J.ID AND D.NJRN=J.NJRN AND D.CODE='LOCK_ID'
                  AND S.ID=J.BOP_ID AND S.NORD=J.NOPER AND S.CODE='PAUSEACC'
                  AND J.NJRN = (SELECT MAX(J2.NJRN) FROM T_OPERJRN J2, T_OPERDET D2, T_SCEN_STD S2 WHERE J2.ORD_ID=ORD AND J2.DEP_ID=DEP_ACC AND J2.UNDOFL='0'
                                       AND D2.ID=J2.ID AND D2.NJRN=J2.NJRN AND D2.CODE='LOCK_ID'
                                       AND S2.ID=J2.BOP_ID AND S2.NORD=J2.NOPER AND S2.CODE='PAUSEACC');
        EXCEPTION
            WHEN NO_DATA_FOUND THEN
                  nRES := 1;
                  sType := 'В БД отсутствует запись с id блокировки, по счету - '|| accCode;
                  dbms_output.put_line('1out -> ' || nRES || '; 2out-> ' || sType);
                  RETURN;
        END;
        IF nRES = 0 THEN
            sType := 'Установлена блокировка на счет с номером - ' || accCode;
            dbms_output.put_line('1out -> ' || nRES || '; 2out-> ' || sType || '; 2out-> ' || CVALUE);
        END IF;
  END;

  PROCEDURE AT_pSetBlockAccJava(accCode in varchar2, lLockType number, lockName varchar2, sReason varchar2, dBegin varchar2, sAmount varchar2, sValCode varchar2, lockId out number)
    IS
    DEP_ACC number(10);
    ID_ACC number(10);
    DEP_ID_ACC number(10);
    ACC_ID number(10);
    nRES number(10) := 0;
    sType varchar2(2000);
    ORD number(10);
    CVALUE number(10);
    BEGIN
        /* по номеру счета определяем его dep_id и id */
        BEGIN
            SELECT DEP_ID, ID, ORD_ID
            INTO DEP_ACC, ID_ACC, ORD
            FROM G_ACCBLN
            WHERE CODE = accCode;
        EXCEPTION
            WHEN NO_DATA_FOUND THEN
                nRES := 1;
                sType := 'В БД отсутствует счет с номером - '|| accCode;
                dbms_output.put_line('1out -> ' || nRES || '; 2out-> ' || sType);
                RETURN;
        END;
        /* вызываем процедуру для приостановки счета */
        BEGIN
            DEP_ID_ACC := DEP_ACC;
            ACC_ID := ID_ACC;
            c_pkgconnect.popen(); c_pkgsession.doper := P_OPERDAY;
            G_BLNACC.PSETPAUSEACC(IDDEP => DEP_ID_ACC,
                                  IDACC => ACC_ID,
                                  ILOCKTYPE => lLockType,
                                  ILOCKNAME => lockName,
                                  SREASON => sReason,
                                  DBEGIN => dBegin,
                                  SLORDNUM => NULL,
                                  SLORGCODE => NULL,
                                  SLORGNAME => NULL,
                                  DLORDDATE => NULL,
                                  SREFER => NULL,
                                  DEND => NULL,
                                  SAMOUNT => sAmount,
                                  SVALCODE => sValCode);
            COMMIT;
        END;
        /* производим поиск id только что созданной блокировки для возврата из процедуры */
        BEGIN
            SELECT D.CVALUE INTO CVALUE FROM T_OPERJRN J,T_OPERDET D,T_SCEN_STD S
            WHERE J.ORD_ID=ORD AND J.DEP_ID=DEP_ACC AND J.UNDOFL='0'
                  AND D.ID=J.ID AND D.NJRN=J.NJRN AND D.CODE='LOCK_ID'
                  AND S.ID=J.BOP_ID AND S.NORD=J.NOPER AND S.CODE='PAUSEACC'
                  AND J.NJRN = (SELECT MAX(J2.NJRN) FROM T_OPERJRN J2, T_OPERDET D2, T_SCEN_STD S2 WHERE J2.ORD_ID=ORD AND J2.DEP_ID=DEP_ACC AND J2.UNDOFL='0'
                                       AND D2.ID=J2.ID AND D2.NJRN=J2.NJRN AND D2.CODE='LOCK_ID'
                                       AND S2.ID=J2.BOP_ID AND S2.NORD=J2.NOPER AND S2.CODE='PAUSEACC');
        EXCEPTION
            WHEN NO_DATA_FOUND THEN
                  nRES := 1;
                  sType := 'В БД отсутствует запись с id блокировки, по счету - '|| accCode;
                  dbms_output.put_line('1out -> ' || nRES || '; 2out-> ' || sType);
                  RETURN;
        END;
        IF nRES = 0 THEN
            sType := 'Установлена блокировка на счет с номером - ' || accCode;
            dbms_output.put_line('1out -> ' || nRES || '; 2out-> ' || sType || '; 2out-> ' || CVALUE);
        END IF;
        lockId := CVALUE;
  END;

  PROCEDURE AT_pUndoSetBlockAcc(accCode varchar2, lLockId number, lockName varchar2, sReason varchar2)
    IS
    DEP_ACC number(10);
    ID_ACC number(10);
    DEP_ID_ACC number(10);
    ACC_ID number(10);
    nRES number(10) := 0;
    sType varchar2(2000);
    BEGIN
        /* по номеру счета определяем его dep_id и id */
        BEGIN
            SELECT DEP_ID, ID
            INTO DEP_ACC, ID_ACC
            FROM G_ACCBLN
            WHERE CODE = accCode;
        EXCEPTION
            WHEN NO_DATA_FOUND THEN
                nRES := 1;
                sType := 'В БД отсутствует счет с номером - '|| accCode;
                dbms_output.put_line('1out -> ' || nRES || '; 2out-> ' || sType);
                RETURN;
            WHEN OTHERS THEN
                nRES := 1;
                sType := 'Возникла ошибка при поиске id счета, по счету - '|| accCode;
                dbms_output.put_line('1out -> ' || nRES || '; 2out-> ' || sType);
                RETURN;
        END;
        /* вызываем процедуру для приостановки счета */
        BEGIN
            DEP_ID_ACC := DEP_ACC;
            ACC_ID := ID_ACC;
            c_pkgconnect.popen(); c_pkgsession.doper := P_OPERDAY;
            G_BLNACC.PENDPAUSEACC(IDDEP => DEP_ID_ACC,
                                  IDACC => ACC_ID,
                                  ILOCKID => lLockId,
                                  ILOCKNAME => lockName,
                                  SREASON => sReason);
            COMMIT;
        END;
        IF nRES = 0 THEN
            sType := 'Снята блокировка со счета с номером - ' || accCode;
            dbms_output.put_line('1out -> ' || nRES || '; 2out-> ' || sType);
        END IF;
    END;

  PROCEDURE AT_pUndoSetBlockAccJava(accCode varchar2, lLockId number, lockName varchar2, sReason varchar2, status out varchar2)
    IS
    DEP_ACC number(10);
    ID_ACC number(10);
    DEP_ID_ACC number(10);
    ACC_ID number(10);
    ORD number(10);
    nRES number(10) := 0;
    sType varchar2(2000);
    check_undo varchar2(2000);
    CVALUE_UNDO date;
    BEGIN
        /* по номеру счета определяем его dep_id и id */
        BEGIN
            SELECT DEP_ID, ID, ORD_ID
            INTO DEP_ACC, ID_ACC, ORD
            FROM G_ACCBLN
            WHERE CODE = accCode;
        EXCEPTION
            WHEN NO_DATA_FOUND THEN
                nRES := 1;
                sType := 'В БД отсутствует счет с номером - '|| accCode;
                dbms_output.put_line('1out -> ' || nRES || '; 2out-> ' || sType);
                RETURN;
            WHEN OTHERS THEN
                nRES := 1;
                sType := 'Возникла ошибка при поиске id счета, по счету - '|| accCode;
                dbms_output.put_line('1out -> ' || nRES || '; 2out-> ' || sType);
                RETURN;
        END;
        /* вызываем процедуру для приостановки счета */
        BEGIN
            DEP_ID_ACC := DEP_ACC;
            ACC_ID := ID_ACC;
            c_pkgconnect.popen(); c_pkgsession.doper := P_OPERDAY;
            G_BLNACC.PENDPAUSEACC(IDDEP => DEP_ID_ACC,
                                  IDACC => ACC_ID,
                                  ILOCKID => lLockId,
                                  ILOCKNAME => lockName,
                                  SREASON => sReason);
            COMMIT;
        END;
        IF nRES = 0 THEN
            sType := 'Снята блокировка со счета с номером - ' || accCode;
            dbms_output.put_line('1out -> ' || nRES || '; 2out-> ' || sType);
        END IF;
        BEGIN
            SELECT l.TODATE INTO CVALUE_UNDO FROM G_LOCK l
            WHERE ID = lLockId;
            check_undo := 'OK';
        EXCEPTION
            WHEN NO_DATA_FOUND THEN
                check_undo := 'ERROR';

        END;
        status := check_undo;

    END;

  PROCEDURE AddTakeOffAdmPrm(USER_LOGIN IN VARCHAR2, TYPE_ACT NUMBER)
  IS

  BEGIN
       IF TYPE_ACT = 1 THEN
           Update C_USR cr
           set cr.DBAFL = 1
           where cr.DBAFL = 0 and cr.CODE LIKE USER_LOGIN || '%';
           COMMIT;

       ELSIF TYPE_ACT = 0 THEN
           Update C_USR cr
           set cr.DBAFL = 0
           where cr.DBAFL = 1 and cr.CODE LIKE USER_LOGIN || '%';
           COMMIT;
       END IF;
  END;
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
                             ARESTFL_USER in char default '0')

IS
ID_DEP number;
status_code number;
msg_error varchar2 (2000);
ID_USER number;
id number (10);
p_ID number;
login_pos varchar2 (2000);
ID_POS number;
ID_STF number;
STF_ID number;
msg_result varchar2 (2000);
status varchar2 (2000);

BEGIN
          -- создаем логин позиции по образу: LOGIN-DEP-Рандомное число
          login_pos := LOGIN || '-' || DEP || '-' || TO_CHAR(round(dbms_random.value(10000, 99999)));

          -- Определяем id департамента
          BEGIN
             SELECT ID
             INTO ID_DEP
             FROM C_DEP_STD
             where CODE = DEP;

          EXCEPTION
            when NO_DATA_FOUND then
             rollback;
             status_code := 0;
             msg_error := 'Переданный департамент ' || DEP || ' не найден в справочнике департаментов';
             dbms_output.put_line('1out -> ' || status_code || '; 2out-> ' || msg_error || '; 3out-> ' || LOGIN);
             raise_application_error (-20000, msg_error);
             RETURN;
          END;


          BEGIN
            -- создаем пользователя. Задача USER в Колвире
            c_pkgconnect.popen(); c_pkgsession.doper := trunc(SYSDATE);
            c_pkguser.pSave(p_ID => id,
                            p_CODE => LOGIN,
                            p_USER_NAME => LOGIN,
                            p_FIO => FIO,
                            p_ARCFL => ARCFL_USER,
                            p_ARESTFL => ARESTFL_USER,
                            p_EMAIL => EMAIL,
                            p_PHONE => PHONE,
                            p_VIRTUALFL => VIRTUALFL,
                            p_FROMDATE => FROMDATE,
                            p_TODATE => TODATE,
                            p_WEB_TYPE => WEB_TYPE,
                            p_PRIM => PRIM,
                            p_Password => PASSWORD,
                            p_SYNCFL => '0',
                            p_INDATA => null);

           -- получаем id созданной записи по переденнамо логину пользователя
           SELECT ID
           INTO ID_USER
           FROM C_USER
           WHERE CODE = LOGIN;

         EXCEPTION
           WHEN NO_DATA_FOUND THEN
                rollback;
                status_code := 0;
                msg_error := 'В C_USER не найдена запись с данным логином - ' || LOGIN;
                dbms_output.put_line('1out -> ' || status_code || '; 2out-> ' || msg_error || '; 3out-> ' || LOGIN);
                raise_application_error (-20000, msg_error);
                RETURN;
         END;

         BEGIN
             -- проводим синхронизацию нового пользователя
             c_pkgconnect.popen(); c_pkgsession.doper := trunc(SYSDATE);
             c_pkguser.pSendUserSync(p_ID => ID_USER);
         EXCEPTION
             WHEN OTHERS THEN
               rollback;
               status_code := 0;
               msg_error := 'Произошла ошибка при синхронизации пользователя - ' || LOGIN;
               dbms_output.put_line('1out -> ' || status_code || '; 2out-> ' || msg_error || '; 3out-> ' || LOGIN);
               raise_application_error (-20000, msg_error);
               RETURN;
         END;

         BEGIN
           -- создаем позицию для пользователя в USRGRANT
           c_pkgconnect.popen(); c_pkgsession.doper := trunc(SYSDATE);
           c_pkgusr.pInsUpd(
                      p_ID => p_ID,
                      p_CODE => login_pos,
                      p_USE_ID => ARM,
                      p_DEP_ID => ID_DEP,
                      p_GROUPFL => GRPFL,
                      p_ARCFL => ARCFL_POS,
                      p_ARESTFL => ARESTFL_POS,
                      p_ADMINFL => ADM,
                      p_LONGNAME => FIO,
                      p_PRIM => PRIM,
                      p_LASTDAYS => 0,
                      p_EMAIL => EMAIL,
                      p_FROMDATE => FROMDATE,
                      p_USER_PHONE => PHONE,
                      p_Mode => null,
                      p_SYNCFL => 0);
           -- проверяем, что позиция пользователя добавлена в C_USR. Заодно получаем ID записи
           SELECT ID
           INTO ID_POS
           FROM C_USR
           WHERE CODE = login_pos;
         EXCEPTION
           WHEN NO_DATA_FOUND THEN
             rollback;
             status_code := 0;
             msg_error := 'Не найдена созданная позиция - ' || LOGIN || ', в C_USR';
             dbms_output.put_line('1out -> ' || status_code || '; 2out-> ' || msg_error || '; 3out-> ' || LOGIN);
             raise_application_error (-20000, msg_error);
             RETURN;
           WHEN OTHERS THEN
             rollback;
             status_code := 0;
             msg_error := 'Произошла ошибка при создании позиции пользователя - ' || LOGIN;
             dbms_output.put_line('1out -> ' || status_code || '; 2out-> ' || msg_error || '; 3out-> ' || LOGIN);
             raise_application_error (-20000, msg_error);
             RETURN;

         END;

         BEGIN
           -- выполняем синхронизацию созданной позиции
           c_pkgusr.pSendUsrSync(p_ID => ID_POS,
                                 p_FROMDATE => FROMDATE,
                                 p_TODATE => TODATE);
         EXCEPTION
            WHEN OTHERS THEN
               rollback;
               status_code := 0;
               msg_error := 'Произошла ошибка при синхронизации позиции пользователя - ' || LOGIN;
               dbms_output.put_line('1out -> ' || status_code || '; 2out-> ' || msg_error || '; 3out-> ' || LOGIN);
               raise_application_error (-20000, msg_error);
               RETURN;
         END;

         BEGIN
           -- привязываем созданную позицию к созданному пользователю
           c_pkgconnect.popen(); c_pkgsession.doper := trunc(SYSDATE);
           c_pkgusr.pSaveStfUsrLink(
                                    pID => ID_STF,
                                    pSTF_ID => ID_POS,
                                    pUSR_ID => ID_USER,
                                    pOWNERFL => '1',
                                    pDELEGATEFL => '0',
                                    pFROMDATE => FROMDATE,
                                    pTODATE => TODATE,
                                    pPRIM => PRIM,
                                    p_SYNCFL => null);

           -- проверяем, что запись о привязке есть в C_STFUSR
           SELECT ID
           INTO STF_ID
           FROM C_STFUSR
           WHERE STF_ID = ID_POS and USR_ID = ID_USER;
         EXCEPTION
           WHEN NO_DATA_FOUND THEN
             rollback;
             status_code := 0;
             msg_error := 'Не найдена привязанная запись по пользователю - ' || LOGIN || ', в C_STFUSR';
             dbms_output.put_line('1out -> ' || status_code || '; 2out-> ' || msg_error || '; 3out-> ' || LOGIN);
             raise_application_error (-20000, msg_error);
             RETURN;
           WHEN OTHERS THEN
             rollback;
             status_code := 0;
             msg_error := 'Произошла ошибка при привязке позиции к пользователю - ' || LOGIN;
             dbms_output.put_line('1out -> ' || status_code || '; 2out-> ' || msg_error || '; 3out-> ' || LOGIN);
             raise_application_error (-20000, msg_error);
             RETURN;
         END;
      commit;
      status := 'OK';
      msg_result := 'Создан пользователь с логином - ' || LOGIN;
      dbms_output.put_line('1out -> ' || status || '; 2out-> ' || msg_result || '; 3out-> ' || LOGIN);


      EXCEPTION
        WHEN OTHERS THEN
          rollback;
          status_code := 0;
          msg_error := 'Произошла ошибка при создании пользователя - ' || LOGIN;
          dbms_output.put_line('1out -> ' || status_code || '; 2out-> ' || msg_error || '; 3out-> ' || LOGIN);
          raise_application_error (-20000, msg_error);
          RETURN;

END;

PROCEDURE AT_pSetUsrGrn(TEST_USER_ID number, REAL_USER_ID number) IS

TYPE rol_array IS VARRAY(50) OF NUMBER; /* массив для хранения id профилей полномочий юзера */
my_rol_array rol_array := rol_array();
TYPE grn_array_grn_id IS VARRAY(50) OF NUMBER; /* массив для хранения grn_id полномочий юзера */
my_grn_array_grn_id grn_array_grn_id := grn_array_grn_id();
TYPE grn_array_id IS VARRAY(50) OF NUMBER; /* массив для хранения id полномочий юзера */
my_grn_array_id grn_array_id := grn_array_id();
TYPE grp_array IS VARRAY(50) OF NUMBER; /* массив для хранения id групп полномочий юзера */
my_grp_array grp_array := grp_array();
CURSOR cursor_rol_array IS SELECT DISTINCT ROL_ID FROM C_USRROL WHERE USR_ID = REAL_USER_ID; /* создаем курсор для получения id профилей полномочий реального пользователя*/
CURSOR cursor_grn_array_grn_id IS SELECT GRN_ID FROM C_USRGRN WHERE USR_ID = REAL_USER_ID ORDER BY GRN_ID ASC; /* создаем курсор для получения grn_id полномочий реального пользователя*/
CURSOR cursor_grn_array_id IS SELECT ID FROM C_USRGRN WHERE USR_ID = REAL_USER_ID ORDER BY GRN_ID ASC; /* создаем курсор для получения id полномочий реального пользователя*/
CURSOR cursor_grp_array IS SELECT DISTINCT HDR_ID from C_USRGRP WHERE DTL_ID = REAL_USER_ID; /* создаем курсор для получения id группы реального пользователя*/
usr_rol NUMBER;
usr_grn NUMBER;
usr_grp NUMBER;
BEGIN
      FOR i IN cursor_rol_array LOOP /* добавляем в массив id профилей полномочий реального пользователя */
          usr_rol := i.ROL_ID;
          my_rol_array.extend;
          my_rol_array(my_rol_array.count) := usr_rol;
      END LOOP;

      FOR i IN cursor_grn_array_grn_id LOOP /* добавляем в массив grn_id полномочий реального пользователя */
          usr_grn := i.GRN_ID;
          my_grn_array_grn_id.extend;
          my_grn_array_grn_id(my_grn_array_grn_id.count) := usr_grn;
      END LOOP;

      FOR i IN cursor_grn_array_id LOOP /* добавляем в массив id полномочий реального пользователя */
          usr_grn := i.ID;
          my_grn_array_id.extend;
          my_grn_array_id(my_grn_array_id.count) := usr_grn;
      END LOOP;

      FOR i IN cursor_grp_array LOOP /* добавляем в массив id групп полномочий реального пользователя */
          usr_grp := i.HDR_ID;
          my_grp_array.extend;
          my_grp_array(my_grp_array.count) := usr_grp;
      END LOOP;

      /* добавляем профили полномочий реального пользователя тестовому пользователю*/
      IF my_rol_array.count > 0 THEN
          FOR j IN 1..my_rol_array.count LOOP
              c_pkgconnect.popen(); c_pkgsession.doper := trunc(sysdate);
              c_pkgusr.pRolInsDel(p_Ins => 1, p_Grp1 => TEST_USER_ID, p_Grp2 => null, p_Dtl1 => my_rol_array(j), p_Dtl2 => null);
          END LOOP;
      ELSE
          DBMS_OUTPUT.PUT_LINE('По переданному id позиции пользователя не найдены профили полномочий');
      END IF;
      /* добавляем полномочия реального пользователя тестовому пользователю*/
      IF my_grn_array_grn_id.count > 0 THEN
          FOR j IN 1..my_grn_array_grn_id.count LOOP
              c_pkgconnect.popen(); c_pkgsession.doper := trunc(sysdate);
              c_pkgusr.pGrnInsDel(p_Ins => 1, p_Grp1 => TEST_USER_ID, p_Grp2 => null, p_Dtl1 => my_grn_array_grn_id(j), p_Dtl2 => my_grn_array_id(j));
          END LOOP;
      ELSE
          DBMS_OUTPUT.PUT_LINE('По переданному id позиции пользователя не найдены полномочия');
      END IF;
      /* добавляем группы полномочий реального пользователя тестовому пользователю*/
      IF my_grp_array.count > 0 THEN
          FOR j IN 1..my_grp_array.count LOOP
              c_pkgconnect.popen(); c_pkgsession.doper := trunc(sysdate);
              c_pkgusr.pGrpUsrInsDel(p_Ins => 1, p_Grp1 => TEST_USER_ID, p_Grp2 => null, p_Dtl1 => my_grp_array(j), p_Dtl2 => '');
          END LOOP;
      ELSE
          DBMS_OUTPUT.PUT_LINE('По переданному id позиции пользователя не найдены группы полномочий');
      END IF;
      /* Делаем пересчет неявно назначенных полномочий по тестовому пользователю*/
      BEGIN
          c_pkgconnect.popen(); c_pkgsession.doper := trunc(sysdate);
          C_PKGACCESS.pPrepUsrGrn (iTUS_ID => TEST_USER_ID);
      END;

END;

PROCEDURE AT_pSetPkbReport(CLI_CODE in varchar2)
IS
TEXT_REPORT CLOB;
ID_CLI number;
CLI_ID_DEP number;
MAX_REP_ID number;
HST NUMBER(10);
FL_ST VARCHAR2(1 BYTE);
STAT_FL CHAR(1);
REPTYPE NUMBER(10);
IDUS NUMBER(10);
NORD_ID NUMBER(5);
IIN VARCHAR2(30);
CLIINFOREQ number;
UNIQ_REP_ID number;
begin
  /*Получаем по коду клиента id и dep_id клиента*/
  SELECT ID, DEP_ID INTO ID_CLI, CLI_ID_DEP FROM G_CLI WHERE CODE = CLI_CODE AND rownum = 1;

  /* Получаем максимальный айди репорта*/
  SELECT MAX(REP_ID) INTO MAX_REP_ID FROM l_clihstext;
  UNIQ_REP_ID := MAX_REP_ID + 1;

  /* Получаем данные отчета, который берется в качестве примера*/
  SELECT HSTINFO, FLSTATE, REPORT, STATUSFL, REPTYPE_ID, ID_US, NORD, TAXCODE, CLIINFOREQ_ID
  INTO HST, FL_ST, TEXT_REPORT, STAT_FL, REPTYPE, IDUS, NORD_ID, IIN, CLIINFOREQ FROM l_clihstext WHERE REP_ID = 17669882;
  /*DBMS_OUTPUT.PUT_LINE('1out ->, ' || HST || ' 2out ->, ' || FL_ST || ' 3out ->, ' || TEXT_REPORT || ' 4out ->, ' || STAT_FL || ' 5out ->, ' || REPTYPE || ' 6out ->, ' || IDUS || ' 7out ->, ' || NORD_ID || ' 8out ->, ' || IIN || ' 9out ->, ' || CLIINFOREQ || ' 10out -> ' || UNIQ_REP_ID);*/

  /* создаем новую запись с отчетом где в cli_id и cli_dep_id будут данные тестового клиента, код которого передам в процедуру*/
  INSERT INTO l_clihstext(DEP_ID, ID, CLI_DEP_ID, CLI_ID, DOC_CODE, DOC_TYPE, DOC_DATE, HSTINFO, REASON, DISSUE, DRETURN, PRIM, FLSTATE, REP_ID, REPORT, STATUSFL, REPTYPE_ID, ID_US, NORD, TAXCODE, CLIINFOREQ_ID, ERR)
  VALUES(null, null, CLI_ID_DEP, ID_CLI, null, null, sysdate, HST, null, sysdate, sysdate, null, FL_ST, UNIQ_REP_ID, TEXT_REPORT, STAT_FL, REPTYPE, IDUS, NORD_ID, IIN, CLIINFOREQ, null);
  COMMIT;
end;

PROCEDURE AT_pSetGrn(id_grn in number, id_grn_2 in number default null, user_pos_id in number, type_grn in varchar2)
IS

BEGIN
  /* Выполняем процедуру для добавления полномочия пользователю*/
  IF type_grn = 'ROL' THEN
    c_pkgconnect.popen(); c_pkgsession.doper := trunc(sysdate);
    c_pkgusr.pRolInsDel(p_Ins => 1, p_Grp1 => user_pos_id, p_Grp2 => null, p_Dtl1 => id_grn, p_Dtl2 => null);
  ELSIF type_grn = 'GRN' THEN
    c_pkgconnect.popen(); c_pkgsession.doper := trunc(sysdate);
    c_pkgusr.pGrnInsDel(p_Ins => 1, p_Grp1 => user_pos_id, p_Grp2 => null, p_Dtl1 => id_grn, p_Dtl2 => id_grn_2);
  ELSIF type_grn = 'GRP' THEN
    c_pkgconnect.popen(); c_pkgsession.doper := trunc(sysdate);
    c_pkgusr.pGrpUsrInsDel(p_Ins => 1, p_Grp1 => user_pos_id, p_Grp2 => null, p_Dtl1 => id_grn, p_Dtl2 => '');
  ELSE
    DBMS_OUTPUT.PUT_LINE('Передан неверный тип полномочия');
  END IF;
  /* Выполняем процедуру по выполнению процедуры по вводу в действие полномочия*/
  c_pkgconnect.popen(); c_pkgsession.doper := trunc(sysdate);
  C_PKGACCESS.pPrepUsrGrn(iTUS_ID => user_pos_id);

END;

PROCEDURE AT_pInsGrnDtst(ID in number,
                         REAL_LOGIN in varchar2,
                         TYPE_GRN in varchar2,
                         ID_GRN in number,
                         ID_GRN_2 in number)
IS
/* процедура инсертит записи с полномочиями в датасет GRNUSERS */

BEGIN
  INSERT INTO GRNUSERS(ID, REAL_LOGIN, TYPE_GRN, ID_GRN, ID_GRN_2)
  VALUES(ID, REAL_LOGIN, TYPE_GRN, ID_GRN, ID_GRN_2);
  COMMIT;
EXCEPTION
  WHEN DUP_VAL_ON_INDEX THEN
    DBMS_OUTPUT.PUT_LINE('Полномочие с ID - ' || ID || ' уже существует в датасете GRNUSERS');
END;

PROCEDURE AT_pCreSKSDea(CLI_CODE in varchar2, TYPE_DEA in varchar2, NUM_DEA in varchar2, ID_TRF_CAT in varchar2)
IS
ID_CLI number;
DEP_ID_CLI number;
ID_DEA number;
BEGIN

  /* Получаем id и dep_id клиента по коду карточки */
  SELECT ID, DEP_ID INTO ID_CLI, DEP_ID_CLI FROM G_CLI
  WHERE CODE = CLI_CODE;

  BEGIN
        c_pkgconnect.popen(); c_pkgsession.doper := trunc(sysdate);
        N_BSCRDDEA.PSAVE(NDEP_ID => DEP_ID_CLI, NID => ID_DEA, SDCL_CODE => TYPE_DEA, DORD => trunc(sysdate),
                         SCODE => NUM_DEA, NCLI_DEP_ID => DEP_ID_CLI,
                         NCLI_ID => ID_CLI, NTRF_IDCAT => ID_TRF_CAT, SPRIM => 'Autotests');
        COMMIT;
        dbms_output.put_line('1out -> ' || 'Создан договор СКС по номеру договора - ' || NUM_DEA);

  EXCEPTION
    WHEN OTHERS THEN
      dbms_output.put_line(replace(sqlerrm, 'ORA' || sqlcode || ': ', ''));
  END;

EXCEPTION
  WHEN NO_DATA_FOUND THEN
    dbms_output.put_line('Не найдены данные по переданному клиенту' || ' - ' || CLI_CODE);
  WHEN OTHERS THEN
    dbms_output.put_line(replace(sqlerrm, 'ORA' || sqlcode || ': ', ''));

END;

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
                                           NOEMBFL in number) -- признак неименной карточки. 0 - нет, 1 - да.
IS
CLI_ID number;
CLI_DEP_ID number;
ID_VAL number;
ID_TRF number;
ID_CARD_DEA number;
ID_ACC number;
DEP_ID_ACC number;
DEA_ACC_ID number;
DEA_ACC_DEP_ID number;
DEP_ID_SKS number;
ID_SKS number;
depId number;
BEGIN
  SELECT ID, DEP_ID INTO CLI_ID, CLI_DEP_ID FROM G_CLI
  WHERE CODE = CLI_CODE;
  BEGIN
    SELECT DEP_ID, ID INTO DEP_ID_SKS, ID_SKS FROM N_CRDDEA
    WHERE CODE = DEA_CODE_SKS;
    BEGIN
      SELECT ID INTO ID_VAL FROM T_VAL_STD
      WHERE CODE = VAL_CODE;

      BEGIN
        SELECT ID INTO ID_TRF FROM S_TRFGRP
        WHERE CODE = TRF_CODE;
        BEGIN
          SELECT ID, DEP_ID INTO ID_ACC, DEP_ID_ACC from G_ACCBLN
          WHERE CODE = ACC_CODE;
          SELECT ID, DEP_ID INTO DEA_ACC_ID, DEA_ACC_DEP_ID FROM S_DEAACC
          where ACC_ID = ID_ACC and ACC_DEP_ID = DEP_ID_ACC;

          BEGIN
            depId := AT_fGetIdDep(DepCode);
            c_pkgconnect.popen(); c_pkgsession.doper := trunc(sysdate);
            N_BSCRD.PSAVE(NDEP_ID => depId, NID => ID_CARD_DEA, NDEA_DEP_ID => DEP_ID_SKS, NDEA_ID => ID_SKS, SDCL_CODE => DCL_CODE, DREQDT => trunc(sysdate),
                          NCRD_ID => CRD_ID, SCARDCODE => CARDCODE, SEMBOSSEDNAME => EMBOSSEDNAME, NBAS_DEP_ID => null, NBAS_ID => null,
                          CURGENTFL => 0, DEXPIREDT => trunc(sysdate)+1095, SCODEWORD => null, NCLI_DEP_ID => CLI_DEP_ID, NCLI_ID => CLI_ID, NDEP_ID_DELIV => depId,
                          NDEAACC_DEP_ID => DEA_ACC_DEP_ID, NDEAACC_ID => DEA_ACC_ID, NVAL_ID => ID_VAL, NHOLDER_DEP_ID => CLI_DEP_ID, NHOLDER_ID => CLI_ID, NTRF_IDCAT => ID_TRF,
                          DORD => trunc(sysdate), SCODE => CODE, SPRIM => 'Autotests', DFROMDATE => null, DTODATE => null, NPARENTPROC => NULL,
                          NPARENTOPR => NULL, SSTATCODE => NULL, SDESIGN => NULL, SCOMPANYNAME => NULL, NNOEMBFL => NOEMBFL, SCODEQUESTION => NULL,
                          NCHKEXPDT => 1, SREFER => NULL, SCARDIDN => NULL, NBAL_DEP_ID => NULL, NSRV_DEP_ID => NULL, NSELL_DEP_ID => NULL,
                          NPREV_DEP_ID => NULL, NPREV_ID => NULL, DNEXTRENEWDT => NULL);
            COMMIT;
          EXCEPTION
            WHEN NO_DATA_FOUND THEN
              dbms_output.put_line('1out -> ' || ID_CARD_DEA || '; 2out-> ' || 'Создан карточный договор');
            WHEN OTHERS THEN
              dbms_output.put_line(replace(sqlerrm, 'ORA' || sqlcode || ': ', ''));
          END;

        EXCEPTION
          WHEN NO_DATA_FOUND THEN
            dbms_output.put_line('Не найдены данные по переданному счету' || ' - ' || ACC_CODE);
          WHEN OTHERS THEN
            dbms_output.put_line(replace(sqlerrm, 'ORA' || sqlcode || ': ', ''));
        END;

      EXCEPTION
        WHEN NO_DATA_FOUND THEN
          dbms_output.put_line('Не найдены данные по переданному тарифу' || ' - ' || TRF_CODE);
        WHEN OTHERS THEN
          dbms_output.put_line(replace(sqlerrm, 'ORA' || sqlcode || ': ', ''));
      END;

    EXCEPTION
      WHEN NO_DATA_FOUND THEN
        dbms_output.put_line('Не найдены данные по переданной валюте' || ' - ' || VAL_CODE);
      WHEN OTHERS THEN
        dbms_output.put_line(replace(sqlerrm, 'ORA' || sqlcode || ': ', ''));
    END;
  EXCEPTION
    WHEN NO_DATA_FOUND THEN
      dbms_output.put_line('Не найдены данные по переданному договору СКС' || ' - ' || DEA_CODE_SKS);
    WHEN OTHERS THEN
      dbms_output.put_line(replace(sqlerrm, 'ORA' || sqlcode || ': ', ''));
  END;
EXCEPTION
  WHEN NO_DATA_FOUND THEN
    dbms_output.put_line('Не найдены данные по переданному клиенту' || ' - ' || CLI_CODE);
  WHEN OTHERS THEN
    dbms_output.put_line(replace(sqlerrm, 'ORA' || sqlcode || ': ', ''));
END;

PROCEDURE AT_pCreNoPersonalCard(DEP_CODE in varchar2, CODE_PRODUCT_CARD in varchar2, VAL_CODE in varchar2, COUNT_CARD in number)
IS
DEP_ID number(10);
PRODUCT_ID number(10);
ID_REQUEST number(20);
VAL_ID number(10);
ID_DEPARTMENT number(10);
ID_VAL number(10);
ID_PRODUCT number(10);
REQUEST_ID number(20);

BEGIN
     /* получаем ID департамента */
     BEGIN
       SELECT ID INTO DEP_ID FROM C_DEP_STD
       WHERE CODE = DEP_CODE;

       /* получаем ID кода карточного продукта */
       BEGIN
          SELECT ID INTO PRODUCT_ID FROM T_DEACLS_STD
          WHERE CODE = CODE_PRODUCT_CARD;
          /* получаем ID валюты */
          BEGIN
              SELECT ID INTO VAL_ID FROM T_VAL_STD
              WHERE CODE = VAL_CODE;
              /* добавляем заявку на создание не персональных карт */
              BEGIN
                  /* получаем максимальный ID заявок */
                  SELECT MAX(ID) INTO ID_REQUEST FROM N_CRDADDREQ;

                  ID_DEPARTMENT := DEP_ID;
                  ID_VAL := VAL_ID;
                  ID_PRODUCT := PRODUCT_ID;
                  REQUEST_ID := ID_REQUEST + 1;

                  DBMS_OUTPUT.PUT_LINE(ID_DEPARTMENT);
                  DBMS_OUTPUT.PUT_LINE(ID_VAL);
                  DBMS_OUTPUT.PUT_LINE(ID_PRODUCT);
                  DBMS_OUTPUT.PUT_LINE(REQUEST_ID);
                  DBMS_OUTPUT.PUT_LINE(COUNT_CARD);


                  INSERT INTO N_CRDADDREQ(ID, DEP_ID, DCL_ID, DEP_ID_DELIV, VAL_ID, CRDCNT, DORD, ID_US, STATE, PRIM, CORRECTDT, ONE_SCA_AND_ACC, CRE_DEA_FL, SCA_DCL_ID, EXPIREDT)
                  VALUES(REQUEST_ID, ID_DEPARTMENT, ID_PRODUCT, ID_DEPARTMENT, VAL_ID, COUNT_CARD, trunc(sysdate), 1, 0, 'Autotests', sysdate, 0, 0, null, null);
                  COMMIT;

                  /* выполняем процедуру по обработке заявки */
                  BEGIN
                      DBMS_OUTPUT.PUT_LINE(ID_DEPARTMENT);
                      DBMS_OUTPUT.PUT_LINE(ID_VAL);
                      DBMS_OUTPUT.PUT_LINE(ID_PRODUCT);
                      DBMS_OUTPUT.PUT_LINE(REQUEST_ID);
                      DBMS_OUTPUT.PUT_LINE(COUNT_CARD);
                      c_pkgconnect.popen(); c_pkgsession.doper := trunc(sysdate);
                      N_PKGNOEMB.pExecuteCrdReq(pID => REQUEST_ID, pDepID => ID_DEPARTMENT);
                      COMMIT;

                  EXCEPTION
                      WHEN OTHERS THEN
                          dbms_output.put_line(SUBSTR( DBMS_UTILITY.format_error_stack
                                               || DBMS_UTILITY.format_error_backtrace, 1, 4000));
                  END;

              EXCEPTION
                  WHEN NO_DATA_FOUND THEN
                      DBMS_OUTPUT.PUT_LINE('Не найден максимальный ID заявок');
                  WHEN OTHERS THEN
                      dbms_output.put_line(SUBSTR( DBMS_UTILITY.format_error_stack
                                          || DBMS_UTILITY.format_error_backtrace, 1, 4000));
              END;
         EXCEPTION
             WHEN NO_DATA_FOUND THEN
                 DBMS_OUTPUT.PUT_LINE('По коду валюты ' || VAL_CODE || ' не найден ID');
         END;
       EXCEPTION
       WHEN NO_DATA_FOUND THEN
           DBMS_OUTPUT.PUT_LINE('По коду продукта ' || CODE_PRODUCT_CARD || ' не найден ID');
       END;

     EXCEPTION
         WHEN NO_DATA_FOUND THEN
             DBMS_OUTPUT.PUT_LINE('По коду департамента ' || DEP_CODE || ' не найден ID');

     END;


END;

PROCEDURE AT_pRunOperWOParams(CODE_NUM in varchar2, -- Номер документа по которому необходимо выполнить операцию
                                                OPER_CODE in varchar2) -- Код операции. Можно посмотреть в DOP1

IS
DEP_ID_DOC number;
ID_DOC number;
OutOper varchar2(2000);
BEGIN
  BEGIN
    -- Получаем ID и DEP_ID по номеру документа
    SELECT DEP_ID, ID INTO DEP_ID_DOC, ID_DOC FROM T_ORD
    WHERE CODE = CODE_NUM;

    BEGIN
      c_pkgconnect.popen(); c_pkgsession.doper := trunc(sysdate);
      T_PKGRUNOPRUTL.pRunOperation(  nDepId => DEP_ID_DOC -- Подразделение документа
                                     , nId => ID_DOC     -- Идентификатор документа
                                     , sOperCode  => OPER_CODE  -- Код операции
                                     , sInOperParams  => null          -- Входные параметры операции
                                     , sOutOperParams => OutOper          -- Выходные параметры операции
                                     , nRaise  => 1 );         -- Генерировать ли исключение ( 0, null-нет )
      COMMIT;
    END;

  END;

END;
PROCEDURE AT_pRunOperWithParams(docNum varchar2, operCode varchar2, params varchar2)
  IS
  docID number;
  docDepId number;
  OutOper varchar2(1000);
  BEGIN
    -- Получаем ID и DEP_ID по номеру документа
    docID := AT_fGetIdDoc(docNum => docNum);
    docDepId := AT_fGetDepIdDoc(docNum => docNum);
    dbms_output.put_line(docID);
    dbms_output.put_line(docDepId);
    c_pkgconnect.popen(); c_pkgsession.doper := trunc(sysdate);
    T_PKGRUNOPRUTL.pRunOperation(  nDepId => docDepId -- Подразделение документа
                                   , nId => docID     -- Идентификатор документа
                                   , sOperCode  => operCode  -- Код операции
                                   , sInOperParams  => params          -- Входные параметры операции
                                   , sOutOperParams => OutOper          -- Выходные параметры операции
                                   , nRaise  => 1 );         -- Генерировать ли исключение ( 0, null-нет )
    COMMIT;
  END;
PROCEDURE AT_pAddPaymentInstructions(cliId varchar2, depId varchar2, fCode varchar2, fId varchar2, swiftName varchar2)
IS
/* Процедура добавляет данные во Платежные инструкции, во вкладке Swift
   fCode - код платежной инструкции
   fId - идентификатор платежной инструкции
   swiftName - наименование платежной инструкции
   cliId - id клиента
   depId - id департамента клиента */

BEGIN
     UPDATE g_clihst
     SET FLD50F_CODE = fCode, FLD50F_IDENTIFIER = fId, SWIFT_NAME = swiftName
     WHERE id = cliId and dep_id = depId and trunc(sysdate) between fromdate and todate;
     COMMIT;
END;

PROCEDURE AT_pRepCurAccCashOrder(AccCode varchar2, Summ varchar2, Iin varchar2, ValCode varchar2) IS
VAL_ID number;
docNum varchar2(100);
DOC_ID number;
id number;
ord number;
dscr varchar2(255);
typeOrder varchar2(10);
casPlan varchar2(10);
BEGIN
  IF ValCode = 'KZT' THEN
      dscr := 'пополнение текущего счета';
      typeOrder := '113';
      casPlan := '02';
  ELSE
      dscr := 'Приход';
      typeOrder := '123';
      casPlan := '260';
  END IF;
  BEGIN
      /* получаем id валюты по коду валюты */
      SELECT ID INTO VAL_ID FROM T_VAL_STD
      WHERE CODE = ValCode;
  EXCEPTION
      WHEN OTHERS THEN
          raise_application_error(-20000,'Произошла ошибка - '||SQLCODE||' -ERROR- '||SQLERRM);
  END;
  /* Получаем рандомный номер договора */
  docNum := 'AT_' || floor(DBMS_Random.Value(1,100000000));
  /* Выполняем процедуру по созданию приходного ордера */
  BEGIN
      c_pkgconnect.popen(); c_pkgsession.doper := trunc(sysdate);
      S_BSCASH.PSAVE(NDEP_ID => 1388, NID => id, SKSO_CODE => typeOrder,
                    INOCMS => NULL, SCHA_CODE => '01031',
                    SCODE => docNum, DDDOK => NULL, DDVAL => NULL,
                    SACC_CODE => AccCode, SAMOUNT => Summ, STXT_DSCR => dscr,
                    SKNP => '331', SCODE_OD => '19',
                    SCODE_BE => '19', STXT_HEAD => NULL,
                    STXT_BUCH => NULL, STXT_PAY => 'Байтмир Ксенян',
                    SIDCARD => 'Удостоверение личности гражданина Республики Казахстан 035667866, документ выдан 11.09.13 г. МВД РК',
                    SPRIM => dscr,
                    SCHK_CODE => NULL, NNUMPAGE => NULL,
                    SFIRSTPAGE => NULL, SLASTPAGE => NULL,
                    NPARENTPROC => NULL, NPARENTOPR => NULL,
                    SLIMFL => NULL, SREFER => NULL,
                    SRNN_CL => Iin, SACCCASH => NULL,
                    NMON_ID => NULL, NCHK_MON_ID => NULL,
                    NDEPUSR_ID => 1388, NVALID => VAL_ID,
                    SAMOUNT_VAL => NULL, NRATE => NULL,
                    SFL_WARRANT => '0', NCSH_ID => 19492814,
                    NCSHCHR_ID => NULL, NCSHVAL_ID => NULL,
                    SCSH_AMOUNT => NULL, NACC_VAL_ID => NULL,
                    NORD_MON_ID => NULL, SDENOMINFL => '0',
                    SDELIVFL => '0', ITCD_ON => NULL,
                    NSDOKTCD => NULL, SCMS_STRG => '0',
                    SCLIACCCOM => NULL, STXT_TAXCODE => Iin);
                    COMMIT;
  EXCEPTION
      WHEN OTHERS THEN
          raise_application_error(-20000,'Произошла ошибка - '||SQLCODE||' -ERROR- '||SQLERRM);
  END;
  /* получаем ID созданного приходного ордера */
  BEGIN
      SELECT ID INTO DOC_ID FROM t_ord o
      WHERE o.code = docNum;
  EXCEPTION
      WHEN OTHERS THEN
          raise_application_error(-20000,'Произошла ошибка - '||SQLCODE||' -ERROR- '||SQLERRM);
  END;
  /* добавляем символ касплана */
  BEGIN
      c_pkgconnect.popen(); c_pkgsession.doper := trunc(sysdate);
      S_BSCASH.PSAVECSR(NDEP_ID => 1388, NID => DOC_ID, INORD => ord, SCSR_CODE => casPlan, SAMOUNT => Summ);
      COMMIT;
  EXCEPTION
      WHEN OTHERS THEN
          raise_application_error(-20000,'Произошла ошибка - '||SQLCODE||' -ERROR- '||SQLERRM);
  END;
  /* выполняем операцию Регистрация */
  BEGIN
      c_pkgconnect.popen(); c_pkgsession.doper := trunc(sysdate);
      Z_PKG_AUTO_TEST.AT_pRunOperWOParams(CODE_NUM => docNum, -- Номер документа по которому необходимо выполнить операцию
                                          OPER_CODE => 'REG'); -- Код операции. Можно посмотреть в DOP1
      COMMIT;
  EXCEPTION
      WHEN OTHERS THEN
          raise_application_error(-20000,'Произошла ошибка - '||SQLCODE||' -ERROR- '||SQLERRM);
  END;
  /* выполняем операцию Прием денег */
  BEGIN
      c_pkgconnect.popen(); c_pkgsession.doper := trunc(sysdate);
      Z_PKG_AUTO_TEST.AT_pRunOperWOParams(CODE_NUM => docNum, -- Номер документа по которому необходимо выполнить операцию
                                          OPER_CODE => 'POST2'); -- Код операции. Можно посмотреть в DOP1
      COMMIT;
  EXCEPTION
      WHEN OTHERS THEN
          raise_application_error(-20000,'Произошла ошибка - '||SQLCODE||' -ERROR- '||SQLERRM);
  END;


END;
PROCEDURE AT_pInsYesKb(cli_code varchar2)
  IS
CLI_DEP_ID number;
CLI_ID number;
  BEGIN
    /*получаем id и dep_id по коду карточки*/
    SELECT DEP_ID, ID INTO CLI_DEP_ID, CLI_ID
    FROM G_CLI
    WHERE CODE = cli_code;
    /* инсертим запись согласия КБ */
    INSERT INTO Z_077_T_L_CLIHST(CLI_ID, CLI_DEP_ID, DISSUE, BKFL, CORRECTDT, USER_ID, PRIM, PRIZN)
    VALUES(CLI_ID, CLI_DEP_ID, trunc(sysdate), 1, sysdate, 1, NULL, NULL);
    COMMIT;
  EXCEPTION
    WHEN OTHERS THEN
         raise_application_error(-20000,'Произошла ошибка - '||SQLCODE||' -ERROR- '||SQLERRM);
  END;
PROCEDURE AT_pCreIndividRate(docNum varchar2, value_rate varchar2)
IS
C_DEP_ID number;
C_ID number;
clc number := 1556569;
ver number;
idPcn number;
BEGIN
  /* Получаем dep_id и id договора по номеру договора */
  BEGIN
    SELECT d.DEP_ID, d.ID
    INTO C_DEP_ID, C_ID
    FROM T_ORD o, T_DEA d
    WHERE o.DEP_ID = d.DEP_ID and o.ID = d.ID and o.code = docNum;
  /* Получаем clc_id и pcn_id индивидуальной ставки по переданному договору */
    DBMS_OUTPUT.PUT_LINE(C_DEP_ID || ' ' || C_ID);
    SELECT CLC_ID, PCN_ID
    INTO clc, idPcn
    FROM LV_QR_ARL
    WHERE DEP_ID = C_DEP_ID
    and ID = C_ID and LONGNAME = 'Проценты по кредиту';
  EXCEPTION
    WHEN OTHERS THEN
      raise_application_error(-20000,'Произошла ошибка - '||SQLCODE||' -ERROR- '||SQLERRM);
  END;
  /* Вызываем процедуру для создания индивидуальной стаки */
  BEGIN
    c_pkgconnect.popen(); c_pkgsession.doper := trunc(sysdate);
    T_PKGPCNSPEC.pCre(nDEP_ID => C_DEP_ID, nID => C_ID, nCLC_ID => clc,
                      idPcn => idPcn, iVer => ver, sPrm => 'Autotests');
    COMMIT;
  EXCEPTION
    WHEN OTHERS THEN
      raise_application_error(-20000,'Произошла ошибка - '||SQLCODE||' -ERROR- '||SQLERRM);
  END;
  /* Вызываем процедуру для коммита индивидуальной ставки */
  BEGIN
    c_pkgconnect.popen(); c_pkgsession.doper := trunc(sysdate);
    T_PKGPCNSPEC.pCommitPcn(idPcn => idPcn, iVer => ver);
    COMMIT;
  EXCEPTION
    WHEN OTHERS THEN
      raise_application_error(-20000,'Произошла ошибка - '||SQLCODE||' -ERROR- '||SQLERRM);
  END;
  BEGIN
    c_pkgconnect.popen(); c_pkgsession.doper := trunc(sysdate);
    T_PKGPCNSPEC.pAddDelHst(iDel => 0,
                            idPcn              => idPcn,       --нового ID быть не может
                            iVer             => ver, --новая версия -- может появитиьс
                            dfromdate        => trunc(sysdate),
                            fpercent         => value_rate,
                            fpercent_add     => value_rate,
                            fpercent_mult    => value_rate,
                            ibase_id         => NULL,
                            fCapRate         => NULL,
                            dCapDate         => NULL,
                            fFloorRate       => NULL,
                            dFloorDate       => NULL,
                            sPrim            => 'Autotests'
                            );
      COMMIT;
    EXCEPTION
      WHEN OTHERS THEN
        raise_application_error(-20000,'Произошла ошибка - '||SQLCODE||' -ERROR- '||SQLERRM);
    END;
  /* Вызываем процедуру для коммита индивидуальной ставки */
  BEGIN
    c_pkgconnect.popen(); c_pkgsession.doper := trunc(sysdate);
    T_PKGPCNSPEC.pCommitPcn(idPcn => idPcn, iVer => ver);
    COMMIT;
  EXCEPTION
    WHEN OTHERS THEN
      raise_application_error(-20000,'Произошла ошибка - '||SQLCODE||' -ERROR- '||SQLERRM);
  END;
END;
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
                                                  subsidizing varchar2 default NULL, womBusinessSub varchar2 default NULL)
IS
cliDepId number;
cliId number;
depId number;
prd number;
pur number;
trf number;
nId number;
C_DEP_ID number;
C_ID number;
credAdminManager varchar2(100);
BEGIN
  /* Получаем id и dep_id по коду карточки клиента */
  BEGIN
    SELECT DEP_ID, ID
    INTO cliDepId, cliId
    FROM G_CLI
    WHERE CODE = cliCode;
  EXCEPTION
    WHEN OTHERS THEN
      raise_application_error(-20000,'Произошла ошибка - '||SQLCODE||' -ERROR- '||SQLERRM);
  END;
  BEGIN
  /* Получаем id департамента по коду департамента */
    SELECT ID
    INTO depId
    FROM c_dep
    WHERE code = depContract;
  EXCEPTION
    WHEN OTHERS THEN
      raise_application_error(-20000,'Произошла ошибка - '||SQLCODE||' -ERROR- '||SQLERRM);
  END;
  BEGIN
  /* Получаем id срока кредита */
    SELECT ID
    INTO prd
    FROM T_DEAPRD_STD
    WHERE LONGNAME = period;
  EXCEPTION
    WHEN OTHERS THEN
      raise_application_error(-20000,'Произошла ошибка - '||SQLCODE||' -ERROR- '||SQLERRM);
  END;
  BEGIN
  /* Получаем id цели кредита */
    SELECT ID
    INTO pur
    FROM L_PURDSC
    WHERE code = purpose;
  EXCEPTION
    WHEN OTHERS THEN
      raise_application_error(-20000,'Произошла ошибка - '||SQLCODE||' -ERROR- '||SQLERRM);
  END;
  BEGIN
  /* Получаем id тарифа */
    SELECT ID
    INTO trf
    FROM S_TRFGRP
    WHERE code = trfCode;
  EXCEPTION
    WHEN OTHERS THEN
      raise_application_error(-20000,'Произошла ошибка - '||SQLCODE||' -ERROR- '||SQLERRM);
  END;
  BEGIN
    /* Создаем кредитный договор */
    c_pkgconnect.popen(); c_pkgsession.doper := trunc(sysdate);
    L_BSLOAN.PSAVE(NDEP_ID => depId,
                   NID => nId,
                   SDCL_CODE => productCode,
                   DFROMDATE => fromdate,
                   DTODATE => todate,
                   SVAL_CODE => valCode,
                   SAMOUNT => amount,
                   NPRD_ID => prd, -- id срока кредитования. Справочниками со сроками T_DEAPRD_STD
                   NCLI_DEP_ID => cliDepId,
                   NCLI_ID => cliId,
                   SPRIM => prim,
                   SCODE => codeContract,
                   IDPUR => pur, -- id цели кредитование. Справочник с целями L_PURDSC
                   NPARENTPROC =>NULL,
                   NPARENTOPR => NULL,
                   SSTATCODE => NULL,
                   DRECV => NULL,
                   DDSF => NULL,
                   SNSF => NULL,
                   SREFER => NULL,
                   NTUS_ID => NULL,
                   CIMPFL => '0',
                   NSELL_DEP_ID => NULL,
                   NDEA_STATE => 0,
                   NSELL_ID => NULL,
                   NBAL_DEP_ID => NULL,
                   NSRV_DEP_ID => NULL,
                   NVAL_RAT => NULL,
                   NTRF_IDCAT => trf,
                   SINDEX_VAL_CODE => NULL,
                   CINDEXFL => '0',
                   NINDEX_VAL_RATE => NULL,
                   SNOMINAL_VALUE => NULL,
                   SPURCHASE_VALUE => NULL,
                   CSELSRVFL => '0',
                   IDBNCHMRK => NULL,
                   STIMETYPE => NULL,
                   NTIMEUNIT => NULL,
                   NCLI_ACC_DEP_ID => NULL,
                   NCLI_ACC_ID => NULL);
    COMMIT;
  EXCEPTION
    WHEN OTHERS THEN
      raise_application_error(-20000,'Произошла ошибка - '||SQLCODE||' -ERROR- '||SQLERRM);
  END;
  BEGIN
    /* Устанавливаем индивидуальную ставку */
    c_pkgconnect.popen(); c_pkgsession.doper := trunc(sysdate);
    Z_PKG_AUTO_TEST.AT_pCreIndividRate(docNum => codeContract, value_rate => individRate);
    COMMIT;
  EXCEPTION
    WHEN OTHERS THEN
      raise_application_error(-20000,'Произошла ошибка - '||SQLCODE||' -ERROR- '||SQLERRM);
  END;
  BEGIN
    /* Получаем id и dep_id созданного договора */
    SELECT d.DEP_ID, d.ID
    INTO C_DEP_ID, C_ID
    FROM T_ORD o, T_DEA d
    WHERE o.DEP_ID = d.DEP_ID and o.ID = d.ID and o.code = codeContract;
  EXCEPTION
    WHEN OTHERS THEN
      raise_application_error(-20000,'Произошла ошибка - '||SQLCODE||' -ERROR- '||SQLERRM);
  END;
  /* Получаем Менеджера НПК БК */
  BEGIN
    c_pkgconnect.popen(); c_pkgsession.doper := trunc(sysdate);
    credAdminManager := Z_PKG_AUTO_TEST.AT_fGetAdminManager;
  EXCEPTION
    WHEN OTHERS THEN
      raise_application_error(-20000,'Произошла ошибка - '||SQLCODE||' -ERROR- '||SQLERRM);
  END;
  /* Устанавливаем параметры договора */
  BEGIN
    c_pkgconnect.popen(); c_pkgsession.doper := trunc(sysdate);
    T_PKGDEAPRM.pSetPrmId(idDea => C_ID, idDep => C_DEP_ID, idPar => 10501, p_value => dateDecisionKK);
    COMMIT;
  EXCEPTION
    WHEN OTHERS THEN
      raise_application_error(-20000,'Произошла ошибка - '||SQLCODE||' -ERROR- '||SQLERRM);
  END; -- Дата решения КК
  BEGIN
    c_pkgconnect.popen(); c_pkgsession.doper := trunc(sysdate);
    T_PKGDEAPRM.pSetPrmId(idDea => C_ID, idDep => C_DEP_ID, idPar => 12998, p_value => dateDecisionKK);
    COMMIT;
  EXCEPTION
    WHEN OTHERS THEN
      raise_application_error(-20000,'Произошла ошибка - '||SQLCODE||' -ERROR- '||SQLERRM);
  END;
  BEGIN
    c_pkgconnect.popen(); c_pkgsession.doper := trunc(sysdate);
    T_PKGDEAPRM.pSetPrmId(idDea => C_ID, idDep => C_DEP_ID, idPar => 9581, p_value => sourceFinance);
    COMMIT;
  EXCEPTION
    WHEN OTHERS THEN
      raise_application_error(-20000,'Произошла ошибка - '||SQLCODE||' -ERROR- '||SQLERRM);
  END; -- Источник финансирования
  BEGIN
    c_pkgconnect.popen(); c_pkgsession.doper := trunc(sysdate);
    T_PKGDEAPRM.pSetPrmId(idDea => C_ID, idDep => C_DEP_ID, idPar => 9258, p_value => signFinance);
    COMMIT;
  EXCEPTION
    WHEN OTHERS THEN
      raise_application_error(-20000,'Произошла ошибка - '||SQLCODE||' -ERROR- '||SQLERRM);
  END; -- Признак финансирования
  BEGIN
    c_pkgconnect.popen(); c_pkgsession.doper := trunc(sysdate);
    T_PKGDEAPRM.pSetPrmId(idDea => C_ID, idDep => C_DEP_ID, idPar => 10210, p_value => noteFinance);
    COMMIT;
  EXCEPTION
    WHEN OTHERS THEN
      raise_application_error(-20000,'Произошла ошибка - '||SQLCODE||' -ERROR- '||SQLERRM);
  END; -- Примечание по финансированию
  BEGIN
    c_pkgconnect.popen(); c_pkgsession.doper := trunc(sysdate);
    T_PKGDEAPRM.pSetPrmId(idDea => C_ID, idDep => C_DEP_ID, idPar => 10942, p_value => sourceFinanceKM);
    COMMIT;
  EXCEPTION
    WHEN OTHERS THEN
      raise_application_error(-20000,'Произошла ошибка - '||SQLCODE||' -ERROR- '||SQLERRM);
  END; -- Источник финансирования КМ
  BEGIN
    c_pkgconnect.popen(); c_pkgsession.doper := trunc(sysdate);
    T_PKGDEAPRM.pSetPrmId(idDea => C_ID, idDep => C_DEP_ID, idPar => 12665, p_value => dateAgreement);
    COMMIT;
  EXCEPTION
    WHEN OTHERS THEN
      raise_application_error(-20000,'Произошла ошибка - '||SQLCODE||' -ERROR- '||SQLERRM);
  END; -- Дата соглашения
  BEGIN
    c_pkgconnect.popen(); c_pkgsession.doper := trunc(sysdate);
    T_PKGDEAPRM.pSetPrmId(idDea => C_ID, idDep => C_DEP_ID, idPar => 12095, p_value => marketRate);
    COMMIT;
  EXCEPTION
    WHEN OTHERS THEN
      raise_application_error(-20000,'Произошла ошибка - '||SQLCODE||' -ERROR- '||SQLERRM);
  END; -- Рыночная ставка
  BEGIN
    c_pkgconnect.popen(); c_pkgsession.doper := trunc(sysdate);
    T_PKGDEAPRM.pSetPrmId(idDea => C_ID, idDep => C_DEP_ID, idPar => 12056, p_value => SPPI);
    COMMIT;
  EXCEPTION
    WHEN OTHERS THEN
      raise_application_error(-20000,'Произошла ошибка - '||SQLCODE||' -ERROR- '||SQLERRM);
  END; -- Результат SPPI
  BEGIN
    c_pkgconnect.popen(); c_pkgsession.doper := trunc(sysdate);
    T_PKGDEAPRM.pSetPrmId(idDea => C_ID, idDep => C_DEP_ID, idPar => 13816, p_value => balanceDebt);
    COMMIT;
  EXCEPTION
    WHEN OTHERS THEN
      raise_application_error(-20000,'Произошла ошибка - '||SQLCODE||' -ERROR- '||SQLERRM);
  END; -- Остаток задолженности по беззалоговым займам
  BEGIN
    c_pkgconnect.popen(); c_pkgsession.doper := trunc(sysdate);
    T_PKGDEAPRM.pSetPrmId(idDea => C_ID, idDep => C_DEP_ID, idPar => 11002, p_value => creditOfficer);
    COMMIT;
  EXCEPTION
    WHEN OTHERS THEN
      raise_application_error(-20000,'Произошла ошибка - '||SQLCODE||' -ERROR- '||SQLERRM);
  END; -- Кредитный офицер
  BEGIN
    c_pkgconnect.popen(); c_pkgsession.doper := trunc(sysdate);
    T_PKGDEAPRM.pSetPrmId(idDea => C_ID, idDep => C_DEP_ID, idPar => 10421, p_value => creditAdmin);
    COMMIT;
  EXCEPTION
    WHEN OTHERS THEN
      raise_application_error(-20000,'Произошла ошибка - '||SQLCODE||' -ERROR- '||SQLERRM);
  END; -- Кредитный администратор
  BEGIN
    c_pkgconnect.popen(); c_pkgsession.doper := trunc(sysdate);
    T_PKGDEAPRM.pSetPrmId(idDea => C_ID, idDep => C_DEP_ID, idPar => 11328, p_value => issuingApproval);
    COMMIT;
  EXCEPTION
    WHEN OTHERS THEN
      raise_application_error(-20000,'Произошла ошибка - '||SQLCODE||' -ERROR- '||SQLERRM);
  END; -- Орган одобрения выдачи
  BEGIN
    c_pkgconnect.popen(); c_pkgsession.doper := trunc(sysdate);
    T_PKGDEAPRM.pSetPrmId(idDea => C_ID, idDep => C_DEP_ID, idPar => 10782, p_value => dateSchedule);
    COMMIT;
  EXCEPTION
    WHEN OTHERS THEN
      raise_application_error(-20000,'Произошла ошибка - '||SQLCODE||' -ERROR- '||SQLERRM);
  END; -- Дата начала формирования графика
  BEGIN
    c_pkgconnect.popen(); c_pkgsession.doper := trunc(sysdate);
    T_PKGDEAPRM.pSetPrmId(idDea => C_ID, idDep => C_DEP_ID, idPar => 11044, p_value => appLoan);
    COMMIT;
  EXCEPTION
    WHEN OTHERS THEN
      raise_application_error(-20000,'Произошла ошибка - '||SQLCODE||' -ERROR- '||SQLERRM);
  END; -- Заявка на кредит без комиссии
  BEGIN
    c_pkgconnect.popen(); c_pkgsession.doper := trunc(sysdate);
    T_PKGDEAPRM.pSetPrmId(idDea => C_ID, idDep => C_DEP_ID, idPar => 10191, p_value => dateSigning);
    COMMIT;
  EXCEPTION
    WHEN OTHERS THEN
      raise_application_error(-20000,'Произошла ошибка - '||SQLCODE||' -ERROR- '||SQLERRM);
  END; -- Дата подписания
  BEGIN
    c_pkgconnect.popen(); c_pkgsession.doper := trunc(sysdate);
    T_PKGDEAPRM.pSetPrmId(idDea => C_ID, idDep => C_DEP_ID, idPar => 1501, p_value => payDay);
    COMMIT;
  EXCEPTION
    WHEN OTHERS THEN
      raise_application_error(-20000,'Произошла ошибка - '||SQLCODE||' -ERROR- '||SQLERRM);
  END; -- День месяца для погашения по графику
  BEGIN
    c_pkgconnect.popen(); c_pkgsession.doper := trunc(sysdate);
    T_PKGDEAPRM.pSetPrmId(idDea => C_ID, idDep => C_DEP_ID, idPar => 5514, p_value => payDayOd);
    COMMIT;
  EXCEPTION
    WHEN OTHERS THEN
      raise_application_error(-20000,'Произошла ошибка - '||SQLCODE||' -ERROR- '||SQLERRM);
  END; -- День месяца для погашения по графику ОД (*указывается для обычного типа графика)
  BEGIN
    c_pkgconnect.popen(); c_pkgsession.doper := trunc(sysdate);
    T_PKGDEAPRM.pSetPrmId(idDea => C_ID, idDep => C_DEP_ID, idPar => 10601, p_value => numDecisionKK);
    COMMIT;
  EXCEPTION
    WHEN OTHERS THEN
      raise_application_error(-20000,'Произошла ошибка - '||SQLCODE||' -ERROR- '||SQLERRM);
  END; --Номер решения КК
  BEGIN
    c_pkgconnect.popen(); c_pkgsession.doper := trunc(sysdate);
    T_PKGDEAPRM.pSetPrmId(idDea => C_ID, idDep => C_DEP_ID, idPar => 12995, p_value => entrepStat);
    COMMIT;
  EXCEPTION
    WHEN OTHERS THEN
      raise_application_error(-20000,'Произошла ошибка - '||SQLCODE||' -ERROR- '||SQLERRM);
  END; --Статус предпринимателя
  BEGIN
    c_pkgconnect.popen(); c_pkgsession.doper := trunc(sysdate);
    T_PKGDEAPRM.pSetPrmId(idDea => C_ID, idDep => C_DEP_ID, idPar => 12997, p_value => lnGuarDamu);
    COMMIT;
  EXCEPTION
    WHEN OTHERS THEN
      raise_application_error(-20000,'Произошла ошибка - '||SQLCODE||' -ERROR- '||SQLERRM);
  END; --Наличие гарантии АО "ФРП "ДАМУ"
  BEGIN
    c_pkgconnect.popen(); c_pkgsession.doper := trunc(sysdate);
    T_PKGDEAPRM.pSetPrmId(idDea => C_ID, idDep => C_DEP_ID, idPar => 12996, p_value => gosProg);
    COMMIT;
  EXCEPTION
    WHEN OTHERS THEN
      raise_application_error(-20000,'Произошла ошибка - '||SQLCODE||' -ERROR- '||SQLERRM);
  END; --Принадлежность к государственным программам
  BEGIN
    c_pkgconnect.popen(); c_pkgsession.doper := trunc(sysdate);
    T_PKGDEAPRM.pSetPrmId(idDea => C_ID, idDep => C_DEP_ID, idPar => 12998, p_value => subsidizing);
    COMMIT;
  EXCEPTION
    WHEN OTHERS THEN
      raise_application_error(-20000,'Произошла ошибка - '||SQLCODE||' -ERROR- '||SQLERRM);
  END; --Субсидирование
  BEGIN
    c_pkgconnect.popen(); c_pkgsession.doper := trunc(sysdate);
    T_PKGDEAPRM.pSetPrmId(idDea => C_ID, idDep => C_DEP_ID, idPar => 12999, p_value => womBusinessSub);
    COMMIT;
  EXCEPTION
    WHEN OTHERS THEN
      raise_application_error(-20000,'Произошла ошибка - '||SQLCODE||' -ERROR- '||SQLERRM);
  END; --Субъект женского предпринимательства
  BEGIN
    c_pkgconnect.popen(); c_pkgsession.doper := trunc(sysdate);
    T_PKGDEAPRM.pSetPrmId(idDea => C_ID, idDep => C_DEP_ID, idPar => 18443, p_value => credAdminManager);
    COMMIT;
  EXCEPTION
    WHEN OTHERS THEN
      raise_application_error(-20000,'Произошла ошибка - '||SQLCODE||' -ERROR- '||SQLERRM);
  END; --Менеджер НПК БК
  BEGIN
    c_pkgconnect.popen(); c_pkgsession.doper := trunc(sysdate);
    T_PKGDEAPRM.pSetPrmId(idDea => C_ID, idDep => C_DEP_ID, idPar => 10194, p_value => '1');
    COMMIT;
  EXCEPTION
    WHEN OTHERS THEN
      raise_application_error(-20000,'Произошла ошибка - '||SQLCODE||' -ERROR- '||SQLERRM);
  END; --Признак для отключения проверки оприходования документов
  BEGIN
    c_pkgconnect.popen(); c_pkgsession.doper := trunc(sysdate);
    T_PKGDEAPRM.pSetPrmId(idDea => C_ID, idDep => C_DEP_ID, idPar => 10193, p_value => '1');
    COMMIT;
  EXCEPTION
    WHEN OTHERS THEN
      raise_application_error(-20000,'Произошла ошибка - '||SQLCODE||' -ERROR- '||SQLERRM);
  END; --Признак для отключения контроля по обеспеченности
  BEGIN
    c_pkgconnect.popen(); c_pkgsession.doper := trunc(sysdate);
    T_PKGDEAPRM.pSetPrmId(idDea => C_ID, idDep => C_DEP_ID, idPar => 9771, p_value => '1');
    COMMIT;
  EXCEPTION
    WHEN OTHERS THEN
      raise_application_error(-20000,'Произошла ошибка - '||SQLCODE||' -ERROR- '||SQLERRM);
  END; --Признак для отключения контроля просроченной задолженности
  BEGIN
    c_pkgconnect.popen(); c_pkgsession.doper := trunc(sysdate);
    T_PKGDEAPRM.pSetPrmId(idDea => C_ID, idDep => C_DEP_ID, idPar => 10207, p_value => '1');
    COMMIT;
  EXCEPTION
    WHEN OTHERS THEN
      raise_application_error(-20000,'Произошла ошибка - '||SQLCODE||' -ERROR- '||SQLERRM);
  END; --Признак для отключения проверки взимания комиссии Z_077_CL100_001
  BEGIN
    c_pkgconnect.popen(); c_pkgsession.doper := trunc(sysdate);
    T_PKGDEAPRM.pSetPrmId(idDea => C_ID, idDep => C_DEP_ID, idPar => 10205, p_value => '1');
    COMMIT;
  EXCEPTION
    WHEN OTHERS THEN
      raise_application_error(-20000,'Произошла ошибка - '||SQLCODE||' -ERROR- '||SQLERRM);
  END; --Признак для отключения проверки взимания комиссии Z_077_CL100_002
  /* Добавляем номер счета в договор */
  BEGIN
    c_pkgconnect.popen(); c_pkgsession.doper := trunc(sysdate);
    Z_PKG_AUTO_TEST.AT_pSavePayAccAtr(DocNumCredit => codeContract);
    COMMIT;
  EXCEPTION
    WHEN OTHERS THEN
      raise_application_error(-20000,'Произошла ошибка - '||SQLCODE||' -ERROR- '||SQLERRM);
  END;
END;
PROCEDURE AT_pCheckIinCli(IIN varchar2)
IS
errNum integer;
BEGIN
  errNum := G_PKGCLI.fChk_BINIIN(sTEXT => IIN, sExpCheckBIN => null);
  dbms_output.put_line('1out -> ' || errNum);
END;
PROCEDURE AT_pRegSksContract(DocNum varchar2)
  IS
  BEGIN
    AT_pRunOperWOParams(CODE_NUM => DocNum, -- Номер документа по которому необходимо выполнить операцию
                        OPER_CODE => 'REG_DO'); -- Код операции. Можно посмотреть в DOP1
  END;
PROCEDURE AT_pUndoBlockLimit(idLim number)
  IS
  OutOper varchar2(2000);
  statLim varchar2(100);
  BEGIN
    /* Получаем статус лимита */
    statLim := AT_fGetStatusLimit(idLim => idLim);
    IF statLim = 'Блокирован' THEN
        c_pkgconnect.popen(); c_pkgsession.doper := trunc(sysdate);
        T_PKGRUNOPRUTL.pRunOperation(  nDepId => 1388 -- Подразделение документа
                                       , nId => idLim     -- Идентификатор документа
                                       , sOperCode  => 'UNDO_BLOCK'  -- Код операции
                                       , sInOperParams  => null          -- Входные параметры операции
                                       , sOutOperParams => OutOper          -- Выходные параметры операции
                                       , nRaise  => 1 );         -- Генерировать ли исключение ( 0, null-нет )
        COMMIT;
    ELSE
        dbms_output.put_line('Лимит не блокирован. Статус лимита - ' || statLim);
    END IF;
  END;
PROCEDURE AT_pSetSwiftPath(PathValue varchar2)
  IS
  BEGIN
    c_pkgconnect.popen(); c_pkgsession.doper := trunc(sysdate);
	EXECUTE IMMEDIATE 'ALTER TRIGGER COLVIR.SCMCHECK_BS DISABLE';
	EXECUTE IMMEDIATE 'ALTER TRIGGER COLVIR.C_PRMVAL_PRMVAL_SCM DISABLE';
    C_PKGPRM.pSetPrm(sCode => 'SWIFTOUT',
                     sValue => PathValue);
    COMMIT;
	EXECUTE IMMEDIATE 'ALTER TRIGGER COLVIR.C_PRMVAL_PRMVAL_SCM ENABLE';
	EXECUTE IMMEDIATE 'ALTER TRIGGER COLVIR.SCMCHECK_BS ENABLE';
  END;


FUNCTION AT_fGetIdDep(DepCode varchar2) RETURN number
  IS
  IdDep number;
  BEGIN
    SELECT ID INTO IdDep FROM c_dep_std
    WHERE CODE = DepCode;
    RETURN IdDep;
  EXCEPTION
    WHEN OTHERS THEN
      RETURN(0);
      Raise_Application_Error(-20000,
                              Localize('Ошибка при получений ID департамента',
                                       'Z_PKG_AUTO_TEST',
                                       'AT_fGetIdDep'));
  END;
FUNCTION AT_fGetIDSksDoc(DocNum varchar2) RETURN NUMBER
  IS
  IdDoc number;
  BEGIN
    SELECT ID INTO IdDoc from T_ORD
    WHERE CODE = DocNum;
    RETURN IdDoc;
  EXCEPTION
    WHEN OTHERS THEN
      RETURN(0);
      Raise_Application_Error(-20000,
                              Localize('Ошибка при получений ID договора СКС',
                                       'Z_PKG_AUTO_TEST',
                                       'AT_fGetIDSksDoc'));
  END;
FUNCTION AT_fGetDocNumCreditLine(CliCode varchar2, DocNumSks varchar2) RETURN VARCHAR2
  IS
    idSks number;
    DocNumLine varchar2(100);
  BEGIN
    idSks := AT_fGetIDSksDoc(DocNumSks);
    SELECT o.CODE INTO DocNumLine
    FROM T_DEA d, G_CLI g, G_CLIHST ch, T_ORD o, L_LDEA l
    WHERE o.DEP_ID = d.DEP_ID and o.ID = d.ID
    and ch.DEP_ID = d.CLI_DEP_ID and ch.ID = d.CLI_ID and ch.dep_id = g.dep_id and ch.id = g.id
    and l.DEP_ID = d.DEP_ID and l.ID = d.ID
    and g.code = CliCode and d.DEA_ID = idSks;
    RETURN DocNumLine;
  EXCEPTION
    WHEN OTHERS THEN
      RETURN(0);
      Raise_Application_Error(-20000,
                              Localize('Ошибка при получении номера договора кредитной линии',
                                       'Z_PKG_AUTO_TEST',
                                       'AT_fGetDocNumCreditLine'));
  END;
FUNCTION AT_fGetCardAccNum(CliCode varchar2) RETURN VARCHAR2
  IS
  AccNum varchar2(100);
  BEGIN
    SELECT a.code
    INTO accNum
    FROM C_DEP D,
      T_BOP_STAT ST, T_PROCESS PR, T_PROCMEM PM,
      LEDACC L,
      T_VAL_STD V,
      T_ACCGRP G,
      G_ACCBLNHST AH,
      G_ACCBLN A,
      G_CLI CLI
    WHERE A.DEP_ID=D.ID
      and A.CHA_ID=L.ID
      and V.ID(+)=AH.VAL_ID
      and AH.DEP_ID=G.DEP_ID and AH.AUT_ID=G.ID
      and A.ID=AH.ID and A.DEP_ID=AH.DEP_ID and P_OPERDAY between AH.FROMDATE and AH.TODATE
      and PM.ORD_ID = A.ORD_ID
      and PM.DEP_ID = A.DEP_ID
      and PM.MAINFL = '1'
      and AH.CLIDEP_ID = CLI.DEP_ID
      and AH.CLI_ID = CLI.ID
      and PR.ID = PM.ID
      and ST.ID = PR.BOP_ID
      and ST.NORD = PR.NSTAT
      and CLI.code = CliCode and st.CODE = 'OPENED' and a.capfl = 1;
    RETURN AccNum;
  EXCEPTION
    WHEN OTHERS THEN
      RETURN(0);
      Raise_Application_Error(-20000,
                              Localize('Ошибка при получении номера счета',
                                       'Z_PKG_AUTO_TEST',
                                       'AT_fGetCardAccNum'));
  END;
FUNCTION AT_fGetStatusLimit(idLim number) RETURN VARCHAR2
  IS
    status varchar2(100);
  BEGIN
    select s.longname INTO status from
           T_ORD o, T_BOP_STAT s, T_PROCESS p, T_PROCMEM m, Q_LIM_RSKDSC r, T_ANCHART a, Q_LIMDSC d, Q_LIM l
    where d.CHA_ID = l.LIMDSC_ID
      and a.ID = d.CHA_ID
      and r.ID = d.RSKDSC_ID
      and o.DEP_ID = l.DEP_ID and o.ID = l.ID
      and o.DEP_ID=m.DEP_ID and o.ID=m.ORD_ID and m.MAINFL='1'
      and p.ID=m.ID
      and s.ID=p.BOP_ID and s.NORD=p.NSTAT and l.id = idLim;
      RETURN status;
  EXCEPTION
    WHEN OTHERS THEN
      RETURN(0);
      Raise_Application_Error(-20000,
                              Localize('Ошибка при получении статуса лимита',
                                       'Z_PKG_AUTO_TEST',
                                       'AT_fGetStatusLimit'));
  END;
FUNCTION AT_fGetIdDoc(docNum varchar2) RETURN NUMBER
  /* Процедура для получения id документа из t_ord */
  IS
    docID number;
  BEGIN
    SELECT ID INTO docID FROM t_ord t
    WHERE t.code = docNum;
    RETURN docID;
  EXCEPTION
    WHEN OTHERS THEN
      RETURN(0);
      Raise_Application_Error(-20000,
                              Localize('Ошибка при получении id карточного договора',
                                       'Z_PKG_AUTO_TEST',
                                       'AT_fGetIdCardDoc'));
  END;
  FUNCTION AT_fGetDepIdDoc(docNum varchar2) RETURN NUMBER
  /* Процедура для получения dep_id документа из t_ord */
  IS
    depID number;
  BEGIN
    SELECT DEP_ID INTO depID FROM t_ord t
    WHERE t.code = docNum;
    RETURN depID;
  EXCEPTION
    WHEN OTHERS THEN
      RETURN(0);
      Raise_Application_Error(-20000,
                              Localize('Ошибка при получении dep_id карточного договора',
                                       'Z_PKG_AUTO_TEST',
                                       'AT_fGetDepIdCardDoc'));
  END;
PROCEDURE AT_pSendToAcceptCard(docNum varchar2)
  IS
    docId number;
    docDepId number;
  BEGIN
    /* выполняем операцию отправкина акцептование */
    AT_pRunOperWOParams(CODE_NUM => docNum,
                        OPER_CODE => 'SNDACCEPT_DO');

  EXCEPTION
    WHEN OTHERS THEN
        Raise_Application_Error(-20000,
                                  Localize('Ошибка при отправке на акцептование',
                                           'Z_PKG_AUTO_TEST',
                                           'AT_pSendToAcceptCard'));
  END;
PROCEDURE AT_pMakeDecisionAcceptCard(docNum varchar2)
  IS
  BEGIN
    /* выполняем операцию отправкина акцептование */
    AT_pRunOperWithParams(docNum => docNum, operCode => 'ANSACCEPT', params => 'ACCPT_DECISION => ACCEPTED');
  END;
FUNCTION AT_fGetIdBpsAcc(codeBps varchar2) RETURN NUMBER
  IS
  idBps number;
  BEGIN
    SELECT ID INTO idBps FROM T_ACCHART_STD
    WHERE CODE = codeBps;
    RETURN idBps;
  EXCEPTION
    WHEN OTHERS THEN
      RETURN(0);
      Raise_Application_Error(-20000,
                              Localize('Ошибка при получении id БПС',
                                       'Z_PKG_AUTO_TEST',
                                       'AT_fGetIdBpsAcc'));
  END;
FUNCTION AT_fGetIdPsAcc(codePs varchar2) RETURN NUMBER
  IS
    idPs number;
  BEGIN
    SELECT ID INTO idPs FROM GV_LEDACC_LIST
    WHERE CODE = codePs;
    RETURN idPs;
  EXCEPTION
    WHEN OTHERS THEN
      RETURN(0);
      Raise_Application_Error(-20000,
                              Localize('Ошибка при получении id ПС',
                                       'Z_PKG_AUTO_TEST',
                                       'AT_pGetIdPsAcc'));
  END;
PROCEDURE AT_pAddBpsToPs(psCode varchar2, bpsCode varchar2)
  IS
  idBps number;
  idPs number;
  BEGIN
    idBps := AT_fGetIdBpsAcc(codeBps => bpsCode);
    idPs := AT_fGetIdPsAcc(codePs => psCode);
    G_PKGDBLGL.pLink_LedAcc(idBase => idBps, idCha => idPs, pReason => null);
    COMMIT;
  EXCEPTION
    WHEN DUP_VAL_ON_INDEX THEN
      dbms_output.put_line(bpsCode || ' - уже существует для ПС ' || psCode);
  END;
FUNCTION AT_fGetIdAgr(DocNumCredit varchar2) RETURN NUMBER
  IS
  idAgr number;
  BEGIN
    SELECT a.ID INTO idAgr FROM T_ORD o, T_ADDAGR a
    WHERE o.DEP_ID = a.DEP_ID and o.ID = a.dea_id
          and o.CODE = DocNumCredit;
    RETURN idAgr;
  EXCEPTION
    WHEN OTHERS THEN
      RETURN(0);
      Raise_Application_Error(-20000,
                              Localize('Ошибка при получении id доп. соглашения',
                                       'Z_PKG_AUTO_TEST',
                                       'AT_fGetIdAgr'));
  END;
FUNCTION AT_fGetDepIdAgr(DocNumCredit varchar2) RETURN NUMBER
  IS
  depIdAgr number;
  BEGIN
    SELECT a.DEP_ID INTO depIdAgr FROM T_ORD o, T_ADDAGR a
    WHERE o.DEP_ID = a.DEP_ID and o.ID = a.dea_id
          and o.CODE = DocNumCredit;
    RETURN depIdAgr;
  EXCEPTION
    WHEN OTHERS THEN
      RETURN(0);
      Raise_Application_Error(-20000,
                              Localize('Ошибка при получении dep_id доп. соглашения',
                                       'Z_PKG_AUTO_TEST',
                                       'AT_fGetDepIdAgr'));
  END;
PROCEDURE AT_pUndoAllOperAgr(DocNumCredit varchar2)
  IS
  idAgr number;
  depIdAgr number;
  OutOper varchar2(1000);
  BEGIN
    idAgr := AT_fGetIdAgr(DocNumCredit => DocNumCredit);
    depIdAgr := AT_fGetDepIdAgr(DocNumCredit => DocNumCredit);
    c_pkgconnect.popen(); c_pkgsession.doper := trunc(sysdate);
    T_PKGRUNOPRUTL.pRunOperation(  nDepId => depIdAgr -- Подразделение документа
                                     , nId => idAgr     -- Идентификатор документа
                                     , sOperCode  => 'UNDOALL'  -- Код операции
                                     , sInOperParams  => null          -- Входные параметры операции
                                     , sOutOperParams => OutOper          -- Выходные параметры операции
                                     , nRaise  => 1 );         -- Генерировать ли исключение ( 0, null-нет )
  END;
PROCEDURE AT_pDelAgr(DocNumCredit varchar2)
  IS
  idAgr number;
  depIdAgr number;
  BEGIN
    AT_pUndoAllOperAgr(DocNumCredit => DocNumCredit);
    COMMIT;
    idAgr := AT_fGetIdAgr(DocNumCredit => DocNumCredit);
    depIdAgr := AT_fGetDepIdAgr(DocNumCredit => DocNumCredit);
    T_BSADDAGR.PDEL(NDEP_ID => depIdAgr, NID => idAgr);
    COMMIT;
  EXCEPTION
    WHEN OTHERS THEN
        idAgr := AT_fGetIdAgr(DocNumCredit => DocNumCredit);
        depIdAgr := AT_fGetDepIdAgr(DocNumCredit => DocNumCredit);
        T_BSADDAGR.PDEL(NDEP_ID => depIdAgr, NID => idAgr);
        COMMIT;
  END;
PROCEDURE AT_pCreateSordpayDoc(docNum varchar2, typeDoc varchar2, typeOper varchar2, amount varchar2, valCode varchar2,
                               codeAccSend varchar2, BikSend varchar2, codeAccGet varchar2, IinGet varchar2,
                               fioHead varchar2, fioGet varchar2, knp varchar2, codeSend varchar2,
                               codeGet varchar2)
IS
NID number;
BEGIN
  c_pkgconnect.popen(); c_pkgsession.doper := trunc(sysdate);
  S_BSPAY.PSAVE(NDEP_ID => 1388,
                NID => NID,
                SKSO_CODE => typeDoc, -- тип документа
                SCHA_CODE => typeOper, -- тип операции
                SAMOUNT => amount, -- сумма платежа
                SVAL_CODE => valCode, -- код валюты
                DDORD => trunc(sysdate),
                DDVAL => trunc(sysdate),
                SCODE_ACL => codeAccSend, -- счет отправителя
                SCODE_BCR => BikSend, -- БИК отправителя
                SCODE_ACR => codeAccGet, -- код получателя
                SRNN_CR => IinGet, -- ИИН получателя
                SCODE_BC => null,
                STXT_HEAD => fioHead, -- ФИО руководителя для подписи
                STXT_BUCH => null,
                STXT_DSCR => 'Autotest',
                STXT_BEN => fioGet, -- ФИО получателя
                NNOCMSFL => 0,
                SKNP => knp, -- КНП
                SCODE_OD => codeSend, -- КОд отправителя
                SCODE_BE => codeGet, -- КБе получателя
                SPRIM => null,
                SCODE => docNum, -- номер документа
                NFLZO => 0,
                IVRFFL => NULL,
                SLIMFL => NULL,
                IPRINTFL => NULL,
                SSPEEDFL => NULL,
                SSOST => NULL,
                SREFER => NULL,
                P_PARENTPROC => NULL,
                P_PARENTOPR => NULL,
                SRNNCLI => NULL,
                STXTPAY => NULL,
                SVOPER => NULL,
                SGCVP => NULL,
                SPERIOD => NULL,
                NDEA_DEP_ID => NULL,
                NDEA_ID => NULL,
                NDEPUSR_ID => NULL,
                SCARD => NULL,
                SORIGINACC => NULL,
                SCMS_STRG => '0',
                SCLIACCCOM => NULL,
                AF_FL => 0,
                SOPV => NULL,
                P_NOLIMIT => NULL);
  COMMIT;
END;
PROCEDURE AT_pOperPaySordpay(depId number, id number)
  IS
  OutOper varchar2(2000);
  BEGIN
    c_pkgconnect.popen(); c_pkgsession.doper := trunc(sysdate);
    T_PKGRUNOPRUTL.pRunOperation(  nDepId => depId -- Подразделение документа
                                   , nId => id     -- Идентификатор документа
                                   , sOperCode  => 'PAY'  -- Код операции
                                   , sInOperParams  => null          -- Входные параметры операции
                                   , sOutOperParams => OutOper          -- Выходные параметры операции
                                   , nRaise  => 1 );         -- Генерировать ли исключение ( 0, null-нет )
      COMMIT;
  END;
PROCEDURE AT_pReplAccSordpay(docNum varchar2, typeDoc varchar2, typeOper varchar2, amount varchar2, valCode varchar2,
                             codeAccSend varchar2, BikSend varchar2, codeAccGet varchar2, IinGet varchar2,
                             fioHead varchar2, fioGet varchar2, knp varchar2, codeSend varchar2,
                             codeGet varchar2)
  IS
    docId number;
  BEGIN
    /* создаем расчетный документ в SORDPAY */
    Z_PKG_AUTO_TEST.AT_pCreateSordpayDoc(docNum => docNum, typeDoc => typeDoc, typeOper => typeOper, amount => amount,
                         valCode => valCode, codeAccSend => codeAccSend, BikSend => BikSend,
                         codeAccGet => codeAccGet, IinGet => IinGet, fioHead => fioHead, fioGet => fioGet,
                         knp => knp, codeSend => codeSend, codeGet => codeGet);
    /* получаем id созданного документа */
    docId := Z_PKG_AUTO_TEST.AT_fGetIdDoc(docNum => docNum);
    /* выполняем операцию Оплатить по документу в SORDPAY */
    Z_PKG_AUTO_TEST.AT_pOperPaySordpay(depId => 1388, id => docId);
  END;
  PROCEDURE AT_pSetFutureOperDayToJrn(countDays number) -- количество дней от сегодняшней даты до будущей
    IS
    dOper date;
    BEGIN
      SELECT DOPER INTO dOper FROM C_CLDDEP WHERE DOPER >= TRUNC(SYSDATE) AND DEP_ID = 1388
                    and doper = trunc(sysdate) + countDays;
    EXCEPTION
      WHEN NO_DATA_FOUND THEN
        INSERT INTO C_CLDDEP VALUES (trunc(sysdate) + countDays, 1388, 1, 0, 0, '', '', '', '', '', '');
        COMMIT;
    END;

  FUNCTION AT_fGetPayDateFromGraph(DocNum varchar2) RETURN varchar2 IS
   datePay VARCHAR2(20);
   payDate DATE;
   depId NUMBER;
   ordId NUMBER;
   BEGIN
   depId := Z_PKG_AUTO_TEST.AT_fGetDepIdDoc(DocNum);
   ordId := Z_PKG_AUTO_TEST.AT_fGetIdDoc(DocNum);
   select s.doper INTO payDate from T_ORD t, T_DEASHDPNT s,  T_ARLCLC c, T_ARLDEA ad, T_ARLDSC d
   where s.ord_id = t.id and s.dep_id = t.dep_id and s.CLC_ID = c.ID and c.ARL_ID = d.ID
   and ad.DEP_ID = s.DEP_ID and ad.ORD_ID = s.ORD_ID and ad.CLC_ID = c.ID and t.dep_id=depId and t.id=ordId
   and d.longname='Основной долг' and s.doper >= trunc(sysdate) and rownum < 2
   order by s.doper;
   datePay := TO_CHAR(payDate, 'dd.mm.yy');
   RETURN datePay;
   EXCEPTION
   WHEN OTHERS THEN
     RETURN(0);
       RAISE_APPLICATION_ERROR(-20000,
           Localize('Ошибка при получении даты платежа', 'Z_PKG_AUTO_TEST', 'AT_fGetPayDateFromGraph'));
  END;
  PROCEDURE AT_pUpdAutoFlPsAcc(psAcc varchar2, -- код ПС счета
                               flagAtoFl varchar2) -- Признак автооткрытия (1 - установить автооткрытие
                                           --                       0 - снять признак
    IS
    BEGIN
      UPDATE ledacc
      SET AUTOFL = flagAtoFl
      WHERE code = psAcc;
      COMMIT;
    EXCEPTION
      WHEN OTHERS THEN
        RAISE_APPLICATION_ERROR(-20000,
           Localize('Ошибка при обновлении признака автооткрытия счета', 'Z_PKG_AUTO_TEST', 'AT_pUpdAutoFlPsAcc'));
    END;
  FUNCTION AT_fGetAdminManager RETURN varchar2 IS
   manager VARCHAR2(100);
    BEGIN
       SELECT DISTINCT CH.LONGNAME INTO manager
       FROM G_CLI C, G_CLIHST CH, A_RESPRS R, A_RESPRSHST RH, H_HISTORY H
       WHERE C.ID = CH.ID AND C.DEP_ID = CH.DEP_ID AND R.CLI_ID = C.ID
       AND R.CLI_DEP_ID = C.DEP_ID AND RH.ID = R.ID AND R.ID=H.PER_ID(+)
       AND trunc(sysdate) between H.FROMDATE(+) AND H.TODATE(+) AND H.MAINFL(+)=1
       AND trunc(sysdate) between RH.FROMDATE and RH.TODATE
       AND trunc(sysdate) between CH.FROMDATE and CH.TODATE
       AND G_PKGCLIROLE.fIsSetRole(c.DEP_ID, c.ID, 'RES') = 1 AND CH.DEP_ID = '1411'
       AND RH.ARCFL = '0' AND ROWNUM < 2;
       RETURN manager;
     EXCEPTION
       WHEN NO_DATA_FOUND THEN
          RETURN 'Менеджер не найден';
       WHEN OTHERS THEN
       raise_application_error(-20000,'Произошла ошибка - '||SQLCODE||' -ERROR- '||SQLERRM);
    END;
  PROCEDURE AT_pSetUnlockAccWU(numAcc varchar2) IS

   BEGIN
     update M_MTS_ACCOUNT_STD T
     set T.LOCKFL = '0'
     where T.CODE = numAcc;
     COMMIT;
   EXCEPTION
     WHEN OTHERS THEN
         RAISE_APPLICATION_ERROR(-20000,
         Localize('Ошибка при снятии блокировки', 'Z_PKG_AUTO_TEST', 'AT_pSetUnlockAccWU'));
   END;
   PROCEDURE AT_pRenameAccauntWU(codeAcc varchar2)
     IS
       codeWU varchar2(30);
       numAcc number;
       numCode varchar2(30);
       numSubAcc number;
       numSubCode varchar2(30);
     BEGIN
       SELECT CODE INTO codeWU FROM M_MTS_ACCOUNT_STD t
       WHERE t.code = codeAcc;
       SELECT round(dbms_random.value(111111,999999)) INTO numAcc from dual;
       SELECT round(dbms_random.value(111111,999999)) INTO numSubAcc from dual;
       numCode := 'ARC' || TO_CHAR(numAcc, 'FM999999');
       numSubCode := TO_CHAR(numSubAcc, 'FM999999');
       UPDATE M_MTS_ACCOUNT_STD t
       SET t.CODE = numCode, t.LONGNAME = numSubCode
       WHERE t.CODE = codeAcc;
       COMMIT;
     EXCEPTION
       WHEN NO_DATA_FOUND THEN
         RETURN;
     END;
  /* Функция для получения cli_id */
  FUNCTION AT_fGetCliId(DocNumCredit varchar2) RETURN varchar2
    IS
      idDoc number;
      depIdDoc number;
      cliIdDoc number;
    BEGIN
      idDoc := Z_PKG_AUTO_TEST.AT_fGetIdDoc(DocNumCredit);
      depIdDoc := Z_PKG_AUTO_TEST.AT_fGetDepIdDoc(DocNumCredit);
      SELECT CLI_ID INTO cliIdDoc
        FROM T_DEA
        WHERE ID = idDoc AND DEP_ID = depIdDoc;
      dbms_output.put_line(cliIdDoc);
      RETURN cliIdDoc;
    EXCEPTION
         WHEN OTHERS THEN
         raise_application_error(-20000,'Произошла ошибка - '||SQLCODE||' -ERROR- '||SQLERRM);
      END;
  /* Функция для получения cli_dep_id */
  FUNCTION AT_fGetCliDepId(DocNumCredit varchar2) RETURN varchar2
  IS
    idDoc number;
    depIdDoc number;
    cliDepIdDoc number;
  BEGIN
    idDoc := Z_PKG_AUTO_TEST.AT_fGetIdDoc(DocNumCredit);
    depIdDoc := Z_PKG_AUTO_TEST.AT_fGetDepIdDoc(DocNumCredit);
    SELECT CLI_DEP_ID INTO cliDepIdDoc
      FROM T_DEA
      WHERE ID = idDoc AND DEP_ID = depIdDoc;
    dbms_output.put_line(cliDepIdDoc);
    RETURN cliDepIdDoc;
  EXCEPTION
       WHEN OTHERS THEN
       raise_application_error(-20000,'Произошла ошибка - '||SQLCODE||' -ERROR- '||SQLERRM);
    END;
  /* Функция для получения номера счета по договору */
  FUNCTION AT_fGetPayAccNum(DocNumCredit varchar2) RETURN varchar2
  IS
    cliId number;
    cliDepId number;
    payAccNum varchar2(50);
  BEGIN
    cliId := AT_fGetCliId(DocNumCredit);
    cliDepId := AT_fGetCliDepId(DocNumCredit);
    SELECT CODE INTO payAccNum
      FROM GV_ACCBLNUPD
      WHERE CLI_ID = cliId AND CLIDEP_ID = cliDepId;
    RETURN payAccNum;
  EXCEPTION
       WHEN OTHERS THEN
       raise_application_error(-20000,'Произошла ошибка - '||SQLCODE||' -ERROR- '||SQLERRM);
    END;
  /* Процедура по сохранению номера счета в договоре */
  PROCEDURE AT_pSavePayAccAtr(DocNumCredit varchar2)
  IS
  idDoc number;
  depIdDoc number;
  inord number;
  cTypCode varchar(20) := 'CLIACC';
  cDepCode varchar(20) := 'CNT';
  accNum varchar(50);
  BEGIN
    idDoc := Z_PKG_AUTO_TEST.AT_fGetIdDoc(DocNumCredit);
    depIdDoc := Z_PKG_AUTO_TEST.AT_fGetDepIdDoc(DocNumCredit);
    accNum := AT_fGetPayAccNum(DocNumCredit);
    T_PKGPAYATR.pSaveDeaPayAtr(idDep => depIdDoc, idDea => idDoc, iNord => inord, cTyp_code => cTypCode, cCODE_ACC => accNum, cDEP_CODE => cDepCode);
  COMMIT;
  EXCEPTION
       WHEN OTHERS THEN
       raise_application_error(-20000,'Произошла ошибка - '||SQLCODE||' -ERROR- '||SQLERRM);
    END;
  /* Функция по получению сейфового ячейки */
  FUNCTION AT_fGetFreeSafeBox(depcode varchar2, storage varchar2) RETURN varchar2
    IS
   safeBox varchar2(50);
  BEGIN
   select ss.code into safeBox from S_SAFEORD so,S_SAFE ss, S_SAFEVAULT sv, C_DEP dep where so.safe_id(+) = ss.id
   and ss.vault_id = sv.id and so.dep_id(+) = dep.id and sv.code = storage
   and ss.INUSEFL = 0 and ss.arcfl = 0 and dep.code = depcode and so.id is null and rownum < 2;
    RETURN safeBox;
  EXCEPTION
       WHEN OTHERS THEN
       raise_application_error(-20000,'Произошла ошибка - '||SQLCODE||' -ERROR- '||SQLERRM);
    END;
  PROCEDURE AT_pSetValRate(valId number, rate number, dateVal varchar2)
    IS
    BEGIN
      c_pkgconnect.popen(); c_pkgsession.doper := trunc(sysdate);
      T_PKGVAL.pSaveRate (
        P_Val_ID => valId                 -- валюта
      , P_Rat_Id => 1                 -- тип курса
      , P_ValBas_Id => 1 -- опорная валюта
      , P_dOp => dateVal     -- дата курса
      , P_nRate => rate                      -- значение курса
      , P_Multi => 0
      );
      COMMIT;
    END;
  /* Функция по получению иин клиента */
  FUNCTION AT_fGetIinCli(cliCode varchar2) RETURN varchar2
  IS
   iinCli varchar2(50);
  BEGIN
   select distinct gh.taxcode into iinCli from G_CLI g, G_CLIHST gh
   where g.DEP_ID = gh.DEP_ID and g.ID = gh.ID and g.CODE = cliCode;
    RETURN iinCli;
  EXCEPTION
       WHEN OTHERS THEN
       raise_application_error(-20000,'Произошла ошибка - '||SQLCODE||' -ERROR- '||SQLERRM);
    END;
  /* Функция по получению номера документа */
  FUNCTION AT_fGetCashOrderCode(cliCode varchar2) RETURN varchar2
  IS
   cashOrderCode varchar2(50);
   iinCli varchar2(50);
  BEGIN
   iinCli := AT_fGetIinCli(cliCode => cliCode);
   select o.code into cashOrderCode from T_ORD o, S_ORDCASH ch
   where o.DEP_ID = ch.DEP_ID and o.ID = ch.ID
         and ch.TXT_TAXCODE = iinCli;
    RETURN cashOrderCode;
  EXCEPTION
       WHEN OTHERS THEN
       raise_application_error(-20000,'Произошла ошибка - '||SQLCODE||' -ERROR- '||SQLERRM);
    END;
  /* Функция возвращающая id кассы */
  FUNCTION AT_fGetCashId(cashCode varchar2) RETURN NUMBER
    IS
      cashId number;
    BEGIN
      SELECT ID INTO cashId FROM M_CSHDSC op
      WHERE op.CODE = cashCode;
      RETURN cashId;
    EXCEPTION
       WHEN OTHERS THEN
       raise_application_error(-20000,'Произошла ошибка - '||SQLCODE||' -ERROR- '||SQLERRM);
    END;
  /* Функция возвращающая dep_id кассы */
  FUNCTION AT_fGetCashDepId(cashCode varchar2) RETURN NUMBER
    IS
      cashDepId number;
    BEGIN
      SELECT DEP_ID INTO cashDepId FROM M_CSHDSC op
      WHERE op.CODE = cashCode;
      RETURN cashDepId;
    EXCEPTION
       WHEN OTHERS THEN
       raise_application_error(-20000,'Произошла ошибка - '||SQLCODE||' -ERROR- '||SQLERRM);
  END;
  /* Функция для получения id позиции по имени поизиции юзера */
  FUNCTION AT_fGetIdUserPosition(userCode varchar2) RETURN NUMBER
    IS
      idUser number;
    BEGIN
      SELECT id INTO idUser FROM c_usr
      WHERE code LIKE userCode || '%';
      RETURN idUser;
    EXCEPTION
       WHEN OTHERS THEN
       raise_application_error(-20000,'Произошла ошибка - '||SQLCODE||' -ERROR- '||SQLERRM);
    END;
  /* Процедура для добавления тестового юзера в кассиры */
  PROCEDURE AT_pAddTestUserToCash(userName varchar2, codeCash varchar2)
  IS
    idCash number;
    idUser number;
    depIdCash number;
  BEGIN
    idCash := AT_fGetCashId(cashCode => codeCash);
    idUser := AT_fGetIdUserPosition(userCode => userName);
    depIdCash := AT_fGetCashDepId(cashCode => codeCash);
    INSERT INTO M_CSHUSR(ID, CSH_ID, CSHTYPE)
    VALUES(idUser, idCash, 1);
    COMMIT;
  EXCEPTION
       WHEN OTHERS THEN
         raise_application_error(-20000,'Произошла ошибка - '||SQLCODE||' -ERROR- '||SQLERRM);
  END;
  /* Процедура для добавления тестового юзера в кассиры по id позиции юзера */
  PROCEDURE AT_pAddTestUserIdToCash(userPosId number, codeCash varchar2)
  IS
    idCash number;
    depIdCash number;
  BEGIN
    idCash := AT_fGetCashId(cashCode => codeCash);
    depIdCash := AT_fGetCashDepId(cashCode => codeCash);
    INSERT INTO M_CSHUSR(ID, CSH_ID, CSHTYPE)
    VALUES(userPosId, idCash, 1);
    COMMIT;
  EXCEPTION
       WHEN DUP_VAL_ON_INDEX THEN
         RETURN;
       WHEN OTHERS THEN
       raise_application_error(-20000,'Произошла ошибка - '||SQLCODE||' -ERROR- '||SQLERRM);
  END;
  /* Процедура для апдейта активнго юзера кассы */
  PROCEDURE AT_pUpdUserCash(userName varchar2, codeCash varchar2)
    IS
      idCash number;
      idUser number;
    BEGIN
      idCash := AT_fGetCashId(cashCode => codeCash);
      idUser := AT_fGetIdUserPosition(userCode => userName);
      EXECUTE IMMEDIATE 'ALTER TRIGGER COLVIR.M_CSHDSC_DTU DISABLE';
      UPDATE M_CSHDSC
      SET US_ID = idUser
      WHERE ID = idCash;
      COMMIT;
      EXECUTE IMMEDIATE 'ALTER TRIGGER COLVIR.M_CSHDSC_DTU ENABLE';
    EXCEPTION
       WHEN OTHERS THEN
       raise_application_error(-20000,'Произошла ошибка - '||SQLCODE||' -ERROR- '||SQLERRM);
    END;
  /* Процедура для апдейта активнго юзера кассы по id позиции юзера*/
  PROCEDURE AT_pUpdUserIdCash(userPosId number, codeCash varchar2)
    IS
      idCash number;
    BEGIN
      idCash := AT_fGetCashId(cashCode => codeCash);
      EXECUTE IMMEDIATE 'ALTER TRIGGER COLVIR.M_CSHDSC_DTU DISABLE';
      UPDATE M_CSHDSC
      SET US_ID = userPosId
      WHERE ID = idCash;
      COMMIT;
      EXECUTE IMMEDIATE 'ALTER TRIGGER COLVIR.M_CSHDSC_DTU ENABLE';
    EXCEPTION
       WHEN OTHERS THEN
       raise_application_error(-20000,'Произошла ошибка - '||SQLCODE||' -ERROR- '||SQLERRM);
    END;
  /* Функция дл получения статуса кассы */
  FUNCTION AT_fGetStatusCash(codeCash varchar2) RETURN VARCHAR2
    IS
      statusName varchar2(30);
      idCash number;
      depIdCash number;
    BEGIN
      idCash := AT_fGetCashId(cashCode => codeCash);
      depIdCash := AT_fGetCashDepId(cashCode => codeCash);
      SELECT s.LONGNAME INTO statusName FROM T_BOP_STAT s, T_PROCESS p, T_PROCMEM m, M_CSHDSC op
      WHERE op.DEP_ID = m.DEP_ID and op.ID = m.ORD_ID and m.MAINFL = 1
            and p.ID = m.ID and s.ID=p.BOP_ID and s.NORD = p.NSTAT
            and op.id = idCash and op.dep_id = depIdCash;
      RETURN statusName;
    EXCEPTION
       WHEN OTHERS THEN
       raise_application_error(-20000,'Произошла ошибка - '||SQLCODE||' -ERROR- '||SQLERRM);
    END;
  /* Процедура для выполнения операции открытия кассы */
  PROCEDURE AT_pOpenCash(codeCash varchar2)
  IS
    idCash number;
    depIdCash number;
    statusCash varchar2(30);
    OutOper varchar2(2000);
  BEGIN
    idCash := AT_fGetCashId(cashCode => codeCash);
    depIdCash := AT_fGetCashDepId(cashCode => codeCash);
    statusCash := AT_fGetStatusCash(codeCash => codeCash);

    IF statusCash = 'Касса закрыта' THEN
      c_pkgconnect.popen(); c_pkgsession.doper := trunc(sysdate);
      T_PKGRUNOPRUTL.pRunOperation(  nDepId => depIdCash -- Подразделение документа
                                     , nId => idCash     -- Идентификатор документа
                                     , sOperCode  => 'OPEN'  -- Код операции
                                     , sInOperParams  => null          -- Входные параметры операции
                                     , sOutOperParams => OutOper          -- Выходные параметры операции
                                     , nRaise  => 1 );         -- Генерировать ли исключение ( 0, null-нет )
      COMMIT;
    ELSE
      c_pkgconnect.popen(); c_pkgsession.doper := trunc(sysdate);
      T_PKGRUNOPRUTL.pRunOperation(  nDepId => depIdCash -- Подразделение документа
                                     , nId => idCash     -- Идентификатор документа
                                     , sOperCode  => 'CLOSE'  -- Код операции
                                     , sInOperParams  => null          -- Входные параметры операции
                                     , sOutOperParams => OutOper          -- Выходные параметры операции
                                     , nRaise  => 1 );         -- Генерировать ли исключение ( 0, null-нет )
      COMMIT;
      c_pkgconnect.popen(); c_pkgsession.doper := trunc(sysdate);
      T_PKGRUNOPRUTL.pRunOperation(  nDepId => depIdCash -- Подразделение документа
                                     , nId => idCash     -- Идентификатор документа
                                     , sOperCode  => 'OPEN'  -- Код операции
                                     , sInOperParams  => null          -- Входные параметры операции
                                     , sOutOperParams => OutOper          -- Выходные параметры операции
                                     , nRaise  => 1 );         -- Генерировать ли исключение ( 0, null-нет )
      COMMIT;
    END IF;
  END;
  FUNCTION AT_fGetCliIdFromCodeCard(cliCode varchar2) RETURN NUMBER
    IS
      cliId number;
    BEGIN
      SELECT DISTINCT gh.ID INTO cliId FROM G_CLI g, G_CLIHST gh
      WHERE g.dep_id = gh.dep_id and g.id = gh.id
      AND trunc(sysdate) between gh.fromdate and gh.todate
      AND g.CODE = cliCode;
      RETURN cliId;
    EXCEPTION
       WHEN OTHERS THEN
         raise_application_error(-20000,'Произошла ошибка - '||SQLCODE||' -ERROR- '||SQLERRM);
    END;
  FUNCTION AT_fGetCliDepIdFromCodeCard(cliCode varchar2) RETURN NUMBER
    IS
      cliDepId number;
    BEGIN
      SELECT DISTINCT gh.DEP_ID INTO cliDepId FROM G_CLI g, G_CLIHST gh
      WHERE g.dep_id = gh.dep_id and g.id = gh.id
      AND trunc(sysdate) between gh.fromdate and gh.todate
      AND g.CODE = cliCode;
      RETURN cliDepId;
    EXCEPTION
       WHEN OTHERS THEN
         raise_application_error(-20000,'Произошла ошибка - '||SQLCODE||' -ERROR- '||SQLERRM);
    END;
  PROCEDURE AT_pAddCliRegDocBase(cliCode varchar2)
    IS
      PassNum number;
      numDoc varchar2(100);
      cliId number;
      cliDepId number;
      nord integer;
    BEGIN
      SELECT round(dbms_random.value(1111111,9999999)) INTO PassNum from dual;
      numDoc := 'TEST - ' || TO_CHAR(PassNum, 'FM9999999');
      cliId := AT_fGetCliIdFromCodeCard(cliCode => cliCode);
      cliDepId := AT_fGetCliDepIdFromCodeCard(cliCode => cliCode);
      c_pkgconnect.popen(); c_pkgsession.doper := trunc(sysdate);
      G_PKGCLIREGDOC.pUpdCliRegDocBase(
                                         P_DEP_ID => cliDepId
                                       , P_ID => cliId
                                       , P_NORD => nord
                                       , P_TYP => null
                                       , P_DOC_ID => 604
                                       , P_SER => null
                                       , P_NUM => numDoc
                                       , P_DT_FROM => trunc(sysdate)
                                       , P_DT_TO => null
                                       , P_GNI_ID => null
                                       , P_LICTYP => null
                                       , P_PRIM => 'Autotest'
                                       , P_ORG => 'УГД по Аль-Фарабийскому району'
                                       , P_ARCFL => '0'
                                       , P_REGNUM => numDoc
                                       , P_LCWRK => null
                                       , P_REGDT => null
                                       , P_CLIHST_FROMDATE => null
                                       , P_DOCLST_ID => null
                                       , P_DOCLST_NORD => null
                                       );
      COMMIT;
    EXCEPTION
       WHEN OTHERS THEN
       raise_application_error(-20000,'Произошла ошибка - '||SQLCODE||' -ERROR- '||SQLERRM);
    END;
  PROCEDURE AT_pSetStopCreditCheck(nord varchar2, decVal varchar2)
    IS
    BEGIN
      c_pkgconnect.popen(); c_pkgsession.doper := trunc(sysdate);
      C_PKGDECTBL.pSetSolution(
                               idTbl      => '337690',
                               iNord      => nord,    -- номер строки в таблице решений: 25 для Кредитов, 26 для Кредитных линий
                               sSolution  => decVal -- 0 для отключения проверки и 1 для включения
                               );
    COMMIT;
    EXCEPTION
      WHEN OTHERS THEN
      raise_application_error(-20000,'Произошла ошибка - '||SQLCODE||' -ERROR- '||SQLERRM);
    END;
    /* Процедура для создания кассового документа в MADVPAY */
  PROCEDURE AT_pCreateCashDoc(ksoCode varchar2, cshCode varchar2, cshCode2 varchar2, usr2Code varchar2, dscr varchar2,
                              fio varchar2, valCode varchar2, icsId number, icsDepId number)
    IS
      cId number;
      cDepId number;
    BEGIN
      c_pkgconnect.popen(); c_pkgsession.doper := trunc(sysdate);
      M_BSCSH.PSAVE(NDEP_ID => cDepId, NID => cId,
                  SKSO_CODE => ksoCode, SCSH_CODE => cshCode,
                  SCSH2_CODE => cshCode2, SUSR2_CODE => usr2Code,
                  SDSCR => dscr,
                  SFIO => fio,
                  SRNN => NULL, SPASSNUM => NULL,
                  SPASSSER => NULL, NPASSTYP_ID => NULL,
                  P_PARENTPROC => NULL, P_PARENTOPR => NULL,
                  SVALCODE => valCode, SBOPCODE => NULL,
                  SBOPSTAT => NULL, SRESIDFL => NULL,
                  SACC_CODE => NULL, SNOCMSFL => '0',
                  SNOCMSPRIM => NULL, DPASSDAT => NULL,
                  SPASSORG => NULL, SVALCMS_CODE => NULL,
                  SPRINTREPFL => '1', NDEP2_ID => NULL,
                  SCYCLE_NUM => NULL, NICS_ID => icsId,
                  NICS_DEP_ID => icsDepId, NCHA_ID => NULL,
                  SAPIFL => '0');
      COMMIT;
    EXCEPTION
      WHEN OTHERS THEN
      raise_application_error(-20000,'Произошла ошибка - '||SQLCODE||' -ERROR- '||SQLERRM);
    END;
  /* Функция для получения статуса проверки на признак Стоп Кредит у клиента */
  FUNCTION AT_fGetStopCreditVal(nord integer) RETURN varchar2
  IS
    solution_val VARCHAR2(10);
    nord_new integer;
  BEGIN
    nord_new := nord;
    SELECT SOLUTION INTO solution_val
    FROM CV_TBLROW
    WHERE ID = 337690 AND NORD = nord_new;
  RETURN solution_val;
  EXCEPTION
    WHEN OTHERS THEN
      raise_application_error(-20000,'Произошла ошибка - '||SQLCODE||' -ERROR- '||SQLERRM);
  END;
  /* процедура для изменения статуса проверки на признак Стоп Кредит у клиента */
  PROCEDURE AT_pChangeStopCredit(nord number, status varchar2)
  IS
    solution_val VARCHAR2(10);
    err_message VARCHAR2(4000);
    classCode VARCHAR2(100);
    objCode VARCHAR2(100);
  BEGIN
    c_pkgconnect.popen(); c_pkgsession.doper := trunc(sysdate);
    solution_val := Z_PKG_AUTO_TEST.AT_fGetStopCreditVal(nord);
        C_PKGDECTBL.pSetSolution(
             idTbl   => '337690',
             iNord   => nord,
             sSolution => status
        );
  COMMIT;
  EXCEPTION
    WHEN OTHERS THEN
        err_message := SQLERRM;
        classCode := Z_PKG_AUTO_TEST.AT_fGetErrClassCode(err_message);
        objCode := Z_PKG_AUTO_TEST.AT_fGetErrObjCode(err_message);
        Z_PKG_AUTO_TEST.AT_pCancelColvirObjStat(classCode, objCode);
        COMMIT;
        C_PKGDECTBL.pSetSolution(
             idTbl   => '337690',
             iNord   => nord,
             sSolution => status
        );
  COMMIT;
  END;

  PROCEDURE p_activate_c_file_2402(c_id_code number) IS
    pCapTrnId G_CAPTMPEXTTRN.ID%type := c_id_code;
    pOper  varchar2(30) := 'A1VAvc';

    rFile   N_CRDIN%rowtype;
    rDtl    N_CRDINDTL%rowtype;

    rTrn    N_CRDINTRN%rowtype;
    rTrnDtl N_CRDINTRNDTL%rowtype;
    iCnt    pls_integer := 0;
    rExt    G_CAPTMPEXTTRN%rowtype;
    sCond   varchar2(30);

  begin
    c_pkgconnect.popen;
    select count(*) into iCnt from N_CRDINTRN t where t.G_CAPTMPEXTTRN_ID = pCapTrnId;
    if iCnt > 0 then
      raise_application_error(-20000, 'Транзакция уже обрабатывалась');
    end if;

    select * into rExt from G_CAPTMPEXTTRN where ID = pCapTrnId;

    select count(*) into iCnt from n_crdin where ondate = trunc(sysdate) and fname like 'CAP04%';
    if iCnt > 0 then
      select * into rFile from n_crdin where id =
      (select max(id) from n_crdin where ondate = trunc(sysdate) and fname like 'CAP04%');
    else
      rFile.PRC_ID := 1;
      rFile.ONDATE := trunc(sysdate);
      rFile.FNUM := 1;
      rFile.FNAME := 'CAP04'||lpad(rFile.FNUM, 3, '_')||'.'||to_char(rFile.ONDATE, 'DDD');
      rFile.FDATE := sysdate;
      rFile.RCVDATE := sysdate;
      rFile.STATE := 1;
      rFile.ARESTFL := '0';
      rFile.ID := null;
      insert into N_CRDIN values rFile returning ID into rFile.ID;
    end if;

    select null, nvl(max(NORD), 0)+1 into rDtl.ID, rDtl.NORD from N_CRDINDTL where FILE_ID = rFile.ID;
    insert into N_CRDINDTL(FILE_ID, NORD, FORMAT, STATE) values (rFile.ID, rDtl.NORD, 'TrnExtr', 0)
    returning ID into rDtl.ID;

    rTrn.ID := rDtl.ID;
    dbms_output.put_line('TRN_ID='||rTrn.ID);
    rTrn.TRN_DATE := rExt.EXTIME;
    rTrn.TRN_VAL := rExt.CUR2_CODE;

    select extractvalue(rExt.xdata, '/trn/card/panm'),
           extractvalue(rExt.xdata, '/trn/card/termtype'),
           extractvalue(rExt.xdata, '/trn/card/rrn'),
           extractvalue(rExt.xdata, '/trn/card/mcc'),
           extractvalue(rExt.xdata, '/trn/card/psys'),
           extractvalue(rExt.xdata, '/trn/card/term'),
           extractvalue(rExt.xdata, '/trn/card/auth'),
           extractvalue(rExt.xdata, '/trn/card/loc'),
           extractvalue(rExt.xdata, '/trn/card/caid'),
           extractvalue(rExt.xdata, '/trn/card/cond')
      into rTrn.CARD_NO,
           rTrn.TERM_TYPE,
           rTrn.REFER,
           rTrn.MCC_CODE,
           rTrn.EVENT_AREA,
           rTrn.TERM_ID,
           rTrn.APR_CODE,
           rTrn.MERCH_NAME,
           rTrn.MERCH_NUM,
           sCond
     from dual;

    rTrn.TRN_SUM := rExt.AMOUNT2;
    rTrn.TRNACC_SUM := rExt.AMOUNT;
    rTrn.TRN_TYPE := pOper;

    rTrn.ACC_VAL := rExt.CUR_CODE;
    rTrn.DEBFL := 1-nvl(rExt.INCFL,0);
    rTrn.SB_VAL := rExt.CUR_CODE;
    rTrn.SB_SUM := rExt.AMOUNT;
    rTrn.CARD_ACC := rExt.OBJ_CODE;

    rTrn.ACQREF_NR := case when rTrn.EVENT_AREA = 'ON-US' then rTrn.APR_CODE||rTrn.REFER else null end;
    rTrn.REVERSFL := '0';
    rTrn.TRN_NUM := rExt.ID;
    rTrn.DOC_TYPE := 'O';
    rTrn.AUTH_SUM := rExt.AMOUNT2;
    rTrn.RECONC_SUM := rTrn.SB_SUM;
    rTrn.RECONC_VAL := rTrn.SB_VAL;
    rTrn.FEEACC_SUM := 0;
    rTrn.FEEACC_DIRECT := null;
    rTrn.CUST_FEEACC_SUM := 0;
    rTrn.CUST_FEEACC_DIRECT := null;

    begin
      select ALFA_3 into rTrn.MERCH_COUNTRY from T_REG_STD where CODE = trim(substr(rTrn.MERCH_NAME, 39, 2));
    exception
      when no_data_found then
        rTrn.MERCH_COUNTRY := 'KAZ';
    end;

    rTrn.MERCH_CITY := trim(substr(rTrn.MERCH_NAME, 26, 13));
    rTrn.MERCH_NAME := trim(substr(rTrn.MERCH_NAME, 1, 24));
    rTrn.PRIMARY_DOC := null;
    rTrn.HASH_CARDCODE := dbms_crypto.Hash(UTL_RAW.CAST_TO_RAW(rTrn.CARD_NO), DBMS_CRYPTO.HASH_SH1);
    rTrn.MASK_CARDCODE := rTrn.CARD_NO;
    rTrn.CONTRA_CONTRACT_NUM := rTrn.TERM_ID;
    rTrn.CARDIDN := rExt.IN_IDN_CODE1;

    select CODE into rTrn.CLI_CODE from g_cliextsys s, n_crd c
     where c.cardidn = rTrn.CARDIDN and s.dep_id = c.holder_dep_id and s.id = c.holder_id and s.systype = 'OW';

    rTrn.REG_DOCNUM := case when rTrn.EVENT_AREA = 'ON-US' then rExt.REFER else null end;

    insert into N_CRDINTRN values rTrn returning Id into rTrn.ID;

    insert into N_CRDINTRNDTL(ID, DTL_NAME, DTL_VALUE) values (rTRN.ID, 'Account_Type', 'P');
    insert into N_CRDINTRNDTL(ID, DTL_NAME, DTL_VALUE) values (rTRN.ID, 'Acquirer_ID', 1819);
    insert into N_CRDINTRNDTL(ID, DTL_NAME, DTL_VALUE) values (rTRN.ID, 'Auth_Registration_Number',  '1012345678');
    insert into N_CRDINTRNDTL(ID, DTL_NAME, DTL_VALUE) values (rTRN.ID, 'Card_Expiry_Date', '31.12.21');
    insert into N_CRDINTRNDTL(ID, DTL_NAME, DTL_VALUE) values (rTRN.ID, 'Contra_Entry_Channel', 'P');
    insert into N_CRDINTRNDTL(ID, DTL_NAME, DTL_VALUE) values (rTRN.ID, 'Contra_Entry_Contract_Number', rTRN.TERM_ID);
    insert into N_CRDINTRNDTL(ID, DTL_NAME, DTL_VALUE) values (rTRN.ID, 'Contra_Entry_Contract_Type', substr(pOper, 3, 2));
    insert into N_CRDINTRNDTL(ID, DTL_NAME, DTL_VALUE) values (rTRN.ID, 'Contract_Number', rTrn.CARD_NO);
    insert into N_CRDINTRNDTL(ID, DTL_NAME, DTL_VALUE) values (rTRN.ID, 'Contract_Type', substr(pOper, 5, 2));
    insert into N_CRDINTRNDTL(ID, DTL_NAME, DTL_VALUE) values (rTRN.ID, 'Device_Financial_Cycle_Number', '123');
    insert into N_CRDINTRNDTL(ID, DTL_NAME, DTL_VALUE) values (rTRN.ID, 'GL_Date', to_char(trunc(sysdate), 'dd.mm.yy'));
    insert into N_CRDINTRNDTL(ID, DTL_NAME, DTL_VALUE) values (rTRN.ID, 'Issuer_Reference_Number', '1234567890');
    insert into N_CRDINTRNDTL(ID, DTL_NAME, DTL_VALUE) values (rTRN.ID, 'Member reconciliation Indic', '');
    insert into N_CRDINTRNDTL(ID, DTL_NAME, DTL_VALUE) values (rTRN.ID, 'Misc_Fee_Amount', '0');
    insert into N_CRDINTRNDTL(ID, DTL_NAME, DTL_VALUE) values (rTRN.ID, 'Misc_Fee_Direction', '');
    insert into N_CRDINTRNDTL(ID, DTL_NAME, DTL_VALUE) values (rTRN.ID, 'Original_Contra_Entry_Number', rTRN.TERM_ID);
    insert into N_CRDINTRNDTL(ID, DTL_NAME, DTL_VALUE) values (rTRN.ID, 'Original_Entry_Number', rTrn.CARD_NO);
    insert into N_CRDINTRNDTL(ID, DTL_NAME, DTL_VALUE) values (rTRN.ID, 'RBS_Contract_Number', rTrn.CARDIDN);
    insert into N_CRDINTRNDTL(ID, DTL_NAME, DTL_VALUE) values (rTRN.ID, 'Settlement_Date', to_char(trunc(sysdate), 'dd.mm.yy'));
    insert into N_CRDINTRNDTL(ID, DTL_NAME, DTL_VALUE) values (rTRN.ID, 'Source_Identification_Spec', '');
    insert into N_CRDINTRNDTL(ID, DTL_NAME, DTL_VALUE) values (rTRN.ID, 'Target_Identification_Spec', '00');
    insert into N_CRDINTRNDTL(ID, DTL_NAME, DTL_VALUE) values (rTRN.ID, 'Transaction_Condition', sCond);
    insert into N_CRDINTRNDTL(ID, DTL_NAME, DTL_VALUE) values (rTRN.ID, 'Value_Date', to_char(trunc(sysdate), 'dd.mm.yy'));

    commit;
  end;

  PROCEDURE p_activate_c_file_2416(c_id_code number) IS
    pCapTrnId G_CAPTMPEXTTRN.ID%type := c_id_code;
    pOper  varchar2(30) := 'I5VAvc';
    rFile   N_CRDIN%rowtype;
    rDtl    N_CRDINDTL%rowtype;
    rTrn    N_CRDINTRN%rowtype;
    rTrnDtl N_CRDINTRNDTL%rowtype;
    iCnt    pls_integer := 0;
    rExt    G_CAPTMPEXTTRN%rowtype;
    sCond   varchar2(30);

  begin
    c_pkgconnect.popen;
    select count(*) into iCnt from N_CRDINTRN t where t.G_CAPTMPEXTTRN_ID = pCapTrnId;
    if iCnt > 0 then
      raise_application_error(-20000, 'Транзакция уже обрабатывалась');
    end if;

    select * into rExt from G_CAPTMPEXTTRN where ID = pCapTrnId;

    select count(*) into iCnt from n_crdin where ondate = trunc(sysdate) and fname like 'CAP04%';
    if iCnt > 0 then
      select * into rFile from n_crdin where id =
      (select max(id) from n_crdin where ondate = trunc(sysdate) and fname like 'CAP04%');
    else
      rFile.PRC_ID := 1;
      rFile.ONDATE := trunc(sysdate);
      rFile.FNUM := 1;
      rFile.FNAME := 'CAP04'||lpad(rFile.FNUM, 3, '_')||'.'||to_char(rFile.ONDATE, 'DDD');
      rFile.FDATE := sysdate;
      rFile.RCVDATE := sysdate;
      rFile.STATE := 1;
      rFile.ARESTFL := '0';
      rFile.ID := null;
      insert into N_CRDIN values rFile returning ID into rFile.ID;
    end if;

    select null, nvl(max(NORD), 0)+1 into rDtl.ID, rDtl.NORD from N_CRDINDTL where FILE_ID = rFile.ID;
    insert into N_CRDINDTL(FILE_ID, NORD, FORMAT, STATE) values (rFile.ID, rDtl.NORD, 'TrnExtr', 0)
    returning ID into rDtl.ID;

    rTrn.ID := rDtl.ID;
    dbms_output.put_line('TRN_ID='||rTrn.ID);
    rTrn.TRN_DATE := rExt.EXTIME;
    rTrn.TRN_VAL := rExt.CUR2_CODE;

    select extractvalue(rExt.xdata, '/trn/card/panm'),
           extractvalue(rExt.xdata, '/trn/card/termtype'),
           extractvalue(rExt.xdata, '/trn/card/rrn'),
           extractvalue(rExt.xdata, '/trn/card/mcc'),
           extractvalue(rExt.xdata, '/trn/card/psys'),
           extractvalue(rExt.xdata, '/trn/card/term'),
           extractvalue(rExt.xdata, '/trn/card/auth'),
           extractvalue(rExt.xdata, '/trn/card/loc'),
           extractvalue(rExt.xdata, '/trn/card/caid'),
           extractvalue(rExt.xdata, '/trn/card/cond')
      into rTrn.CARD_NO,
           rTrn.TERM_TYPE,
           rTrn.REFER,
           rTrn.MCC_CODE,
           rTrn.EVENT_AREA,
           rTrn.TERM_ID,
           rTrn.APR_CODE,
           rTrn.MERCH_NAME,
           rTrn.MERCH_NUM,
           sCond
     from dual;

    rTrn.TRN_SUM := rExt.AMOUNT2;
    rTrn.TRNACC_SUM := rExt.AMOUNT;
    rTrn.TRN_TYPE := pOper;
    rTrn.ACC_VAL := rExt.CUR_CODE;
    rTrn.DEBFL := 1-nvl(rExt.INCFL,0);
    rTrn.SB_VAL := rExt.CUR_CODE;
    rTrn.SB_SUM := rExt.AMOUNT;
    rTrn.CARD_ACC := rExt.OBJ_CODE;
    rTrn.ACQREF_NR := case when rTrn.EVENT_AREA = 'ON-US' then rTrn.APR_CODE||rTrn.REFER else null end;
    rTrn.REVERSFL := '0';
    rTrn.TRN_NUM := rExt.ID;
    rTrn.DOC_TYPE := 'O';
    rTrn.AUTH_SUM := rExt.AMOUNT2;
    rTrn.RECONC_SUM := rTrn.SB_SUM;
    rTrn.RECONC_VAL := rTrn.SB_VAL;
    rTrn.FEEACC_SUM := 0;
    rTrn.FEEACC_DIRECT := null;
    rTrn.CUST_FEEACC_SUM := 0;
    rTrn.CUST_FEEACC_DIRECT := null;
    begin
      select ALFA_3 into rTrn.MERCH_COUNTRY from T_REG_STD where CODE = trim(substr(rTrn.MERCH_NAME, 39, 2));
    exception
      when no_data_found then
        rTrn.MERCH_COUNTRY := 'KAZ';
    end;

    rTrn.MERCH_CITY := trim(substr(rTrn.MERCH_NAME, 26, 13));
    rTrn.MERCH_NAME := trim(substr(rTrn.MERCH_NAME, 1, 24));
    rTrn.PRIMARY_DOC := null;
    rTrn.HASH_CARDCODE := dbms_crypto.Hash(UTL_RAW.CAST_TO_RAW(rTrn.CARD_NO), DBMS_CRYPTO.HASH_SH1);
    rTrn.MASK_CARDCODE := rTrn.CARD_NO;
    rTrn.CONTRA_CONTRACT_NUM := rTrn.TERM_ID;
    rTrn.CARDIDN := rExt.IN_IDN_CODE1;

    select CODE into rTrn.CLI_CODE from g_cliextsys s, n_crd c
     where c.cardidn = rTrn.CARDIDN and s.dep_id = c.holder_dep_id and s.id = c.holder_id and s.systype = 'OW';

    rTrn.REG_DOCNUM := case when rTrn.EVENT_AREA = 'ON-US' then rExt.REFER else null end;

    insert into N_CRDINTRN values rTrn returning Id into rTrn.ID;

    insert into N_CRDINTRNDTL(ID, DTL_NAME, DTL_VALUE) values (rTRN.ID, 'Account_Type', 'P');
    insert into N_CRDINTRNDTL(ID, DTL_NAME, DTL_VALUE) values (rTRN.ID, 'Acquirer_ID', 1819);
    insert into N_CRDINTRNDTL(ID, DTL_NAME, DTL_VALUE) values (rTRN.ID, 'Auth_Registration_Number',  '1012345678');
    insert into N_CRDINTRNDTL(ID, DTL_NAME, DTL_VALUE) values (rTRN.ID, 'Card_Expiry_Date', '31.12.21');
    insert into N_CRDINTRNDTL(ID, DTL_NAME, DTL_VALUE) values (rTRN.ID, 'Contra_Entry_Channel', 'P');
    insert into N_CRDINTRNDTL(ID, DTL_NAME, DTL_VALUE) values (rTRN.ID, 'Contra_Entry_Contract_Number', rTRN.TERM_ID);
    insert into N_CRDINTRNDTL(ID, DTL_NAME, DTL_VALUE) values (rTRN.ID, 'Contra_Entry_Contract_Type', substr(pOper, 3, 2));
    insert into N_CRDINTRNDTL(ID, DTL_NAME, DTL_VALUE) values (rTRN.ID, 'Contract_Number', rTrn.CARD_NO);
    insert into N_CRDINTRNDTL(ID, DTL_NAME, DTL_VALUE) values (rTRN.ID, 'Contract_Type', substr(pOper, 5, 2));
    insert into N_CRDINTRNDTL(ID, DTL_NAME, DTL_VALUE) values (rTRN.ID, 'Device_Financial_Cycle_Number', '123');
    insert into N_CRDINTRNDTL(ID, DTL_NAME, DTL_VALUE) values (rTRN.ID, 'GL_Date', to_char(trunc(sysdate), 'dd.mm.yy'));
    insert into N_CRDINTRNDTL(ID, DTL_NAME, DTL_VALUE) values (rTRN.ID, 'Issuer_Reference_Number', '1234567890');
    insert into N_CRDINTRNDTL(ID, DTL_NAME, DTL_VALUE) values (rTRN.ID, 'Member reconciliation Indic', '');
    insert into N_CRDINTRNDTL(ID, DTL_NAME, DTL_VALUE) values (rTRN.ID, 'Misc_Fee_Amount', '0');
    insert into N_CRDINTRNDTL(ID, DTL_NAME, DTL_VALUE) values (rTRN.ID, 'Misc_Fee_Direction', '');
    insert into N_CRDINTRNDTL(ID, DTL_NAME, DTL_VALUE) values (rTRN.ID, 'Original_Contra_Entry_Number', rTRN.TERM_ID);
    insert into N_CRDINTRNDTL(ID, DTL_NAME, DTL_VALUE) values (rTRN.ID, 'Original_Entry_Number', rTrn.CARD_NO);
    insert into N_CRDINTRNDTL(ID, DTL_NAME, DTL_VALUE) values (rTRN.ID, 'RBS_Contract_Number', rTrn.CARDIDN);
    insert into N_CRDINTRNDTL(ID, DTL_NAME, DTL_VALUE) values (rTRN.ID, 'Settlement_Date', to_char(trunc(sysdate), 'dd.mm.yy'));
    insert into N_CRDINTRNDTL(ID, DTL_NAME, DTL_VALUE) values (rTRN.ID, 'Source_Identification_Spec', '');
    insert into N_CRDINTRNDTL(ID, DTL_NAME, DTL_VALUE) values (rTRN.ID, 'Target_Identification_Spec', '00');
    insert into N_CRDINTRNDTL(ID, DTL_NAME, DTL_VALUE) values (rTRN.ID, 'Transaction_Condition', sCond);
    insert into N_CRDINTRNDTL(ID, DTL_NAME, DTL_VALUE) values (rTRN.ID, 'Value_Date', to_char(trunc(sysdate), 'dd.mm.yy'));

     commit;
  end;

  PROCEDURE turn_off_doc_sign(C_ID number, C_DEP_ID number, id_par number, in_value varchar2 default NULL) IS
  begin
   c_pkgconnect.popen(); c_pkgsession.doper := trunc(sysdate);
   T_PKGDEAPRM.pSetPrmId(idDea => C_ID, idDep => C_DEP_ID, idPar => id_par, p_value => in_value);
    COMMIT;
  EXCEPTION
    WHEN OTHERS THEN
      raise_application_error(-20000,'Произошла ошибка - '||SQLCODE||' -ERROR- '||SQLERRM);
  END;
  FUNCTION AT_fGetRandomNum(cnt number) RETURN NUMBER
    IS
      minNum varchar2(30);
      maxNum varchar2(30);
      rndNum number;
    BEGIN
      for minCnt IN 1 .. cnt
        LOOP
          minNum := minNum || '1';
          maxNum := maxNum || '9';
        END LOOP;
      SELECT round(dbms_random.value(TO_NUMBER(minNum, maxNum) ,TO_NUMBER(maxNum, maxNum))) INTO rndNum from dual;
      RETURN rndNum;
    EXCEPTION
      WHEN OTHERS THEN
        raise_application_error(-20000,'Произошла ошибка - '||SQLCODE||' -ERROR- '||SQLERRM);
    END;
  FUNCTION AT_fGetDocNum(depId number, idDoc number) RETURN VARCHAR2
    IS
      docNum varchar2(30);
    BEGIN
      SELECT CODE INTO docNum FROM t_ord
      WHERE dep_id = depId and id = idDoc;
      RETURN docNum;
    EXCEPTION
      WHEN OTHERS THEN
        raise_application_error(-20000,'Произошла ошибка - '||SQLCODE||' -ERROR- '||SQLERRM);
    END;
  FUNCTION AT_fGetLongnameCli(iinCli varchar2) RETURN VARCHAR2
    IS
      longname varchar2(255);
    BEGIN
      SELECT gh.longname INTO longname FROM g_cli g, g_clihst gh
      WHERE g.dep_id = gh.dep_id and g.id = gh.id
      AND trunc(sysdate) BETWEEN gh.fromdate AND gh.todate
      AND gh.taxcode = iinCli AND rownum = 1;
      RETURN longname;
    EXCEPTION
      WHEN OTHERS THEN
        raise_application_error(-20000,'Произошла ошибка - '||SQLCODE||' -ERROR- '||SQLERRM);
    END;
  FUNCTION AT_pCreateIrPtpForCard2(depCode varchar2, orderType varchar2, operCode varchar2, summOper varchar2, valSumm varchar2,
                                    payerAcc varchar2, bik varchar2, payerGet varchar2, iinGet varchar2,
                                    fioHead varchar2, knp varchar2, kod varchar2, kbe varchar2) RETURN VARCHAR2
    IS
      depId number;
      rndNum number;
      docNum varchar2(30);
      fioGet varchar2(255);
      NID number;
    BEGIN
      depId := AT_fGetIdDep(DepCode => depCode);
      fioGet := AT_fGetLongnameCli(iinCli => iinGet);
      rndNum := AT_fGetRandomNum(cnt => 9);
      docNum := 'AT - ' || rndNum;
      c_pkgconnect.popen(); c_pkgsession.doper := trunc(sysdate);
      S_BSPAY.PSAVE(NDEP_ID => depId,
                    NID => NID,
                    SKSO_CODE => orderType, -- тип документа
                    SCHA_CODE => operCode, -- тип операции
                    SAMOUNT => summOper, -- сумма платежа
                    SVAL_CODE => valSumm, -- код валюты
                    DDORD => trunc(sysdate),
                    DDVAL => trunc(sysdate),
                    SCODE_ACL => payerAcc, -- счет отправителя
                    SCODE_BCR => bik, -- БИК отправителя
                    SCODE_ACR => payerGet, -- код получателя
                    SRNN_CR => iinGet, -- ИИН получателя
                    SCODE_BC => null,
                    STXT_HEAD => fioHead, -- ФИО руководителя для подписи
                    STXT_BUCH => null,
                    STXT_DSCR => 'Autotest',
                    STXT_BEN => fioGet, -- ФИО получателя
                    NNOCMSFL => 0,
                    SKNP => knp, -- КНП
                    SCODE_OD => kod, -- КОд отправителя
                    SCODE_BE => kbe, -- КБе получателя
                    SPRIM => null,
                    SCODE => docNum, -- номер документа
                    NFLZO => 0,
                    IVRFFL => NULL,
                    SLIMFL => NULL,
                    IPRINTFL => NULL,
                    SSPEEDFL => NULL,
                    SSOST => NULL,
                    SREFER => NULL,
                    P_PARENTPROC => NULL,
                    P_PARENTOPR => NULL,
                    SRNNCLI => NULL,
                    STXTPAY => NULL,
                    SVOPER => NULL,
                    SGCVP => NULL,
                    SPERIOD => NULL,
                    NDEA_DEP_ID => NULL,
                    NDEA_ID => NULL,
                    NDEPUSR_ID => NULL,
                    SCARD => NULL,
                    SORIGINACC => NULL,
                    SCMS_STRG => '0',
                    SCLIACCCOM => NULL,
                    AF_FL => 0,
                    SOPV => NULL,
                    P_NOLIMIT => NULL);
    COMMIT;
    RETURN AT_fGetDocNum(depId => depId, idDoc => NID);
    EXCEPTION
      WHEN OTHERS THEN
        raise_application_error(-20000,'Произошла ошибка - '||SQLCODE||' -ERROR- '||SQLERRM);
    END;
  PROCEDURE AT_pRegIrPtp(docNum varchar2)
    IS
    BEGIN
      AT_pRunOperWOParams(CODE_NUM => docNum,
                          OPER_CODE => 'REQREG');
    EXCEPTION
      WHEN OTHERS THEN
        raise_application_error(-20000,'Произошла ошибка - '||SQLCODE||' -ERROR- '||SQLERRM);
    END;
  PROCEDURE AT_pInputToCard2(docNum varchar2, -- номер документа в Sordpay
                             queue varchar2) -- приоритет помещения
    IS
      sParams varchar2(255);
    BEGIN
      sParams := 'PRIOR => ' || queue;
      Z_PKG_AUTO_TEST.AT_pRunOperWithParams(docNum => docNum, operCode => 'ADDCRD2', params => sParams);
    EXCEPTION
      WHEN OTHERS THEN
        raise_application_error(-20000,'Произошла ошибка - '||SQLCODE||' -ERROR- '||SQLERRM);
    END;
  FUNCTION AT_fGetStatusDoc(docNum varchar2) RETURN VARCHAR2
    IS
      status varchar2(1000);
    BEGIN
      SELECT DISTINCT s.longname into status
      FROM T_ORD o, T_BOP_STAT s, T_PROCESS p, T_PROCMEM m, S_ORDPAY op
      WHERE
        op.DEP_ID = m.DEP_ID AND op.ID = m.ORD_ID
        AND p.ID = m.ID AND s.ID=p.BOP_ID AND s.NORD = p.NSTAT
        AND op.DEP_ID = o.DEP_ID AND op.ID = o.ID AND o.code = docNum;
        RETURN status;
    EXCEPTION
      WHEN OTHERS THEN
        raise_application_error(-20000,'Произошла ошибка - '||SQLCODE||' -ERROR- '||SQLERRM);
    END;
  FUNCTION AT_fGetIdDocCL(docNum varchar2) RETURN NUMBER
  /* Функция для получения id документа из t_ord для Кредитных линий SLLOAN */
  IS
    docID number;
  BEGIN
    SELECT o.ID INTO docID FROM t_ord o, T_DEA d, L_LDEA l
    WHERE o.code = docNum and o.DEP_ID = d.DEP_ID and o.ID = d.ID and l.DEP_ID = d.DEP_ID and l.ID = d.ID;
    RETURN docID;
  EXCEPTION
    WHEN OTHERS THEN
      RETURN(0);
      Raise_Application_Error(-20000,
                              Localize('Ошибка при получении id карточного договора',
                                       'Z_PKG_AUTO_TEST',
                                       'AT_fGetIdCardDoc'));
  END;
  FUNCTION AT_fGetDepIdDocCL(docNum varchar2) RETURN NUMBER
  /* Функция для получения dep_id документа из t_ord для Кредитных линий SLLOAN */
  IS
    depID number;
  BEGIN
    SELECT o.DEP_ID INTO depID FROM t_ord o, T_DEA d, L_LDEA l
    WHERE o.code = docNum and o.DEP_ID = d.DEP_ID and o.ID = d.ID and l.DEP_ID = d.DEP_ID and l.ID = d.ID;
    RETURN depID;
  EXCEPTION
    WHEN OTHERS THEN
      RETURN(0);
      Raise_Application_Error(-20000,
                              Localize('Ошибка при получении dep_id карточного договора',
                                       'Z_PKG_AUTO_TEST',
                                       'AT_fGetDepIdCardDoc'));
  END;
  /* Функция для получения id индивидуальной ставки договора Кредитных линий */
  FUNCTION AT_fGetPcnId(depId integer, docId integer, pcnType varchar2) RETURN NUMBER
  IS
   PcnId integer;
  BEGIN
    SELECT PCN_ID INTO PcnId FROM LV_QR_ARL
    WHERE DEP_ID = depId and ID = docId and LONGNAME = pcnType;
    RETURN PcnId;
  EXCEPTION
   WHEN too_many_rows THEN
    raise_application_error(-20001, 'Запрос вернул более одной строки');
END;
  /* Процедура для изменения индивидуальной ставки договора Кредитных линий */
  PROCEDURE AT_pSetPcnValue(docnum varchar2, pcnTypeVal varchar2, perval varchar2) IS
    doc_id integer;
    dep_id integer;
    id_pcn integer;
  BEGIN
   c_pkgconnect.popen(); c_pkgsession.doper := trunc(sysdate);
   doc_id := Z_PKG_AUTO_TEST.AT_fGetIdDocCL(DocNum);
   dep_id := Z_PKG_AUTO_TEST.AT_fGetDepIdDocCL(DocNum);
   id_pcn := AT_fGetPcnId(depId => dep_id, docId => doc_id, pcnType => pcnTypeVal);
   T_PKGPCNSPEC.pAddNewValue(idPcn => id_pcn, dFromdate => trunc(sysdate), fpercent => perval);
   COMMIT;
  EXCEPTION
    WHEN OTHERS THEN
      raise_application_error(-20000,'Произошла ошибка - '||SQLCODE||' -ERROR- '||SQLERRM);
  END;
  PROCEDURE AT_pInputPtpToCard2(docNum varchar2, -- номер документа в Sordpay
                                queue varchar2) -- приоритет помещения
    IS
      sParams varchar2(255);
    BEGIN
      sParams := 'PRIOR => ' || queue;
      Z_PKG_AUTO_TEST.AT_pRunOperWithParams(docNum => docNum, operCode => 'ADDCRD2NOREG', params => sParams);
    EXCEPTION
      WHEN OTHERS THEN
        raise_application_error(-20000,'Произошла ошибка - '||SQLCODE||' -ERROR- '||SQLERRM);
    END;
  /* Функция для получения даты для редактирования графика AHDCONT*/
  FUNCTION AT_fGetPlanDateFromGraphicDog(paymentName varchar2, depId number, dId number) RETURN VARCHAR2
  IS
    planDate VARCHAR2(100);
  BEGIN
    SELECT DISTINCT t.plandate INTO planDate FROM T_DEADTL d, TT_POINT t, T_DEADTLDSC ds,
    T_DEADTLSTT st, T_DEA de, T_DEACLS dcl
    WHERE d.TT_ID = t.ID
    and d.TT_NORD = t.NORD
    and d.DTL_ID = ds.ID
    and d.DTL_ID = st.ID
    and d.STATUS = st.STATUS
    and d.dep_id = de.dep_id
    and d.id = de.id
    and d.doctype = dcl.id(+)
    and d.dep_id = depId and d.id = dId
    and ds.longname = paymentName;
    RETURN planDate;
  EXCEPTION
     WHEN OTHERS THEN
       raise_application_error(-20000,'Произошла ошибка - '||SQLCODE||' -ERROR- '||SQLERRM);
   END;
/*
  PROCEDURE AT_pCreGrn(
      P_DEP_ID       IN INTEGER,
      P_ID           IN OUT INTEGER,
      P_DCLID        IN INTEGER,
      P_CLI_DEPID    IN INTEGER,
      P_CLI_ID       IN INTEGER,
      P_SDOK         IN VARCHAR2 DEFAULT '0',
      P_VAL_ID       IN INTEGER DEFAULT NULL,
      P_COR_DEPID    IN INTEGER DEFAULT NULL,
      P_COR_ID       IN INTEGER DEFAULT NULL,
      P_CODE         IN VARCHAR2 DEFAULT NULL,
      P_NUMDEAL      IN VARCHAR2 DEFAULT NULL,
      P_DORD         IN DATE DEFAULT SYSDATE,
      P_FROMDATE     IN DATE DEFAULT SYSDATE,
      P_TODATE       IN DATE DEFAULT NULL,
      P_PARENT_PROC  IN INTEGER DEFAULT NULL,
      P_PARENT_OPR   IN INTEGER DEFAULT NULL,
      P_GRNNAZN      IN INTEGER DEFAULT NULL,
      P_DEA_DEP_ID   IN INTEGER DEFAULT NULL,
      P_DEA_ID       IN INTEGER DEFAULT NULL,
      P_SRV_DEP_ID   IN INTEGER DEFAULT NULL
  )
  IS
      oGRN_DEPID     INTEGER;
      oGRN_ID        INTEGER;
      oGRN_NAME      G_CLIHST.LONGNAME%TYPE;
      oADDRESS       G_CLIHST.ADDRESS%TYPE;
      oCODE          G_CLI.Code%TYPE;
      oSWIFT_NAME    G_CLIHST.Swift_Name%TYPE;
      iNAZN          INTEGER := P_GRNNAZN;

  BEGIN
      -- Получаем атрибуты гаранта
      BEGIN
          JGRNDEA.getGrnDef(P_DEP_ID, oGRN_DEPID, oGRN_ID, oGRN_NAME, oADDRESS, oCODE, oSWIFT_NAME);
      EXCEPTION
          WHEN OTHERS THEN
              dbms_output.put_line('Ошибка при получении атрибутов гарантии: ' || SQLERRM);
              RAISE;
      END;

      -- Если назначение гарантии не передано, находим его
      IF iNAZN IS NULL THEN
          BEGIN
              iNAZN := bs_dom.DVALNAME('J_GRNNAZN', T_PkgDeaPrm.fClsParByCode(P_DCLID, 'J_GRNNAZN'));
          EXCEPTION
              WHEN OTHERS THEN
                  dbms_output.put_line('Ошибка при получении назначения гарантии: ' || SQLERRM);
                  RAISE;
          END;
      END IF;

      -- Вызов процедуры создания/обновления гарантии
      BEGIN
          JGRNDEA.pCreUpdGRN(
              iDEP          => P_DEP_ID,
              idDeal        => P_ID,
              iDCL_ID       => P_DCLID,
              sCode         => P_CODE,
              sNUMDEAL      => P_NUMDEAL,
              dORD          => P_DORD,
              dRECV         => NULL,
              dFROMDATE     => P_FROMDATE,
              dTODATE       => P_TODATE,
              dDREQUEST     => NULL,
              cWS           => '1',       -- Гарантия/поручительство
              cNORECALL     => '0',       -- Отзывная/безотзывная
              cCONDIFL      => '0',       -- Условная/безусловная
              iW_FRM        => NVL(bs_dom.DVALNAME('J_GRNFRM', T_PkgDeaPrm.fClsParByCode(P_DCLID, 'FRM_DEF')), 1),
              iNAZN         => iNAZN,
              cDOCUMFL      => '0',
              iVAL_ID       => NVL(P_VAL_ID, t_pkgval.fValCode2Id(NVL(T_PkgDeaPrm.fClsParByCode(P_DCLID, 'J_VALAUTO'), P_CodeNatVal))),
              iVRATE_ID     => T_PkgVal.fGetDscVrtTypDef,
              iRate         => 1,
              sSDOK         => P_SDOK,
              iCLI_DEP_ID   => P_CLI_DEPID,
              iCLI_ID       => P_CLI_ID,
              iCOR_DEP_ID   => P_COR_DEPID,
              iCOR_ID       => P_COR_ID,
              sCOR_NAME     => G_PkgCli.fGetCliLongName(P_COR_DEPID, P_COR_ID),
              iGRN_DEP_ID   => oGRN_DEPID,
              iGRN_ID       => oGRN_ID,
              sGRN_NAME     => oGRN_NAME,
              iCANAL        => J_PKGICS.fGetTypeCanalByName(T_PkgDeaPrm.fClsParByCode(P_DCLID, 'TYPCON_DEF')),
              iTUS_ID       => P_IDUS,
              iTRF_IDCAT    => T_PkgTrf.fGetCatId(T_PkgDeaPrm.fClsParByCode(P_DCLID, 'J_TRFGRPAUTO'), 0),
              p_parent_Proc => P_PARENT_PROC,
              p_parentOpr   => P_PARENT_OPR,
              iDEA_DEP_ID   => P_DEA_DEP_ID,
              iDEA_ID       => P_DEA_ID,
              nSRV_DEP_ID   => P_SRV_DEP_ID
          );
          COMMIT;
          dbms_output.put_line('Гарантия успешно создана/обновлена: ' || P_NUMDEAL);

      EXCEPTION
          WHEN OTHERS THEN
              dbms_output.put_line('Ошибка при создании/обновлении гарантии: ' || SQLERRM);
              RAISE;
      END;

  END AT_pCreGrn;*/
  FUNCTION AT_fGetIdProductCode(productCode varchar2) RETURN NUMBER
    IS
      productId number;
    BEGIN
      SELECT id INTO productId FROM T_DEACLS dc
      WHERE dc.code = productCode;
      RETURN productId;
    EXCEPTION
      WHEN OTHERS THEN
        raise_application_error(-20000,'Произошла ошибка - '||SQLCODE||' -ERROR- '||SQLERRM);
    END;
  FUNCTION AT_fGetIdVal(valCode varchar2) RETURN NUMBER
    IS
      valId number;
    BEGIN
      SELECT id INTO valId FROM T_VAL_STD
      WHERE CODE = valCode;
      RETURN valId;
    EXCEPTION
      WHEN OTHERS THEN
        raise_application_error(-20000,'Произошла ошибка - '||SQLCODE||' -ERROR- '||SQLERRM);
    END;
  FUNCTION AT_fGetIdTrfCat(trfCode varchar2) RETURN NUMBER
    IS
      trfId number;
    BEGIN
      SELECT ID INTO trfId FROM S_TRFGRP
      WHERE CODE = trfCode;
      RETURN trfId;
    EXCEPTION
      WHEN OTHERS THEN
        raise_application_error(-20000,'Произошла ошибка - '||SQLCODE||' -ERROR- '||SQLERRM);
    END;
  FUNCTION AT_fGetLongnameCliByCodeCard(cardCode varchar2) RETURN VARCHAR2
    IS
      longname varchar2(255);
    BEGIN
      SELECT gh.longname INTO longname FROM g_cli g, g_clihst gh
      WHERE g.dep_id = gh.dep_id and g.id = gh.id
      AND trunc(sysdate) BETWEEN gh.fromdate AND gh.todate
      AND g.code = cardCode AND rownum = 1;
      RETURN longname;
    EXCEPTION
      WHEN OTHERS THEN
        raise_application_error(-20000,'Произошла ошибка - '||SQLCODE||' -ERROR- '||SQLERRM);
    END;
  PROCEDURE AT_pCreAkkred(aDep varchar2, aProductCode varchar2, aValCode varchar2, aSum varchar2, aCliCode varchar2,
                          aDocNumCreLine varchar2, aTrfCat varchar2, aCliAcc varchar2, aTodate varchar2,
                          aCorAgentCode varchar2, aSrvDepCode varchar2, typeDoc varchar2, bnkRole varchar2,
                          execmeth varchar2)
    -- aSrvDepCode - Подразделение обслуживания
    IS
      gCliDepId number;
      gCliId number;
      gDepAkk number;
      gProductId number;
      gValId number;
      gIdCreLine number;
      gDepIdCreLine number;
      gIdTrf number;
      gDepIdCorAgent number;
      gIdCorAgent number;
      gIdSrvDep number;
      gCorLongname G_CLIHST.LONGNAME%TYPE;
      deal number;
      gDocNum varchar2(30);
      rndNum number;
      numAkkred varchar2(30);
    BEGIN
      gCliDepId := AT_fGetCliDepIdFromCodeCard(cliCode => aCliCode);
      gCliId := AT_fGetCliIdFromCodeCard(cliCode => aCliCode);
      gDepAkk := AT_fGetIdDep(DepCode => aDep);
      gProductId := AT_fGetIdProductCode(productCode => aProductCode);
      gValId := AT_fGetIdVal(valCode => aValCode);
      IF aDocNumCreLine is not null THEN
        gIdCreLine := AT_fGetIdDocCL(docNum => aDocNumCreLine);
        gDepIdCreLine := AT_fGetDepIdDocCL(docNum => aDocNumCreLine);
      END IF;
      gIdTrf := AT_fGetIdTrfCat(trfCode => aTrfCat);
      gDepIdCorAgent := AT_fGetCliDepIdFromCodeCard(cliCode => aCorAgentCode);
      gIdCorAgent := AT_fGetCliIdFromCodeCard(cliCode => aCorAgentCode);
      gIdSrvDep := AT_fGetIdDep(DepCode => aSrvDepCode);
      gCorLongname := AT_fGetLongnameCliByCodeCard(cardCode => aCorAgentCode);
      rndNum := AT_fGetRandomNum(cnt => 9);
      gDocNum := 'AT/DOC/' || rndNum;
      numAkkred := 'AT/ACRED/' || rndNum;
      JLET.PCREUPDLET(IDEP => gDepAkk,
                      IDDEAL => deal,
                      IDCL_ID => gProductId,
                      SCODE => gDocNum,
                      SNUMDEAL => numAkkred,
                      DORD => trunc(sysdate),
                      DRECV => trunc(sysdate),
                      DFROMDATE => trunc(sysdate),
                      DTODATE => aTodate,
                      DREQUEST => null,
                      ICONDI_PAY => 0,
                      CBNK_ROLE => bnkRole,
                      IANALIZ_L => null,
                      CSW_43T => null,
                      CSW_43P => null,
                      DSW_31D_1 => aTodate,
                      SPLACE_FIN => 'ITALY',
                      SPLACE_IN => null,
                      SPLACE_OUT => null,
                      IVAL_ID => gValId,
                      IVRATE_ID => NULL,
                      IRATE => NULL,
                      SCHARGES => NULL,
                      ISW_39A_1 => NULL,
                      ISW_39A_2 => NULL,
                      SSW_39B => NULL,
                      SSW_42M => NULL,
                      SSW_42P => NULL,
                      SSW_44D => NULL,
                      SSW_48 => NULL,
                      SSW_40A => '0',
                      SSDOK => aSum,
                      ICLI_DEP_ID => gCliDepId,
                      ICLI_ID => gCliId,
                      NSRV_DEP_ID => gIdSrvDep,
                      ICOR_DEP_ID => gDepIdCorAgent,
                      ICOR_ID => gIdCorAgent,
                      SCOR_NAME => gCorLongname,
                      SCOR_CODE_ACC => NULL,
                      ICOR_NORD => NULL,
                      ICANAL => 21,
                      IDEA_DEP_ID => gDepIdCreLine,
                      IDEA_ID => gIdCreLine,
                      SPRIM => 'Autotests',
                      ITUS_ID => 1,
                      SCONDITEXT => null,
                      ICATEGORY => 1,
                      P_PARENT_PROC => NULL,
                      P_PARENTOPR => NULL,
                      SSW_49 => '0',
                      SSW_72 => NULL,
                      ITHIRD_DEP_ID => null,
                      ITHIRD_ID => null,
                      CDELIVCOVERFL => null,
                      ITRF_IDCAT => gIdTrf,
                      PNDEALIBNK => NULL,
                      P_V39C => NULL,
                      P_V44E => NULL,
                      P_V44F => NULL,
                      P_BENFIRST_DEP_ID => NULL,
                      P_BENFIRST_ID => NULL,
                      P_BENFIRST_NAME => NULL,
                      P_BENSEC_DEP_ID => NULL,
                      P_BENSEC_ID => NULL,
                      P_BENSEC_NAME => NULL,
                      P_EXECATTR => NULL,
                      P_CLI_CUR_ACC => aCliAcc,
                      P_CLI_VAL_ACC => NULL,
                      P_LET_PRIM => 'Autotests',
                      P_STAT_CODE => NULL,
                      P_EXECMETH => execmeth,
                      CPOSTFINFL => '0',
                      CDISCOUNTFL => '0',
                      CRSRVFL => '0',
                      CCNTGRNFL => '0',
                      DDENDGRN => NULL,
                      CTRANSFERFL => '0',
                      P_DELAY_TYPE => NULL,
                      P_DELAY_DAYS => NULL,
                      P_POSTFIN_DAYS => NULL,
                      NBAL_DEP_ID => NULL);
      COMMIT;
      /* Добавление документа во вкладку Типы документов */
      IF typeDoc is not null THEN
        AT_pAddDocTypeAkkred(docNumAkkred => gDocNum, typeDoc => typeDoc);
      END IF;
      dbms_output.put_line('1out -> ' || gDocNum);
    END;
  FUNCTION AT_fGetDepIdAkkred(docNum varchar2) RETURN NUMBER
    IS
      AkkDepId number;
    BEGIN
      SELECT DEP_ID INTO AkkDepId from JV_LET
      WHERE code = docNum;
      RETURN AkkDepId;
    EXCEPTION
      WHEN OTHERS THEN
        raise_application_error(-20000,'Произошла ошибка - '||SQLCODE||' -ERROR- '||SQLERRM);
    END;
  FUNCTION AT_fGetIdAkkred(docNum varchar2) RETURN NUMBER
    IS
      AkkId number;
    BEGIN
      SELECT ID INTO AkkId from JV_LET
      WHERE code = docNum;
      RETURN AkkId;
    EXCEPTION
      WHEN OTHERS THEN
        raise_application_error(-20000,'Произошла ошибка - '||SQLCODE||' -ERROR- '||SQLERRM);
    END;
  FUNCTION AT_fGetIdTypeDoc(codeDoc varchar2) RETURN NUMBER
    IS
      docId number;
    BEGIN
      SELECT ID INTO docId FROM J_ICSDOC_STD
      WHERE CODE = codeDoc;
      RETURN docId;
    EXCEPTION
      WHEN OTHERS THEN
        raise_application_error(-20000,'Произошла ошибка - '||SQLCODE||' -ERROR- '||SQLERRM);
    END;
  PROCEDURE AT_pAddDocTypeAkkred(docNumAkkred varchar2, typeDoc varchar2)
    IS
      akkDepId number;
      akkId number;
      docId number;
    BEGIN
      akkDepId := AT_fGetDepIdAkkred(docNum => docNumAkkred);
      akkId := AT_fGetIdAkkred(docNum => docNumAkkred);
      docId := AT_fGetIdTypeDoc(codeDoc => typeDoc);
      J_PKGICS.pCreUpdDeaDocType (P_ID              => null,
                                  P_DEA_DEP_ID      => akkDepId,
                                  P_DEA_ID          => akkId,
                                  P_DOC_TYPE_ID     => docId,
                                  P_COPYFL          => null);
      COMMIT;
    EXCEPTION
      WHEN OTHERS THEN
        raise_application_error(-20000,'Произошла ошибка - '||SQLCODE||' -ERROR- '||SQLERRM);
    END;
  PROCEDURE AT_pCalcSheduleAkkred(docNum varchar2)
    IS
      OutOper varchar2(1000);
      akkDepId number;
      akkId number;
    BEGIN
      akkDepId := AT_fGetDepIdAkkred(docNum => docNum);
      akkId := AT_fGetIdAkkred(docNum => docNum);
      c_pkgconnect.popen(); c_pkgsession.doper := trunc(sysdate);
      T_PKGRUNOPRUTL.pRunOperation(  nDepId => akkDepId -- Подразделение документа
                                     , nId => akkId     -- Идентификатор документа
                                     , sOperCode  => 'GRAFCALC'  -- Код операции
                                     , sInOperParams  => null          -- Входные параметры операции
                                     , sOutOperParams => OutOper          -- Выходные параметры операции
                                     , nRaise  => 1 );         -- Генерировать ли исключение ( 0, null-нет )
      COMMIT;
    END;
  PROCEDURE AT_pRegAkkred(docNum varchar2, operCode varchar2)
    IS
      OutOper varchar2(1000);
      akkDepId number;
      akkId number;
    BEGIN
      akkDepId := AT_fGetDepIdAkkred(docNum => docNum);
      akkId := AT_fGetIdAkkred(docNum => docNum);
      c_pkgconnect.popen(); c_pkgsession.doper := trunc(sysdate);
      T_PKGRUNOPRUTL.pRunOperation(  nDepId => akkDepId -- Подразделение документа
                                     , nId => akkId     -- Идентификатор документа
                                     , sOperCode  => operCode  -- Код операции
                                     , sInOperParams  => null          -- Входные параметры операции
                                     , sOutOperParams => OutOper          -- Выходные параметры операции
                                     , nRaise  => 1 );         -- Генерировать ли исключение ( 0, null-нет )
      COMMIT;
    END;
  PROCEDURE AT_pUchetAkkred(docNum varchar2)
    IS
      OutOper varchar2(1000);
      akkDepId number;
      akkId number;
    BEGIN
      akkDepId := AT_fGetDepIdAkkred(docNum => docNum);
      akkId := AT_fGetIdAkkred(docNum => docNum);
      c_pkgconnect.popen(); c_pkgsession.doper := trunc(sysdate);
      T_PKGRUNOPRUTL.pRunOperation(  nDepId => akkDepId -- Подразделение документа
                                     , nId => akkId     -- Идентификатор документа
                                     , sOperCode  => 'OP9'  -- Код операции
                                     , sInOperParams  => null          -- Входные параметры операции
                                     , sOutOperParams => OutOper          -- Выходные параметры операции
                                     , nRaise  => 1 );         -- Генерировать ли исключение ( 0, null-нет )
      COMMIT;
    END;
  FUNCTION AT_fGetPayDateFromGraphAkkred(DocNum varchar2) RETURN varchar2 IS
     datePay VARCHAR2(20);
     payDate DATE;
     akkDepId NUMBER;
     akkId NUMBER;
     BEGIN
       akkDepId := AT_fGetDepIdAkkred(docNum => DocNum);
       akkId := AT_fGetIdAkkred(docNum => DocNum);
       select s.doper INTO payDate from T_ORD t, T_DEASHDPNT s,  T_ARLCLC c, T_ARLDEA ad, T_ARLDSC d
       where s.ord_id = t.id and s.dep_id = t.dep_id and s.CLC_ID = c.ID and c.ARL_ID = d.ID
       and ad.DEP_ID = s.DEP_ID and ad.ORD_ID = s.ORD_ID and ad.CLC_ID = c.ID and t.dep_id=akkDepId and t.id=akkId
       and s.doper >= trunc(sysdate) and rownum < 2
       order by s.doper;
       datePay := TO_CHAR(payDate, 'dd.mm.yy');
       RETURN datePay;
     EXCEPTION
     WHEN OTHERS THEN
       RETURN(0);
         RAISE_APPLICATION_ERROR(-20000,
             Localize('Ошибка при получении даты платежа', 'Z_PKG_AUTO_TEST', 'AT_fGetPayDateFromGraphAkkred'));
     END;
   FUNCTION AT_fGetReferOper(docNum varchar2, codeOper varchar2) RETURN VARCHAR2
     IS
       depId number;
       ordId number;
       refer varchar2(100);
     BEGIN
       depId := Z_PKG_AUTO_TEST.AT_fGetDepIdDoc(docNum);
       ordId := Z_PKG_AUTO_TEST.AT_fGetIdDoc(docNum);
       SELECT j.REFER INTO refer from T_OPERJRN j, T_SCEN s
       WHERE s.ID = j.BOP_ID
       AND s.NORD = j.NOPER
       AND j.DEP_ID = depId AND j.ORD_ID = ordId AND s.CODE = codeOper
       ORDER BY j.EXECDT DESC FETCH FIRST 1 ROWS ONLY;
       RETURN refer;
     EXCEPTION
       WHEN OTHERS THEN
         raise_application_error(-20000,'Произошла ошибка - '||SQLCODE||' -ERROR- '||SQLERRM);
     END;
   FUNCTION AT_fGetReferOperForPayDoc(docNum varchar2, codeOper varchar2) RETURN VARCHAR2
     IS
       ordId number;
       depId number;
       refer varchar2(100);
       payDocCode varchar2(100);
       depIdPayDoc number;
       idPayDoc number;
     BEGIN
       /* Получаем код последнего добавленного платежного документа */
       depId := Z_PKG_AUTO_TEST.AT_fGetDepIdDoc(docNum);
       ordId := Z_PKG_AUTO_TEST.AT_fGetIdDoc(docNum);
       SELECT o.CODE INTO payDocCode FROM T_DEAPAY t, t_ord o
       WHERE t.dep_id = o.dep_id AND t.id = o.id AND t.dep_id = depId AND t.dea_id = ordId
       ORDER BY o.DIMPORT DESC FETCH FIRST 1 ROWS ONLY;

       depIdPayDoc := Z_PKG_AUTO_TEST.AT_fGetDepIdDoc(payDocCode);
       idPayDoc := Z_PKG_AUTO_TEST.AT_fGetIdDoc(payDocCode);
       /* Получаем референс последей операции */
       SELECT j.REFER INTO refer from T_OPERJRN j, T_SCEN s
       WHERE s.ID = j.BOP_ID
       AND s.NORD = j.NOPER
       AND j.DEP_ID = depIdPayDoc AND j.ORD_ID = idPayDoc AND s.CODE = codeOper
       ORDER BY j.EXECDT DESC FETCH FIRST 1 ROWS ONLY;
       RETURN refer;
     EXCEPTION
       WHEN OTHERS THEN
         raise_application_error(-20000,'Произошла ошибка - '||SQLCODE||' -ERROR- '||SQLERRM);
     END;
   FUNCTION AT_fGetIdPayAttr(codeAttr varchar2) RETURN NUMBER
     IS
       attrId number;
     BEGIN
       SELECT ID INTO attrId FROM T_PAYATRTYP t
       WHERE t.CODE = codeAttr;
       RETURN attrId;
     EXCEPTION
       WHEN OTHERS THEN
         raise_application_error(-20000,'Произошла ошибка - '||SQLCODE||' -ERROR- '||SQLERRM);
     END;
   FUNCTION AT_fGetNordIdPayAttr(depIdDoc number, idDoc number, idAttr number, accCodeAttr varchar2) RETURN NUMBER
     IS
       nordAttr number;
     BEGIN
       SELECT a.NORD INTO nordAttr FROM T_DEAPAYATR a, T_PAYATRTYP t
       WHERE a.dep_id = depIdDoc AND a.id = idDoc
       AND a.typ_id = idAttr AND a.code_acc = accCodeAttr AND rownum < 2;
       RETURN nordAttr;
     EXCEPTION
       WHEN OTHERS THEN
         raise_application_error(-20000,'Произошла ошибка - '||SQLCODE||' -ERROR- '||SQLERRM);
     END;
   FUNCTION AT_fGetLongnamePayAttr(docNum varchar2, codeAttr varchar2, accCodeAttr varchar2) RETURN VARCHAR2
     IS
       depId number;
       ordId number;
       idAttr number;
       nordIdAttr number;
       longname varchar2(1000);
     BEGIN
       /* Получаем dep_id и id номера договора */
       depId := Z_PKG_AUTO_TEST.AT_fGetDepIdDoc(docNum);
       ordId := Z_PKG_AUTO_TEST.AT_fGetIdDoc(docNum);
       /* Получаем id платежного атрибута */
       idAttr := AT_fGetIdPayAttr(codeAttr => codeAttr);
       /* Получаем NORD платежного атрибута по договору */
       nordIdAttr := AT_fGetNordIdPayAttr(depIdDoc => depId,
                                          idDoc => ordId,
                                          idAttr => idAttr,
                                          accCodeAttr => accCodeAttr);
       /* Вызываем функция для получения longname платежного атрибута */
       longname := substr(T_PKGPAYATR.fGetPayTypName(depId, ordId, nordIdAttr, idAttr),1,250);
       RETURN longname;
     EXCEPTION
       WHEN OTHERS THEN
         raise_application_error(-20000,'Произошла ошибка - '||SQLCODE||' -ERROR- '||SQLERRM);
     END;

  /* Функция возвращает класс объекта DDPVCS из кода ошибки "Объект 'DECTBL;Z_077_DSBL_OPR_BLK' уже корректируется разработчиком '***'" */
  FUNCTION AT_fGetErrClassCode(err_message IN VARCHAR2)
    RETURN VARCHAR2 IS
       reg_exp VARCHAR2(100) := '''([^'';]*)';
       class_code VARCHAR2(100);
    BEGIN
       class_code := REGEXP_SUBSTR(err_message, reg_exp, 1, 1, NULL, 1);
    RETURN class_code;
  END;
  /* Функция возвращает код объекта DDPVCS из кода ошибки "Объект 'DECTBL;Z_077_DSBL_OPR_BLK' уже корректируется разработчиком '***'" */
  FUNCTION AT_fGetErrObjCode(err_message IN VARCHAR2)
    RETURN VARCHAR2 IS
      reg_exp VARCHAR2(100) := '''[^'']*;([^'']*)''';
      obj_code VARCHAR2(100);
    BEGIN
      obj_code := REGEXP_SUBSTR(err_message, reg_exp, 1, 1, NULL, 1);
    RETURN obj_code;
  END;
  /* Процедура сбрасывает статус объекта в Colvir DDPVCS по классу и коду объекта, например, objCode = 'Z_PKG_AUTO_TEST', classCode = 'PKG' */
  PROCEDURE AT_pCancelColvirObjStat(classCode varchar2, objCode varchar2)
    IS
    BEGIN
         UPDATE C_SCM ss
         SET ss.USR_ID = null, ss.STATE = 'N', ss.STATE_DATE = null
         WHERE ss.id = (select s.id from C_CFGCLS c, C_SCM s
                        where  s.CLS_ID = c.ID
                        and s.OBJ_CODE = objCode and c.code = classCode);
         COMMIT;
    END;
  /* Процедура по созданию договора для списания товарно-материальных ценностей, задача OUT */
  PROCEDURE AT_pCreateOutDoc(productCode varchar2, depCode varchar2, depOrd varchar2, senderId number,
                             pzOfl varchar2, calcndsFl varchar2, COSTNDSFL varchar2, arestFl varchar2,
                             prim varchar2,typeDocReason number, descDocReason varchar2)
    IS
      randomNum number;
      randomStr varchar2(30);
      depDocId number;
      depOrdId number;
      pdclId number;
      randomRsn varchar2(30);
      outId number;
    BEGIN
      randomNum := AT_fGetRandomNum(cnt => 9);
      randomStr := 'AT/OUT/' || randomNum;
      randomRsn := 'AT/RSN/' || randomNum;
      depDocId := AT_fGetIdDep(DepCode => depCode);
      depOrdId := AT_fGetIdDep(DepCode => depOrd);
      pdclId := AT_fGetIdProductCode(productCode => productCode);
      c_pkgconnect.popen(); c_pkgsession.doper := trunc(sysdate);
      M_OFF.PCREUPDDOC(PDEP_ID => depDocId,
                               PID => outId, PDCL_ID => pdclId,
                               PCODE => randomStr, PDORD => P_OPERDAY,
                               PDRECV => P_OPERDAY, PDFROMDATE => P_OPERDAY,
                               PDTODATE => NULL, PDEXEC => P_OPERDAY, PSDOK => NULL,
                               PDEP_ORD => depOrdId, PSENDER_ID => senderId, PRSN_ID => null,
                               PZOFL => pzOfl, PCALCNDSFL => calcndsFl, PCOSTNDSFL => COSTNDSFL,
                               PARESTFL => arestFl, PPRIM => prim, PTUS_ID => 1,
                               P_PARENT_PROC => NULL, P_PARENTOPR => NULL, PRSN_CODE => randomRsn,
                               PRSN_DORD => P_OPERDAY, PRSN_DOC_ID => typeDocReason, PRSN_DOC_DESCR => descDocReason,
                               PRES_ID => NULL);
      dbms_output.put_line('1out -> ' || randomStr);
      COMMIT;
    EXCEPTION
      WHEN OTHERS THEN
        raise_application_error(-20000,'Произошла ошибка - '||SQLCODE||' -ERROR- '||SQLERRM);
    END;
end Z_PKG_AUTO_TEST;