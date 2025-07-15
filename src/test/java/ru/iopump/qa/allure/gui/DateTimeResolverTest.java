package ru.iopump.qa.allure.gui;

import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import ru.iopump.qa.allure.properties.AllureProperties;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.TimeZone;

@Slf4j
public class DateTimeResolverTest {

    private DateTimeResolver resolverSpy;

    @Before
    public void setUp() throws Exception {

        var resolver = new DateTimeResolver(
                new AllureProperties(null, null, false, "dd/MM/yy HH:mm:ss", null) //yy/MM/dd HH:mm:ss
        );
        var formatterInCurrentTimeZone = DateTimeFormatter.ofPattern("dd/MM/yy HH:mm:ss").withZone(TimeZone.getDefault().toZoneId()); //yy/MM/dd HH:mm:ss

        resolverSpy = Mockito.spy(resolver);
        Mockito.when(resolverSpy.acquireFormatter()).thenReturn(formatterInCurrentTimeZone);
    }

    @Test
    public void printDate() {
        var dateInZeroTimeZone = LocalDateTime.now(ZoneOffset.UTC);

        String res = resolverSpy.printDate(dateInZeroTimeZone);
        log.info(res);

    }
}