# Suqi — UI/UX Implementation Plan

> Scope: **UI/UX only** (screens, layout, components, states). No API / backend work is planned here.
> Covers only **non-existent or incomplete** features. Finished work is listed in the status tables so it is not re-planned.
> Audience: the three user types — **Buyers/Visitors**, **Sellers (Suqi shop owners)**, **Owner/Admin/Developer**.

---

## 0. Document status

| Question | Finding |
|---|---|
| Was there an implementation plan in the repo? | **No.** `README.md` is the AI Studio boilerplate; no plan/doc files exist. |
| Evidence a plan existed elsewhere | Code comments cite an external plan: `Plan §5.2, §6, §11, §12, §19 (Screens 01, 05, 11), P3 Operational Features` and `Checklist 04…24`. That document is **not in the repo**. |
| Action taken | This file is the new, repo-resident plan. It keeps those section ideas where they can be inferred but uses its own IDs (`F-`, `B-`, `S-`, `A-`). Add the original plan next to it if it can be recovered, and reconcile IDs. |

**Status legend**

| Icon | Meaning |
|---|---|
| ✅ | Done — functional **and** on the new design |
| 🟡 | Functional, but still on the **old visual style** (needs restyle only) |
| 🟠 | **Partial** — missing parts, placeholder actions, or local-only (not persisted) |
| ⛔ | **Missing** — screen does not exist |

**Priority**: **P0** blocks other work · **P1** core value · **P2** important · **P3** nice to have.

---

## 1. Codebase snapshot (what the audit found)

- **Stack**: Kotlin, Jetpack Compose (Material 3), single `MainActivity`, ~24.9k lines (3k are icon vectors).
- **Navigation**: custom back-stack in `SuqiViewModel` with a sealed `ScreenDestination` (**43 destinations**). No Navigation-Compose, no deep links, no shared transitions.
- **Data**: Room v2 with a built-in SQLite backend and seeded data; a Retrofit interface exists for a future server. *(Out of scope here.)*
- **Roles**: the model has `CUSTOMER / SELLER / ADMIN`, but **the UI never branches on role**. The "Admin Console" row in Account is visible to everyone, and there is a single implicit "current user".
- **Design system**: new tokens/components exist (`SuqiTokens.kt`, `SuqiProductCard`, `SuqiPromoCarousel`, `SuqiMediaGallery`, `SuqiMediaEditor`, `SuqiAttributeEditor`). **Only Home and Categories use them fully.** About **230 usages** of the old static light tokens (`SuqiSurface`, `SuqiSecondaryBackground`, `SuqiBackground`, `SuqiBorder`) remain across 14 screens (Seller Studio alone: 66).
- **Localization**: language picker exists, but there are **no `values-am` / `values-om` resources**; all UI strings are hard-coded English, so the picker is cosmetic.
- **Tests**: one small unit test file; no UI tests.

### Demo data / hard-coding to remove (found in code)

| Where | What |
|---|---|
| `CategoriesScreen` | Cart badge shows **2 when the cart is empty** |
| `SuqiViewModel` | Unread-notification badge starts at **3**; checkout recipient preset to "Fitsum Gebre" |
| `AccountHubScreen` | Avatar initials "FG" and fallback name hard-coded |
| `AuthOnboardingScreens` | Google sign-in uses a fixed name/email; Telegram sign-in is a toast |
| `SellerStudioScreens` | Dashboard "ETB 248,750"; backend adds fixed +248,750 / +2 pending; shop id `shop_techhub` hard-coded in 7 places |
| `OrdersScreens` | Delivery driver "Alemayehu T." hard-coded |
| `ProductDetailsScreen` | Share button only shows a "link copied" toast |
| `AdditionalScreens` | Admin categories, banners and moderation are **local state only** (nothing persists; banners never reach Home) |

---

## 2. Completion summary (estimates from the screen audit)

*Feature coverage* = how much of a complete marketplace scope for that role has a working screen. *Design alignment* = share of that role's screens already on the new design language.

| Role | Existing screens | Feature coverage | Design alignment | Biggest gaps |
|---|---|---|---|---|
| **Buyer / Visitor** | 22 | **~70%** | **~15%** (Home, Categories, card, gallery) | Payment-proof upload, search filters, profile, returns, real notifications, localization, guest gating |
| **Seller** | 12 | **~55%** | **~10%** (product form pieces) | Analytics, inventory, shop branding, reviews mgmt, earnings, returns, seller order detail |
| **Owner / Admin / Developer** | 7 | **~30%** | **0%** | Role gate, dashboard, users/shops/orders/disputes, finance, support, content, developer tools |

---

## 3. Design language (applies to every screen below)

**Direction**: modern minimalist, orange-on-cream, human and warm. Typography-led, generous white space, soft layered cards.

| Token | Value | Use |
|---|---|---|
| Page background | `#FFF8F0` (cream) | Every screen (light mode) |
| Card | `#FFFFFF` | Cards, sheets, inputs |
| Primary | `#FF6500` | CTAs, active states, highlights, prices |
| Soft orange | `#FFF0E6` | Selected tint, icon wells |
| Text / secondary | `#171717` / `#737373` | Titles / supporting copy |
| Peach | `#FFDFC7` | Banner/hero gradient end |
| Shop tint | `#F3ECFB` (border `#E4D8F5`) | Shop-related cards only |
| Radius | 24dp cards · 30dp hero/sheets · 999dp pills · 18dp image tiles | |
| Shadow | `suqiWarmShadow` (warm brown-orange, soft, 6–14dp) | Cards, circular buttons |
| Font | Plus Jakarta Sans (real weight files) | Bold titles, SemiBold buttons, Regular body |

