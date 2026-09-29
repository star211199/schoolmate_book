#!/usr/bin/env bash
#
# 重建演示数据（可在 reset-demo.sh 之后执行）：
#   1. admin 登录
#   2. POST /api/classes 建班 —— 这一步会联动创建「班级群 + 群会话」
#   3. PUT  /api/classes/{id} 补上毕业日期（建班入参不含该字段）
#   4. 把邀请码改回 CLASS01（建班接口生成的是随机码）
#   5. zhangsan 用邀请码加入班级 —— 验证成员同步进班级群
#   6. 校验两人的会话列表里都出现班级群
#
set -euo pipefail

BASE="${SCHOOLMATE_BASE:-$HOME/schoolmate_book}"
. "$BASE/app/env.sh"
export LD_LIBRARY_PATH="$BASE/runtime/lib:${LD_LIBRARY_PATH:-}"

API="http://127.0.0.1:${SERVER_PORT:-8080}/api"
CLASS_NAME="2024级智能科学与技术1班"
INVITE="CLASS01"

py() { python3 -c "$1"; }

login() {
  curl -s -X POST "$API/auth/login" -H 'Content-Type: application/json' \
    -d "{\"username\":\"$1\",\"password\":\"123456\"}" \
    | py "import json,sys;print(json.load(sys.stdin).get('data',{}).get('token',''))"
}

echo "==> admin 登录"
ADMIN_TK="$(login admin)"
[ -n "$ADMIN_TK" ] || { echo "!! admin 登录失败"; exit 1; }
echo "    ok"

echo "==> 创建班级：$CLASS_NAME"
CREATE=$(curl -s -X POST "$API/classes" -H "Authorization: Bearer $ADMIN_TK" -H 'Content-Type: application/json' \
  -d "{\"className\":\"$CLASS_NAME\",\"grade\":\"2024级\",\"major\":\"智能科学与技术\",\"description\":\"广东技术师范大学 · 智能科学与技术1班\"}")
echo "    $CREATE" | head -c 300; echo
CLASS_ID=$(echo "$CREATE" | py "import json,sys;print(json.load(sys.stdin).get('data',{}).get('id',''))")
[ -n "$CLASS_ID" ] || { echo "!! 建班失败"; exit 1; }

echo "==> 补毕业日期 2028-06-30"
curl -s -X PUT "$API/classes/$CLASS_ID" -H "Authorization: Bearer $ADMIN_TK" -H 'Content-Type: application/json' \
  -d "{\"className\":\"$CLASS_NAME\",\"grade\":\"2024级\",\"major\":\"智能科学与技术\",\"graduationDate\":\"2028-06-30\",\"description\":\"广东技术师范大学 · 智能科学与技术1班\"}" \
  | head -c 200; echo

echo "==> 邀请码改回 $INVITE"
"$BASE/runtime/mysql/bin/mysql" "--defaults-file=$BASE/app/my.cnf" -u root -p"$DB_ROOT_PASSWORD" "$DB_NAME" \
  -e "UPDATE class_info SET invite_code='$INVITE' WHERE id=$CLASS_ID;" 2>/dev/null

echo "==> zhangsan 用邀请码加入"
ZS_TK="$(login zhangsan)"
[ -n "$ZS_TK" ] || { echo "!! zhangsan 登录失败"; exit 1; }
curl -s -X POST "$API/classes/join" -H "Authorization: Bearer $ZS_TK" -H 'Content-Type: application/json' \
  -d "{\"inviteCode\":\"$INVITE\"}" | head -c 200; echo

echo "==> 校验班级群"
curl -s "$API/groups" -H "Authorization: Bearer $ADMIN_TK" \
  | py "import json,sys
d=json.load(sys.stdin).get('data') or []
for g in d: print('    群:', g.get('groupName'), '| 类型', g.get('groupType'), '| 成员数', g.get('memberCount'), '| sessionId', g.get('sessionId'))"

echo "==> 校验 admin / zhangsan 的会话列表"
for u in admin zhangsan; do
  TK="$(login $u)"
  echo "    [$u]"
  curl -s "$API/chat/sessions" -H "Authorization: Bearer $TK" \
    | py "import json,sys
d=json.load(sys.stdin).get('data') or []
for s in d: print('      ', s.get('sessionType'), s.get('title'), '| 未读', s.get('unreadCount'))"
done

echo
echo "演示数据就绪：admin/123456（班级创建者）、zhangsan/123456；邀请码 $INVITE"
