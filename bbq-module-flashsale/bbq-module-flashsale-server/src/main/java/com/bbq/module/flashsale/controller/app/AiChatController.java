package com.bbq.module.flashsale.controller.app;

import com.bbq.framework.common.util.servlet.ServletUtils;
import com.bbq.framework.security.core.LoginUser;
import com.bbq.module.flashsale.config.Result;
import com.bbq.module.flashsale.service.AliAiChatService;
import com.bbq.module.flashsale.service.OpenAiChatService;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/flashsale/api/ai")
@RequiredArgsConstructor
public class AiChatController {

    private final AliAiChatService aliAiChatService;
    private final OpenAiChatService openAiChatService;

    @PostMapping("/ali-chat")
    public Result<String> alChat(@RequestParam("userInputMsg")String userInputMsg) {
        return Result.success(aliAiChatService.chat(userInputMsg));
    }

    @PostMapping("/open-chat")
    public Result<String> openChat(@RequestParam("userInputMsg")String userInputMsg) {
        return Result.success(openAiChatService.chat(userInputMsg));
    }

//    @PostMapping("/lc-chat")
//    public Result<String> lcChat(@RequestParam("userInputMsg")String userInputMsg) {
//        return Result.success(chatModel.chat(userInputMsg));
//    }
}
