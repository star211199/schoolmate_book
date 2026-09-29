// WebSocket 实时链路测试：握手鉴权 / 群消息扇出 / 发送回执 / 心跳 / 非法token
const BASE = 'http://127.0.0.1:8080/api';
const WS_BASE = 'ws://127.0.0.1:8080/api/ws/chat';
const OK = 20000;
// 每轮运行使用唯一后缀，避免上一轮持久化的消息触发幂等分支而干扰断言
const RUN = Date.now().toString(36);
let PASS = 0, FAIL = 0;
const fails = [];
function check(name, cond, extra = '') {
  if (cond) { PASS++; console.log('  [PASS] ' + name); }
  else { FAIL++; fails.push(name); console.log('  [FAIL] ' + name + (extra ? '  -> ' + JSON.stringify(extra) : '')); }
}
async function api(method, path, token, body) {
  const headers = { 'Content-Type': 'application/json' };
  if (token) headers['Authorization'] = 'Bearer ' + token;
  const r = await fetch(BASE + path, { method, headers, body: body ? JSON.stringify(body) : undefined });
  return await r.json();
}
const sleep = (ms) => new Promise(r => setTimeout(r, ms));

function openWs(token, opts = {}) {
  const url = WS_BASE + '?token=' + encodeURIComponent(token);
  const ws = new WebSocket(url);
  const frames = [];
  ws.addEventListener('message', (e) => { try { frames.push(JSON.parse(e.data)); } catch (_) { frames.push(e.data); } });
  return new Promise((resolve, reject) => {
    ws.addEventListener('open', () => resolve({ ws, frames }));
    ws.addEventListener('error', () => reject(new Error('ws error')));
    ws.addEventListener('close', (e) => { if (!opts.expectOpen) reject(new Error('closed ' + e.code)); });
  });
}
const waitFor = async (frames, type, timeout = 3000) => {
  const t0 = Date.now();
  while (Date.now() - t0 < timeout) {
    const f = frames.find(x => x && x.type === type);
    if (f) return f;
    await sleep(50);
  }
  return null;
};

