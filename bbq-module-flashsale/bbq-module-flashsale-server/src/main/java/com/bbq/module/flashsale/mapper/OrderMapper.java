package com.bbq.module.flashsale.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bbq.module.flashsale.entity.OrderInfo;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

public interface OrderMapper extends BaseMapper<OrderInfo> {

    @Update("UPDATE order_info SET status = #{targetStatus} WHERE id = #{orderId} AND status = #{expectedStatus}")
    int updateStatusIfMatch(@Param("orderId") Long orderId,
                            @Param("expectedStatus") int expectedStatus,
                            @Param("targetStatus") int targetStatus);
}
