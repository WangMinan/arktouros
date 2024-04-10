package edu.npu.arktouros.controller;

import edu.npu.arktouros.model.vo.R;
import edu.npu.arktouros.service.otel.search.SearchService;
import jakarta.annotation.Resource;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

    @GetMapping
    public R getTopology(@NotBlank @RequestParam("namespace") String namespace) {
        return searchService.getTopology(namespace);
    }
}
