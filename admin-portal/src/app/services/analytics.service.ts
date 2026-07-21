import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { catchError } from 'rxjs/operators';

export interface AnalyticsOverview {
  status: string;
  service: string;
  timestamp: string;
  metrics: {
    totalMongoMessages: number;
    totalActiveUsers: number;
    systemLatencyMs: number;
    activeServices: string[];
  };
  recentLogs: Array<{
    _id: string;
    eventType: string;
    description: string;
    username: string;
    createdAt: string;
  }>;
}

@Injectable({
  providedIn: 'root'
})
export class AnalyticsService {
  private apiUrl = 'http://localhost:5000/api';

  constructor(private http: HttpClient) {}

  getOverview(): Observable<AnalyticsOverview> {
    return this.http.get<AnalyticsOverview>(`${this.apiUrl}/analytics/overview`).pipe(
      catchError(() => {
        // Fallback demo data if Node.js microservice is starting up
        return of({
          status: 'STANDBY',
          service: 'FriendsHub Polyglot Node.js + Express + MongoDB Service',
          timestamp: new Date().toISOString(),
          metrics: {
            totalMongoMessages: 42,
            totalActiveUsers: 18,
            systemLatencyMs: 12,
            activeServices: ['Spring Boot (Java 17)', 'Express (Node.js)', 'Angular Dashboard', 'MongoDB Atlas', 'Supabase Postgres']
          },
          recentLogs: [
            { _id: '1', eventType: 'USER_LOGIN', description: 'Admin session connected to Angular Portal', username: 'System Admin', createdAt: new Date().toISOString() },
            { _id: '2', eventType: 'MESSAGE_SENT', description: 'Low-latency message routed via MongoDB', username: 'Alex', createdAt: new Date().toISOString() },
            { _id: '3', eventType: 'POST_CREATED', description: 'Post event synced across microservices', username: 'Jordan', createdAt: new Date().toISOString() }
          ]
        });
      })
    );
  }
}
