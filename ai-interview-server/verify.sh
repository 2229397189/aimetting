#!/usr/bin/env bash
# 验证脚本：验证 /api/interview/sessions、/api/admin 已注册，且 404 不再被掩盖成 B0001/500
# 输出统一写入 verify-out.txt
BASE="http://127.0.0.1:18081"
OUT="verify-out.txt"
CURL="curl -s --noproxy * -m 20"

: > "$OUT"

log() { echo "$*" >> "$OUT"; }

log "############ (a) 全量已注册路由（来自 springdoc /v3/api-docs） ############"
$CURL "$BASE/v3/api-docs" | tr ',' '\n' | grep -o '"/api/[^"]*"' | sort -u >> "$OUT" 2>&1

log ""
log "############ (b1) 无 token 访问真实路径 GET /api/interview/sessions ############"
$CURL -i "$BASE/api/interview/sessions" >> "$OUT" 2>&1

log ""
log "############ (b2) 无 token 访问团队长测试用路径 GET /api/interviews（非真实端点） ############"
$CURL -i "$BASE/api/interviews" >> "$OUT" 2>&1

log ""
log "############ (c1) POST /api/auth/register ############"
$CURL -i -X POST "$BASE/api/auth/register" \
  -H 'Content-Type: application/json' \
  -d '{"username":"koutest01","password":"test12345","email":"koutest01@example.com","nickname":"寇豆码"}' >> "$OUT" 2>&1

log ""
log "############ (c2) POST /api/auth/login ############"
LOGIN_JSON=$($CURL -X POST "$BASE/api/auth/login" \
  -H 'Content-Type: application/json' \
  -d '{"username":"koutest01","password":"test12345"}')
echo "$LOGIN_JSON" >> "$OUT"
TOKEN=$(echo "$LOGIN_JSON" | grep -o '"accessToken":"[^"]*"' | head -1 | sed 's/"accessToken":"//;s/"$//')
log ""
log "提取到的 accessToken = ${TOKEN:0:40}...(长度 ${#TOKEN})"

log ""
log "############ (c3) POST /api/interview/sessions 创建会话（带 token） ############"
$CURL -i -X POST "$BASE/api/interview/sessions" \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"directions":["JAVA"],"difficulty":"MEDIUM","totalQuestion":3}' >> "$OUT" 2>&1

log ""
log "############ (c4) GET /api/interview/sessions 分页（带 token） ############"
$CURL -i "$BASE/api/interview/sessions" -H "Authorization: Bearer $TOKEN" >> "$OUT" 2>&1

log ""
log "############ (d) GET /api/admin/users（带 token，普通用户预期 A0301） ############"
$CURL -i "$BASE/api/admin/users" -H "Authorization: Bearer $TOKEN" >> "$OUT" 2>&1

log ""
log "############ (e) GET /api/definitely-not-exist（预期 A0404 + 404） ############"
$CURL -i "$BASE/api/definitely-not-exist" >> "$OUT" 2>&1

log ""
log "############ (f) GET /api/user/stats 回归（带 token） ############"
$CURL -i "$BASE/api/user/stats" -H "Authorization: Bearer $TOKEN" >> "$OUT" 2>&1

log ""
log "############ DONE ############"
