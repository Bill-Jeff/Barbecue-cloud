//package com.bbq.module.flashsale.config;
//
//import com.bbq.module.flashsale.entity.User;
//import com.bbq.module.flashsale.service.UserService;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.boot.ApplicationArguments;
//import org.springframework.boot.ApplicationRunner;
//import org.springframework.stereotype.Component;
//
//@Slf4j
//@Component
//@RequiredArgsConstructor
//public class DataInitializer implements ApplicationRunner {
//
//    private final UserService userService;
//
//    @Override
//    public void run(ApplicationArguments args) {
//        if (userService.findByUsername("admin") == null) {
//            User admin = new User();
//            admin.setUsername("admin");
//            admin.setPassword("123456");
//            admin.setNickname("超级管理员");
//            admin.setRole("admin");
//            admin.setStatus(1);
//            userService.save(admin);
//            log.info("默认管理员已创建: admin / 123456");
//        }
//    }
//}
