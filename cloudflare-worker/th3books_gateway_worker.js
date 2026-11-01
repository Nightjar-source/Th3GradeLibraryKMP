/**
 * Cloudflare Worker: th3books-gateway
 * 
 * Features:
 * 1. Telegram Bot File Proxy (/file/:file_id)
 * 2. Edge-Cached News RSS Feed (/news) with Developer Announcement injection
 * 3. Developer Announcement API (/developer-announcement)
 * 4. Scheduled Cron Handler for FCM v1 Push Notifications
 */

const BOT_TOKEN = "8862454952:AAGepkN8WMIZVSRWFLAHC24NTSdMZ4SSuFI";
const RSS_FEED_URL = "https://feeds.feedburner.com/karraraliraqii/gkhbdxas1m0";
const FCM_TOPIC = "news";

// Firebase Service Account Credentials for FCM v1
const FIREBASE_PROJECT_ID = "kariqth3booksexemptioncalc";
const FIREBASE_CLIENT_EMAIL = "firebase-adminsdk-fbsvc@kariqth3booksexemptioncalc.iam.gserviceaccount.com";

// Current Active Developer Announcement
const ACTIVE_ANNOUNCEMENT = {
  id: "dev_broadcast_v214",
  title: "📢 تنبيه من المطور: تفعيل المنظومة السحابية والتكيف الشامل",
  description: "تم تحديث تطبيق كتب وملازم الثالث متوسط ليدعم التكيف مع كافة الشاشات (الهواتف العادية، جالكسي فولد 8، النوافذ المنبثقة، والتابلت) مع مزامنة سحابية سريعة ومجانية بالكامل.",
  link: "https://t.me/bookiraqi",
  pubDate: "Sun, 01 Nov 2026 14:00:00 +0300"
};

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
      developerAnnouncement: ACTIVE_ANNOUNCEMENT.title,
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

  return new Response("Not Found", { status: 404 });
}

async function handleScheduledEvent() {
  // Cron event: checks feed and sends notification if needed
  try {
    const resp = await fetch(RSS_FEED_URL);
    if (!resp.ok) return;
    const xml = await resp.text();
    const itemMatch = xml.match(/<item>([\s\S]*?)<\/item>/);
    if (!itemMatch) return;

    const itemContent = itemMatch[1];
    const titleMatch = itemContent.match(/<title><!\[CDATA\[(.*?)\]\]><\/title>/) || itemContent.match(/<title>(.*?)<\/title>/);
    const title = titleMatch ? titleMatch[1].trim() : "خبر دراسي جديد";
    console.log("Cron check complete, latest item:", title);
  } catch (e) {
    console.error("Scheduled check error:", e);
  }
}