**Reusable patterns** (build once in F-01, then reuse):
- **Header**: cream background, left title (20sp Bold), right **white 42dp circular action buttons** with warm shadow; back button same style.
- **Cards**: white, 24dp, warm shadow, 16dp padding. Never a visible border unless selected.
- **Chips**: pill, 12sp; selected = solid orange + white text; idle = white + 1dp border.
- **Buttons**: primary = solid orange pill 46–48dp; secondary = orange outline pill; destructive = red outline `#D92D20`.
- **Sheets/dialogs**: 30dp top radius, drag handle, one primary action.
- **Status pills**: success / warning / error / neutral (existing `SuqiStatusBadge`, restyled).
- **Empty state**: 96dp illustration well (soft orange circle + line icon), title, one-line help, one primary button.
- **Touch targets** ≥ 48dp; icon-only buttons must have a content description.

**Admin variant**: same language but **denser** — 16dp margins, list rows instead of big image cards, restrained orange (only for primary actions/active filters), semantic colors for status, tables become stacked list rows on phones.

---

## 4. Shared foundations (needed by all three roles)

### F-01 · Design-system rollout — **P0**
- **Status**: 🟠 tokens + 6 components exist; 14 screens still old.
- **Build**: shared scaffolds — `SuqiScreen` (cream page + header slot + snackbar offset), `SuqiHeader`, `SuqiSectionCard`, `SuqiEmptyState`, `SuqiSkeleton` (shimmer in 24dp rounded blocks), `SuqiBottomSheet`, `SuqiFilterChips`, `SuqiListRow` (icon well + title + sub + chevron), `SuqiSearchField` (white pill, warm shadow), `SuqiStepper` (qty), `SuqiStatCard`, `SuqiBarChart` / `SuqiLineChart` (orange line, peach fill, 3 gridlines).
- **Then**: replace old static tokens screen-by-screen; verify **dark mode** (cream → `#121212`, card → `#1C1C1E`, orange stays, shadows off, border on).
- **Done when**: no screen references the old static light tokens; every screen reads colors from `LocalSuqiColors`.

### F-02 · Role-aware shell & navigation — **P0**
- **Status**: ⛔ role never read by the UI.
- **Account hub** shows sections by role: *Buyer* (orders, wishlist, following, addresses, coupons, support) · *Seller* (Seller Studio card, only if role = SELLER; otherwise "Become a Seller" card) · *Admin* (Admin Console card, only for ADMIN roles) · *Developer* (hidden; appears after 7 taps on app version or in debug builds).
- **Role switcher** (sheet from avatar): "Shopping as Buyer / Managing Shop / Admin" with the current role marked by an orange check.
- **Seller shell**: Seller Studio gets its own 5-item bottom bar (Dashboard, Orders, Products, Inbox, More) using the same floating liquid bar styling; **Admin shell**: top segmented tabs (Overview, Queues, Manage, Tools).
- **Guards**: opening an admin/seller screen without the role shows the "Restricted" state (lock icon well, explanation, "Back to shop").

### F-03 · Auth & session UI — **P1** (see B-01 for the screens)
- Session states: *Guest*, *Signed-in buyer*, *Seller*, *Admin*. Guest sees "Sign in to continue" sheets (B-17); sign-out returns to Home as guest.

### F-04 · Localization (English / አማርኛ / Afaan Oromoo) — **P1**
- Externalize strings to `strings.xml` + `values-am` + `values-om`; make the language picker **apply immediately** (recompose) and persist.
- **Design checks**: Ge'ez script needs +2sp line height and no letter-spacing; allow chips/buttons to wrap to two lines; numbers and ETB stay Latin; date fields show the Gregorian date with an optional Ethiopian-calendar toggle (P3).
- Product text already has `nameAmharic/nameAfaanOromo` fields: show the localized name when present, fall back to English.

### F-05 · Loading / empty / error / offline states — **P1**
- Replace spinners with **skeletons** that mirror the final layout (card skeletons for grids, row skeletons for lists).
- Every list screen gets an empty state (pattern in §3). Add a global **offline banner** (peach strip under the header: icon + "You're offline — showing saved items" + Retry) and an **error card** with Retry.

### F-06 · Accessibility & responsive — **P2**
- Raise small icon buttons (34dp) to 48dp hit areas; verify 4.5:1 contrast for secondary text on cream; test at 130% font scale; cap content width at 600dp and center on tablets/foldables (grids go 2 → 3 → 4 columns).

---

## 5. Buyer / Visitor

### 5.1 Screen status

| Screen | Status | Notes |
|---|---|---|
| Onboarding (language, region) | 🟡 | Language choice not applied (F-04) |
| Auth (guest / Telegram / Google) | 🟠 | Providers mocked; no phone + OTP |
| Home | ✅ | New carousel, cards, cream page. Banners hard-coded (admin A-12 will feed them) |
| Categories | ✅ | New card design; cart badge demo bug |
| Category listing | 🟡 | Sort + sub-category + in-stock only; **no price/rating filters** |
| Search | 🟠 | Recent/popular + 2-option sort; **no filters, no live suggestions** |
| Shops list / Storefront | 🟡 | Collections tab is a toast; no shop info/policies sheet |
| Product details | 🟠 | New gallery, options, dynamic specs ✅; **missing similar items, Q&A, size guide, real share, rating summary** |
| Cart | 🟡 | Multi-shop groups, promo field |
| Checkout – delivery | 🟡 | Address picker works |
| Checkout – payment | 🟠 | **Transaction reference text only — no receipt screenshot** |
| Order submitted | 🟡 | |
| My orders / Order detail | 🟠 | Timeline, cancel, resubmit ✅; **no return/refund, invoice, reorder, tracking card is hard-coded** |
| Chat list / thread | 🟡 | Rich mentions; "Photo attached" is a placeholder |
| Notifications | 🟠 | Flat list only; no tabs/preferences |
| Wishlist / Following | 🟡 | |
| Saved addresses | 🟠 | List + add; **no edit/delete/map pin** |
| Reviews | 🟠 | Read + submit text; no photos, helpful votes or filters |
| Account hub | 🟠 | No profile edit; role sections missing (F-02) |
| App settings / Help & legal | 🟡 | Static content |

