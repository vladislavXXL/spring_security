package com.example.authorization;

import lombok.extern.log4j.Log4j2;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Log4j2
@SpringBootApplication
public class AuthorizationApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthorizationApplication.class, args);
    }

    @Order(1)
    @Configuration
    @EnableWebSecurity
    class ActuatorSecurityConfiguration extends WebSecurityConfigurerAdapter {
        @Override
        protected void configure(HttpSecurity http) throws Exception {
            http
                    .requestMatcher(EndpointRequest.toAnyEndpoint())
                    .authorizeRequests()
                        .requestMatchers(EndpointRequest.to(HealthEndpoint.class)).permitAll()
                        .anyRequest().authenticated()
                    .and()
                    .httpBasic();
        }
    }

    @Configuration
    @EnableWebSecurity
    class SecurityConfiguration extends WebSecurityConfigurerAdapter {
        @Override
        protected void configure(HttpSecurity http) throws Exception {
            http.csrf().disable();
            http.httpBasic();
            http.authorizeRequests()
                    .mvcMatchers("/root").hasAnyAuthority("ROLE_ADMIN")
                    .mvcMatchers(HttpMethod.GET, "/a").access("hasRole('ROLE_ADMIN')")
                    .mvcMatchers(HttpMethod.POST, "/b").access("@authz.check(request, principal)")
                    .mvcMatchers("/users/{name}").access("#name == principal?.username")
                    .anyRequest().permitAll();
        }
    }

    @Service("authz")
    class AuthService {
        public boolean check(HttpServletRequest request, CustomUser principal) {
            log.info("Checking incoming request: " + request.getRequestURI() + " for principal " + principal.getUsername());
            return true;
        }
    }

    @RestController
    class RootRestController {
        @GetMapping("/root")
        String root() {
            return "root";
        }
    }

    @RestController
    class LetterRestController {
        @GetMapping("/a")
        String a() {
            return "a";
        }

        @PostMapping("/b")
        String b() {
            return "b";
        }

        @GetMapping("/c")
        String c() {
            return "c";
        }
    }

    @RestController
    class UserRestController {
        @GetMapping("/users/{name}")
        String userByName(@PathVariable String name) {
            return "user: " + name;
        }
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Service
    class CustomUserDetailsService implements UserDetailsService {
        private final Map<String, UserDetails> detailsMap = new ConcurrentHashMap<>();

        CustomUserDetailsService() {
            this.detailsMap.put("jlong", new CustomUser("jlong", passwordEncoder().encode("password"), true, "USER"));
            this.detailsMap.put("rwinch", new CustomUser("rwinch", passwordEncoder().encode("password"), true, "USER", "ADMIN"));
        }

        @Override
        public UserDetails loadUserByUsername(String s) throws UsernameNotFoundException {
            if (!this.detailsMap.containsKey(s)) {
                throw new UsernameNotFoundException("Can't find " + s + "!");
            }
            return this.detailsMap.get(s);
        }
    }

    class CustomUser implements UserDetails {
        private final Set<GrantedAuthority> authorities = new HashSet<>();
        private final String userName, password;
        private final boolean active;

        public CustomUser(String userName, String password, boolean active, String... authorities) {
            this.userName = userName;
            this.password = password;
            this.active = active;
            this.authorities.addAll(Arrays.stream(authorities).map(a -> new SimpleGrantedAuthority("ROLE_" + a)).collect(Collectors.toSet()));
        }

        @Override
        public Collection<? extends GrantedAuthority> getAuthorities() {
            return this.authorities;
        }

        @Override
        public String getPassword() {
            return this.password;
        }

        @Override
        public String getUsername() {
            return this.userName;
        }

        @Override
        public boolean isAccountNonExpired() {
            return this.active;
        }

        @Override
        public boolean isAccountNonLocked() {
            return this.active;
        }

        @Override
        public boolean isCredentialsNonExpired() {
            return this.active;
        }

        @Override
        public boolean isEnabled() {
            return this.active;
        }
    }
}
