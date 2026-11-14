---
name: klibs-multiplatform
description: المعيار الرسمي للمشروع — klibs.io MCP + معايير Compose Multiplatform وأندرويد 12+. استخدمه عند إضافة/ترقية/فحص أي اعتمادية، أو مراجعة الأكواد، أو إصلاح حركة، أو التعامل مع الأسرار.
---

# klibs-multiplatform — الإضافة الكاملة (klibs.io MCP + معايير المشروع)

## 1) خادم klibs.io MCP الرسمي

- Endpoint: `https://api.klibs.io/mcp` (Streamable HTTP, no auth)
- التوثيق الرسمي: https://github.com/JetBrains/klibs-io/blob/master/integrations/mcp/README.md
- مسجّل في `~/.config/opencode/opencode.jsonc` وفي `opencode.json` بالمشروع:
  ```json
  { "mcp": { "klibs": { "type": "remote", "url": "https://api.klibs.io/mcp", "enabled": true } } }
  ```
- الأدوات: `tools.klibs.getLatestVersion({groupId, artifactId})` و `tools.klibs.searchProjects({query, targetGroupFilters?, maxPackagesPerProject?})`

## 2) سياسة الإصدارات (تُفحص بتاريخ اليوم)

- المعيار: **latestStableVersion فقط**. ممنوع alpha/beta/rc/dev إلا بطلب صريح.
- `packageFound: false` = الحزمة غير مفهرسة (غالباً Android-only) → تحقق من Google Maven / Maven Central.
- قبل أي ترقية: افحص دعم الأهداف عبر `searchProjects` (المشروع: Android + common + Wasm معلّق).
- الترقيات الكاسرة (major) تُوثّق كتوصية ولا تُطبّق تلقائياً.

الوضع الحالي (تدقيق 2026-11-14):
- Compose MPP 1.12.2 = الأحدث مستقر ✅ (kotlin 2.4.20)
- ktor 2.3.12 (الترقية لـ 3.6.1 توصية: كاسرة + coil-network-ktor2→ktor3)
- material-kolor 4.1.1 (الترقية لـ 5.0.1 توصية: major)
- play-services-ads 23.6.0 (الترقية لـ 25.5.0 توصية: major)
- appfunctions 1.0.0-alpha01 (غير مفهرس + alpha: ثابت)

## 3) معايير أندرويد 12+ والحركة (No Slop — 120 FPS Fluidity)

- **مطاطية أندرويد 12**: على API 31+ النظام يرسم stretch overscroll أصلاً. المطاطية المخصصة (`ElasticOverscroll.kt`) تبقى للأجهزة الأقدم فقط عبر `LocalSystemStretchOverscroll` — ممنوع مضاعفة التأثير (تعارض حركي).
- كل قراءة حالة لكل فريم يجب أن تكون **داخل draw lambda** (graphicsLayer/drawBehind/Canvas) — ممنوع قراءة `.value` في composition.
- ممنوع كتابة snapshot state أثناء composition — عبر `LaunchedEffect`/`snapshotFlow`.
- `Modifier` factories ليست `@Composable` — استخدم `composed {}`.
- الحركات: spring للسائل/القشطة (dampingRatio 0.6–0.78، stiffness 280–360)، tween سريع للأشهر (180–300ms)، وكل الـ animations بعناوين `label`.
- الأجهزة الضعيفة (`LocalIsLowEndDevice`): لا حركات ثقيلة (orb/infinite/shimmer) — ثابت أو مبسّط.
- كل حركة تُتوقف عند الخروج من الشاشة؛ لا infinite transition في عناصر Lazy خارج الشاشة المرئية.
- لا عناصر مخصصة غير معتادة أو "اجتهادات شخصية" — التزم بأنماط Material 3 Expressive والأصلية للمنصة.

## 4) الأسرار (GitHub Secrets فقط)

- ممنوع أي كلمة مرور/رمز في أي ملف مرفوع (build.gradle.kts، كود، مستندات).
- التوقيع: `local.properties` (محلي، gitignored) أو `KEYSTORE_PASSWORD`/`KEY_PASSWORD` كمتغيرات بيئة؛ CI يقرأ `KEYSTORE_BASE64` + كلمتي المرور من GitHub Secrets.
- لا رموز PAT داخل روابط git remote.

## 5) قائمة فحص المراجعة

- [ ] كل الاعتماديات على أحدث مستقر via klibs.io (أو موثّقة كتوصية)
- [ ] كل الشاشات تفحص `isWide`/heightClass للتكيف (تابلت/مطويات/نوافذ صغيرة)
- [ ] لا قراءة حالة في composition، لا كتابة snapshot في composition
- [ ] لا حركات متضاربة مع النظام (overscroll، predictive back)
- [ ] لا أسرار مكشوفة، لا slop، لا أكواد قديمة ميتة
