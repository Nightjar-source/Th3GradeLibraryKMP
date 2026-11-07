/**
 * Cloudflare Worker: th3books-gateway
 * 
 * Features:
 * 1. Telegram Bot File Proxy (/file/:file_id)
 * 2. Edge-Cached News RSS Feed (/news) with Developer Announcement injection
 * 3. Developer Announcement API (/developer-announcement)
 * 4. Scheduled Cron Handler (every 10m) for FCM v1 Push Notifications
 *    According to guide: الوكيل/إشعارات_فايربيس_السحابية.md
 */

const BOT_TOKEN = "8862454952:AAGepkN8WMIZVSRWFLAHC24NTSdMZ4SSuFI";
const RSS_FEED_URL = "https://feeds.feedburner.com/karraraliraqii/gkhbdxas1m0";
const FCM_TOPIC = "news";

// Firebase Service Account Credentials for FCM v1 (Zero-Cost Serverless Push)
const FIREBASE_PROJECT_ID = "kariqth3booksexemptioncalc";
const FIREBASE_CLIENT_EMAIL = "firebase-adminsdk-fbsvc@kariqth3booksexemptioncalc.iam.gserviceaccount.com";
const FIREBASE_PRIVATE_KEY = `-----BEGIN PRIVATE KEY-----
MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQDjXkkgggcse1Q0
YfRVUt/dBIfunDGRd6Ipo1P1vvD6zWoXbw2mItwZDSUkAOk29O8GFJQghwEim7S4
FfLyhOYK5UUbMWRdAjN2+UDoBefwf+QMo6rsrnpHacEN+lTW+pDp4Y1SXKxeoDSA
i4jtyPpV9xh9CKaR1SUgrNgV2yHMAIcg38caBYTBRH1fiVYnP810+Is6oZqC+brj
iTnRc8QN2OdQBkxRt7ORPOKLZstTMvIRkJnPNWBZ1VmP9FC3trxeSSFxtfmd+MwK
i+WNERSM4SeBDmimASQGwCaVjTd7DekHbrfEHExDV+zqKmUq0FqcZpsIBKFERS+Q
xw33HUyfAgMBAAECggEAL9eBD6QMmmGBAFy0Ktf7Xug7KgwXqgJATv6dBhKdZkie
0Dt/5btkEKlhbUf7yqKTQPZHjOibZorY5ftIWZt5REWl2ujzs1ZpOdmMvMWxXjuq
4rX1m0PDWw93nddUzr9+ipf5QqFZa7fOFFZIA7DMbBZIQtb1Ja5x14RHG6I2ZFQ6
jd4Ll512PHsJn7MxMxWjR+Njtp5fBaZHH84Yuu68yesRIBefz+rZm3sGsZEf09JU
DG0J4l4oo2DwWmAKz+CckXFNoH3X6j6+NCSoj03UcaBMEsuS4D8K/64BjMPI/6KD
mr6Kxdy6XEhGpewhS4HgN1dYjuAqe4xrnbYrjr/5oQKBgQD1/9ksYWeSDWstwFjt
gQn855mPoCfszEp3S9v72fLByMoqKnTJ8jlvv+90/m3y1AynTR2p+OO0JNuuUeIC
vqUHA+GOcKi6ZKERfxePodKh76z6wAkiX7QTSRpndmqJJ7H9qB2PzkssKC0OCBPJ
tzmqgscPXGxJ6FZK0wyAea1q5wKBgQDsnIpriY5IW9S+ByIigjegjddcP5ViC682
aQ7Xsjt4/nqwUOId4ATwMsotDDbcfS3LXbHTl/FOUWLlSWzpsNP+KSy+Oa6sarYQ
QfYfkE8ygOXzcdUWh+iJK/lpOrvzgarLTqTHMjvnBWX/s3DOePzVR7Un+f8pXMU2
wVWKPiRRiQKBgQCWjoWuIdcvjkLgd251vZoNTPmw64ZD6SxSIv16AIQQkgEZGW7t
AfChMkktw2IrUhiEdDAJjShfYoeJAthecauyxW9QrCa4HUpu2Ofkole1gGjP9qQg
g45El2Mvm0vMsZy/8BpXnxxm5BffFJZ3yEnkz7Fg92HXQfk+RjjKkK3QVQKBgQCx
j4P80Vx6BpfvBvFhzjCqhucPrhOPYU9aWJqv2WvyD57Ig9o8Kynpi2PXTGw7KKFe
FPDm5gZLDJ5XjLNz56Qe/o06L5VahDYemzF1XFligAKQn2MWpmUl0182zcHQKgWe
zmSvq43yf0WdDu7fs0GhWE+lMWF/i8PfpFRqCto8aQKBgEbYzHP1K7YK+KxdBiSb
d1n/N/GKFBCCLbZT7WWKc5K51MnnqtPwE7iUmZb1L9DHOvH2g0P1IHI/TBRyvcYj
UtiAzJI25ICeQgSOQU+eJB8hpjwFcHGcz0M1dDA0uIRItCwPBAhOPNKvYAGGHxvZ
dgujgf4i2gHuo3IpvRg3ux4X
-----END PRIVATE KEY-----
`;

