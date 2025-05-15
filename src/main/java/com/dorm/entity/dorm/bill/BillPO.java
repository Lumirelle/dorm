package com.dorm.entity.dorm.bill;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("dorm_bill")
public class BillPO {

    private Integer id;

    /**
     * 水费账单
     */
    private String waterNo;

    /**
     * 电费账单
     */
    private String electricityNo;

    private Integer dormId;

}
