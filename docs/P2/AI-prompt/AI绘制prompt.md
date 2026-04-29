# AI绘制prompt

工具：chatgpt-image2

## 架构图

```
Generate a clean, modern, flat-style technical software architecture diagram (NOT isometric, NOT 3D, NOT cartoon) for a campus mutual-aid web platform called "CampusHub". Image ratio 16:9, high resolution (at least 2048×1152), white background, sans-serif labels in English (avoid Chinese characters in the rendered image since most image models render them poorly).

Overall composition: a strict TOP-DOWN, 4-tier layered diagram. Each tier is a horizontal band spanning the full width, separated by thin gray dividers. Tier label sits on the left margin in bold gray.

=== TIER 1 — CLIENT (light blue band, top) ===
Two rounded rectangle cards, side by side:
  • Left card: "FE_Student SPA" with sub-label "React 19 + TS 5.9 + Vite 8"
  • Right card: "FE_Admin SPA" with sub-label "React 19, /admin routes"
Both cards have a small browser icon on top-left.
Two arrows go down from these cards merging into Tier 2, labeled "HTTPS / REST + JWT".

=== TIER 2 — EDGE (light yellow band) ===
One wide rectangle: "Nginx" with four bullet sub-labels arranged horizontally inside:
  "TLS Termination | Static Hosting | Reverse Proxy | IP/URI Rate Limit"
A small Nginx logo or generic gateway icon on the left of this rectangle.

=== TIER 3 — APPLICATION (light green band, the largest tier) ===
A single big rounded container titled "Spring Boot Monolith — single fat-jar, single JVM, single DB transaction".
INSIDE this container, draw a 4×3 grid of 12 small module chips, each with the module name centered in bold:
  Row 1: auth | user | credit
  Row 2: task | trade | edu
  Row 3: team | im | notify
  Row 4: report | admin | search
Color the 3 chips in Row 1 (auth/user/credit) slightly darker green to mark "L1 foundation domain".
Between chips draw thin DASHED gray arrows in multiple directions to suggest in-process communication, with one annotation balloon pointing into the cluster: "Cross-module: XxxApi (sync) or ApplicationEvent (BEFORE_COMMIT). Direct cross-module @Autowired Repository = forbidden."

=== TIER 4 — RESOURCE (light gray band, bottom) ===
Four rectangles in a row, with appropriate icons:
  1. "MySQL 8" (database cylinder icon) — sub-label "single instance, logical sharding by table prefix"
  2. "MinIO Object Storage" (bucket icon) — sub-label "3 buckets: verify-material / task-proof / study-resource"
  3. "Aliyun SMS Gateway" (cloud icon) — sub-label "auth code only, ≤300 RMB/month"
  4. "Redis 7 (OPTIONAL)" (key-value icon) — drawn with a DASHED border in lighter gray to mark "disabled by default — see ADR-005"

Arrows from Tier 3 to Tier 4: SOLID arrows for the first 3 (MySQL, MinIO, SMS). DASHED arrow to Redis. Each arrow has a small label: "JDBC + HikariCP" / "S3 API" / "REST" / "disabled, feature flag".

=== LEGEND (bottom-right corner, small box) ===
- Solid arrow → synchronous in-process or active dependency
- Dashed arrow → optional / event-based / disabled-by-default
- Light blue = Client, Yellow = Edge, Green = Application, Gray = Resource

Visual style: modern enterprise architecture diagram à la AWS / GCP whitepapers; clean type, generous whitespace, no clipart, no people figures, no neon colors. Make sure all labels are crisp and legible at the rendered size.
```

## 模块依赖图

```
Generate a clean, modern, flat-style 2D module dependency diagram (NOT isometric, NOT 3D) for the backend of "CampusHub" — a Java + Spring Boot modular monolith. Image ratio 16:9, high resolution (at least 2048×1152), white background, all labels in English (avoid Chinese characters in the rendered image).

Overall composition: a strict LEFT-TO-RIGHT, three-column layered architecture diagram. Each column is a vertically-stacked group of module chips inside a softly-rounded translucent container. The three column containers sit side-by-side with arrows flowing only from RIGHT to LEFT (callers depend on lower-layer modules on the left).

=== COLUMN 1 (LEFT) — "L1 Foundation Domain" (light blue background) ===
Three module chips stacked vertically, each is a small rounded rectangle:
  • auth   — sub-label "phone+SMS, JWT, manual student-card review"
  • user   — sub-label "nickname, privacy flags, public DTO"
  • credit — sub-label "score & points, freeze/settle/unfreeze"
Header label above the column: "L1 — Foundation Domain (no downstream deps)"

=== COLUMN 2 (MIDDLE) — "L2 Business Domain" (light green background) ===
Five module chips stacked vertically:
  • task   — sub-label "post / accept / state machine"
  • trade  — sub-label "second-hand goods, EXIF cleanup"
  • edu    — sub-label "course review, tutoring tasks"
  • team   — sub-label "team recruitment, skill tags"
  • im     — sub-label "private chat, task threads"
Header above column: "L2 — Business Domain (depends on L1)"

=== COLUMN 3 (RIGHT) — "L3 Cross-cutting Domain" (light yellow background) ===
Four module chips stacked vertically:
  • notify — sub-label "system messages, event-driven"
  • report — sub-label "complaint, appeal 7d/3times, arbitration"
  • admin  — sub-label "review, audit log, config"
  • search — sub-label "read-only, full-text & filters"
Header above column: "L3 — Cross-cutting Domain (depends on L1 + L2)"

=== ARROWS (CRITICAL — read carefully) ===
ALL arrows go from RIGHT-side columns toward LEFT-side columns (caller → callee = depends on). NEVER draw a left-to-right arrow.

Solid blue arrows (synchronous XxxApi calls):
  task    → auth, user, credit
  trade   → task, user, credit
  edu     → auth, user, credit, task
  team    → auth, user
  im      → auth, user, task
  report  → task, credit, im
  admin   → auth, report, credit
  search  → task, edu, trade   (label every arrow from search with "read-only")

Dashed orange arrows (ApplicationEvent subscriptions):
  notify -.-> task, trade, edu, report, auth   (label "subscribes Event")

Add 2~3 short annotation labels on representative arrows:
  - task → credit: "freeze / deduct / settle"
  - report → credit: "deduct (BEFORE_COMMIT)"
  - notify -.-> task: "AFTER_COMMIT + @Async"

=== ANTI-PATTERN HINT (small red box, bottom-left corner) ===
A tiny example arrow drawn in RED with a big "✗" overlay, going from L1 → L2, labeled "VIOLATION: lower layer must NOT depend on upper layer". This makes the rule visually obvious to readers.

=== LEGEND (bottom-right corner, small white box) ===
- Solid blue arrow = synchronous XxxApi call
- Dashed orange arrow = ApplicationEvent subscription
- Red dashed arrow = forbidden reverse dependency
- Color blocks = L1 (blue) / L2 (green) / L3 (yellow)

Visual style: modern enterprise dependency diagram, generous spacing between arrows, use orthogonal/curved connectors that bend rather than straight overlapping lines so labels stay readable. No clipart, no human figures, no decorative elements. All labels must be crisp and legible.
```

