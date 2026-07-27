# Shopix

A multi-module Android e-commerce application designed to provide a complete online shopping ecosystem with dedicated applications for customers, sellers, and administrators.

Designed in Figma:[Shopix App Design File](https://www.figma.com/design/zwCZDhzK5P1z6zsy4132cM/Shopix?node-id=0-1&t=aNtamF5nR8yQBRbk-1)

## Project Structure

This repository contains three independent Android applications:

- **Shopix** — Customer-facing app. Users can browse products, manage their cart, place orders, track deliveries, and manage their profile.
- **ShopixSeller** — Seller-facing app. Vendors can add products, manage inventory, process customer orders, and monitor sales.
- **ShopixAdmin** — Administrative dashboard for managing users, sellers, products, orders, categories, and overall platform operations.

## Tech Stack

- **Language:** Kotlin
- **Build System:** Gradle with Kotlin DSL
- **Platform:** Android
- **Backend / Database:** Firebase
- **Media Storage:** Cloudinary (Product & Profile Images)

## Getting Started

### Prerequisites

- Android Studio Arctic Fox or later
- JDK 11 or later
- Android SDK with the latest build tools

### Building the Project

Each module can be built independently:

```bash
# Build Shopix (Customer)
cd Shopix
./gradlew build

# Build ShopixSeller
cd ShopixSeller
./gradlew build

# Build ShopixAdmin
cd ShopixAdmin
./gradlew build
```

### Running the Applications

1. Open the desired module folder in Android Studio.
2. Sync the Gradle files.
3. Connect an Android device or start an emulator.
4. Run the application.

## Module Descriptions

### Shopix (Customer)

The customer application enables users to discover products across categories, search items, add products to their cart or wishlist, place secure orders, track order status, and manage their account.

### ShopixSeller

The seller application allows vendors to create and manage their online store, publish products, update inventory, receive customer orders, and monitor business performance.

### ShopixAdmin

The administrative application provides complete control over the platform, including user management, seller approvals, product moderation, category management, order management, and overall system administration.

## Development

This project follows standard Android development practices using Kotlin. Each module is structured as an independent Android application with its own build configuration, allowing separate development and deployment.

## License

This project is licensed under the MIT License — see the [LICENSE](./LICENSE) file for details.
