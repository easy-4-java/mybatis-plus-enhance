package com.baomidou.mybatisplus.enhance.i18n;

import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户表控制器
 */
@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private IUserService userService;

    /**
     * 测试验签
     */
    @GetMapping(name = "测试查询签名验证", value = "/detail")
    public UserEntity detail(Long id) {
        // 测试MP API（查询结果并验签）
        UserEntity entity = userService.getSignedById(id);
        if (null == entity) {
            return new UserEntity();
        }
        return entity;
    }

    /**
     * 新增用户表，测试加密
     */
    @GetMapping(name = "新增用户表，测试加密", value = "/add")
    public UserEntity add(UserEntity entity) {
        // 测试MP API
        userService.saveSigned(entity);
        return entity;
    }

    /**
     * 修改用户表
     */
    @GetMapping(name = "修改用户表", value = "/update")
    public UserEntity update(UserEntity entity) {
        // 测试MP API
        userService.updateSignedById(entity);
        return entity;
    }
}
