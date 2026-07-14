package io.ddd4j.alignment.entity;

import org.apache.ibatis.enhance.annotation.crypto.EncryptedField;
import org.apache.ibatis.enhance.annotation.crypto.EncryptedTable;
import org.apache.ibatis.enhance.annotation.crypto.TableSignature;
import org.apache.ibatis.enhance.annotation.crypto.TableSignatureField;

/**
 * 对齐测试用共享实体：两个框架共用同一套注解（统一路径后）。
 *
 * <p>注解路径统一为 org.apache.ibatis.enhance.annotation.crypto.*，
 * Plus 版和 non-Plus 版都能正确识别。</p>
 */
@EncryptedTable
@TableSignature
public class SharedUserEntity {
    private Long id;
    private String name;

    @EncryptedField
    @TableSignatureField(order = 1)
    private String mobile;

    @TableSignatureField(order = 2)
    private String email;

    @TableSignatureField(stored = true)
    private String hamc;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getMobile() { return mobile; }
    public void setMobile(String mobile) { this.mobile = mobile; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getHamc() { return hamc; }
    public void setHamc(String hamc) { this.hamc = hamc; }
}
