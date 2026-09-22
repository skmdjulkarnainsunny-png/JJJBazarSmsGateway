# Sender Filter

এই version-এ SMS parser শুধু এই sender name গ্রহণ করে:

- bKash
- Nagad
- Rocket

অন্য কোনো phone number বা sender থেকে একই format-এর SMS এলেও database-এ যাবে না।

উদাহরণ:

`bKash` → গ্রহণযোগ্য
`Nagad` → গ্রহণযোগ্য
`Rocket` → গ্রহণযোগ্য
`017xxxxxxxx` → Reject

নোট: SMS sender name spoof করার মতো carrier/network-level সমস্যা থাকলে SMS-only পদ্ধতিতে 100% authenticity নিশ্চিত করা যায় না। Payment-এর চূড়ান্ত verification-এর জন্য transaction/API verification আরও নিরাপদ।
