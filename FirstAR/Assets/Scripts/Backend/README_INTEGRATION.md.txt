# FirstAR ↔ Backend Integration Guide

## Architecture

```
FirstAR/Assets/Scripts/Backend/
├── BackendConfig.cs         → ScriptableObject: server URL + all endpoint paths
├── ApiClient.cs             → Singleton: HTTP client with JWT auth + token refresh
├── AuthService.cs           → Singleton: login, register, logout, user profile
├── PlantService.cs          → Singleton: plant recommendations, my crops
├── HomeService.cs           → Singleton: weather, daily quote, calendar
├── DesignUploadService.cs   → Singleton: uploads AR screenshots to backend
├── BackendManager.cs        → Singleton: initializes everything, health check on start
└── ConnectionTestUI.cs      → Debug UI: test connection, login, fetch data
```

## Setup in Unity Editor

### 1. Create BackendConfig Asset
- Right-click in Project → **Create → Gharsih → Backend Config**
- Set the `Base Url` field:
  - **Android device on same WiFi:** `http://<your-PC-IP>:8081`
  - **Android Emulator:** `http://10.0.2.2:8081`
  - **Unity Editor testing:** `http://localhost:8081`

### 2. Create BackendManager GameObject
- Create an **empty GameObject** in your AR scene → name it `BackendManager`
- Add these components:
  - `BackendManager` → assign the BackendConfig asset
  - `ApiClient` → assign the same BackendConfig asset
  - `AuthService`
  - `PlantService`
  - `HomeService`
  - `DesignUploadService`

### 3. Connect AR Screenshot Upload
- The `ARPlacementManager` (RaycastScript.cs) **already** calls `DesignUploadService` 
  after saving a screenshot. No additional wiring needed.

### 4. Test Connection (Optional Debug Panel)
- Add `ConnectionTestUI` to a debug Canvas with text fields and buttons
- Allows testing health check, login, and crop fetching in-app

## How It Works

1. **App starts** → `BackendManager.Start()` calls health check
2. **User logs in** → `AuthService.Login(email, password)` gets JWT token
3. **Token stored** → `ApiClient` adds `Authorization: Bearer <token>` to all requests
4. **Token expires** → `ApiClient` automatically refreshes using refresh token
5. **AR screenshot** → saved locally + uploaded to backend if authenticated

## API Endpoints Used

| Feature              | Endpoint                                    |
|---------------------|---------------------------------------------|
| Health Check         | GET `/api/security/health`                  |
| Login                | POST `/api/auth/login`                      |
| Register             | POST `/api/auth/register`                   |
| Refresh Token        | POST `/api/auth/refresh-token`              |
| User Profile         | GET `/api/users/me`                         |
| Weather              | GET `/api/user/home/weather`                |
| Daily Quote          | GET `/api/user/home/daily-quote`            |
| Calendar             | GET `/api/user/home/calendar`               |
| Plant Questions      | GET `/api/user/plant-recommendation/questions` |
| Submit Answers       | POST `/api/user/plant-recommendation/submit-answers` |
| Crops Overview       | GET `/api/user/my-crops/overview`           |
| Planned Crops        | GET `/api/user/my-crops/planned`            |
| Planted Crops        | GET `/api/user/my-crops/planted`            |
| Notifications Count  | GET `/api/user/notifications/unread-count`  |
| Upload Design        | POST `/api/user/designs/upload`             |

## Backend Requirements

The Spring Boot backend must be running on port `8081` with:
- PostgreSQL database connected
- CORS allowing the Unity client's origin
- JWT authentication enabled
