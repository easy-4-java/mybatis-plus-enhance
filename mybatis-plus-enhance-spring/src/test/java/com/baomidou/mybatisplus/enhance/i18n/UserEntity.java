package com.baomidou.mybatisplus.enhance.i18n;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.enhance.crypto.annotation.TableSignature;
import com.baomidou.mybatisplus.enhance.crypto.annotation.TableSignatureField;
import lombok.Data;

import java.io.Serializable;

@Data
@TableSignature
@TableName(value = "user_entity")
public class UserEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private String name;
    // 使用@TableSignatureField 注解
    @TableSignatureField(order = 1)
    private String mobile;
    // 使用@TableSignatureField 注解
    @TableSignatureField(order = 2)
    private String email;
    // 需要存储HMAC的字段用这个注解
    @TableSignatureField(stored = true)
    private String hamc;
}
