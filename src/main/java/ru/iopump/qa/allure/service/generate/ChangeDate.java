package ru.iopump.qa.allure.service.generate;


import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import ru.iopump.qa.allure.service.generate.ErrorExecutingScript;

public class ChangeDate {
    // класс предназначен для прибавления или вычиания даты
    // если надо получить прошлую или будущую даты

    public String FutureLastDate(String sdate, String time_interval, int count, boolean full_year) throws ParseException, ErrorExecutingScript {
        // метод возвращают будущую/прошлую дату
        // для прошлой даты необходимо передавать в count отрицательные значения
        // метод изменяет дату по дням, месяцам, годам
        // при надобности можно добавить другие интервалы
        // метод принимает строку в формате - ddMMyyyy
        // Если нужен сокращенный год, то передаем флаг short_year = true
        // Помимо строки с датой метод принимает
        // time_interval - Временной отрезок на который необходимо увеличить дату (YEAR, MONTH и т.д.)
        // count - Количество на которое необходимо увеличить
        // флаг short_year - логический тип данных. Если указать true, то будет возвращаться сокращенный год
        //String date = "01-01-2020";
        SimpleDateFormat df = new SimpleDateFormat();
        if (full_year == true) {
            df = new SimpleDateFormat("ddMMyyyy");
        } else {
            df = new SimpleDateFormat("ddMMyy");
        }
        final Date date = df.parse(sdate); // conversion from String
        final java.util.Calendar cal = GregorianCalendar.getInstance();
        cal.setTime(date);
        if (time_interval == "month") {
            cal.add(GregorianCalendar.MONTH, count); // date manipulation
        } else if (time_interval == "day") {
            cal.add(GregorianCalendar.DATE, count); // date manipulation
        } else if (time_interval == "year") {
            cal.add(GregorianCalendar.YEAR, count); // date manipulation
        } else {
            throw new ErrorExecutingScript(String.format("Переданный неверный тип временного интервала - %s", time_interval));
        }
        //cal.add(GregorianCalendar.MONTH, 5); // date manipulation
        //String result = "result: " + df.format(cal.getTime());
        String result = df.format(cal.getTime());
        return result;
    }
}

