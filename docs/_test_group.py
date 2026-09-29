# -*- coding: utf-8 -*-
"""群聊模块端到端测试：班级群自动创建 / 成员同步 / 自建群 / 群消息扇出 / 退群解散."""
import json
import os
import urllib.request
import urllib.error
from urllib.parse import urlparse

OK = 20000
# 可用环境变量覆盖，便于指向不同环境：
#   TEST_BASE   —— 接口根地址，默认本机后端
#   TEST_ORIGIN —— 模拟的浏览器来源
# 注意：natapp 免费隧道对「突发新连接数」有限流（约 3 秒内 25 次连接即返回
# 429 Too much connections in one mintue），密集请求的回归脚本直接跑穿透地址会
# 大面积误报。建议回归时用 TEST_BASE 指向 127.0.0.1:5173（前端代理），
# 再单独用 TEST_ORIGIN 模拟穿透域名，即可在不触发限流的前提下验证完整链路。
BASE = os.environ.get("TEST_BASE", "http://127.0.0.1:8080/api")
PASS, FAIL = [], []

# 浏览器在「非 GET 请求」上一定会自动带上 Origin 头（curl / urllib 默认不带）。
# 后端 Spring 会拿 Origin 和 Host 比对来判定是否跨域，不匹配又不在白名单时直接
# 返回 403 Invalid CORS request。曾经就是因为测试脚本不带 Origin，45 项全过却
# 掩盖了「浏览器里一点登录就报网络异常」的真实缺陷。这里模拟浏览器补上。
_o = urlparse(BASE)
ORIGIN = os.environ.get("TEST_ORIGIN") or ("%s://%s" % (_o.scheme, _o.netloc))


def call(method, path, token=None, body=None):
    url = BASE + path
    data = json.dumps(body).encode("utf-8") if body is not None else None
    req = urllib.request.Request(url, data=data, method=method)
    req.add_header("Content-Type", "application/json")
    req.add_header("Origin", ORIGIN)
    if token:
        req.add_header("Authorization", "Bearer " + token)
    try:
        with urllib.request.urlopen(req, timeout=15) as r:
            return json.loads(r.read().decode("utf-8"))
    except urllib.error.HTTPError as e:
        try:
            return json.loads(e.read().decode("utf-8"))
        except Exception:
            return {"code": e.code, "message": "HTTP " + str(e.code)}


def check(name, cond, extra=""):
    (PASS if cond else FAIL).append(name)
    print(("  [PASS] " if cond else "  [FAIL] ") + name + (("  -> " + str(extra)) if extra else ""))


def login(username, password="123456"):
    r = call("POST", "/auth/login", body={"username": username, "password": password})
    return r.get("data", {}).get("token") if r.get("code") == OK else None


def register(username, nickname):
    return call("POST", "/auth/register", body={"username": username, "password": "123456", "nickname": nickname})


def my_sessions(tk):
    return call("GET", "/chat/sessions", token=tk).get("data", []) or []


def find_session(tk, sid):
    return [s for s in my_sessions(tk) if str(s.get("id")) == str(sid)]


print("=" * 60)
print("STEP 1  准备用户")
print("=" * 60)
for u, n in [("lisi", "李四"), ("wangwu", "王五")]:
    r = register(u, n)
    print("  register %s -> %s %s" % (u, r.get("code"), r.get("message")))

tk = {}
for name, u in [("admin", "admin"), ("zhangsan", "zhangsan"), ("lisi", "lisi"), ("wangwu", "wangwu")]:
    tk[name] = login(u)
check("四个用户登录成功", all(tk.values()), {k: bool(v) for k, v in tk.items()})

ids = {}
for name in tk:
    me = call("GET", "/auth/me", token=tk[name])
    ids[name] = me.get("data", {}).get("id")
print("  userIds =", ids)

print()
print("=" * 60)
print("STEP 2  创建班级 -> 应自动创建 CLASS 群 + 会话")
print("=" * 60)
r = call("POST", "/classes", token=tk["admin"], body={
    "className": "2024级智能科学与技术1班", "grade": "2024级", "major": "智能科学与技术",
    "description": "群聊自动化测试班级"})
check("创建班级成功", r.get("code") == OK, r.get("message"))
cls = r.get("data", {})
class_id = cls.get("id")
invite = cls.get("inviteCode")
print("  classId=%s inviteCode=%s" % (class_id, invite))

