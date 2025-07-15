package ru.iopump.qa.allure.properties;

import lombok.Data;
import lombok.Getter;
import java.util.List;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "azure")
@Getter
@Setter
public class TeamsProperties {
    private List<String> defaultUsers;
}
