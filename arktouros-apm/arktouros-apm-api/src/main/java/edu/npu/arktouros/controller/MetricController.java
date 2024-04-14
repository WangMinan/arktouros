package edu.npu.arktouros.controller;

import edu.npu.arktouros.model.dto.MetricQueryDto;
import edu.npu.arktouros.model.vo.R;
import edu.npu.arktouros.service.otel.search.SearchService;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author : [wangminan]
 * @description : 数值有关查询接口
 */
@RestController
@RequestMapping("/metric")
public class MetricController {

    @Resource
    private SearchService searchService;

    @GetMapping
    public R getMetrics(@Validated MetricQueryDto metricQueryDto) {
        return searchService.getMetrics(metricQueryDto);
    }
}
