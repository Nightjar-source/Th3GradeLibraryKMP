import { clsx, type ClassValue } from "clsx";
import { twMerge } from "tailwind-merge";

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}

export const MOCK_DATA = {
  books: [
    { id: 'b1', title: 'الرياضيات - الجزء الأول', author: 'المنهج الوزاري 2026', color: 'from-blue-500 to-indigo-600', cover: 'https://images.unsplash.com/photo-1635070041078-e363dbe005cb?w=400&q=80' },
    { id: 'b2', title: 'اللغة العربية - القواعد', author: 'المنهج الوزاري 2026', color: 'from-emerald-400 to-teal-600', cover: 'https://images.unsplash.com/photo-1588615419957-ed6996593e56?w=400&q=80' },
    { id: 'b3', title: 'الفيزياء', author: 'المنهج الوزاري 2026', color: 'from-purple-500 to-fuchsia-600', cover: 'https://images.unsplash.com/photo-1636466497217-26a8cbeaf0aa?w=400&q=80' },
  ],
  notes: [
    { id: 'n1', title: 'ملزمة الشمس - رياضيات', author: 'أ. علي كمال', color: 'from-amber-400 to-orange-500', cover: 'https://images.unsplash.com/photo-1516979187457-637abb4f9353?w=400&q=80' },
    { id: 'n2', title: 'مراجعة مركزة - أحياء', author: 'د. سارة عادل', color: 'from-teal-400 to-emerald-500', cover: 'https://images.unsplash.com/photo-1530213786676-41801264c489?w=400&q=80' },
  ],
  news: [
    { id: 'nw1', title: 'عاجل: جدول الامتحانات الوزارية للثالث متوسط', pubDate: '25 مايو 2026', link: '#', imageUrl: 'https://images.unsplash.com/photo-1434030216411-0b793f4b4173?w=400&q=80' },
    { id: 'nw2', title: 'وزارة التربية تعلن تقليص المناهج لبعض المواد', pubDate: '20 مايو 2026', link: '#', imageUrl: 'https://images.unsplash.com/photo-1524995997946-a1c2e315a42f?w=400&q=80' },
  ]
};
