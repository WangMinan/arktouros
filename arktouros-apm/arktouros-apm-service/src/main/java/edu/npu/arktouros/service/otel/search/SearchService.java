package edu.npu.arktouros.service.otel.search;

import edu.npu.arktouros.model.dto.BaseQueryDto;
import edu.npu.arktouros.model.vo.R;

public interface SearchService {
    R getServiceList(BaseQueryDto queryDto);
}
