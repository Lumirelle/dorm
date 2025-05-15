package com.dorm.controller.notice;


import com.dorm.entity.notice.AddNoticeDTO;
import com.dorm.entity.notice.NoticePO;
import com.dorm.entity.notice.NoticeVO;
import com.dorm.entity.user.UserPO;
import com.dorm.service.notice.NoticeService;
import com.dorm.service.user.UserService;
import jakarta.annotation.Resource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Controller
public class NoticeController {
    @Resource
    private NoticeService noticeService;

    @Resource
    private UserService userService;

    @RequestMapping("/notice/list")
    public String listNotice(Model model) {
        List<NoticePO> noticePOList = noticeService.listByCreateTimeDesc();
        List<NoticeVO> notices = new ArrayList<>();
        for (NoticePO noticePO : noticePOList) {
            UserPO userPO = userService.getById(noticePO.getUserId());
            NoticeVO noticeVO = NoticeVO.valueOf(noticePO, userPO);
            notices.add(noticeVO);
        }
        model.addAttribute("notices", notices);
        return "notice/list";
    }

    @RequestMapping("/api/notice/add")
    @PreAuthorize("hasRole('ADMIN')")
    public String addNotice(
            @ModelAttribute @Validated AddNoticeDTO noticeDTO,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes
    ) {
        String url = "redirect:/notice/list";

        // 基本参数检验
        if (bindingResult.hasErrors() && bindingResult.getFieldError() != null) {
            redirectAttributes.addFlashAttribute("msg", bindingResult.getFieldError().getDefaultMessage());
            return url;
        }

        // 校验用户是否存在
        UserPO userPO = userService.getById(noticeDTO.getUserId());
        if (userPO == null) {
            redirectAttributes.addFlashAttribute("msg", "用户不存在");
            return url;
        }

        NoticePO noticePO = NoticePO.valueOf(noticeDTO);
        noticePO.setCreateTime(new Date());
        noticeService.save(noticePO);

        return url;
    }

    @RequestMapping("/api/notice/delete/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String deleteNotice(@PathVariable Integer id) {
        noticeService.removeById(id);
        return "redirect:/notice/list";
    }
}