### 5.2 Planned screens (missing or incomplete)

#### B-01 · Phone sign-in & OTP — **P1**
- **Replaces/extends** `AuthScreen` (keep Telegram, Google, Guest as secondary options).
- **Layout**: cream page, Suqi logo mark top-left, headline "Welcome to Suqi" (28sp Bold, *Suqi* in orange), subtitle. Card with **+251 prefix chip** + phone field (formats `9XX XXX XXX`), full-width orange "Continue" pill. Divider "or", then three outline pills: Telegram, Google, Continue as guest.
- **OTP step** (same screen, slide transition): "Enter the 6-digit code sent to +251 9XX…", **6 separate 52dp rounded boxes** (active box orange border), auto-advance, paste support, resend timer ("Resend in 0:42"), "Change number" link.
- **States**: invalid number (red helper text), wrong code (boxes shake, red border), locked after 5 tries (warning card with countdown).
- **Done when**: both steps reachable, validation messages shown, guest path unchanged.

#### B-02 · Profile & Edit profile — **P1**
- **Entry**: tap avatar/name on Account hub.
- **Layout**: header "Profile" + Save. Top: 96dp circular avatar with orange camera badge (opens sheet: Camera / Gallery / Remove), name under it. Card "Personal info": name, phone (read-only with Verified pill), email (optional), gender chips, birthday (date picker). Card "Preferences": language, default city/sub-city (opens the existing location sheet). Danger zone: "Sign out", "Delete account" (red outline, confirm dialog with typed word).
- **Done when**: avatar initials come from the real name (no "FG"), edits reflect on Account hub.

#### B-03 · Search 2.0 (suggestions, filters, sort) — **P1**
- **Layout**: header = back + white pill search field with clear (×) and mic icon (P3). Below: horizontal chip row **Sort · Filter (count badge) · In stock · Free delivery · Verified shops**.
- **Typing state**: suggestion list (history icon rows, product-name rows with thumbnail, shop rows with shop-tint avatar), matched text bold.
- **Results**: toggle grid/list (icons top-right of the summary line "128 results"), grid uses `SuqiProductCard`, list uses a horizontal card variant (96dp image).
- **Filter sheet** (30dp top radius, shared with B-04): price range slider with min/max fields (ETB), category chips, brand chips (from item-type parameters), rating (4★+, 3★+), condition (New/Used/Refurbished), location (sub-city), delivery options. Footer: "Reset" text button + "Show N results" orange pill.
- **Sort sheet**: Best match, Price low→high, Price high→low, Newest, Top rated, Best selling (radio rows with orange dot).
- **States**: no results (empty state with "Clear filters" button + "Try these popular searches" chips).

#### B-04 · Category listing filters — **P2**
- Same filter/sort sheets as B-03, sub-category chips stay as the sticky second header row. Active filters appear as removable chips under the header.

