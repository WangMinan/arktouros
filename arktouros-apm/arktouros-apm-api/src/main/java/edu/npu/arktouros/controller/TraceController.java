package edu.npu.arktouros.controller;

import edu.npu.arktouros.model.dto.EndPointQueryDto;
import edu.npu.arktouros.model.vo.R;
import edu.npu.arktouros.service.otel.search.SearchService;
import jakarta.annotation.Resource;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author : [wangminan]
 * @description : 链路有关的接口
 */
@RestController
@RequestMapping("/trace")
public class TraceController {
    @Resource
    private SearchService searchService;

    @GetMapping("/endPoints")
    public R getEndPointListByServiceName(@Validated EndPointQueryDto endPointQueryDto) {
        return searchService.getEndPointListByServiceName(endPointQueryDto);
    }

    @GetMapping("/topology")
    public R getSpanTopologyByTraceQuery(@NotEmpty String traceId) {
        return searchService.getSpanTopologyByTraceId(traceId);
    }
}
