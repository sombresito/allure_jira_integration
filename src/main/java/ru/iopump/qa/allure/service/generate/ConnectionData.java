package ru.iopump.qa.allure.service.generate;



import ru.iopump.qa.allure.properties.GenerateConfig;
import org.aeonbits.owner.ConfigFactory;

import java.sql.*;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;



public class ConnectionData {
    private static Connection connection;
    private static ResultSet send_query;
    private static Statement statement;
    private static ConnectionData connectionData;
    //private static AppConfig config;
    private static GenerateConfig config;

    public static void connectToDatabase(String url, String username, String password) throws SQLException {
        connection = DriverManager.getConnection(url, username, password);
    }

    public static void disconnectFromDatabase() throws SQLException {
        if (connection != null) {
            connection.close();
        }
    }

    public ResultSet executeQuery(String nameTable, String conditionName, String conditionSearch) throws SQLException {
        Statement statement = connection.createStatement();
        String sqlQueryBD = "select * from  " + nameTable + " WHERE " + conditionName + " ='" + conditionSearch + "'";
        return statement.executeQuery(sqlQueryBD);
    }

    public ResultSet upDateQuery(String nameTable, String nameSet, String refID, String conditionName, String conditionSearch) throws SQLException {
        Statement statement = connection.createStatement();
        String sqlUpQueryBD = "update " + nameTable + " set " + nameSet + " = '" + refID + "' WHERE " + conditionName + " ='" + conditionSearch + "'";
        return statement.executeQuery(sqlUpQueryBD);
    }

    public static void executeQueryDML(String query, String dbUrl, String uiUser, String uiPass) throws SQLException {
        // Получаем URL из конфигурации или задаем вручную

        try {
            connectToDatabase(dbUrl, uiUser, uiPass);
        } catch (SQLException sqlEx) {
            sqlEx.printStackTrace();
        }
        try {
            statement = connection.createStatement();
            send_query = statement.executeQuery(query);
        } catch (SQLException sqlEx) {
            sqlEx.printStackTrace();
        } finally {
            try {
                disconnectFromDatabase();
            } catch (SQLException sqlEx) {
                sqlEx.printStackTrace();
            }
            try {
                send_query.close();
            } catch (SQLException sqlEx) {
                sqlEx.printStackTrace();
            }
        }
    }


    public static ArrayList<ArrayList> executeQuerySelect(String query, String dbUrl, String uiLogin, String uiPassword) throws SQLException {
        ArrayList<ArrayList> row_select = new ArrayList<ArrayList>();
        try {
            // Вместо config.UserBdLogin() и config.UserBdPass() используем данные, введённые пользователем
            connectToDatabase(dbUrl, uiLogin, uiPassword);
        } catch (SQLException sqlEx) {
            sqlEx.printStackTrace();
        }
        try {
            statement = connection.createStatement();
        } catch (SQLException sqlEx) {
            sqlEx.printStackTrace();
        }
        try {
            send_query = statement.executeQuery(query);
            // Перебираем строки выборки
            if (send_query != null) {
                while (send_query.next()) {
                    ArrayList<String> data_select = new ArrayList<String>();
                    ResultSetMetaData resultSetMetaData = send_query.getMetaData();
                    for (int i = 1; i <= resultSetMetaData.getColumnCount(); i++) {
                        int type = resultSetMetaData.getColumnType(i);
                        if (type == Types.VARCHAR || type == Types.CHAR) {
                            data_select.add(send_query.getString(i));
                        } else if (type == Types.NUMERIC) {
                            int get_element = send_query.getInt(i);
                            data_select.add(String.valueOf(Double.valueOf(get_element)));
                        } else if (type == Types.DOUBLE) {
                            Double get_element = send_query.getDouble(i);
                            data_select.add(Double.toString(get_element));
                        } else if (type == Types.TIMESTAMP) {
                            Date get_element = send_query.getDate(i);
                            DateFormat df = new SimpleDateFormat("dd.MM.yyyy");
                            data_select.add(df.format(get_element));
                        } else {
                            System.out.println(String.format("Тип %s в данный момент не поддерживается методом. Пропускаем", type));
                            continue;
                        }
                    }
                    row_select.add(data_select);
                }
            }
        } catch (SQLException sqlEx) {
            sqlEx.printStackTrace();
        } finally {
            try {
                disconnectFromDatabase();
            } catch (SQLException sqlEx) {
                sqlEx.printStackTrace();
            }
            try {
                send_query.close();
            } catch (SQLException sqlEx) {
                sqlEx.printStackTrace();
            }
            return row_select;
        }
    }

