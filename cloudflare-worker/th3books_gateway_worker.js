/**
 * Cloudflare Worker: th3books-gateway
 * 
 * Features:
 * 1. Telegram Bot File Proxy (/file/:file_id)
 * 2. Edge-Cached News RSS Feed (/news) with Developer Announcement injection
 * 3. Developer Announcement API (/developer-announcement)
 * 4. Scheduled Cron Handler (every 10m) for FCM v1 Push Notifications
 *    According to guide: الوكيل/إشعارات_فايربيس_السحابية.md
 * 5. Dynamic Sliding Marker via Cloudflare KV (TH3BOOKS_KV) - Zero data bloating.
 */

// Retrieve credentials dynamically from Cloudflare Encrypted Secrets (Settings -> Variables and Secrets)
const BOT_TOKEN = typeof globalThis.BOT_TOKEN !== "undefined" && globalThis.BOT_TOKEN ? globalThis.BOT_TOKEN : "";
const RSS_FEED_URL = "https://feeds.feedburner.com/karraraliraqii/gkhbdxas1m0";
const FCM_TOPIC = "news";

// Firebase Service Account Credentials for FCM v1 (Zero-Cost Serverless Push)
const FIREBASE_PROJECT_ID = typeof globalThis.FIREBASE_PROJECT_ID !== "undefined" && globalThis.FIREBASE_PROJECT_ID ? globalThis.FIREBASE_PROJECT_ID : "kariqth3booksexemptioncalc";
const FIREBASE_CLIENT_EMAIL = typeof globalThis.FIREBASE_CLIENT_EMAIL !== "undefined" && globalThis.FIREBASE_CLIENT_EMAIL ? globalThis.FIREBASE_CLIENT_EMAIL : "firebase-adminsdk-fbsvc@kariqth3booksexemptioncalc.iam.gserviceaccount.com";
const FIREBASE_PRIVATE_KEY = typeof globalThis.FIREBASE_PRIVATE_KEY !== "undefined" && globalThis.FIREBASE_PRIVATE_KEY ? globalThis.FIREBASE_PRIVATE_KEY : "";

// KV Namespace binding
const KV = typeof globalThis.TH3BOOKS_KV !== "undefined" && globalThis.TH3BOOKS_KV ? globalThis.TH3BOOKS_KV : null;

// Current Active Developer Announcement
const ACTIVE_ANNOUNCEMENT = null; // Disabled - cleaned from feed
const _DISABLED_ANNOUNCEMENT = {
  id: "dev_broadcast_v214",
  title: "📢 تنبيه من المطور: تفعيل المنظومة السحابية والتكيف الشامل",
  description: "تم تحديث تطبيق كتب وملازم الثالث متوسط ليدعم التكيف مع كافة الشاشات (الهواتف العادية، جالكسي فولد 8، النوافذ المنبثقة، والتابلت) مع مزامنة سحابية سريعة ومجانية بالكامل.",
  link: "https://t.me/bookiraqi",
  pubDate: "Sun, 01 Nov 2026 14:00:00 +0300"
};

// In-memory fallback tracker
let lastNotifiedGuid = "";

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

function extractFirstOriginalImage(itemContent, descRaw) {
  let match = itemContent.match(/<enclosure[^>]*url="([^"]*)"/i);
  if (!match) {
    match = itemContent.match(/<media:content[^>]*url="([^"]*)"/i);
  }
  if (!match && descRaw) {
    const unescapedDesc = descRaw
      .replace(/&quot;/g, '"')
      .replace(/&amp;/g, '&')
      .replace(/&lt;/g, '<')
      .replace(/&gt;/g, '>');
    match = unescapedDesc.match(/<img[^>]+src=["']([^"']+)["']/i);
  }
  if (!match) return "";
  let imgUrl = match[1].trim().replace(/&amp;/g, '&');
  
  // Transform Blogger thumbnail sizes to full-resolution /s1600/
  imgUrl = imgUrl.replace(/\/(s(72-c|160|200|320|400|640|800)|w\d+-h\d+[^/]*)\//i, '/s1600/');
  return imgUrl;
}

/**
 * Dynamic Sliding Watermark in Cloudflare KV:
 * Stores only ONE single dynamic pointer ("dynamic_sliding_marker").
 * With each sync, the previous marker is overwritten by the latest link+pubDate.
 * Zero database accumulation, zero bloat.
 */
async function getLastNotifiedGuid() {
  try {
    if (KV) {
      const val = await KV.get("dynamic_sliding_marker");
      if (val && val.trim()) return val.trim();
    }
  } catch (e) {
    console.error("KV read error:", e);
  }
  return lastNotifiedGuid;
}

