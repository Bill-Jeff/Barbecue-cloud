//package com.bbq.module.flashsale.service;
//
//import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
//import com.bbq.module.flashsale.entity.User;
//import com.bbq.module.flashsale.mapper.UserMapper;
//import lombok.RequiredArgsConstructor;
//import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
//import org.springframework.stereotype.Service;
//
//import java.util.List;
//
//@Service
//@RequiredArgsConstructor
//public class UserService {
//
//    private final UserMapper userMapper;
//    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();
//
//    public User findByUsername(String username) {
//        return userMapper.selectOne(
//                new LambdaQueryWrapper<User>().eq(User::getUsername, username));
//    }
//
//    public List<User> listAll() {
//        return userMapper.selectList(
//                new LambdaQueryWrapper<User>().orderByDesc(User::getCreateTime));
//    }
//
//    public void save(User user) {
//        if (user.getId() == null) {
//            user.setPassword(ENCODER.encode(user.getPassword()));
//            userMapper.insert(user);
//        } else {
//            if (user.getPassword() != null && !user.getPassword().isEmpty()) {
//                user.setPassword(ENCODER.encode(user.getPassword()));
//            } else {
//                user.setPassword(null);
//            }
//            userMapper.updateById(user);
//        }
//    }
//
//    public void delete(Long id) {
//        userMapper.deleteById(id);
//    }
//
//    public boolean checkPassword(String raw, String hashed) {
//        return ENCODER.matches(raw, hashed);
//    }
//}
