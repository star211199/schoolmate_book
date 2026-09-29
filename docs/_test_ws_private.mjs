// 私聊实时推送：确定性复现测试（连跑 3 轮）
// 可用环境变量覆盖：TEST_BASE / TEST_WS / TEST_ORIGIN
// 注意：natapp 免费隧道对突发新连接数限流（429 Connections Exceed），
// 回归时建议用 SSH 端口转发把 TEST_BASE / TEST_WS 指到 127.0.0.1:15173，
// 再用 TEST_ORIGIN 模拟穿透域名来源。
const BASE = process.env.TEST_BASE || 'http://127.0.0.1:8080/api';
const WS_BASE = process.env.TEST_WS || 'ws://127.0.0.1:8080/api/ws/chat';
// 浏览器在非 GET 请求上一定会带 Origin（Node 的 fetch 默认不带）。
// 后端用它和 Host 比对判定跨域，不匹配且不在白名单时返回 403 Invalid CORS request。
const ORIGIN = process.env.TEST_ORIGIN || new URL(BASE).origin;
const sleep = (ms) => new Promise(r => setTimeout(r, ms));

async function api(method, path, token, body) {
  const headers = { 'Content-Type': 'application/json', Origin: ORIGIN };
  if (token) headers['Authorization'] = 'Bearer ' + token;
  const r = await fetch(BASE + path, { method, headers, body: body ? JSON.stringify(body) : undefined });
  return await r.json();
}

function openWs(token) {
  const ws = new WebSocket(WS_BASE + '?token=' + encodeURIComponent(token));
  const frames = [];
  ws.addEventListener('message', (e) => { try { frames.push(JSON.parse(e.data)); } catch (_) { frames.push(e.data); } });
  return new Promise((resolve, reject) => {
    ws.addEventListener('open', () => resolve({ ws, frames }));
    ws.addEventListener('error', () => reject(new Error('ws error')));
    ws.addEventListener('close', (e) => reject(new Error('closed ' + e.code)));
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
  const tk = {};
  for (const u of ['admin', 'zhangsan']) {
    const r = await api('POST', '/auth/login', null, { username: u, password: '123456' });
    tk[u] = r.data.token;
  }
  // 确保私聊会话存在
  const meZ = await api('GET', '/auth/me', tk.zhangsan);
  const sid = (await api('POST', '/chat/sessions/private/' + meZ.data.id, tk.admin)).data;
  console.log('privateSessionId =', sid);

  for (let round = 1; round <= 3; round++) {
    console.log('\n---------- ROUND ' + round + ' ----------');
    const a = await openWs(tk.admin);
    const z = await openWs(tk.zhangsan);
    await sleep(200);
    console.log('  connected: admin=%s zhangsan=%s', a.ws.readyState, z.ws.readyState);
    console.log('  admin conn frames =', a.frames.map(f => f.type));

    // 张三 -> 管理员
    a.frames.length = 0; z.frames.length = 0;
    z.ws.send(JSON.stringify({ type: 'CHAT_SEND', data: { sessionId: sid, msgType: 'TEXT', content: 'R' + round + '-zs->admin', clientMsgId: 'r' + round + '-a' } }));
    const ackZ = await waitFor(z.frames, 'MESSAGE_ACK', 3000);
    const gotAdmin = await waitFor(a.frames, 'CHAT_MESSAGE', 3000);
    console.log('  [%s] zhangsan 收到 ACK', ackZ ? 'OK' : 'MISS');
    console.log('  [%s] admin 收到 CHAT_MESSAGE', gotAdmin ? 'OK' : 'MISS');
    console.log('  admin frames =', JSON.stringify(a.frames.map(f => f.type)));
    if (gotAdmin) console.log('  admin content =', gotAdmin.data.content);

    // 管理员 -> 张三
    a.frames.length = 0; z.frames.length = 0;
    a.ws.send(JSON.stringify({ type: 'CHAT_SEND', data: { sessionId: sid, msgType: 'TEXT', content: 'R' + round + '-admin->zs', clientMsgId: 'r' + round + '-b' } }));
    const ackA = await waitFor(a.frames, 'MESSAGE_ACK', 3000);
    const gotZ = await waitFor(z.frames, 'CHAT_MESSAGE', 3000);
    console.log('  [%s] admin 收到 ACK', ackA ? 'OK' : 'MISS');
    console.log('  [%s] zhangsan 收到 CHAT_MESSAGE', gotZ ? 'OK' : 'MISS');
    console.log('  zhangsan frames =', JSON.stringify(z.frames.map(f => f.type)));
    if (gotZ) console.log('  zhangsan content =', gotZ.data.content);

    a.ws.close(); z.ws.close();
    await sleep(500);
  }
  process.exit(0);
})().catch(e => { console.error('异常：', e); process.exit(1); });
