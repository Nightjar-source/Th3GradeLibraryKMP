import React, { useState, useEffect, useCallback } from 'react';
import { motion, AnimatePresence } from 'motion/react';
import { RefreshCw, Trash2, Settings2, ExternalLink } from 'lucide-react';
import { format } from 'date-fns';
import { ar } from 'date-fns/locale';

import { NewsItem, AppSettings } from '../types';
import { glassCard } from '../styles';
import { cn } from '../utils';

export default function NewsFeed({ settings, onUpdateSettings, onOpenSettings }: { settings: AppSettings, onUpdateSettings: (s: AppSettings)=>void, onOpenSettings: ()=>void }) {
  const [news, setNews] = useState<NewsItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [syncing, setSyncing] = useState(false);
  const [lastSyncTime, setLastSyncTime] = useState<number>(settings.lastSync || 0);

  const loadLocalNews = useCallback(() => {
    try {
      const cached = localStorage.getItem('app_news_cache');
      if (cached) {
        return JSON.parse(cached) as NewsItem[];
      }
    } catch (e) {}
    return [];
  }, []);

  const saveLocalNews = useCallback((items: NewsItem[]) => {
    localStorage.setItem('app_news_cache', JSON.stringify(items));
    setNews(items);
  }, []);

  const fetchNews = useCallback(async (forced = false) => {
    const now = Date.now();
    const timeSinceSync = now - lastSyncTime;
    
    // Determine interval in ms
    const intervalMs = settings.syncInterval === 'on_open' ? 0 : 
                       settings.syncInterval === '30m' ? 30 * 60 * 1000 :
                       settings.syncInterval === '1h' ? 60 * 60 * 1000 :
                       settings.syncInterval === '12h' ? 12 * 60 * 60 * 1000 :
                       24 * 60 * 60 * 1000;
    
    // Check if we actually need to sync
    if (!forced && settings.syncInterval !== 'on_open' && timeSinceSync < intervalMs && news.length > 0) {
      return; 
    }

    setSyncing(true);
    try {
      const response = await fetch('/api/news');
      if (!response.ok) throw new Error("Failed to fetch news");
      const data = await response.json();
      
      let fetchedItems: NewsItem[] = data.items;
      
      // Auto cache deletion logic (Dynamic clear based on user choice)
      if (settings.cacheClearInterval !== 'never') {
        const intervalDays = settings.cacheClearInterval === '7d' ? 7 : 
                             settings.cacheClearInterval === '1m' ? 30 : 
                             settings.cacheClearInterval === '3m' ? 90 : 180;
        const cutoffTime = now - (intervalDays * 24 * 60 * 60 * 1000);
        fetchedItems = fetchedItems.filter(item => {
           const pubTime = new Date(item.pubDate).getTime();
           return pubTime >= cutoffTime; 
        });
      }

      // Check for new items to play sound
      if (settings.notificationSound && fetchedItems.length > news.length) {
         try {
           const audio = new Audio('https://assets.mixkit.co/active_storage/sfx/2869/2869-preview.mp3');
           audio.volume = 0.5;
           await audio.play();
         } catch(e) {}
      }

      saveLocalNews(fetchedItems);
      setLastSyncTime(now);
      localStorage.setItem('app_news_last_sync', now.toString());
      
      // Update global setting
      onUpdateSettings({ ...settings, lastSync: now });

    } catch (error) {
      console.error("Error syncing news:", error);
    } finally {
      setSyncing(false);
      setLoading(false);
    }
  }, [lastSyncTime, news.length, settings, saveLocalNews, onUpdateSettings]);

  useEffect(() => {
    const local = loadLocalNews();
    if (local.length > 0) {
      setNews(local);
      setLoading(false);
    }
    const lastTime = parseInt(localStorage.getItem('app_news_last_sync') || '0', 10);
    setLastSyncTime(lastTime);
    
    // Auto-fetch on mount (if required by logic or if 'on_open' triggered it due to old lastSync)
    if(settings.syncInterval === 'on_open' || (lastTime === 0 && local.length === 0)){
       fetchNews(true);
    } else {
       fetchNews();
    }

    // Setup background logic 
    const interval = setInterval(() => {
      fetchNews();
    }, 60000); 

    return () => clearInterval(interval);
  }, [fetchNews, loadLocalNews, settings.syncInterval]);

  const deleteNewsItem = (id: string, e: React.MouseEvent) => {
    e.stopPropagation();
    const updated = news.filter(n => n.id !== id);
    saveLocalNews(updated);
  };

  const openNews = (link: string) => {
    window.open(link, '_blank');
  };

  return (
    <>
      <header className="mb-6 mt-2 px-2 flex justify-between items-start">
        <div>
          <h1 className="font-reem text-3xl font-black text-slate-800 dark:text-white flex items-center gap-3">
            آخر الأخبار
            <button 
              onClick={onOpenSettings} 
              className="p-2 bg-white/50 dark:bg-slate-800/50 rounded-full hover:bg-slate-200 dark:hover:bg-slate-700 transition border border-slate-200 shadow-sm dark:border-slate-700 active:scale-95"
            >
              <Settings2 size={20} className="text-teal-600 dark:text-teal-400"/>
            </button>
          </h1>
          <div className="flex items-center gap-2 mt-2">
             <span className="text-slate-500 dark:text-slate-400 text-[11px] font-medium bg-slate-200/50 dark:bg-slate-800 rounded-full px-3 py-1 border border-slate-300 dark:border-slate-700">
               تحديثات ومزامنة مستمرة
             </span>
          </div>
        </div>
        <div className="flex flex-col items-end gap-2">
           <button 
             onClick={() => fetchNews(true)}
             disabled={syncing}
             className={cn("px-4 py-2 rounded-[1.25rem] backdrop-blur-md transition-all active:scale-95 flex items-center gap-2 font-bold text-xs shadow-md border", syncing ? "bg-indigo-100 text-indigo-400 border-indigo-200" : "bg-indigo-600 text-white hover:bg-indigo-500 border-indigo-500")}
           >
             <RefreshCw size={16} className={cn(syncing && "animate-spin")} />
             <span className="font-reem">جلب حالاً</span>
           </button>
           {lastSyncTime > 0 && (
             <span className="text-[10px] text-slate-400 font-medium font-mono bg-slate-100 dark:bg-slate-800/50 px-2 py-0.5 rounded-full border border-slate-200 dark:border-slate-700/50">
               {format(lastSyncTime, 'hh:mm a', { locale: ar })}
             </span>
           )}
        </div>
      </header>

      {loading ? (
         <div className="flex justify-center p-10"><RefreshCw className="animate-spin text-indigo-500" size={32} /></div>
      ) : news.length > 0 ? (
        <div className="flex flex-col gap-5">
          <AnimatePresence>
            {news.map((n, idx) => (
              <motion.div 
                key={n.id} initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0, scale: 0.9 }} transition={{ delay: idx * 0.05 }}
                className={cn("p-3 flex flex-col gap-3 cursor-pointer relative", glassCard)}
                onClick={() => openNews(n.link)}
              >
                {n.imageUrl && (
                  <img src={n.imageUrl} className="w-full h-auto max-h-[60vh] object-contain bg-slate-100/50 dark:bg-slate-800/50 rounded-[1.5rem] shadow-sm" alt="news" loading="lazy" />
                )}
                <div className="p-2 pt-0 pb-1 flex flex-col min-h-full">
                  <div className="flex justify-between items-start mb-2">
                    <span className="text-[10px] font-bold text-indigo-500 bg-indigo-50 dark:bg-indigo-500/10 px-2 py-1 rounded-lg border border-indigo-100 dark:border-indigo-500/20 font-mono">
                      {format(new Date(n.pubDate), 'dd MMM', { locale: ar })}
                    </span>
                    <button onClick={(e) => deleteNewsItem(n.id, e)} className="text-slate-400 hover:text-red-500 transition-colors bg-white/50 dark:bg-slate-800/50 p-1.5 rounded-full z-10 active:scale-90 border border-slate-200 dark:border-slate-700">
                      <Trash2 size={16} />
                    </button>
                  </div>
                  <h3 className="font-reem text-lg font-bold text-slate-800 dark:text-white leading-snug">{n.title}</h3>
                  {n.contentSnippet && (
                    <p className="text-sm text-slate-500 dark:text-slate-400 mt-2 line-clamp-2 leading-relaxed font-medium">{n.contentSnippet}</p>
                  )}
                  <div className="flex items-center gap-1.5 mt-4 text-indigo-500 text-xs font-bold bg-indigo-50 dark:bg-indigo-500/10 w-fit px-3 py-1.5 rounded-xl border border-indigo-100 dark:border-indigo-500/20 shadow-sm font-reem">
                    <ExternalLink size={14} /> عرض في المتصفح الأصل
                  </div>
                </div>
              </motion.div>
            ))}
          </AnimatePresence>
        </div>
      ) : (
        <div className="py-20 flex flex-col items-center justify-center text-slate-400 dark:text-slate-600 text-center bg-slate-100/50 dark:bg-slate-800/50 rounded-[2.5rem] border border-slate-200 dark:border-slate-800 shadow-inner">
            <Settings2 size={48} className="mb-4 opacity-20" />
            <p className="font-bold text-lg font-reem">لا توجد أخبار حالياً</p>
            <p className="text-sm text-slate-500 mt-2 max-w-[200px] leading-relaxed">اضغط على زر (جلب حالاً) لاكتشاف آخر الإشعارات</p>
        </div>
      )}
    </>
  );
}
