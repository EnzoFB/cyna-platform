import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';

export interface ClientLogEntry {
  timestamp: string;
  level: 'ERROR' | 'WARN' | 'INFO' | 'DEBUG';
  message: string;
  url?: string;
  stack?: string;
  userAgent?: string;
  correlationId?: string;
  appVersion?: string;
}

const LOG_ENDPOINT = `${environment.apiUrl}/logs/client`;
const FLUSH_INTERVAL_MS = 30000;
const MAX_STACK_LENGTH = 4000;
const MAX_PENDING = 100;
const DB_NAME = 'cyna-logs';
const STORE_NAME = 'pending';

@Injectable({ providedIn: 'root' })
export class LoggingService {
  private http = inject(HttpClient);
  private buffer: ClientLogEntry[] = [];
  private db: IDBDatabase | null = null;
  private isFlushing = false;

  constructor() {
    this.initDb().catch(() => { /* silent fail */ });
    setInterval(() => this.flush(), FLUSH_INTERVAL_MS);
    window.addEventListener('online', () => this.drain());
  }

  logError(error: unknown, context?: { url?: string }): void {
    const entry = this.buildEntry('ERROR', error, context);
    this.buffer.push(entry);
    this.flush();
  }

  logWarn(message: string, context?: { url?: string }): void {
    const entry = this.buildEntry('WARN', message, context);
    this.buffer.push(entry);
    this.flush();
  }

  logInfo(message: string, context?: { url?: string }): void {
    const entry = this.buildEntry('INFO', message, context);
    this.buffer.push(entry);
  }

  private buildEntry(
    level: ClientLogEntry['level'],
    payload: unknown,
    context?: { url?: string }
  ): ClientLogEntry {
    const correlationId = sessionStorage.getItem('x-correlation-id') ?? this.generateCorrelationId();
    let message: string;
    let stack: string | undefined;

    if (payload instanceof Error) {
      message = payload.message || 'Unknown error';
      stack = payload.stack?.substring(0, MAX_STACK_LENGTH);
    } else if (typeof payload === 'string') {
      message = payload;
    } else {
      try {
        message = JSON.stringify(payload);
      } catch {
        message = 'Unknown log payload';
      }
    }

    message = this.stripPII(message);
    if (stack) {
      stack = this.stripPII(stack);
    }

    return {
      timestamp: new Date().toISOString(),
      level,
      message,
      url: context?.url ?? window.location.href,
      stack,
      userAgent: navigator.userAgent,
      correlationId,
      appVersion: '1.0.0',
    };
  }

  private stripPII(text: string): string {
    return text
      .replace(/password["']?\s*[:=]\s*["'][^"']{3,}["']/gi, 'password":"[REDACTED]"')
      .replace(/email["']?\s*[:=]\s*["'][^"']+@[^"']+["']/gi, 'email":"[REDACTED]"')
      .replace(/cardNumber["']?\s*[:=]\s*["']\d{4}[\s-]?\d{4}[\s-]?\d{4}[\s-]?\d{4}["']/gi, 'cardNumber":"[REDACTED]"')
      .replace(/cvv["']?\s*[:=]\s*["']\d{3,4}["']/gi, 'cvv":"[REDACTED]"');
  }

  private generateCorrelationId(): string {
    const id = crypto.randomUUID();
    sessionStorage.setItem('x-correlation-id', id);
    return id;
  }

  private async initDb(): Promise<void> {
    return new Promise((resolve, reject) => {
      const request = indexedDB.open(DB_NAME, 1);
      request.onerror = () => reject(request.error);
      request.onsuccess = () => {
        this.db = request.result;
        resolve();
      };
      request.onupgradeneeded = () => {
        request.result.createObjectStore(STORE_NAME, { keyPath: 'id', autoIncrement: true });
      };
    });
  }

  private async flush(): Promise<void> {
    if (this.isFlushing) {
      return;
    }
    if (this.buffer.length === 0 && !navigator.onLine) {
      return;
    }
    this.isFlushing = true;
    try {
      const entries = [...this.buffer];
      this.buffer = [];

      const pending = await this.readPending();
      const all = [...pending, ...entries];
      if (all.length === 0) {
        return;
      }

      if (!navigator.onLine) {
        await this.writePending(all);
        return;
      }

      this.http.post(LOG_ENDPOINT, all).subscribe({
        next: () => this.clearPending(),
        error: async () => {
          await this.writePending(all);
        },
      });
    } finally {
      this.isFlushing = false;
    }
  }

  private async drain(): Promise<void> {
    await this.flush();
  }

  private async readPending(): Promise<ClientLogEntry[]> {
    if (!this.db) return [];
    return new Promise((resolve, reject) => {
      const tx = this.db!.transaction(STORE_NAME, 'readonly');
      const store = tx.objectStore(STORE_NAME);
      const req = store.getAll();
      req.onsuccess = () => resolve(req.result as ClientLogEntry[]);
      req.onerror = () => reject(req.error);
    });
  }

  private async writePending(entries: ClientLogEntry[]): Promise<void> {
    if (!this.db || entries.length === 0) return;
    const toStore = entries.length > MAX_PENDING ? entries.slice(-MAX_PENDING) : entries;
    const tx = this.db.transaction(STORE_NAME, 'readwrite');
    const store = tx.objectStore(STORE_NAME);
    for (const e of toStore) {
      store.put(e);
    }
    return new Promise((resolve, reject) => {
      tx.oncomplete = () => resolve();
      tx.onerror = () => reject(tx.error);
    });
  }

  private async clearPending(): Promise<void> {
    if (!this.db) return;
    const tx = this.db.transaction(STORE_NAME, 'readwrite');
    const store = tx.objectStore(STORE_NAME);
    store.clear();
    return new Promise((resolve, reject) => {
      tx.oncomplete = () => resolve();
      tx.onerror = () => reject(tx.error);
    });
  }
}
