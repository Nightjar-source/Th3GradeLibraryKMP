import React from 'react';
import { motion, AnimatePresence } from 'motion/react';
import { X, Moon, Sun, Monitor, Palette, Sparkles, Code2 } from 'lucide-react';

import { AppSettings, ThemeType } from '../types';
import { cn } from '../utils';

interface SettingsProps {
  isOpen: boolean;
  onClose: () => void;
  settings: AppSettings;
  onUpdateSettings: (settings: AppSettings) => void;
}

export default function AppSettingsModal({ isOpen, onClose, settings, onUpdateSettings }: SettingsProps) {
  
  const update = (key: keyof AppSettings, value: any) => {
    onUpdateSettings({ ...settings, [key]: value });
  };

  const colors = ['#6366f1', '#14b8a6', '#f59e0b', '#ec4899', '#8b5cf6'];

  return (
    <AnimatePresence>
      {isOpen && (
        <div className="fixed inset-0 z-[200] flex items-end sm:items-center justify-center pointer-events-auto">
          <motion.div 
            initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}
            onClick={onClose}
            className="absolute inset-0 bg-slate-900/40 dark:bg-black/60 backdrop-blur-sm"
          />
          
          <motion.div 
            initial={{ y: '100%' }} animate={{ y: 0 }} exit={{ y: '100%' }}
            transition={{ type: 'spring', damping: 25, stiffness: 200 }}
            className="relative w-full max-w-md bg-white dark:bg-slate-900 rounded-t-[2.5rem] sm:rounded-[2.5rem] p-6 pb-12 shadow-2xl overflow-hidden max-h-[90vh] flex flex-col border border-white/20 dark:border-white/10"
            dir="rtl"
          >
             <div className="flex justify-between items-center mb-6">
                <h2 className="font-reem text-2xl font-bold text-slate-800 dark:text-white flex items-center gap-2">
                  <Palette size={24} className="text-indigo-500" />
                  المظهر (Theme)
                </h2>
                <button onClick={onClose} className="p-2 bg-slate-100 dark:bg-slate-800 rounded-full text-slate-500 hover:text-slate-800 dark:hover:text-white transition active:scale-90 shadow-sm">
                  <X size={20} />
                </button>
             </div>

             <div className="overflow-y-auto pr-2 custom-scrollbar flex flex-col gap-6">
               
               {/* --- Theme Mode Settings --- */}
               <section>
                 <h3 className="text-sm font-bold text-slate-700 dark:text-slate-300 mb-3 flex items-center gap-2 font-reem">
                   <Sun size={16} className="text-orange-500" /> وضع الإضاءة
                 </h3>
                 <div className="flex bg-slate-100 dark:bg-slate-800/50 p-1.5 rounded-[1.5rem] border border-slate-200 dark:border-slate-700">
                   <ThemeBtn active={settings.theme === 'light'} onClick={() => update('theme', 'light')} icon={<Sun size={18}/>} label="فاتح" />
                   <ThemeBtn active={settings.theme === 'dark'} onClick={() => update('theme', 'dark')} icon={<Moon size={18}/>} label="مظلم" />
                   <ThemeBtn active={settings.theme === 'system'} onClick={() => update('theme', 'system')} icon={<Monitor size={18}/>} label="اوتوماتيك" />
                 </div>
               </section>

               <div className="h-[1px] w-full bg-slate-200 dark:bg-slate-800" />

               {/* --- Material You & Colors Settings --- */}
               <section>
                 <h3 className="text-sm font-bold text-indigo-500 mb-3 flex items-center gap-2 font-reem">
                   <Sparkles size={16} /> ألوان متريال ديزاين 3
                 </h3>
                 <p className="text-xs text-slate-500 dark:text-slate-400 mb-4 leading-relaxed font-medium">
                   استخراج الألوان من النظام (Material You Dynamic Colors) لتعطي إضاءة ناعمة وتباين بصري مريح Expressive.
                 </p>
                 
                 <div className="flex items-center justify-between bg-slate-50 dark:bg-slate-800/30 p-4 rounded-2xl border border-slate-100 dark:border-slate-800 mt-2 mb-4">
                    <span className="font-bold text-sm text-slate-700 dark:text-slate-200">تفعيل الألوان الديناميكية النظام</span>
                    <button 
                      onClick={() => update('useMaterialYou', !settings.useMaterialYou)}
                      className={cn("w-12 h-6 rounded-full flex items-center transition-colors px-1", settings.useMaterialYou ? "bg-indigo-500" : "bg-slate-300 dark:bg-slate-600")}
                    >
                       <motion.div 
                         animate={{ x: settings.useMaterialYou ? -24 : 0 }} 
                         className="w-4 h-4 bg-white rounded-full shadow-md" 
                       />
                    </button>
                 </div>

                 <div className={cn("transition-all duration-300 bg-white dark:bg-slate-800/20 p-4 rounded-2xl border border-slate-100 dark:border-slate-800", settings.useMaterialYou ? "opacity-50 pointer-events-none" : "opacity-100")}>
                    <h4 className="text-xs font-bold text-slate-500 mb-4 font-reem">أو اختر لونك المفضل يدوياً:</h4>
                    <div className="flex justify-between gap-2">
                      {colors.map(color => (
                         <button 
                           key={color}
                           onClick={() => update('primaryColor', color)}
                           className={cn("w-10 h-10 rounded-full border-2 transition-transform", settings.primaryColor === color ? "scale-110 border-white shadow-lg shadow-black/20" : "border-transparent hover:scale-105")}
                           style={{ backgroundColor: color }}
                         />
                      ))}
                    </div>
                 </div>
               </section>

             </div>
          </motion.div>
        </div>
      )}
    </AnimatePresence>
  );
}

function ThemeBtn({ active, onClick, icon, label }: { active: boolean, onClick: () => void, icon: any, label: string }) {
  return (
    <button 
      onClick={onClick} 
      className={cn("flex-1 py-3 flex items-center justify-center gap-2 rounded-[1.25rem] transition-all font-bold text-sm", 
        active ? "bg-white dark:bg-slate-700 shadow shadow-slate-200/50 dark:shadow-black/20 text-indigo-600 dark:text-indigo-400" : "text-slate-500 dark:text-slate-400 hover:bg-white/50 dark:hover:bg-slate-700/50"
      )}
    >
      {icon} <span className="font-reem">{label}</span>
    </button>
  );
}
