package com.bbq.module.flashsale.service;

import cn.hutool.core.util.ObjectUtil;
import com.bbq.module.flashsale.entity.Product;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AiChatService {

    private final ChatClient chatClient;
    private final PvService pvService;
    private final ProductService productService;

    public AiChatService(ChatClient.Builder builder,
                         PvService pvService,
                         ProductService productService) {
        this.pvService = pvService;
        this.productService = productService;
        this.chatClient = builder
                .defaultSystem("""
                    你是一个烧烤店客服助手，名字叫小考。回答要简洁友好，控制在100字以内。

                    重要规则：
                    1. 只根据提供的菜单内容回答，绝不编造菜单上没有的菜品
                    2. 本店没有任何优惠活动、折扣券、满减、会员折扣等，绝对不要编造任何优惠信息
                    3. 如果用户问到优惠/折扣/活动，统一回复"目前暂时没有优惠活动哦，但我们的价格已经很实惠了~"
                    4. 推荐菜品时使用菜单中的准确名称
                    5. 当用户说"加购""加入购物车""帮我加"等要求时，在回复末尾单独一行加上标签：
                       [CART:菜品准确名称1,菜品准确名称2]
                       菜品名称必须和菜单中完全一致
                    """)
                .build();
    }

    public String chat(String userInput) {
        String menuContext = buildMenuContext();
        String prompt;
        if (ObjectUtil.isNotEmpty(menuContext)) {
            prompt = "以下是当前店内完整菜单：\n" + menuContext + "\n用户问题：" + userInput;
        } else {
            prompt = userInput;
        }
        return chatClient.prompt()
                .user(prompt)
                .call()
                .content();
    }

    private String buildMenuContext() {
        List<Product> allProducts = productService.listAllProducts();
        if (allProducts.isEmpty()) {
            return null;
        }
        List<Map<String, Object>> hotProducts = pvService.getHotProducts(10);
        Set<Long> hotIds = hotProducts.stream()
                .map(p -> ((Number) p.get("id")).longValue())
                .collect(Collectors.toSet());

        StringBuilder sb = new StringBuilder();
        for (Product p : allProducts) {
            sb.append("- ").append(p.getName())
                    .append("，¥").append(p.getPrice());
            if (p.getDescription() != null && !p.getDescription().isEmpty()) {
                sb.append("，").append(p.getDescription());
            }
            if (hotIds.contains(p.getId())) {
                sb.append(" 【热门】");
            }
            sb.append("\n");
        }
        return sb.toString();
    }

}
