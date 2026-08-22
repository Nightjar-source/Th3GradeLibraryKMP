import React from 'react';
import { motion, AnimatePresence } from 'motion/react';
import { X, Clock, Database, BellRing, Settings2 } from 'lucide-react';

import { AppSettings, SyncInterval, CacheInterval } from '../types';
import { cn } from '../utils';

interface SettingsProps {
  isOpen: boolean;
  onClose: () => void;
  settings: AppSettings;
  onUpdateSettings: (settings: AppSettings) => void;
}

export default function NewsSettingsModal({ isOpen, onClose, settings, onUpdateSettings }: SettingsProps) {
  
  const update = (key: keyof AppSettings, value: any) => {
    onUpdateSettings({ ...settings, [key]: value });
  };

  return (
    <AnimatePresence>
      {isOpen && (
        <div className="fixed inset-0 z-[200] flex items-end sm:items-center justify-center pointer-events-auto">
          {/* Backdrop */}
          <motion.div 
            initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}
            onClick={onClose}
            className="absolute inset-0 bg-slate-900/40 dark:bg-black/60 backdrop-blur-sm"
          />
          
          {/* Modal Sheet */}
          <motion.div 
            initial={{ y: '100%' }} animate={{ y: 0 }} exit={{ y: '100%' }}
            transition={{ type: 'spring', damping: 25, stiffness: 200 }}
            className="relative w-full max-w-md bg-white dark:bg-slate-900 rounded-t-[2.5rem] sm:rounded-[2.5rem] p-6 pb-12 shadow-2xl overflow-hidden max-h-[90vh] flex flex-col border border-white/20 dark:border-white/10"
            dir="rtl"
          >
             <div className="flex justify-between items-center mb-6">
                <h2 className="font-reem text-2xl font-bold text-slate-800 dark:text-white flex items-center gap-2">
                   <Settings2 size={24} className="text-teal-500" />
                   إعدادات الأخبار
                </h2>
                <button onClick={onClose} className="p-2 bg-slate-100 dark:bg-slate-800 rounded-full text-slate-500 hover:text-slate-800 dark:hover:text-white transition active:scale-90 shadow-sm">
                  <X size={20} />
                </button>
             </div>

             <div className="overflow-y-auto pr-2 custom-scrollbar flex flex-col gap-8">

               {/* --- Sync Settings --- */}
               <section>
                 <h3 className="text-sm font-bold text-teal-600 dark:text-teal-400 mb-3 flex items-center gap-2 font-reem">
                   <Clock size={18} /> مزامنة الأخبار بالخلفية
                 </h3>
                 <p className="text-xs text-slate-500 dark:text-slate-400 mb-4 leading-relaxed font-medium">
                   يتم الجلب والمزامنة آلياً حسب الوقت المحدد وتغيير الخبر ديناميكياً.
                 </p>
                 <div className="grid grid-cols-2 gap-2">
                   <OptionBtn active={settings.syncInterval === '30m'} onClick={() => update('syncInterval', '30m')} label="كل نصف ساعة" />
                   <OptionBtn active={settings.syncInterval === '1h'} onClick={() => update('syncInterval', '1h')} label="كل ساعة" />
                   <OptionBtn active={settings.syncInterval === '12h'} onClick={() => update('syncInterval', '12h')} label="كل 12 ساعة" />
                   <OptionBtn active={settings.syncInterval === '24h'} onClick={() => update('syncInterval', '24h')} label="كل يوم" />
                   <OptionBtn active={settings.syncInterval === 'on_open'} onClick={() => update('syncInterval', 'on_open')} label="عند فتح التطبيق فقط" className="col-span-2" />
                 </div>
               </section>

               <div className="h-[1px] w-full bg-slate-200 dark:bg-slate-800" />

               {/* --- Sound Notifications --- */}
               <section>
                 <h3 className="text-sm font-bold text-indigo-500 mb-3 flex items-center gap-2 font-reem">
                   <BellRing size={18} /> التنبيهات والأصوات
                 </h3>
                 <div className="flex items-center justify-between bg-slate-50 dark:bg-slate-800/30 p-4 rounded-2xl border border-slate-100 dark:border-slate-800 shadow-sm">
                    <span className="font-bold text-sm text-slate-700 dark:text-slate-200 font-reem">تفعيل صوت الإشعار مع الخبر الجديد</span>
                    <button 
                      onClick={() => update('notificationSound', !settings.notificationSound)}
                      className={cn("w-12 h-6 rounded-full flex items-center transition-colors px-1", settings.notificationSound ? "bg-indigo-500" : "bg-slate-300 dark:bg-slate-600")}
                    >
                       <motion.div 
                         animate={{ x: settings.notificationSound ? -24 : 0 }} 
                         className="w-4 h-4 bg-white rounded-full shadow-md" 
                       />
                    </button>
                 </div>
               </section>

               <div className="h-[1px] w-full bg-slate-200 dark:bg-slate-800" />

               {/* --- Cache Settings --- */}
               <section>
                 <h3 className="text-sm font-bold text-rose-500 mb-3 flex items-center gap-2 font-reem">
                   <Database size={18} /> التخزين المؤقت وحذف الأخبار (الكاش)
                 </h3>
                 <p className="text-xs text-slate-500 dark:text-slate-400 mb-4 leading-relaxed font-medium">
                   حذف الأخبار المخزنة قديماً بشكل آلي بالتناوب.
                 </p>
                 <div className="grid grid-cols-2 gap-2">
                   <OptionBtn active={settings.cacheClearInterval === '7d'} onClick={() => update('cacheClearInterval', '7d')} label="كل أسبوع" />
                   <OptionBtn active={settings.cacheClearInterval === '1m'} onClick={() => update('cacheClearInterval', '1m')} label="كل شهر" />
                   <OptionBtn active={settings.cacheClearInterval === '3m'} onClick={() => update('cacheClearInterval', '3m')} label="كل 3 أشهر (الافتراضي)" />
                   <OptionBtn active={settings.cacheClearInterval === '6m'} onClick={() => update('cacheClearInterval', '6m')} label="كل 6 أشهر" />
                   <OptionBtn active={settings.cacheClearInterval === 'never'} onClick={() => update('cacheClearInterval', 'never')} label="أبداً" className="col-span-2 text-rose-600 bg-rose-50 border-rose-200 dark:border-rose-900/50 dark:bg-rose-900/20" />
                 </div>
               </section>

             </div>
          </motion.div>
        </div>
      )}
    </AnimatePresence>
  );
}

function OptionBtn({ active, onClick, label, className }: { active: boolean, onClick: () => void, label: string, className?: string }) {
  return (
    <button 
      onClick={onClick} 
      className={cn("py-3 px-2 flex justify-center items-center rounded-[1.25rem] text-xs font-bold transition-all border font-reem shadow-sm", 
        active ? "border-teal-500 bg-teal-50 dark:bg-teal-500/10 text-teal-700 dark:text-teal-400" : "border-slate-200 dark:border-slate-700 hover:border-teal-300 dark:hover:border-teal-700 text-slate-600 dark:text-slate-300",
        className
      )}
    >
      {label}
    </button>
  );
}
