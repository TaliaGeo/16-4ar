#!/bin/bash

# OAuth2 Testing Script for Gharsih Backend
# تسكريبت اختبار OAuth2 لباك إند غرسيه

echo "=== OAuth2 Testing Script ==="
echo "نص اختبار OAuth2"
echo ""

BASE_URL="http://localhost:8081"

echo "1. Testing OAuth2 Status..."
echo "اختبار حالة OAuth2..."
curl -s "${BASE_URL}/api/auth/oauth2/status" | jq '.'
echo ""

echo "2. Getting OAuth2 URLs..."
echo "الحصول على روابط OAuth2..."
curl -s "${BASE_URL}/api/auth/oauth2/urls" | jq '.'
echo ""

echo "3. Testing Google OAuth2 Authorization (will show error with test credentials)..."
echo "اختبار Google OAuth2 (سيظهر خطأ مع بيانات الاختبار)..."
curl -i "${BASE_URL}/oauth2/authorize/google"
echo ""

echo "4. Testing Facebook OAuth2 Authorization (will show error with test credentials)..."
echo "اختبار Facebook OAuth2 (سيظهر خطأ مع بيانات الاختبار)..."
curl -i "${BASE_URL}/oauth2/authorize/facebook"
echo ""

echo "=== Test Results ==="
echo "نتائج الاختبار"
echo ""
echo "✅ If you see OAuth2 status as 'configured', the backend is ready"
echo "✅ إذا رأيت حالة OAuth2 كـ 'configured'، فالباك إند جاهز"
echo ""
echo "❌ You will see errors in authorization because we're using test credentials"
echo "❌ ستحصل على أخطاء في التفويض لأننا نستخدم بيانات اختبار"
echo ""
echo "🔧 Next Steps / الخطوات التالية:"
echo "1. Get real Google Client ID & Secret from https://console.developers.google.com/"
echo "2. Get real Facebook App ID & Secret from https://developers.facebook.com/"
echo "3. Update the environment variables in .env file"
echo "4. Restart the application"
echo ""