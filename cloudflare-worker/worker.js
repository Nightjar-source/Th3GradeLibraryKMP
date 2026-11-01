/**
 * Cloudflare Worker: Th3GradeLibrary Cloud Gateway & News Sync Engine
 * 
 * Features:
 * 1. Fast Edge News Cache (/news) -> Proxies FeedBurner RSS with Edge Caching & CORS
 * 2. Telegram File Proxy (/file/:file_id) -> Streams large PDF downloads smoothly
 * 3. Automated Background Cron Sync -> Checks for fresh news and sends Firebase FCM v1 push notifications
 * 4. Zero Cost, 100% Free Tier (Cloudflare Workers + Firebase FCM)
 */

// Configuration constants
const RSS_FEED_URL = "https://feeds.feedburner.com/karraraliraqii/gkhbdxas1m0";
const FCM_TOPIC = "news";

export default {
  // 1. HTTP Request Handler
  async fetch(request, env, ctx) {
    const url = new URL(request.url);

    // Root status endpoint
    if (url.pathname === "/" || url.pathname === "") {
      return new Response(JSON.stringify({
        status: "ok",
        service: "th3books-gateway",
        version: "2.1.4-cloud",
        timestamp: new Date().toISOString()
      }), {
        headers: { "Content-Type": "application/json", "Access-Control-Allow-Origin": "*" }
      });
    }

    // High-speed News RSS endpoint with Edge Caching
    if (url.pathname === "/news") {
      const cacheUrl = new URL(request.url);
      const cacheKey = new Request(cacheUrl.toString(), request);
      const cache = caches.default;

      // Check Cloudflare Edge Cache first
      let response = await cache.match(cacheKey);
      if (!response) {
        try {
          const fetchResp = await fetch(RSS_FEED_URL, {
            headers: {
              "User-Agent": "Th3GradeLibraryKMP-EdgeWorker/2.1",
              "Accept": "application/rss+xml, application/xml, text/xml, */*"
            }
          });

          if (!fetchResp.ok) {
            return new Response(`Upstream feed error: ${fetchResp.status}`, { status: fetchResp.status });
          }

          const rssText = await fetchResp.text();

          // Edge cache for 5 minutes, client cache for 2 minutes
          response = new Response(rssText, {
            status: 200,
            headers: {
              "Content-Type": "application/xml; charset=utf-8",
              "Access-Control-Allow-Origin": "*",
              "Access-Control-Allow-Methods": "GET, HEAD, OPTIONS",
              "Cache-Control": "public, max-age=120, s-maxage=300, stale-while-revalidate=600"
            }
          });

          // Asynchronously write to Cloudflare cache
          ctx.waitUntil(cache.put(cacheKey, response.clone()));
        } catch (err) {
          return new Response(`Error fetching news: ${err.message}`, { status: 502 });
        }
      }

      return response;
    }

    // Telegram Bot File Proxy (Streaming download)
    if (url.pathname.startsWith("/file/")) {
      const fileId = url.pathname.replace("/file/", "");
      if (!fileId) {
        return new Response("Missing file_id", { status: 400 });
      }

      const botToken = env.TELEGRAM_BOT_TOKEN;
      if (!botToken) {
        return new Response("Telegram Bot Token not configured in Cloudflare Secrets", { status: 500 });
      }

      try {
        // Step A: Get File Path from Telegram API
        const getFileUrl = `https://api.telegram.org/bot${botToken}/getFile?file_id=${fileId}`;
        const fileInfoResp = await fetch(getFileUrl);
        const fileInfo = await fileInfoResp.json();

        if (!fileInfo.ok || !fileInfo.result?.file_path) {
          return new Response("File not found on Telegram servers", { status: 404 });
        }

        // Step B: Stream the file from Telegram to the client
        const downloadUrl = `https://api.telegram.org/file/bot${botToken}/${fileInfo.result.file_path}`;
        const downloadResp = await fetch(downloadUrl);

        const headers = new Headers(downloadResp.headers);
        headers.set("Access-Control-Allow-Origin", "*");
        headers.set("Content-Disposition", `inline; filename="${fileId}.pdf"`);

        return new Response(downloadResp.body, {
          status: downloadResp.status,
          headers: headers
        });
      } catch (err) {
        return new Response(`Proxy download error: ${err.message}`, { status: 502 });
      }
    }

    // Trigger News Check & Push Notification manually via POST /sync-news
    if (url.pathname === "/sync-news" && request.method === "POST") {
      const authHeader = request.headers.get("Authorization");
      if (env.CRON_SECRET && authHeader !== `Bearer ${env.CRON_SECRET}`) {
        return new Response("Unauthorized", { status: 401 });
      }

      const result = await checkForNewsAndNotify(env);
      return new Response(JSON.stringify(result), {
        headers: { "Content-Type": "application/json" }
      });
    }

    return new Response("Route not found", { status: 404 });
  },

  // 2. Scheduled Cron Trigger Handler (Automated Background Sync)
  async scheduled(event, env, ctx) {
    ctx.waitUntil(checkForNewsAndNotify(env));
  }
};

