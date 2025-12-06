# Nexus Chat 后端服务

Nexus Chat 的 Spring Boot 后端服务器 - 一个 Nexus 风格的实时聊天应用。

## 技术栈

- **Spring Boot 3.2.0** - 核心框架
- **Spring Data JPA** - 数据库 ORM
- **Spring Security** - 认证与授权
- **Spring WebSocket** - 使用 STOMP 协议的实时消息传输
- **MySQL** - 关系型数据库
- **JWT** - 基于令牌的身份认证
- **Maven** - 构建工具

## 前置要求

- Java 17 或更高版本
- MySQL 8.0 或更高版本
- Maven 3.6+

## 安装配置

1. **创建 MySQL 数据库**：
```sql
CREATE DATABASE nexus_chat;
```

2. **配置数据库**（可选）：
如需修改，请编辑 `src/main/resources/application.properties`：
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/nexus_chat
spring.datasource.username=root
spring.datasource.password=root
```

3. **构建项目**：
```bash
mvn clean install
```

4. **运行应用**：
```bash
mvn spring-boot:run
```

服务器将在 `http://localhost:8080` 启动

## API 接口

### 用户认证
- `POST /api/auth/register` - 注册新用户
- `POST /api/auth/login` - 用户登录
- `POST /api/auth/logout` - 用户登出

### 用户管理
- `GET /api/users/{id}` - 根据 ID 获取用户
- `GET /api/users/username/{username}` - 根据用户名获取用户
- `GET /api/users` - 获取所有用户
- `PUT /api/users/{id}/profile` - 更新用户资料
- `PUT /api/users/{id}/status` - 更新在线状态

### 聊天管理
- `POST /api/chats/direct` - 创建私聊
- `POST /api/chats/group` - 创建群聊
- `GET /api/chats/user/{userId}` - 获取用户的聊天列表
- `GET /api/chats/{chatId}` - 获取聊天详情

### 消息管理
- `POST /api/messages` - 发送消息
- `GET /api/messages/chat/{chatId}` - 获取聊天消息（分页）
- `PUT /api/messages/{messageId}/read` - 标记消息为已读
- `PUT /api/messages/chat/{chatId}/read` - 标记所有聊天消息为已读

### 联系人管理
- `POST /api/contacts` - 添加联系人
- `DELETE /api/contacts` - 删除联系人
- `GET /api/contacts/user/{userId}` - 获取用户联系人列表

### 文件管理
- `POST /api/files/upload` - 上传文件
- `POST /api/files/upload/chunk` - 上传文件分片（用于大文件）

## WebSocket 端点

通过 SockJS 连接到 WebSocket，地址为 `/ws`。

### 订阅主题：
- `/topic/chat/{chatId}` - 聊天室消息
- `/topic/users` - 用户状态更新

### 发送消息到：
- `/app/chat.sendMessage` - 发送聊天消息
- `/app/user.status` - 更新用户状态
- `/app/chat.typing` - 发送输入状态指示器

## 数据库架构

应用使用以下数据表：
- `users` - 用户账户
- `contacts` - 用户联系人
- `chats` - 聊天室（私聊/群聊）
- `chat_members` - 聊天参与者
- `messages` - 聊天消息
- `message_read_status` - 已读回执
- `file_uploads` - 文件元数据

## 功能特性

- ✅ 基于 JWT 的身份认证
- ✅ 通过 WebSocket (STOMP) 实时消息传输
- ✅ 私聊和群聊
- ✅ 消息已读状态跟踪
- ✅ 未读消息计数
- ✅ 在线/离线状态跟踪
- ✅ 联系人管理
- ✅ 支持分片上传的文件上传功能
- ✅ 消息分页加载

## 开发

运行测试：
```bash
mvn test
```

生产环境打包：
```bash
mvn clean package
```

## 注意事项

- 生产环境中应更改默认的 JWT 密钥
- 文件上传存储在 `uploads/` 目录
- WebSocket 使用 SockJS 回退以提高浏览器兼容性
- 开发环境中已启用 CORS（生产环境需要配置）
