 🚀 TradeSphere (NKB Trade Sphere)

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

### - Landing Screen
<img width="287" height="600" alt="Landing Page" src="https://github.com/user-attachments/assets/22c64ca5-6701-464a-8b5b-b54a9043cb2f" />


### - Signup Screen
<img width="332" height="628" alt="Signup Page" src="https://github.com/user-attachments/assets/44ba2036-cdfe-4ae5-903f-2d3bb6b7c58a" />

### Pop-Up Details

### - Pop-Up Screen 
<img width="287" height="607" alt="Popup Page" src="https://github.com/user-attachments/assets/8017fd91-fe5c-4bca-83b3-09602bf0a945" />

### Home Page

### - Home Screen
<img width="318" height="641" alt="Home Page" src="https://github.com/user-attachments/assets/9e35ce83-306a-44b7-996f-b0433cdd58a1" />

### Search Page 

### - Search Screen
<img width="315" height="671" alt="Search Page" src="https://github.com/user-attachments/assets/121afc74-945d-444f-b09b-77c8e715c7b1" />

### Sell Page 

### - Sell Screen
<img width="290" height="597" alt="Sell Page" src="https://github.com/user-attachments/assets/52d2eb87-bf4b-47c9-8ea7-baa738bff607" />


### - Buying Screen
<img width="316" height="636" alt="Buying Page" src="https://github.com/user-attachments/assets/f5feea07-f063-4840-9f70-f9d589d79daa" />

### Messages Page 

### - Messages Screen
<img width="295" height="600" alt="Messages Page" src="https://github.com/user-attachments/assets/ced5f94e-a489-4be5-b62c-44c290cc8482" />

### Profile

### - Profile Screen
<img width="295" height="590" alt="Profile Page" src="https://github.com/user-attachments/assets/35901aa6-00df-4813-adee-be4301d66772" />

### - Wishlist Screen
<img width="293" height="595" alt="Wishlist Page" src="https://github.com/user-attachments/assets/b1b0ef0b-9d61-4e2c-815b-dded34be869e" />




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
