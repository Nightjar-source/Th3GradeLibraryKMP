import React, { useState, useEffect, useRef, useCallback } from 'react';
import { motion, AnimatePresence } from 'motion/react';
import { 
  Book, FileText, Newspaper, Search, X, 
  LayoutGrid, StretchHorizontal, ChevronRight,
  ArrowLeftRight, ArrowUpDown, Menu, Maximize,
  Home, Info, User, MessageCircle, Star, Bookmark, Settings,
  PlusSquare, AppWindow, Mic, Bot
} from 'lucide-react';

import { MOCK_DATA, cn } from '../utils';
import { glassPill, glassCard, liquidOrbParent, liquidOrbLight, liquidOrbDark } from '../styles';
import { BookItem, NewsItem, AppSettings } from '../types';

// Mock components
import NewsFeed from './NewsFeed';
import AppSettingsModal from './AppSettingsModal';
import NewsSettingsModal from './NewsSettingsModal';

export default function AppUI() {
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState<'home' | 'list' | 'pdf' | 'about' | 'news' | 'bookmarks' | 'shortcuts'>('home');
  const [category, setCategory] = useState<'books' | 'notes' | null>(null); 
  const [viewMode, setViewMode] = useState<'grid' | 'list'>('grid');
  const [selectedItem, setSelectedItem] = useState<BookItem | null>(null);
  
  const [isSearchOpen, setIsSearchOpen] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');
  const searchInputRef = useRef<HTMLInputElement>(null);

  const [isDrawerOpen, setIsDrawerOpen] = useState(false);
  const [isSettingsOpen, setIsSettingsOpen] = useState(false);
  const [isNewsSettingsOpen, setIsNewsSettingsOpen] = useState(false);
  const [savedItems, setSavedItems] = useState<BookItem[]>([]);
  const [toastMessage, setToastMessage] = useState<string | null>(null);

  // Default app settings
  const [settings, setSettings] = useState<AppSettings>(() => {
    try {
      const saved = localStorage.getItem('app_settings');
      if (saved) return JSON.parse(saved);
    } catch (e) {}
    return {
      theme: 'system',
      useMaterialYou: true,
      primaryColor: '#6366f1',
      syncInterval: '30m',
      cacheClearInterval: '3m',
      notificationSound: true,
      lastSync: 0
    };
  });

  const [isDark, setIsDark] = useState(false);

  // Apply Theme
  useEffect(() => {
    localStorage.setItem('app_settings', JSON.stringify(settings));
    
    if (settings.theme === 'system') {
      const mediaQuery = window.matchMedia('(prefers-color-scheme: dark)');
      setIsDark(mediaQuery.matches);
      const handler = (e: MediaQueryListEvent) => setIsDark(e.matches);
      mediaQuery.addEventListener('change', handler);
      return () => mediaQuery.removeEventListener('change', handler);
    } else {
      setIsDark(settings.theme === 'dark');
    }
  }, [settings]);

  useEffect(() => {
    if (isDark) document.documentElement.classList.add('dark');
    else document.documentElement.classList.remove('dark');
  }, [isDark]);

  useEffect(() => {
    const timer = setTimeout(() => setLoading(false), 1200);
    return () => clearTimeout(timer);
  }, []);

  useEffect(() => {
    if (isSearchOpen && searchInputRef.current) searchInputRef.current.focus();
    else setSearchQuery('');
  }, [isSearchOpen]);

  const navigate = (targetPage: any, targetCategory: any = null, item: any = null) => {
    window.scrollTo({ top: 0, behavior: 'smooth' });
    if (targetCategory) setCategory(targetCategory);
    if (item) setSelectedItem(item);
    setPage(targetPage);
    setIsDrawerOpen(false);
    setIsSearchOpen(false);
  };

  const toggleSave = (item: BookItem) => {
    if (savedItems.find(i => i.id === item.id)) {
      setSavedItems(savedItems.filter(i => i.id !== item.id));
    } else {
      setSavedItems([...savedItems, item]);
    }
  };

  if (loading) {
    return (
      <div className="bg-m3-surface dark:bg-m3-surface-dark h-screen w-full flex flex-col items-center justify-center transition-colors duration-500">
        <motion.div 
          initial={{ scale: 0.8, opacity: 0 }} 
          animate={{ scale: [0.8, 1.1, 1], opacity: 1 }} 
          transition={{ duration: 0.6, ease: "easeOut" }}
          className="flex flex-col items-center gap-6"
        >
          <div className="w-20 h-20 bg-gradient-to-tr from-indigo-500 to-blue-400 rounded-[2rem] shadow-2xl shadow-indigo-500/50 flex items-center justify-center animate-pulse">
            <Book size={40} className="text-white" />
          </div>
          <h1 className="font-reem text-3xl font-bold text-slate-800 dark:text-white tracking-wide">مكتبة الثالث متوسط</h1>
        </motion.div>
      </div>
    );
  }

  const showBottomNav = !['pdf'].includes(page);
  // @ts-ignore
  const filteredData = category && MOCK_DATA[category] ? MOCK_DATA[category].filter((item:any) => item.title.includes(searchQuery)) : [];

  return (
    <div 
      className="min-h-screen text-slate-800 dark:text-slate-100 font-sans selection:bg-indigo-500/30 bg-m3-surface dark:bg-m3-surface-dark transition-colors duration-300 pb-28 relative"
      style={!settings.useMaterialYou ? { '--color-m3-surface': `${settings.primaryColor}10`, '--color-m3-surface-dark': `${settings.primaryColor}1a` } as any : {}}
    >
      
      {/* Top Floating App Bar */}
      <nav className={cn(
        "fixed top-4 left-4 right-4 z-[60] flex items-center justify-between px-3 py-3 transition-all duration-300 rounded-full",
        glassPill,
        page === 'pdf' ? '-translate-y-24 opacity-0 pointer-events-none' : 'translate-y-0 opacity-100'
      )}>
        <div className="flex items-center gap-2">
          {page !== 'home' ? (
            <LiquidButton onClick={() => page === 'list' ? navigate('home') : navigate('home')}>
              <ChevronRight size={22} className="text-slate-700 dark:text-slate-200" />
            </LiquidButton>
          ) : (
            <div className="w-10 h-10 bg-gradient-to-tr from-indigo-600 to-blue-500 rounded-full flex items-center justify-center shadow-lg shadow-indigo-500/30 mx-1">
              <Book size={18} className="text-white" />
            </div>
          )}
          <span className="font-reem font-bold text-lg tracking-wide mt-1 line-clamp-1 text-slate-800 dark:text-white">
            {page === 'news' ? 'آخر الأخبار' : 'مكتبة الثالث متوسط'}
          </span>
        </div>
        
        <div className="flex items-center gap-1">
          {page === 'list' && (
            <>
              <LiquidButton onClick={() => setViewMode(viewMode === 'grid' ? 'list' : 'grid')}>
                {viewMode === 'grid' ? <StretchHorizontal size={18} /> : <LayoutGrid size={18} />}
              </LiquidButton>
              <LiquidButton isActive={isSearchOpen} onClick={() => setIsSearchOpen(!isSearchOpen)}>
                {isSearchOpen ? <X size={18} /> : <Search size={18} />}
              </LiquidButton>
            </>
          )}
        </div>
      </nav>

      <AnimatePresence>
        {isSearchOpen && page === 'list' && (
          <motion.div 
            initial={{ opacity: 0, y: -20, scale: 0.95 }} animate={{ opacity: 1, y: 0, scale: 1 }} exit={{ opacity: 0, y: -10, scale: 0.95 }}
            transition={{ type: "spring", stiffness: 300, damping: 25 }}
            className="fixed top-24 left-4 right-4 z-[55]"
          >
            <div className="bg-white/80 dark:bg-slate-800/80 backdrop-blur-2xl border border-white/50 dark:border-white/10 shadow-xl shadow-indigo-900/10 dark:shadow-black/40 rounded-[2rem] p-2 flex items-center gap-3">
              <Search size={20} className="text-slate-400 mr-3" />
              <input 
                ref={searchInputRef} type="text" placeholder="ابحث، أو اسأل المساعد الذكي..." value={searchQuery} onChange={(e) => setSearchQuery(e.target.value)}
                className="flex-1 bg-transparent outline-none text-slate-700 dark:text-slate-200 placeholder-slate-400 font-medium font-reem"
              />
              <button 
                 onClick={(e) => { e.stopPropagation(); setToastMessage("جاري الاستماع للبحث الصوتي..."); setTimeout(()=>setToastMessage(null), 2000) }}
                 className="p-2 text-slate-500 hover:text-indigo-500 hover:bg-indigo-50 dark:hover:bg-indigo-500/20 rounded-full transition-colors active:scale-95"
              >
                 <Mic size={18} />
              </button>
              {searchQuery && (
                <LiquidButton onClick={() => setSearchQuery('')} small><X size={14} /></LiquidButton>
              )}
            </div>
          </motion.div>
        )}
      </AnimatePresence>

      <main className={cn("px-4 max-w-2xl mx-auto transition-all duration-300", page === 'list' && isSearchOpen ? 'pt-40' : 'pt-28', 'pb-[40vh]')}>
        <AnimatePresence mode="wait">
          {page === 'home' && (
            <motion.div key="home" initial={{ opacity: 0, y: 10 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0, y: -10 }} className="space-y-6">
              <header className="mb-8 mt-2 px-2">
                <h1 className="font-reem text-4xl font-black text-slate-800 dark:text-white leading-tight">
                  دراستك أسهل<br/>
                  <span className="text-transparent bg-clip-text bg-gradient-to-l from-indigo-500 to-blue-400">في مكان واحد.</span>
                </h1>
              </header>

              <div className="flex flex-col gap-4">
                <MenuCard title="الملازم الدراسية" desc="أفضل الملخصات لأساتذة العراق" icon={<FileText size={40} />} color="from-teal-400 to-emerald-600" onClick={() => navigate('list', 'notes')} />
                <MenuCard title="الكتب الرسمية" desc="المنهج الوزاري المعتمد 2026" icon={<Book size={40} />} color="from-indigo-500 to-blue-600" onClick={() => navigate('list', 'books')} />
              </div>
            </motion.div>
          )}

          {page === 'list' && (
            <motion.div key="list" initial={{ opacity: 0, x: -20 }} animate={{ opacity: 1, x: 0 }} exit={{ opacity: 0, x: 20 }} className={viewMode === 'grid' ? "grid grid-cols-2 gap-4 pb-10" : "flex flex-col gap-4 pb-10"}>
              <div className="col-span-full mb-2">
                <h2 className="font-reem text-xl font-bold dark:text-white px-2 text-slate-700">
                  {category === 'books' ? 'الكتب الرسمية' : 'الملازم الدراسية'}
                </h2>
              </div>
              {filteredData.length > 0 ? filteredData.map((item: any, idx: number) => (
                <ContentCard key={item.id} item={item} viewMode={viewMode} delay={idx * 0.05} onClick={() => navigate('pdf', category, item)} />
              )) : (
                <div className="col-span-2 py-20 flex flex-col items-center justify-center text-slate-400 dark:text-slate-600">
                   <Search size={48} className="mb-4 opacity-50" />
                   <p className="font-bold">لم يتم العثور على نتائج</p>
                </div>
              )}
            </motion.div>
          )}

          {page === 'bookmarks' && (
            <motion.div key="bookmarks" initial={{ opacity: 0, y: 10 }} animate={{ opacity: 1, y: 0 }} className="space-y-4 pb-10">
              <header className="mb-6 mt-2 px-2 flex items-center gap-3">
                <div className="p-3 bg-amber-100 dark:bg-amber-900/30 text-amber-500 rounded-full">
                  <Star size={24} className="fill-amber-500" />
                </div>
                <h1 className="font-reem text-3xl font-black text-slate-800 dark:text-white">المحفوظات</h1>
              </header>
              {savedItems.length > 0 ? (
                <div className="flex flex-col gap-4">
                  {savedItems.map(item => (
                     <ContentCard key={item.id} item={item} viewMode="list" onClick={() => navigate('pdf', null, item)} />
                  ))}
                </div>
              ) : (
                <div className="py-20 flex flex-col items-center justify-center text-slate-400 dark:text-slate-600 text-center">
                  <Bookmark size={64} className="mb-4 opacity-20" />
                  <p className="font-bold text-lg font-reem">لا توجد محفوظات</p>
                  <p className="text-sm mt-2">اضغط على النجمة داخل أي كتاب لحفظه هنا.</p>
                </div>
              )}
            </motion.div>
          )}

          {page === 'news' && (
            <motion.div key="news" initial={{ opacity: 0, y: 10 }} animate={{ opacity: 1, y: 0 }} className="space-y-6 pb-10">
              <NewsFeed settings={settings} onUpdateSettings={setSettings} onOpenSettings={() => setIsNewsSettingsOpen(true)} />
            </motion.div>
          )}

          {page === 'shortcuts' && (
            <motion.div key="shortcuts" initial={{ opacity: 0, x: -20 }} animate={{ opacity: 1, x: 0 }} exit={{ opacity: 0, x: 20 }} className="space-y-4 pb-10">
              <header className="mb-6 mt-2 px-2 flex items-center gap-3">
                <div className="p-3 bg-fuchsia-100 dark:bg-fuchsia-900/30 text-fuchsia-500 rounded-full">
                  <PlusSquare size={24} className="fill-fuchsia-500/20" />
                </div>
                <div>
                  <h1 className="font-reem text-3xl font-black text-slate-800 dark:text-white">إضافة اختصار</h1>
                  <p className="text-slate-500 dark:text-slate-400 text-xs mt-1">اصنع اختصاراً سريعاً لملزمتك أو كتابك على شاشة هاتفك</p>
                </div>
              </header>

              <div className="grid grid-cols-2 gap-4">
                {MOCK_DATA.books.concat(MOCK_DATA.notes).map((item, idx) => (
                  <motion.div 
                     key={item.id}
                     whileTap={{ scale: 0.95 }}
                     onClick={() => {
                        setToastMessage(`تم إضافة "${item.title}" إلى الشاشة الرئيسية`);
                        setTimeout(() => setToastMessage(null), 3000);
                     }}
                     className={cn("flex flex-col p-3 rounded-[2rem] border border-white/50 dark:border-white/5 cursor-pointer shadow-sm relative overflow-hidden group", glassCard)}
                  >
                     <div className="relative w-full aspect-[4/5] rounded-2xl overflow-hidden bg-slate-100 dark:bg-slate-800">
                        <img src={item.cover} className="w-full h-full object-cover transform group-hover:scale-110 transition-transform duration-700" />
                        <div className="absolute inset-0 bg-black/40 flex items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity">
                           <PlusSquare size={32} className="text-white" />
                        </div>
                     </div>
                     <span className="font-reem font-bold mt-3 text-slate-800 dark:text-slate-100 text-[15px] leading-tight text-center line-clamp-2">{item.title}</span>
                  </motion.div>
                ))}
              </div>
            </motion.div>
          )}

          {page === 'about' && (
            <motion.div key="about" initial={{ opacity: 0, scale: 0.95 }} animate={{ opacity: 1, scale: 1 }} className="space-y-6 pb-10">
               <div className={cn("rounded-[2.5rem] p-8 text-center", glassCard)}>
                 <div className="w-20 h-20 bg-gradient-to-tr from-indigo-500 to-blue-400 rounded-full mx-auto flex items-center justify-center mb-6 shadow-lg shadow-indigo-500/30 border border-white/20">
                    <Book size={32} className="text-white" />
                 </div>
                 <h2 className="font-reem text-2xl font-bold text-slate-800 dark:text-white mb-4">رسالة التطبيق</h2>
                 <p className="text-slate-600 dark:text-slate-300 leading-relaxed font-medium">
                   تم تطوير هذا التطبيق لطلاب وطالبات الصف الثالث متوسط، ليكون الرفيق الدائم في رحلتهم الدراسية وتسهيل الوصول للمنهج.
                 </p>
               </div>

               <div className="bg-gradient-to-br from-slate-900 to-slate-800 dark:from-slate-800 dark:to-slate-950 rounded-[2.5rem] p-8 text-white relative overflow-hidden shadow-2xl shadow-slate-900/30">
                  <div className="absolute top-0 right-0 w-48 h-48 bg-indigo-500/20 blur-3xl rounded-full -mr-10 -mt-10" />
                  
                  <div className="flex items-center gap-4 mb-8 relative z-10">
                    <div className="w-16 h-16 bg-white/10 rounded-full backdrop-blur-md flex items-center justify-center border border-white/20 shadow-inner">
                       <User size={32} className="text-indigo-300" />
                    </div>
                    <div>
                      <p className="text-indigo-300/80 text-sm font-bold tracking-wider mb-1">المطور</p>
                      <h3 className="font-reem text-3xl font-black">عبودي</h3>
                    </div>
                  </div>

                  <div className="bg-black/20 backdrop-blur-md rounded-3xl p-5 border border-white/5 relative z-10">
                     <p className="text-slate-300 text-sm mb-4 leading-relaxed">
                       للاتصال بنا، أو للإبلاغ عن خطأ راسلنا على تليغرام:
                     </p>
                     <motion.a 
                       whileTap={{ scale: 0.95 }}
                       href="https://t.me/iraqitrb" target="_blank" rel="noopener noreferrer"
                       className="flex items-center justify-center gap-3 w-full bg-indigo-500 hover:bg-indigo-400 text-white font-bold py-3.5 px-6 rounded-2xl shadow-lg shadow-indigo-500/30 transition-colors"
                     >
                        <MessageCircle size={20} />
                        <span className="font-reem text-lg">مراسلة عبر تليغرام</span>
                     </motion.a>
                  </div>
               </div>
               
            </motion.div>
          )}
        </AnimatePresence>
      </main>

      <AnimatePresence>
        {page === 'pdf' && selectedItem && (
           <PDFViewer item={selectedItem} isSaved={!!savedItems.find(i => i.id === selectedItem?.id)} onToggleSave={() => toggleSave(selectedItem)} onClose={() => navigate('list', category)} />
        )}
      </AnimatePresence>

      {/* Bottom Nav */}
      <AnimatePresence>
        {showBottomNav && (
          <motion.div 
            initial={{ y: 100, opacity: 0 }} animate={{ y: 0, opacity: 1 }} exit={{ y: 100, opacity: 0 }}
            transition={{ type: "spring", damping: 25, stiffness: 200 }}
            className="fixed bottom-6 left-4 right-4 z-[50] pointer-events-none"
          >
            <nav className={cn("max-w-md mx-auto pointer-events-auto p-2 flex justify-between items-center rounded-full", glassPill)}>
              <NavItem label="المكتبة" icon={<Home size={22} />} active={page === 'home'} onClick={() => navigate('home')} />
              <NavItem label="الأخبار" icon={<Newspaper size={22} />} active={page === 'news'} onClick={() => navigate('news')} />
              <NavItem label="المحفوظات" icon={<Star size={22} />} active={page === 'bookmarks'} onClick={() => navigate('bookmarks')} />
              <NavItem label="القائمة" icon={<Menu size={22} />} active={isDrawerOpen} onClick={() => setIsDrawerOpen(true)} />
            </nav>
          </motion.div>
        )}
      </AnimatePresence>

      {/* RTL Drawer Menu */}
      <AnimatePresence>
        {isDrawerOpen && (
          <>
            <motion.div 
              initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}
              onClick={() => setIsDrawerOpen(false)}
              className="fixed inset-0 bg-slate-900/40 dark:bg-black/60 backdrop-blur-sm z-[70]"
            />
            <motion.div 
              initial={{ x: '100%' }} animate={{ x: 0 }} exit={{ x: '100%' }}
              transition={{ type: 'spring', damping: 25, stiffness: 200 }}
              className="fixed top-0 right-0 bottom-0 w-[80%] max-w-sm bg-white/90 dark:bg-slate-900/90 backdrop-blur-3xl z-[80] shadow-2xl flex flex-col rounded-l-[2.5rem] border-l border-white/20 dark:border-white/10 overflow-hidden"
            >
              <div className="bg-gradient-to-br from-indigo-600 to-blue-500 p-8 pt-16 text-white relative">
                 <div className="absolute top-0 right-0 w-32 h-32 bg-white/10 rounded-full blur-2xl -mt-10 -mr-10" />
                 <div className="w-16 h-16 bg-white/20 backdrop-blur-md rounded-[1.5rem] flex items-center justify-center mb-4 border border-white/20 shadow-lg relative z-10">
                    <Book size={32} className="text-white" />
                 </div>
                 <h2 className="font-reem text-2xl font-bold relative z-10">مكتبة الثالث متوسط</h2>
              </div>

              <div className="flex-1 p-4 flex flex-col gap-2 overflow-y-auto mt-2 custom-scrollbar">
                <DrawerItem icon={<Home size={22} />} label="المكتبة" active={page === 'home'} onClick={() => navigate('home')} />
                <DrawerItem icon={<Star size={22} />} label="المحفوظات" active={page === 'bookmarks'} onClick={() => navigate('bookmarks')} />
                <DrawerItem icon={<AppWindow size={22} />} label="إضافة اختصار" active={page === 'shortcuts'} onClick={() => navigate('shortcuts')} />
                <DrawerItem icon={<Settings size={22} />} label="الإعدادات المظهرية" active={isSettingsOpen} onClick={() => {setIsSettingsOpen(true); setIsDrawerOpen(false);}} />
                <DrawerItem icon={<Info size={22} />} label="حول التطبيق" active={page === 'about'} onClick={() => navigate('about')} />
              </div>
            </motion.div>
          </>
        )}
      </AnimatePresence>

      <AppSettingsModal isOpen={isSettingsOpen} onClose={() => setIsSettingsOpen(false)} settings={settings} onUpdateSettings={setSettings} />
      <NewsSettingsModal isOpen={isNewsSettingsOpen} onClose={() => setIsNewsSettingsOpen(false)} settings={settings} onUpdateSettings={setSettings} />

      {/* Global Toast Message */}
      <AnimatePresence>
        {toastMessage && (
           <motion.div 
             initial={{ opacity: 0, y: 50, scale: 0.9 }} 
             animate={{ opacity: 1, y: 0, scale: 1 }} 
             exit={{ opacity: 0, scale: 0.9, y: 20 }}
             className="fixed bottom-28 left-1/2 -translate-x-1/2 z-[250] bg-slate-800 dark:bg-white text-white dark:text-slate-900 px-6 py-3.5 rounded-[1.5rem] shadow-xl flex items-center gap-3 w-max max-w-[90vw] border border-white/10 dark:border-slate-800/10"
           >
             <PlusSquare size={20} className="text-teal-400 dark:text-teal-600" />
             <span className="font-reem text-sm font-bold truncate">{toastMessage}</span>
           </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}

// Subcomponents

function LiquidButton({ children, onClick, isActive, small }: any) {
  return (
    <motion.button 
      whileTap={{ scale: 0.85 }} onClick={onClick} 
      className={cn("rounded-full flex items-center justify-center transition-colors border border-transparent", 
        small ? 'p-1.5' : 'p-2.5',
        isActive ? 'bg-indigo-100 dark:bg-indigo-500/20 text-indigo-600 dark:text-indigo-400 border-indigo-200/50 dark:border-indigo-500/30' : 'bg-transparent hover:bg-slate-100 dark:hover:bg-slate-800 text-slate-600 dark:text-slate-300'
      )}
    >
      {children}
    </motion.button>
  );
}

function MenuCard({ title, desc, icon, color, onClick }: any) {
  return (
    <motion.button 
      whileTap={{ scale: 0.96 }} onClick={onClick} 
      className={cn("w-full h-[220px] rounded-[2.5rem] bg-gradient-to-br p-8 text-white flex flex-col justify-between shadow-xl shadow-slate-300/50 dark:shadow-black/20 text-right border border-white/10", color, liquidOrbParent)}
    >
      <div className={liquidOrbLight} />
      <div className={liquidOrbDark} />
      <div className="relative z-10 flex justify-between items-start w-full">
        <div className="bg-white/20 p-4 rounded-[1.5rem] backdrop-blur-xl shadow-inner border border-white/20">{icon}</div>
      </div>
      <div className="relative z-10 mt-auto">
        <h3 className="font-reem text-3xl font-bold mb-2 drop-shadow-sm">{title}</h3>
        <p className="text-white/80 text-sm font-medium drop-shadow-sm">{desc}</p>
      </div>
    </motion.button>
  );
}

function ContentCard({ item, viewMode, delay, onClick }: any) {
  return (
    <motion.div 
      initial={{ opacity: 0, y: 15 }} animate={{ opacity: 1, y: 0 }} transition={{ delay, type: "spring", stiffness: 300, damping: 25 }}
      whileTap={{ scale: 0.95 }} onClick={onClick} 
      className={cn("rounded-[2rem] overflow-hidden cursor-pointer group", glassCard, viewMode === 'list' ? 'flex items-center p-3 gap-4 h-32' : 'flex flex-col p-3')}
    >
      <div className={cn("relative overflow-hidden bg-slate-100 dark:bg-slate-800", viewMode === 'list' ? 'w-24 h-full rounded-2xl' : 'w-full aspect-[4/5] rounded-2xl')}>
        <img src={item.cover} alt={item.title} className="w-full h-full object-cover transform group-hover:scale-110 transition-transform duration-700 ease-out" loading="lazy" />
        <div className="absolute inset-0 bg-black/20 opacity-0 group-hover:opacity-100 transition-opacity duration-300 flex items-center justify-center">
           <Maximize className="text-white drop-shadow-md" size={24} />
        </div>
      </div>
      <div className={cn("flex flex-col justify-center gap-1.5", viewMode === 'list' ? 'flex-1' : 'pt-3 pb-1 px-1')}>
        <h4 className="font-reem font-bold text-slate-800 dark:text-slate-100 text-[15px] leading-tight line-clamp-2">{item.title}</h4>
        <p className="text-[11px] text-slate-500 dark:text-slate-400 font-medium">{item.author}</p>
        <div className="mt-auto flex items-center gap-2 pt-2">
           <div className={cn("w-2.5 h-2.5 rounded-full bg-gradient-to-tr", item.color)} />
        </div>
      </div>
    </motion.div>
  );
}

function NavItem({ label, icon, active, onClick }: any) {
  return (
    <motion.button 
      whileTap={{ scale: 0.85 }} onClick={onClick} 
      className="relative flex flex-col items-center justify-center py-2 px-4 transition-all duration-300 rounded-full group flex-1"
    >
      {active && <motion.div layoutId="nav-indicator" className="absolute inset-0 bg-indigo-50 dark:bg-indigo-500/10 rounded-full" />}
      <div className={cn("relative z-10 transition-colors duration-300", active ? 'text-indigo-600 dark:text-indigo-400' : 'text-slate-400 dark:text-slate-500 group-hover:text-slate-600 dark:group-hover:text-slate-300')}>{icon}</div>
      <span className={cn("relative z-10 text-[10px] mt-1 font-bold font-reem transition-colors duration-300", active ? 'text-indigo-600 dark:text-indigo-400' : 'text-slate-400 dark:text-slate-500')}>{label}</span>
    </motion.button>
  );
}

function DrawerItem({ icon, label, active, onClick }: any) {
  return (
    <motion.button 
      whileTap={{ scale: 0.95 }} onClick={onClick}
      className={cn("flex items-center gap-4 w-full p-4 rounded-[1.5rem] transition-all duration-200", active ? 'bg-indigo-50 dark:bg-indigo-500/10 text-indigo-600 dark:text-indigo-400 font-bold' : 'text-slate-600 dark:text-slate-300 hover:bg-slate-50 dark:hover:bg-slate-800 font-medium')}
    >
      <div className={active ? 'text-indigo-600 dark:text-indigo-400' : 'text-slate-400 dark:text-slate-500'}>{icon}</div>
      <span className="font-reem text-lg pt-1">{label}</span>
    </motion.button>
  );
}

// Advanced PDF Simulator with Framer Motion Drag and constraints (pull to edge effect)
function PDFViewer({ item, isSaved, onToggleSave, onClose }: any) {
  const [orientation, setOrientation] = useState('vertical');
  
  useEffect(() => {
    document.body.style.overflow = 'hidden';
    return () => { document.body.style.overflow = 'unset'; };
  }, []);

  return (
    <motion.div 
      initial={{ opacity: 0, x: -50 }} animate={{ opacity: 1, x: 0 }} exit={{ opacity: 0, x: -50 }} 
      transition={{ type: "spring", damping: 25, stiffness: 200 }}
      className="fixed inset-0 z-[100] bg-slate-950 flex flex-col"
    >
      <div className="absolute top-4 left-4 right-4 z-[110] flex justify-between items-center pointer-events-none" dir="rtl">
        <div className="flex gap-2 pointer-events-auto">
          <button onClick={onClose} className="p-3 bg-white/10 hover:bg-white/20 backdrop-blur-xl border border-white/10 rounded-full text-white transition-all active:scale-90">
            <X size={20} />
          </button>
        </div>
        <div className="flex gap-2 pointer-events-auto">
          <button 
            onClick={onToggleSave} 
            className={cn("p-3 backdrop-blur-xl border border-white/10 rounded-full transition-all active:scale-90", isSaved ? 'bg-amber-500/20 text-amber-400' : 'bg-white/10 hover:bg-white/20 text-white')}
          >
            <Star size={20} className={isSaved ? "fill-amber-400" : ""} />
          </button>
          <button onClick={() => setOrientation(orientation === 'vertical' ? 'horizontal' : 'vertical')} className="p-3 bg-indigo-600/80 hover:bg-indigo-500 backdrop-blur-xl shadow-lg shadow-indigo-500/30 border border-white/10 rounded-full text-white transition-all active:scale-90">
            {orientation === 'vertical' ? <ArrowLeftRight size={20} /> : <ArrowUpDown size={20} />}
          </button>
        </div>
      </div>

      {/* PDF Pages container aligned natively for reaching start/middle/end points using padding */}
      <div 
        className={cn("flex-1 flex gap-10 overflow-auto no-scrollbar scroll-smooth p-4", orientation === 'vertical' ? 'flex-col pt-[50vh] pb-[50vh] snap-y snap-mandatory' : 'flex-row items-center px-[50vw] snap-x snap-mandatory')} 
        dir="ltr"
      >
        {[1, 2, 3].map((page) => (
          <div key={page} className={cn("flex-shrink-0 flex items-center justify-center snap-center", orientation === 'vertical' ? 'w-full min-h-[90vh]' : 'w-[85vw] h-[80vh]')}>
            <div className="bg-white dark:bg-slate-900 w-full max-w-2xl h-full md:aspect-[1/1.4] md:h-auto rounded-[2.5rem] shadow-2xl flex flex-col items-center justify-center relative p-8 text-center overflow-hidden border border-white/5 cursor-pointer hover:scale-[1.01] transition-transform duration-300">
              <div className="absolute top-6 left-6 text-slate-300 dark:text-slate-700 font-mono text-3xl font-bold">P.{page}</div>
              <img src={item.cover} className="absolute inset-0 w-full h-full object-cover opacity-5 blur-sm pointer-events-none" alt="" />
              <FileText size={80} className="text-slate-200 dark:text-slate-800 mt-16 relative z-10" />
            </div>
          </div>
        ))}
      </div>
    </motion.div>
  );
}
