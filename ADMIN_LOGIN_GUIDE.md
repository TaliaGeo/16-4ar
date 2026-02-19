# 🔐 آلية دخول الأدمن للنظام

## 📋 **طرق دخول الأدمن**

### 1. **الإدمن الافتراضي (تلقائي)**

عند تشغيل التطبيق لأول مرة، يتم إنشاء إدمن افتراضي تلقائياً:

```java
// في SecurityInitializationService.java
private void createDefaultAdminUser(Role adminRole) {
    String adminEmail = "admin@example.com";  // 📧 البريد الافتراضي
    String adminPassword = "admin123";        // 🔑 الرقم السري الافتراضي
    
    if (!userRepository.existsByEmail(adminEmail)) {
        User admin = new User();
        admin.setEmail(adminEmail);
        admin.setPassword(passwordEncoder.encode(adminPassword)); // مشفر
        admin.setFullName("System Administrator");
        admin.setActive(true);
        
        // إضافة صلاحية ADMIN
        Set<Role> roles = new HashSet<>();
        roles.add(adminRole);  // ROLE_ADMIN
        admin.setRoles(roles);
        
        userRepository.save(admin);
        log.info("Default admin user created: {} / {}", adminEmail, adminPassword);
    }
}
```

**بيانات الإدمن الافتراضي:**
- 📧 **البريد**: `admin@example.com`
- 🔑 **الرقم السري**: `admin123`
- 👤 **الاسم**: "System Administrator"

### 2. **إنشاء إدمن جديد (عبر API)**

إذا لم يوجد أي إدمن في النظام، يمكن إنشاء إدمن جديد:

```java
// API Endpoint لإنشاء إدمن جديد
POST /api/admin/users/create-admin

// البيانات المطلوبة:
{
    "email": "admin@gharsih.com",
    "password": "Admin@123", 
    "fullName": "مسؤول النظام"
}
```

**شروط الأمان:**
```java
@PreAuthorize("hasRole('ADMIN') or @adminUserService.hasAdminUser() == false")
public ResponseEntity<AdminUserResponse> createAdmin(@Valid @RequestBody CreateAdminRequest request) {
    // يعمل فقط إذا:
    // 1. المستخدم الحالي إدمن، أو
    // 2. لا يوجد أي إدمن في النظام (للإعداد الأولي)
}
```

### 3. **ترقية مستخدم عادي لإدمن**

إدمن موجود يمكنه ترقية أي مستخدم عادي:

```java
// ترقية مستخدم للإدمن
PUT /api/admin/users/{userId}/promote

public void promoteToAdmin(Long userId) {
    User user = userRepository.findById(userId);
    Role adminRole = getAdminRole();  // ROLE_ADMIN
    
    // إضافة صلاحية ADMIN للمستخدم
    user.getRoles().add(adminRole);
    userRepository.save(user);
    
    log.info("User {} promoted to admin successfully", userId);
}
```

---

## 🔐 **كيف يدخل الإدمن؟**

### الطريقة الأولى: **تسجيل دخول عادي**

```java
// نفس آلية المستخدمين العاديين
POST /api/auth/signin
{
    "email": "admin@example.com",
    "password": "admin123"
}

// النظام يعرف أنه إدمن من الـ Roles
Response: {
    "accessToken": "eyJhbGciOiJIUzUxMiJ9...",
    "tokenType": "Bearer",
    "user": {
        "id": 1,
        "email": "admin@example.com", 
        "fullName": "System Administrator",
        "roles": ["ROLE_ADMIN"]  // 🔑 هذا يحدد أنه إدمن
    }
}
```

### الطريقة الثانية: **OAuth2 (جوجل/فيسبوك)**

```java
// إذا كان الإدمن مسجل بـ OAuth2
GET /oauth2/authorize/google
// بعد تسجيل الدخول، النظام يتحقق من الصلاحيات

@Override
public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
    OAuth2User oauth2User = super.loadUser(userRequest);
    
    String email = oauth2User.getAttribute("email");
    User user = userRepository.findByEmail(email);
    
    // إذا كان المستخدم موجود وله صلاحية ADMIN
    if (user != null && user.getRoles().stream().anyMatch(role -> role.getName().equals("ADMIN"))) {
        // يحصل على صلاحيات الإدمن
    }
}
```

---

## 🚨 **أمان النظام**

### الحماية:
```java
// التحقق من وجود إدمن
@GetMapping("/api/admin/users/has-admin")
public ResponseEntity<Map<String, Boolean>> hasAdmin() {
    boolean hasAdmin = adminUserService.hasAdminUser();
    return ResponseEntity.ok(Map.of("hasAdmin", hasAdmin));
}

// منع حذف آخر إدمن
public void demoteFromAdmin(Long userId) {
    long adminCount = countActiveAdmins();
    
    if (adminCount <= 1) {
        throw new BadRequestException("لا يمكن إزالة آخر مسؤول في النظام");
    }
}
```

### الصلاحيات:
```java
// صلاحيات الإدمن في النظام
@PreAuthorize("hasRole('ADMIN')")  // للوصول للوحة التحكم

@PreAuthorize("hasRole('ADMIN') or hasRole('USER')")  // للوصول المشترك

@PreAuthorize("hasAuthority('admin:access')")  // صلاحية محددة
```

---

## 📱 **مثال عملي - دخول الإدمن**

### الخطوات:

1. **التشغيل الأول:**
   ```bash
   mvn spring-boot:run
   # يتم إنشاء إدمن تلقائي: admin@example.com / admin123
   ```

2. **تسجيل الدخول:**
   ```http
   POST http://localhost:8081/api/auth/signin
   Content-Type: application/json

   {
       "email": "admin@example.com",
       "password": "admin123"
   }
   ```

3. **استخدام التوكن:**
   ```http
   GET http://localhost:8081/api/admin/plants
   Authorization: Bearer eyJhbGciOiJIUzUxMiJ9...
   ```

4. **الوصول للوحة الأدمن:**
   ```javascript
   // في التطبيق
   if (user.roles.includes('ROLE_ADMIN')) {
       // عرض وحة التحكم
       showAdminPanel();
   }
   ```

**النتيجة: الإدمن يحصل على وصول كامل لكل APIs الإدارة! 🎯**