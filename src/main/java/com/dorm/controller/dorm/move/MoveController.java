package com.dorm.controller.dorm.move;


import cn.hutool.core.date.DateTime;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.dorm.entity.dorm.DormPO;
import com.dorm.entity.dorm.DormVO;
import com.dorm.entity.dorm.move.AddMoveDTO;
import com.dorm.entity.dorm.move.MovePO;
import com.dorm.entity.dorm.move.MoveVO;
import com.dorm.entity.dorm.move.QueryMoveDTO;
import com.dorm.entity.dorm.move.UpdateMoveDTO;
import com.dorm.entity.user.UserVO;
import com.dorm.entity.user.student.StudentPO;
import com.dorm.entity.user.student.StudentVO;
import com.dorm.entity.user.teacher.TeacherPO;
import com.dorm.enums.dorm.DormStatus;
import com.dorm.enums.dorm.move.MoveStatus;
import com.dorm.enums.dorm.move.MoveType;
import com.dorm.enums.user.UserRoles;
import com.dorm.enums.user.teacher.TeacherType;
import com.dorm.service.dorm.DormService;
import com.dorm.service.dorm.move.MoveService;
import com.dorm.service.user.student.StudentService;
import com.dorm.service.user.teacher.TeacherService;
import com.dorm.utils.IdListUtils;
import com.dorm.utils.SecurityUtils;
import com.dorm.utils.UploadUtils;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Controller
public class MoveController {
    @Resource
    private MoveService moveService;

    @Resource
    private StudentService studentService;

    @Resource
    private DormService dormService;

    @Resource
    private UploadUtils uploadUtils;
    @Autowired
    private SecurityUtils securityUtils;
    @Autowired
    private TeacherService teacherService;

    @RequestMapping("/move/list")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER', 'STUDENT')")
    public String showMoveListPage(
        @RequestParam(name = "pageNum", defaultValue = "1", required = false) Integer pageNum,
        @RequestParam(name = "pageSize", defaultValue = "15", required = false) Integer pageSize,
        QueryMoveDTO moveDTO,
        Model model
    ) {
        // 分页
        if (pageNum < 0) {
            pageNum = 1;
        }
        if (pageSize < 0) {
            pageSize = 15;
        }
        List<MovePO> movePOList;
        try (Page<MovePO> ignored = PageHelper.startPage(pageNum, pageSize)) {
            if (Strings.isNotBlank(moveDTO.getName())) {
                movePOList = moveService.listMoveByStudentName(moveDTO.getName());
            } else {
                movePOList = moveService.list();
            }
        }

        // 处理 MovePO -> MoveVO
        // 返回搬迁数据
        List<MoveVO> moves = new ArrayList<>();
        for (MovePO movePO : movePOList) {
            StudentPO studentPO = studentService.getById(movePO.getStudentId());
            DormPO fromDormPO = dormService.getById(movePO.getFromDormId());
            DormPO toDormPO = dormService.getById(movePO.getToDormId());
            // 构造 MoveVO 信息
            MoveVO student = MoveVO.valueOf(movePO, studentPO, fromDormPO, toDormPO);
            moves.add(student);
        }

        // 如果是学生，筛选出自己的搬迁申请
        UserVO user = securityUtils.getCurrentUser();
        StudentVO userStudent = null;
        if (user.getRole() == UserRoles.STUDENT) {
            StudentPO studentPO = studentService.getOne(new QueryWrapper<>(StudentPO.class).eq(
                "user_id", user.getId()));
            DormPO dormPO = dormService.getById(studentPO.getDormId());
            userStudent = StudentVO.valueOf(studentPO, dormPO);
            if (studentPO != null) {
                moves = moves.stream().filter(i -> i.getStudentId().equals(studentPO.getId())).toList();
            } else {
                moves = new ArrayList<>();
            }
        }

        PageInfo<MoveVO> pageInfo = new PageInfo<>(moves);
        model.addAttribute("pageInfo", pageInfo);

        // 回显查询条件
        model.addAttribute("name", moveDTO.getName());

        // FIXME: 额外的学生信息和宿舍信息，用来在添加搬迁时选择
        if (user.getRole() == UserRoles.ADMIN || user.getRole() == UserRoles.TEACHER) {
            List<StudentVO> students = new ArrayList<>();
            List<StudentPO> studentPOList = studentService.list();
            for (StudentPO studentPO1 : studentPOList) {
                // 获取宿舍信息
                DormPO dormPO = dormService.getById(studentPO1.getDormId());
                StudentVO student = StudentVO.valueOf(studentPO1, dormPO);
                students.add(student);
            }
            model.addAttribute("students", students);
        } else if (user.getRole() == UserRoles.STUDENT) {
            model.addAttribute("userStudent", userStudent);
        }

        List<DormVO> dorms = DormVO.valuesOf(dormService.list());
        model.addAttribute("dorms", dorms);

        return "move/list";
    }

