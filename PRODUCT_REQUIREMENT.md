# 产品需求文档 (PRD): 智财助手 (Smart Finance Tracker)

| 文档版本 | 修改日期 | 修改人 | 修改描述 |
| :--- | :--- | :--- | :--- |
| v1.0.0 | 2025-06-23 | Product Manager | 初始版本发布，定义核心功能与商业逻辑 |

## 1. 项目引言 (Introduction)

### 1.1 项目背景
在数字化时代，个人财务管理日益复杂。现有的市场解决方案往往呈现两极分化：要么是操作繁琐的专业会计软件，要么是功能过于单一、缺乏数据分析能力的简易记账工具。对于初级全栈工程师而言，开发一款兼具**易用性、数据可视化及预算预警**的轻量级财务系统，不仅能解决个人“记账难、坚持难”的痛点，更是掌握现代 Web 开发核心技术栈的绝佳实践。

### 1.2 商业价值与目标
*   **核心价值**：通过极简的交互实现“3秒记账”，利用直观的图表帮助用户洞察消费习惯，通过预算机制辅助用户实现储蓄目标。
*   **技术练手价值**：涵盖全栈开发核心技能（身份认证、复杂 CRUD、数据聚合分析、响应式 UI、安全性设计）。
*   **目标用户**：职场新人、自由职业者、大学生以及任何希望改善财务状况的个人群体。

---

## 2. 用户画像 (User Persona)

| 维度 | 描述 |
| :--- | :--- |
| **姓名** | 小李 (Li Lei) |
| **职业** | 入职 2 年的软件工程师 |
| **收入水平** | 月收入 8k-12k，有稳定结余意愿 |
| **痛点** | 经常点外卖、网购导致月底超支；不知道钱具体花在哪了；手动记账太麻烦难以坚持。 |
| **核心诉求** | 需要一个能自动统计分类、在手机和电脑端同步、并在快超支时提醒他的工具。 |

---

## 3. 功能需求详情 (Functional Requirements)

### 3.1 模块汇总表

| 模块名称 | 优先级 | 功能点列表 | 业务价值 |
| :--- | :--- | :--- | :--- |
| **用户中心** | P0 | 注册、登录、JWT 鉴权、个人信息维护 | 确保数据隐私与安全 |
| **记账管理** | P0 | 极速记账、历史记录增删改查、分类管理 | 系统的核心数据入口 |
| **数据看板** | P1 | 月度/年度收支总览、分类饼图、趋势折线图 | 提供决策支持，体现数据价值 |
| **预算系统** | P1 | 月度总预算设置、分类预算、进度条预警 | 帮助用户控制消费，增加用户粘性 |
| **高级功能** | P2 | 账单导出 (Excel/CSV)、多账本切换、暗黑模式 | 提升用户体验，满足个性化需求 |

### 3.2 详细业务流程

#### A. 记账流程 (Transaction Flow)
1.  **触发**：用户点击首页“记一笔”按钮。
2.  **输入**：
    *   金额（数字键盘，支持小数点后两位）
    *   类型（支出 / 收入 / 转账）
    *   分类（餐饮、交通、购物、住房、娱乐等）
    *   日期（默认当前，支持补录）
    *   标签/备注（可选）
3.  **处理**：前端校验 -> 调用 API -> 后端存入数据库 -> 更新缓存。
4.  **反馈**：页面提示“记账成功”，并自动跳转到最新记录列表。

#### B. 预算预警逻辑 (Budget Alert Logic)
*   **设定**：用户设置当月总预算为 $X$。
*   **计算**：实时计算当月累计支出 $Y$。
*   **展示规则**：
    *   若 $Y/X < 50\%$：进度条显示绿色，状态正常。
    *   若 $50\% \le Y/X < 80\%$：进度条显示黄色，提示“预算已使用过半”。
    *   若 $Y/X \ge 80\%$：进度条显示红色，高亮显示“注意！即将超支”。

---

## 4. 技术架构建议 (Technical Architecture)

为了最大化项目的简历含金量，建议采用以下技术栈：

### 4.1 前端 (Frontend)
*   **框架**：Vue 3 (Composition API)。
*   **语言**：TypeScript (强类型约束，减少财务计算错误)。
*   **状态管理**：Pinia。
*   **UI 组件库**：Tailwind CSS + Shadcn/UI。
*   **可视化**：ECharts。

### 4.2 后端 (Backend)
*   **方案 B (Java)**：Spring Boot + MyBatis Plus (传统大厂首选)。
*   **安全**：Spring Security，配合 JWT 实现无状态认证。

### 4.3 数据库 (Database)
*   **选型**：MySQL。
*   **关键点**：金额字段必须使用 `DECIMAL` 类型，严禁使用 `FLOAT/DOUBLE` 以避免精度丢失。

---

## 5. 数据模型设计 (Data Schema)

### 5.1 用户表 (`users`)
```sql
CREATE TABLE users (
    id INT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    avatar_url VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

### 5.2 交易记录表 (`transactions`)
```sql
CREATE TABLE transactions (
    id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    type ENUM('EXPENSE', 'INCOME') NOT NULL,
    category VARCHAR(50) NOT NULL, -- e.g., "Food", "Transport"
    transaction_date DATE NOT NULL,
    description TEXT,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user_date (user_id, transaction_date)
);
```

---

## 6. 非功能性需求 (Non-functional Requirements)

1.  **安全性 (Security)**：
    *   所有 API 接口（除登录注册外）必须经过 JWT 中间件校验。
    *   密码存储必须加盐哈希（推荐使用 bcrypt）。
2.  **性能 (Performance)**：
    *   Dashboard 图表数据加载延迟应低于 500ms。
    *   前端首屏加载时间（FCP）控制在 1.5s 以内。
3.  **兼容性 (Compatibility)**：
    *   界面需实现响应式布局，完美适配手机、平板及桌面显示器。
4.  **代码规范 (Code Quality)**：
    *   遵循 ESLint + Prettier 规范。
    *   关键业务逻辑（如金额计算）需编写单元测试。

---

## 7. 开发路线图 (Roadmap)

### 第一阶段：基础建设 (Week 1)
*   [ ] 完成数据库设计与建表。
*   [ ] 搭建前后端项目脚手架。
*   [ ] 实现用户注册、登录及 Token 验证功能。

### 第二阶段：核心业务 (Week 2)
*   [ ] 实现账单的增、删、改、查 API。
*   [ ] 完成前端记账表单与列表页开发。
*   [ ] 对接分类管理与日期选择功能。

### 第三阶段：数据可视化 (Week 3)
*   [ ] 开发 Dashboard 页面。
*   [ ] 集成 ECharts，实现收支构成饼图与趋势图。
*   [ ] 编写后端聚合查询接口（Sum/Group By）。

### 第四阶段：进阶与优化 (Week 4)
*   [ ] 实现预算设置与进度条预警逻辑。
*   [ ] 增加 Excel 导出功能。
*   [ ] 部署上线（推荐使用 Docker + Railway/Vercel）。

---

> **产品经理寄语**：
> 这个项目看似简单，但要做到“极致”有很多细节可以打磨。比如：如何处理跨时区的时间戳？如何优化大量数据下的图表渲染速度？这些问题的解决过程，就是你从初级工程师向中高级工程师跃迁的过程。加油！
