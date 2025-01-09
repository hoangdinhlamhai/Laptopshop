package vn.hoidanit.laptopshop.config;

import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.session.security.web.authentication.SpringSessionRememberMeServices;
import vn.hoidanit.laptopshop.service.CustomUserDetailsService;
import vn.hoidanit.laptopshop.service.UserService;

@Configuration
@EnableMethodSecurity(securedEnabled = true)
public class SecurityConfiguration {
    @Bean
    public PasswordEncoder PasswordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService(UserService userService) {
        return new CustomUserDetailsService(userService);
    }

    @Bean
    public DaoAuthenticationProvider authProvider(
            PasswordEncoder passwordEncoder,
            UserDetailsService userDetailsService) {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder);
//        authProvider.setHideUserNotFoundExceptions(false); ẩn đi thông báo user not found của security
        return authProvider;
    }

    //ghi đè lại những thứ có trong Interface "AuthenticationSuccessHandler" của spring sau đó gọi nó ở line 79
    @Bean
    public AuthenticationSuccessHandler customSuccessHandler() {
        return new CustomSuccessHandler();
    }

    //tích hợp remember me (mỗi lần thoát trình duyệt sẽ ko bị logout)
    @Bean
    public SpringSessionRememberMeServices rememberMeServices() {
        SpringSessionRememberMeServices rememberMeServices =
                new SpringSessionRememberMeServices();
        // optionally customize
        rememberMeServices.setAlwaysRemember(true);
        return rememberMeServices;
    }

    //phân quyền ng dùng và hơn thế nữa
    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // ver6 lambda
        //từ v6.0, Spring auto “validate” forward request, có nghĩa là với spring MVC, các
        //request tới view (jsp) sẽ bị check.

        // chạy tới "/login" -> return về đường link (sẽ bị chặn bởi spring ver6.0)
        http
                .authorizeHttpRequests(authorize -> authorize
                        // mặc định spring sẽ chặn 2 type FORWARD và INCLUDE
                        // INCLUDE là việc kèm theo thông tin của bên khác (ở đây là có thêm th/tin của service)
                        // ctrl click DispatcherType để xem chi tiết
                        .dispatcherTypeMatchers(DispatcherType.FORWARD,
                                DispatcherType.INCLUDE).permitAll()
                        // ở controller sau khi truy cập vào trang chủ ("/") có truy cập vào service -> spring sẽ chặn(INCLUDE) -> ko truy cập được vào trang chủ
                        // nên DispatcherType.INCLUDE để gỡ bỏ việc chặn
                        .requestMatchers("/", "/login", "/product/**", "/register", "/product/**",
                                "/client/**", "/css/**", "/js/**", "/images/**").permitAll()
                        // thêm "/" thì khi vào trang chủ sẽ ko bị đá về trang login
                        // nếu ko thêm thì khi load trang sẽ không tìm thấy file đó
                        // .permitAll() cho phép tất cả loại tài khoản (admin/user) => tức là khi vào các link trên thì sẽ không bị đá về trang login để xác thực

                        .requestMatchers("/admin/**").hasRole("ADMIN") //những tk có role là ADMIN mới được truy cập vào link /admin/...
                        //hàm hasRole sẽ tự động bỏ tiền tố "ROLE_" => phải thêm vào "ROLE_" (line 35 file CustomUserDetailsService)

                        .anyRequest().authenticated())

                .sessionManagement((sessionManagement) -> sessionManagement
                        .sessionCreationPolicy(SessionCreationPolicy.ALWAYS) //luôn tạo session mới nếu người dùng chưa có session
                        .invalidSessionUrl("/logout?expired")//hết hạn session -> tự động logout
                        .maximumSessions(1) //1 thời điểm chỉ có 1 người được log vào
                        .maxSessionsPreventsLogin(false))//khi người sau log vào thì sẽ đá người trước ra

                .logout(logout -> logout.deleteCookies("JSESSIONID").invalidateHttpSession(true)) //mỗi khi logout thì xoá cookie và báo hiệu session đã hết hạn

                //include remember me (kết hợp cấu hình session ở file application.properties line 23)
                .rememberMe(r -> r.rememberMeServices(rememberMeServices()))

                .formLogin(formLogin -> formLogin
                        .loginPage("/login")
                        .failureUrl("/login?error")
                        .successHandler(customSuccessHandler())
                        .permitAll())
                // khi log vào gặp lỗi 403 forbiden thì dùng
                .exceptionHandling(ex -> ex.accessDeniedPage("/access-deny"));
        return http.build();
    }
}
