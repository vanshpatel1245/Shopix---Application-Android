# Shopix — Android E-Commerce Platform

A multi-panel Android e-commerce platform with dedicated apps for Buyers, Sellers, and Admins, backed by Firebase.

Designed in Figma: [Shopix Design File](https://www.figma.com/design/zwCZDhzK5P1z6zsy4132cM/Shopix?node-id=0-1&t=aNtamF5nR8yQBRbk-1)

## Project Structure

This repository contains three independent Android applications:

- **Buyer** (`com.shopix.buyer`) — Storefront app. Browse products, manage cart and favorites, place orders, and edit profile.
- **Seller** — Seller-facing app for managing product listings, orders, and store performance.
- **Admin** — Administrative app for managing users, sellers, products, and orders across the platform.

## Tech Stack

- **Language:** Kotlin
- **Build System:** Gradle with Kotlin DSL
- **Platform:** Android (compileSdk 36, minSdk 24, targetSdk 35)
- **Backend:** [Firebase](https://firebase.google.com/) (Authentication, Firestore)
- **Image Loading:** Glide

## Getting Started

### Prerequisites

- Android Studio (latest stable)
- JDK 11+
- A Firebase project with Authentication and Firestore enabled

### Firebase Setup

Each module expects its own `google-services.json` (already included for local development). If setting this up against your own Firebase project instead:

1. Create a Firebase project at [console.firebase.google.com](https://console.firebase.google.com)
2. Register each app (Buyer, Seller, Admin) with its respective package name
3. Download `google-services.json` for each and place it in the corresponding `app/` folder
4. Enable **Authentication** and **Firestore** in the Firebase console
5. Set appropriate Firestore security rules before going beyond local development/testing

### Building the Project

Each module builds independently:

```bash
# Buyer
cd Buyer
./gradlew build

# Seller
cd Seller
./gradlew build

# Admin
cd Admin
./gradlew build
```

### Running the Applications

1. Open the desired module folder in Android Studio
2. Sync Gradle
3. Connect an Android device or start an emulator
4. Run the app

## License

This project is licensed under the MIT License — see the [LICENSE](./LICENSE) file for details.
