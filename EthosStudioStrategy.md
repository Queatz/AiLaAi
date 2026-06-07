# Strategy: Converting AiLaAi to Ethos Studio

This document outlines the strategy to transform the existing AiLaAi social/chat platform into **Ethos Studio**, a hyper-focused marketplace for time-bound free activities with limited capacity and premium add-ons.

## 1. Core Architectural Shift

The transformation moves from a general-purpose social network to a specialized service marketplace. 

*   **From:** Account-centric social interactions (chats, signals, trades).
*   **To:** Activity-centric bookings with provider-driven add-ons.
*   **Visitor Access:** No sign-up required for browsing and booking.

---

## 2. Data Model Evolution (Shared Module)

We will leverage the existing ArangoDB/KMP structure but refocus the entities.

### New & Refocused Models
*   **Activity** (Extends/Replaces `Card`): 
    *   `startTime`, `endTime` (ICT timezone).
    *   `totalCapacity` (Int).
    *   `remainingSpots` (Computed or cached).
    *   `address` (String text).
    *   `photos` (List of URLs).
*   **AddOnDefinition**: 
    *   Created by **Ethos Studio** (Content team).
    *   Contains template info: title, description, base price range, customization options.
*   **ProviderAddOn**: 
    *   Provider's activation of an `AddOnDefinition`.
    *   Specific `basePrice` and `customizationPrices`.
*   **Booking**: 
    *   Fields: `activityId`, `providerId`, `selectedAddOns`, `status` (`pending`, `confirmed`, `completed`, `rejected`, `cancelled`).
    *   `reviewToken` (UUID) for account-less reviews.
*   **Review**: 
    *   Linked to `bookingId`. 
    *   Fields: `nickname`, `rating`, `comment`, `photo`.

### Deprecation Path
Existing modules like `Bot`, `Trade`, `Signal`, `Story`, and `Script` will be removed from the public UI to minimize bloat and focus on the MVP.

---

## 3. Backend Refactoring (Ktor & ArangoDB)

### Access Control
*   **Visitor Routes:** All activity listing, detail view, and booking initiation routes must be public (remove `authenticate` block).
*   **Dashboard Auth:** Maintain JWT authentication for **Providers**, **Admin**, and **Ethos Studio** users.

### Real-Time Filtering
*   The `GET /activities` endpoint must use the server clock to filter:
    *   `startTime <= now <= endTime`.
    *   `remainingSpots > 0`.
    *   `status == "published"`.

### Booking Logic
1.  **POST /bookings**: Validates availability, creates a `pending` record, generates a `reviewToken`.
2.  **Zalo Payload**: Returns a pre-filled Zalo deep-link string containing booking details.

---

## 4. Frontend Transformation (Compose Web & Android)

The UI will be overhauled for a mobile-first, "Available Now" experience.

### Visitor Interface (Public)
*   **Homepage:** A clean, real-time list of "Available Now" activities.
*   **Activity Detail:** 
    *   Interactive add-on selector.
    *   Dynamic price calculation.
    *   "Book via Zalo" primary action.
*   **Review Submission:** A dedicated route `/review/{token}` that allows nickname-based submission without login.

### Dashboards (Secured)
*   **Provider Dashboard:** 
    *   Focus on "Pending" booking queue for quick Accept/Reject.
    *   "Mark Complete" action which reveals the review link to be shared via Zalo.
*   **Ethos Studio Dashboard:** Content management for activities and add-on templates.
*   **Admin Dashboard:** Pulse metrics (Live activities, weekly commission) and provider vetting.

---

## 5. Zalo Integration Strategy

Since Zalo is the primary communication and payment channel:
1.  **Deep-Links:** Use `zalo.me/{phone}?text={encoded_message}`.
2.  **Fallback Page:** If the deep-link fails or for desktop users, provide a "Copy Message" utility page.
3.  **Booking ID:** Every Zalo message includes a unique `Reference ID` from the backend booking record for reconciliation.

---

## 6. Implementation Phases

| Phase | Focus | Key Deliverables |
|:--- |:--- |:--- |
| **1. Core** | Models & DB | ArangoDB schema updates, `Activity` and `Booking` models. |
| **2. API** | Public Access | Refactored Ktor routes, "Available Now" filtering logic. |
| **3. Web MVP** | Visitor UI | Homepage, Activity Detail, and Zalo deep-link flow. |
| **4. Dashboards** | Provider Flow | Booking management (Confirm/Complete) and earnings summary. |
| **5. Reviews** | Token Logic | Token-based review submission and display. |
| **6. Launch** | Polish & Vetting | Pulse metrics, invoicing, and launch checklist validation. |
