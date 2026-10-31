# PluxMatch

> Match 匹配插件，附属于 Plux 系列，原为个人自用插件，现开源。

PluxMatch 是一款面向 **BungeeCord / Velocity** 代理的跨服匹配插件。平台无关核心下沉到 `PluxMatch-Common`，代理端只做薄适配层，后端 Bukkit/Spigot/Paper 通过 `PluxMatch-Bukkit` 把命令与 GUI 转发回代理。匹配采用「每人独立找房」的轻量模型。

## ✨ 特性

- **跨平台**：支持 BungeeCord 与 Velocity 3.x；单一 jar 覆盖 Bukkit **1.7 → 最新** 全版本
- **匹配算法**：`FIFO`、`POPULATION`（population-mode：`RANDOM` / `LEAST` / `MOST`）
- **ELO 段位系统**：动态 K 值、`expand_elo` 超时策略（等待越久，可接受的分段带宽越宽）、段位（青铜 → 大师）晋升事件
- **组队匹配（Party）**：创建 / 邀请 / 接受 / 拒绝 / 踢出 / 解散 / 组队进队列
- **服务器池管理**：ping 健康检查、最大失败次数、回退服务器、按组分配房间
- **多后端存储**：MySQL / SQLite / MongoDB（HikariCP 连接池）
- **Redis 跨代理同步**：多代理共享队列与玩家数据
- **Chest GUI**：匹配主菜单与房间列表，跨版本材质 / Title 自动降级
- **排行榜 / 个人统计 / 对局历史**
- **PlaceholderAPI** 变量支持
- **配置热重载** `/matchadmin reload`

## 🧩 模块结构

| 模块 | 说明 | 产物 |
|------|------|------|
| `PluxMatch-Common` | 平台无关核心（匹配 / ELO / 段位 / 队列 / 队伍 / 存储 / API） | `PluxMatch-Common-1.5.2.jar`（被 shade 进代理端产物） |
| `PluxMatch-Bungee` | BungeeCord 适配层 | `PluxMatch-1.5.2.jar`（shaded，放**代理**） |
| `PluxMatch-Velocity` | Velocity 适配层 | `PluxMatch-Velocity-1.5.2.jar`（shaded，放**代理**） |
| `PluxMatch-Bukkit` | 后端命令 / GUI 转发辅助 | `PluxMatch-Bukkit-1.5.2.jar`（放**后端服务器**） |

## 📥 下载

