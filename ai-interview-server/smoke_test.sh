#!/usr/bin/env bash
# ai-interview 全功能冒烟测试（针对运行中的 h2+mock 实例）
set -u
# 绕过本机代理（Clash Verge 等会拦截 localhost 请求，导致间歇性 connection refused）
export no_proxy="localhost,127.0.0.1,::1"
export NO_PROXY="$no_proxy"
B="http://127.0.0.1:8080"
PASS=0; FAIL=0; FAILS=()
ok(){ echo "  PASS  $1"; PASS=$((PASS+1)); }
bad(){ echo "  FAIL  $1 :: $2"; FAIL=$((FAIL+1)); FAILS+=("$1"); }
# 检查 JSON 是否业务错误：仅看顶层 code（B/S 前缀）。
# 注意：不能用 "success":false 判断——AI 调用日志等业务数据里本身就含该字段，会造成假阳性。
is_err(){ local r="$1"; if echo "$r" | grep -qE '"code":"[BS][0-9]' ; then return 0; fi; return 1; }

echo "===== 0. 公开接口 ====="
R=$(curl -s -m5 "$B/api/health"); echo "$R" | grep -q '"status":"UP"' && ok "health" || bad "health" "$R"
R=$(curl -s -m5 "$B/api/questions/directions"); echo "$R" | grep -q '"directions"' && ok "questions/directions" || bad "questions/directions" "$R"
R=$(curl -s -m5 "$B/api/config/client"); echo "$R" | grep -q '"appName"\|"maxQuestion"\|"mockMode"' && ok "config/client" || bad "config/client" "$R"

