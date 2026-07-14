package com.bbq.module.flashsale.mapper;

import com.bbq.module.flashsale.entity.FlashSale;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.List;

/**
 * 接口继承 ElasticsearchRepository，提供基础 CRUD 方法
 */
public interface FlashSaleRepository extends ElasticsearchRepository<FlashSale, String> {

    /**
     * 通过方法名自动实现查询：根据名称查找
     *
     * @param name
     * @return
     */
    List<FlashSale> findByName(String name);

    /**
     * 根据名称和价格范围查找
     *
     * @param name
     * @param minPrice
     * @param maxPrice
     * @return
     */
    List<FlashSale> findByNameAndPriceBetween(String name, double minPrice, double maxPrice);

}
