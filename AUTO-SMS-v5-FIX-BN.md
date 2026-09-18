# Saad Topup SMS Gateway v5 — Auto Fix

## কী কী ঠিক করা হয়েছে

1. **BootReceiver** যোগ করা হয়েছে → ফোন রিস্টার্ট হলেও অটো কাজ করবে।
2. **Battery Unrestricted** বাটন যোগ করা হয়েছে → এক ক্লিকে সেটিংস খুলে দেয়।
3. **Notification** → SMS সফলভাবে আপলোড হলে নোটিফিকেশন দেখায় (অটো কাজ করছে কিনা বোঝা যায়)।
4. **Sender matching** আরও ফ্লেক্সিবল করা হয়েছে (bkash, nagad, rocket contains check)।
5. **Status screen** এ SMS Permission + Battery অবস্থা দেখায়।
6. `goAsync()` আগের মতোই আছে → upload শেষ না হওয়া পর্যন্ত receiver মরে না।
7. Version → **5.0-auto-fix**

## অটো কাজ না করলে যা করবেন (খুব জরুরি)

### 1. SMS Permission
অ্যাপ খুলে **“1. Allow SMS Permission”** চাপুন → Allow দিন।

### 2. Battery Unrestricted (সবচেয়ে গুরুত্বপূর্ণ)
**“2. Battery → Unrestricted”** বাটন চাপুন → Allow / No restrictions সিলেক্ট করুন।

### 3. Xiaomi / Redmi / Poco
Settings → Apps → Manage apps → Saad Topup SMS Gateway → Autostart → **Enable**

### 4. Realme / Oppo / Vivo
Settings → Battery → App battery management → Saad Topup → **Unrestricted / Allow background activity**

### 5. Force Stop করবেন না
অ্যাপ লিস্ট থেকে Force Stop চাপলে অটো বন্ধ হয়ে যায়।

### 6. টেস্ট
নিজের নাম্বারে ছোট amount-এ bKash/Nagad পাঠান।  
সফল হলে নোটিফিকেশন আসবে + Firebase deposits-এ দেখা যাবে।

## Build
Android Studio দিয়ে বা GitHub Actions দিয়ে নতুন APK বানান (versionCode 5)।
