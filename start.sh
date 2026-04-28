#!/usr/bin/env bash
# CampusHub 一键启动脚本
# ─────────────────────────────────────────────────────────────
# 用法：
#   ./start.sh           本地开发模式（默认）
#   ./start.sh local     同上：mvnw spring-boot:run + vite dev
#   ./start.sh prod      生产模式：构建后用 java -jar + vite preview
#   ./start.sh stop      停止 prod 模式后台进程
#   ./start.sh help      显示帮助
#
# 平台：macOS / Linux / Windows (Git Bash 或 WSL)
# Windows 原生 CMD/PowerShell 跑不了 .sh —— 请用 Git Bash。
# ─────────────────────────────────────────────────────────────

set -u

ROOT="$(cd "$(dirname "$0")" && pwd)"
BACKEND="$ROOT/backend"
FRONTEND="$ROOT/frontend"
LOG_DIR="$ROOT/.run-logs"
PID_DIR="$ROOT/.run-pids"
mkdir -p "$LOG_DIR" "$PID_DIR"

# ============================================================
# 配色 & 输出（256 色柔和调色板，支持 NO_COLOR 标准）
# ============================================================
if [[ -t 1 && -z "${NO_COLOR:-}" ]]; then
  C_INFO=$'\033[38;5;110m'   # 浅钢蓝 → 信息
  C_OK=$'\033[38;5;78m'      # 玉绿   → 成功
  C_WARN=$'\033[38;5;214m'   # 暖橙   → 警告
  C_ERR=$'\033[38;5;203m'    # 珊瑚红 → 错误
  C_TODO=$'\033[38;5;141m'   # 淡紫   → 待办
  C_DIM=$'\033[38;5;245m'    # 灰     → 次要
  C_BOLD=$'\033[1m'
  C_RST=$'\033[0m'
else
  C_INFO=""; C_OK=""; C_WARN=""; C_ERR=""; C_TODO=""; C_DIM=""; C_BOLD=""; C_RST=""
fi

info() { printf "  ${C_INFO}→${C_RST}  %s\n" "$*"; }
ok()   { printf "  ${C_OK}✓${C_RST}  %s\n" "$*"; }
warn() { printf "  ${C_WARN}⚠${C_RST}  %s\n" "$*"; }
err()  { printf "  ${C_ERR}✗${C_RST}  %s\n" "$*"; }
todo() { printf "  ${C_TODO}●${C_RST}  ${C_BOLD}%s${C_RST}\n" "$*"; }
hint() { printf "      ${C_DIM}╰─ %s${C_RST}\n" "$*"; }
hr()   { printf "${C_DIM}  ────────────────────────────────────────────${C_RST}\n"; }

banner() {
  printf "\n${C_BOLD}  CampusHub${C_RST} ${C_DIM}· 一键启动 · 模式: ${C_RST}${C_BOLD}%s${C_RST}\n" "$1"
  hr
}

# ============================================================
# OS 检测
# ============================================================
case "$(uname -s)" in
  Darwin)               OS="mac" ;;
  Linux)                OS="linux" ;;
  MINGW*|MSYS*|CYGWIN*) OS="windows" ;;
  *)                    OS="unknown" ;;
esac

if [[ "$OS" == "windows" ]]; then
  MVNW="./mvnw.cmd"
else
  MVNW="./mvnw"
  chmod +x "$BACKEND/mvnw" 2>/dev/null || true
fi

# ============================================================
# 公共工具
# ============================================================
need_cmd() {
  if ! command -v "$1" >/dev/null 2>&1; then
    err "缺少命令 ${C_BOLD}$1${C_RST} — $2"
    return 1
  fi
}

check_basic_deps() {
  local missing=0
  need_cmd node "请安装 Node.js >= 18（建议 20 LTS）" || missing=1
  need_cmd npm  "随 Node.js 安装"                     || missing=1
  need_cmd java "请安装 Java 17+"                     || missing=1
  [[ $missing -eq 1 ]] && exit 1
  ok "Node / Java 命令检查通过"
}

write_pid()  { echo "$2" > "$PID_DIR/$1.pid"; }
read_pid()   { [[ -f "$PID_DIR/$1.pid" ]] && cat "$PID_DIR/$1.pid"; }
clear_pid()  { rm -f "$PID_DIR/$1.pid"; }

