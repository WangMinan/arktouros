package edu.npu.arktouros.service.otel.search;

import edu.npu.arktouros.mapper.otel.search.SearchMapper;
import edu.npu.arktouros.model.common.ResponseCodeEnum;
import edu.npu.arktouros.model.dto.BaseQueryDto;
import edu.npu.arktouros.model.vo.R;
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


    @Override
    public R getServiceList(BaseQueryDto queryDto) {
        R r = new R();
        r.put("code", ResponseCodeEnum.SUCCESS);
        r.put("result", searchMapper.getServiceList(queryDto));
        return r;
    }
}
