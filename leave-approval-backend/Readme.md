# Leave Approval System - Backend API

## 1. 项目概述

本后端服务是 **请假审批系统** 的核心 API 组件，基于 **Spring Boot (v3.4.5)** 构建。它采用 **Java** (请指定版本, 如 17/21) 编程语言，并利用 **Spring Security** 和 **JWT (JSON Web Tokens)** 实现安全的认证与授权机制。数据持久化依赖 **Spring Data JPA** 和 **Hibernate**，后端数据库采用 **MySQL**。

该服务旨在为前端应用提供一套完整、安全且高效的 RESTful API 接口，支持包括用户管理、请假申请提交与处理、以及基于职责链和状态模式的复杂审批工作流。

## 2. 核心技术与特性

### 2.1. 技术栈

*   **核心框架**: Spring Boot 3.4.5
*   **编程语言**: Java 17
*   **安全框架**: Spring Security (与Spring Boot版本匹配), JWT
*   **数据访问**: Spring Data JPA, Hibernate
*   **数据库**: MySQL
*   **构建工具**: Apache Maven
*   **API 文档化**: Springdoc OpenAPI (集成 Swagger UI)
*   **辅助库**: Lombok (简化模型代码), Jackson (JSON序列化/反序列化)

### 2.2. 主要特性

*   **认证与授权**:
    *   基于 JWT 的无状态认证机制。
    *   精细化的基于角色的访问控制 (RBAC) 保护 API 端点。
    *   自定义 Token 过滤器及认证/授权失败处理。
*   **审批工作流**:
    *   通过**职责链模式 (Chain of Responsibility)** 实现灵活、可扩展的审批流程。
    *   各审批节点根据预设规则处理或传递审批任务。
*   **状态管理**:
    *   请假申请的生命周期采用**状态模式 (State Pattern)** 进行管理，保证了操作的原子性和状态转换的正确性。
*   **API 设计**:
    *   遵循 RESTful 设计原则，提供语义清晰的 API 接口。
    *   使用数据传输对象 (DTO) 进行客户端与服务端的数据交互。
*   **异常处理**:
    *   集中的全局异常处理机制，确保向客户端返回规范化的错误响应。
*   **数据初始化 (可选)**:
    *   包含 `DataLoader` 组件，用于在应用启动时初始化基础数据（如管理员账户、角色等），便于开发和测试。

## 3. 功能模块与 API 端点

### 3.1. 认证模块 (`/api/auth`)
    *   `POST /login`: 用户登录认证，成功后返回 JWT。
    *   `POST /register`: 新用户注册，支持在注册时指定直属经理。
    *   `GET /potential-managers`: 获取系统中可作为其他用户直属经理的用户列表。

### 3.2. 请假申请模块 (`/api/leave-requests`)
    *   `POST /`: 用户提交新的请假申请。
    *   `GET /my`: 获取当前登录用户提交的所有请假申请记录 (支持分页)。
    *   `GET /pending-my-approval`: 获取当前登录用户需要处理的待审批请假申请列表 (支持分页)。
    *   `GET /{id}`: 获取指定ID的请假申请的详细信息。
    *   `PUT /{id}/action`: 审批人对指定的请假申请执行审批操作 (批准/驳回)。
    *   `PUT /{id}/cancel`: 申请人取消其提交的、尚未处理的请假申请。

### 3.3. 用户管理模块 (管理员权限, `/api/admin/users`)
    *   `GET /`: 获取所有系统用户的列表 (支持分页)。
    *   `POST /`: 创建新用户账户 (可指定角色、直属经理等信息)。
    *   `GET /{id}`: 获取指定ID的用户的详细信息。
    *   `PUT /{id}`: 更新指定ID的用户信息。
    *   `DELETE /{id}`: 删除指定ID的用户账户。

_(API端点路径和具体参数请参考自动生成的API文档)_

## 4. 环境配置与运行指南

### 4.1. 前提条件

*   Java Development Kit (JDK) - 版本 [请指定您的版本, 如 17 或 21]
*   Apache Maven - 版本 3.6.x 或更高
*   MySQL Server - 版本 5.7.x 或 8.x

### 4.2. 应用配置

主要的应用程序配置位于 `src/main/resources/application.properties` 文件中。

