package edu.npu.arktouros.controller;

import edu.npu.arktouros.model.vo.R;
import edu.npu.arktouros.service.otel.search.SearchService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author : [wangminan]
 * @description : Service有关的接口
 */
@RestController
@RequestMapping("/service")
public class ServiceController {

    @Resource
    private SearchService searchService;

    // 获取服务列表
    @GetMapping
    public R getServiceList() {
        return R.ok();
    }
}
