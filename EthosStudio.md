## Product Design Requirements Document – Hyper-Focused MVP

**Version:** 3.0  
**Date:** 2026-06-07  
**Concept:** Time‑bound free activities with limited capacity. Add‑ons from vetted providers. Booking and payment entirely via Zalo. Platform earns via weekly provider commission on completed add‑ons.  
**Market:** D2, Saigon, Vietnam.  
**Tech stack:** KMP, Compose, Kotlin/JS, ArangoDB, Ktor.

---

### 1. Roles & Permissions

| Role | Description | Access |
|------|-------------|--------|
| **Visitor** | Any user browsing the platform. No account needed. | View available activities, details, add‑ons. Initiate booking via Zalo. Submit a review via Zalo‑sent link. |
| **Provider** | Vetted service provider (no public badge). | Activate add‑ons for specific activities, set custom prices & options, accept/reject bookings, mark complete, view weekly commission summary. |
| **Admin** | Platform operator. | Approve providers, manage activities & providers, view pulse metrics, handle disputes manually, generate invoices. |
| **Ethos Studio** | Content team. | Create/edit activities (time, capacity, description, photos) and activity‑specific add‑ons. |

---

### 2. Global Requirements

- [ ] **Languages:** Vietnamese (default) and English, switchable.
- [ ] **Responsive:** Mobile‑first; works on iOS Safari, Android Chrome.
- [ ] **Currency:** VND, tax inclusive.
- [ ] **Time zone:** All times in ICT (Vietnam). Server‑side clock determines “available now”.
- [ ] **Real‑time filtering:** Only activities where `server time` is between the activity’s start and end time AND `remaining spots > 0` appear on the homepage. When an activity ends or fills, it disappears immediately.
- [ ] **Zalo integration:** Deep‑link to open a pre‑filled Zalo chat for booking. If Zalo is not installed, a fallback web page with the same pre‑filled message and a “Copy to clipboard” button.
- [ ] **Review links:** Unique, token‑based URLs generated per booking. Providers share the link after service; no login required to submit a review.
- [ ] **Legal:** Terms of Service clarify platform facilitates connection only, all payments and service delivery handled by providers, providers carry own insurance.

---

### 3. Homepage – “Available Now”

- [ ] **Activity list:** Only shows activities currently within their scheduled window and with spots remaining.
- [ ] **Activity card (compact):**
    - [ ] Primary photo
    - [ ] Title
    - [ ] Short description (1–2 lines)
    - [ ] Free badge
    - [ ] Time slot (e.g., “16:00–18:00”)
    - [ ] Remaining spots (e.g., “4 spots left / 15”)
    - [ ] Add‑on count (e.g., “2 add‑ons”)
    - [ ] Category tag
    - [ ] Average rating & review count (if any)
- [ ] **Filters:**
    - [ ] Category dropdown
    - [ ] Search bar (title, description, venue name)
- [ ] **No accounts → no favorites.** Visitors can bookmark the URL in their browser.

---

### 4. Activity Detail Page

- [ ] **Essential info:**
    - [ ] Title, full description (with soft encouragement to support venue)
    - [ ] Time window: start – end
    - [ ] Remaining spots (e.g., “3 spots left out of 10”)
    - [ ] Category
- [ ] **Photos:** Carousel of studio‑provided images.
- [ ] **Location:** Text address only. “Open in Google Maps” link (no map embed).
- [ ] **Add‑ons panel:**
    - [ ] Each add‑on shows:
        - [ ] Provider name & avatar
        - [ ] Short description
        - [ ] Base price
        - [ ] Customisation options with +price (expandable)
    - [ ] Customisations are toggles/quantity selectors; the total add‑on price updates dynamically.
    - [ ] Add‑ons belong only to this activity.
- [ ] **Reviews:**
    - [ ] Aggregate rating and review count.
    - [ ] List of reviews (nickname, rating, text, optional photo). No login attached.

---

### 5. Booking Flow (No Sign‑up)

- [ ] **Step 1 – Configure add‑ons:** User selects one or more add‑ons and customisations. Total add‑on price shown.
- [ ] **Step 2 – “Book via Zalo” button:**
    - [ ] Opens Zalo deep link (or fallback web page) with a pre‑filled message containing:
        - Activity name, date/time
        - Selected add‑ons with customisations & prices
        - Total add‑on cost
        - User’s Zalo contact (auto‑filled if possible) or a placeholder “Your name: ___”
- [ ] **Step 3 – Backend booking record:** The platform creates a booking record with status `pending`. A unique review token is generated and stored with the booking. A reference ID is shown to the user for their records, but no account needed.
- [ ] **Step 4 – Provider confirmation:** Provider receives the Zalo message, then uses their dashboard to Accept or Reject within a configurable timeframe (default 60 minutes).
    - [ ] On Accept → status `confirmed`, spots reduce.
    - [ ] On Reject/timeout → status `rejected`, spots released, booking “closed”.
