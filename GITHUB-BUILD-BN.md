# Android Studio ছাড়া GitHub দিয়ে APK বানানোর নিয়ম

## 1. GitHub-এ নতুন repository বানাও
- GitHub.com এ login করো
- New repository চাপো
- নাম দাও: `SaadTopupSmsGateway`
- Public বা Private যেকোনোটি দিতে পারো
- Create repository

## 2. ZIP Extract করো
`SaadTopupSmsGateway_GitHub_APK.zip` extract করলে যে project folder পাবে, তার ভেতরের সব file GitHub repository-তে upload করো।

Repository root-এ এগুলো দেখা উচিত:
- `app/`
- `server/`
- `.github/`
- `build.gradle`
- `settings.gradle`

## 3. GitHub Actions নিজে APK build করবে
`.github/workflows/build-apk.yml` আগে থেকেই দেওয়া আছে।

Files upload/commit করার পর:
- GitHub repository → Actions
- `Build Android APK` workflow নির্বাচন করো
- `Run workflow` চাপতে পারো
- build শেষ হওয়া পর্যন্ত অপেক্ষা করো

## 4. APK নেওয়া
Workflow সফল হলে:
- Actions → সফল workflow run
- নিচে `Artifacts` section
- `SaadTopupSmsGateway-debug-apk` download
- ZIP খুললে `app-debug.apk` পাবে

## গুরুত্বপূর্ণ
এই workflow শুধু Android APK build করে। SMS → Firebase server endpoint আলাদাভাবে deploy করতে হবে। APK-এর Settings-এ Firebase RTDB URL নয়, deployed HTTPS API/function URL দিতে হবে।

Realtime Database:
https://saad-topup-default-rtdb.asia-southeast1.firebasedatabase.app

Production security:
- Firebase RTDB public write কোরো না।
- API token GitHub code-এর মধ্যে hard-code কোরো না।
- Website-এর balance credit server-side করা উচিত।
