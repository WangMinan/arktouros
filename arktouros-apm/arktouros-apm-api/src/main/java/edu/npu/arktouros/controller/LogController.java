package edu.npu.arktouros.controller;

import edu.npu.arktouros.model.dto.LogQueryDto;
import edu.npu.arktouros.model.vo.R;
import edu.npu.arktouros.service.search.SearchService;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author : [wangminan]
 * @description : 日志有关的接口
 */
@RestController
public class LogController {

    @Resource
    private SearchService searchService;

    @GetMapping("/logs")
    public R getLogList(@Validated LogQueryDto logQueryDto) {
        return searchService.getLogList(logQueryDto);
    }

    @GetMapping("/log/levels")
    public R getAllLogLevels(@RequestParam(value = "query", required = false) String query) {
        return searchService.getAllLogLevels(query);
    }
}
