package com.bbq.module.flashsale.mapper;

import com.github.yulichang.base.MPJBaseMapper;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import com.bbq.module.flashsale.entity.FlashSale;
import com.bbq.module.flashsale.entity.Product;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface FlashSaleMapper extends MPJBaseMapper<FlashSale> {
    default List<FlashSale> listActive(){
        return selectJoinList(FlashSale.class,
                new MPJLambdaWrapper<FlashSale>()
                        .selectAll(FlashSale.class)
                        .selectAs(Product::getName, FlashSale::getProductName)
                        .selectAs(Product::getImage, FlashSale::getProductImage)
                        .selectAs(Product::getPrice, FlashSale::getOriginalPrice)
                        .leftJoin(Product.class, Product::getId, FlashSale::getProductId)
                        .gt(FlashSale::getCount, 0)
                        .orderByAsc(FlashSale::getSaleTime));
    }

    default List<FlashSale> listAll() {
        return selectJoinList(FlashSale.class,
                new MPJLambdaWrapper<FlashSale>()
                        .selectAll(FlashSale.class)
                        .selectAs(Product::getName, FlashSale::getProductName)
                        .selectAs(Product::getImage, FlashSale::getProductImage)
                        .selectAs(Product::getPrice, FlashSale::getOriginalPrice)
                        .leftJoin(Product.class, Product::getId, FlashSale::getProductId)
                        .orderByAsc(FlashSale::getSaleTime));
    }

    @Update("UPDATE flash_sale SET count = count - #{quantity} WHERE product_id = #{productId} AND count >= #{quantity}")
    int decrementStock(@Param("productId") Long productId, @Param("quantity") int quantity);
}
