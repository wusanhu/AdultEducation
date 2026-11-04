# 部署说明

## 1. 环境准备

| 组件 | 建议版本 | 端口 |
| --- | --- | --- |
| JDK | 21 | - |
| MySQL | 8.0 | 3306 |
| Redis | 6/7 | 6379 |
| Nginx | 1.22+ | 80/443 |
| Node.js | 20（仅构建前端） | - |

创建运行用户与目录：

```bash
sudo useradd -r -s /sbin/nologin adultedu
sudo mkdir -p /opt/adulteducation/data/uploads
sudo chown -R adultedu:adultedu /opt/adulteducation
```

## 2. 初始化数据库

应用会自动创建 `adult_edu` 库和表，只需保证账号有建库权限：

```sql
CREATE USER IF NOT EXISTS 'adultedu'@'127.0.0.1' IDENTIFIED BY 'ChangeMe123!';
GRANT ALL PRIVILEGES ON adult_edu.* TO 'adultedu'@'127.0.0.1';
FLUSH PRIVILEGES;
```

也可以提前手工创建空库：

```sql
CREATE DATABASE IF NOT EXISTS adult_edu DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
```

## 3. 构建

```bash
# 后端
cd backend
mvn -DskipTests clean package

# 管理后台（输出到 backend/src/main/resources/static/admin）
cd ../admin
npm ci && npm run build

# 移动端（输出到 backend/src/main/resources/static/m）
cd ../mobile
npm ci && npm run build

# 重新打包，把前端产物打入 jar
cd ../backend
mvn -DskipTests package
```

> 若采用前后端分离部署，可跳过前端构建，由 Nginx 直接托管 `admin/dist` 与 `mobile/dist`，并保留 `/api` 与 `/uploads` 反向代理。

## 4. 配置

生产环境建议通过环境变量覆盖 `application.yml`：

```bash
export SPRING_DATASOURCE_URL='jdbc:mysql://127.0.0.1:3306/adult_edu?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false'
export SPRING_DATASOURCE_USERNAME='adultedu'
export SPRING_DATASOURCE_PASSWORD='ChangeMe123!'
export SPRING_DATA_REDIS_HOST='127.0.0.1'
export SPRING_DATA_REDIS_PASSWORD=''
export APP_JWT_SECRET='请替换为至少 32 位随机字符串'
export APP_UPLOAD_DIR='/opt/adulteducation/data/uploads'
```

关键安全项：

- `app.jwt.secret` 必须替换，长度 ≥ 32；
- MySQL、Redis 不要暴露公网；
- 生产环境关闭演示模式：`app.face.mock=false`、`app.sms.mock=false`、`app.wechat.mock=false`；
- 首次上线后立即修改 `admin` 默认密码；
- 通过 Nginx 配置 HTTPS。

## 5. 启动

```bash
sudo cp deploy/adult-education.service /etc/systemd/system/
sudo systemctl daemon-reload
sudo systemctl enable --now adult-education
sudo systemctl status adult-education
```

查看日志：

```bash
journalctl -u adult-education -f
```

## 6. Nginx

```bash
sudo cp deploy/nginx.conf /etc/nginx/conf.d/adult-education.conf
sudo nginx -t
sudo systemctl reload nginx
```

访问：

- 管理后台：`https://edu.example.com/admin/`
- 学员 H5：`https://edu.example.com/m/`
- Swagger：`https://edu.example.com/swagger-ui.html`（建议生产环境关闭）

Nginx 已配置 `client_max_body_size 520m`，与后端 500MB 视频上传上限匹配。

## 7. 备份与维护

```bash
# 每日备份数据库
mysqldump -uadultedu -p adult_edu | gzip > /backup/adult_edu_$(date +%F).sql.gz

# 备份上传文件
tar czf /backup/uploads_$(date +%F).tar.gz /opt/adulteducation/data/uploads
```

定时任务（应用内已实现）：

- 每日 02:00 自动结束到期培训班；
- 每日 02:30 清理 180 天前核验抓拍图；
- 每日 09:00 汇总提醒超过 24 小时的待审批事项。

## 8. 升级

```bash
sudo systemctl stop adult-education
# 替换 jar 与静态资源
sudo systemctl start adult-education
```

`schema.sql` 与 `data.sql` 均为幂等脚本，升级重启不会清空业务数据；如涉及字段变更，请按版本补充 `ALTER TABLE` 脚本后再启动。
