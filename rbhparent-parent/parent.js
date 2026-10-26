const { io } = require("socket.io-client");

// الاتصال بسيرفر rbhparent المركزي
const socket = io("http://localhost:3000");

const familyId = "family_7rbh_zone"; // يجب أن يكون نفس معرف غرفة الطفل ليلتقوا فيه
const deviceRole = "parent";         // هوية الجهاز (الأب)

socket.on("connect", () => {
    console.log(`[هاتف الأب] متصل بالسيرفر بنجاح. معرف الاتصال: ${socket.id}`);

    // الانضمام إلى الغرفة العائلية المشتركة
    socket.emit("join_room", { userId: familyId, role: deviceRole });
    console.log(`[هاتف الأب] دخلت الآن إلى الغرفة العائلية الفعالة: ${familyId}\n`);

    // محاكاة: الأب يرسل أمر "قفل الهاتف" الفوري لطفله بعد 7 ثوانٍ من الاتصال للتجربة
    setTimeout(() => {
        console.log("🔒 [لوحة التحكم] جاري إرسال أمر قفل الهاتف اللحظي إلى جهاز الطفل الآن...");
        socket.emit("lock_device", { userId: familyId, lock: true });
    }, 7000);

    // محاكاة ثانية: الأب يرسل أمر "إلغاء القفل" بعد 15 ثانية من الاتصال
    setTimeout(() => {
        console.log("🔓 [لوحة التحكم] جاري إرسال أمر فك القفل والسماح بالاستخدام للطفل...");
        socket.emit("lock_device", { userId: familyId, lock: false });
    }, 15000);
});

// الاستماع اللحظي لإحداثيات الموقع القادمة من هاتف الطفل وعرضها للأب فوراً
socket.on("receive_location", (data) => {
    console.log(`📍 [رصد الخريطة] موقع طفلك الحالي الآن: Latitude (${data.latitude.toFixed(5)}), Longitude (${data.longitude.toFixed(5)})`);
});

socket.on("disconnect", () => {
    console.log("[هاتف الأب] انقطع الاتصال بالسيرفر المركزي.");
});
