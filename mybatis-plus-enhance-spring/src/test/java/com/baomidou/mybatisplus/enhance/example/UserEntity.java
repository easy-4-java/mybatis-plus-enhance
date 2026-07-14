package com.baomidou.mybatisplus.enhance.example;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import org.apache.ibatis.enhance.annotation.crypto.TableSignature;
import org.apache.ibatis.enhance.annotation.crypto.TableSignatureField;
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
    @TableSignatureField(order = 1)
    private String mobile;
    @TableSignatureField(order = 2)
    private String email;
    @TableSignatureField(stored = true)
    private String hamc;
}
