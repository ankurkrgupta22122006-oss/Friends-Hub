# FriendsHub 🌐

> A production-grade full-stack social media platform — built for scale, secured by design.

**Live Demo:** [www.friendshub.me](https://www.friendshub.me)

---

## ✨ Features

### Core Social
- Posts with images, likes, comments, shares
- Stories (24hr auto-expiry)
- Follow / unfollow system
- Public & private profiles
- Group messaging

### Real-time
- WebSocket-based live chat
- Instant push notifications (online users)
- Online presence indicator (Redis TTL heartbeat)

### Authentication & Security
- JWT stateless authentication
- **Google OAuth** (Sign in with Google)
- JWT blacklisting on logout (Redis)
- Forgot password / OTP reset flow
- Role-based access control (USER / ADMIN)
- Sliding-window rate limiting (60 req/min per IP)
- Circuit breaker pattern (Resilience4j)
- Hardened error responses (no stack trace leakage)

### Performance
- Redis distributed caching (feed, profiles, sessions)
- Async event processing via Kafka (notifications, emails)
- Supabase CDN for media storage
- Prometheus + Actuator metrics

---

## 🏗️ Architecture
┌─────────────────┐     ┌──────────────────┐     ┌─────────────────┐
│   React 18      │────▶│  Spring Boot 3.2 │────▶│  PostgreSQL     │
│   Vite + TW     │     │  Java 17         │     │  (Supabase)     │
│   Framer Motion │     │  Railway         │     └─────────────────┘
│   Vercel CDN    │     └────────┬─────────┘
└─────────────────┘              │
┌────────┴─────────┐
│                  │
┌───────▼──────┐   ┌───────▼──────┐
│  Redis       │   │  Kafka        │
│  (Upstash)   │   │  Async Events │
│  Cache +     │   │  Notifications│
│  Presence    │   │  Emails       │
└──────────────┘   └──────────────┘

---

## ⚙️ Tech Stack

| Layer | Technology |
|---|---|
| Frontend | React 18, Vite, Tailwind CSS, Framer Motion |
| Backend | Java 17, Spring Boot 3.2, Spring Security |
| Database | PostgreSQL via Supabase |
| Cache | Redis via Upstash |
| Async | Apache Kafka |
| Auth | JWT + Google OAuth (Supabase Auth) |
| Media | Supabase Storage |
| Realtime | Spring WebSocket (STOMP) |
| Observability | Prometheus, Micrometer, Spring Actuator |
| Resilience | Resilience4j Circuit Breaker |
| Deployment | Railway (backend), Vercel (frontend) |

---

## 🚀 Local Development

### Prerequisites
- Java 17+, Maven, Node.js 18+
- Docker (for local Redis + Kafka)

### Backend
```bash
# Start local Redis + Kafka
docker compose up -d

# Run Spring Boot
mvn spring-boot:run
```

Server: `http://localhost:8080`
Health: `http://localhost:8080/actuator/health`
Metrics: `http://localhost:8080/actuator/prometheus`
Kafka UI: `http://localhost:8090`

### Frontend
```bash
cd frontend
npm install
npm run dev
```

Frontend: `http://localhost:5173`

---

## 🔐 Environment Variables

Copy `.env.example` to `.env` and fill in:

```bash
# Database (Supabase PostgreSQL)
DB_URL=jdbc:postgresql://...
DB_USER=postgres
DB_PASS=...

# Auth
JWT_SECRET=your_base64_secret

# Supabase
SUPABASE_URL=https://xxx.supabase.co
SUPABASE_KEY=your_anon_key

# Redis (Upstash)
REDIS_URL=rediss://default:...@....upstash.io:6379

# Kafka
KAFKA_SERVERS=localhost:9092

# Google OAuth
GOOGLE_CLIENT_ID=...
GOOGLE_CLIENT_SECRET=...

# Email
MAIL_USERNAME=your@gmail.com
MAIL_PASSWORD=your_app_password

# App
FRONTEND_URL=https://www.friendshub.me
```

---

## 📡 API Endpoints

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/auth/register` | Register with email |
| POST | `/api/auth/login` | Login with email/password |
| POST | `/api/auth/oauth/google` | Google OAuth login/register |
| POST | `/api/auth/forgot-password` | Send OTP to email |
| POST | `/api/auth/reset-password` | Reset with OTP |
| GET | `/api/posts/feed` | Get home feed (cached) |
| POST | `/api/posts` | Create post |
| POST | `/api/posts/{id}/like` | Like a post (async notification) |
| GET | `/api/users/{username}` | Get user profile (cached) |
| GET | `/api/chat/messages/{roomId}` | Get chat messages |
| GET | `/actuator/health` | Health check |
| GET | `/actuator/prometheus` | Prometheus metrics |

---

## 🗺️ Roadmap

- [x] JWT Authentication
- [x] Google OAuth
- [x] Real-time WebSocket Chat
- [x] Redis Caching + Presence
- [x] Kafka Async Notifications
- [x] Forgot Password / OTP Reset
- [x] Circuit Breaker + Rate Limiting
- [x] Prometheus Metrics
- [ ] AI-powered feed ranking
- [ ] Mobile app (React Native)
- [ ] End-to-end message encryption

---

*Built with ❤️ by [Jay Anand](https://github.com/Jayanand07)* here is repo link - https://github.com/Jayanand07/Friends-Hub.git
