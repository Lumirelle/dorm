package com.dorm.filter.security;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.dorm.entity.user.UserPO;
import com.dorm.entity.user.UserVO;
import com.dorm.entity.user.teacher.TeacherPO;
import com.dorm.entity.user.teacher.TeacherVO;
import com.dorm.enums.user.UserRoles;
import com.dorm.enums.user.teacher.TeacherType;
import com.dorm.service.user.UserService;
import com.dorm.service.user.teacher.TeacherService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@RequiredArgsConstructor
public class DefaultSessionAttributesFilter extends OncePerRequestFilter {

    private final UserService userService;

    private final TeacherService teacherService;

    @Override
    protected void doFilterInternal(
        @NonNull HttpServletRequest request,
        @NonNull HttpServletResponse response,
        @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.isAuthenticated() &&
            authentication.getPrincipal() instanceof UserDetails userDetails) {

            // 设置baseUrl
            String baseUrl = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort();
            request.getSession().setAttribute("baseUrl", baseUrl);

            // 设置用户信息
            QueryWrapper<UserPO> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("username", userDetails.getUsername());
            UserPO userPO = userService.getOne(queryWrapper);
            UserVO user = UserVO.valueOf(userPO);
            request.getSession().setAttribute("user", user);

            // 如果是教师，设置教师类型信息
            if (user.getRole().equals(UserRoles.TEACHER)) {
                TeacherPO teacherPO = teacherService.getOne(new QueryWrapper<TeacherPO>().eq("user_id", user.getId()));
                if (teacherPO != null) {
                    request.getSession().setAttribute("teacherType", teacherPO.getTeacherType());
                } else {
                    request.getSession().setAttribute("teacherType", TeacherType.UNSET);
                }
            }
        }

        filterChain.doFilter(request, response);
    }
}