/**
 * Checks FeedBurner for new items.
 * Uses Cloudflare KV (NEWS_KV) to remember the latest item GUID.
 * Sends FCM v1 push notification if a new item is detected.
 */
async function checkForNewsAndNotify(env) {
  try {
    const resp = await fetch(RSS_FEED_URL);
    if (!resp.ok) return { error: `Feed fetch status ${resp.status}` };

    const xml = await resp.text();
    const itemMatch = xml.match(/<item>([\s\S]*?)<\/item>/);
    if (!itemMatch) return { message: "No items in feed" };

    const itemContent = itemMatch[1];
    const titleMatch = itemContent.match(/<title><!\[CDATA\[(.*?)\]\]><\/title>/) || itemContent.match(/<title>(.*?)<\/title>/);
    const guidMatch = itemContent.match(/<guid[^>]*>(.*?)<\/guid>/) || itemContent.match(/<link>(.*?)<\/link>/);

    const latestTitle = titleMatch ? titleMatch[1].trim() : "خبر دراسي جديد";
    const latestGuid = guidMatch ? guidMatch[1].trim() : "";

    if (!latestGuid) return { message: "Could not extract GUID" };

    // Check against KV stored GUID
    let lastSeenGuid = "";
    if (env.NEWS_KV) {
      lastSeenGuid = await env.NEWS_KV.get("last_news_guid") || "";
    }

    if (lastSeenGuid === latestGuid) {
      return { status: "unchanged", guid: latestGuid };
    }

    // A new item was detected! Send FCM notification
    const pushResult = await sendFirebaseNotification(env, {
      title: "أخبار الثالث متوسط",
      body: latestTitle,
      topic: FCM_TOPIC
    });

    // Save the new GUID to KV
    if (env.NEWS_KV) {
      await env.NEWS_KV.put("last_news_guid", latestGuid);
    }

    return { status: "notified", title: latestTitle, guid: latestGuid, push: pushResult };
  } catch (err) {
    return { error: err.message };
  }
}

/**
 * Sends a notification using Google Firebase Cloud Messaging HTTP v1 API.
 * Uses native Web Crypto to sign the Google Service Account JWT without any dependencies.
 */
async function sendFirebaseNotification(env, { title, body, topic }) {
  if (!env.FIREBASE_PROJECT_ID || !env.FIREBASE_CLIENT_EMAIL || !env.FIREBASE_PRIVATE_KEY) {
    return { skipped: "Firebase secrets not configured in Cloudflare environment" };
  }

  try {
    const accessToken = await getGoogleOAuthToken(
      env.FIREBASE_CLIENT_EMAIL,
      env.FIREBASE_PRIVATE_KEY,
      "https://www.googleapis.com/auth/firebase.messaging"
    );

    const fcmEndpoint = `https://fcm.googleapis.com/v1/projects/${env.FIREBASE_PROJECT_ID}/messages:send`;
    const messagePayload = {
      message: {
        topic: topic,
        notification: {
          title: title,
          body: body
        },
        android: {
          priority: "HIGH",
          notification: {
            sound: "custom_sound",
            channel_id: "news_channel_sound",
            default_sound: false,
            notification_priority: "PRIORITY_MAX"
          }
        },
        data: {
          click_action: "OPEN_NEWS"
        }
      }
    };

    const resp = await fetch(fcmEndpoint, {
      method: "POST",
      headers: {
        "Authorization": `Bearer ${accessToken}`,
        "Content-Type": "application/json"
      },
      body: JSON.stringify(messagePayload)
    });

    const respJson = await resp.json();
    return { ok: resp.ok, response: respJson };
  } catch (err) {
    return { ok: false, error: err.message };
  }
}

/**
 * Native WebCrypto Google Service Account JWT signer
 */
async function getGoogleOAuthToken(clientEmail, privateKeyPem, scope) {
  const now = Math.floor(Date.now() / 1000);
  const header = { alg: "RS256", typ: "JWT" };
  const payload = {
    iss: clientEmail,
    scope: scope,
    aud: "https://oauth2.googleapis.com/token",
    exp: now + 3600,
    iat: now
  };

  const base64UrlEncode = (obj) => {
    const str = typeof obj === "string" ? obj : JSON.stringify(obj);
    const utf8Bytes = new TextEncoder().encode(str);
    let binary = "";
    for (let i = 0; i < utf8Bytes.length; i++) binary += String.fromCharCode(utf8Bytes[i]);
    return btoa(binary).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
  };

  const encodedHeader = base64UrlEncode(header);
  const encodedPayload = base64UrlEncode(payload);
  const signInput = `${encodedHeader}.${encodedPayload}`;

  // Clean PEM key
  const pemContents = privateKeyPem
    .replace(/-----BEGIN PRIVATE KEY-----/g, "")
    .replace(/-----END PRIVATE KEY-----/g, "")
    .replace(/\\n/g, "")
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
  const encodedSignature = btoa(binarySig).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");

  const assertion = `${signInput}.${encodedSignature}`;

  const tokenResp = await fetch("https://oauth2.googleapis.com/token", {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: `grant_type=urn:ietf:params:oauth:grant-type:jwt-bearer&assertion=${assertion}`
  });

  const tokenData = await tokenResp.json();
  return tokenData.access_token;
}
