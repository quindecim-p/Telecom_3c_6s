package telecom.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import telecom.enums.RoleType;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final UserDetailsService userDetailsService;

    @Autowired
    public SecurityConfig(UserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    /**
     * Бин для шифрования паролей.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Бин для настройки провайдера аутентификации.
     */
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationSuccessHandler customAuthenticationSuccessHandler() {
        return new CustomAuthenticationSuccessHandler();
    }


    /**
     * Основная конфигурация цепочки фильтров безопасности.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(authz -> authz
                        .requestMatchers("/css/**").permitAll() // Разрешить доступ к статике
                        .requestMatchers("/", "/home", "/register", "/login").permitAll() // Разрешить доступ к этим страницам всем
                        .requestMatchers("/services/**").hasRole(RoleType.SUBSCRIBER.name()) // Доступ абонентам
                        .requestMatchers("/operator/**").hasAnyRole(RoleType.OPERATOR.name()) // Доступ операторам
                        .requestMatchers("/admin/**").hasRole(RoleType.ADMIN.name()) // Доступ только админам
                        .anyRequest().authenticated() // Все остальные запросы требуют аутентификации
                )
                .formLogin(form -> form
                        .loginPage("/login") // Страница логина
                        .loginProcessingUrl("/login") // URL, куда отправляется форма логина (обрабатывается Spring Security)
                        .successHandler(customAuthenticationSuccessHandler()) // Куда редиректить после успешного логина
                        .failureUrl("/login?error=true") // Куда редиректить при ошибке логина
                        .permitAll() // Разрешить доступ к странице логина всем
                )
                .logout(logout -> logout
                        .logoutRequestMatcher(new AntPathRequestMatcher("/logout")) // URL для выхода
                        .logoutSuccessUrl("/login?logout=true") // Куда редиректить после выхода
                        .invalidateHttpSession(true) // Сделать сессию невалидной
                        .deleteCookies("JSESSIONID") // Удалить куки сессии
                        .permitAll() // Разрешить выход всем
                );

        return http.build();
    }
}