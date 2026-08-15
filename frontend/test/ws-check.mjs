// Verifies the editor's WebSocket endpoint: HTTP upgrade against
// /gecko/ws-raw and a STOMP CONNECT handshake. Node built-ins only.
import http from 'node:http';
import crypto from 'node:crypto';

const port = process.argv[2] ?? '8081';

const key = crypto.randomBytes(16).toString('base64');
const req = http.request({
  host: 'localhost',
  port,
  path: '/gecko/ws-raw',
  headers: {
    Connection: 'Upgrade',
    Upgrade: 'websocket',
    'Sec-WebSocket-Key': key,
    'Sec-WebSocket-Version': '13',
  },
});

req.on('upgrade', (res, socket) => {
  console.log('UPGRADE OK:', res.statusCode);

  // minimal WS frame writer (client frames are masked)
  const send = (payload) => {
    const data = Buffer.from(payload, 'utf8');
    const mask = crypto.randomBytes(4);
    let header;
    if (data.length < 126) {
      header = Buffer.from([0x81, 0x80 | data.length]);
    } else {
      header = Buffer.from([0x81, 0x80 | 126, data.length >> 8, data.length & 0xff]);
    }
    const masked = Buffer.from(data);
    for (let i = 0; i < masked.length; i++) masked[i] ^= mask[i % 4];
    socket.write(Buffer.concat([header, mask, masked]));
  };

  let buffer = Buffer.alloc(0);
  socket.on('data', (chunk) => {
    buffer = Buffer.concat([buffer, chunk]);
    // decode unmasked server text frames (length < 126 assumed for CONNECTED)
    while (buffer.length >= 2) {
      const len = buffer[1] & 0x7f;
      if (buffer.length < 2 + len) break;
      const text = buffer.subarray(2, 2 + len).toString('utf8');
      buffer = buffer.subarray(2 + len);
      if (text.startsWith('CONNECTED')) {
        console.log('STOMP CONNECTED received');
        socket.destroy();
        process.exit(0);
      }
    }
  });

  send('CONNECT\naccept-version:1.2\nhost:check\n\n\0');
  setTimeout(() => {
    console.error('TIMEOUT waiting for STOMP CONNECTED');
    process.exit(1);
  }, 5000);
});

req.on('response', (res) => {
  console.error('HANDSHAKE REJECTED:', res.statusCode);
  process.exit(1);
});

req.end();
