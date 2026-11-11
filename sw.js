// Service Worker for Al-Arabiyyah Bayna Yadayk PWA
const CACHE_NAME = 'arabiya-pwa-v1';

const STATIC_ASSETS = [
  './',
  './index.html',
  './manifest.json',
  './icons/icon-192.png',
  './icons/icon-512.png',
  './unit1.html',
  './unit2.html',
  './unit3.html',
  './unit4.html',
  './unit5.html',
  './unit6.html',
  './unit7.html',
  './unit8.html',
  './audio/words/salawat_human.mp3',
  './audio/words/radiyallahu_anhu.mp3',
  './audio/words/radiyallahu_anha.mp3',
  './audio/words/allah_damma.mp3',
  './audio/words/allah_kasra.mp3',
  './audio/words/allah_fatha.mp3'
];

self.addEventListener('install', (event) => {
  event.waitUntil(
    caches.open(CACHE_NAME).then((cache) => {
      console.log('[PWA SW] Pre-caching core offline assets');
      return cache.addAll(STATIC_ASSETS);
    }).then(() => self.skipWaiting())
  );
});

self.addEventListener('activate', (event) => {
  event.waitUntil(
    caches.keys().then((keys) => {
      return Promise.all(
        keys.map((key) => {
          if (key !== CACHE_NAME) {
            console.log('[PWA SW] Removing old cache:', key);
            return caches.delete(key);
          }
        })
      );
    }).then(() => self.clients.claim())
  );
});

self.addEventListener('fetch', (event) => {
  // Only handle GET requests
  if (event.request.method !== 'GET') return;

  event.respondWith(
    caches.match(event.request).then((cachedResponse) => {
      if (cachedResponse) {
        return cachedResponse;
      }
      return fetch(event.request).then((networkResponse) => {
        // Cache fetched audio or font resources dynamically
        if (
          networkResponse && 
          networkResponse.status === 200 &&
          (event.request.url.includes('/audio/') || event.request.url.includes('fonts.gstatic.com'))
        ) {
          const responseToCache = networkResponse.clone();
          caches.open(CACHE_NAME).then((cache) => {
            cache.put(event.request, responseToCache);
          });
        }
        return networkResponse;
      }).catch(() => {
        // Fallback for offline navigation
        if (event.request.mode === 'navigate') {
          return caches.match('./index.html');
        }
      });
    })
  );
});