(async () => {
  console.log('='.repeat(60));
  console.log('STEP 1  准备用户与一个群');
  console.log('='.repeat(60));
  const tk = {};
  for (const u of ['admin', 'zhangsan', 'lisi']) {
    const r = await api('POST', '/auth/login', null, { username: u, password: '123456' });
    tk[u] = r.data && r.data.token;
  }
  check('三个用户登录成功', tk.admin && tk.zhangsan && tk.lisi);

  const ids = {};
  for (const u of Object.keys(tk)) {
    const me = await api('GET', '/auth/me', tk[u]);
    ids[u] = me.data.id;
  }
  console.log('  userIds =', ids);

  // 张三建群拉 admin + 李四
  let r = await api('POST', '/groups', tk.zhangsan, { groupName: 'WS实时测试群', memberIds: [ids.admin, ids.lisi] });
  check('建群成功', r.code === OK, r.message);
  const gid = r.data;
  const detail = await api('GET', '/groups/' + gid, tk.zhangsan);
  const sid = detail.data.sessionId;
  console.log('  groupId=%s sessionId=%s', gid, sid);

  console.log();
  console.log('='.repeat(60));
  console.log('STEP 2  合法 token 握手');
  console.log('='.repeat(60));
  const cAdmin = await openWs(tk.admin);
  const cZs = await openWs(tk.zhangsan);
  const cLs = await openWs(tk.lisi);
  check('admin WebSocket 连接成功', cAdmin.ws.readyState === 1);
  check('zhangsan WebSocket 连接成功', cZs.ws.readyState === 1);
  check('lisi WebSocket 连接成功', cLs.ws.readyState === 1);

  const connFrame = await waitFor(cAdmin.frames, 'CONNECTED', 2000);
  check('收到 CONNECTED 帧', !!connFrame, cAdmin.frames);

  console.log();
  console.log('='.repeat(60));
  console.log('STEP 3  非法 token 应被拒绝握手');
  console.log('='.repeat(60));
  let rejected = false;
  try { const bad = await openWs('this.is.not.a.jwt'); await sleep(500); rejected = bad.ws.readyState !== 1; }
  catch (_) { rejected = true; }
  check('非法 token 无法建立连接', rejected);

  console.log();
  console.log('='.repeat(60));
  console.log('STEP 4  群消息实时扇出 + 发送方回执');
  console.log('='.repeat(60));
  cAdmin.frames.length = 0; cLs.frames.length = 0; cZs.frames.length = 0;

  const payload = { type: 'CHAT_SEND', data: { sessionId: sid, msgType: 'TEXT', content: 'WS 实时群消息', clientMsgId: 'ws-001-' + RUN } };
  cZs.ws.send(JSON.stringify(payload));

  const ack = await waitFor(cZs.frames, 'MESSAGE_ACK', 4000);
  check('发送方收到 MESSAGE_ACK', !!ack, cZs.frames.map(f => f.type));
  check('回执携带 clientMsgId', !!ack && ack.data && ack.data.clientMsgId === 'ws-001-' + RUN, ack && ack.data);

  const recvAdmin = await waitFor(cAdmin.frames, 'CHAT_MESSAGE', 4000);
  check('admin 实时收到 CHAT_MESSAGE', !!recvAdmin, cAdmin.frames.map(f => f.type));
  const recvLs = await waitFor(cLs.frames, 'CHAT_MESSAGE', 4000);
  check('lisi 实时收到 CHAT_MESSAGE', !!recvLs, cLs.frames.map(f => f.type));
  check('推送内容正确', !!recvAdmin && recvAdmin.data.content === 'WS 实时群消息', recvAdmin && recvAdmin.data);
  check('推送带会话ID', !!recvLs && String(recvLs.data.sessionId) === String(sid), recvLs && recvLs.data);
  check('推送带发送者昵称', !!recvLs && recvLs.data.senderNickname === '张三', recvLs && recvLs.data);

  console.log();
  console.log('='.repeat(60));
  console.log('STEP 5  超长消息长度校验');
  console.log('='.repeat(60));
  cZs.frames.length = 0;
  cZs.ws.send(JSON.stringify({ type: 'CHAT_SEND', data: { sessionId: sid, msgType: 'TEXT', content: 'x'.repeat(2500), clientMsgId: 'ws-over-' + RUN } }));
  const errFrame = await waitFor(cZs.frames, 'ERROR', 3000);
  check('超长消息返回 ERROR 帧', !!errFrame, cZs.frames.map(f => f.type));
  check('超长提示为友好文案而非「服务端处理失败」',
    !!errFrame && /不能超过/.test(errFrame.data.message), errFrame && errFrame.data);

  console.log();
  console.log('='.repeat(60));
  console.log('STEP 5B  幂等重发：必须补发 ACK 且不重复落库');
  console.log('='.repeat(60));
  cZs.frames.length = 0; cAdmin.frames.length = 0;
  const dupId = 'ws-dup-' + RUN;
  cZs.ws.send(JSON.stringify({ type: 'CHAT_SEND', data: { sessionId: sid, msgType: 'TEXT', content: '幂等消息', clientMsgId: dupId } }));
  const ack1 = await waitFor(cZs.frames, 'MESSAGE_ACK', 4000);
  check('首次发送收到 ACK', !!ack1, cZs.frames.map(f => f.type));
  const firstMsgId = ack1 && ack1.data.id;

  cZs.frames.length = 0;
  cZs.ws.send(JSON.stringify({ type: 'CHAT_SEND', data: { sessionId: sid, msgType: 'TEXT', content: '幂等消息', clientMsgId: dupId } }));
  const ack2 = await waitFor(cZs.frames, 'MESSAGE_ACK', 4000);
  check('幂等重发也能收到 ACK（修复点）', !!ack2, cZs.frames.map(f => f.type));
  check('幂等重发返回同一条消息 id', !!ack2 && ack2.data.id === firstMsgId, { firstMsgId, second: ack2 && ack2.data.id });

  const dupRecv = cAdmin.frames.filter(f => f.type === 'CHAT_MESSAGE' && f.data.clientMsgId === dupId);
  check('对方只收到 1 条（未重复扇出）', dupRecv.length === 1, dupRecv.length);

  console.log();
  console.log('='.repeat(60));
  console.log('STEP 6  心跳 PING/PONG');
  console.log('='.repeat(60));
  cAdmin.frames.length = 0;
  cAdmin.ws.send(JSON.stringify({ type: 'PING' }));
  const pong = await waitFor(cAdmin.frames, 'PONG', 3000);
  check('PING 收到 PONG', !!pong, cAdmin.frames.map(f => f.type));

  console.log();
  console.log('='.repeat(60));
  console.log('STEP 7  私聊实时推送');
  console.log('='.repeat(60));
  // 复用之前 admin(1) <-> zhangsan(2) 的私聊会话
  const sess = await api('GET', '/chat/sessions', tk.zhangsan);
  const priv = (sess.data || []).find(s => s.sessionType === 'PRIVATE' && String(s.targetUserId) === String(ids.admin));
  if (!priv) { check('存在私聊会话', false, sess.data); }
  else {
    cAdmin.frames.length = 0;
    cZs.ws.send(JSON.stringify({ type: 'CHAT_SEND', data: { sessionId: priv.id, msgType: 'TEXT', content: 'WS 私聊消息', clientMsgId: 'ws-p1-' + RUN } }));
    const got = await waitFor(cAdmin.frames, 'CHAT_MESSAGE', 4000);
    check('私聊对方实时收到消息', !!got, cAdmin.frames.map(f => f.type));
    check('私聊内容正确', !!got && got.data.content === 'WS 私聊消息', got && got.data);
  }

  console.log();
  console.log('='.repeat(60));
  console.log('STEP 8  在线状态：断开后在线标记变化');
  console.log('='.repeat(60));
  let mem = await api('GET', '/groups/' + gid + '/members?pageNum=1&pageSize=50', tk.zhangsan);
  let lisiM = (mem.data.records || []).find(m => String(m.userId) === String(ids.lisi));
  check('lisi 连接时 online=true', !!lisiM && lisiM.online === true, lisiM);

  cLs.ws.close();
  await sleep(800);
  mem = await api('GET', '/groups/' + gid + '/members?pageNum=1&pageSize=50', tk.zhangsan);
  lisiM = (mem.data.records || []).find(m => String(m.userId) === String(ids.lisi));
  check('lisi 断线后 offline=false', !!lisiM && lisiM.online === false, lisiM);

  cAdmin.ws.close(); cZs.ws.close();
  await sleep(300);

  console.log();
  console.log('='.repeat(60));
  console.log('结果：PASS=' + PASS + '  FAIL=' + FAIL);
  console.log('='.repeat(60));
  if (fails.length) { console.log('失败项：'); fails.forEach(f => console.log('  - ' + f)); }
  process.exit(FAIL ? 1 : 0);
})().catch(e => { console.error('脚本异常：', e); process.exit(1); });
