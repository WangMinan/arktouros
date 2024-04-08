package edu.npu.arktouros.controller;

import edu.npu.arktouros.service.otel.search.SearchService;
import jakarta.annotation.Resource;
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
}
