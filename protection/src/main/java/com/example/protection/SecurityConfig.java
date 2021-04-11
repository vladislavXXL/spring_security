package com.example.protection;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.provisioning.UserDetailsManager;

@Configuration
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        super.configure(http);

        //Cache control vulnerability
//        http.headers().cacheControl().disable();

        //Adds https protocol by default
//        http.headers()
//                .httpStrictTransportSecurity();
//        http.requiresChannel()
//                .requestMatchers(r -> r.getHeader("x-forwarded-proto") != null).requiresSecure();

        //XSS


    }

    @Bean
    UserDetailsManager memory() {
        return new InMemoryUserDetailsManager();
    }

    @Bean
    InitializingBean initializer(UserDetailsManager manager) {
        return () -> {
            UserDetails user = User.withDefaultPasswordEncoder()
                    .username("user")
                    .password("password")
                    .roles("USER")
                    .build();
            manager.createUser(user);
        };
    }
}
