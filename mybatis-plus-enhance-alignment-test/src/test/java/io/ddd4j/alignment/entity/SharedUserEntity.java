package io.ddd4j.alignment.entity;

/**
 * 对齐测试用共享实体：不引用任何框架特定类，不携带任何 ORM/加密注解。
 *
 * <p>字段含义（与 {@code mybatis-plus-enhance-spring} 的 {@code UserEntity} 对齐）：</p>
 * <ul>
 *   <li>id — 自增主键</li>
 *   <li>name — 业务字段（不加密、不签名）</li>
 *   <li>mobile — @EncryptedField（加密） + @TableSignatureField(order=1)</li>
 *   <li>email — 业务字段（不加密） + @TableSignatureField(order=2)</li>
 *   <li>hamc — @TableSignatureField(stored=true) 签名存储</li>
 * </ul>
 *
 * <p>注解信息通过 {@link io.ddd4j.alignment.spi.SharedEntityConfigurator}
 * 在两侧分别反射各自的注解类获取。</p>
 */
public class SharedUserEntity {
    private Long id;
    private String name;
    private String mobile;
    private String email;
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
