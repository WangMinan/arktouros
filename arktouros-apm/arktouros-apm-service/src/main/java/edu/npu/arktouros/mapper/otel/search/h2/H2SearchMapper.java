package edu.npu.arktouros.mapper.otel.search.h2;

import edu.npu.arktouros.mapper.otel.search.SearchMapper;
import edu.npu.arktouros.model.dto.BaseQueryDto;
import edu.npu.arktouros.model.vo.R;

/**
 * @author : [wangminan]
 * @description : H2搜索Mapper
 */
public class H2SearchMapper extends SearchMapper {
    @Override
    public R getServiceList(BaseQueryDto queryDto) {
        return R.ok();
    }
}
