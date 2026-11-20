# انتقال نسخه بومی کرامت‌یار به GitHub با ویندوز

## ۱) ابتدا مخزن قبلی را آماده کنید
اگر قبلاً پروژه قبلی را commit/push کرده‌اید، نیازی به حذف مخزن نیست. نسخه جدید را روی فایل‌های قبلی جایگزین کنید. اگر هنوز چیزی push نشده، پوشه مخزن را خالی کنید.

## ۲) ZIP را استخراج کنید
محتویات `karamatyar_native_compose.zip` را Extract All کنید. داخل پوشه باید این موارد را ببینید:
- `app`
- `.github`
- `settings.gradle`
- `build.gradle`
- `gradle.properties`

## ۳) کپی در مخزن محلی
در GitHub Desktop مخزن `karamatyar-android` را Clone کنید. همه فایل‌های داخل پوشه استخراج‌شده را (نه پوشه مادر) داخل پوشه مخزن محلی کپی کنید و جایگزینی فایل‌های هم‌نام را تأیید کنید.

## ۴) Commit و Push
در GitHub Desktop باید تغییرات را ببینید. در Summary بنویسید `Replace prototype with native Compose app`، سپس `Commit to main` و `Push origin` را بزنید.

## ۵) دریافت APK
به مخزن GitHub > Actions بروید. اجرای `Build KaramatYar Native Android APK` را باز کنید. پس از سبز شدن، پایین صفحه در Artifacts فایل `karamatyar-native-apk` را دانلود کنید و ZIP آن را باز کنید تا `app-debug.apk` را بردارید.

## نکته
اگر build قرمز شد، روی اجرای ناموفق بزنید و از بخش خطاها تصویر بفرستید. هیچ رمز، توکن یا کد تأییدی ارسال نکنید.
