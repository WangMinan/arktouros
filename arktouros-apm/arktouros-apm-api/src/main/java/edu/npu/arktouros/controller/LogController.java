package edu.npu.arktouros.controller;

import edu.npu.arktouros.model.dto.LogQueryDto;
import edu.npu.arktouros.model.vo.R;
import edu.npu.arktouros.service.otel.search.SearchService;
import jakarta.annotation.Resource;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author : [wangminan]
 * @description : 日志有关的接口
 */
@RestController
@RequestMapping("/log")
public class LogController {

    @Resource
    private SearchService searchService;

    @GetMapping
    public R getLogList(@Validated LogQueryDto logQueryDto) {
        return searchService.getLogList(logQueryDto);
    }

    @GetMapping("/{id}")
    public R getLogById(@PathVariable @NotEmpty String id) {
        return searchService.getLogById(id);
    }
}
