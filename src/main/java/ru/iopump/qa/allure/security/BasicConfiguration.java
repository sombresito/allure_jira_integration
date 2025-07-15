package ru.iopump.qa.allure.security;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configurers.provisioning.InMemoryUserDetailsManagerConfigurer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.ldap.authentication.ad.ActiveDirectoryLdapAuthenticationProvider;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import ru.iopump.qa.allure.properties.BasicProperties;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@Getter
@Setter
@Slf4j
public class BasicConfiguration extends WebSecurityConfigurerAdapter {

    private final BasicProperties basicProperties;

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        PasswordEncoder encoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();

        // 1. Настраиваем локальных пользователей (In-Memory)
        InMemoryUserDetailsManager userDetailsManager = new InMemoryUserDetailsManager();
        for (BasicProperties.User user : basicProperties.getUsers()) {
            userDetailsManager.createUser(
                    org.springframework.security.core.userdetails.User.withUsername(user.getUsername())
                            .password(encoder.encode(user.getPassword()))
                            .roles(user.getRoles())
                            .build()
            );
        }

        DaoAuthenticationProvider inMemoryAuthProvider = new DaoAuthenticationProvider();
        inMemoryAuthProvider.setUserDetailsService(userDetailsManager);
        inMemoryAuthProvider.setPasswordEncoder(encoder);
        auth.authenticationProvider(inMemoryAuthProvider);

        // 2. Настраиваем AD аутентификацию
        ActiveDirectoryLdapAuthenticationProvider adProvider =
                new ActiveDirectoryLdapAuthenticationProvider(
                        "bank.corp.centercredit.kz",
                        "ldap://bcc-dc01.bank.corp.centercredit.kz:389/");
        adProvider.setConvertSubErrorCodesToExceptions(true);
        adProvider.setUseAuthenticationRequestCredentials(true);

        auth.authenticationProvider(adProvider);
    }


    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
                .headers().frameOptions().sameOrigin()
                .and()
                .csrf().disable()
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .authorizeRequests()
//                .antMatchers("/ui/", "/ui/**", "/favicon.ico")
//                .permitAll()
                .antMatchers("/public/**").permitAll()
                .anyRequest().authenticated()
                .and()
                .httpBasic();

    }
}
