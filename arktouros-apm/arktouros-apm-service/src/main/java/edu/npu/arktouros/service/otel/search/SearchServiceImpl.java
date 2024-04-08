package edu.npu.arktouros.service.otel.search;

import edu.npu.arktouros.mapper.otel.search.SearchMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

/**
 * @author : [wangminan]
 * @description : 搜索服务
 */
@Service
public class SearchServiceImpl implements SearchService {

    @Resource
    private SearchMapper searchMapper;
}
