package com.bbq.module.crm.service;

import com.bbq.module.crm.entity.CrmClue;
import com.bbq.module.crm.mapper.CrmClueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CrmClueService {

    private final CrmClueRepository crmClueRepository;

    public CrmClue saveClue(CrmClue clue) {
        return crmClueRepository.save(clue);
    }

    public List<CrmClue> getClue(CrmClue clue) {
        return crmClueRepository.findByMobile(clue.getMobile());
    }

    public List<CrmClue> getClueEndingWith(CrmClue clue) {
        return crmCl3ueRepository.findByMobileEndingWith(clue.getMobile());
    }
}
