package vn.hoidanit.laptopshop.service;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

//annotation để tự động khởi tạo depenence injection
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserService userService;

    public CustomUserDetailsService(UserService userService) {
        this.userService = userService;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // tại security cũng có class tên là User, import z cho đỡ nhầm
        vn.hoidanit.laptopshop.domain.User user = this.userService.getUserByEmail(username);
        if (user == null) {
            throw new UsernameNotFoundException("User not found");
        }
        //ta đang cần trả về kiểu user trong domain
        //user(spring) là con của UserDetails nên chỉ cần trả về thằng con là đc (tính đa hình)
        //trả về user(spring) với các tham số lấy từ user(domain)
        return new User(
                user.getEmail(),
                user.getPassword(),
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_"+user.getRole().getName())));
        //spring sẽ tự thêm tiền tố "ROLE_" => mình cũng phải thêm vào theo
    }
}
