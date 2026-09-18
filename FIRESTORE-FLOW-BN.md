SaadTopup SMS Gateway — Firestore version

Flow:
Nagad/bKash/Rocket SMS
-> SMS Reader APK
-> TrxID + Amount + Method + Date
-> Firestore deposits
-> Website

This build writes:
amount, date, method, status="pending", trxId, userEmail="", userId=""

Important:
SMS does not normally contain the website user's Firebase userId/userEmail.
Therefore this APK cannot reliably attach those fields to a specific existing user by itself.
The website/server should match a pending deposit to the user using the transaction/payment flow.

Firebase config:
google-services.json is included for project saad-topup and package com.saadtopup.smsgateway.
