import express from "express";
import path from "path";
import { createServer as createViteServer } from "vite";
import Parser from "rss-parser";

const parser = new Parser({
  customFields: {
    item: [
      ['media:thumbnail', 'mediaThumbnail'],
      ['media:content', 'mediaContent']
    ]
  }
});

async function startServer() {
  const app = express();
  const PORT = 3000;

  // JSON parsing middleware
  app.use(express.json());

  // API routes FIRST
  app.get("/api/health", (req, res) => {
    res.json({ status: "ok" });
  });

  // Proxy the RSS feed to bypass CORS and return JSON
  app.get("/api/news", async (req, res) => {
    try {
      const feed = await parser.parseURL("https://feeds.feedburner.com/karraraliraqii/gkhbdxas1m0");
      
      const newsItems = feed.items.map(item => {
        // Find an image url. Sometimes it's in media:thumbnail, media:content, or enclosures.
        let imageUrl = null;
        if (item.mediaThumbnail && item.mediaThumbnail.$ && item.mediaThumbnail.$.url) {
            imageUrl = item.mediaThumbnail.$.url;
        } else if (item.enclosure && item.enclosure.url && item.enclosure.type && item.enclosure.type.startsWith('image/')) {
            imageUrl = item.enclosure.url;
        } else if (item.mediaContent && item.mediaContent.$ && item.mediaContent.$.url) {
            imageUrl = item.mediaContent.$.url;
        }

        return {
          id: item.guid || item.id || item.link,
          title: item.title,
          link: item.link,
          contentSnippet: item.contentSnippet,
          pubDate: item.pubDate,
          imageUrl: imageUrl
        };
      });

      res.json({ items: newsItems });
    } catch (error) {
      console.error("Error fetching RSS feed:", error);
      res.status(500).json({ error: "Failed to fetch RSS feed" });
    }
  });

  // Vite middleware for development
  if (process.env.NODE_ENV !== "production") {
    const vite = await createViteServer({
      server: { middlewareMode: true },
      appType: "spa",
    });
    app.use(vite.middlewares);
  } else {
    const distPath = path.join(process.cwd(), 'dist');
    app.use(express.static(distPath));
    // Use get * for Express 4, *all for Express 5
    app.get('*', (req, res) => {
      res.sendFile(path.join(distPath, 'index.html'));
    });
  }

  app.listen(PORT, "0.0.0.0", () => {
    console.log(`Server running on http://localhost:${PORT}`);
  });
}

startServer().catch(console.error);
