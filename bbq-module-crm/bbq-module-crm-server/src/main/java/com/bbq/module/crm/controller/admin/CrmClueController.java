package com.bbq.module.crm.controller.admin;

import com.bbq.framework.common.pojo.CommonResult;
import com.bbq.module.crm.entity.CrmClue;
import com.bbq.module.crm.service.CrmClueService;
import jakarta.annotation.security.PermitAll;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/clue/api")
@RequiredArgsConstructor
public class CrmClueController {

    private final CrmClueService crmClueService;
    @PostMapping("/save")
    @PermitAll
    public CommonResult<CrmClue> save(@RequestBody CrmClue clue) {
        return CommonResult.success(crmClueService.saveClue(clue));
    }

    @PostMapping("/get")
    @PermitAll
    public CommonResult<List<CrmClue>> get(@RequestBody CrmClue clue) {
        return CommonResult.success(crmClueService.getClue(clue));
    }

    @PostMapping("/get-4mobile")
    @PermitAll
    public CommonResult<List<CrmClue>> get4Mobile(@RequestBody CrmClue clue) {
        return CommonResult.success(crmClueService.getClueEndingWith(clue));
    }

}
