# DealLens 🛒🔍 — Offline Android Deal Calculator, Cart Optimizer & Voucher Wallet

**DealLens** is a **100% offline, privacy-first native Android application** built with **Kotlin, Jetpack Compose (Material 3), Room SQLite, Hilt, and Google ML Kit OCR**. It empowers smart shoppers to compare unit prices, evaluate bulk deals, optimize grocery carts against complex store vouchers, and track shopping expenses locally.

---

## ✨ Features

### 1. 🧮 Smart Unit Price & Deal Calculator
- Compare unit costs across different volumes, weights, and packaging sizes (e.g., `g`, `kg`, `ml`, `L`, `pcs`).
- Evaluate multi-buy discounts (e.g., "Buy 2 for RM 15", "Buy 1 Get 1 at 50% off").
- Side-by-side comparison matrix with instant visual indicators for the best value.

### 2. 🧺 Cart Optimizer & Voucher Allocation
- Maximize savings by matching cart items against discount rules and platform/store vouchers.
- Handles minimum spend thresholds, category caps, and percentage discounts.
- Dynamic cart solver to achieve optimal voucher splits and avoid overspending.

### 3. 🎫 Digital Voucher Wallet & Expiry Reminders
- Store and manage vouchers with minimum spends, cashback limits, and expiry dates.
- Background reminder alerts via Android `WorkManager` before vouchers expire.

### 4. 📸 On-Device Receipt & Price Tag OCR
- Capture or import shelf price tags and shopping receipts.
- Fast offline text extraction using on-device Google ML Kit — zero network permissions required.
- Quick fill to extract product name, price, quantity, and unit of measurement.

### 5. 💰 MoneyLens Interoperability
- Export shopping transactions and deal records into **MoneyLens** import-ready JSON formats.
- Complete offline JSON database backup and restore.

---

## 🏗 Architecture & Tech Stack

- **UI:** 100% Jetpack Compose with Material Design 3 and Dynamic Color.
- **Architecture:** Clean Architecture + MVI/MVVM with multi-module design (`:app`, `:core:domain`, `:core:pricing`, `:core:data`).
- **Database:** Local Room SQLite with KSP and type converters.
- **Dependency Injection:** Hilt / Dagger.
- **Concurrency:** Kotlin Coroutines & StateFlow.
- **Background Tasks:** Android Jetpack WorkManager for voucher alerts.
- **Privacy:** 100% on-device operation, zero tracking, no telemetry.

---

## 🛠 Building & Testing

```bash
# Run unit tests across core modules
./gradlew test

# Assemble debug APK
./gradlew assembleDebug
```
