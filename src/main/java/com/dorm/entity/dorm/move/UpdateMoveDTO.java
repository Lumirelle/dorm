package com.dorm.entity.dorm.move;

import com.dorm.enums.dorm.move.MoveStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateMoveDTO {
    @NotNull(message = "id不能为空")
    private Integer id;

    @NotNull(message = "搬迁状态不能为空")
    private MoveStatus status;

}