    @RequestMapping("/api/move/add")
    @PreAuthorize("hasAnyRole('ADMIN', 'STUDENT')")
    public String addMove(
        @ModelAttribute @Validated AddMoveDTO moveDTO,
        BindingResult bindingResult,
        @RequestParam MultipartFile file,
        RedirectAttributes redirectAttributes
    ) {
        String url = "redirect:/move/list";

        // 基本参数检验
        if (bindingResult.hasErrors() && bindingResult.getFieldError() != null) {
            redirectAttributes.addFlashAttribute("msg", bindingResult.getFieldError().getDefaultMessage());
            return url;
        }

        // 学生 ID 检验
        StudentPO studentPO = studentService.getById(moveDTO.getStudentId());
        if (studentPO == null) {
            redirectAttributes.addFlashAttribute("msg", "学生 ID 不存在");
            return url;
        }

        // 如果学生有未结束的搬迁记录，则不能再提交新的搬迁申请
        List<MovePO> studentMoves = moveService.list(new QueryWrapper<>(MovePO.class).eq(
            "student_id", moveDTO.getStudentId()));
        if (studentMoves.stream()
            .anyMatch(i -> i.getStatus() != MoveStatus.PASS && i.getStatus() != MoveStatus.REJECT && i.getStatus() != MoveStatus.CANCELLED)) {
            redirectAttributes.addFlashAttribute("msg", "学生仍有未处理的搬迁记录");
            return url;
        }

        // 宿舍 ID 检验（）
        if (Objects.equals(studentPO.getDormId(), moveDTO.getToDormId())) {
            redirectAttributes.addFlashAttribute("msg", "来源和迁往宿舍 ID 不能相同");
            return url;
        }

        try {
            MovePO movePO = MovePO.valueOf(moveDTO);
            // 学生信息中的宿舍 ID 就是来源宿舍 ID
            movePO.setFromDormId(studentPO.getDormId());
            // 上传证明文件
            movePO.setProve(uploadUtils.uploadFile(file));
            // 添加搬迁记录时默认设置为待辅导员审核状态，同时设置创建时间
            movePO.setStatus(MoveStatus.WAIT_INSTRUCTOR_AUDIT);
            movePO.setCreateTime(new DateTime());
            moveService.save(movePO);
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("msg", "上传图片失败");
            return url;
        }

        return url;
    }