# ============================================================
# Mode: local（开发，默认）
# ============================================================
mode_local() {
  banner "local"
  info "系统：${C_BOLD}${OS}${C_RST}"
  check_basic_deps

  # 后端本地配置
  local local_prop="$BACKEND/src/main/resources/application-local.properties"
  local local_example="$BACKEND/src/main/resources/application-local.properties.example"

  if [[ ! -f "$local_prop" ]]; then
    cp "$local_example" "$local_prop"
    todo "已生成 application-local.properties"
    hint "请编辑 $local_prop 填入 MySQL 密码后重新运行"
    exit 1
  fi
  if grep -q "你的MySQL密码" "$local_prop" 2>/dev/null; then
    todo "application-local.properties 中的密码仍是占位符"
    hint "$local_prop"
    exit 1
  fi
  ok "后端本地配置就绪"

  # MySQL 提示（不阻断）
  if command -v mysqladmin >/dev/null 2>&1 && mysqladmin ping -h 127.0.0.1 --silent 2>/dev/null; then
    ok "MySQL 运行中（127.0.0.1:3306）"
  else
    warn "MySQL 未运行（127.0.0.1:3306）"
    case "$OS" in
      mac)     hint "启动：brew services start mysql" ;;
      linux)   hint "启动：sudo systemctl start mysql" ;;
      windows) hint "启动（管理员）：net start MySQL80" ;;
    esac
    hint "首次需建库：CREATE DATABASE campushub CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
  fi

  # 前端依赖
  if [[ ! -d "$FRONTEND/node_modules" ]]; then
    info "安装前端依赖（npm install）..."
    ( cd "$FRONTEND" && npm install ) || { err "npm install 失败"; exit 1; }
  fi
  ok "前端依赖就绪"

  hr
  info "启动后端 → ${C_DIM}$LOG_DIR/backend.log${C_RST}"
  ( cd "$BACKEND" && exec $MVNW spring-boot:run ) > "$LOG_DIR/backend.log" 2>&1 &
  local be_pid=$!

  info "启动前端 → ${C_DIM}$LOG_DIR/frontend.log${C_RST}"
  ( cd "$FRONTEND" && exec npm run dev ) > "$LOG_DIR/frontend.log" 2>&1 &
  local fe_pid=$!

  cleanup_local() {
    trap - INT TERM EXIT
    printf "\n"
    info "停止前后端..."
    kill "$be_pid" "$fe_pid" 2>/dev/null || true
    sleep 1
    kill -9 "$be_pid" "$fe_pid" 2>/dev/null || true
    ok "已退出"
  }
  trap cleanup_local INT TERM EXIT

  printf "\n  ${C_OK}${C_BOLD}已启动${C_RST}\n"
  printf "  ${C_DIM}前端${C_RST}  http://localhost:5173\n"
  printf "  ${C_DIM}后端${C_RST}  http://localhost:8080\n"
  printf "  ${C_DIM}日志${C_RST}  tail -f $LOG_DIR/{backend,frontend}.log\n"
  printf "  ${C_DIM}停止${C_RST}  ${C_BOLD}Ctrl+C${C_RST}\n\n"

  wait
}