groups = call("GET", "/groups", token=tk["admin"]).get("data", []) or []
class_groups = [g for g in groups if g.get("groupType") == "CLASS" and str(g.get("classId")) == str(class_id)]
check("建班后自动出现 CLASS 群", len(class_groups) == 1, groups)
check("班级群会话ID已绑定", bool(class_groups and class_groups[0].get("sessionId")), class_groups)
class_group_id = class_groups[0].get("id") if class_groups else None
class_session_id = class_groups[0].get("sessionId") if class_groups else None
print("  classGroupId=%s classSessionId=%s" % (class_group_id, class_session_id))

print()
print("=" * 60)
print("STEP 3  成员加入班级 -> 应同步进班级群")
print("=" * 60)
for name in ["zhangsan", "lisi", "wangwu"]:
    r = call("POST", "/classes/join", token=tk[name], body={"inviteCode": invite})
    check("%s 加入班级" % name, r.get("code") == OK, r.get("message"))

r = call("GET", "/groups/%s/members?pageNum=1&pageSize=50" % class_group_id, token=tk["admin"])
mems = (r.get("data") or {}).get("records", []) or []
names = [m.get("nickname") for m in mems]
check("班级群成员数=4（建班者+3）", len(mems) == 4, names)
check("群主排第一位", bool(mems) and mems[0].get("memberRole") == "OWNER", mems[0] if mems else None)
print("  成员顺序 =", names)

for name in ["zhangsan", "lisi"]:
    hit = find_session(tk[name], class_session_id)
    check("%s 会话列表含班级群" % name, len(hit) == 1, my_sessions(tk[name]))
    if hit:
        check("%s 班级群会话类型=GROUP" % name, hit[0].get("sessionType") == "GROUP", hit[0].get("sessionType"))

print()
print("=" * 60)
print("STEP 4  移出班级成员 -> 应自动移出班级群")
print("=" * 60)
r = call("DELETE", "/classes/%s/members/%s" % (class_id, ids["wangwu"]), token=tk["admin"])
check("把王五移出班级", r.get("code") == OK, r.get("message"))
r = call("GET", "/groups/%s/members?pageNum=1&pageSize=50" % class_group_id, token=tk["admin"])
mems2 = (r.get("data") or {}).get("records", []) or []
check("班级群成员回落到 3 人", len(mems2) == 3, [m.get("nickname") for m in mems2])
check("王五会话列表已无该群", len(find_session(tk["wangwu"], class_session_id)) == 0, my_sessions(tk["wangwu"]))

print()
print("=" * 60)
print("STEP 5  自建群（张三拉 admin + 李四）")
print("=" * 60)
r = call("POST", "/groups", token=tk["zhangsan"], body={
    "groupName": "考研互助小组", "memberIds": [ids["admin"], ids["lisi"]]})
check("创建自建群成功", r.get("code") == OK, r.get("message"))
custom_group_id = r.get("data")
print("  customGroupId=%s" % custom_group_id)

g = call("GET", "/groups/%s" % custom_group_id, token=tk["zhangsan"]).get("data", {})
check("群类型=CUSTOM", g.get("groupType") == "CUSTOM", g.get("groupType"))
check("我在群内角色=OWNER", g.get("myRole") == "OWNER", g.get("myRole"))
custom_session_id = g.get("sessionId")
print("  customSessionId=%s memberCount=%s" % (custom_session_id, g.get("memberCount")))
check("自建群成员数=3", g.get("memberCount") == 3, g.get("memberCount"))

r = call("GET", "/groups/%s" % custom_group_id, token=tk["wangwu"])
check("非成员访问群资料被拒", r.get("code") != OK, r.get("message"))

print()
print("=" * 60)
print("STEP 6  群消息扇出 + 未读数")
print("=" * 60)
n0 = call("POST", "/chat/messages", token=tk["zhangsan"], body={
    "sessionId": custom_session_id, "msgType": "TEXT",
    "content": "大家好，这是群聊第一条消息", "clientMsgId": "g-001"})
check("群发消息成功", n0.get("code") == OK, n0.get("message"))
mid = (n0.get("data") or {}).get("id")
print("  msgId=%s" % mid)

n1 = call("POST", "/chat/messages", token=tk["zhangsan"], body={
    "sessionId": custom_session_id, "msgType": "TEXT",
    "content": "<b>群聊 XSS</b> 测试", "clientMsgId": "g-002"})
