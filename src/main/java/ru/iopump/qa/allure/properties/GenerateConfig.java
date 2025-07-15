package ru.iopump.qa.allure.properties;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.aeonbits.owner.Config;
import org.aeonbits.owner.Config.Key;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "base")
public class GenerateConfig {
    private String yes_bd;
    private String tp_bd;
    private String bt_bd;
    private String test_bd;

    public String getYes_bd() {
        return yes_bd;
    }

    public void setYes_bd(String yes_bd) {
        this.yes_bd = yes_bd;
    }

    public String getTp_bd() {
        return tp_bd;
    }

    public void setTp_bd(String tp_bd) {
        this.tp_bd = tp_bd;
    }

    public String getBt_bd() {
        return bt_bd;
    }

    public void setBt_bd(String bt_bd) {
        this.bt_bd = bt_bd;
    }

    public String getTest_bd() {
        return test_bd;
    }

    public void setTest_bd(String test_bd) {
        this.test_bd = test_bd;
    }
}