    @RequestMapping("/move/update/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public String showMoveUpdatePage(@PathVariable Integer id, RedirectAttributes redirectAttributes, Model model) {
        String notExistUrl = "redirect:/move/list";

        // 获取搬迁信息
        MovePO movePO = moveService.getById(id);

        if (movePO == null) {
            redirectAttributes.addFlashAttribute("msg", "搬迁记录不存在");
            return notExistUrl;
        }

        // 获取当前用户
        UserVO user = securityUtils.getCurrentUser();

        // 如果是换宿舍，只有管理员和辅导员可以修改
        if (
            movePO.getType() == MoveType.CHANGE
                && user.getRole() != UserRoles.ADMIN
                && (user.getRole() != UserRoles.TEACHER || securityUtils.getTeacherType() != TeacherType.INSTRUCTOR)
        ) {
            redirectAttributes.addFlashAttribute("msg", "只有管理员和辅导员可以修改换宿舍申请");
            return notExistUrl;
        }

        // 如果是其他的，则检验是否进入本人审核阶段
        if (user.getRole() == UserRoles.TEACHER) {
            // 是辅导员
            if (securityUtils.getTeacherType() == TeacherType.INSTRUCTOR && movePO.getStatus() != MoveStatus.WAIT_INSTRUCTOR_AUDIT) {
                redirectAttributes.addFlashAttribute("msg", "只能审核待辅导员审核的搬迁记录");
                return notExistUrl;
            }
            // 是学办
            if (securityUtils.getTeacherType() == TeacherType.OFFICE && movePO.getStatus() != MoveStatus.WAIT_OFFICE_AUDIT) {
                redirectAttributes.addFlashAttribute("msg", "只能审核待学办审核的搬迁记录");
                return notExistUrl;
            }
            // 是学工部
            if (securityUtils.getTeacherType() == TeacherType.AFFAIRS && movePO.getStatus() != MoveStatus.WAIT_AFFAIRS_AUDIT) {
                redirectAttributes.addFlashAttribute("msg", "只能审核待学工部审核的搬迁记录");
                return notExistUrl;
            }
        }

        StudentPO studentPO = studentService.getById(movePO.getStudentId());
        DormPO fromDormPO = dormService.getById(movePO.getFromDormId());
        DormPO toDormPO = dormService.getById(movePO.getToDormId());
        MoveVO move = MoveVO.valueOf(movePO, studentPO, fromDormPO, toDormPO);
        model.addAttribute("move", move);

        return "move/update";
    }

