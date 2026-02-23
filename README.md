# BloodLink - Android Blood Donation Platform

BloodLink is a comprehensive Android application designed to bridge the gap between blood donors and those in need. It leverages Firebase for real-time data management, location services for proximity-based requests, and a robust administrative system for inventory tracking.

## 🚀 Core Features

- **User Authentication**: Secure signup and login system for donors, seekers, and administrators.
- **Blood Requests**: Users can create emergency blood requests with location-specific details.
- **Donation Management**: Track donation history and schedule new donations.
- **Real-time Inventory**: Live tracking of blood types available across different banks/locations.
- **Location Services**: Interactive maps and location picker for precise request coordination.
- **Notifications**: Real-time alerts for urgent blood needs and status updates.
- **Analytics Dashboard**: Visual representation of donation trends and inventory status.

## 🏗️ Project Architecture & Modules

The project is structured into several key functional modules within the `com.example.blood` package:

### 📱 User Interface (Activities)
- `MainActivity`: The entry point of the application.
- `Login` & `Signup`: Handles user authentication flows.
- `Dashboard`: The central hub for user interactions and navigation.
- `RequestBlood` & `EmergencyRequestActivity`: Interfaces for creating blood requirements.
- `DonateActivity`: Interface for donors to record or schedule donations.
- `BloodInventoryActivity`: Displays current stock of different blood types.
- `Profile` & `ProfileActivity`: User profile management and settings.
- `DonationHistory`: Comprehensive list of past donations.
- `AnalyticsActivity`: Displays statistical data and graphs.
- `LocationPickerActivity` & `BloodBanksMapActivity`: Map-based tools for location coordination.

### 📊 Data Models (`models` package)
- `UserProfile`: Represents user data, including contact info and blood type.
- `BloodRequest`: Data structure for blood requirements.
- `BloodDonation`: Records of donation events.
- `BloodBank`: Represents a blood bank facility and its location.
- `BloodInventory`: Stock levels for various blood groups.

### 🔥 Backend & Integration (`firebase` & `integration` packages)
- `FirebaseManager`: Centralized class for all Firestore/Realtime Database operations.
- `FirebaseHelper`: Utility for common Firebase tasks.
- `InitialDataLoader`: Handles seeding the database with necessary initial data.
- `IntegrationExamples`: Provides templates for connecting UI components with backend logic.

### 🛠️ Utilities (`utils` package)
- `NotificationHelper`: Manages system-level notifications and alerts.

## 🛠️ Tech Stack

- **Language**: Java
- **UI Framework**: Android XML with Material Design
- **Backend**: Firebase (Authentication, Realtime Database/Firestore)
- **Maps**: OSMDroid (OpenStreetMap)
- **Location**: Google Play Services Location
- **Build System**: Gradle

## 📁 Project Structure

```bash
Blood/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── AndroidManifest.xml    # App configuration and permissions
│   │   │   ├── java/com/example/blood/ # Source code
│   │   │   │   ├── firebase/          # Firebase logic
│   │   │   │   ├── integration/       # UI-Backend integration patterns
│   │   │   │   ├── models/            # Data entities
│   │   │   │   └── utils/             # Helper utilities
│   │   │   └── res/                   # Layouts, Drawables, Strings
│   └── build.gradle                   # App-level dependencies
├── build.gradle                       # Project-level build script
└── README.md                          # Documentation
```

## ⚙️ Prerequisites & Setup

1. **Android Studio**: Jellyfish or later recommended.
2. **JDK**: Version 11.
3. **Firebase Setup**:
   - Place your `google-services.json` in the `app/` directory.
   - Enable Email/Password authentication in Firebase Console.
   - Set up Realtime Database or Firestore as per the `firebase_rules.json`.
4. **Permissions**: The app requires Internet, Fine Location, and SMS permissions for full functionality.

---
Developed as a platform to save lives through efficient blood resource management.
