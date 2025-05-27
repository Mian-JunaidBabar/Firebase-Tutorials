# 🔐 Firebase Google Sign-In (Android)

This Android app demonstrates how to integrate **Google Sign-In** authentication using **Firebase Auth**.

---

## 🌟 Features

- 🔑 Sign in with Google (using FirebaseAuth)
- ✅ Automatically saves user profile (name, email, profile URL)
- 🧾 Realtime DB stores authenticated user details
- 🔁 Auto-login after authentication
- 🚪 Sign-out functionality

---

## 📂 Firebase Setup

1. Create a project in [Firebase Console](https://console.firebase.google.com)
2. Enable:
   - **Authentication → Google Sign-In**
   - **Realtime Database** (optional, if saving user data)
3. Go to Project Settings → Add Android App
4. Download `google-services.json` and place it in the `app/` folder
5. Sync Gradle and build the app

---

## 🧱 Tech Stack

- Java
- Android Studio
- Firebase Authentication
- Firebase Realtime Database

---

## 🚫 Limitations

- No fallback to email/password login (Google only)
- No role-based access control
- No profile editing (just display name and email)

---

## 📎 Notes

This is a **demo app** built for academic use, showcasing Firebase integration and Google Sign-In in Android. Not production-ready.

---

## 📄 License

MIT License — Free for learning and demo use.