**数据库连接配置**:
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/leave_approval_system?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
spring.datasource.username=root
spring.datasource.password=123456 # 请务必修改为您的实际数据库密码
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# JPA/Hibernate 配置
spring.jpa.hibernate.ddl-auto=update # 开发环境可设为 update，生产环境建议 validate 或 none
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect
Use code with caution.
Markdown
重要: 在生产环境中，强烈建议将 spring.jpa.hibernate.ddl-auto 设置为 validate 或 none，并使用专业的数据库迁移工具（如 Flyway 或 Liquibase）来管理数据库 Schema 的演进。
JWT 安全配置:
# !!! 生产环境警告 !!!
# 下方的 app.jwtSecret 密钥仅为示例。
# 请务必生成一个高强度的、唯一的随机字符串（至少64位，包含多种字符类型）作为您的生产密钥。
app.jwtSecret=your_very_long_and_super_secret_and_random_jwt_secret_key_here_replace_this_!@#$%^&*()_+
app.jwtExpirationMs=86400000 # Token 有效期 (示例: 24小时，单位毫秒)
Use code with caution.
Properties
4.3. 构建项目
在项目根目录下打开终端，执行以下 Maven 命令：
mvn clean package
Use code with caution.
Bash
或者，如果想跳过测试：
mvn clean package -DskipTests
Use code with caution.
Bash
构建成功后，会在 target/ 目录下生成一个可执行的 JAR 文件 (例如 leave-approval-backend-0.0.1-SNAPSHOT.jar)。
4.4. 运行应用
您可以通过以下任一方式启动后端服务：
通过 IDE 运行:
直接在您的集成开发环境 (如 IntelliJ IDEA, Eclipse STS) 中找到主应用程序类 com.example.leaveapproval.LeaveApprovalBackendApplication.java 并运行。
使用 Maven Spring Boot 插件:
mvn spring-boot:run
Use code with caution.
Bash
直接运行 JAR 文件:
java -jar target/leave-approval-backend-0.0.1-SNAPSHOT.jar
Use code with caution.
Bash
(请确保 JAR 文件名与您构建生成的实际文件名一致)
应用启动后，默认监听端口为 8080。
5. API 文档 (Swagger UI)
本项目已集成 Springdoc OpenAPI，可自动生成并提供交互式的 API 文档界面 (Swagger UI)。
在后端服务成功启动后，请通过浏览器访问以下地址查看 API 文档：
Swagger UI 界面: http://localhost:8080/swagger-ui.html
OpenAPI 3.0 描述文件 (JSON): http://localhost:8080/api-docs
(如果您的服务端口或 springdoc.swagger-ui.path / springdoc.api-docs.path 配置与默认值不同，请相应调整上述链接。)
6. 项目模块结构 (简述)
(项目模块结构图或更详细的文本描述，可以从您提供的截图中整理。例如：)
config/: 包含应用的安全配置 (Spring Security, JWT) 和其他全局配置类。
controller/: 实现了所有对外暴露的 RESTful API 端点。
dto/: 定义了用于API请求和响应的数据传输对象。
exception/: 包含全局异常处理器和自定义的业务异常类。
model/: 定义了应用的领域模型和 JPA 实体，包括 state/ 子包中的状态模式实现。
repository/: 包含了所有 Spring Data JPA 数据访问接口。
service/: 存放核心业务逻辑实现，分为 approval.chain (审批链), leave (请假核心业务), user (用户管理业务)。
util/: 包含如 JwtUtils (JWT生成与解析) 和 DataLoader (数据初始化) 等工具类。
LeaveApprovalBackendApplication.java: Spring Boot 应用的主启动类。
7. 注意事项与未来工作
安全性: 生产环境中的 app.jwtSecret 必须替换为高强度密钥。数据库密码等敏感信息应通过外部化配置（如环境变量、Spring Cloud Config）管理，而非硬编码在 application.properties 中。
数据库迁移: 考虑在生产部署流程中引入数据库迁移工具。
测试覆盖: 建议为核心业务逻辑编写单元测试和集成测试，以保证代码质量和系统稳定性。
日志策略: 完善生产环境的日志级别、格式和输出目的地配置。
可扩展性: 考虑未来可能的性能瓶颈，如数据库查询优化、引入缓存机制等。