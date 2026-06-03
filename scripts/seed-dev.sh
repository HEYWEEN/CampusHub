#!/usr/bin/env bash
# 本地测试数据 seeder —— 走后端 API 造数据（过真实校验，自动处理 phone 加密/信用/认证）。
# 用法：先 ./start.sh local 起后端（8080），再 bash scripts/seed-dev.sh
# 幂等性：重复运行会重复造任务/商品（每次新增），账号是按手机号 get-or-create。
set -euo pipefail
BASE="${BASE:-http://localhost:8080}"
SMS_CODE="123456"   # dev MockSmsSender 固定验证码

jqv() { python3 -c "import sys,json;print(json.load(sys.stdin).get('data',{}).get('$1',''))" 2>/dev/null; }

# 登录(首次自动建账) → 提交认证 → dev-approve → 补 100 启动积分 → 回显 token
prepare_user() {
  local phone="$1" name="$2"
  curl -s -X POST "$BASE/api/auth/sms-codes" -H 'Content-Type: application/json' \
    -d "{\"phone\":\"$phone\",\"scene\":\"LOGIN_REGISTER\"}" >/dev/null
  sleep 0.3
  local tok
  tok=$(curl -s -X POST "$BASE/api/auth/token" -H 'Content-Type: application/json' \
    -d "{\"phone\":\"$phone\",\"smsCode\":\"$SMS_CODE\"}" | jqv accessToken)
  if [ -z "$tok" ]; then echo "  ⚠️ $phone 登录失败(可能被限流),跳过" >&2; return 1; fi
  curl -s -X POST "$BASE/api/auth/verifications" -H "Authorization: Bearer $tok" -H 'Content-Type: application/json' \
    -d "{\"realName\":\"$name\",\"studentNo\":\"$phone\",\"idCard\":\"320100200001010000\",\"attachmentUrls\":[\"/uploads/dev-id.png\"]}" >/dev/null || true
  curl -s -X POST "$BASE/api/auth/verifications/me/dev-approve" -H "Authorization: Bearer $tok" >/dev/null || true
  curl -s -X POST "$BASE/api/auth/credit/me/dev-signup-bonus" -H "Authorization: Bearer $tok" >/dev/null || true
  echo "$tok"
}

iso_in() { python3 -c "import sys,datetime;print((datetime.datetime.now().astimezone()+datetime.timedelta(hours=int(sys.argv[1]))).isoformat())" "$1"; }

publish_task() { # token type title reward hours pickup building remark
  local tok="$1" type="$2" title="$3" reward="$4" hours="$5" pickup="$6" building="$7" remark="$8"
  curl -s -X POST "$BASE/api/tasks" -H "Authorization: Bearer $tok" -H 'Content-Type: application/json' \
    -d "{\"taskType\":\"$type\",\"title\":\"$title\",\"rewardPoint\":$reward,\"deadlineAt\":\"$(iso_in "$hours")\",\"pickupHint\":\"$pickup\",\"deliveryBuilding\":\"$building\",\"remark\":\"$remark\"}" \
    | python3 -c "import sys,json;d=json.load(sys.stdin);print('  ✓ 任务',d.get('data',{}).get('taskId'),'$title') if d.get('code')==0 else print('  ✗ $title:',d.get('message'))"
}

publish_item() { # token title desc price locDetail
  local tok="$1" title="$2" desc="$3" price="$4" loc="$5"
  curl -s -X POST "$BASE/api/trade/items" -H "Authorization: Bearer $tok" -H 'Content-Type: application/json' \
    -d "{\"title\":\"$title\",\"description\":\"$desc\",\"pricePoint\":$price,\"pickupLocationType\":\"BUILDING_RANGE\",\"pickupLocationDetail\":\"$loc\",\"imageUrls\":[\"/uploads/dev-item.png\"]}" \
    | python3 -c "import sys,json;d=json.load(sys.stdin);print('  ✓ 商品','$title') if d.get('code')==0 else print('  ✗ $title:',d.get('message'))"
}

echo "→ 准备测试用户..."
T1=$(prepare_user 13900000001 "测试小明") || T1=""
T2=$(prepare_user 13900000002 "测试小红") || T2=""
T3=$(prepare_user 13900000003 "测试学长") || T3=""

echo "→ 发布任务..."
[ -n "$T1" ] && publish_task "$T1" ERRAND "帮取快递到紫金楼" 8 6 "菜鸟驿站" "紫金楼" "两个中等包裹,辛苦啦"
[ -n "$T1" ] && publish_task "$T1" ERRAND "代拿外卖到南雍苑" 6 3 "南门外卖柜" "南雍苑" "麻辣烫一份,趁热"
[ -n "$T1" ] && publish_task "$T1" MUTUAL_HELP "搬宿舍搭把手" 20 48 "见面沟通" "17舍" "周末搬几箱书"
[ -n "$T2" ] && publish_task "$T2" ERRAND "图书馆占座(早八)" 5 12 "线下" "杜厦图书馆" "二楼靠窗最佳"
[ -n "$T2" ] && publish_task "$T2" TUTOR "高数II期末答疑1次" 40 72 "线上腾讯会议" "线上" "重点二重积分"
[ -n "$T3" ] && publish_task "$T3" TUTOR "Python入门带飞" 50 96 "线下面授" "仙林图书馆研讨间" "零基础,目标做出小项目"
[ -n "$T3" ] && publish_task "$T3" MUTUAL_HELP "借用科学计算器" 5 24 "面交" "计科楼" "考试用,当天还"

echo "→ 发布二手..."
[ -n "$T3" ] && publish_item "$T3" "九成新 Kindle Paperwhite" "屏幕无划痕,带原装保护套" 200 "仙林 启明苑18栋"
[ -n "$T2" ] && publish_item "$T2" "考研数学复习全书(无笔记)" "今年最新版,几乎全新" 30 "杜厦图书馆面交"
[ -n "$T1" ] && publish_item "$T1" "小型电饭煲(单人份)" "用了一学期,功能正常" 50 "紫金楼楼下"

echo "✅ seed 完成"
