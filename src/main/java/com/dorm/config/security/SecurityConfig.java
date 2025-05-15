package com.dorm.config.security;

import com.dorm.filter.security.DefaultSessionAttributesFilter;
import com.dorm.service.user.UserService;
import com.dorm.service.user.teacher.TeacherService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security 配置类，注意，在使用了它之后，POST 请求需要传递 _csrf 参数
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
        HttpSecurity http,
        UserService userService,
        TeacherService teacherService
    ) throws Exception {
        // 在认证过滤器之后添加自定义过滤器，在 session 中添加默认属性（baseUrl 和用户信息）
        http.addFilterAfter(
            new DefaultSessionAttributesFilter(userService, teacherService),
            UsernamePasswordAuthenticationFilter.class
        );

        http.authorizeHttpRequests(customizer -> customizer
                // 放行接口
                .requestMatchers("/login", "/register", "/api/register", "/api/all-in-one-card/consume-self")
                .permitAll()
                // 放行静态资源
                .requestMatchers("/css/**", "/icons/**", "/images/**", "/js/**", "/vendor/**")
                .permitAll()
                // 其他请求需要认证
                .anyRequest()
                .authenticated()
            )
            // 登录由 spring security 实现，不需要自己实现接口 /api/login
            .formLogin(customizer -> customizer
                .loginPage("/login")
                .loginProcessingUrl("/api/login")
                // 登录成功
                .defaultSuccessUrl("/home")
                // 登录失败
                .failureHandler((request, response, exception) -> {
                    String errorMsg;
                    if (exception instanceof BadCredentialsException) {
                        errorMsg = "用户名或密码错误";
                    } else if (exception instanceof DisabledException) {
                        errorMsg = "账户已禁用";
                    } else {
                        errorMsg = "登录失败";
                    }

                    // 将错误信息存储在session中
                    // 设置一个标志表示这是重定向后的消息
                    request.getSession().setAttribute("msg", errorMsg);
                    request.getSession()
                        .setAttribute("isRedirectMessage", true);

                    // 将错误信息传递到登录页面
                    response.sendRedirect("/login");
                }))
            // 退出登录也是一样
            .logout(customizer -> customizer
                .logoutUrl("/api/logout")
                .logoutSuccessUrl("/login"));
        return http.build();
    }

}
