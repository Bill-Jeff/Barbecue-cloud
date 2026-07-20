package com.bbq.module.crm.mapper;


import com.bbq.module.crm.entity.CrmClue;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.List;

/**
 * 接口继承 ElasticsearchRepository，提供基础 CRUD 方法
 */
public interface CrmClueRepository extends ElasticsearchRepository<CrmClue, String> {

    /**
     * 通过方法名自动实现查询：根据名称查找
     *
     * @param mobile
     * @return
     */
    List<CrmClue> findByMobile(String mobile);

    public static void main(String[] args) {
        float initMoney = 190000;
        float targetMoney = 350000;
        float rate = 0.01f;
        int day = 0;
        while (initMoney < targetMoney) {
            initMoney = initMoney + initMoney * rate;
            System.out.println("第 " + day + " 天，余额：" + initMoney+ "，一天赚" + initMoney * rate);
            day ++;
        }
        System.out.println(day + " 天之后5万变1亿");
    }

    List<CrmClue> findByMobileEndingWith(String mobile);
}
