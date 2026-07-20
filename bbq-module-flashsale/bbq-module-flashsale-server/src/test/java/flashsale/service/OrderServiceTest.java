package flashsale.service;


import com.bbq.module.flashsale.FlashSaleApplication;
import com.bbq.module.flashsale.entity.OrderInfo;
import com.bbq.module.flashsale.mapper.OrderMapper;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.Random;

@SpringBootTest(classes = FlashSaleApplication.class)
public class OrderServiceTest {
    private static final Random random = new Random();
    private static final int CHINESE_START = 0x4E00;
    private static final int CHINESE_END = 0x9FFF;
    @Resource
    private OrderMapper orderInfoMapper;

    @Test
    public void saveOrder() {
        // 生成一个长度为 3 的随机中文名
        String randomName = randomChineseString(3);
        // 执行测试...
        System.out.println("生成的中文名：" + randomName);
        ArrayList<OrderInfo> list = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            OrderInfo orderInfo = new OrderInfo();
            orderInfo.setTableNo(randomName);
            System.out.println(randomChineseString(3));
            list.add(orderInfo);
        }
        orderInfoMapper.insert(list);
        System.out.println("插入完成");
    }

    // 生成一个随机中文字符
    public static char randomChineseChar() {
        return (char) (CHINESE_START + random.nextInt(CHINESE_END - CHINESE_START + 1));
    }

    // 生成随机中文句子（指定长度）
    public static String randomChineseString(int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(randomChineseChar());
        }
        return sb.toString();
    }

}
