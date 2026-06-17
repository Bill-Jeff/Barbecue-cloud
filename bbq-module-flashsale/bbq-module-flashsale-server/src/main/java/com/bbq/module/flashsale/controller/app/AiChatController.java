package com.bbq.module.flashsale.controller.app;

import com.bbq.module.flashsale.config.Result;
import com.bbq.module.flashsale.entity.OrderInfo;
import com.bbq.module.flashsale.service.AiChatService;
import com.bbq.module.flashsale.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/flashsale/api/ai")
@RequiredArgsConstructor
public class AiChatController {

    private final AiChatService aiChatService;

    @PostMapping("/chat")
    public Result<String> chat(@RequestParam("userInputMsg")String userInputMsg) {
        return Result.success(aiChatService.chat(userInputMsg));
    }
}
