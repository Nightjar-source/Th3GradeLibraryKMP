export interface BookItem {
  id: string;
  title: string;
  author: string;
  color: string;
  cover: string;
}

export interface NewsItem {
  id: string;
  title: string;
  pubDate: string;
  imageUrl: string | null;
  link: string;
  contentSnippet?: string;
  isRead?: boolean;
}

export type ThemeType = 'light' | 'dark' | 'system';
export type CacheInterval = '7d' | '1m' | '3m' | '6m' | 'never';
export type SyncInterval = '30m' | '1h' | '12h' | '24h' | 'on_open';

export interface AppSettings {
  theme: ThemeType;
  useMaterialYou: boolean;
  primaryColor: string;
  syncInterval: SyncInterval;
  cacheClearInterval: CacheInterval;
  notificationSound: boolean;
  lastSync: number;
}
