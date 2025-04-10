package edu.npu.arktouros.controller;

import edu.npu.arktouros.model.vo.R;
import edu.npu.arktouros.service.operation.DataOperationService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author : [wangminan]
 * @description : 其余通用接口
 */
@RestController
public class CommonController {
    @Resource
    private DataOperationService dataOperationService;

    @DeleteMapping("/all")
    public R deleteAllData() {
        dataOperationService.deleteAllData();
        return R.ok();
    }
}
