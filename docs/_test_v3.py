# -*- coding: utf-8 -*-
"""v3 新功能端到端测试：通知中心（含触发点） + 时光胶囊（含到期可见性与权限）.

分两阶段执行，因为「胶囊到期」必须把 open_time 改到过去才能验证，
而这条路径要绕过 @Future 校验直接改库：

    python docs/_test_v3.py           # 阶段一：REST 全流程
    # （脚本会把 new capsule id 写到 .v3_state.json）
    python docs/_test_v3.py --phase2  # 阶段二：读状态文件，验证到期可见 + 定时任务

环境变量：
    TEST_BASE    接口根地址，默认本机后端 http://127.0.0.1:8080/api
    TEST_ORIGIN  模拟的浏览器来源（默认与 TEST_BASE 同源）
    MYSQL_BIN    mysql 客户端路径，用于阶段二的改库
    MYSQL_USER / MYSQL_PASS / MYSQL_DB

为什么必须带 Origin：浏览器在「非 GET 请求」上一定自动带 Origin，
不带 Origin 的回归会漏掉整整一类 CORS 缺陷（曾经 45 项全过仍漏）。
"""
import json
import os
import subprocess
import sys
import time
import urllib.error
import urllib.request
from urllib.parse import urlparse

OK = 20000
BASE = os.environ.get("TEST_BASE", "http://127.0.0.1:8080/api")
_o = urlparse(BASE)
ORIGIN = os.environ.get("TEST_ORIGIN") or ("%s://%s" % (_o.scheme, _o.netloc))
STATE_FILE = os.path.join(os.path.dirname(os.path.abspath(__file__)), ".v3_state.json")

MYSQL_BIN = os.environ.get("MYSQL_BIN", "mysql")
MYSQL_USER = os.environ.get("MYSQL_USER", "root")
MYSQL_PASS = os.environ.get("MYSQL_PASS", "1234")
MYSQL_DB = os.environ.get("MYSQL_DB", "schoolmate_book")

PASS, FAIL, SKIP = [], [], []


def call(method, path, token=None, body=None, timeout=15):
    url = BASE + path
    data = json.dumps(body).encode("utf-8") if body is not None else None
    req = urllib.request.Request(url, data=data, method=method)
    req.add_header("Content-Type", "application/json")
    req.add_header("Origin", ORIGIN)
    if token:
        req.add_header("Authorization", "Bearer " + token)
    try:
        with urllib.request.urlopen(req, timeout=timeout) as r:
            return json.loads(r.read().decode("utf-8"))
    except urllib.error.HTTPError as e:
        try:
            return json.loads(e.read().decode("utf-8"))
        except Exception:
            return {"code": e.code, "message": "HTTP " + str(e.code)}
    except Exception as e:
        return {"code": -1, "message": str(e)}


def db_exec(sql):
    """直接改库（仅用于构造测试前置数据）。mysql 不可用时返回 False。"""
    try:
        p = subprocess.run(
            [MYSQL_BIN, "-u" + MYSQL_USER, "-p" + MYSQL_PASS, "-N", "-B", "-e", sql],
            capture_output=True, text=True, timeout=20,
        )
        if p.returncode != 0:
            print("    [db] " + (p.stderr or "").strip()[:160])
            return False
        return True
    except Exception as e:
        print("    [db] 不可用：" + str(e)[:120])
        return False


def check(name, cond, extra=""):
    (PASS if cond else FAIL).append(name)
    print(("  [PASS] " if cond else "  [FAIL] ") + name + (("  -> " + str(extra)) if extra else ""))


def skip(name, why):
    SKIP.append(name)
    print("  [SKIP] " + name + "  -> " + why)


def login(username, password="123456"):
    r = call("POST", "/auth/login", body={"username": username, "password": password})
    if r.get("code") != OK:
        print("  登录失败 %s: %s" % (username, r.get("message")))
        return None
    return r["data"]["token"]


def unread(tk):
    r = call("GET", "/notifications/unread-count", tk)
    return r.get("data") if r.get("code") == OK else None


def notif_list(tk, **params):
    q = "&".join("%s=%s" % (k, v) for k, v in params.items())
    r = call("GET", "/notifications" + (("?" + q) if q else ""), tk)
    return r.get("data", {}) if r.get("code") == OK else {}


def latest_notif(tk, ntype):
    """取指定类型的最新一条通知"""
    d = notif_list(tk, pageNum=1, pageSize=20)
    for n in d.get("records", []):
        if n.get("type") == ntype:
            return n
    return None


