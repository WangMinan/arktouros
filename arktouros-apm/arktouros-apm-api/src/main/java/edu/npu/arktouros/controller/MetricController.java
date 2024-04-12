package edu.npu.arktouros.controller;

import edu.npu.arktouros.model.vo.R;
import edu.npu.arktouros.service.otel.search.SearchService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author : [wangminan]
 * @description : 数值有关查询接口
 */
@RestController("/metric")
public class MetricController {

    @Resource
    private SearchService searchService;

    @GetMapping
    public R getMetrics(@RequestParam("limit") int limit) {
        return R.ok();
    }
}
