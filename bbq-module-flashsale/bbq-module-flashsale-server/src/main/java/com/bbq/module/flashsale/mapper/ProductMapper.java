package com.bbq.module.flashsale.mapper;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bbq.module.flashsale.entity.Product;

import java.util.List;

public interface ProductMapper extends BaseMapper<Product> {

    default List<Product> getList(List<Long> ids) {
        return selectList(new QueryWrapper<Product>().in("id", ids));
    }
}
