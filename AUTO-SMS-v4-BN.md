Saad Topup SMS Gateway v4 — AUTO SMS

এই version-এ Scan Recent SMS চাপা ছাড়াই নতুন SMS আসার সাথে সাথে database-এ পাঠানোর ব্যবস্থা করা হয়েছে।

Flow:
নতুন bKash/Nagad/Rocket SMS
-> Android SMS_RECEIVED
-> SmsParser
-> Firestore deposits

Phone setup:
1. App install/update করে একবার open করুন।
2. SMS permission Allow দিন।
3. Settings > Apps > Saad Topup SMS Gateway > Battery > Unrestricted (যদি option থাকে)।
4. App Force Stop করবেন না।
5. "Scan Recent SMS" শুধু পুরনো SMS test করার জন্য।

Technical fix:
SmsReceiver এখন goAsync() ব্যবহার করে এবং Firestore upload complete না হওয়া পর্যন্ত BroadcastReceiver alive রাখে।