// Current Active Developer Announcement
const ACTIVE_ANNOUNCEMENT = null; // Disabled - cleaned from feed
const _DISABLED_ANNOUNCEMENT = {
  id: "dev_broadcast_v214",
  title: "📢 تنبيه من المطور: تفعيل المنظومة السحابية والتكيف الشامل",
  description: "تم تحديث تطبيق كتب وملازم الثالث متوسط ليدعم التكيف مع كافة الشاشات (الهواتف العادية، جالكسي فولد 8، النوافذ المنبثقة، والتابلت) مع مزامنة سحابية سريعة ومجانية بالكامل.",
  link: "https://t.me/bookiraqi",
  pubDate: "Sun, 01 Nov 2026 14:00:00 +0300"
};

// In-memory & Edge Cache tracker for last sent notification GUID
let lastNotifiedGuid = "";
const CLOUDFLARE_ACCOUNT_ID = "582d8bda218c7b16eff91fa9ffeddeef";
const CLOUDFLARE_KV_ID = "85e9ce7f9e0d49b7b3b628b20ea6fc3f";
// Token injected via worker secret binding
const CLOUDFLARE_API_TOKEN = typeof CF_API_TOKEN !== "undefined" ? CF_API_TOKEN : "";

function cleanHtmlEntities(str) {
  if (!str) return "";
  return str
    .replace(/&quot;/g, '"')
    .replace(/&apos;/g, "'")
    .replace(/&#39;/g, "'")
    .replace(/&amp;/g, '&')
    .replace(/&lt;/g, '<')
    .replace(/&gt;/g, '>')
    .replace(/&nbsp;/g, ' ')
    .replace(/&#160;/g, ' ')
    .replace(/&#8220;/g, '"')
    .replace(/&#8221;/g, '"')
    .replace(/&#8216;/g, "'")
    .replace(/&#8217;/g, "'")
    .replace(/&#8211;/g, '-')
    .replace(/&#8212;/g, '—')
    .replace(/<[^>]*>/g, '')
    .trim();
}


async function getFirestoreLastGuid(token) {
  try {
    const url = `https://firestore.googleapis.com/v1/projects/${FIREBASE_PROJECT_ID}/databases/(default)/documents/app_state/news_sync`;
    const resp = await fetch(url, {
      headers: { "Authorization": `Bearer ${token}` }
    });
    if (resp.ok) {
      const data = await resp.json();
      return data.fields?.last_notified_guid?.stringValue || "";
    }
  } catch (e) {
    console.error("Firestore read error:", e);
  }
  return "";
}

async function setFirestoreLastGuid(token, guid) {
  try {
    const url = `https://firestore.googleapis.com/v1/projects/${FIREBASE_PROJECT_ID}/databases/(default)/documents/app_state/news_sync?updateMask.fieldPaths=last_notified_guid`;
    await fetch(url, {
      method: "PATCH",
      headers: {
        "Authorization": `Bearer ${token}`,
        "Content-Type": "application/json"
      },
      body: JSON.stringify({
        fields: {
          last_notified_guid: { stringValue: guid }
        }
      })
    });
  } catch (e) {
    console.error("Firestore write error:", e);
  }
}

async function getLastNotifiedGuid() {
  // 1. Native Cloudflare KV Binding if bound
  try {
    if (typeof TH3BOOKS_KV !== "undefined") {
      const val = await TH3BOOKS_KV.get("last_notified_guid");
      if (val) return val;
    }
  } catch (e) {}

  // 2. Cloudflare KV REST API via Token
  try {
    const kvUrl = `https://api.cloudflare.com/client/v4/accounts/${CLOUDFLARE_ACCOUNT_ID}/storage/kv/namespaces/${CLOUDFLARE_KV_ID}/values/last_notified_guid`;
    const resp = await fetch(kvUrl, {
      headers: { "Authorization": `Bearer ${CLOUDFLARE_API_TOKEN}` }
    });
    if (resp.ok) {
      const val = await resp.text();
      if (val && !val.includes("error")) return val;
    }
  } catch (e) {}

  // 3. Fallback to Cloudflare Edge Cache
  try {
    const cache = caches.default;
    const cacheKey = new Request("https://th3books-gateway.karrar1iq.workers.dev/internal/last-notified-guid");
    const response = await cache.match(cacheKey);
    if (response) {
      return (await response.text()).trim();
    }
  } catch (e) {}

  return lastNotifiedGuid;
}

async function setLastNotifiedGuid(guid) {
  lastNotifiedGuid = guid;

  // 1. Native Cloudflare KV Binding
  try {
    if (typeof TH3BOOKS_KV !== "undefined") {
      await TH3BOOKS_KV.put("last_notified_guid", guid);
    }
  } catch (e) {}

  // 2. Cloudflare KV REST API via Token
  try {
    const kvUrl = `https://api.cloudflare.com/client/v4/accounts/${CLOUDFLARE_ACCOUNT_ID}/storage/kv/namespaces/${CLOUDFLARE_KV_ID}/values/last_notified_guid`;
    await fetch(kvUrl, {
      method: "PUT",
      headers: { "Authorization": `Bearer ${CLOUDFLARE_API_TOKEN}` },
      body: guid
    });
  } catch (e) {}

  // 3. Cloudflare Edge Cache
  try {
    const cache = caches.default;
    const cacheKey = new Request("https://th3books-gateway.karrar1iq.workers.dev/internal/last-notified-guid");
    const response = new Response(guid, {
      headers: { "Cache-Control": "max-age=2592000" } // 30 days
    });
    await cache.put(cacheKey, response);
  } catch (e) {}
}

addEventListener('fetch', event => {
  event.respondWith(handleRequest(event.request, event));
});

addEventListener('scheduled', event => {
  event.waitUntil(handleScheduledEvent());
});

async function handleRequest(request, event) {
  const url = new URL(request.url);

  // CORS preflight
  if (request.method === "OPTIONS") {
    return new Response(null, {
      headers: {
        "Access-Control-Allow-Origin": "*",
        "Access-Control-Allow-Methods": "GET, HEAD, POST, OPTIONS",
        "Access-Control-Allow-Headers": "*"
      }
    });
  }

  const path = url.pathname;

  // 1. Root / Health Status
  if (path === "/" || path === "/health") {
    return new Response(JSON.stringify({
      status: "ok",
      service: "th3books-gateway",
      version: "2.1.4-cloud",
      developerAnnouncement: ACTIVE_ANNOUNCEMENT?.title || null,
      timestamp: new Date().toISOString()
    }), {
      headers: {
        "Content-Type": "application/json; charset=utf-8",
        "Access-Control-Allow-Origin": "*"
      }
    });
  }

  // 2. Telegram File Proxy
  if (path.startsWith("/file/")) {
    const fileId = path.replace("/file/", "");
    if (!fileId) return new Response("Missing file_id", { status: 400 });

    try {
      const tgFileUrl = `https://api.telegram.org/bot${BOT_TOKEN}/getFile?file_id=${encodeURIComponent(fileId)}`;
      const getFileResp = await fetch(tgFileUrl);
      const fileInfo = await getFileResp.json();

      if (!fileInfo.ok || !fileInfo.result || !fileInfo.result.file_path) {
        return new Response(JSON.stringify(fileInfo), {
          status: 404,
          headers: { "Content-Type": "application/json", "Access-Control-Allow-Origin": "*" }
        });
      }

      const directDownloadUrl = `https://api.telegram.org/file/bot${BOT_TOKEN}/${fileInfo.result.file_path}`;
      const fileResp = await fetch(directDownloadUrl, { headers: request.headers });

      const responseHeaders = new Headers(fileResp.headers);
      responseHeaders.set("Access-Control-Allow-Origin", "*");
      responseHeaders.set("Cache-Control", "public, max-age=604800, immutable");

      return new Response(fileResp.body, {
        status: fileResp.status,
        statusText: fileResp.statusText,
        headers: responseHeaders
      });
    } catch (err) {
      return new Response("File proxy error: " + err.message, { status: 502 });
    }
  }

  // 3. Ultra-Fast Cached News Feed with Developer Announcement Injection
  if (path === "/news") {
    const cacheUrl = new URL(request.url);
    const cacheKey = new Request(cacheUrl.toString(), request);
    const cache = caches.default;

    let response = await cache.match(cacheKey);
    if (!response) {
      try {
        const fetchResp = await fetch(RSS_FEED_URL, {
          headers: {
            "User-Agent": "Mozilla/5.0 (compatible; Th3GradeLibraryCloud/2.1)",
            "Accept": "application/rss+xml, application/xml, text/xml, */*"
          }
        });

        if (!fetchResp.ok) {
          return new Response(`Upstream feed error: ${fetchResp.status}`, { status: fetchResp.status });
        }

        let rssXml = await fetchResp.text();

        // Inject developer announcement as top item if present
        if (ACTIVE_ANNOUNCEMENT && ACTIVE_ANNOUNCEMENT.id) {
          const announcementItem = `
    <item>
      <title><![CDATA[${ACTIVE_ANNOUNCEMENT.title}]]></title>
      <link>${ACTIVE_ANNOUNCEMENT.link}</link>
      <guid isPermaLink="false">${ACTIVE_ANNOUNCEMENT.id}</guid>
      <pubDate>${ACTIVE_ANNOUNCEMENT.pubDate}</pubDate>
      <description><![CDATA[${ACTIVE_ANNOUNCEMENT.description}]]></description>
    </item>`;
          rssXml = rssXml.replace("<channel>", `<channel>${announcementItem}`);
        }

        response = new Response(rssXml, {
          status: 200,
          headers: {
            "Content-Type": "application/xml; charset=utf-8",
            "Access-Control-Allow-Origin": "*",
            "Cache-Control": "public, max-age=120, s-maxage=300, stale-while-revalidate=600"
          }
        });

        event.waitUntil(cache.put(cacheKey, response.clone()));
      } catch (err) {
        return new Response("Feed error: " + err.message, { status: 502 });
      }
    }
    return response;
  }

  // 4. Developer Announcement JSON Endpoint
  if (path === "/developer-announcement") {
    return new Response(JSON.stringify(ACTIVE_ANNOUNCEMENT), {
      headers: {
        "Content-Type": "application/json; charset=utf-8",
        "Access-Control-Allow-Origin": "*"
      }
    });
  }

    // 5. Test Trigger FCM Endpoint (POST /trigger-push)
  if (path === "/send-test-push" || (path === "/trigger-push" && request.method === "POST")) {
    const pushResult = await sendFcmNotification({
      title: "وزارة التربية: إعلان نتائج الدور الثالث للصف الثالث متوسط",
      body: "أعلنت وزارة التربية رسمياً قبل قليل نتائج الامتحانات الوزارية لطلبة الثالث متوسط 2026/2027 عبر الرابط المباشر.",
      imageUrl: "https://images.unsplash.com/photo-1546410531-bb4caa6b424d?w=800&auto=format&fit=crop&q=80",
      data: {
        title: "وزارة التربية: إعلان نتائج الدور الثالث للصف الثالث متوسط",
        link: "https://t.me/bookiraqi",
        image_url: "https://images.unsplash.com/photo-1546410531-bb4caa6b424d?w=800&auto=format&fit=crop&q=80",
        pubDate: new Date().toUTCString(),
        content: "أعلنت وزارة التربية رسمياً قبل قليل نتائج الامتحانات الوزارية لطلبة الثالث متوسط 2026/2027 عبر الرابط المباشر.",
        is_test: "true"
      }
    });
    return new Response(JSON.stringify({ status: "sent", result: pushResult }), {
      headers: { "Content-Type": "application/json", "Access-Control-Allow-Origin": "*" }
    });
  }
  if (path === "/trigger-push" && request.method === "POST") {
    const result = await handleScheduledEvent(true);
    return new Response(JSON.stringify(result), {
      headers: { "Content-Type": "application/json; charset=utf-8" }
    });
  }

  return new Response("Not Found", { status: 404 });
}

/**
 * Scheduled Cron Handler (runs every 10 minutes)
 * Checks FeedBurner, detects fresh items, and triggers FCM v1
 */
async function handleScheduledEvent(force = false) {
  try {
    const feedResp = await fetch(RSS_FEED_URL, {
      headers: { "User-Agent": "Th3Books-Edge-Bot/2.2.4 (Android KMP Cloud)" }
    });
    if (!feedResp.ok) return { error: "Failed to fetch RSS feed" };

    const xml = await feedResp.text();

    const titleMatch = xml.match(/<item>[\s\S]*?<title>(?:<!\[CDATA\[)?([\s\S]*?)(?:\]\]>)?<\/title>/i);
    const linkMatch = xml.match(/<item>[\s\S]*?<link>(?:<!\[CDATA\[)?([\s\S]*?)(?:\]\]>)?<\/link>/i);
    const pubDateMatch = xml.match(/<item>[\s\S]*?<pubDate>(?:<!\[CDATA\[)?([\s\S]*?)(?:\]\]>)?<\/pubDate>/i);
    const descMatch = xml.match(/<item>[\s\S]*?<description>(?:<!\[CDATA\[)?([\s\S]*?)(?:\]\]>)?<\/description>/i);
    const imgMatch = xml.match(/<item>[\s\S]*?<enclosure[^>]*url="([^"]*)"/i) ||
                     xml.match(/<item>[\s\S]*?<media:content[^>]*url="([^"]*)"/i) ||
                     (descMatch && descMatch[1].match(/src="([^"]*)"/i));

    if (!titleMatch) return { status: "no_items_found" };

    const rawTitle = titleMatch[1].replace(/<[^>]*>/g, "").trim();
    const title = cleanHtmlEntities(rawTitle.replace("أخبار الثالث متوسط:", "").replace("أخبار الثالث متوسط", "").trim());
    const link = linkMatch ? linkMatch[1].trim() : "";
    const pubDate = pubDateMatch ? pubDateMatch[1].trim() : "";
    const imageUrl = imgMatch ? imgMatch[1].trim() : "";
    const snippet = descMatch ? cleanHtmlEntities(descMatch[1]).substring(0, 150) : "";

    // Parse timestamp to exact epoch seconds for strict temporal boundary verification
    const parsedTime = Date.parse(pubDate);
    const epochSec = isNaN(parsedTime) ? Math.floor(Date.now() / 1000) : Math.floor(parsedTime / 1000);
    const currentTopMarker = `${link}___${epochSec}`;

    // Read stored boundary watermark from Cloudflare KV
    const storedTopMarker = await getLastNotifiedGuid();

    if (!force && storedTopMarker === currentTopMarker) {
      return { status: "unchanged", marker: currentTopMarker };
    }

    // Update boundary watermark in Cloudflare KV
    await setLastNotifiedGuid(currentTopMarker);

    // Differentiate between Web News (Title + Link + Image) vs In-App Administrative Announcement
    const hasWebLink = link.startsWith("http://") || link.startsWith("https://");
    const hasImage = imageUrl.length > 0;
    const isWebArticle = hasWebLink && hasImage && !link.includes("dev_") && !link.includes("admin_");

    // Send FCM v1 pure Data-Only push
    const pushResult = await sendFcmNotification({
      title: title,
      body: "", // Keep body empty so headline is solely displayed
      imageUrl: imageUrl || "",
      data: {
        title: title,
        link: link,
        pubDate: pubDate,
        content: snippet || "",
        image_url: imageUrl || "",
        is_news_article: isWebArticle ? "true" : "false",
        is_announcement: isWebArticle ? "false" : "true"
      }
    });

    return { status: "notified", title: title, push: pushResult, marker: currentTopMarker };
  } catch (e) {
    console.error("Scheduled check error:", e);
    return { error: e.message };
  }
}

/**
 * Sends FCM v1 notification using Google Service Account JWT signing (Web Crypto)
 */
async function sendFcmNotification({ title, body, imageUrl, data }) {
  try {
    const token = await getGoogleOAuthToken();
    const endpoint = `https://fcm.googleapis.com/v1/projects/${FIREBASE_PROJECT_ID}/messages:send`;

    const fcmData = Object.assign({}, data || {}, {
      title: title || "",
      body: body || "",
      content: body || "",
      image_url: imageUrl || (data && data.image_url) || ""
    });

    // Pure Data-Only FCM message:
    // Ensures zero battery drain, zero automatic system wake-up lag,
    // and lets Android client strictly enforce the user's sync schedule.
    const message = {
      message: {
        topic: FCM_TOPIC,
        data: fcmData,
        android: {
          priority: "HIGH"
        }
      }
    };

    const resp = await fetch(endpoint, {
      method: "POST",
      headers: {
        "Authorization": `Bearer ${token}`,
        "Content-Type": "application/json"
      },
      body: JSON.stringify(message)
    });

    return await resp.json();
  } catch (err) {
    return { error: err.message };
  }
}

async function getGoogleOAuthToken() {
  const now = Math.floor(Date.now() / 1000);
  const header = { alg: "RS256", typ: "JWT" };
  const payload = {
    iss: FIREBASE_CLIENT_EMAIL,
    scope: "https://www.googleapis.com/auth/firebase.messaging",
    aud: "https://oauth2.googleapis.com/token",
    exp: now + 3600,
    iat: now
  };

  const b64Url = (obj) => {
    const str = typeof obj === "string" ? obj : JSON.stringify(obj);
    const utf8Bytes = new TextEncoder().encode(str);
    let binary = "";
    for (let i = 0; i < utf8Bytes.length; i++) binary += String.fromCharCode(utf8Bytes[i]);
    return btoa(binary).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
  };

  const encodedHeader = b64Url(header);
  const encodedPayload = b64Url(payload);
  const signInput = `${encodedHeader}.${encodedPayload}`;

  const pemContents = FIREBASE_PRIVATE_KEY
    .replace(/-----BEGIN PRIVATE KEY-----/g, "")
    .replace(/-----END PRIVATE KEY-----/g, "")
    .replace(/\\n/g, "")
    .replace(/\n/g, "")
    .replace(/\s+/g, "");

  const binaryDer = Uint8Array.from(atob(pemContents), c => c.charCodeAt(0));

  const cryptoKey = await crypto.subtle.importKey(
    "pkcs8",
    binaryDer.buffer,
    { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" },
    false,
    ["sign"]
  );

  const signature = await crypto.subtle.sign(
    "RSASSA-PKCS1-v1_5",
    cryptoKey,
    new TextEncoder().encode(signInput)
  );

  let binarySig = "";
  const sigBytes = new Uint8Array(signature);
  for (let i = 0; i < sigBytes.length; i++) binarySig += String.fromCharCode(sigBytes[i]);
  const encodedSig = btoa(binarySig).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");

  const assertion = `${signInput}.${encodedSig}`;

  const tokenResp = await fetch("https://oauth2.googleapis.com/token", {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: `grant_type=urn:ietf:params:oauth:grant-type:jwt-bearer&assertion=${assertion}`
  });

  const tokenData = await tokenResp.json();
  return tokenData.access_token;
}
