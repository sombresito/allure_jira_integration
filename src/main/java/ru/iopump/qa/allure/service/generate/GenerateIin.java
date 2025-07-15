package ru.iopump.qa.allure.service.generate;


import ru.iopump.qa.allure.service.generate.ErrorExecutingScript;
import ru.iopump.qa.allure.service.generate.ConnectionData;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Random;
import java.time.Year;
import java.util.List;
import java.util.Arrays;

public class GenerateIin {
    private static String cnt_null = "0";
    private static List<String> long_month = Arrays.asList("01", "03", "05", "07", "08", "10", "12");
    private static List<String> short_month = Arrays.asList("04", "06", "09", "11");
    private static String num_year;
    private static String num_month;
    private static String num_day;
    private static String num_reg;
    private static String type_org;
    private static String type_filial;
    private static String iin;
    private static char century_and_sex;
    private static Random random = new Random();
    private static String century_and_sex_str = "03456";

    public static String generateiinjur() throws Exception {
        // Функция возвращает сформированный ИИН юр лица или ИП

        // получаем рандомный месяц регистрации
        String get_num_month = GetMonth();

        // получаем последние 2 цифры рандомного года
        String get_num_year = GetYear("jur");

        // передаем тип организации от 4 до 6. 4 - юр рез 5 - юр нерез 6 - ип совм
        String get_type_org = GetTypeOrg();

        // тип представительства. ГО, филиал, представительство, крест хозяйство
        String get_type_fil = GetTypeFilial();

        // получаем номер регистрации. Номер создается рандомна и содержит 5 цифр
        String get_num_reg = GetRegNum(5);


        // Расчитываем разряд ИИН. Он формируется по специальной формуле
        int get_control_num = GetControlNum("jur");

        // если модуль и в этот раз >= 10, то возвращаем одни нули
        if (get_control_num >= 10) {
            iin = "000000000000";
        } else {
            iin = get_num_year + get_num_month + get_type_org + get_type_fil + get_num_reg + Integer.toString(get_control_num);

        }
        return iin;
    }
    public static String generateiinfl() throws Exception {
        // Функция возвращает сформированный ИИН физ лица

        // получаем год рождения/регистрации
        String get_num_year = GetYear("fl");

        // получаем рандомный месяц регистрации
        String get_num_month = GetMonth();

        // получаем рандомный день рождения
        String get_num_day = GetDays();

        // получаем номер регистрации. Номер создается рандомна и содержит 4 цифры
        String get_num_reg = GetRegNum(4);

        // Расчитываем разряд ИИН. Он формируется по специальной формуле
        int get_control_num = GetControlNum("fl");

        // если модуль >= 10, то возвращаем одни нули
        if (get_control_num >= 10) {
            iin = "000000000000";
        } else {
            iin = num_year + num_month + num_day + String.valueOf(century_and_sex) + num_reg + Integer.toString(get_control_num);
        }
        return iin;
    }
    private static String GetMonth() {
        // метод возвращает рандомный месяц в двузначном виде
        // если рандомится целое число, то впереди добавляется 0
        int mounth = random.nextInt((12 - 1) + 1) + 1;
        int cnt_mounth = 2;
        int cnt_null_m = cnt_mounth - Integer.toString(mounth).length();
        num_month = GenerateIin.cnt_null.repeat(cnt_null_m) + Integer.toString(mounth);
        return num_month;
    }
    private static String GetDays() {
        // получаем рандомный день рождения
        int cnt_day;
        if (long_month.contains(num_month)) {
            cnt_day = 31;
        } else if (short_month.contains(num_month)) {
            cnt_day = 30;
        } else {
            cnt_day = 28;
        }
        int day = random.nextInt((cnt_day - 1) + 1) + 1;
        int cnt_null = 2;
        int cnt_null_d = cnt_null - Integer.toString(day).length();
        num_day = GenerateIin.cnt_null.repeat(cnt_null_d) + Integer.toString(day);

        return num_day;
    }
    private static String GetYear(String type_entity) throws Exception {
        // метод принимает строку в которой указан тип лица: "fl" or "jur"
        // и выводит рандомный год рождения/регистрации. Сокращенный вариант.

        // определяем век по сгенерированной цифре. Если 20 век, то берем года от 1950 до 1999.
        // для физ лица
        //0 - для иностранных граждан
        //1 - для мужчин, родившихся в XIX веке НЕ ИСПОЛЬЗУЕТСЯ ЗДЕСЬ
        //2 - для женщин, родившихся в XIX веке НЕ ИСПОЛЬЗУЕТСЯ ЗДЕСЬ
        //3 - для мужчин, родившихся в XX веке
        //4 - для женщин, родившихся в XX веке
        //5 - для мужчин, родившихся в XXI веке
        //6 - для женщин, родившихся в XXI веке
        if (type_entity == "fl") {
            int century_and_sex_index = random.nextInt((4 - 0) + 1) + 0;
            century_and_sex = century_and_sex_str.charAt(century_and_sex_index);
            if ("034".contains(Character.toString(century_and_sex))) {
                num_year = Integer.toString(random.nextInt((99 - 50) + 1) + 50);
            } else {
                String currentYear = Integer.toString(Year.now().getValue()).substring(2, 4);
                String year = Integer.toString(random.nextInt(((Integer.parseInt(currentYear) - 21) - 0) + 1) + 0);
                int cnt_year = 2;
                int cnt_null_y = cnt_year - year.length();
                num_year = GenerateIin.cnt_null.repeat(cnt_null_y) + year;
            }
        } else if (type_entity == "jur") {
            // получаем последние 2 цифры рандомного года для юр лица
            String currentYear = Integer.toString(Year.now().getValue()).substring(2, 4);
            int year = random.nextInt((Integer.parseInt(currentYear) - 1)) + 0;
            int cnt_year = 2;
            int cnt_null_y = cnt_year - Integer.toString(year).length();
            num_year = GenerateIin.cnt_null.repeat(cnt_null_y) + Integer.toString(year);
        } else {
            String error_msg = String.format("Передан неверный тип лица - %s", type_entity);
            throw new Exception(error_msg);
        }
        return num_year;
    }
    private static String GetRegNum(Integer cnt_reg_num) {
        // получаем номер регистрации. Номер создается рандомно
        // метод принимает integer - количество цифр в числе
        String count_repeat = "9".repeat(cnt_reg_num);
        String num_reg_rnd = Integer.toString(random.nextInt((Integer.parseInt(count_repeat) - 1) + 1) + 1);
        //int cnt_num_reg = 5;
        int cnt_null_num_reg = cnt_reg_num - num_reg_rnd.length();
        num_reg = GenerateIin.cnt_null.repeat(cnt_null_num_reg) + num_reg_rnd;

        return num_reg;
    }
    private static Integer GetControlNum(String type_entity) {
        // Расчитываем разряд ИИН. Он формируется по специальной формуле
        String part_iin = null;
        if (type_entity == "fl") {
            part_iin = num_year + num_month + num_day + String.valueOf(century_and_sex) + num_reg;
        } else if (type_entity == "jur") {
            part_iin = num_year + num_month + type_org + type_filial + num_reg;
        }
        ArrayList<Integer> weight_list = new ArrayList<Integer>();
        // добавляем в список значения весов для разряда ИИН
        for (int i = 1; i < 12; i++) {
            weight_list.add(i);
        }
        int control_num = 0;
        // производим умножение каждого элемента ИИН с элементами весов разряда. 1 к 1, 2 к 2 и тд
        for (int i = 0; i < 11; i++) {
            control_num += (weight_list.get(i) * Integer.parseInt(String.valueOf(part_iin.charAt(i))));
        }

        // расчитываем модуль получившейся суммы от произведения элемента ИИН и разряда веса

        Double dbl_mod = Double.valueOf(control_num) / 11;
        int mod = dbl_mod.intValue() * 11;
        control_num -= mod;

        // если контрольный разряд = 10, то используем другой вес разряда. Если и тогда разряд = 10, то данный ИИН не используется
        // а возвращаться будут одни нули
        if (control_num >= 10) {
            int control_num_other = 0;
            List<Integer> weight_list_other = Arrays.asList(3, 4, 5, 6, 7, 8, 9, 10, 11, 1, 2);
            // производим умножение каждого элемента ИИН с элементами весов разряда. 1 к 1, 2 к 2 и тд
            for (int i = 0; i < 11; i++) {
                control_num_other += (weight_list_other.get(i) * Integer.parseInt(String.valueOf(part_iin.charAt(i))));
            }

            // расчитываем модуль получившейся суммы от произведения элемента ИИН и разряда веса
            Double dbl_mod_other = Double.valueOf(control_num_other) / 11;
            int mod_other = dbl_mod_other.intValue() * 11;
            control_num_other -= mod_other;

            return control_num_other;

        }
        return control_num;
    }
    private static String GetTypeOrg() {
        // передаем тип организации от 4 до 6. 4 - юр рез 5 - юр нерез 6 - ип совм
        type_org = Integer.toString(random.nextInt((6 - 4) + 1) + 4);
        return type_org;
    }
    private static String GetTypeFilial() {
        // тип представительства. ГО, филиал, представительство, крест хозяйство
        String type_fil_str = "0124";
        int type_fil = random.nextInt((3 - 0) + 1) + 0;
        type_filial = String.valueOf(type_fil_str.charAt(type_fil));

        return type_filial;
    }
}