    @RequestMapping("/api/move/update")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public String updateMove(
        @ModelAttribute @Validated UpdateMoveDTO moveDTO,
        BindingResult bindingResult,
        RedirectAttributes redirectAttributes
    ) {
        String successUrl = "redirect:/move/list";
        String errorUrl = "redirect:/move/update/" + moveDTO.getId();

        // 基本参数检验
        if (bindingResult.hasErrors() && bindingResult.getFieldError() != null) {
            redirectAttributes.addFlashAttribute("msg", bindingResult.getFieldError().getDefaultMessage());
            return errorUrl;
        }

        // ID 检验
        MovePO oldMove = moveService.getById(moveDTO.getId());
        if (oldMove == null) {
            redirectAttributes.addFlashAttribute("msg", "搬迁不存在");
            return errorUrl;
        }

        // 学生 ID 检验
        StudentPO studentPO = studentService.getById(oldMove.getStudentId());
        if (studentPO == null) {
            redirectAttributes.addFlashAttribute("msg", "学生 ID 不存在");
            return errorUrl;
        }

        // 获取当前用户
        UserVO user = securityUtils.getCurrentUser();

        // 如果是换宿舍，只有管理员和辅导员可以修改
        if (
            oldMove.getType() == MoveType.CHANGE
                && user.getRole() != UserRoles.ADMIN
                && (user.getRole() != UserRoles.TEACHER || securityUtils.getTeacherType() != TeacherType.INSTRUCTOR)
        ) {
            redirectAttributes.addFlashAttribute("msg", "只有管理员和辅导员可以修改换宿舍申请");
            return errorUrl;
        }

        // 如果是教师，则检验是否进入本人审核阶段
        if (user.getRole() == UserRoles.TEACHER) {
            TeacherPO teacherPO = teacherService.getOne(new QueryWrapper<>(TeacherPO.class).eq(
                "user_id", user.getId()));
            if (teacherPO == null) {
                redirectAttributes.addFlashAttribute("msg", "非法教师身份");
                return errorUrl;
            }
            // 是辅导员
            if (teacherPO.getTeacherType() == TeacherType.INSTRUCTOR && oldMove.getStatus() != MoveStatus.WAIT_INSTRUCTOR_AUDIT) {
                redirectAttributes.addFlashAttribute("msg", "只能审核待辅导员审核的搬迁记录");
                return errorUrl;
            }
            // 是学办
            if (teacherPO.getTeacherType() == TeacherType.OFFICE && oldMove.getStatus() != MoveStatus.WAIT_OFFICE_AUDIT) {
                redirectAttributes.addFlashAttribute("msg", "只能审核待学办审核的搬迁记录");
                return errorUrl;
            }
            // 是学工部
            if (teacherPO.getTeacherType() == TeacherType.AFFAIRS && oldMove.getStatus() != MoveStatus.WAIT_AFFAIRS_AUDIT) {
                redirectAttributes.addFlashAttribute("msg", "只能审核待学工部审核的搬迁记录");
                return errorUrl;
            }
        }

        // 更新搬迁信息
        UpdateWrapper<MovePO> uw = new UpdateWrapper<>();
        uw.eq("id", moveDTO.getId());
        uw.set("status", moveDTO.getStatus());
        // 设置更新时间
        uw.set("update_time", new DateTime());
        moveService.update(uw);

        // 如果搬迁状态为通过，则更新宿舍和学生状态
        if (moveDTO.getStatus().equals(MoveStatus.PASS)) {
            // 更新宿舍状态
            DormPO fromDorm = dormService.getById(oldMove.getFromDormId());
            if (fromDorm != null) {
                fromDorm.decreaseSetting();
                fromDorm.setStatus(DormStatus.FREE);
                dormService.updateById(fromDorm);
            }

            DormPO toDorm = dormService.getById(oldMove.getToDormId());
            if (toDorm != null) {
                toDorm.increaseSetting();
                if (Objects.equals(toDorm.getSetting(), toDorm.getPeople())) {
                    toDorm.setStatus(DormStatus.FULL);
                }
                dormService.updateById(toDorm);
            }

            // 更新学生状态
            studentPO.setDormId(oldMove.getToDormId());
            studentService.updateById(studentPO);
        }

        return successUrl;
    }

    @RequestMapping("/api/move/cancel/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STUDENT')")
    public String cancelMove(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        MovePO movePO = moveService.getById(id);

        if (movePO == null) {
            redirectAttributes.addFlashAttribute("msg", "搬迁记录不存在");
            return "redirect:/move/list";
        }
        if (movePO.getStatus() == MoveStatus.PASS || movePO.getStatus() == MoveStatus.REJECT || movePO.getStatus() == MoveStatus.CANCELLED) {
            redirectAttributes.addFlashAttribute("msg", "搬迁记录已不可撤销");
            return "redirect:/move/list";
        }

        // 取消搬迁
        movePO.setStatus(MoveStatus.CANCELLED);
        movePO.setUpdateTime(new DateTime());
        moveService.updateById(movePO);

        return "redirect:/move/list";
    }

    @ResponseBody
    @RequestMapping("/api/move/batchCancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'STUDENT')")
    public String batchCancelMove(String idList) {
        List<Integer> list = IdListUtils.convertToIntegerList(idList);

        if (moveService.isAnyIdNotExist(list)) {
            return "某个 ID 不存在";
        }
        if (moveService.isAnyIdNotCancelable(list)) {
            return "某个 ID 的搬迁记录已不可撤销";
        }

        boolean flag = true;
        for (Integer id : list) {
            // 取消搬迁
            MovePO movePO = moveService.getById(id);
            movePO.setStatus(MoveStatus.CANCELLED);
            movePO.setUpdateTime(new DateTime());
            flag = flag & moveService.updateById(movePO);
        }

        if (!flag) {
            return "批量撤销部分失败";
        } else {
            return "OK";
        }
    }

}
