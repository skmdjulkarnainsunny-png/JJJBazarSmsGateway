Saad Topup SMS Gateway

কাজ:
- ফোনে নতুন bKash / Nagad / Rocket SMS এলে payment SMS detect করবে।
- শুধু method, amount, transaction ID, sender, timestamp server endpoint-এ পাঠাবে।
- OTP/PIN বা unrelated SMS upload করার জন্য তৈরি করা হয়নি।
- "Scan Recent SMS" দিয়ে inbox-এর সাম্প্রতিক 100 SMS থেকে payment SMS detect করা যায়।

Website compatibility:
- আপনার website `XNXANIKPAY` Realtime Database path-এ `txid` দিয়ে payment খোঁজে।
- এই project-এর server endpoint একই path-এ data লিখে।

খুব গুরুত্বপূর্ণ:
1) Firebase Realtime Database enable করতে হবে।
2) Website Firebase config-এ databaseURL যোগ করতে হবে।
3) Browser/client থেকে user balance update করা নিরাপদ নয়। Final production version-এ balance credit server-side Cloud Function দিয়ে করা উচিত।
4) Realtime Database public write করবেন না।
5) App-এ direct Firebase admin credential রাখবেন না।

Build:
- Android Studio-তে project folder open করুন।
- SDK 35 install থাকলে Build > Build APK(s)।
- Generated APK সাধারণত app/build/outputs/apk/debug/app-debug.apk এ পাওয়া যাবে।

App setup:
- Server endpoint URL বসান।
- X-Device-Token token বসান।
- SMS permission Allow দিন।

Server:
- server/index.js Firebase Cloud Functions-এর example endpoint।
- DEVICE_TOKEN environment variable/secret হিসেবে সেট করতে হবে।
- Firebase project-এ Realtime Database configure থাকতে হবে।


YOUR REALTIME DATABASE
https://saad-topup-default-rtdb.asia-southeast1.firebasedatabase.app

IMPORTANT:
এই URL app-এর "Server endpoint URL" ঘরে বসাতে হবে না।
App-এর endpoint হবে deployed HTTPS Cloud Function/API URL.
Realtime Database URL server-side code-এর মধ্যে ব্যবহার হবে।

NEXT:
1. Firebase Console > Realtime Database-এ database চালু আছে কিনা নিশ্চিত করুন।
2. Firebase Functions/server deploy করুন।
3. Deploy করার পর যে HTTPS function URL পাবেন, সেটাই APK-এর Server endpoint URL হবে।
4. একই secret/device token server এবং APK-তে ব্যবহার করুন।
5. Website Firebase config-এ databaseURL হিসেবে উপরের RTDB URL যোগ করুন।
6. Production-এ balance credit server-side function দিয়ে করা উচিত; browser-side balance update বন্ধ করুন।

GITHUB APK BUILD
Android Studio ছাড়াই GitHub Actions দিয়ে APK build করা যাবে। বিস্তারিত GITHUB-BUILD-BN.md-এ আছে।