def ids():
    """取种子用户 id"""
    r = db_exec("USE %s; SELECT username, id FROM user WHERE deleted=0 "
                "AND username IN ('admin','zhangsan','lisi','wangwu') ORDER BY id;" % MYSQL_DB)
    return r


# =====================================================================
# 阶段一
# =====================================================================
def phase1():
    print("\n================ 阶段一：通知中心 + 时光胶囊（REST） ================\n")

    admin, zs, ls, ww = (login("admin"), login("zhangsan"), login("lisi"), login("wangwu"))
    if not all([admin, zs, ls, ww]):
        print("  四个种子账号登录失败，无法继续")
        return 1

    # 取用户 id（用于权限断言与库清理）
    r = call("GET", "/auth/me", ww)
    ww_id = r.get("data", {}).get("id")
    r = call("GET", "/auth/me", zs)
    zs_id = r.get("data", {}).get("id")
    r = call("GET", "/auth/me", ls)
    ls_id = r.get("data", {}).get("id")
    r = call("GET", "/auth/me", admin)
    admin_id = r.get("data", {}).get("id")
    print("  账号 id: admin=%s zhangsan=%s lisi=%s wangwu=%s" % (admin_id, zs_id, ls_id, ww_id))

    # ---------- 前置：清掉这对用户的既有好友关系，保证申请流程可复现 ----------
    print("\n-- 前置清理：zhangsan <-> wangwu 的好友关系 --")
    db_exec("USE %s; DELETE FROM friendship WHERE (user_a_id=%s AND user_b_id=%s) "
            "OR (user_a_id=%s AND user_b_id=%s);"
            % (MYSQL_DB, zs_id, ww_id, ww_id, zs_id))
    db_exec("USE %s; DELETE FROM friend_request WHERE (from_user_id=%s AND to_user_id=%s) "
            "OR (from_user_id=%s AND to_user_id=%s);"
            % (MYSQL_DB, zs_id, ww_id, ww_id, zs_id))

    # 让 lisi / wangwu 加入 1 班（胶囊与动态都需要班级成员身份）
    for tk, who in ((ls, "lisi"), (ww, "wangwu")):
        r = call("POST", "/classes/join", tk, {"inviteCode": "CLASS01"})
        already = r.get("code") != OK and "已" in str(r.get("message"))
        print("  %s 加入班级：%s%s" % (who, r.get("code"), "" if r.get("code") == OK else " / " + str(r.get("message"))))
        if r.get("code") != OK and not already:
            print("  !! 入班失败，后续班级相关断言可能不成立")

    # =================================================================
    # 一、通知中心 —— 好友申请 / 通过
    # =================================================================
    print("\n-- 一、通知：好友申请与通过 --")
    before = unread(ww)
    r = call("POST", "/friends/requests", zs, {"toUserId": ww_id, "message": "我是张三，加个好友"})
    check("zhangsan 发起好友申请 → code 20000", r.get("code") == OK, r.get("message"))
    check("wangwu 未读数 +1", unread(ww) == (before or 0) + 1, "before=%s after=%s" % (before, unread(ww)))

    n = latest_notif(ww, "FRIEND_REQUEST")
    check("wangwu 收到 FRIEND_REQUEST 通知", n is not None)
    if n:
        check("  标题含申请人昵称", "张三" in (n.get("title") or ""), n.get("title"))
        check("  bizType=FRIEND", n.get("bizType") == "FRIEND", n.get("bizType"))
        check("  fromUserId 为 zhangsan", n.get("fromUserId") == zs_id, n.get("fromUserId"))
        check("  初始 read=false", n.get("read") is False, n.get("read"))

        # 标记已读
        r = call("PUT", "/notifications/%s/read" % n["id"], ww)
        check("标记单条已读 → code 20000", r.get("code") == OK, r.get("message"))
        check("  未读数回落", unread(ww) == before, "expect=%s got=%s" % (before, unread(ww)))
        again = latest_notif(ww, "FRIEND_REQUEST")
        check("  该条 read=true", again and again.get("read") is True)
    else:
        skip("单条已读相关断言", "未取到 FRIEND_REQUEST 通知")

    # 接受申请 → zhangsan 收到 FRIEND_ACCEPTED
    reqs = call("GET", "/friends/requests?status=PENDING", ww).get("data") or []
    target = next((x for x in reqs if x.get("fromUserId") == zs_id), None)
    if target:
        zs_before = unread(zs)
        r = call("PUT", "/friends/requests/%s/accept" % target["id"], ww)
        check("wangwu 同意申请 → code 20000", r.get("code") == OK, r.get("message"))
        check("  zhangsan 未读数 +1", unread(zs) == (zs_before or 0) + 1,
              "before=%s after=%s" % (zs_before, unread(zs)))
        n2 = latest_notif(zs, "FRIEND_ACCEPTED")
        check("  zhangsan 收到 FRIEND_ACCEPTED 通知", n2 is not None)
        if n2:
            check("  bizType=FRIEND", n2.get("bizType") == "FRIEND", n2.get("bizType"))
    else:
        skip("好友通过通知", "未找到待处理申请（可能已是好友）")

    # =================================================================
    # 二、通知中心 —— 点赞 / 评论（含「不通知自己」）
    # =================================================================
    print("\n-- 二、通知：点赞与评论 --")
    r = call("POST", "/classes/1/moments", admin, {"content": "v3 通知中心联调动态"})
    check("admin 发布班级动态 → code 20000", r.get("code") == OK, r.get("message"))
    mid = (r.get("data") or {}).get("id")
    if not mid:
        print("  !! 动态发布失败，跳过点赞/评论断言")
        mid = None

    if mid:
        admin_before = unread(admin)
        r = call("POST", "/moments/%s/like" % mid, ww)
        check("wangwu 点赞 → code 20000", r.get("code") == OK, r.get("message"))
        check("  admin 未读数 +1", unread(admin) == (admin_before or 0) + 1,
              "before=%s after=%s" % (admin_before, unread(admin)))
        n3 = latest_notif(admin, "MOMENT_LIKE")
        check("  admin 收到 MOMENT_LIKE 通知", n3 is not None)
        if n3:
            check("  bizType=MOMENT", n3.get("bizType") == "MOMENT", n3.get("bizType"))
            check("  bizId=classId（前端才能跳到班级页）", n3.get("bizId") == 1, n3.get("bizId"))

        # 取消点赞不通知
        c1 = unread(admin)
        r = call("POST", "/moments/%s/like" % mid, ww)
        check("取消点赞 → 不产生新通知", unread(admin) == c1, "before=%s after=%s" % (c1, unread(admin)))

        # 自己赞自己不通知
        c2 = unread(admin)
        call("POST", "/moments/%s/like" % mid, admin)
        check("自己赞自己 → 不产生通知", unread(admin) == c2, "before=%s after=%s" % (c2, unread(admin)))
        call("POST", "/moments/%s/like" % mid, admin)  # 撤销，恢复现场

        # 评论
        c3 = unread(admin)
        r = call("POST", "/moments/%s/comments" % mid, ww, {"content": "这条动态写得不错"})
        check("wangwu 评论 → code 20000", r.get("code") == OK, r.get("message"))
        check("  admin 未读数 +1", unread(admin) == (c3 or 0) + 1, "before=%s after=%s" % (c3, unread(admin)))
        n4 = latest_notif(admin, "MOMENT_COMMENT")
        check("  admin 收到 MOMENT_COMMENT 通知", n4 is not None)
        if n4:
            check("  摘要含评论内容", "不错" in (n4.get("content") or ""), n4.get("content"))

        # 自己评自己不通知
        c4 = unread(admin)
        call("POST", "/moments/%s/comments" % mid, admin, {"content": "自己回自己一句"})
        check("自己评论自己 → 不产生通知", unread(admin) == c4, "before=%s after=%s" % (c4, unread(admin)))

    # 只看未读
    d = notif_list(admin, pageNum=1, pageSize=50, unreadOnly="true")
    all_unread = (d.get("records") or []) and all(n.get("read") is False for n in d["records"])
    check("unreadOnly=true 只返回未读", bool(all_unread), "条数=%s" % len(d.get("records") or []))

    # 全部已读
    r = call("PUT", "/notifications/read-all", admin)
    check("全部标记已读 → code 20000", r.get("code") == OK, r.get("message"))
    check("  admin 未读数归零", unread(admin) == 0, unread(admin))

    # 越权：不能读别人的通知
    if mid:
        r = call("GET", "/notifications", ww)
        others = r.get("data", {}).get("records", []) or []
        check("通知列表只含本人数据", all(x.get("userId") in (None, ww_id) for x in others),
              "共 %s 条" % len(others))
    r = call("PUT", "/notifications/999999/read", ww)
    check("标记不存在的通知 → 不报 500", r.get("code") in (40400, 40300, 40001), r.get("code"))

    # =================================================================
    # 三、时光胶囊 —— 写入与封存
    # =================================================================
    print("\n-- 三、时光胶囊：写信与封存 --")
    future = "2030-06-30 12:00:00"
    past = "2020-01-01 00:00:00"

    r = call("POST", "/capsules", zs,
             {"title": "写给毕业那天的自己", "content": "希望你还在写代码。",
              "openTime": future, "openType": "SELF"})
    check("写 SELF 胶囊 → code 20000", r.get("code") == OK, r.get("message"))
    self_id = r.get("data")
    check("  返回胶囊 id", isinstance(self_id, int), self_id)

    r = call("POST", "/capsules", ls,
             {"title": "写给全班的一封信", "content": "十年后还要一起吃饭。",
              "openTime": future, "openType": "PUBLIC", "classId": 1})
    check("写 PUBLIC 胶囊 → code 20000", r.get("code") == OK, r.get("message"))
    pub_id = r.get("data")
    check("  返回胶囊 id", isinstance(pub_id, int), pub_id)

    r = call("POST", "/capsules", zs,
             {"title": "过去的时间", "content": "x", "openTime": past, "openType": "SELF"})
    check("开启时间在过去 → 被拒", r.get("code") == 40001, "%s / %s" % (r.get("code"), r.get("message")))

    r = call("POST", "/capsules", zs,
             {"title": "缺班级", "content": "x", "openTime": future, "openType": "PUBLIC"})
    check("PUBLIC 不传 classId → 被拒", r.get("code") == 40001, r.get("message"))

    r = call("POST", "/capsules", ww,
             {"title": "非成员投信", "content": "x", "openTime": future,
              "openType": "PUBLIC", "classId": 1})
    # wangwu 已在前面入班，这里仅确认「不加班的班」会被拦
    r2 = call("POST", "/capsules", ww,
              {"title": "非成员投信2", "content": "x", "openTime": future,
               "openType": "PUBLIC", "classId": 999999})
    check("向未加入的班级投信 → 被拒", r2.get("code") in (40300, 40400), r2.get("code"))

    # =================================================================
    # 四、时光胶囊 —— 未到期不可见与权限
    # =================================================================
    print("\n-- 四、时光胶囊：未到期不可见与权限 --")
    mine = call("GET", "/capsules/my", zs).get("data") or []
    mine_self = next((c for c in mine if c.get("id") == self_id), None)
    check("我的列表含刚写的胶囊", mine_self is not None)
    if mine_self:
        check("  未到期 content 为 null", mine_self.get("content") is None, mine_self.get("content"))
        check("  openable=false", mine_self.get("openable") is False)
        check("  countdownSeconds>0", (mine_self.get("countdownSeconds") or 0) > 0,
              mine_self.get("countdownSeconds"))

    r = call("GET", "/capsules/%s" % self_id, zs)
    check("写信人本人看详情 → 仍未返回内容", r.get("code") == OK and r["data"].get("content") is None,
          r.get("data", {}).get("content"))

    r = call("GET", "/capsules/%s" % self_id, ww)
    check("他人看 SELF 胶囊 → 404", r.get("code") == 40400, r.get("code"))

    r = call("GET", "/capsules/%s" % pub_id, ls)
    check("班级成员看 PUBLIC 详情 → 20000 且无内容",
          r.get("code") == OK and r["data"].get("content") is None, r.get("code"))

    r = call("GET", "/capsules/class/1", ww)
    check("班级墙（成员）可访问", r.get("code") == OK, "%s / %s" % (r.get("code"), r.get("message")))
    wall = r.get("data") or []
    wall_pub = next((c for c in wall if c.get("id") == pub_id), None)
    check("  墙上能看到 PUBLIC 胶囊", wall_pub is not None)
    if wall_pub:
        check("  未到期不返回内容", wall_pub.get("content") is None)
        check("  带作者昵称", bool(wall_pub.get("nickname")), wall_pub.get("nickname"))

    r = call("DELETE", "/capsules/%s" % pub_id, zs)
    check("删他人的胶囊 → 403", r.get("code") == 40300, "%s / %s" % (r.get("code"), r.get("message")))

    # 写一封稍后删除的 SELF 胶囊，验证删除权限
    r = call("POST", "/capsules", zs,
             {"title": "待撤回的信", "content": "这封会被删掉", "openTime": future, "openType": "SELF"})
    tmp_id = r.get("data")
    r = call("DELETE", "/capsules/%s" % tmp_id, zs)
    check("本人撤回封存中的胶囊 → 20000", r.get("code") == OK, r.get("message"))
    check("  撤回后详情 404", call("GET", "/capsules/%s" % tmp_id, zs).get("code") == 40400)

    # 阶段二需要的状态
    with open(STATE_FILE, "w", encoding="utf-8") as f:
        json.dump({"self_id": self_id, "pub_id": pub_id, "admin_id": admin_id,
                   "zs_id": zs_id, "class_id": 1}, f, ensure_ascii=False)
    print("\n  已写出阶段二状态文件：%s" % STATE_FILE)
    return 0