check("第二条群消息成功", n1.get("code") == OK, n1.get("message"))
body2 = (n1.get("data") or {}).get("content", "")
check("群消息内容已转义", "&lt;b&gt;" in body2, body2)

for name in ["admin", "lisi"]:
    hit = find_session(tk[name], custom_session_id)
    uc = hit[0].get("unreadCount") if hit else None
    check("%s 群未读=2" % name, uc == 2, uc)

hit = find_session(tk["zhangsan"], custom_session_id)
check("发送者自己群未读=0", bool(hit) and hit[0].get("unreadCount") == 0, hit[0] if hit else None)

print()
print("=" * 60)
print("STEP 7  群历史（正序）+ 发送者昵称")
print("=" * 60)
recs = call("GET", "/chat/sessions/%s/messages?size=20" % custom_session_id, token=tk["lisi"]).get("data", []) or []
check("李四能读到群历史 2 条", len(recs) == 2, len(recs))
check("历史为正序（id 递增）", len(recs) == 2 and recs[0]["id"] < recs[1]["id"], [x.get("id") for x in recs])
check("群消息带发送者昵称", bool(recs) and recs[0].get("senderNickname") == "张三", recs[0] if recs else None)

print()
print("=" * 60)
print("STEP 8  已读上报 / 拉人 / 退群 / 解散")
print("=" * 60)
last_id = recs[-1]["id"] if recs else None
r = call("PUT", "/chat/sessions/%s/read?lastReadMessageId=%s" % (custom_session_id, last_id), token=tk["lisi"])
check("李四已读上报成功", r.get("code") == OK, r.get("message"))
hit = find_session(tk["lisi"], custom_session_id)
check("已读后李四群未读=0", bool(hit) and hit[0].get("unreadCount") == 0, hit[0] if hit else None)

r = call("POST", "/groups/%s/members" % custom_group_id, token=tk["zhangsan"], body={"userIds": [ids["wangwu"]]})
check("张三拉王五入群", r.get("code") == OK, r.get("message"))
g = call("GET", "/groups/%s" % custom_group_id, token=tk["zhangsan"]).get("data", {})
check("自建群成员数=4", g.get("memberCount") == 4, g.get("memberCount"))
check("王五会话列表出现该群", len(find_session(tk["wangwu"], custom_session_id)) == 1, my_sessions(tk["wangwu"]))

r = call("DELETE", "/groups/%s" % custom_group_id, token=tk["lisi"])
check("非群主解散群被拒", r.get("code") != OK, r.get("message"))

r = call("POST", "/groups/%s/quit" % custom_group_id, token=tk["wangwu"])
check("王五退群成功", r.get("code") == OK, r.get("message"))
g = call("GET", "/groups/%s" % custom_group_id, token=tk["zhangsan"]).get("data", {})
check("退群后成员数回落=3", g.get("memberCount") == 3, g.get("memberCount"))
check("退群后王五会话消失", len(find_session(tk["wangwu"], custom_session_id)) == 0, my_sessions(tk["wangwu"]))

r = call("POST", "/groups/%s/quit" % class_group_id, token=tk["zhangsan"])
check("班级群不可直接退群", r.get("code") != OK, r.get("message"))

r = call("DELETE", "/groups/%s" % custom_group_id, token=tk["zhangsan"])
check("群主解散自建群成功", r.get("code") == OK, r.get("message"))
r = call("GET", "/groups/%s" % custom_group_id, token=tk["zhangsan"])
check("解散后群资料不可访问", r.get("code") != OK, r.get("message"))

print()
print("=" * 60)
print("STEP 9  解散班级 -> 班级群一并取消")
print("=" * 60)
r = call("DELETE", "/classes/%s" % class_id, token=tk["admin"])
check("解散班级成功", r.get("code") == OK, r.get("message"))
gs = [g for g in (call("GET", "/groups", token=tk["admin"]).get("data", []) or [])
      if str(g.get("id")) == str(class_group_id)]
check("班级群已从列表消失", len(gs) == 0, gs)
check("成员会话列表已无班级群", len(find_session(tk["zhangsan"], class_session_id)) == 0, my_sessions(tk["zhangsan"]))

print()
print("=" * 60)
print("结果：PASS=%d  FAIL=%d" % (len(PASS), len(FAIL)))
print("=" * 60)
if FAIL:
    print("失败项：")
    for f in FAIL:
        print("  - " + f)
