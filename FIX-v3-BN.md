Saad Topup SMS Gateway v3 FIX

এই version-এ দুইটা সমস্যা ঠিক করা হয়েছে:

1) Nagad TxnID
Nagad SMS:
Money Received.
Amount: Tk 5075.00
Sender: 01967965165
Ref: N/A
TxnID: 75XZZW4U

এখন app "Sender" number নেবে না। "TxnID:" এর value 75XZZW4U-ই trxId হবে।

2) Website "This Transaction ID has already been used"
আগের app SMS আসার সাথে deposits collection-এ trxId লিখত, আর পুরোনো website code যেকোনো existing trxId-কে already used বলত।

নতুন flow:
SMS -> deposits/{TxnID}, status=pending, source=sms_gateway, used=false
Website verify -> ওই pending SMS record খুঁজবে
Amount + Method match -> user balance add
একই record -> status=approved, used=true, userId/userEmail বসবে
একই TxnID দ্বিতীয়বার -> "already used"

IMPORTANT:
APK ঠিক করতে GitHub-এ এই ZIP-এর Android project upload/build করুন।
Website-এর জন্য website/index-fixed.html আপনার বর্তমান index.html-এর জায়গায় ব্যবহার করুন।
