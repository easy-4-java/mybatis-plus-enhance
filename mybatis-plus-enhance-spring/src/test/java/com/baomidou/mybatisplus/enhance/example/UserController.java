package com.baomidou.mybatisplus.enhance.example;

import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户表控制器（示例代码，演示 Signed Service API 用法）。
 */
@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private IUserService userService;

    @GetMapping(name = "测试查询签名验证", value = "/detail")
    public UserEntity detail(Long id) {
        UserEntity entity = userService.getSignedById(id);
        if (null == entity) {
            return new UserEntity();
        }
        return entity;
    }

    @GetMapping(name = "新增用户表，测试加密", value = "/add")
    public UserEntity add(UserEntity entity) {
        userService.saveSigned(entity);
        return entity;
    }

    @GetMapping(name = "修改用户表", value = "/update")
    public UserEntity update(UserEntity entity) {
        userService.updateSignedById(entity);
        return entity;
    }
}
