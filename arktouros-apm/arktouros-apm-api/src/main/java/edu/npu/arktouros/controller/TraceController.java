package edu.npu.arktouros.controller;

import edu.npu.arktouros.model.dto.EndPointQueryDto;
import edu.npu.arktouros.model.dto.SpanNamesQueryDto;
import edu.npu.arktouros.model.dto.SpanTimesQueryDto;
import edu.npu.arktouros.model.dto.SpanTopologyQueryDto;
import edu.npu.arktouros.model.vo.R;
import edu.npu.arktouros.service.operation.DataOperationService;
import edu.npu.arktouros.service.search.SearchService;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
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

    @Resource
    private DataOperationService dataOperationService;

    @GetMapping("/endPoints")
    public R getEndPointListByServiceName(@Validated EndPointQueryDto endPointQueryDto) {
        return searchService.getEndPointListByServiceName(endPointQueryDto);
    }

    @GetMapping("/topology")
    public R getSpanTopologyByTraceQuery(@Validated SpanTopologyQueryDto spanTopologyQueryDto) {
        return searchService.getSpanTopologyByTraceId(spanTopologyQueryDto);
    }

    @GetMapping("/spanNames")
    public R getSpanNamesByServiceName(@Validated SpanNamesQueryDto spanNamesQueryDto) {
        return searchService.getSpanNamesByServiceName(spanNamesQueryDto);
    }

    @GetMapping("/times")
    public R getSpanTimesBySpanName(@Validated SpanTimesQueryDto spanTimesQueryDto) {
        return searchService.getSpanTimesBySpanName(spanTimesQueryDto);
    }

    @DeleteMapping("/spans")
    public R deleteAllSpans() {
        dataOperationService.deleteAllSpans();
        return R.ok();
    }
}