前往 [Releases](https://github.com/Reationura/Plux-Match/releases) 下载对应平台的 jar：

- 用 BungeeCord → `PluxMatch-1.5.2.jar`
- 用 Velocity → `PluxMatch-Velocity-1.5.2.jar`
- 每个后端游戏服 → `PluxMatch-Bukkit-1.5.2.jar`

## 🔧 编译构建

**前置要求**

- JDK **8 或更高**（源码按 `--release 8` 编译，产物字节码为 Java 8，可运行于老 Java 8 代理 / 服务端）
- Maven **3.6+**

**构建步骤**

```bash
git clone https://github.com/Reationura/Plux-Match.git
cd Plux-Match
mvn clean package
```

构建完成后，产物位于各模块的 `target/` 目录：

```
PluxMatch-Bungee/target/PluxMatch-1.5.2.jar
PluxMatch-Velocity/target/PluxMatch-Velocity-1.5.2.jar
PluxMatch-Bukkit/target/PluxMatch-Bukkit-1.5.2.jar
PluxMatch-Common/target/PluxMatch-Common-1.5.2.jar   # 核心库，已自动 shade 进代理端产物
```

> 首次构建会从 Paper / Spigot / Sonatype / PlaceholderAPI 仓库拉取依赖，请保证网络可达。
> 代理端（Bungee / Velocity）为含全部依赖的 shaded 胖 jar，无需额外前置库。

## 🚀 安装部署

1. **代理端**：将 `PluxMatch`（BungeeCord 或 Velocity 其中之一的产物）放入代理 `plugins/`。
2. **后端服务器**：将 `PluxMatch-Bukkit` 放入每个后端游戏服 `plugins/`，用于把 `/match gui`、房间列表、匹配结果等命令与插件消息转发回代理。
3. 首次启动后在 `plugins/PluxMatch/`（或代理插件目录）生成配置，按需修改后 `/matchadmin reload`。

## ⚙️ 配置说明

| 文件 | 作用 |
|------|------|
| `config.yml` | 主配置：数据库、Redis、匹配 / ELO / 段位、排行榜、服务器、消息、安全 |
| `queues.yml` | 队列定义与队列分组（用于 GUI） |
| `servers.yml` | 服务器池 / 分组 |
| `gui.yml` | GUI 物品与文本 |
| `messages.yml` | 消息文案 |

`config.yml` 关键片段：

```yaml
database:
  type: sqlite        # sqlite | mysql | mongodb

matchmaking:
  default-algorithm: FIFO
  elo:
    initial: 1000
    k-factor: 32
  timeout-strategy: expand_elo     # 等待越久，ELO 分段带宽越宽
  elo-band:
    initial: 100
    max: 500
    per-second: 25

redis:
  enabled: false      # 多代理同步时开启

tiers:                # 段位（按 ELO 阈值映射展示名，含颜色代码）
  bronze:   { min-elo: 0,    display: "&c青铜" }
  # ... silver / gold / platinum / diamond / master

server:
  ping-interval: 5
  max-failures: 3
  fallback-server: lobby
```

`queues.yml` 片段：

```yaml
queues:
  solo-bedwars:
    name: "单人起床战争"
    enabled: true
    group: bedwars
    min-players: 2
    max-players: 16
    team-size: 1
    algorithm: POPULATION
    population-mode: RANDOM
    allow-party: false
    server-group: bedwars

queue-groups:
  bedwars:
    name: "起床战争"
    queues: [solo-bedwars, team-bedwars, vip-bedwars]
```

## 🎮 命令

**玩家命令** `/match`（别名 `pm` / `pp` / `matchmaking`）

| 命令 | 说明 |
|------|------|
| `/match` | 查看帮助 |
| `/match gui <组名>` | 打开指定组的匹配 GUI |
| `/match join <队列>` | 加入匹配队列 |
| `/match leave` | 退出队列 |
| `/match status` | 查看匹配状态 |
| `/match party create\|invite\|accept\|deny\|kick\|disband\|list\|join` | 组队操作 |
| `/match history` | 查看对局记录 |
| `/match stats` | 查看个人统计 |
| `/match leaderboard [winrate]` | 查看排行榜 |

**管理员命令** `/matchadmin`（别名 `pma` / `matchadm`）

| 命令 | 说明 |
|------|------|
| `/matchadmin reload` | 重载配置 |
| `/matchadmin queue create\|delete\|enable\|disable\|list <名称>` | 队列管理 |
| `/matchadmin add <玩家> <队列>` | 强制加入队列 |
| `/matchadmin remove <玩家>` | 强制移出队列 |
| `/matchadmin server add <服务器> <组名> \| remove <服务器> \| list` | 服务器池管理 |
| `/matchadmin forcestart <队列>` | 强制触发匹配 |
| `/matchadmin stats <玩家>` | 查看指定玩家统计 |

## 🔑 权限节点

| 节点 | 说明 |
|------|------|
| `pluxmatch.admin` | 代理端管理命令的门控权限 |
| `match.admin` | 核心逻辑校验的管理权限（默认 op） |
| `match.queue.<队列ID>` | 进入特定队列的权限 |

> 管理命令在代理端由 `pluxmatch.admin` 放行、在核心由 `match.admin` 校验，建议对管理员**同时授予两者**。

## 🪧 跨版本兼容（1.7 → 最新）

- **物品材质**：1.13+ 使用扁平化材质名（`LIME_WOOL` 等）；1.7–1.12 自动回退旧材质名 + 数据（`WOOL` / `INK_SACK` / `STAINED_GLASS_PANE`）。
- **Title**：≥1.14 用五参重载，≥1.8 用两参，1.7 无 Title API 时降级为聊天消息。
- **代理端**：老构建缺少 `createTitle` / `ACTION_BAR` 时，Title 退化为普通消息、ActionBar 静默跳过。
- **Velocity**：仅支持 3.x 系列。

## 📄 许可

本项目基于 [LICENSE](LICENSE) 开源。
