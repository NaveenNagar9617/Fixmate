# FixMate — Master System Features & Technical Architecture Guide

This document contains a complete, exhaustive breakdown of every single feature, micro-capability, background automation engine, security control, and user workflow implemented in **FixMate** (Hostel Issue Tracker & Maintenance Management System).

---

## 📑 Table of Contents
1. [Authentication, Users & Access Control (IAM & RBAC)](#1-authentication-users--access-control-iam--rbac)
2. [Complaint Lifecycle & State Machine](#2-complaint-lifecycle--state-machine)
3. [Intelligent Automation Engines (AI & Heuristics)](#3-intelligent-automation-engines-ai--heuristics)
4. [File & Storage Security (MinIO S3 Private Bucket)](#4-file--storage-security-minio-s3-private-bucket)
5. [Real-Time WebSocket & Push Notifications (STOMP + Redis)](#5-real-time-websocket--push-notifications-stomp--redis)
6. [Audit Timeline, Collaboration & Comments](#6-audit-timeline-collaboration--comments)
7. [Analytics, KPI Dashboards & Reporting](#7-analytics-kpi-dashboards--reporting)
8. [Broken Object-Level Authorization (BOLA/IDOR Defense)](#8-broken-object-level-authorization-bolaidor-defense)
9. [Summary Tech Stack Reference](#9-summary-tech-stack-reference)

---

## 1. Authentication, Users & Access Control (IAM & RBAC)

### 👨‍🎓 Student Self-Registration
- **Dedicated Public Registration (`POST /api/v1/auth/register`):**
  - Public registration is strictly locked to `Role.STUDENT`. Any client-supplied privileged role is ignored on the backend.
  - Form collects student-specific metadata: Full Name, Email, Room Number, Hostel Block (`Block A` to `Block E`), and Phone Number.
  - Password policy: Minimum **10 to 128 characters**, with automatic rejection of common weak passwords (`password123`, `1234567890`, etc.).

### 🛠️ Admin-Only Staff Provisioning
- **Staff Creation (`POST /api/v1/admin/staff`):**
  - Worker accounts (Plumber, Electrician, etc.) cannot self-register.
  - Authenticated Administrators provision staff accounts via an interactive modal in the Admin Dashboard.
  - Supports 7 trade categories: `PLUMBER`, `ELECTRICIAN`, `CARPENTER`, `IT_TECHNICIAN`, `CLEANER`, `SECURITY`, `GENERAL`.
  - Admin assigns a temporary password, and staff profile is automatically linked in the database.

### 🔑 Unified Login & Role-Based Routing
- **Login Endpoint (`POST /api/v1/auth/login`):**
  - Common entry point for Students, Staff, and Admins.
  - Validates credentials using Spring Security's `AuthenticationManager` and `BCryptPasswordEncoder` (cost factor 10).
  - Generic error responses (`"Invalid email or password"`) prevent account enumeration attacks.
  - Frontend automatically inspects the authenticated role and navigates to the appropriate dashboard:
    - `STUDENT` $\rightarrow$ Student Dashboard (`/student`)
    - `STAFF` $\rightarrow$ Staff Assigned Tasks (`/staff`)
    - `ADMIN` $\rightarrow$ Admin Analytics & Management Portal (`/admin`)

### 🔄 Stateless JWT + 7-Day Redis Token Rotation
- **15-Minute Short-Lived Access Token:**
  - Stateless JWT signed with HMAC-SHA256 (`HS256`), valid for exactly **15 minutes (`900000ms`)**.
- **7-Day Sliding Redis Refresh Token:**
  - Refresh tokens stored in Redis (`refresh_token:<userId>`) with a 7-day TTL.
- **Refresh Token Rotation:**
  - Every call to `POST /api/v1/auth/refresh` invalidates the old refresh token and issues a fresh token pair.
- **Axios Silent 401 Interceptor:**
  - When the 15-minute access token expires, the client's HTTP interceptor silently refreshes the token in the background and retries the original request without user interruption.
- **Logout Revocation:**
  - `POST /api/v1/auth/logout` immediately deletes the user's Refresh Token from Redis, ensuring no new access tokens can be obtained.

### 👤 Profile & Avatar Management
- `GET /api/v1/users/me` — Fetches current user profile with an active 1-hour presigned avatar URL.
- `PUT /api/v1/users/me` — Updates user name, phone, room number, or block.
- `POST /api/v1/users/me/photo` — Uploads profile photo with multi-layer image validation, cleans up old photo from MinIO, and returns the new presigned URL.

---

## 2. Complaint Lifecycle & State Machine

### 📝 Complaint Submission
- **Fields:** Title, Category, Priority preference, Location Block, Room Number, Description, and optional initial photo.
- **Transactional Decoupling:**
  - The complaint record is saved and committed to PostgreSQL DB first.
  - File upload to MinIO and photo DB record persistence occur independently, preventing long DB transaction hold times during network uploads.

### 🔄 Strict State Machine Transitions
```text
  [SUBMITTED] ────────► [ASSIGNED] ────────► [IN_PROGRESS] ────────► [RESOLVED] ────────► [CLOSED]
       │                    ▲                                            │                   ▲
       │                    │                                            │ (If unsatisfied)  │
       ▼                    │                                            ▼                   │
  [ESCALATED] ──────────────┘                                       [REOPENED] ──────────────┘
```
- **Permission Matrix:**
  - **Staff:** Can transition assigned complaints to `IN_PROGRESS` and `RESOLVED`.
  - **Student:** Can `CLOSE` (confirm fix) or `REOPEN` (if issue persists) their own complaints.
  - **Admin:** Complete override authority across all statuses.

### 📸 Resolution Proof ("After-Photo")
- When a staff member marks a complaint as `RESOLVED`, they upload an "After Photo" proving the repair work was completed.
- Both initial ("Before") and resolution ("After") photos are viewable in the complaint detail page.

### ⭐ Star Rating & Feedback
- Once a complaint is `CLOSED`, the student can rate the assigned staff member from **1 to 5 Stars** with optional textual feedback.
- System automatically recalculates the worker's average rating in their `StaffProfile`.

### 👥 Nearby Complaints & Community Upvoting
- Students can browse active non-private complaints within their hostel block.
- Students experiencing the same issue can click **Upvote** ("Same problem here"), reducing duplicate submissions.

---

## 3. Intelligent Automation Engines (AI & Heuristics)

### 🚨 1. Emergency Keyword Auto-Priority Evaluator
- `PriorityEvaluationService` inspects the complaint title and description for hazard keywords:
  - **CRITICAL Triggers:** *"sparking"*, *"smoke"*, *"fire"*, *"flood near electric"*, *"short circuit"*, *"gas leak"*.
  - **HIGH Triggers:** *"no water entire floor"*, *"power outage"*, *"lift stuck"*, *"overflow"*.
- Automatically elevates complaint priority regardless of what the user selected.

### ⏱️ 2. Dynamic SLA Deadline Calculator
- Automatically computes resolution deadlines upon complaint creation:
  - 🔴 **CRITICAL:** **2 Hours**
  - 🟠 **HIGH:** **6 Hours**
  - 🟡 **MEDIUM:** **24 Hours**
  - 🟢 **LOW:** **48 Hours**

### ⏰ 3. Automated SLA Breach & Escalation Engine
- `SLACheckScheduler` cron job executes periodically.
- Detects complaints where `now > slaDeadline` and status is still unresolved.
- Automatically:
  1. Transitions status to `ESCALATED`.
  2. Creates an `Escalation` record targeting the hostel Admin.
  3. Writes an immutable event to the `ComplaintTimeline`.
  4. Dispatches high-priority WebSocket alerts to Admin, Student, and Staff.

### 👷 4. Smart Automated Staff Assignment Engine
- `AssignmentService` matches complaint category to staff specializations (e.g. Plumbing $\rightarrow$ Plumber).
- Selects the **least-loaded, currently on-duty** worker.
- Automatically transitions status to `ASSIGNED` and notifies the worker.
- If no matching staff is on duty, complaint waits in queue and is automatically dispatched as soon as a worker toggles **"On Duty"**.

### 🔍 5. Duplicate Detection & Auto-Grouping Engine
- `DuplicateDetectionService` compares incoming complaints against existing active complaints using text similarity + exact block/room/category matching.
- Automatically links duplicates under a parent complaint and transfers upvotes.
- Priority auto-elevation based on community upvotes:
  - 5+ Upvotes $\rightarrow$ Upgraded to `MEDIUM`.
  - 10+ Upvotes $\rightarrow$ Upgraded to `CRITICAL`.

---

## 4. File & Storage Security (MinIO S3 Private Bucket)

### 🛡️ Multi-Layer Image Validation (`ImageValidationUtil`)
1. **Size Limit:** Max 5MB per upload.
2. **Extension Whitelist:** Strictly `.jpg`, `.jpeg`, `.png`, `.webp`.
3. **MIME Type Inspection:** `image/jpeg`, `image/png`, `image/webp`.
4. **Magic Byte Signature Inspection:** Validates raw file header bytes (`FF D8 FF` for JPEG, `89 50 4E 47` for PNG, `RIFF...WEBP` for WebP).
5. **Raster Pixel Decoding:** Native pixel decoding via Java ImageIO + TwelveMonkeys WebP SPI ensures uploaded files are genuine images and not disguised executables or scripts.

### 🔒 100% Private S3 Storage & Dynamic Presigning
- MinIO S3 bucket is completely private (no anonymous read access).
- Files are accessed exclusively through **HMAC-SHA256 1-Hour Presigned URLs** generated on-demand by the backend using the public gateway endpoint.

---

## 5. Real-Time WebSocket & Push Notifications (STOMP + Redis)

### ⚡ STOMP Channel Security
- `WebSocketAuthInterceptor` intercepts inbound STOMP connection frames.
- Validates JWT tokens on `CONNECT` frames and binds authenticated principals to the session.
- Enforces destination authorization on `SUBSCRIBE` frames (e.g. `/topic/admin/**` restricted strictly to `ROLE_ADMIN`).

### 📡 Real-Time Destinations
- `/user/queue/notifications` — Private, targeted alerts for individual users (assignment, status updates, SLA warnings).
- `/topic/announcements` — Broadcast topic for hostel-wide warden announcements.

### 🌐 Multi-Node Redis Pub/Sub Scale-Out
- `RedisWebSocketSubscriber` synchronizes push events across multiple backend server instances in a distributed cluster.

### 🚀 Zero-Reload Client State Synchronization
- When a WebSocket event arrives at the frontend (`useWebSocket.ts`):
  1. Displays an interactive toast notification with emoji indicator.
  2. Automatically invalidates React Query caches (`['analytics']`, `['complaints']`, `['admin']`, `['notifications']`).
  3. UI cards, charts, and tables re-render live data with zero manual page reloads.

---

## 6. Audit Timeline, Collaboration & Comments

### 📜 Immutable Complaint Timeline Audit Log
- Every lifecycle event is permanently recorded with timestamp, author, previous status, new status, and note:
  - Submission $\rightarrow$ Priority Evaluation $\rightarrow$ Assignment $\rightarrow$ Progress Updates $\rightarrow$ Escalations $\rightarrow$ Resolution $\rightarrow$ Rating.

### 💬 Two-Tier Commenting System
- **Public Comments:** Interactive messaging between the student and assigned worker (e.g. scheduling room visits).
- **Internal Notes:** Staff and Admin private notes hidden from students.

### 📢 Hostel Announcements Feed
- Admins can publish prioritized announcements (e.g. *"Water supply maintenance tomorrow 2 PM - 5 PM"*).
- Displays as a live banner on student dashboards.

---

## 7. Analytics, KPI Dashboards & Reporting

### 📊 Executive KPI Metrics
- Total Complaints, Active Pending Count, Resolved Count, Escalated Count.
- Overall Resolution Rate (%) and Average SLA Resolution Time (in hours).

### 📈 Visual Interactive Charts
- **Category Breakdown Donut Chart:** Distribution across Plumbing, Electrical, Carpentry, etc.
- **Block-wise Distribution Bar Chart:** Comparison across Hostel Blocks A, B, C, D, E.
- **30-Day Trend Line Chart:** Daily incoming vs resolved complaint volumes.
- **Complaint Density Heatmap:** Visual matrix of issue frequency across hostel locations.

### 👷 Staff Workload & Duty Management
- Monitor real-time active complaint load per staff member.
- Toggle duty status (`Set On Duty` / `Set Off Duty`).
- Track average customer satisfaction ratings per worker.

### 📑 CSV Export Engine
- Filter complaints by Date Range, Status, Priority, Category, and Block.
- One-click export of complete complaint dataset to formatted `.csv` for administrative reporting.

---

## 8. Broken Object-Level Authorization (BOLA/IDOR Defense)

### 🛡️ Three-Tier Security Model
$$\text{Authentication (Valid JWT)} \longrightarrow \text{Role-Based RBAC (@PreAuthorize)} \longrightarrow \text{Object Ownership / Assignment}$$

### 🔐 Endpoint Access Control Matrix

| Endpoint | Method | Allowed Principals | Unauthorized Response |
|---|---|---|:---:|
| `/api/v1/complaints/{id}` | `GET` | Admin OR Student Owner OR Assigned Staff | `403 Forbidden` |
| `/api/v1/complaints/{id}/timeline` | `GET` | Admin OR Student Owner OR Assigned Staff | `403 Forbidden` |
| `/api/v1/complaints/{id}/escalations` | `GET` | Admin OR Student Owner OR Assigned Staff | `403 Forbidden` |
| `/api/v1/complaints/{id}/comments` | `POST` | Admin OR Student Owner OR Assigned Staff | `403 Forbidden` |
| `/api/v1/complaints/{id}/photo/after` | `POST` | Assigned Staff ONLY | `403 Forbidden` |
| `/api/v1/complaints/{id}/status` | `PATCH` | Admin OR Assigned Staff (Student can only close/reopen) | `403 Forbidden` |
| `/api/v1/complaints/{id}/priority` | `PATCH` | Admin OR Assigned Staff ONLY | `403 Forbidden` |
| `/api/v1/complaints/{id}/rating` | `POST` | Student Owner ONLY (Complaint must be `CLOSED`) | `403 Forbidden` |

---

## 9. Summary Tech Stack Reference

| Layer | Technology | Purpose |
|---|---|---|
| **Backend Framework** | Java 21 / Spring Boot 3.4.3 | Core REST APIs & Micro-Architecture |
| **Database & ORM** | PostgreSQL 16 / Spring Data JPA / Flyway | Relational persistence & DB schema migrations |
| **Caching & Pub/Sub** | Redis 7 | Refresh tokens, Rate limiting, WebSocket distributed clustering |
| **Object Storage** | MinIO S3 (Private Bucket) | Photo storage & HMAC-SHA256 presigned access |
| **Real-Time Messaging** | Spring WebSocket (STOMP) + SockJS | Live bi-directional notifications |
| **Frontend Framework** | React 18 + TypeScript + Vite | Single Page Application (SPA) |
| **Styling & UI** | TailwindCSS + Radix UI + Lucide Icons | Responsive Glassmorphism UI |
| **State & Server Sync** | TanStack React Query + Zustand | Client state management & real-time cache invalidation |
| **Charts & Visuals** | Recharts | Analytics dashboards & workload charts |
