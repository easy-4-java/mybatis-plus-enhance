package com.baomidou.mybatisplus.enhance.i18n;

import com.baomidou.mybatisplus.enhance.crypto.handler.DataSignatureHandler;
import com.baomidou.mybatisplus.enhance.service.impl.EnhanceServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class IUserServiceImpl extends EnhanceServiceImpl<UserMapperEnhance, UserEntity> implements IUserService {

    @Autowired
    public IUserServiceImpl(DataSignatureHandler dataSignatureHandler) {
        super(dataSignatureHandler);
    }

}