async function setLastNotifiedGuid(guid) {
  lastNotifiedGuid = guid;
  try {
    if (KV) {
      await KV.put("dynamic_sliding_marker", guid);
    }
  } catch (e) {
    console.error("KV write error:", e);
  }
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
      version: "2.3.6-cloud",
      hasKV: !!KV,
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
            "User-Agent": "Mozilla/5.0 (compatible; Th3GradeLibraryCloud/2.3)",
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
  if (path === "/send-test-push") {
    const pushResult = await sendFcmNotification({
      title: "وزارة التربية: إعلان نتائج الدور الثالث للصف الثالث متوسط",
      body: "أعلنت وزارة التربية رسمياً قبل قليل نتائج الامتحانات الوزارية لطلبة الثالث متوسط 2026/2027 عبر الرابط المباشر.",
      imageUrl: "https://images.unsplash.com/photo-1546410531-bb4caa6b424d?w=800&auto=format&fit=crop&q=80",
      data: {
        type: "news",
        title: "وزارة التربية: إعلان نتائج الدور الثالث للصف الثالث متوسط",
        link: "https://t.me/bookiraqi",
        click_url: "https://t.me/bookiraqi",
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
 * Checks FeedBurner, detects all fresh items published since last sync, and triggers FCM v1
 */
async function handleScheduledEvent(force = false) {
  try {
    const feedResp = await fetch(RSS_FEED_URL, {
      headers: { "User-Agent": "Th3Books-Edge-Bot/2.3.6 (Android KMP Cloud)" }
    });
    if (!feedResp.ok) return { error: "Failed to fetch RSS feed" };

    const xml = await feedResp.text();

    // Extract all <item> tags
    const itemRegex = /<item>([\s\S]*?)<\/item>/gi;
    const items = [];
    let match;
    while ((match = itemRegex.exec(xml)) !== null) {
      const itemContent = match[1];
      const titleMatch = itemContent.match(/<title>(?:<!\[CDATA\[)?([\s\S]*?)(?:\]\]>)?<\/title>/i);
      const linkMatch = itemContent.match(/<link>(?:<!\[CDATA\[)?([\s\S]*?)(?:\]\]>)?<\/link>/i);
      const pubDateMatch = itemContent.match(/<pubDate>(?:<!\[CDATA\[)?([\s\S]*?)(?:\]\]>)?<\/pubDate>/i);
      const descMatch = itemContent.match(/<description>(?:<!\[CDATA\[)?([\s\S]*?)(?:\]\]>)?<\/description>/i);

      if (titleMatch) {
        const rawTitle = titleMatch[1].replace(/<[^>]*>/g, "").trim();
        const title = cleanHtmlEntities(rawTitle.replace("أخبار الثالث متوسط:", "").replace("أخبار الثالث متوسط", "").trim());
        const link = linkMatch ? linkMatch[1].trim() : "";
        const pubDate = pubDateMatch ? pubDateMatch[1].trim() : "";
        const descRaw = descMatch ? descMatch[1] : "";
        const imageUrl = extractFirstOriginalImage(itemContent, descRaw);
        const snippet = descRaw ? cleanHtmlEntities(descRaw).substring(0, 150) : "";

        const parsedTime = Date.parse(pubDate);
        const epochSec = isNaN(parsedTime) ? Math.floor(Date.now() / 1000) : Math.floor(parsedTime / 1000);
        const marker = `${link}___${epochSec}`;

        items.push({
          title,
          link,
          pubDate,
          imageUrl,
          snippet,
          marker
        });
      }
    }

    if (items.length === 0) return { status: "no_items_found" };

    // Read stored dynamic sliding marker from Cloudflare KV
    const storedTopMarker = await getLastNotifiedGuid();
    const newestMarker = items[0].marker;

    if (!force && storedTopMarker === newestMarker) {
      return { status: "unchanged", marker: newestMarker };
    }

    // Collect ALL new items published since last sync (from newest down to boundary)
    const newItems = [];
    for (const itm of items) {
      if (!force && itm.marker === storedTopMarker) {
        break; // Reached last notified boundary
      }
      newItems.push(itm);
    }

    // Update dynamic sliding marker in Cloudflare KV to the newest item (replaces previous value)
    await setLastNotifiedGuid(newestMarker);

    const pushResults = [];
    // Process items in reverse (oldest new item to newest) so notifications arrive in chronological order
    const itemsToNotify = newItems.reverse();
    for (const itm of itemsToNotify) {
      const res = await sendFcmNotification({
        title: itm.title,
        body: itm.snippet || "",
        imageUrl: itm.imageUrl || "",
        data: {
          type: "news",
          title: itm.title,
          link: itm.link,
          click_url: itm.link,
          pubDate: itm.pubDate,
          content: itm.snippet || "",
          image_url: itm.imageUrl || ""
        }
      });
      pushResults.push(res);
    }

    return {
      status: "notified_batch",
      count: itemsToNotify.length,
      newestMarker,
      pushes: pushResults
    };
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
