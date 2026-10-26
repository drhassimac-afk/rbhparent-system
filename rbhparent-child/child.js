const { io } = require("socket.io-client");

// الاتصال بالسيرفر المحلي المطور المربوط بقاعدة البيانات
const socket = io("http://localhost:3000");

const familyId = "family_7rbh_zone"; // معرف الغرفة العائلية المشترك
const deviceRole = "child";

socket.on("connect", () => {
    console.log(`[rbhparent-child] متصل بالسيرفر بنجاح. ID: ${socket.id}`);

    // الانضمام للغرفة العائلية
    socket.emit("join_room", { familyId: familyId, role: deviceRole });
    
    // بدء إرسال الموقع الجغرافي دورياً للسيرفر
    startSendingLocation();
});

// الاستماع اللحظي لأوامر القفل القادمة من الأب عبر السيرفر
socket.on("child_lock_command", (data) => {
    console.log("\n⚠️ [تنبيه رقابي عاجل] ⚠️");
    if (data.lock) {
        console.log("🔒 تم قفل الجهاز الآن بالكامل وحظر واجهة تصفح التطبيقات!");
    } else {
        console.log("🔓 تم إلغاء قفل الجهاز بنجاح والسماح بالاستخدام.");
    }
});

function startSendingLocation() {
    let lat = 36.6500; // إحداثيات افتراضية دقيقة لقرب منطقتك
    let lng = 3.2500;

    setInterval(() => {
        // محاكاة تحرك طفيف في الموقع الجغرافي للطفل
        lat += (Math.random() - 0.5) * 0.0005;
        lng += (Math.random() - 0.5) * 0.0005;

        console.log(`[GPS] إرسال الموقع الحالي إلى قاعدة البيانات: Lat ${lat.toFixed(5)}, Lng ${lng.toFixed(5)}`);
        
        socket.emit("send_location", {
            familyId: familyId,
            latitude: lat,
            longitude: lng
        });
    }, 8000); // إرسال الموقع وتخزينه دورياً كل 8 ثوانٍ
}

socket.on("disconnect", () => {
    console.log("[rbhparent-child] قطع الاتصال بالسيرفر المركزي.");
});

