package com.dorm.entity.user.student;

import com.dorm.enums.user.UserSex;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class UpdateStudentDTO {
    @NotNull(message = "id不能为空")
    private Integer id;

    @NotBlank(message = "姓名不能为空")
    private String name;

    @NotNull(message = "性别不能为空")
    private UserSex sex;

    @Positive(message = "年龄必须为正数")
    private Integer age;

    @NotBlank(message = "专业不能为空")
    private String major;

    @NotBlank(message = "学院不能为空")
    private String college;

    // 和用户关联
    @NotNull(message = "用户不能为空")
    private Integer userId;

    // 和宿舍关联
    @NotNull(message = "用户不能为空")
    private Integer dormId;
}
