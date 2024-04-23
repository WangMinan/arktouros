package edu.npu.arktouros.controller;

import edu.npu.arktouros.model.dto.ServiceQueryDto;
import edu.npu.arktouros.model.vo.R;
import edu.npu.arktouros.service.otel.search.SearchService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author : [wangminan]
 * @description : Service有关的接口
 */
@RestController
@RequestMapping("/service")
@Slf4j
public class ServiceController {

    @Resource
    private SearchService searchService;

    // 获取服务列表
    @GetMapping
    public R getServiceList(@Validated ServiceQueryDto query) {
        return searchService.getServiceList(query);
    }

    @GetMapping("/namespace")
    public R getNamespaceList(@RequestParam(value = "query", required = false) String query) {
        return searchService.getNamespaceList(query);
    }

    @GetMapping("/topology")
    public R getTopology(@RequestParam(value = "namespace", required = false) String namespace) {
        if (StringUtils.isEmpty(namespace)) {
            namespace = "default";
        }
        return searchService.getServiceTopology(namespace);
    }
}