# =====================================================================
# 阶段二
# =====================================================================
def phase2():
    print("\n================ 阶段二：到期可见 + 定时任务 ================\n")
    if not os.path.exists(STATE_FILE):
        print("  缺少状态文件，请先跑阶段一")
        return 1
    with open(STATE_FILE, encoding="utf-8") as f:
        st = json.load(f)
    self_id, pub_id = st["self_id"], st["pub_id"]

    zs, ls = login("zhangsan"), login("lisi")
    if not zs or not ls:
        return 1

    print("\n-- 一、把两封胶囊的开启时间改到 1 分钟前（绕过 @Future）--")
    ok = db_exec("USE %s; UPDATE time_capsule SET open_time = DATE_SUB(NOW(), INTERVAL 1 MINUTE) "
                 "WHERE id IN (%s, %s);" % (MYSQL_DB, self_id, pub_id))
    if not ok:
        skip("到期可见性断言", "mysql 不可用，无法构造「已到期」数据")
        return 0

    print("\n-- 二、到期后内容可见 --")
    r = call("GET", "/capsules/%s" % self_id, zs)
    d = r.get("data") or {}
    check("本人看已到期 SELF → openable=true", d.get("openable") is True, d.get("openable"))
    check("  返回 content", d.get("content") == "希望你还在写代码。", d.get("content"))
    check("  倒计时归零", d.get("countdownSeconds") == 0, d.get("countdownSeconds"))

    r = call("GET", "/capsules/class/1", ls)
    wall_pub = next((c for c in (r.get("data") or []) if c.get("id") == pub_id), None)
    check("班级墙上的 PUBLIC 已到期并可见内容",
          wall_pub is not None and wall_pub.get("openable") is True
          and "一起吃饭" in (wall_pub.get("content") or ""),
          (wall_pub or {}).get("openable"))

    # 等定时任务（fixedDelay 60s）把它翻成 OPENED 并发通知
    print("\n-- 三、定时任务：置 OPENED 并通知写信人（最多等 80 秒）--")
    before = unread(zs)
    opened_notif = None
    deadline = time.time() + 80
    while time.time() < deadline:
        n = latest_notif(zs, "CAPSULE_OPENED")
        if n and n.get("bizId") in (self_id, pub_id):
            opened_notif = n
            break
        time.sleep(5)

    if opened_notif:
        check("写信人收到 CAPSULE_OPENED 通知", True)
        check("  bizType=CAPSULE", opened_notif.get("bizType") == "CAPSULE",
              opened_notif.get("bizType"))
        check("  标题含信件标题", "时光胶囊" in (opened_notif.get("title") or ""),
              opened_notif.get("title"))
        check("  未读数增加", unread(zs) > (before or 0),
              "before=%s after=%s" % (before, unread(zs)))
    else:
        check("写信人收到 CAPSULE_OPENED 通知", False, "80 秒内未等到定时任务产出通知")

    r = call("GET", "/capsules/my", zs)
    mine_self = next((c for c in (r.get("data") or []) if c.get("id") == self_id), None)
    check("已到期胶囊 status=OPENED", mine_self and mine_self.get("status") == "OPENED",
          (mine_self or {}).get("status"))

    r = call("DELETE", "/capsules/%s" % self_id, zs)
    check("已开启的胶囊不允许删除 → 40900", r.get("code") == 40900, r.get("code"))
    return 0


def main():
    phase1() if "--phase2" not in sys.argv else phase2()
    print("\n================ 结果 ================")
    print("PASS %d / FAIL %d / SKIP %d" % (len(PASS), len(FAIL), len(SKIP)))
    if FAIL:
        print("失败项：")
        for f in FAIL:
            print("  - " + f)
    return 1 if FAIL else 0


if __name__ == "__main__":
    sys.exit(main())
