# 外卖系统 · Ansible 部署（快速上手）

把外卖系统部署到**一台 Linux 虚拟机**：Nginx 双后端负载均衡 + MinIO 对象存储 + MySQL + Redis，
外加 Zabbix 监控与「本地 + MinIO」双份数据库备份。

- 控制节点：WSL2（已装 Docker 与 Ansible）
- 目标机：Rocky / CentOS / AlmaLinux 9，root 可 SSH 登录

完整的架构说明、运维与回滚见 [`../../docs/部署文档.md`](../../docs/部署文档.md)，
部署后的逐项自检见 [`../../docs/部署验收清单.md`](../../docs/部署验收清单.md)。

---

## 五步走

```bash
cd deploy/ansible          # 以下命令都在这个目录执行

# 1) 主机清单：填目标机 IP 与私钥路径
cp inventory.ini.example inventory.ini
vi inventory.ini

# 2) 密钥：填 MySQL / Redis / MinIO / JWT 密码
cp group_vars/all/vault.yml.example group_vars/all/vault.yml
vi group_vars/all/vault.yml
#    生成随机串可用：openssl rand -base64 32

# 3) 加密密钥文件（推荐；之后跑 playbook 要带 --ask-vault-pass）
ansible-vault encrypt group_vars/all/vault.yml

# 4) 非敏感变量：按需改 Zabbix Server 地址、MinIO 控制台绑定地址
vi group_vars/all/main.yml

# 5) 安装集合 → 执行部署
ansible-galaxy collection install -r requirements.yml
ansible-playbook site.yml --ask-vault-pass
```

> 第 3 步若跳过（不加密），第 5 步就不要带 `--ask-vault-pass`。

## 执行过程

| 阶段     | 在哪台机 | 做什么                                                                     |
| -------- | -------- | -------------------------------------------------------------------------- |
| 前置检查 | 目标机   | 校验发行版、检查 80/81 端口是否被占                                        |
| 构建镜像 | 控制节点 | `docker build` 后端与网关 → `docker save` 成 tar                           |
| 部署     | 目标机   | 补基础环境（chrony/NTP）→ 装 Docker → 日志 → 防火墙 → 起服务 → 备份 → 监控 |

## 常用变量（`group_vars/all/main.yml`）

| 变量                  | 说明                                                                    |
| --------------------- | ----------------------------------------------------------------------- |
| `app_version`         | 应用镜像版本（compose 与构建共用这个值）                                |
| `rebuild_images`      | `true` 每次重新构建并重传镜像；`false` 只做配置类变更                   |
| `zabbix_server_ip`    | Zabbix Server 地址（不装监控就先留占位，装的时候会校验非空）            |
| `minio_console_bind`  | MinIO 控制台绑定地址，默认 `0.0.0.0`；想只让局域网访问就填目标机内网 IP |
| `configure_firewalld` | 是否配置 firewalld（RHEL 系默认 true）                                  |

## 更新代码后重新发布

```bash
ansible-playbook site.yml --ask-vault-pass      # rebuild_images=true 时会重新构建并滚动更新
```

## 出问题先看这里

```bash
# 目标机上
cd /opt/takeout && docker compose --env-file .env ps
docker compose --env-file .env logs --tail=100
systemctl status takeout-backup.timer zabbix-agent2
```

- 拉镜像慢/失败：改 `docker_registry_mirrors`（`group_vars/all/main.yml`）
- 监控项取不到值：`/usr/sbin/zabbix_agent2 -t "takeout.tcp[80]"` 应返回 `[t|1]`
- 备份没上传 MinIO：手动跑 `/opt/takeout/backup/backup-db.sh` 看输出