#### B-05 · Product details completion — **P1**
Additions to the existing screen (gallery, options, specs stay):
- **Rating summary card** under the title: big score (36sp), 5-bar histogram (orange fill), count, "Write a review" outline pill, photo-review thumbnails strip.
- **Delivery & seller strip**: white card, store icon in shop tint, shop name + verified check, "Replies in ~1h", Chat / Visit shop pills, delivery estimate and fee line, "Verified payment protection".
- **Size guide** (only when a *Size* option exists): "Size guide" text button next to the selector → bottom sheet with a size table (rows = sizes, columns = measurements from the product's parameters; falls back to a generic apparel/shoe chart).
- **Questions & answers**: header "Questions (N)", two latest Q&As as cards (Q in orange bold, answer indented, seller badge), "Ask a question" outline pill → sheet with text field. Empty: "Be the first to ask".
- **Similar products** and **More from this shop**: horizontal rows of `SuqiProductCard` (160dp), section titles 18sp Bold + "See all".
- **Share**: real share sheet (product image card preview, copy link, Telegram, WhatsApp, "More…").
- **Report listing** (⋯ menu): reasons sheet (Wrong info, Fake/counterfeit, Prohibited item, Other + note).
- **Sticky bar**: price (with strikethrough), quantity stepper, Chat icon button, **Add to Cart** (outline) + **Buy Now** (solid) pills.

#### B-06 · Payment proof upload (Checkout – payment) — **P0 for trust**
- **Layout**: step indicator (Delivery ✓ — Payment — Done). Card 1 "Pay to": shop-tint card with provider logo well (Telebirr, CBE, etc.), account name, account/phone number with **Copy** pill, seller instructions, amount due in 28sp orange.
- Card 2 "Confirm your payment": transaction reference field (existing validation) **+ "Upload receipt screenshot"** dashed 120dp drop-zone (camera / gallery), thumbnail with remove (×) once added, optional payer phone/name.
- Footer sticky: total + "Submit order" pill (disabled until reference is valid; receipt optional or required per shop setting — S-08).
- **States**: uploading progress ring on thumbnail, invalid reference (inline red text), image too large (toast + compress).

#### B-07 · Order tracking, invoice & help — **P1**
- **Order detail additions**: (1) **Tracking card** — vertical timeline already exists; add estimated delivery chip ("Arrives Tue, 12 Nov") and, when dispatched, a courier card (avatar, name, call + chat buttons) populated from real data (remove the hard-coded driver); (2) **Receipt** button → full-screen invoice sheet (shop header, items, fees, total, payment ref, QR) with Share/Save PDF; (3) **Reorder** button (adds all in-stock items to cart); (4) **Need help?** row → issue sheet (Item missing, Damaged, Wrong item, Payment problem, Other) → creates a support ticket (A-16).
- Delivered orders show **Rate order** (opens B-11) and **Return item** (B-08) for 7 days.

#### B-08 · Returns & refunds — **P2**
- **Flow** (3 steps in one screen): choose items (checkbox cards with thumbnail) → reason (radio list + comment + up to 4 photos using the media editor tile style) → resolution (Refund / Replace) + summary → Submit.
- **Return status screen**: timeline (Requested → Approved → Item sent back → Refunded), seller note card, refund method line.
- **States**: window expired (neutral card explaining the policy), already requested (links to status).

#### B-09 · Notification center 2.0 — **P1**
- **Header**: "Notifications" + settings gear (white circle) + "Mark all read".
- **Tabs** (pill segmented, scrollable): All · Orders · Promotions · Messages · System; unread count dots.
- **Item**: 44dp icon well (soft orange; shop tint for shop events), title bold when unread, 2-line body, relative time, unread orange dot; swipe left = delete, swipe right = mark read. Grouped by Today / Yesterday / Earlier.
- **Preferences screen**: grouped switch rows (Order updates, Chat messages, Promotions & deals, Back-in-stock/price drops, Followed shops) with a "Quiet hours" time range row.
- **Empty**: bell illustration well, "You're all caught up".
- Header badge count must come from real unread data (remove the preset 3).

#### B-10 · Address book 2.0 — **P1**
- **List**: address cards (label chip Home/Work/Other with icon, recipient, phone, full address), **Default** orange pill, overflow menu (Edit, Set default, Delete). Primary FAB-style pill "Add address".
- **Add/Edit screen**: map preview card (static map tile with draggable orange pin — placeholder tile is fine for now) + "Use current location" outline pill; fields: label chips, recipient, phone (+251), region → city → sub-city selectors (existing pickers), street/landmark, delivery notes, "Make default" switch.
- **States**: delete confirmation dialog; address used by an active order shows "In use" and blocks delete.

#### B-11 · Reviews 2.0 — **P2**
- **Write review sheet**: product mini-card, 5 large tappable stars (orange fill) with label ("Great"), tag chips (Quality, Delivery, Packaging, Value), text field, photo tiles (reuse `SuqiMediaEditor` tile style, max 4), anonymous switch, "Submit" pill.
- **List**: filter chips (All, With photos, 5★…1★), sort (Newest, Helpful), review cards (avatar, name, stars, date, "Verified purchase" pill, text, photo strip, **seller reply** indented in shop tint, "Helpful (N)" button).
- **Empty**: "No reviews yet — be the first".

#### B-12 · Deals & flash sales — **P2**
- **Entry**: Home "Hot Deals" section header and carousel "Shop Now" banners.
- **Layout**: header with orange flame badge, **countdown chips** (HH : MM : SS in 44dp white rounded boxes) for the current sale, category chips, product grid using `SuqiProductCard` (discount badge prominent), "Sale ends" progress bar ("68% claimed") under price on limited-stock items.
- **States**: no active sale → "Next sale starts in…" card with reminder bell toggle.

#### B-13 · Wishlist 2.0 — **P3**
- Collections (All, + user-created lists as chips), price-drop badge ("↓ ETB 500"), back-in-stock badge, multi-select "Move to cart", share list.

#### B-14 · Shop storefront completion — **P2**
- **Header**: cover image with gradient, 72dp logo circle with white ring (crop-fill, same fix as Featured Shops), name + verified, rating, followers, Follow pill, Chat pill, ⋯ (Share, Report).
- **Info sheet** ("About this shop"): description, hours, address with map tile, delivery & pickup summary, return policy, payment methods icons, joined date.
- **Collections tab**: real collection cards (cover, name, count) opening filtered grids; remove the toast placeholder.
- Tabs underline style: Products · Collections · Reviews · About.

#### B-15 · Coupons & wallet — **P3**
- Account row "Coupons". **Tabs**: Available · Used · Expired. Coupon card = ticket shape (notched edge, soft orange left block with percent/amount, code, conditions, expiry, "Copy" / "Use now"). Cart promo field gets a "View coupons" link opening the same list as a sheet.

#### B-16 · Guest experience & gating — **P1**
- **Gate sheet** when a guest taps Add to wishlist / Checkout / Chat / Follow / Write review: illustration well, "Sign in to continue", primary "Sign in with phone", secondary Telegram/Google, "Maybe later".
- **Account hub (guest)**: replace the profile card with a sign-in card; hide order/seller/admin rows; keep Settings, Help.
- Cart stays usable as guest; sign-in is requested at checkout.

#### B-17 · Help center 2.0 — **P2**
- **Layout**: search field, 2×3 topic grid (Orders, Payments, Delivery, Returns, Account, Sellers) as soft-orange icon cards, expandable FAQ list, "Contact support" card (in-app chat with Suqi support, call, Telegram) and "My tickets" row with status pills.
- Legal pages (Terms, Privacy) as clean reading screens (18sp title, 15sp body, 1.5 line height, in-page table of contents).

#### B-18 · Settings completion — **P2**
- Add: notification preferences link (B-09), language **applied live** (F-04), data saver (don't autoload videos), clear cache, privacy toggles, "Delete account" (B-02), About screen (version, licenses). Keep theme selector as three preview tiles (System / Light / Dark).

#### B-19 · Compare products — **P3**
- Compare tray (floating pill "Compare (2)") on product cards; screen with horizontally scrolling product columns (image, name, price) and **aligned parameter rows** driven by the dynamic specs (rows only where at least one product has a value, differences highlighted in soft orange).

---

## 6. Sellers (Suqi shop owners)

### 6.1 Screen status

| Screen | Status | Notes |
|---|---|---|
| Become a seller (guided steps) | 🟡 | |
| Seller Studio dashboard | 🟠 | KPI numbers partly hard-coded; chart static; period dropdown inert |
| Add / Edit product | 🟠 | **Functionally complete** (media, item types, parameters, variants) but old styling; no draft autosave |
| Product management | 🟠 | List/search/status tabs, Edit ✅; **no pause/archive/delete/duplicate, no bulk, no quick stock edit** |
| Shop orders | 🟡 | Verify, dispatch, deliver, chat; **no order detail screen** |
| Payment verification queue | 🟡 | Approve/reject; **no receipt viewer** |
| Payment methods | 🟠 | Add/remove; "Edit" is a placeholder; shop id hard-coded |
| Delivery setup | 🟠 | UI-only, not connected to what buyers see |
| Promotions | 🟠 | Create/deactivate; no scope, schedule or performance |
| Shop settings | 🟠 | Text fields + vacation mode; **no logo/cover, hours, social, policies** |
| Seller verification | 🟠 | Requirements list + submit; **no document capture/status tracker** |
| Seller subscription | 🟠 | Plan card only; no upgrade/payment flow |

### 6.2 Planned screens

#### S-01 · Dashboard 2.0 — **P1**
- **Header**: shop logo circle + "Good morning, {Shop}" + notification bell (white circle) + shop status pill (Open / Vacation).
- **Onboarding checklist card** (until 100%): progress ring + 5 tasks (Add logo, Add payment method, Set delivery, Add 3 products, Verify shop); each row tappable, completed rows get an orange check.
- **KPI row** (horizontal, `SuqiStatCard` 150dp): Sales, Orders, Pending payments (peach warning tint), Low stock — each with delta vs previous period.
- **Sales overview**: period segmented control (7D / 30D / 90D), line chart (orange line, peach fill), tap a point for tooltip.
- **To-do list**: "3 payments to verify", "2 orders to dispatch", "1 unanswered chat" as action rows with counts and chevrons.
- **Top products** (3 rows) and **Shop health** card (response time, fulfilment rate, rating) with green/amber/red pills.
- **Quick actions** grid stays (restyled): Add product, Verify orders, Inventory, Promotions, Analytics, Shop profile.
- All numbers real (remove the fixed 248,750 and +2).

#### S-02 · Analytics & reports — **P2**
- **Header** with date-range chip (opens range sheet) and export icon.
- **Sections** (cards): Revenue (line), Orders by status (stacked bar), Views → Cart → Orders **funnel** (3 horizontal bars with conversion %), Top products (ranked list with sparkline), Customers by sub-city (horizontal bars), Payment methods split (donut).
- **Empty** (new shop): illustration "Your numbers will appear after your first order" + "Share your shop" button.
- Free plan shows 7-day data; older ranges show a soft-orange "Unlock with Pro" lock overlay linking to S-16.

#### S-03 · Inventory manager — **P1**
- **Layout**: search + filter chips (All, Low stock, Out of stock, Paused). Each row: 56dp thumbnail, name, variant summary ("6 variants"), **inline stock stepper** (− 12 +), low-stock amber pill, tap name → edit product.
- **Multi-select** (long-press): bottom action bar "Set stock", "Pause", "Mark out of stock".
- **Variant drill-down**: tapping a product with variants expands rows per variant (label, SKU, stock field, price override).
- **States**: no products → empty state with "Add your first product".

#### S-04 · Product management 2.0 — **P1**
- Keep list + status tabs (All, Active, Paused, Archived, Draft) and restyle rows (72dp thumbnail, name, price, stock chip, status dot).
- **Row overflow (⋯)** sheet: Edit, Duplicate, Pause/Activate, Archive, Delete (red, confirm dialog), View as buyer, Share.
- **Multi-select + bulk bar** (Pause, Archive, Delete, Change category, Apply discount).
- **Sort**: Newest, Best selling, Low stock, Price.
- **Add/Edit product (incomplete parts)**: draft autosave banner ("Draft saved 10:42 · Resume"), **preview as buyer** button (opens the details screen with the unsaved data), unsaved-changes dialog on back, per-section completeness chips (Media ✓, Details ✓, Price ✓), restyle fields to the new design.

#### S-05 · Seller order detail — **P1**
- **Layout**: header with order ref + status pill. Cards: **Buyer** (avatar, name, phone with call/chat buttons, address with Copy), **Items** (thumbnail, name, chosen options as chips, qty × price), **Payment** (method, reference, amount-match chip ✓/⚠, **receipt thumbnail** → full-screen viewer), **Timeline** (editable: mark verified/prepared/dispatched/delivered, optional note and courier details), **Notes** (private).
- **Sticky action bar**: contextual primary pill (Verify payment → Mark prepared → Dispatch → Mark delivered) + secondary ⋯ (Cancel order with reason, Print packing slip, Report issue).
- **States**: cancelled / rejected payment banner with reason; buyer resubmitted payment highlight.

#### S-06 · Payment verification 2.0 — **P1**
- Queue cards show **receipt thumbnail**, amount, reference, time waiting (amber after 6h), match chip. Tap → half-height sheet with large receipt image (pinch-zoom), details, **Approve** (orange) / **Reject** (red outline → reason chips: Wrong amount, Reference not found, Unreadable receipt, Other).
- Filters: Pending · Verified · Rejected; count badges.

#### S-07 · Shop profile & branding — **P1**
- **Header preview**: live mini storefront header (cover + logo) updating as the owner edits.
- **Media**: cover image (16:7 crop tool), logo (circle crop), gallery (up to 6 tiles, reuse `SuqiMediaEditor` tile pattern).
- **Info**: name, category (chips), description (with counter), phone, Telegram, Instagram/TikTok/website links, **opening hours** (7 rows, each with open switch + two time pickers; "Copy to all days"), address with landmark, policies (returns, delivery notes).
- **Share**: "Storefront link" row with Copy and a **QR code** card (Save / Share image).
- **Vacation mode** card (switch + return date + auto-reply message).
- Sticky "Save changes"; unsaved-changes dialog.

#### S-08 · Delivery & pickup setup polish — **P2**
- Make the three option cards (Standard, Express, Pickup) real toggles with fee, ETA window, cut-off time pickers; **coverage** as sub-city chips grouped by city with "Select all"; **free-delivery threshold** field; "Require receipt screenshot" switch (used by B-06).
- **"Preview as buyer"** card at the bottom showing exactly how the options look in checkout.

#### S-09 · Promotions 2.0 — **P2**
- **List** of promos as ticket cards (code, discount, scope, uses/limit bar, status pill, expiry) with performance numbers (orders, revenue).
- **Create wizard** (3 steps): Type (Percent / Fixed / Free delivery) → Scope (whole shop / categories / specific products with picker) and conditions (min order, per-customer limit, usage cap) → Schedule (start/end) + preview of the buyer-facing coupon card. 
- **Flash sale builder**: pick products, sale price, stock cap, time window; preview of the B-12 card.
- **Banner request** (P3): submit a creative to admin for Home placement.

#### S-10 · Reviews & Q&A management — **P2**
- **Tabs**: Reviews · Questions. Summary card (avg, histogram, response rate).
- **Review card**: stars, text, photos, product chip, **Reply** (inline text field with templates), **Report** (reasons sheet). Filters: Unanswered, Low rating, With photos.
- **Questions**: unanswered first, quick-answer sheet. Unanswered counts feed the dashboard to-do (S-01).

#### S-11 · Customers — **P3**
- Searchable list (avatar, name, orders count, total spent, last order), segment chips (New, Repeat, Inactive). Detail sheet: order history, notes field, **Chat** and **Send promo code** actions.

#### S-12 · Earnings & payouts — **P2**
- **Header card**: available balance (32sp), pending balance, "Request payout" pill (disabled below minimum).
- **Ledger**: grouped by week; rows for sales (+), commission (−), subscription fee (−), refunds (−), payouts; filter chips; each row opens a receipt-like detail.
- **Statements**: month list with Download/Share. Payout method row (bank / Telebirr) with edit.
- **Empty**: "No earnings yet".

#### S-13 · Returns & refunds handling — **P2**
- **Queue** (Requested · Approved · Completed · Rejected) with buyer, item thumbnail, reason chip, evidence photo count, age.
- **Detail**: reason, buyer's photos (gallery), order summary; actions **Approve** / **Offer replacement** / **Reject (reason required)**; timeline after decision.

#### S-14 · Seller inbox & notifications — **P1**
- **Inbox tab** in the seller bottom bar: unified list (chats + order alerts), filters (All, Unread, Orders, Questions), unread dots, order-context chips on chats.
- **Chat additions**: **quick replies** row above the keyboard (editable templates: "Payment received ✓", "Your order is on the way", "Item in stock"), send product/order cards, attach photos (real picker; remove the placeholder toast).
- **Notification settings**: new order, payment submitted, low stock, review, question — each a switch.

#### S-15 · Verification 2.0 — **P1**
- **Status tracker** top card: 4 steps (Submitted → In review → Decision → Verified) with the current step highlighted; verified state shows the blue-check badge preview.
- **Document capture**: tiles for ID (front/back), Trade license, Shop photo, Selfie with ID — each 120dp dashed tile → camera/gallery → thumbnail with retake; guidance text under each (e.g., "All four corners visible, no glare").
- **Rejected state**: reasons list from admin, only the failed tiles reopen; "Resubmit" pill.
- **States**: upload progress per tile, size/format errors.

#### S-16 · Subscription & billing — **P2**
- **Plan compare**: two cards (Free Pilot / Pro) stacked, benefits with orange checks, current plan marked, price in ETB with monthly/yearly segmented toggle.
- **Upgrade flow**: choose plan → pay (reuse B-06 pay-to card with Suqi's account, reference + receipt) → "Pending approval" state → success.
- **Billing**: invoices list (month, amount, status pill, download), next renewal date, cancel/downgrade link.

#### S-17 · Collections manager — **P3**
- List of collections (cover mosaic of first 4 products, name, count), reorder by drag handles, create/edit screen: name, cover (auto/mosaic or upload), product multi-picker with search, **visible on storefront** switch. Feeds B-14 Collections tab.

#### S-18 · Staff accounts — **P3**
- List of staff (avatar, name, role pill: Manager, Order handler, Catalog), invite by phone sheet, role permissions matrix (rows = capabilities, columns = roles with switches), remove with confirmation.

---

## 7. Owner / Admin / Developer

**Role tiers (one Admin shell, permissions differ)**
- **Owner**: everything, plus business dashboards and finance (A-02, A-13, A-14).
- **Admin (operations)**: queues, moderation, support, content (A-03 … A-12, A-15 … A-21).
- **Developer**: tools and diagnostics (A-22, A-23), can also *act as* test buyer/seller.

### 7.1 Screen status

| Screen | Status | Notes |
|---|---|---|
| Admin Console (quick actions + 3 toggles) | 🟠 | **Visible to every user**; no counts |
| Seller verification queue | 🟡 | Approve/reject persisted; no document viewer, reasons or history |
| Feature toggles (3) | ✅ | Chat, External contact, Subscription mode — persisted |
| Categories | 🟠 | Local state only; no sub-categories, images, reorder |
| Banners | 🟠 | Local state only; **never shown on Home** |
| Moderation (reviews) | 🟠 | Seeded demo reports, local state |
| Subscription plans | 🟠 | "Edit" shows a toast |
| App settings (shared) | 🟡 | |

### 7.2 Planned screens

#### A-01 · Admin sign-in & role gate — **P0**
- **Entry**: Account hub → Admin card (only for admin roles) or direct sign-in link in Help (hidden).
- **Layout**: dark-cream hybrid page: shield icon well, "Suqi Admin" title, email/phone + password (or OTP) card, "Remember this device" switch; **role badge** after sign-in (Owner / Admin / Developer).
- **Restricted state** for non-admin users (see F-02). Session timeout dialog after 15 min idle.

#### A-02 · Owner dashboard — **P1**
- **Top**: date-range segmented control, KPI cards (GMV, Revenue/commission, Orders, Active shops, New users, Conversion).
- **Charts**: GMV line with previous-period dashed overlay; orders by status stack; new users vs new shops dual bars; top categories donut; top shops ranked list.
- **Alerts card** (red/amber chips): pending verifications > 24h, flagged payments, open disputes, failed uploads.
- **Export** icon (CSV/PDF sheet).

#### A-03 · Admin console home 2.0 — **P1**
- Replace the quick-action tiles with **queue cards** carrying live counts: Seller verifications, Flagged reviews, Reported listings, Disputes, Support tickets, Payout requests; each card shows oldest-item age and a chevron. Below: "Quick tools" icon grid (Banners, Categories, Broadcast, Locations, Content), then the 3 existing feature toggles in a collapsible "Platform switches" card.

#### A-04 · Users management — **P2**
- Search + filter chips (All, Buyers, Sellers, Suspended, New this week). Row: avatar, name, phone, role pill, status dot.
- **Detail**: profile card, activity summary (orders, spend, reviews, reports against), actions (Suspend / Restore with reason, Reset sessions, View orders). Suspend is a red-outline pill with a confirmation dialog and a required reason.

#### A-05 · Shops management — **P2**
- Search + chips (Verified, Pending, Suspended, Pro plan). Row: shop-tint logo circle, name, owner, city, plan pill, rating.
- **Detail**: header like the storefront, tabs **Overview** (stats, verification status, plan), **Products** (counts, flagged), **Orders**, **Finance**, **Notes**. Actions: Feature on Home (switch with schedule), Suspend, Change plan, Message owner.

#### A-06 · Seller verification 2.0 — **P1**
- **Queue** with filter chips (New, Resubmitted, Waiting for info) and age badges.
- **Review screen**: left-to-right pager of submitted documents with pinch-zoom (reuse gallery), **checklist** (ID readable, name matches, license valid, shop photo authentic, address plausible) as checkboxes, applicant and shop info cards.
- **Decision bar**: Approve (orange) / Request changes (outline; pick failed items + note) / Reject (red outline; reason required). **History** card showing previous decisions.

#### A-07 · Listings moderation — **P2**
- Queue of reported/flagged products: card with image, name, shop, report reasons (chips) and count. Review screen uses the media gallery + specs preview; actions Keep, Request edit, Take down (reason), Suspend shop.

#### A-08 · Orders & disputes — **P2**
- **Orders search** by reference/phone/shop with status chips; read-only order detail (same layout as S-05, no seller actions) plus **Dispute** tab: case cards (buyer claim, seller response, evidence galleries) with resolution actions (Refund buyer, Side with seller, Partial, Escalate) and a mandatory note. Status timeline per case.

#### A-09 · Review moderation 2.0 — **P2**
- Persist decisions; tabs Reported · Hidden · Restored; bulk select; reason chips; side-by-side "Report" and "Review" cards; restore action.

#### A-10 · Categories manager 2.0 — **P2**
- **Tree list** with expand/collapse, drag handles to reorder, 48dp image/icon well per category, active switch.
- **Edit sheet**: names in English / አማርኛ / Afaan Oromoo, icon picker (Lucide set grid), cover image, sub-categories (chip editor), **linked item types** (multi-select from A-11), visibility.

#### A-11 · Item types & parameter templates manager — **P1**
- Manages the templates sellers pick in the product form (today they live in code).
- **List**: item types grouped by section with emoji, parameter count, usage count; "New item type".
- **Editor**: name + emoji + default category + section; **parameter list** (drag to reorder, switch enabled-by-default, "Required", "Buyers choose" flag), **Add parameter** (library search / create custom with the same type chips as the seller editor: Text, Number, Yes/No, Single, Multiple, Color, Date, Measurement), choices/units editor, section name.
- **Preview** pane: "How sellers see it" (renders the real seller parameter form) and "How buyers see it" (specs card).
- **Versioning note**: edits never remove values already saved on products (matches seller behavior).

#### A-12 · Banners & homepage curator — **P1**
- **List** of banners as mini carousel cards (image tile, badge text, headline with highlighted word, dates, status pill), drag to reorder, active switch.
- **Editor**: live **preview of the Home carousel slide** (exact `SuqiPromoCarousel` look), fields: badge + icon, headline with *highlight word* picker, subtitle, button text, **image upload with crop 4:5 tile**, destination picker (category / shop / product / deals page / URL), schedule (start/end), audience (all / new users / Addis only).
- **Home sections order**: reorder + show/hide Featured shops, New arrivals, Popular, Recommended, Recently viewed.
- Result must feed the real Home carousel (replaces hard-coded slides).

#### A-13 · Subscription plans & monetization — **P2**
- Plan cards (Free Pilot, Pro) with edit button opening a full editor: name, price (monthly/yearly), benefits list (add/remove/reorder), limits (products, promo codes, analytics days), "Most popular" flag, active switch.
- **Revenue card** (MRR, active subscribers) and **Pilot mode** switch with end date; list of **pending upgrade approvals** (receipt viewer + approve/reject).

#### A-14 · Finance: commissions & payouts — **P2**
- **Summary**: platform revenue, commissions, subscription revenue, pending payouts. **Commission rules** card (default %, per-category overrides). **Payout requests** queue (shop, amount, method, approve/mark paid with reference). **Ledger** with filters and CSV export.

#### A-15 · Notifications broadcast — **P2**
- **Compose**: audience selector (All, Buyers, Sellers, Shop followers, City), title, body (counter), optional image and destination, **live phone-style preview**, schedule (now / later), "Send test to me". **History** list with sent/opened counts. **Templates** (saved messages).

#### A-16 · Support inbox — **P2**
- **List**: tabs Open · Pending · Resolved, priority/age chips, requester role pill. **Thread**: chat-style with internal-note bubbles (peach), order context card, quick replies, assign-to, status changer, resolution summary.

#### A-17 · Locations manager — **P3**
- Region → City → Sub-city expandable tree, add/rename/disable, per-sub-city default delivery zone flag; used by location pickers and seller coverage chips.

#### A-18 · Payment providers directory — **P3**
- List of providers (Telebirr, CBE Birr, Awash, etc.) with logo upload, display name, **account-format rule** (digits length/prefix) and reference-format hint used by checkout validation; active switch; preview of the buyer "Pay to" card.

#### A-19 · Content manager — **P3**
- Edit Help topics, FAQs, Terms/Privacy, onboarding copy, empty-state texts. **Three-language tabs** (EN / አማ / Oro) with "missing translation" amber dots; Markdown-lite editor with preview; publish/draft status.

#### A-20 · Audit log — **P2**
- Chronological list: actor avatar + role, action sentence ("Admin Sara suspended shop *Tech Hub*"), target chip, timestamp; filters by actor/action/date; detail sheet showing before/after values.

#### A-21 · Reports & exports — **P3**
- Report cards (Sales, Users, Shops, Orders, Finance, Moderation) → configure (date range, filters) → preview table (stacked rows) → Export CSV/PDF/Share.

#### A-22 · Developer tools — **P2** (hidden behind the developer role)
- **Sections** as list rows: **Environment** (Built-in vs Remote backend switch, base URL field, test connection), **Data tools** (seed demo data, reset database, export/import DB snapshot, DB version/schema info), **Act as** (switch to a test Buyer / Seller / Admin session without signing out), **Feature flags** (raw list with switches; includes media limits and max variants), **Diagnostics** (app/version, device info, network/request log viewer with status chips, recent errors/crash list), **Design system gallery** (live catalog of buttons, chips, cards, sheets, charts, status pills in light/dark for visual QA), **Maintenance mode & minimum app version** (switches shown to users as a full-screen notice).

#### A-23 · Platform switches (Features 2.0) — **P3**
- Extend the 3 existing toggles into grouped switch cards: Commerce (chat, external contact, reviews, Q&A), Media (video uploads, max files, max size), Catalog (variants, custom parameters), Monetization (subscription mode, pilot end), Experience (flash sales, compare). Each switch has a one-line description and "Affects: Buyers / Sellers".

---

## 8. Roadmap

| Phase | Goal | Items |
|---|---|---|
| **0 · Foundations** | One design system, role-aware app | F-01, F-02, F-05, remove demo data (§1) |
| **1 · Buyer conversion & trust** | Safe, clear purchases | B-06, B-03/B-04, B-05, B-10, B-16, B-01, B-02, B-09, B-07 |
| **2 · Seller operations** | Run a shop daily | S-01, S-04, S-03, S-05/S-06, S-07, S-15, S-14 |
| **3 · Admin core** | Operate the platform | A-01, A-03, A-06, A-12, A-11, A-10 |
| **4 · Growth & money** | Revenue and retention | B-12, B-11, B-08, S-02, S-09, S-12, S-13, S-16, A-02, A-13, A-14, A-09, A-15, A-16 |
| **5 · Polish & long tail** | Reach and depth | F-03/F-04 completion, F-06, B-13, B-14, B-15, B-17, B-18, B-19, S-08, S-10, S-11, S-17, S-18, A-04, A-05, A-07, A-08, A-17 … A-23 |

**Dependencies**: F-01 before every restyle · F-02 before any seller/admin work · B-06 needs S-08 (receipt-required switch) and S-06 · B-12 needs S-09 (flash sale builder) · Home banners need A-12 · Category/templates need A-10/A-11 · Localization (F-04) should land before the long content screens (A-19, B-17).

---

## 9. Definition of Done for every screen above

1. Uses shared scaffolds and tokens (no old static colors); correct in light **and** dark.
2. Has **loading skeleton, empty, error, and offline** states.
3. Works with **English / አማርኛ / Afaan Oromoo** strings (no hard-coded text) and 130% font scale.
4. All touch targets ≥ 48dp, icon buttons labelled, contrast checked.
5. No hard-coded demo data; no toast-only placeholder actions.
6. Cards 24dp, hero/sheets 30dp, warm shadows; headers follow the shared pattern.
7. Visual review against the reference image set (carousel, product card, category card, shop card) before merge.
