# CampusHub Backend

本项目是 CampusHub 的后端部分，基于 Spring Boot 框架开发。

## 技术栈
- **Language**: Java 17
- **Framework**: Spring Boot 3.x
- **Build Tool**: Maven
- **Database**: MySQL

## 目录结构
- `src/main/java`: 业务逻辑代码
- `src/main/resources`: 配置文件 (properties/yml)
- `src/test/java`: 单元测试与集成测试

## 开发环境配置
1. 确保已安装 JDK 17。
2. 配置 MySQL 数据库，并在 `src/main/resources/application.properties` 中修改对应的数据库连接信息。

## 运行方式
在 `backend` 目录下执行：
```bash
./mvnw spring-boot:run
```

## 测试方式
在 `backend` 目录下执行：
```bash
./mvnw test
```