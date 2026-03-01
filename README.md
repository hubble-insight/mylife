# MyLife - 个人生活记录网站

基于 Java 17 + Spring Boot 3.x 的个人网站，整合新浪微博和百度网盘资源。

## 功能特性

- **首页**: 展示微博数量、资源统计、最新微博和资源预览
- **动态栏**: 同步并展示新浪微博动态
- **资源库**: 分类展示百度网盘资源（文档/视频/音频）
- **授权管理**: 百度网盘 OAuth2 授权管理，自动刷新 token

## 技术栈

- **后端**: Java 17 + Spring Boot 3.2.0
- **前端**: Thymeleaf + Bootstrap 5
- **数据库**: SQLite
- **缓存**: Caffeine

## 快速开始

### 1. 环境要求

- Java 17+
- Maven 3.6+

### 2. 获取 API 密钥

#### 微博 API
1. 访问 [微博开放平台](https://open.weibo.com/)
2. 创建应用获取 App Key 和 App Secret
3. 通过 OAuth 2.0 获取 Access Token
4. 获取用户 ID

#### 百度网盘 API
1. 访问 [百度网盘开放平台](https://pan.baidu.com/union)
2. 创建应用获取 App ID 和 App Secret
3. 配置回调地址为：`http://localhost:8080/oauth/baidu/callback`
4. **无需手动换取 token**，应用内置 OAuth2 授权流程

### 3. 配置 API 密钥

**只需配置百度网盘的 App ID 和 App Secret**：

**方式一：环境变量**
```bash
export WEIBO_APP_ID=your_app_id
export WEIBO_APP_SECRET=your_app_secret
export WEIBO_USER_ID=your_user_id

export BAIDU_APP_ID=your_app_id
export BAIDU_APP_SECRET=your_app_secret
```

**方式二：直接修改 application.yml**
```yaml
api:
  weibo:
    app-id: your_app_id
    app-secret: your_app_secret
    user-id: your_user_id
  baidu-pan:
    app-id: your_app_id
    app-secret: your_app_secret
    redirect-uri: http://localhost:8080/oauth/baidu/callback
```

### 4. 运行应用

```bash
# 使用 Java 21（或 Java 17+）
JAVA_HOME=/path/to/java mvn spring-boot:run
```

访问 http://localhost:8080

## 项目结构

```
mylife/
├── src/main/java/com/mylife/
│   ├── MyLifeApplication.java    # 主应用类
│   ├── config/                    # 配置类
│   ├── controller/                # Web 控制器
│   ├── service/                   # 业务逻辑
│   ├── repository/                # 数据访问
│   ├── model/                     # 实体类
│   └── client/                    # 第三方 API 客户端
├── src/main/resources/
│   ├── templates/                 # Thymeleaf 模板
│   ├── static/                    # 静态资源
│   └── application.yml            # 配置文件
├── data/                          # SQLite 数据库目录
└── pom.xml                        # Maven 配置
```

## 功能使用说明

### 1. 新浪微博同步

访问首页查看最新微博，或访问 http://localhost:8080/weibo 查看完整微博列表。

点击"同步微博"按钮从新浪微博 API 获取最新数据。

### 2. 百度网盘 OAuth2 授权

1. 访问授权管理页面：http://localhost:8080/oauth/baidu/status
2. 点击"开始授权"按钮
3. 跳转到百度网盘授权页面，登录并同意授权
4. 自动跳回授权管理页面，完成授权
5. 之后访问资源库时会自动同步百度网盘文件

**Token 自动管理**：
- access_token 过期时自动使用 refresh_token 刷新
- 无需手动配置 token
- 可随时在授权管理页面刷新或撤销授权

### 3. 同步百度网盘资源

授权后访问 http://localhost:8080/resources?sync=true 即可同步资源。

## 注意事项

1. 微博 API 需要手动配置 access_token（因为微博 OAuth2 流程较复杂）
2. 百度网盘使用 OAuth2 授权，token 自动保存到数据库
3. API 有调用频率限制，数据缓存 10 分钟
4. SQLite 数据库文件存储在 `./data/mylife.db`

## API 路由

| 路径 | 说明 |
|------|------|
| `/` | 重定向到首页 |
| `/home` | 首页 |
| `/weibo` | 微博列表 |
| `/weibo?sync=true` | 同步微博 |
| `/resources` | 资源库 |
| `/resources?type=document` | 文档资源 |
| `/resources?type=video` | 视频资源 |
| `/resources?type=audio` | 音频资源 |
| `/oauth/baidu/status` | 百度网盘授权管理 |
| `/oauth/baidu/authorize` | 开始授权 |
| `/oauth/baidu/callback` | OAuth 回调 |

## 开发计划

- [ ] 支持 HTMX 异步加载
- [ ] 添加图片相册功能
- [ ] 支持本地文件上传
- [ ] 添加搜索功能
- [ ] 支持微博 OAuth2 授权
