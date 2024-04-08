package edu.npu.arktouros.controller;

import edu.npu.arktouros.service.otel.search.SearchService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author : [wangminan]
 * @description : 绘图有关的接口
 */
@RestController
@RequestMapping("/topology")
public class TopologyController {

    @Resource
    private SearchService searchService;
}
