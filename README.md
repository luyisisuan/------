# 现代企业级请假审批管理系统 (Modern Enterprise Leave Approval System)

![系统主界面预览]![image](https://github.com/user-attachments/assets/09291f22-c082-49fc-bb07-025edd331f6d)
![image](https://github.com/user-attachments/assets/3b4384d8-49db-467c-81e8-7a053ea5d937)
![image](https://github.com/user-attachments/assets/aebe1f0b-969e-4243-8ea2-5c073f123ff7)
![image](https://github.com/user-attachments/assets/286ecb05-09a8-4f62-bc74-10f6b5867756)
![image](https://github.com/user-attachments/assets/7a86ba80-1355-4c79-8cce-3c82e1d7fcd8)


本项目是一个 **全栈的请假审批与管理解决方案**，旨在为企业和组织提供一个高效、安全且用户友好的数字化平台。前端采用最新的 **Vue 3 (Composition API)**、**Vite** 构建工具、**Pinia** 状态管理和 **Element Plus** UI组件库，打造了美观、响应迅速的交互界面。后端则基于强大的 **Spring Boot** 框架，结合 **Spring Security (JWT)** 进行安全认证，**Spring Data JPA/Hibernate** 与 **MySQL** 实现数据持久化，并通过精心设计的**职责链模式**和**状态模式**来处理复杂的审批工作流和申请生命周期管理。


## ✨ 项目亮点与核心特性

*   **现代化技术栈**: 全面采用业界主流和前沿技术，确保高性能、高可维护性和良好的开发体验。
*   **优雅且响应式的用户界面**: 基于 Element Plus 进行深度定制和美化，确保在桌面、平板和移动设备上均有出色的视觉和交互效果。
*   **安全可靠的认证授权**:
    *   基于 JWT (JSON Web Tokens) 的无状态认证机制，安全高效。
    *   精细的基于角色的访问控制 (RBAC)，通过 Vue Router 前端路由守卫和 Spring Security 后端注解保护，确保用户只能访问其权限范围内的功能和数据。
    *   API 请求自动携带认证 Token，401 未授权错误自动处理并引导用户重新登录。
*   **完整且灵活的请假审批流程**:
    *   支持多种请假类型（年假、病假、事假等）的提交。
    *   用户可方便地跟踪自己提交的请假申请的实时审批状态和历史记录。
    *   申请人可以取消处于待审批状态的请假。
    *   审批流程通过**职责链模式**实现，易于根据企业实际需求调整审批层级和规则（例如，TeamLead -> DeptManager -> HR）。
    *   通过**状态模式**精确管理请假申请的生命周期 (如待审批、已批准、已驳回、已取消)，确保操作的合法性和数据一致性。
*   **高效的审批工作台**:
    *   审批人可以清晰地查看分配给自己的待审批任务列表，支持分页和排序。
    *   提供便捷的在线批准或驳回操作界面，并可附加审批意见。
*   **强大的管理员后台**:
    *   集中管理所有系统用户账户，包括用户的创建、信息编辑、角色分配、直属经理设置及账户状态（启用/禁用）管理。
    *   安全地删除用户账户。
*   **优秀的用户体验**:
    *   所有异步操作均配备清晰的加载状态指示 (如按钮loading、表格loading、骨架屏占位等)。
    *   表单输入提供即时校验反馈。
    *   使用 Element Plus 的 `ElMessage` 和 `ElMessageBox` 提供友好的操作成功/失败提示和重要操作的二次确认。
    *   页面切换具有平滑的过渡动画效果。
    *   为列表和内容区域提供友好的空状态提示。
*   **自动生成API文档**: 后端集成 Springdoc OpenAPI (Swagger UI)，自动生成交互式API文档，方便前后端协作与接口测试。

## 🛠️ 技术栈详解

### 前端 (Frontend)

*   **核心框架**: Vue 3.x (Composition API, `<script setup>`)
*   **构建工具**: Vite 5.x (或更高)
*   **状态管理**: Pinia 2.x
*   **路由**: Vue Router 4.x
*   **UI 组件库**: Element Plus (最新稳定版)
*   **HTTP 客户端**: Axios (封装于 `src/services/api.js`)
*   **CSS**: 全局样式 (`src/style.css`), Scoped CSS, CSS 自定义属性 (CSS Variables), 支持深浅色模式。
*   **图标库**: `@element-plus/icons-vue`
*   **开发语言**: JavaScript (ESNext)

### 后端 (Backend)

*   **核心框架**: Spring Boot 3.4.5 (请根据实际情况更新版本)
*   **编程语言**: Java 17
*   **安全框架**: Spring Security (与Spring Boot版本匹配), JWT 认证与授权
*   **数据持久化**: Spring Data JPA, Hibernate ORM
*   **数据库**: MySQL (推荐 8.x 版本)
*   **构建工具**: Apache Maven
*   **API 文档**: Springdoc OpenAPI (集成 Swagger UI)
*   **辅助库**: Lombok (简化模型代码), Jackson (JSON处理)
*   **设计模式应用**: 职责链模式 (审批流), 状态模式 (申请状态管理)

## 🚀 主要功能模块

_(这里可以更详细地列出每个模块下的具体功能点)_

1.  **用户认证与账户**
    *   用户注册 (支持选择直属经理)
    *   用户登录 (JWT签发)
    *   用户登出 (JWT失效/清理)
    *   密码加密存储
    *   获取可选经理列表 (供注册/用户管理使用)
2.  **仪表盘与导航**
    *   个性化欢迎信息
    *   基于角色的快速操作入口
    *   全局响应式导航栏
3.  **请假申请管理**
    *   发起新的请假申请 (表单校验、日期选择、类型选择、理由填写)
    *   我的请假申请列表 (分页、排序、状态筛选 - 可选)
    *   查看请假申请详情 (包含基本信息、审批历史)
    *   申请人取消待审批的请假申请
4.  **请假审批工作流**
    *   待我审批列表 (分页、排序)
    *   审批操作 (批准/驳回，填写审批意见)
    *   审批历史记录与展示
5.  **管理员中心 - 用户管理**
    *   用户列表展示 (分页、搜索、筛选 - 可选)
    *   创建新用户账户 (分配角色、设置直属经理、初始密码)
    *   编辑用户信息 (修改姓名、邮箱、部门、角色、直属经理、启用/禁用状态)
    *   删除用户账户 (附带确认)

## ⚙️ 项目部署与运行指南

### 前提条件

*   **前端**: Node.js (v18.x 或 LTS 最新版), npm/yarn/pnpm
*   **后端**: JDK ([指定版本]), Apache Maven (v3.6+), MySQL Server

### 后端配置与启动

1.  **克隆后端仓库**: `git clone [您的后端仓库URL]`
2.  **配置数据库**: 修改 `src/main/resources/application.properties` 中的数据库连接（URL, 用户名, 密码）和 JWT 密钥。**请务必为生产环境生成一个强随机JWT密钥。**
    ```properties
    # 示例 (具体内容请参考您项目中的 application.properties)
    spring.datasource.url=jdbc:mysql://localhost:3306/leave_approval_system?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
    spring.datasource.username=root
    spring.datasource.password=your_actual_password
    app.jwtSecret=YOUR_ULTRA_STRONG_AND_UNIQUE_JWT_SECRET
    app.jwtExpirationMs=86400000 
    ```
3.  **构建项目**:
    ```bash
    cd path/to/leave-approval-backend
    mvn clean package
    ```
4.  **运行后端服务**:
    ```bash
    java -jar target/leave-approval-backend-0.0.1-SNAPSHOT.jar 
    ```
    服务默认启动在 `http://localhost:8080`。

### 前端配置与启动

1.  **克隆前端仓库**: `git clone [您的前端仓库URL]`
2.  **安装依赖**:
    ```bash
    cd path/to/leave-approval-frontend
    npm install 
    ```
3.  **配置API代理**: 检查 `vite.config.js` 中的 `server.proxy` 设置，确保其 `target` 指向您正在运行的后端服务地址 (默认为 `http://localhost:8080`)。
4.  **运行开发服务器**:
    ```bash
    npm run dev
    ```
    前端应用通常启动在 `http://localhost:5173`。

### API 文档 (Swagger UI)

后端服务成功启动后，可以通过浏览器访问以下链接查看和测试API接口：
`http://localhost:8080/swagger-ui.html`

## 🎨 视觉设计与组件库

本项目UI基于 **Element Plus** 组件库构建，并进行了自定义样式调整以实现统一的视觉风格。全局设计令牌（颜色、字体、间距等）通过CSS自定义属性进行管理，支持浅色和深色模式（基于操作系统偏好）。

## 🗺️ 项目结构导览

### 前端 (`leave-approval-frontend`)
*   `src/assets/`: 全局静态资源，如主CSS文件 (`style.css` 或 `main.css`)。
*   `src/components/layout/`: 布局组件，如 `Navbar.vue`。
*   `src/router/`: Vue Router 配置，包含路由表和导航守卫。
*   `src/services/`: API服务封装 (Axios实例 `api.js`, `authService.js`, 等)。
*   `src/stores/`: Pinia状态管理模块 (核心为 `auth.js`)。
*   `src/views/`: 页面级组件。
*   `src/main.js`: 应用入口，初始化Vue、Pinia、Router、Element Plus。
*   `vite.config.js`: Vite构建配置，包含开发服务器代理。

### 后端 (`leave-approval-backend`)
*   `src/main/java/com/example/leaveapproval/config/`: Spring Security及JWT相关配置。
*   `src/main/java/com/example/leaveapproval/controller/`: REST API控制器。
*   `src/main/java/com.example/leaveapproval/dto/`: 数据传输对象定义。
*   `src/main/java/com/example/leaveapproval/model/`: JPA实体及领域模型，包括状态模式的实现。
*   `src/main/java/com/example/leaveapproval/repository/`: Spring Data JPA仓库接口。
*   `src/main/java/com/example/leaveapproval/service/`: 业务逻辑服务层，包含审批链等核心逻辑。
*   `src/main/java/com/example/leaveapproval/util/`: 工具类，如JWT处理、数据加载器。
*   `src/main/resources/application.properties`: 应用核心配置文件。
*   `pom.xml`: Maven项目依赖与构建配置。

## 🤝 贡献

欢迎对本项目提出改进建议或贡献代码！请遵循标准的GitHub Flow (Fork -> Feature Branch -> Pull Request)。
在提交Pull Request之前，请确保：
*   代码通过了本地的构建和测试（如果已配置）。
*   代码风格与项目现有风格保持一致（建议配置ESLint/Prettier等工具）。
*   提交信息清晰明了。

## 📜 许可证

本项目采用 [MIT许可证](LICENSE) (请根据您的选择替换，如果尚未添加LICENSE文件，可以考虑添加一个)。

---