- [ ] **Step 5 – Completion:** Provider marks the booking as `completed` in their dashboard. This triggers the commission calculation and reveals the review link. Provider can then paste the review link into the Zalo chat with the user.
- [ ] **Cancellation:** User can request cancellation via Zalo. Provider can mark booking as `cancelled` in the dashboard, optionally noting a fee (platform does not enforce). Spots are released.

---

### 6. Reviews (Token‑Based, No Account)

- [ ] Review link format: `platform.com/review/{token}`.
- [ ] Page contains:
    - [ ] Activity name, add‑on used (read‑only)
    - [ ] Nickname field (free text)
    - [ ] Star rating (1–5)
    - [ ] Comment (optional, max 500 chars)
    - [ ] Optional photo upload
- [ ] Submit → stored with booking reference (no user ID). Displayed on activity detail instantly after a basic profanity filter.

---

### 7. Provider Dashboard

- [ ] **Profile:** Photo, display name, short bio, Zalo contact (hidden from public). No credentials, no verified badge.
- [ ] **Add‑on selection:**
    - [ ] Browse all add‑ons created by Ethos Studio, filtered by activity. Activate/deactivate them.
    - [ ] For each activated add‑on, set:
        - [ ] Base price (within platform‑defined range)
        - [ ] Customisation options and their +price
        - [ ] Cancellation policy text (informational only)
    - [ ] Pause/unpause any add‑on with one click.
- [ ] **Booking management:**
    - [ ] **Pending:** List of bookings needing Accept/Reject. Provider action updates status and spots.
    - [ ] **Confirmed:** Upcoming sessions (sorted by date).
    - [ ] **Completed:** History; provider marks a booking as completed here, which reveals the review link.
    - [ ] **Cancelled:** Historical list.
    - [ ] No calendar view; just sorted lists.
- [ ] **Earnings summary:**
    - [ ] Current week: total add‑on revenue from completed bookings.
    - [ ] Commission due (platform’s percentage).
    - [ ] No history beyond this week; no payout integration. Invoices handled externally by admin.

---

### 8. Admin Dashboard

- [ ] **Provider management:**
    - [ ] List, search, approve/reject new applications.
    - [ ] Suspend or deactivate providers.
- [ ] **Activity oversight:**
    - [ ] Enable/disable any activity manually.
    - [ ] Quick view of remaining spots, current status.
- [ ] **Booking dispute resolution:**
    - [ ] View all bookings; filter by status.
    - [ ] Manual override to force‑complete or cancel (with audit log).
- [ ] **Pulse metrics (single view):**
    - [ ] Activities live right now
    - [ ] Total bookings today
    - [ ] Completed bookings this week
    - [ ] Commission due this week (aggregate)
    - [ ] No drill‑downs, no exports.
- [ ] **Invoicing:** Generate simple weekly commission invoice per provider (PDF). Track payment status manually (paid/unpaid).

---

### 9. Ethos Studio (Content Creation)

- [ ] **Activity management:**
    - [ ] Create new activity:
        - [ ] Title, category.
        - [ ] Start time & end time (required).
        - [ ] Total capacity (spots).
        - [ ] Rich description.
        - [ ] Photo uploads (gallery).
        - [ ] Address (text) for display.
    - [ ] Edit, clone, archive activities.
    - [ ] Publish flow: Draft → Published.
- [ ] **Add‑on builder:**
    - [ ] Add‑ons are created inside an activity (activity‑specific).
    - [ ] Each add‑on definition:
        - [ ] Title
        - [ ] Description
        - [ ] Sample base price range (min‑max)
        - [ ] Customisation templates (e.g., “Extra 30 min: +X VND”)
        - [ ] Sample photos
    - [ ] Providers later adopt these and set their own prices/options.
- [ ] **No provider‑side customization here** – providers only see and enable/configure add‑ons defined by Studio.

---

### 10. Technical Constraints

- [ ] **Stack:** KMP shared logic, Compose web UI (Kotlin/JS), ArangoDB, Ktor server.
- [ ] **Time‑based filtering:** Server must compute “available now” per request using the server clock. No caching that would show expired activities.
- [ ] **Booking creation:** When a visitor taps “Book via Zalo,” the frontend sends a request to create a pending booking (with selected add‑ons) and returns the Zalo deep‑link message. The booking record is generated before the Zalo chat is opened.
- [ ] **Review token:** A securely random token (UUID) is stored with each booking. The review page checks token validity and if a review already submitted (one per booking).
- [ ] **No authentication layer** – the entire platform is accessible without login, except admin/studio/provider dashboards which use separate authentication (basic session or token).

---

### 11. Launch Checklist

- [ ] Activity creation pipeline: 10–20 initial D2 activities fully set up.
- [ ] Zalo deep‑link and fallback flows tested on real devices.
- [ ] Provider dashboard booking flow (pending → confirm → complete) works end‑to‑end.
- [ ] Review token flow works: provider shares link, user submits, review appears.
- [ ] Pulse metrics display correctly.
- [ ] Terms of Service live with liability disclaimers.
