const express = require('express');
const http = require('http');
const { Server } = require('socket.io');
const sqlite3 = require('sqlite3').verbose();

const app = express();
const server = http.createServer(app);
const io = new Server(server, { cors: { origin: "*" } });

const PORT = process.env.PORT || 3000;
app.use(express.json());

// الاتصال بقاعدة البيانات وإنشاء الجداول تلقائياً
const db = new sqlite3.Database('./rbhparent.db', (err) => {
    if (err) console.error('خطأ في قاعدة البيانات:', err.message);
    else console.log('🔒 متصل بقاعدة بيانات SQLite بنجاح.');
});

db.serialize(() => {
    // 1. جدول تتبع المواقع التاريخي
    db.run(`CREATE TABLE IF NOT EXISTS location_logs (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        family_id TEXT,
        latitude REAL,
        longitude REAL,
        time TEXT DEFAULT CURRENT_TIMESTAMP
    )`);
    // 2. جدول حالة الأجهزة (مغلق / مفتوح)
    db.run(`CREATE TABLE IF NOT EXISTS device_locks (
        family_id TEXT PRIMARY KEY,
        is_locked INTEGER DEFAULT 0
    )`);
});

// واجهة برمجية (API) للأب لجلب آخر 20 موقع جغرافي للطفل
app.get('/api/history/:familyId', (req, res) => {
    db.all(`SELECT * FROM location_logs WHERE family_id = ? ORDER BY id DESC LIMIT 20`, [req.params.familyId], (err, rows) => {
        if (err) return res.status(500).json({ error: err.message });
        res.json({ success: true, history: rows });
    });
});

// واجهة برمجية لمعرفة حالة القفل الحالية للجهاز
app.get('/api/status/:familyId', (req, res) => {
    db.get(`SELECT is_locked FROM device_locks WHERE family_id = ?`, [req.params.familyId], (err, row) => {
        if (err) return res.status(500).json({ error: err.message });
        res.json({ success: true, isLocked: row ? row.is_locked === 1 : false });
    });
});

// إدارة الاتصالات اللحظية عبر WebSockets
io.on('connection', (socket) => {
    console.log(`جهاز متصل جديد: ${socket.id}`);

    socket.on('join_room', (data) => {
        socket.join(data.familyId);
        console.log(`الجهاز [${socket.id}] انضم للعائلة: ${data.familyId} بصفة: ${data.role}`);
    });

    // استقبال الموقع من الطفل وحفظه ثم بثه للأب
    socket.on('send_location', (data) => {
        const { familyId, latitude, longitude } = data;
        db.run(`INSERT INTO location_logs (family_id, latitude, longitude) VALUES (?, ?, ?)`, [familyId, latitude, longitude]);
        io.to(familyId).emit('receive_location', { latitude, longitude, time: new Date().toLocaleTimeString() });
    });

    // استقبال أمر القفل من الأب وحفظه وتمريره للطفل فوراً
    socket.on('lock_device', (data) => {
        const { familyId, lock } = data;
        const lockValue = lock ? 1 : 0;
        db.run(`INSERT INTO device_locks (family_id, is_locked) VALUES (?, ?) ON CONFLICT(family_id) DO UPDATE SET is_locked = ?`, [familyId, lockValue, lockValue]);
        io.to(familyId).emit('child_lock_command', { lock });
    });

    socket.on('disconnect', () => console.log(`جهاز قطع الاتصال: ${socket.id}`));
});

server.listen(PORT, '0.0.0.0', () => console.log(`سيرفر rbhparent المركزي يعمل على المنفذ: ${PORT}`));