# ============================================================
# Mode: prod（生产，构建+运行）
# ============================================================
mode_prod() {
  banner "prod"
  info "系统：${C_BOLD}${OS}${C_RST}"
  check_basic_deps

  # 1. 加载 .env.prod
  local env_file="$ROOT/.env.prod"
  local env_example="$ROOT/.env.prod.example"
  if [[ ! -f "$env_file" ]]; then
    todo "缺少 .env.prod"
    if [[ -f "$env_example" ]]; then
      hint "请执行：cp .env.prod.example .env.prod 后填入真实值"
    else
      hint "请创建 .env.prod 文件并填入 DB_HOST / DB_USERNAME / DB_PASSWORD 等"
    fi
    exit 1
  fi
  set -a
  # shellcheck disable=SC1090
  source "$env_file"
  set +a
  ok ".env.prod 已加载"

  # 2. 关键变量检查
  local req_vars=(DB_USERNAME DB_PASSWORD)
  local v missing=0
  for v in "${req_vars[@]}"; do
    if [[ -z "${!v:-}" ]]; then
      err ".env.prod 中缺失：${C_BOLD}$v${C_RST}"
      missing=1
    fi
  done
  [[ $missing -eq 1 ]] && exit 1

  # 3. 检查 prod profile 配置
  local prod_prop="$BACKEND/src/main/resources/application-prod.properties"
  if [[ ! -f "$prod_prop" ]]; then
    err "缺少 application-prod.properties"
    hint "$prod_prop"
    exit 1
  fi
  ok "后端生产配置就绪"

  # 4. 前端 prod env（可选，缺失只警告）
  if [[ ! -f "$FRONTEND/.env.production" ]]; then
    warn "frontend/.env.production 不存在"
    hint "如需自定义 API 地址，请从 .env.production.example 复制并修改"
  fi

  # 5. 构建后端
  hr
  info "构建后端（mvnw clean package -DskipTests）..."
  ( cd "$BACKEND" && exec $MVNW -q clean package -DskipTests ) || { err "后端构建失败"; exit 1; }
  local jar
  jar=$(ls "$BACKEND/target"/*.jar 2>/dev/null | grep -v '\.original\.jar$' | head -1)
  [[ -z "$jar" ]] && { err "未找到产物 jar"; exit 1; }
  ok "后端产物：${C_DIM}${jar#$ROOT/}${C_RST}"

  # 6. 构建前端
  info "构建前端（npm ci && npm run build）..."
  if [[ ! -d "$FRONTEND/node_modules" ]]; then
    ( cd "$FRONTEND" && npm ci ) || { err "npm ci 失败"; exit 1; }
  fi
  ( cd "$FRONTEND" && npm run build ) || { err "前端构建失败"; exit 1; }
  ok "前端产物：${C_DIM}frontend/dist${C_RST}"

  # 7. 启动
  hr
  export SPRING_PROFILES_ACTIVE=prod

  info "启动后端 java -jar → ${C_DIM}$LOG_DIR/backend.log${C_RST}"
  ( exec java -jar "$jar" ) > "$LOG_DIR/backend.log" 2>&1 &
  local be_pid=$!
  write_pid backend "$be_pid"

  info "启动前端 vite preview → ${C_DIM}$LOG_DIR/frontend.log${C_RST}"
  ( cd "$FRONTEND" && exec npm run preview -- --host 0.0.0.0 --port 4173 ) > "$LOG_DIR/frontend.log" 2>&1 &
  local fe_pid=$!
  write_pid frontend "$fe_pid"

  cleanup_prod() {
    trap - INT TERM EXIT
    printf "\n"
    info "停止前后端..."
    kill "$be_pid" "$fe_pid" 2>/dev/null || true
    sleep 1
    kill -9 "$be_pid" "$fe_pid" 2>/dev/null || true
    clear_pid backend; clear_pid frontend
    ok "已退出"
  }
  trap cleanup_prod INT TERM EXIT

  printf "\n  ${C_OK}${C_BOLD}已启动 (prod)${C_RST}\n"
  printf "  ${C_DIM}前端${C_RST}  http://0.0.0.0:4173\n"
  printf "  ${C_DIM}后端${C_RST}  http://0.0.0.0:${SERVER_PORT:-8080}\n"
  printf "  ${C_DIM}日志${C_RST}  tail -f $LOG_DIR/{backend,frontend}.log\n"
  printf "  ${C_DIM}停止${C_RST}  Ctrl+C  ${C_DIM}或${C_RST}  ./start.sh stop\n"
  printf "  ${C_DIM}守护${C_RST}  生产建议改用 systemd / nohup ./start.sh prod &${C_RST}\n\n"

  wait
}

# ============================================================
# Mode: stop（停止后台进程）
# ============================================================
mode_stop() {
  banner "stop"
  local stopped=0 name pid
  for name in backend frontend; do
    pid=$(read_pid "$name" || true)
    if [[ -n "${pid:-}" ]] && kill -0 "$pid" 2>/dev/null; then
      kill "$pid" 2>/dev/null || true
      sleep 1
      kill -9 "$pid" 2>/dev/null || true
      ok "已停止 $name (pid=$pid)"
      stopped=1
    else
      info "$name 未在运行"
    fi
    clear_pid "$name"
  done
  [[ $stopped -eq 0 ]] && info "无活动进程"
}

# ============================================================
# Mode: help
# ============================================================
mode_help() {
  cat <<EOF

  ${C_BOLD}CampusHub 启动脚本${C_RST}

  ${C_DIM}用法${C_RST}
    ./start.sh [mode]

  ${C_DIM}模式${C_RST}
    ${C_BOLD}local${C_RST}    本地开发（默认）— mvnw spring-boot:run + vite dev
    ${C_BOLD}prod${C_RST}     服务器生产    — 构建后 java -jar + vite preview
    ${C_BOLD}stop${C_RST}     停止 prod 模式启动的后台进程
    ${C_BOLD}help${C_RST}     显示本帮助

  ${C_DIM}配置文件${C_RST}
    本地后端     backend/src/main/resources/application-local.properties
    本地前端     vite 默认即可（如需 .env.local 自行添加）
    生产后端     backend/src/main/resources/application-prod.properties  ${C_DIM}（仅引用 env，无明文）${C_RST}
    生产敏感值   .env.prod                                              ${C_DIM}（gitignore，从 .env.prod.example 复制）${C_RST}
    生产前端     frontend/.env.production                               ${C_DIM}（从 .env.production.example 复制）${C_RST}

  ${C_DIM}部署到服务器（典型流程）${C_RST}
    1. 在服务器装好 Java 17+ / Node 20+ / MySQL
    2. git clone 项目
    3. cp .env.prod.example .env.prod  并填值
    4. ./start.sh prod
    5. 守护进程（推荐）：写 systemd unit 调用 ./start.sh prod

EOF
}

# ============================================================
# Dispatcher
# ============================================================
MODE="${1:-local}"
case "$MODE" in
  local)              mode_local ;;
  prod|production)    mode_prod ;;
  stop)               mode_stop ;;
  help|--help|-h)     mode_help ;;
  *)
    err "未知模式：$MODE"
    mode_help
    exit 1
    ;;
esac
