# دليل إعداد OAuth2 لـ Google و Facebook

## 1. إعداد Google OAuth2

### الخطوة 1: إنشاء مشروع Google Cloud
1. اذهب إلى [Google Cloud Console](https://console.developers.google.com/)
2. انقر على "Create Project" أو "إنشاء مشروع"
3. أدخل اسم المشروع: "Gharsih App"
4. انقر على "Create"

### الخطوة 2: تفعيل Google+ API
1. في القائمة الجانبية، اذهب إلى "APIs & Services" > "Library"
2. ابحث عن "Google+ API" أو "People API"
3. انقر على "ENABLE"

### الخطوة 3: إنشاء OAuth2 Credentials
1. اذهب إلى "APIs & Services" > "Credentials"
2. انقر على "CREATE CREDENTIALS" > "OAuth client ID"
3. اختر "Web application"
4. أضف هذه البيانات:
   - **Name**: Gharsih Backend
   - **Authorized redirect URIs**: 
     - `http://localhost:8081/oauth2/callback/google`
     - `http://localhost:8081/login/oauth2/code/google`
5. انقر على "Create"
6. **احفظ Client ID و Client Secret**

---

## 2. إعداد Facebook OAuth2

### الخطوة 1: إنشاء تطبيق Facebook
1. اذهب إلى [Facebook Developers](https://developers.facebook.com/)
2. انقر على "My Apps" > "Create App"
3. اختر "Consumer" أو "Business"
4. أدخل اسم التطبيق: "Gharsih App"
5. أدخل بريدك الإلكتروني
6. انقر على "Create App ID"

### الخطوة 2: إعداد Facebook Login
1. في لوحة تحكم التطبيق، انقر على "Add Product"
2. ابحث عن "Facebook Login" وانقر على "Set Up"
3. اختر "Web" platform
4. أدخل Site URL: `http://localhost:8081`

### الخطوة 3: تكوين OAuth Redirect URIs
1. اذهب إلى "Facebook Login" > "Settings"
2. في "Valid OAuth Redirect URIs" أضف:
   - `http://localhost:8081/oauth2/callback/facebook`
   - `http://localhost:8081/login/oauth2/code/facebook`
3. احفظ التغييرات

### الخطوة 4: الحصول على App ID و App Secret
1. اذهب إلى "Settings" > "Basic"
2. **احفظ App ID و App Secret**

---

## 3. إعداد متغيرات البيئة

بعد الحصول على المعرفات، ستحتاج إلى إعداد متغيرات البيئة:

```bash
# Google OAuth2
GOOGLE_CLIENT_ID=your-google-client-id-here
GOOGLE_CLIENT_SECRET=your-google-client-secret-here

# Facebook OAuth2
FACEBOOK_CLIENT_ID=your-facebook-app-id-here
FACEBOOK_CLIENT_SECRET=your-facebook-app-secret-here
```

---

## 4. اختبار الإعداد

بعد إعداد متغيرات البيئة، يمكنك اختبار:
- Google: `http://localhost:8081/oauth2/authorize/google`
- Facebook: `http://localhost:8081/oauth2/authorize/facebook`

---

## الخطوات التالية
1. احصل على المعرفات من Google و Facebook
2. أرسلها لي لأقوم بإعدادها في التطبيق
3. سنختبر تسجيل الدخول معاً