    public ArrayList<String> CallSqlProcedure(String package_name, String proc, ArrayList prm_proc, String BdUrl,  String uiLogin, String uiPassword)
            throws SQLException, ErrorExecutingScript, ParseException {
        // Создаём конфигурацию, если нужно брать, например, URL из настроек
        CallableStatement cs = null;
        CallableStatement st = null;
        ArrayList<Integer> index_out_prm = new ArrayList<>();
        ArrayList<String> cnt_prm = new ArrayList<>();
        ArrayList<String> out_prm = new ArrayList<>();
        String str_cnt_prm;

        // Формируем строку параметров для вызова процедуры
        for (int i = 0; i < prm_proc.size(); i++) {
            cnt_prm.add("?");
        }
        str_cnt_prm = String.join(", ", cnt_prm);

        // Получаем информацию о параметрах процедуры
        String select_prm = String.format("SELECT a.POSITION, A.DATA_TYPE, A.IN_OUT " +
                "FROM USER_ARGUMENTS A, ALL_OBJECTS O " +
                "WHERE A.OBJECT_ID = O.OBJECT_ID AND A.OBJECT_NAME = '%s' " +
                "ORDER BY POSITION ASC", proc.toUpperCase());
        ArrayList<ArrayList> list_prm = executeQuerySelect(select_prm, BdUrl, uiLogin, uiPassword);

        // Проверяем количество выходных параметров
        String select_out_prm = String.format("SELECT COUNT(A.IN_OUT) " +
                "FROM USER_ARGUMENTS A, ALL_OBJECTS O " +
                "WHERE A.OBJECT_ID = O.OBJECT_ID AND A.OBJECT_NAME = '%s' AND A.IN_OUT = 'OUT'", proc.toUpperCase());
        ArrayList<ArrayList> list_out_prm = executeQuerySelect(select_out_prm, BdUrl, uiLogin, uiPassword);

        // Подключаемся к БД, используя введённые логин и пароль
        try {
            connectToDatabase(BdUrl, uiLogin, uiPassword);
        } catch (SQLException sqlEx) {
            sqlEx.printStackTrace();
        }

        String popen = "{call c_pkgconnect.popen(); c_pkgsession.doper := trunc(sysdate)}";
        try {
            st = connection.prepareCall(popen);
            st.execute();
        } catch (SQLException sqlEx) {
            try {
                disconnectFromDatabase();
            } catch (SQLException er) {
                er.printStackTrace();
            }
            try {
                st.close();
            } catch (SQLException er) {
                er.printStackTrace();
            }
            sqlEx.printStackTrace();
        }

        try {
            if (package_name == null) {
                cs = connection.prepareCall(String.format("{call %s(%s)}", proc.toUpperCase(), str_cnt_prm));
                System.out.println("Процедура используется без пакета");
            } else {
                System.out.println("Процедура находится в пакете");
                cs = connection.prepareCall(String.format("{call %s.%s(%s)}", package_name.toUpperCase(), proc.toUpperCase(), str_cnt_prm));
            }
        } catch (SQLException sqlEx) {
            sqlEx.printStackTrace();
        }

        // Заполняем параметры процедуры
        for (int i = 0; i < list_prm.size(); i++) {
            if (list_prm.get(i).get(0).equals(String.format("%d.0", i + 1))) {
                if (list_prm.get(i).get(2).equals("IN")) {
                    if (list_prm.get(i).get(1).equals("VARCHAR2")) {
                        cs.setString(i + 1, (String) prm_proc.get(i));
                    } else if (list_prm.get(i).get(1).equals("NUMBER")) {
                        int int_prm = Integer.parseInt((String) prm_proc.get(i));
                        cs.setInt(i + 1, int_prm);
                    } else if (list_prm.get(i).get(1).equals("DATE")) {
                        DateFormat df = new SimpleDateFormat("dd.MM.yyyy");
                        try {
                            Date date = df.parse(String.valueOf(prm_proc.get(i)));
                            Timestamp date_time = new Timestamp(date.getTime());
                            java.sql.Date date_prm = new java.sql.Date(date_time.getTime());
                            cs.setDate(i + 1, date_prm);
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                    } else {
                        throw new ErrorExecutingScript(String.format("Тип данных %s пока не поддерживается методом", list_prm.get(i).get(1)));
                    }
                } else if (list_prm.get(i).get(2).equals("OUT")) {
                    index_out_prm.add(i + 1);
                    if (list_prm.get(i).get(1).equals("VARCHAR2")) {
                        cs.registerOutParameter(i + 1, Types.VARCHAR);
                    } else if (list_prm.get(i).get(1).equals("NUMBER")) {
                        cs.registerOutParameter(i + 1, Types.INTEGER);
                    } else if (list_prm.get(i).get(1).equals("DATE")) {
                        cs.registerOutParameter(i + 1, Types.TIMESTAMP);
                    } else {
                        throw new ErrorExecutingScript(String.format("Тип данных %s пока не поддерживается методом", list_prm.get(i).get(1)));
                    }
                } else {
                    throw new ErrorExecutingScript(String.format("По данному индексу содержится неизвестный тип параметра - %s", list_prm.get(i).get(2)));
                }
            }
        }

        try {
            System.out.println(String.format("Вызываем процедуру - %s", proc.toUpperCase()));
            cs.execute();
            System.out.println(String.format("Выполнена процедура - %s", proc.toUpperCase()));
        } catch (SQLException sqlEx) {
            sqlEx.printStackTrace();
        }

        // Обработка выходных параметров
        if (index_out_prm.size() > 0) {
            for (int i = 0; i < index_out_prm.size(); i++) {
                if (list_prm.get(index_out_prm.get(i) - 1).get(1).equals("VARCHAR2")) {
                    String get_value_str = cs.getString(index_out_prm.get(i));
                    out_prm.add(get_value_str);
                } else if (list_prm.get(index_out_prm.get(i) - 1).get(1).equals("NUMBER")) {
                    int get_value_int = cs.getInt(index_out_prm.get(i));
                    out_prm.add(Integer.toString(get_value_int));
                } else if (list_prm.get(index_out_prm.get(i)).get(1).equals("DATE")) {
                    Date get_value_date = cs.getDate(index_out_prm.get(i));
                    SimpleDateFormat formatter = new SimpleDateFormat("MM.dd.yyyy");
                    out_prm.add(formatter.format(get_value_date));
                } else {
                    throw new ErrorExecutingScript(String.format("Тип данных %s пока не поддерживается методом", list_prm.get(index_out_prm.get(i)).get(1)));
                }
            }
        } else {
            out_prm = null;
        }

        try {
            disconnectFromDatabase();
        } catch (SQLException sqlEx) {
            sqlEx.printStackTrace();
        }
        try {
            st.close();
        } catch (SQLException sqlEx) {
            sqlEx.printStackTrace();
        }
        return out_prm;
    }

    public ResultSet ExequteSelect(String select, ArrayList lst_prm, String BdUrl, String uiLogin, String uiPassword) throws SQLException {
        // Используем логин и пароль, введённые пользователем

        connectToDatabase(BdUrl, uiLogin, uiPassword);
        Statement statement = connection.createStatement();
        PreparedStatement stat = connection.prepareStatement(select);
        stat.setArray(1, connection.createArrayOf("text", new ArrayList[]{lst_prm}));
        ResultSet rs = stat.executeQuery();

        return rs;
    }

}