## 部署拓扑图

```
Generate a clean, modern, flat-style 2D deployment topology diagram (NOT isometric, NOT 3D) for the production deployment of "CampusHub". Image ratio 16:9, high resolution (at least 2048×1152), white background. All labels in English (avoid Chinese characters in the rendered image).

Overall composition: TOP-DOWN data flow with three vertical zones:
  Zone A (top, 15% height) — public clients
  Zone B (middle, 65% height) — a SINGLE cloud server, drawn as one big rounded rectangle
  Zone C (bottom, 20% height) — external SaaS dependency

=== ZONE A — Clients (top) ===
A single horizontal row showing two browser/laptop icons labeled:
  "Student Browser"   "Admin Browser"
Both connect downward via a SOLID arrow labeled "HTTPS : 443" pointing into Zone B.

=== ZONE B — Single Cloud Server (the main subject of the diagram) ===
A large rounded rectangle taking ~65% of the canvas height. Title bar at the top of this rectangle: "Cloud Server  /  4 vCPU · 8 GB RAM · 100 GB SSD  /  Ubuntu 20.04  /  Docker Compose"

Inside this server rectangle, lay out 5 container boxes in a clear data-flow pattern:

  TIER 1 — entry container (top of Zone B):
    [Nginx]  port 80/443  — sub-label "TLS termination · Static SPA hosting · Reverse proxy · Rate limit"
    Show two small SPA build folder icons next to Nginx ("FE_Student build" / "FE_Admin build") to indicate static hosting.

  TIER 2 — application container (middle of Zone B):
    [Spring Boot Monolith]  port 8080  — sub-label "fat-jar, 12 business packages, JVM 17"

  TIER 3 — data containers (bottom row of Zone B, three side-by-side):
    [MySQL 8]   port 3306  — sub-label "single instance, ACID local txn"
    [MinIO]     port 9000  — sub-label "3 buckets: verify-material, task-proof, study-resource"
    [Redis 7]   port 6379  — sub-label "OPTIONAL · disabled by default · ADR-005"   — draw with DASHED border in lighter tone

Each container rectangle should display a small Docker whale icon in the top-right corner of the container box to indicate it runs in Docker.

Connections inside Zone B (all SOLID arrows unless stated):
  - Nginx → Spring Boot, label "reverse proxy /api/**"
  - Nginx → its own static folders, label "/ → SPA build"
  - Spring Boot → MySQL,  label "JDBC + HikariCP"
  - Spring Boot → MinIO,  label "S3 API"
  - Spring Boot ⇢ Redis (DASHED arrow), label "feature-flag off"

Below the three data containers, draw a thin gray strip labeled "Host volumes (bind mounts)" with three small disk icons under MySQL / MinIO / Redis showing data persistence to the host.

Add a small clock icon labeled "Cron 03:00 daily" with two DASHED arrows pointing to MySQL and MinIO, with a tiny annotation "mysqldump → /backup (7-day retention) + MinIO bucket replica".

Add a heart icon labeled "Health check /actuator/health · restart on 3 failures" near the Spring Boot container.

=== ZONE C — External Dependency (bottom) ===
A single rectangle outside the server: "Aliyun SMS API" with a cloud icon. A SOLID arrow goes from Spring Boot down/right to this rectangle, label "REST · auth code only · ≤ 300 RMB/month".

=== LEGEND (small box, bottom-right corner) ===
- Solid arrow = active runtime connection
- Dashed arrow = optional / disabled-by-default / scheduled job
- Dashed-border container = optional (feature flag)
- Light blue = container, light yellow = external SaaS, light gray = host storage

Visual style: modern infra-architecture diagram in the style of AWS/Aliyun reference architectures. Soft pastel fills, thin dark gray strokes, generous whitespace. Container labels rendered in clean monospaced or sans-serif font. NO 3D perspective, NO isometric, NO clipart of people, NO decorative noise. All text must be sharp and readable at the rendered size.
```

