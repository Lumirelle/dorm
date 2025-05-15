package com.dorm.controller.statistics;

import com.dorm.entity.BarChartColumn;
import com.dorm.entity.dorm.BuildingSettingVO;
import com.dorm.service.dorm.DormService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("main")
public class StatisticController {

    @Resource
    private DormService dormService;

    @RequestMapping("mainMenu")
    public List<BarChartColumn> mainMenu() {
        // 每栋楼的人数
        List<BuildingSettingVO> peoplePerBuild = dormService.countPeoplePerBuild();

        // 设置到柱状图的列里
        List<BarChartColumn> columns = new ArrayList<>();
        for (BuildingSettingVO peopleNumber : peoplePerBuild) {
            BarChartColumn column = new BarChartColumn();
            //楼栋的名字
            column.setType(peopleNumber.getBuilding());
            column.setMount(Integer.valueOf(peopleNumber.getSetting()));
            columns.add(column);
        }
        return columns;
    }
}