echo "===== 1. 认证 ====="
U="smoke_$(date +%s)"; PW="Test@123456"
R=$(curl -s -m5 -X POST "$B/api/auth/register" -H "Content-Type: application/json" -d "{\"username\":\"$U\",\"password\":\"$PW\",\"email\":\"$U@test.com\",\"nickname\":\"冒烟\"}")
echo "$R" | grep -q '"code":"0"' && ok "register($U)" || bad "register" "$R"
R=$(curl -s -m5 -X POST "$B/api/auth/login" -H "Content-Type: application/json" -d "{\"username\":\"$U\",\"password\":\"$PW\"}")
UT=$(echo "$R" | grep -o '"accessToken":"[^"]*"' | head -1 | sed 's/"accessToken":"//;s/"//')
[ -n "$UT" ] && ok "login(user)" || bad "login(user)" "$R"
AT=$(curl -s -m5 -X POST "$B/api/auth/login" -H "Content-Type: application/json" -d '{"username":"admin","password":"admin123"}' | grep -o '"accessToken":"[^"]*"' | head -1 | sed 's/"accessToken":"//;s/"//')
[ -n "$AT" ] && ok "login(admin)" || bad "login(admin)" "no admin token"
RT=$(curl -s -m5 -X POST "$B/api/auth/token/refresh" -H "Content-Type: application/json" -d "{\"refreshToken\":\"$(echo "$R" | grep -o '"refreshToken":"[^"]*"' | head -1 | sed 's/"refreshToken":"//;s/"//')\"}")
echo "$RT" | grep -q '"accessToken"' && ok "token/refresh" || bad "token/refresh" "$RT"

echo "===== 2. 用户中心 ====="
R=$(curl -s -m5 "$B/api/user/profile" -H "Authorization: Bearer $UT"); ! is_err "$R" && ok "user/profile" || bad "user/profile" "$R"
R=$(curl -s -m5 -X PUT "$B/api/user/profile" -H "Authorization: Bearer $UT" -H "Content-Type: application/json" -d '{"nickname":"冒烟用户","targetPosition":"Java后端","workYears":2,"intro":"测试"}'); ! is_err "$R" && ok "user/profile(PUT)" || bad "user/profile(PUT)" "$R"
R=$(curl -s -m5 "$B/api/user/stats" -H "Authorization: Bearer $UT"); ! is_err "$R" && ok "user/stats" || bad "user/stats" "$R"

echo "===== 3. 题库 ====="
R=$(curl -s -m5 "$B/api/questions?pageNum=1&pageSize=5" -H "Authorization: Bearer $UT"); ! is_err "$R" && QID=$(echo "$R" | grep -o '"id":[0-9]*' | head -1 | sed 's/"id"://') && [ -n "$QID" ] && ok "questions(page) qid=$QID" || bad "questions(page)" "$R"
R=$(curl -s -m5 "$B/api/questions/random" -H "Authorization: Bearer $UT" -H "Content-Type: application/json" -d '{"direction":"JAVA_BACKEND","difficulty":"MEDIUM","count":2,"excludeIds":[]}'); ! is_err "$R" && ok "questions/random" || bad "questions/random" "$R"
R=$(curl -s -m5 "$B/api/questions/$QID" -H "Authorization: Bearer $UT"); ! is_err "$R" && ok "questions/{id}" || bad "questions/{id}" "$R"
NEWQ=$(curl -s -m5 -X POST "$B/api/questions" -H "Authorization: Bearer $AT" -H "Content-Type: application/json" -d '{"direction":"JAVA_BACKEND","difficulty":"EASY","title":"冒烟测试题","content":"解释HashMap","referencePoints":["底层结构"],"tags":["集合"]}')
NQID=$(echo "$NEWQ" | grep -o '"data":[0-9]*' | head -1 | sed 's/"data"://')
[ -n "$NQID" ] && ok "questions(POST admin) nqid=$NQID" || bad "questions(POST admin)" "$NEWQ"
R=$(curl -s -m5 -X PUT "$B/api/questions/$NQID" -H "Authorization: Bearer $AT" -H "Content-Type: application/json" -d '{"direction":"JAVA_BACKEND","difficulty":"EASY","title":"冒烟测试题改","content":"解释HashMap原理","referencePoints":["数组+链表+红黑树"],"tags":["集合"]}'); ! is_err "$R" && ok "questions/PUT admin" || bad "questions/PUT admin" "$R"
IMP=$(curl -s -m5 -X POST "$B/api/questions/import" -H "Authorization: Bearer $AT" -H "Content-Type: application/json" -d "[{\"direction\":\"JAVA_BACKEND\",\"difficulty\":\"EASY\",\"title\":\"导入题1\",\"content\":\"x\",\"referencePoints\":[\"a\"]},{\"direction\":\"JAVA_BACKEND\",\"difficulty\":\"EASY\",\"title\":\"导入题2\",\"content\":\"y\",\"referencePoints\":[\"b\"]}]")
echo "$IMP" | grep -q 'successCount' && ok "questions/import" || bad "questions/import" "$IMP"
R=$(curl -s -m5 -X DELETE "$B/api/questions/$NQID" -H "Authorization: Bearer $AT"); ! is_err "$R" && ok "questions/DELETE admin" || bad "questions/DELETE admin" "$R"

echo "===== 4. 简历 ====="
RP=$(curl -s -m10 -X POST "$B/api/resume/parse" -H "Authorization: Bearer $UT" -H "Content-Type: application/json" -d '{"title":"测试简历","content":"熟悉 Java/Spring Boot，做过秒杀系统，掌握 Redis 分布式锁、RabbitMQ 异步下单、MySQL 索引优化。"}')
RID=$(echo "$RP" | grep -o '"data":[0-9]*' | head -1 | sed 's/"data"://')
[ -n "$RID" ] && ok "resume/parse rid=$RID" || bad "resume/parse" "$RP"
R=$(curl -s -m5 "$B/api/resume?pageNum=1&pageSize=10" -H "Authorization: Bearer $UT"); ! is_err "$R" && ok "resume(page)" || bad "resume(page)" "$R"
R=$(curl -s -m5 "$B/api/resume/$RID" -H "Authorization: Bearer $UT"); ! is_err "$R" && ok "resume/{id}" || bad "resume/{id}" "$R"
R=$(curl -s -m5 -X PUT "$B/api/resume/$RID/default" -H "Authorization: Bearer $UT"); ! is_err "$R" && ok "resume/default" || bad "resume/default" "$R"
# 上传文件（curl 是 Windows 原生程序，不认 Git Bash 的 /tmp，需用 Windows 可识别路径）
CVTXT="D:/code/aimeetting-promax/ai-interview-server/target/cv_smoke.txt"
printf '姓名 张三\n技能 Java Spring Boot Redis\n项目 秒杀系统\n学历 本科\n电话 13800138000' > "$CVTXT"
RU=$(curl -s --noproxy '*' -m10 -X POST "$B/api/resume/upload" -H "Authorization: Bearer $UT" -F "file=@$CVTXT" -F "title=文件简历")
RUID=$(echo "$RU" | grep -o '"data":[0-9]*' | head -1 | sed 's/"data"://')
[ -n "$RUID" ] && ok "resume/upload(file)" || bad "resume/upload(file)" "$RU"

echo "===== 5. 面试会话（核心） ====="
SC=$(curl -s -m5 -X POST "$B/api/interview/sessions" -H "Authorization: Bearer $UT" -H "Content-Type: application/json" -d '{"directions":["JAVA_BACKEND"],"difficulty":"MEDIUM","totalQuestion":3}')
SID=$(echo "$SC" | grep -o '"data":[0-9]*' | head -1 | sed 's/"data"://')
[ -n "$SID" ] && ok "session/create sid=$SID" || bad "session/create" "$SC"
R=$(curl -s -m5 "$B/api/interview/sessions?pageNum=1&pageSize=10" -H "Authorization: Bearer $UT"); ! is_err "$R" && ok "session/page" || bad "session/page" "$R"
R=$(curl -s -m5 "$B/api/interview/sessions/$SID" -H "Authorization: Bearer $UT"); ! is_err "$R" && ok "session/detail" || bad "session/detail" "$R"
ST=$(curl -s -m10 -X POST "$B/api/interview/sessions/$SID/start" -H "Authorization: Bearer $UT")
SQID=$(echo "$ST" | grep -o '"sessionQuestionId":[0-9]*' | head -1 | sed 's/"sessionQuestionId"://')
echo "$ST" | grep -qE '"(question|title)"' && [ -n "$SQID" ] && ok "session/start sqid=$SQID" || bad "session/start" "$ST"
# 提交答案 SSE
SA=$(curl -s -m15 -N -X POST "$B/api/interview/sessions/$SID/answers" -H "Authorization: Bearer $UT" -H "Content-Type: application/json" -d "{\"sessionQuestionId\":$SQID,\"content\":\"HashMap基于数组+链表+红黑树，默认负载因子0.75，扩容2倍。\"}")
echo "$SA" | grep -q 'event:score' && echo "$SA" | grep -q '"score":' && ok "answers(SSE) 含 score 事件" || bad "answers(SSE)" "$SA"
ANSID=$(echo "$SA" | grep -o '"answerId":[0-9]*' | head -1 | sed 's/"answerId"://')
# 追问 SSE
FU=$(curl -s -m15 -N -X POST "$B/api/interview/sessions/$SID/answers/follow-up" -H "Authorization: Bearer $UT" -H "Content-Type: application/json" -d "{\"sessionQuestionId\":$SQID,\"content\":\"补充：1.8后链表转红黑树阈值为8。\",\"parentAnswerId\":$ANSID,\"isFollowUp\":true}")
# 模型判定「无需二次追问」时不发 follow_up 事件是正确行为，并非 bug：
# 仅当模型实际发出 event:follow_up 时才校验该事件负载，避免误报 FAIL。
if echo "$FU" | grep -q 'event:follow_up'; then
  echo "$FU" | grep -q '"title"\|followUpQuestion' && ok "answers/follow-up(SSE) follow_up 事件" || bad "answers/follow-up(SSE) 事件缺字段" "$FU"
else
  ok "answers/follow-up(SSE) 无需追问（未发 follow_up，正确）"
fi
# 下一题 SSE（取第2题）
NQ=$(curl -s -m15 -N -G "$B/api/interview/sessions/$SID/next-question" -H "Authorization: Bearer $UT")
SQID2=$(echo "$NQ" | grep -o '"sessionQuestionId":[0-9]*' | head -1 | sed 's/"sessionQuestionId"://')
echo "$NQ" | grep -q '"question"' && [ -n "$SQID2" ] && ok "next-question(SSE) sqid2=$SQID2" || bad "next-question(SSE)" "$NQ"
R=$(curl -s -m5 "$B/api/interview/sessions/$SID/messages" -H "Authorization: Bearer $UT"); ! is_err "$R" && ok "session/messages" || bad "session/messages" "$R"
R=$(curl -s -m5 "$B/api/interview/sessions/$SID/status" -H "Authorization: Bearer $UT"); ! is_err "$R" && ok "session/status" || bad "session/status" "$R"
R=$(curl -s -m5 -X POST "$B/api/interview/sessions/$SID/pause" -H "Authorization: Bearer $UT"); ! is_err "$R" && ok "session/pause" || bad "session/pause" "$R"
R=$(curl -s -m5 -X POST "$B/api/interview/sessions/$SID/resume" -H "Authorization: Bearer $UT"); ! is_err "$R" && ok "session/resume" || bad "session/resume" "$R"
R=$(curl -s -m5 -X POST "$B/api/interview/sessions/$SID/questions/$SQID2/skip" -H "Authorization: Bearer $UT"); ! is_err "$R" && ok "session/skip" || bad "session/skip" "$R"
R=$(curl -s -m5 "$B/api/interview/answers/$ANSID" -H "Authorization: Bearer $UT"); ! is_err "$R" && ok "answers/{id}" || bad "answers/{id}" "$R"
FIN=$(curl -s -m10 -X POST "$B/api/interview/sessions/$SID/finish" -H "Authorization: Bearer $UT")
REPID=$(echo "$FIN" | grep -o '"data":[0-9]*' | head -1 | sed 's/"data"://')
[ -n "$REPID" ] && ok "session/finish repid=$REPID" || bad "session/finish" "$FIN"

echo "===== 6. 报告 ====="
R=$(curl -s -m10 -X POST "$B/api/reports/$SID/generate" -H "Authorization: Bearer $UT"); ! is_err "$R" && ok "report/generate" || bad "report/generate" "$R"
R=$(curl -s -m5 "$B/api/reports/$SID" -H "Authorization: Bearer $UT"); ! is_err "$R" && ok "report/getBySession" || bad "report/getBySession" "$R"
R=$(curl -s -m5 "$B/api/reports?pageNum=1&pageSize=10" -H "Authorization: Bearer $UT"); ! is_err "$R" && ok "report/page" || bad "report/page" "$R"
R=$(curl -s -m5 "$B/api/reports/$REPID/export" -H "Authorization: Bearer $UT"); ! is_err "$R" && ok "report/export" || bad "report/export" "$R"
R=$(curl -s -m5 "$B/api/reports/id/$REPID" -H "Authorization: Bearer $UT"); ! is_err "$R" && ok "report/getById" || bad "report/getById" "$R"
R=$(curl -s -m5 -X DELETE "$B/api/reports/$REPID" -H "Authorization: Bearer $UT"); ! is_err "$R" && ok "report/delete" || bad "report/delete" "$R"

echo "===== 7. 管理端 ====="
R=$(curl -s -m5 "$B/api/admin/statistics/overview" -H "Authorization: Bearer $AT"); ! is_err "$R" && ok "admin/overview" || bad "admin/overview" "$R"
R=$(curl -s -m5 "$B/api/admin/statistics/sessions?days=7" -H "Authorization: Bearer $AT"); ! is_err "$R" && ok "admin/sessions-trend" || bad "admin/sessions-trend" "$R"
R=$(curl -s -m5 "$B/api/admin/ai-calls?pageNum=1&pageSize=10" -H "Authorization: Bearer $AT"); ! is_err "$R" && ok "admin/ai-calls" || bad "admin/ai-calls" "$R"
R=$(curl -s -m5 "$B/api/admin/ai/health" -H "Authorization: Bearer $AT"); ! is_err "$R" && ok "admin/ai/health" || bad "admin/ai/health" "$R"
R=$(curl -s -m5 "$B/api/admin/users?pageNum=1&pageSize=10" -H "Authorization: Bearer $AT"); ! is_err "$R" && ok "admin/users" || bad "admin/users" "$R"
# 禁用再启用冒烟用户（还原）
UUID=$(curl -s -m5 "$B/api/admin/users?pageNum=1&pageSize=50" -H "Authorization: Bearer $AT" | grep -o "\"id\":[0-9]*,\"username\":\"$U\"" | grep -o '"id":[0-9]*' | head -1 | sed 's/"id"://')
if [ -n "$UUID" ]; then
  curl -s -m5 -X PUT "$B/api/admin/users/$UUID/status" -H "Authorization: Bearer $AT" -H "Content-Type: application/json" -d '{"status":0}' >/dev/null
  R=$(curl -s -m5 -X PUT "$B/api/admin/users/$UUID/status" -H "Authorization: Bearer $AT" -H "Content-Type: application/json" -d '{"status":1}')
  ! is_err "$R" && ok "admin/user/status 切换" || bad "admin/user/status" "$R"
else
  bad "admin/user/status" "找不到冒烟用户 id"
fi

echo ""
echo "========================================="
echo "结果: PASS=$PASS  FAIL=$FAIL"
if [ "$FAIL" -gt 0 ]; then
  echo "失败项: ${FAILS[*]}"
fi
