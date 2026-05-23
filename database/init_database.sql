-- 智财助手 (Smart Finance Tracker) 数据库初始化脚本
-- 版本: v1.0.0
-- 目标数据库: smart_finance_db

-- 1. 如果数据库已存在则删除（可选，生产环境慎用）
DROP DATABASE IF EXISTS smart_finance_db;

-- 2. 创建数据库并设置字符集为 utf8mb4（支持 emoji 等特殊字符）
CREATE DATABASE smart_finance_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 3. 使用该数据库
USE smart_finance_db;

-- 4. 创建用户表 (users)
CREATE TABLE users (
    id INT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    email VARCHAR(100) NOT NULL UNIQUE COMMENT '邮箱地址',
    password_hash VARCHAR(255) NOT NULL COMMENT '加密后的密码',
    avatar_url VARCHAR(255) DEFAULT NULL COMMENT '头像URL',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_email (email) COMMENT '邮箱索引，用于快速登录查询'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户信息表';

-- 5. 创建交易记录表 (transactions)
CREATE TABLE transactions (
    id INT PRIMARY KEY AUTO_INCREMENT COMMENT '交易ID',
    user_id INT NOT NULL COMMENT '关联的用户ID',
    amount DECIMAL(10, 2) NOT NULL COMMENT '交易金额（精确到分）',
    type VARCHAR(20) NOT NULL COMMENT '交易类型：EXPENSE/INCOME',
    category VARCHAR(50) NOT NULL COMMENT '交易分类（如：餐饮、交通）',
    transaction_date DATE NOT NULL COMMENT '交易发生日期',
    description TEXT COMMENT '备注说明',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
    
    -- 外键约束：确保交易记录必须属于一个存在的用户
    CONSTRAINT fk_user_transaction FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    
    -- 复合索引：优化按用户和日期查询的性能（Dashboard 常用）
    INDEX idx_user_date (user_id, transaction_date),
    INDEX idx_category (category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='个人财务交易记录表';

-- 6. 验证表结构
SHOW TABLES;