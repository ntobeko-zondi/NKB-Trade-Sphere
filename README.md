# 🚀 TradeSphere (NKB Trade Sphere)

<div align="center">

### A Modern Android Marketplace Platform

Buy • Sell • Discover • Connect

Built with **Java**, **SQLite**, **Android Studio**, and **Material Design**

</div>

---

## 📱 Overview

TradeSphere is a fully functional Android marketplace application developed as part of a university software engineering project.

The platform enables users to browse products, create listings, save items, exchange messages, rate sellers, and manage their marketplace activity through a modern mobile interface.

Unlike many student projects that rely heavily on cloud services, TradeSphere demonstrates the ability to design and implement a complete local-first application architecture using SQLite, secure local authentication, repository patterns, and structured Android development practices.

---

## ✨ Key Features

### 🔐 Authentication & User Management

* User registration and login
* Salted password hashing
* Persistent user sessions
* Secure local account storage
* Session recovery

### 🛍 Marketplace Experience

* Browse available products
* Category-based discovery
* Swipe-card product exploration
* Product search functionality
* Detailed product pages
* Save favourite items

### 💬 Messaging System

* User-to-user conversations
* Local SQLite message storage
* Marketplace communication workflow

### ⭐ Rating & Reputation

* Seller ratings
* User credibility tracking
* Marketplace trust indicators

### 🎨 Modern UI/UX

* Material Design principles
* Light and Dark Mode
* Responsive layouts
* Animated interactions
* Intuitive navigation

### 💾 Data Persistence

* SQLite database
* Repository architecture
* Structured data access layer
* Offline functionality

---

## 🏗 Architecture

TradeSphere follows a layered architecture that separates presentation, business logic, and persistence.

```text
UI Layer
│
├── Activities
├── Fragments
├── Adapters
└── Custom Views
        │
        ▼
Repository Layer
│
├── ProductRepository
└── UserRepository
        │
        ▼
Data Layer
│
├── SQLite Database
├── Models
└── Catalog Seed Data
```

This separation improves maintainability, scalability, and testability.

---

## 🛠 Technology Stack

| Category        | Technology        |
| --------------- | ----------------- |
| Language        | Java              |
| IDE             | Android Studio    |
| Database        | SQLite            |
| Networking      | Volley            |
| Image Loading   | Glide             |
| UI Framework    | AndroidX          |
| Design System   | Material Design   |
| Session Storage | SharedPreferences |
| Version Control | Git & GitHub      |

---

## 📸 Screenshots

### Authentication

![Login Screen](screenshots/login.png)

### Home Feed

![Home Screen](screenshots/home.png)

### Product Details

![Product Details](screenshots/product_details.png)

### Profile

![Profile](screenshots/profile.png)

---

## 🧠 What We Learned

This project provided practical experience in:

* Mobile application development
* Software architecture
* Database design
* Authentication systems
* UI/UX engineering
* Team collaboration
* Version control workflows
* Agile development practices

---

## 🚧 Future Improvements

* Cloud synchronization
* Real-time messaging
* Push notifications
* Image uploads to cloud storage
* AI-powered recommendations
* Payment gateway integration
* Seller verification system
* Multi-language support

---

## 📚 Documentation

| Document                       | Description                           |
| ------------------------------ | ------------------------------------- |
| docs/PRD.md                    | Product Requirements Document         |
| docs/SQLITE_PRODUCT_CATALOG.md | Database Design & SQLite Architecture |

---

## 👥 Development Team

Developed by the NKB TradeSphere Team as part of a university software engineering project.

---

## 🚀 Getting Started

### Requirements

* JDK 17+
* Android Studio
* Android SDK (compileSdk 34)

### Build

Linux / macOS

```bash
./gradlew assembleDebug
```

Windows

```bat
gradlew.bat assembleDebug
```

---

## ⭐ Why This Project Matters

TradeSphere demonstrates the ability to build a complete Android application using industry-relevant development practices, including layered architecture, local authentication, persistent storage, repository patterns, UI design, and software engineering principles.

It serves as a practical example of end-to-end mobile application development from requirements gathering and system design to implementation and testing.
