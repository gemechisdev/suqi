# Suqi: UI/UX Implementation Plan (v3.11, frontend-first)

> Scope: **UI/UX only** (screens, layout, components, states). No API or backend work is planned here.
> Covers only **non-existent or incomplete** features. Everything the re-audit confirmed as done is listed once in §2 so it is not planned again.
> Audience: **Buyers/Visitors**, **Sellers (Suqi shop owners)**, **Owner/Admin/Developer**.
> **Mode: frontend-first.** Demo scaffolding (visible admin credentials, demo OTP, fake Telegram/Google sign-in, seeded data) is **intentionally kept** so every screen stays reachable. It is not planned for removal; see §3.
> Latest audit: `suqi__11_.zip` (61.0k lines, 85 Kotlin files, 64 `ScreenDestination` routes, Room **v10** with 29 entities, 82 `@Test` methods of which 6 are new and unrun). **Phase 5 is built but not yet closed**: static review found everything wired (routes, entry points, API, backend, strings in EN/AM/OM, migration 9→10), but nothing was compiled or run, and Phase 5 had no tests until `Phase5LogicTest` was added in this version. Close-out checklist in §9. Phases 0 to 4 stay closed.
> Method: full read of each uploaded codebase against this plan and the update notes, re-checked item by item after every phase (latest: `suqi.zip`).

---

## 0. What changed

### 0.0f Phase 5 built, pending close-out (v3.10 → v3.11)
Work done in AI Studio until the credit limit stopped it; reviewed statically only. Room v9 → v10 (`MIGRATION_9_10` registered, 7 new entities), 9 new routes (`ReturnRequest`, `ReturnStatus`, `Deals`, `SellerReturns`, `SellerEarnings`, `SellerAnalytics`, `AdminFinance`, `AdminSupportInbox`, `AdminBroadcast`), 12 new files (`SuqiPhase5Strings` with 152 keys complete in EN/AM/OM, plus the screens below), about 150 new repository / API / backend methods, all three layers in sync.
| Item | What is in the code |
|---|---|
| **B-08 + S-13 Returns** | `BuyerReturnsScreens` (request flow, status screen) and `SellerReturnsScreen`; Return item pill now opens the flow from order detail; backend allows one open request per order (a rejected one can be resubmitted), requires a reason note to reject, and notifies the buyer on every decision. |
| **S-12 + A-14 Earnings and finance** | `SellerEarningsScreen` (balance, payout account, payout request with minimum and balance checks) and `AdminFinanceScreen` (commission rules, payout decisions with required reference on Mark paid). Returns feed the seller balance. Replaces the "Payout requests" queue stand-in. |
| **S-16 + A-13 Subscription and plans** | `SellerSubscriptionV2Screen` (compare, upgrade with receipt, cancel) and `AdminPlansV2Screen` (plans, pilot mode and end date, upgrade approvals). The old screens stay as private `Legacy…` functions behind wrappers. |
| **S-09 + B-12 Promotions and deals** | `PromotionsV2Screen` (scope, usage caps, flash sales) and `BuyerDealsScreen` (countdown chips, entry from Home Hot Deals). Cart promo codes are now validated by the backend (`validatePromoCode`: active, usage cap, minimum spend, product / category scope); the hardcoded `SUMMER25` / `TECH10` stay only as a fallback when no validator is passed. |
| **S-02 Analytics** | `SellerAnalyticsScreen` with 7 / 30 / 90 day ranges, cached per range. |
| **B-11 + A-09 Reviews 2.0** | `ReviewsV2Screen` (star filter, helpful votes per user, seller reply display) and `AdminModerationV2Screen` (tabs, bulk actions). |
| **A-16 + A-15 Support and broadcast** | `AdminSupportInboxScreen` (ticket messages, internal notes, status / priority / assignee) and `AdminBroadcastScreen` (compose, history, templates, scheduling). |
| **Tests** | `Phase5LogicTest` (6, **not run**): return request, duplicate block and seller decision rules; non-delivered order refused; payout minimum and balance; promo validation; helpful toggle and reply rules; ticket messages. |

**Findings and stand-ins (Phase 5)**
| # | Finding | Impact / fix |
|---|---|---|
| P5-1 | **Nothing compiled or run.** About 10k new lines were produced without a Gradle build in this audit; the previous AI Studio session was cut off while editing `AdminConsoleScreen` / `MainActivity` (both edits turned out to be present). | Run `gradle :app:testDebugUnitTest` first; fix compile errors before anything else. |
| P5-2 | **No Compose UI tests for the 9 new routes** (DoD #7 and the entry criterion). Only backend rules are covered. | One state-changing Compose test per new screen. |
| P5-3 | **Return window uses order `createdAt`**, not delivery time, and an unparseable date allows the return. | Store a `deliveredAt` when status becomes Delivered. |
| P5-4 | **No server-side role or ownership checks and no state machine.** `decideReturnRequest`, `decidePayoutRequest`, `replyToReview`, bulk moderation and broadcasts do not check who calls them, and any action can jump to any status (for example Complete refund from Requested, Mark paid from Pending). Only the UI gates them. | Acceptable frontend-first; enforce when the API lands, and add allowed-transition tables now. |
| P5-5 | **Analytics "cart adds"** comes from a placeholder extension (`CartDao.getAllUsersPlaceholderCount`), not real events. | Label as estimate in the UI or track real cart events. |
| P5-6 | **Cart fallback promo codes** remain in `CartScreen` for the case without a validator. | Remove once every caller passes the validator. |

### 0.0e Per-user chats, wishlist and following (v3.9 → v3.10)
Backend and data change only; no new screens or routes. It closes the "Delete wipes are not per-user" stand-in from §0.0d.
| Item | Result |
|---|---|
| **Room v8 → v9** | New tables `wishlist_items` and `shop_follows` (primary key user + product / shop) and `chat_messages.customerUserId`. `MIGRATION_8_9` is registered; it copies existing wishlist and follow flags and existing chat rows to the seeded buyer `user_fitsum` and renames the two seeded conversation ids to `conv_<shop>_user_fitsum`. The old `isWishlisted` / `isFollowing` columns stay on products and shops but are no longer the source of truth. |
| **Wishlist and following** | Toggle, product and shop lists, details, the FOLLOWING filter and pending shops all read the signed-in user's rows; seeded data is stored as rows for `user_fitsum`. Deleting a product removes its wishlist rows; a new, duplicated or seller-edited product no longer carries a global flag. |
| **Chat conversation ids** | Conversation id is now `conv_<shop>_<customerUserId>` (one thread per buyer per shop). `getChatMessages`, `markChatConversationRead` and `sendChatMessage` accept an optional `conversationId`; the API service, repository, `ChatThread` route, buyer list, seller inbox and `MainActivity` pass it through. Buyers see only their own conversations; a seller in Seller mode sees every conversation of the active shop, and replies go to the chosen conversation. Messages are ordered by insertion (`rowid`) instead of the display-string timestamp. |
| **Delete account** | Now per-user: removes the user's wishlist rows, follow rows and chat messages (by `customerUserId`) and decrements `followersCount` on followed shops. The three global wipes are gone. |
| **Tests** | `Phase4LogicTest` has a new isolation test (Buyer A's wishlist, follow and chat are invisible to Buyer B; the seller sees both threads and a reply with a `conversationId` reaches only that buyer) and the delete test was adapted; `Phase4ComposeUiTest` uses a waiting helper for sign-in; `ExampleRobolectricTest` clears the `FileProvider` cache between tests. Unit tests now run with a 2 GB heap (`app/build.gradle.kts`). Database and repository singletons are keyed by `filesDir` so each Robolectric test gets a fresh instance (no effect in the app). Total 76 `@Test` methods. |

**Notes and stand-ins (v3.10)**
| Note | Details |
|---|---|
| **Migration attribution** | Existing wishlist, follows and chats are assigned to `user_fitsum`, not to whoever used the device. Fine for demo data; a real user's pre-v9 wishlist and chats would be shown to the seeded buyer instead. |
| **Guest bucket** | Guests share one `user_guest` id for wishlist and follows, and nothing merges it into an account on sign-in. Acceptable while the guest gate asks users to sign in first. |
| **Seller reply fallback** | A seller message sent without a `conversationId` goes to the shop's most recent conversation. The UI always passes the id now; keep the fallback only until callers are confirmed. |
| **Dead columns** | `products.isWishlisted` and `shops.isFollowing` are unused; drop them in the next schema change. |

### 0.0d Phase 4 carry-overs closed (v3.8 → v3.9)
The two carry-overs from §0.0c were fixed. No new screens or routes were added.
| Item | Result |
|---|---|
| **C-2 Delete account** | `deleteAccount()` now refuses guests and staff, and refuses accounts that own a shop (localized message). Otherwise it deletes cart, saved addresses, notifications and support tickets, clears wishlist and following flags and chat messages, and **anonymizes** reviews and orders (`deleted_user`, name, phone, address and payer id blanked). The dialog text (EN/AM/OM) now states exactly this. After success the UI also clears the local ticket-thread store, then goes through `signOut()`. |
| **C-1 Tests** | `Phase4ComposeUiTest` grew from 6 to 11: notification mark read and delete, storefront About tab (hours and open status), invoice Save and Share create outputs, address In-use badge blocks delete while an unused address deletes, ticket thread reply appends user and support messages; the delete test now clicks confirm and asserts a guest session, a fresh nav stack and the user row gone. `Phase4LogicTest` grew from 7 to 9: full delete-cleanup test and the shop-owner block. Total 75 `@Test` methods. |

**Stand-in from C-2 (closed in v3.10, see §0.0e)**
| Note | Details |
|---|---|
| **Delete wipes are not per-user** | `chat_messages` has no user column, and wishlist / following are single flags on `products` and `shops`, so `deleteAllMessages()`, `clearAllWishlist()` and `clearAllFollowing()` clear them for everyone on the device, including conversations in a seller's inbox. Acceptable for the one-user-per-device demo model; make them per-user when the API lands. |

### 0.0c Phase 4 closed (v3.7 → v3.8; carry-overs closed in §0.0d)
Phase 4 (Buyer completion: `B-09`, `B-14`, `B-07`, `B-02`, `B-10`, `B-18`, `B-17`) was audited in code. All seven items are built, reachable and localized (new `SuqiPhase4Strings` catalog with EN/AM/OM, wired as `buyerWork`; three new routes `NotificationPrefs`, `AboutApp`, `TicketThread`). Two carry-overs remain (test debt and delete-account cleanup), see below.
| Item | Result |
|---|---|
| **B-09 Notifications** | Done (`BuyerNotificationScreens`): Today / Yesterday / Earlier sticky headers, `SuqiSwipeRow` (left = delete, right = mark read) with the 48dp delete icon kept as fallback, System tab with per-type unread dots, preferences screen with five `SuqiSwitchRow`s and quiet hours via `SuqiTimeField`, stored locally in `BuyerPreferencesStore`. Reachable from the notification header and from Settings. |
| **B-14 Storefront** | Done (`ShopsScreen` + `BuyerStorefrontAbout`): `SuqiTabs` now Products, Collections, Reviews, **About**; ⓘ in the header; About shows Open now / Closed pill from `businessHoursJson`, 7-day hours, delivery and payment summary, return policy (with empty-state text), gallery and social links; ⋯ menu with Share and Report shop (files a support ticket). |
| **B-07 Order completion** | Done (`BuyerOrderExtras`, used in `OrdersScreens`): estimated-delivery chip ("Arrives …" from order date plus the upper bound of the shop ETA), invoice sheet with items, fees, discount, total, payment and reference, `SuqiQrCard` with the order ref, PNG **Save** (Pictures) and **Share** (`FileProvider`). **Return item** pill shows on delivered orders but is disabled with a reason until B-08 ships. |
| **B-02 Profile** | Done (`AccountHubScreen`): green Verified pill on the phone row, default-location row opening the region/city/sub-city sheet, Sign out and **Delete account** (dialog, must type the localized confirm word, red confirm pill disabled until it matches). Delete calls `repository.deleteAccount()`, clears buyer prefs and search history, then goes through `signOut()` so caches and the nav stack reset. |
| **B-10 Addresses** | Done (`AdditionalScreens`, `BuyerAddressExtras`): map placeholder tile with pin note, **Use current location** (last known fix, graceful when there is no permission or fix), **In use** pill and blocked delete with explanation; the backend `deleteSavedAddress` enforces the same rule. |
| **B-18 Settings** | Done (`BuyerSettingsScreens`): theme as preview tiles, link to notification preferences, data saver (read by `SuqiMediaGallery` to stop video autoload), clear cache with live size (`cacheDir` and Coil memory cache; never touches `filesDir` uploads) plus `clearUserCaches()`, two privacy toggles that are enforced (`recordProductView`, `SearchScreen` history), About screen (version, licenses, privacy row), simulator and diagnostics visible only after developer unlock (7 taps on the version). |
| **B-17 Help 2.0** | Done (`BuyerHelpScreens`): search field, 2×3 topic grid filtering FAQ articles, contact card (in-app message, call via `ACTION_DIAL`, Telegram), My tickets opening a chat-style `TicketThreadScreen` with status pill and reply box, legal documents with table of contents and 15sp / 22.5sp body text. |
| **Tests** | `Phase4LogicTest` (7): day grouping, shop open-now, address in-use rule, ETA date, buyer prefs round trip, byte formatting, Phase 4 string catalog (no missing translations, format specifiers kept). `Phase4ComposeUiTest` (6): preference switch persists, quiet hours reveal time fields, data saver and privacy toggles persist, simulator hidden until developer unlock, help topic filters articles, delete needs typed confirmation. |

**Carry-overs (closed in v3.9, see §0.0d)**
| # | Finding | Fix |
|---|---|---|
| C-1 | **Compose test debt vs the Phase 4 entry criterion** ("one state-changing action per screen"). Not covered: notification list (swipe or icon delete, mark read), storefront About tab (tab switch, Open now pill), invoice sheet (Save / Share), address In-use pill and blocked delete, ticket thread reply. The delete-account test stops at "confirm becomes enabled"; nothing asserts the user is removed or the session resets. | Add one Compose test each for the five screens above, plus a ViewModel/backend test that `deleteAccount()` followed by `signOut()` leaves a guest session and empty caches. |
| C-2 | **`deleteAccount()` only removes the user row and the cart** (`BuiltinSqliteBackend`). There are no foreign keys, so the user's saved addresses, notifications, wishlist, chats, reviews and orders stay behind, and a seller who deletes their account leaves an ownerless shop. | Delete (or anonymize) the user's saved addresses, notifications, wishlist and chats; keep orders and reviews as anonymized history; block deletion, or require closing the shop first, for accounts that own a shop. Document the rule in the dialog text. |

**Known stand-ins & audit notes (Phase 4)**
| Note / Stand-in | Details |
|---|---|
| **Unused preferences** | Buyer notification switches and quiet hours are persisted but nothing consumes them (same status as the seller switches); wire when a notification dispatcher exists. |
| **Device-wide stores** | `BuyerPreferencesStore` and `TicketThreadStore` are plain `SharedPreferences`, not per account; they are wiped only on account deletion, not on sign-out, so a second account on the same device sees the first one's notification and privacy settings and ticket replies. |
| **Verified pill** | Shown whenever the phone field is non-empty; there is no real verification state yet. |
| **Map tile and location** | Address map is a placeholder tile; "Use current location" reads the last known fix only (no active location request). |
| **Return item** | Disabled stand-in with reason until B-08 (Phase 5). |
| **Invoice PNG** | Header text is `"Suqi · <localized title>"` drawn on a canvas with the default typeface; check Amharic / Oromo rendering in the manual QA pass. |
| **Report shop** | Creates a support ticket with an English subject (`"Report shop: …"`); acceptable as data, localize if admins need to read it in another language. |

### 0.0 Persistent Navigation State & Session Cleanup (v3.6 note)
| Item | Details |
|---|---|
| **Unique nav IDs & `SaveableStateHolder`** | `SuqiViewModel` tracks a stack of `NavEntry(id, destination)` entries with unique `nav_N` IDs; `SuqiAppRoot` wraps screens in `SaveableStateHolder.SaveableStateProvider(entry.id)` so revisiting the same destination type (e.g., product A → B → A) keeps separate saved UI/scroll state while root tabs (Home, Seller Studio, Account) reuse their entry ID across tab switches. |
| **Disposal & sign-out cleanup** | Popped entry IDs are queued in `disposedNavIds` and purged via `SaveableStateHolder.removeState`. `clearUserCaches()` clears all in-memory `cached*` ViewModel fields, recently viewed, unread badge, and active checkout state whenever `refreshSession()` detects a user ID change; `signOut()` clears caches and resets the nav stack to a fresh root entry. |
| **Process-death limitation** | Navigation stack entries and in-memory screen caches live in `SuqiViewModel` and are **not** persisted across OS process death (`SavedStateHandle`); they survive configuration changes (rotation) and normal in-app back/tab navigation, whereas after process kill the app restarts on Home. |
| **Verified (v3.7)** | `clearUserCaches()` resets all 36 `cached*` fields plus recently viewed, unread badge, guest gate, admin console tab and every active-checkout field. A `sessionRefreshGeneration` counter discards results of an overtaken refresh. Account Hub and the other sign-out entry go through `signOut()`; the auth screen's "continue as guest" and the dev-tools role switcher call `loginAsGuest()` then `refreshSession()`, so the id-change check still clears caches (the dev-tools switch to Guest keeps the nav stack, acceptable for a dev tool). |
| **Test-harness note** | The five loaders that needed it (ShopSettings, DeliverySetup, SellerStudio, SellerOrders, SellerOrderDetail) now call one documented helper, `applyOnMain` in `util/UiThread.kt`, instead of inline `withContext(Dispatchers.Main)`. It works around `UnconfinedTestDispatcher` resuming on IO threads and is a no-op in the app. Remove it once tests inject a standard dispatcher for the backend's IO work; other loaders keep the plain pattern. |
| **Tests** | `NavigationBackstackTest` adds 9 ViewModel/session unit tests (including `clearUserCaches()` on account switch and `signOut()` nav-stack/cache reset) and 4 end-to-end Compose/Robolectric back-stack & scroll preservation tests. |

### 0.0a Phase 3 closed (v3.5 → v3.6)
Phase 3 (Seller daily work: `S-03`, `S-04`, `S-07`, `S-14`, `S-08`) was audited and the two remaining findings were fixed before closing:
| Item | Result |
|---|---|
| **S-03 Inventory manager** | Done (`SellerInventoryScreen`: 3 entry points, SKU search, counts on filter chips, 56dp thumbnail, low-stock pill, variant row expansion with SKU/stock/price editors, steppers disabled for variant products, long-press bulk bar with Set stock / Pause / Out of stock; bulk Set Stock clarifies "(per variant)" when variant products are selected). |
| **S-04 Product management** | Done (Draft tab, sort sheet, duplicate, share, view-as-buyer, bulk pause/archive/category/discount/delete with confirmation; `originalPrice` preserved; drafts/paused excluded from buyer `getProducts`; Add/Edit unsaved-changes guard, completeness chips, buyer preview, autosaved draft cleared on publish). |
| **S-07 Shop profile & branding** | Done (live header preview, 7-day opening hours + Copy to all days, up to 6 gallery photos via `SuqiImagePickerTile`, cover/logo/gallery all imported into `filesDir/suqi_uploads/` so picker access never lapses, social links & policies with counters, real scannable ZXing QR code in `SuqiQrCard` with PNG Save to Pictures/SAF and PNG Share via `FileProvider`). |
| **S-14 Inbox & chat** | Done (filter tabs with counts & unread dot, persisted read/unread via `markChatConversationRead`, order-context chips, editable persisted quick replies, order attachment picker & cards, persisted notification settings sheet). |
| **S-08 Delivery polish** | Done (cut-off pickers via `SuqiTimeField`, grouped coverage chips with Select all / Clear, buyer checkout preview card). |
| **Data & tests** | Room v7→v8 migration registered, codecs and `ensureSeeded()` guards verified, Phase 3 backend/Compose tests plus ZXing QR decode round-trip, `FileProvider` URI, and `filesDir` image import tests passing (`gradle :app:testDebugUnitTest`). |

**Known stand-ins & audit notes (Phase 3)**
| Note / Stand-in | Details |
|---|---|
| **Drafts** | Autosaved product draft sits in one device-wide `SharedPreferences` slot (`SellerPreferencesStore`), not per account, and is written only for new products, not edits. |
| **Set stock (variants)** | Bulk Set Stock on a product with variants sets every variant to N, so total stock becomes N × variants; the dialog label displays "Stock units (per variant)" when any selected item has variants. |
| **Unused settings (stand-in)** | Seller inbox notification switches and delivery cut-off times (`standardCutoffTime`, `expressCutoffTime`) are persisted and shown in previews, but nothing reads them operationally yet (acceptable for frontend-first; wire when notification dispatcher / checkout cut-off enforcement lands). |
| **`shop.productCount`** | Counts drafts, paused, and archived products in addition to active listings. |
| **Compose UI tests** | Phase 3 Compose UI tests are mostly smoke checks (screen renders, sheet opens, selection toggles); stepper increments, bulk-apply mutations, and save paths are verified in backend/unit tests (`ExampleRobolectricTest`). |

### 0.0b Phase 2 closed (v3.4 → v3.5)
Phase 2 (Operate the platform) was audited twice. The first audit found five gaps; all five were fixed and re-verified in code.
| Item | Result |
|---|---|
| **A-02 Owner dashboard** | Metrics are now real: orders, users and shops are filtered by `createdAt` against the 7D / 30D / 90D / All window; previous-period GMV and buckets come from real prior-window orders; top categories from `itemsJson` line items; shop sparklines from real bucketed GMV; conversion = distinct ordering buyers ÷ non-guest accounts. Room v7 adds `createdAt` to users and shops. Alerts open the console Queues tab. |
| **A-12 Banners** | Audience (All / New users / Addis only) is applied on Home together with start/end dates. |
| **A-03 Console** | Platform-switch subtitles are localized (EN/AM/OM). |
| **A-10 Categories** | Shop Types tab shows live read-only shop counts per type. |
| **DoD #7** | 5 Compose UI tests (one per Phase 2 screen) plus a windowing/audience backend test. 31 tests total. |
| **Plan sync** | S-06 closed (it shipped in Phase 1); §4 component table, H-2, H-5, SwitchRow/ListRow and the §1 summary refreshed. |

**Known stand-ins (kept on purpose, to be replaced when the owning item lands)**
| Stand-in | Replaced by |
|---|---|
| "Reported listings" queue = flagged reviews whose reason mentions a listing | A-07 listings moderation |
| "Payout requests" queue = orders pending payment verification | A-14 finance / payouts |
| Owner alert "open disputes" = open tickets + reported reviews; "flagged payments" opens the Queues tab | A-08 disputes, A-14 |
| Platform revenue treats every verified shop as paying the active plan price | A-13 plans / A-14 |
| Shop Types tab is read-only (type matched by name) | Add editing only if a real need appears |
| Seed timestamps are relative strings ("3 days ago") re-resolved at read time, so seeded history slides forward; unparseable `createdAt` counts as "now" | Real timestamps once the API lands |
| "All" range has no previous period, so no delta is shown | Intended |


### 0.1 Localization completed (v3.3 → v3.4)
The remaining English was found by scanning every screen and component, then fixed in code (delivered as `suqi_localization_patch.zip`, 19 files; apply over `suqi_cc553f`).
| Fixed | How |
|---|---|
| ~73 leftover strings: OTP lockout and mismatch messages, welcome toast, onboarding placeholders, report-listing reasons, seller product-form toasts, reject reasons, VAT, banner editor labels, shop-type cards, version line, developer-unlock toasts, "Just now", content descriptions, guest-gate action phrases (checkout, follow, chat, wishlist), default shop description | New `SuqiExtraStrings.kt` (73 entries × EN/AM/OM), nested in `SuqiStringsData.extras`, so the existing catalog test covers it |
| Backend/service messages (H-7) | New `SuqiApiMessages.kt`: ~35 regex rules map English service messages to AM/OM, including ones with names, counts and references. `SuqiViewModel.showToast` now localizes every toast automatically; `localizeMessage()` is used for inline errors (checkout, admin sign-in). Unknown text (a future server message) passes through unchanged |
| Place names | New `SuqiPlaces.kt` with all regions, regional hubs and 12 Addis sub-cities in AM/OM; `placeLabel()` applied on Home location bar and sheet, Settings location, Addresses, Shops, Chat, Onboarding chips, Admin verification. Stored values stay English |
| Delivery titles and ETAs | New `SuqiDeliveryText.kt`: stored English titles and ETAs ("Standard Courier Delivery", "1-3 business days", "Same day") display translated in Orders, Product details and Seller order detail |
| Tests | 3 new tests: place names (including every sub-city and hub offered in the app), API-message localization with pass-through, delivery title/ETA localization. Existing catalog test now also guards `extras` |

**Not compiled here.** Gradle is not available in my environment, so I checked the patch statically (balanced syntax, `strings` in scope at every use, imports). Please run `./gradlew :app:testDebugUnitTest` once and tell me about any compile error.

**Intentionally left as-is**: brand name "Suqi", format placeholders (`+251 9XX…`, `YYYY-MM-DD`, `TXN8492041`, URLs), the demo credential hint, developer-only diagnostics, seed/sample data, "Popular searches" demo terms, and clipboard labels.

### 0.1a Previous upload (v3.2 → v3.3)
This upload is a **localization sweep**. No new screens or destinations were added (still 49 routes).
| Area | Now in code | Plan impact |
|---|---|---|
| **F-04 localization** | `SuqiStrings.kt` grew from ~540 to ~3,600 lines (about 850 string entries, 8.8k Ethiopic characters). Nearly every screen now reads from `LocalSuqiStrings`: Chat, Shops, Following, Wishlist, Notifications, Addresses, Reviews, Profile, Onboarding, **all Seller Studio, Seller verification, Admin console, Admin verification, Developer tools**, plus shared components (attribute editor, media editor, product card, promo carousel). The catalog test (`findMissingTranslations`) guards all three languages. | F-04 is **essentially done**. Only a small leftover list remains (see §3.2 and F-04). |
| **Localized content** | Category and product names and product-template subtitles now have Amharic/Oromo variants and are used on Home, Search, Product details, Shops, Chat, Seller screens | Done. |
| **Filters** | `SuqiRangeSlider` now drives the price range in **Search** and **Category listing** | Quick win done. |
| **Everything else** | No change in features | Plan items below are unchanged. |

### 0.1b Previous upload (v3.1 → v3.2)
| Area | Now in code | Plan impact |
|---|---|---|
| **F-07 shared components** | `SuqiExtendedComponents.kt` (1,556 lines) adds `SuqiTabs`, `SuqiSwitchRow`, `SuqiDonutChart`, `SuqiFunnelBars`, `SuqiSparkline`, `SuqiRangeSlider`, `SuqiStepIndicator`, `SuqiTimeField`, `SuqiDateField`, `SuqiCountdownChips`, `SuqiCouponTicket`, `SuqiSwipeRow`, `SuqiDragHandleList`, `SuqiImagePickerTile`, `SuqiDocumentViewer`, `SuqiQrCard` | F-07 **done**. 10 of 16 are still unused (see §4); `SuqiRangeSlider` has since been wired into filters. |
| **F-04 localization (first pass)** | `ui/i18n/SuqiStrings.kt` (105 keys × EN/AM/OM), `LocalSuqiStrings` provided in `Theme.kt`, live switch, persisted language, Ge'ez line-height handling, `values-am`/`values-om`, a unit test that fails on missing translations | Infrastructure done (coverage finished in v3.3). |
| **B-01 auth polish** | 6 OTP digit boxes, paste, shake on wrong code, 30 s lockout, resend countdown, change number | **Done.** Removed from plan. |
| **S-15 Verification 2.0** | `SellerVerificationScreen.kt`: status tracker, 5 document tiles (`SuqiImagePickerTile`), re-upload only failed docs, badge preview, activity list, `VerificationDocsCodec` | **Done.** Removed from plan. |
| **A-06 Admin verification 2.0** | `AdminVerificationScreen.kt`: tabs (Pending / Verified / Rejected / Resubmitted / Waiting info), document viewer, checklist, Approve / Request changes / Reject with reasons, history | **Done** (age badges not confirmed). |
| **A-01 completion** | Remember-this-device, role badge, 15-min idle timeout dialog (with a "Lock session" tester) | **Done.** |
| **A-10 / A-12 images** | Category cover and banner 4:5 artwork now use `SuqiImagePickerTile` (URL field remains as a fallback) | Image-picker part **done**. |
| **H-1, H-3** | `HomeScreen` has 0 old-token usages; recent searches persisted | **Done.** |
| **`DemoMode`** | `BackendConfig.DemoMode` flag gates the demo hint, OTP hint, fake social fallbacks, and image helpers | **Done.** |
| **Tests** | 17 tests (adds string-catalog completeness, verification codec round trip, seller verification flow, 16 categories seeded) | Still **no Compose UI-flow tests**. |

### 0.2 Earlier findings (v2 → v3)
Role shell, design-system rollout, demo-data cleanup, B-06 payment proof, A-01 sign-in, 16 categories, and 49 destinations were confirmed done in the earlier audit and remain excluded.

**Status legend**: 🔶 built, not yet verified or tested (Phase 5) · ✅ done (not re-planned) · 🟠 partial (only the missing part is planned) · ⛔ missing (full plan below). **Priority**: P0 blocks work · P1 core value · P2 important · P3 nice to have.

---

## 1. Completion summary

*Feature coverage* = share of a complete marketplace scope for that role that has a working screen. *Design alignment* = share of the role's screens on the cream-and-orange system.

| Role | Screens now | Feature coverage (v2 → now) | Design alignment (v2 → now) | Biggest remaining gaps |
|---|---|---|---|---|
| **Buyer / Visitor** | 27 | ~70% → **~96%** | ~15% → **~100%** | Returns/refunds, deals, coupons, compare, wishlist 2.0, review photos/helpful, seller reply display |
| **Seller** | 15 | ~75% → **~86%** | ~10% → **~100%** | Analytics, earnings, returns, reviews/Q&A management, customers, collections, staff accounts, promo scope/flash-sale wizard |
| **Owner / Admin / Developer** | 12 | ~46% → **~62%** | **~100%** | Users/shops/orders/disputes, finance and payouts, full support inbox, audit log, locations/content/broadcast as full screens (now sheets), richer dev tools |

*(Estimates from screen-by-screen review, not measured. Trilingual coverage is now ~100% of UI text across all three roles (brand names, format placeholders and seed data excluded).)*

---

## 2. Verified complete (excluded from the plan below)

**Foundations**: shared scaffolds (`SuqiScreen`, `SuqiSectionCard`, `SuqiEmptyState`, `SuqiSkeletonList`, `SuqiBottomSheet`, `SuqiFilterChips`, `SuqiStatCard`, `SuqiLineChart`, `SuqiBarChart`); role-gated routing with `SuqiRestrictedState`; guest gate (`requireSignedInOrPrompt`); offline banner; role switcher; developer unlock.

**Buyer**: Home (live banners from admin), Categories (16 sections), Category listing filters and sort (B-04), Search with live suggestions, filters, sort and grid/list toggle (B-03), Product details with rating histogram, Q&A, size guide, similar and more-from-shop rows, share sheet, report sheet (B-05), Cart with live promo validation, Checkout delivery, **Checkout payment with receipt upload (B-06)**, Order tracking with resubmit, reorder, courier card and help ticket (most of B-07), Chat with image attachments and preview, Notifications with type filter, mark read, delete (part of B-09), Address add/edit/default/delete (part of B-10), Reviews with report (part of B-11), Shop storefront with Collections tab and share sheet (part of B-14), Profile edit with avatar picker (part of B-02), Help with FAQ dialog, ticket submission and history, Terms/Privacy documents (part of B-17), Wishlist, Following, Settings (language, theme).

**Buyer, closed in Phase 4**: Notifications with day groups, swipe actions, System tab and preferences with quiet hours (B-09); Storefront underline tabs with About tab and ⓘ / ⋯ menu (B-14); Order ETA chip and invoice sheet with QR, Save and Share (B-07, Return item pill disabled until B-08); Profile Verified pill, default location and Delete account (B-02); Address map placeholder, current location and In use block (B-10); Settings theme tiles, data saver, clear cache, privacy toggles, About screen, developer-gated simulator (B-18); Help topic grid, search, contact card, ticket thread and legal table of contents (B-17).

**Seller**: Dashboard 2.0 with real KPIs, 7D/30D/90D chart, pipeline chart, readiness checklist (S-01); Inventory Manager with SKU search, filter counts, variant expansion, steppers and bulk actions (S-03); Product Management with Draft tab, sort, duplicate, bulk actions, autosave draft, completeness chips and buyer preview (S-04); Order detail (S-05); Payment verification with pinch-zoom receipt, tabs, waiting-time badge and amount-match chip (S-06); Shop branding with live preview, 7-day hours, `SuqiImagePickerTile` gallery, social/policies and scannable ZXing QR save/share image (S-07); Delivery with cut-off `SuqiTimeField`, grouped coverage chips and buyer preview (S-08); Payment methods CRUD (S-08 part); Promo code CRUD persisted (S-09 core); Inbox with filter tabs, read/unread state, quick replies, order attachment cards and notification settings (S-14).

**Localization (v3.3)**: all three roles are translated into አማርኛ and Afaan Oromoo, including seller, admin and developer screens, with localized category/product names.

**Added in v3.2**: localization infrastructure and buyer-core coverage (F-04 core); all F-07 components; phone OTP polish (B-01); seller document-based verification with status tracker (S-15); admin verification review with checklist, request-changes and history (A-06); admin remember-device, role badge, idle timeout (A-01); banner/category image pickers.

**Admin**: Staff sign-in with OWNER/ADMIN/DEVELOPER tiers (A-01); Console with live counts; Categories CRUD with EN/አማ/Oro names (A-10 core); Banners CRUD feeding Home (A-12 core); Review moderation dismiss/delete (A-09 core); Subscription plans CRUD (A-13 core); 3 feature toggles; Dev tools with table counts, role switch, offline simulator and resync (A-22 core). All persisted in Room.

---

## 3. Demo scaffolding (kept on purpose) and remaining UI clean-ups

### 3.1 Kept as-is (not planned for removal)
| Where | What stays | Why |
|---|---|---|
| `AdminSignInScreen` | Staff ID/PIN hint with Fill button | Fast access to Owner/Admin/Developer views |
| `AuthScreen` | Demo OTP `123456`, fake Telegram/Google sheets with fallback values | No real auth yet |
| Seed data, Unsplash fallback images, static support phone and version text | Placeholder content | Replaced when the API lands |
| Settings/Account developer entries | Offline simulator, diagnostics, role switch | Needed to preview every state |

**One small rule so plugging in the API later is painless**: keep every demo shortcut behind a single `DemoMode` flag (a `const val` in `BackendConfig.kt` or `BuildConfig`). New screens in this plan should read demo behavior from that flag instead of scattering literals. Turning the flag off later hides all demo UI in one change. No visible change today.

### 3.2 Still worth fixing (pure UI, no backend needed)
| # | Where | Problem | Fix |
|---|---|---|---|
| H-2 | Admin banners/categories | ✅ Done: URL fields removed, image pickers only | none |
| H-5 | Whole app | 3 shared components are still unused (`SuqiFunnelBars`, `SuqiCountdownChips`, `SuqiCouponTicket`); 🟠 partial | Wire in as each screen below lands (see §4 table) |

---

## 4. Design language (unchanged; applies to every new screen)

Orange-on-cream, human and warm, typography-led. Tokens: page `#FFF8F0`, card `#FFFFFF`, primary `#FF6500`, soft orange `#FFF0E6`, text `#171717` / `#737373`, peach `#FFDFC7`, shop tint `#F3ECFB`. Radius 24dp cards, 30dp sheets/hero, 999dp pills, 18dp image tiles. Warm shadow on cards. Plus Jakarta Sans. Touch targets ≥ 48dp. Dark mode: cream → `#121212`, card → `#1C1C1E`, border on, shadows off.
**Admin variant**: denser, 16dp margins, list rows instead of image cards, orange only for primary actions and active filters, semantic colors for status.
**Reuse first**: use the components in §2 and add only the ones below.

### Shared components: built, and where they should be used next (F-07 ✅)
| Component | Used now | Planned first use |
|---|---|---|
| `SuqiTabs` | A-06, S-06, A-10, A-12, A-11, B-09, B-14 | S-10, A-09, A-16 |
| `SuqiSwitchRow` | Admin sign-in, A-03, A-10, A-11, A-12, seller screens, Search, Category listing, B-09 preferences, B-18 | A-23 |
| `SuqiStepIndicator` | S-15 | B-08 returns, S-09 wizard, S-16 upgrade |
| `SuqiImagePickerTile` | S-15, S-07 gallery, admin banner/category | B-08, B-11 |
| `SuqiDocumentViewer` | S-15, A-06, S-06 | A-13 approvals, A-08 evidence |
| `SuqiDonutChart` | A-02 | S-02 |
| `SuqiFunnelBars` | none | S-02 |
| `SuqiSparkline` | A-02 | S-02 |
| `SuqiRangeSlider` | Search, Category listing | Done |
| `SuqiTimeField` | S-07 hours, S-08 cut-off, B-09 quiet hours | Done |
| `SuqiDateField` | A-12 schedule, S-07 vacation | S-09 schedule |
| `SuqiCountdownChips` | none | B-12 |
| `SuqiCouponTicket` | none | B-15, S-09 preview |
| `SuqiSwipeRow` | S-03 inventory, B-09 notifications | Done |
| `SuqiDragHandleList` | A-10, A-11, A-12 | S-17 |
| `SuqiQrCard` | S-07 share card, B-07 invoice | Done |
| `SuqiListRow` | Account hub, Seller studio, A-02, A-03, B-09, B-18 | Done |

`SuqiListRow` now exists and is the standard settings-style row.

---

## 5. Shared foundations (remaining)

### F-04 · Localization: ✅ done
Fully trilingual across Buyer, Seller and Admin, including toasts, service messages, place names, delivery text, reasons and placeholders. **Rule going forward**: every new screen adds its keys to `SuqiStringsData` (or `extras`) in the same change so the catalog test keeps passing; new service messages get a rule in `SuqiApiMessages`; new place names go in `SuqiPlaces`.
- **One manual QA pass remains** (cannot be automated): switch to አማርኛ and Afaan Oromoo and look for clipped or overflowing labels in chips, `SuqiTabs`, the seller floating nav and tables; Oromo labels are usually longer.
- **Housekeeping**: `res/values-*/strings.xml` hold a handful of entries and duplicate the Kotlin catalog; keep them in sync or remove.

### F-06 · Accessibility, responsive, quality: **P2**
- 48dp hit areas for remaining small icon buttons, 4.5:1 contrast on cream, 130% font scale pass, content width cap 600dp and 2→3→4 grid columns on tablets/foldables.
- **Tests**: add Compose UI tests for the five money-critical flows (checkout with receipt, seller verify payment, admin sign-in gate, role switch, guest gate) and a light/dark screenshot pass for each role's home.

---

## 6. Buyer / Visitor: remaining work

### 6.1 Status of remaining items

| Item | Status | What is left |
|---|---|---|
| B-02 Profile | ✅ | Closed in Phase 4 (account-delete cleanup closed in v3.9) |
| B-07 Order help | 🟠 | Invoice and ETA chip done in Phase 4; **Return item** is a disabled stand-in until B-08 |
| B-08 Returns | 🔶 | Built in Phase 5; close-out pending (P5-1, P5-2) |
| B-09 Notifications | ✅ | Closed in Phase 4 |
| B-10 Addresses | ✅ | Closed in Phase 4 |
| B-11 Reviews | 🔶 | Built in Phase 5 (V2); write sheet, tags and photo upload not individually verified; close-out pending |
| B-12 Deals | 🔶 | Built in Phase 5; close-out pending |
| B-13 Wishlist 2.0 | ⛔ | Collections, price-drop, multi-select |
| B-14 Storefront | ✅ | Closed in Phase 4 |
| B-15 Coupons | ⛔ | Wallet and cart "View coupons" |
| B-17 Help 2.0 | ✅ | Closed in Phase 4 |
| B-18 Settings | ✅ | Closed in Phase 4 |
| B-19 Compare | ⛔ | Full flow |

### 6.2 Plans

#### B-02 · Profile completion: ✅ done (closed in Phase 4)
- Phone row shows a green **Verified** pill (read-only). "Default location" row opens the existing region/city/sub-city sheet.
- Danger zone at bottom of the screen: **Sign out** (outline) and **Delete account** (red outline). Delete opens a dialog explaining what is lost, with a text field that must match "DELETE", then a red confirm pill.

#### B-07 · Order completion: 🟠 invoice and ETA done in Phase 4; Return item pill waits for B-08
- **Estimated delivery chip** under the status (e.g. "Arrives Tue, 12 Nov") when the delivery option has an ETA window.
- **Receipt / Invoice** button → full-screen sheet: shop header with logo, order ref, date, items table, fees, discount, total, payment method and reference, QR with order ref; actions Share and Save as image/PDF.
- **Return item** pill appears on delivered orders for 7 days (opens B-08); after the window shows a neutral disabled pill with the reason.

#### B-08 · Returns & refunds: **P2**
- **Single screen, 3 steps** with `SuqiStepIndicator`: (1) choose items (checkbox cards with thumbnail, qty) → (2) reason (radio list + comment + up to 4 photos via `SuqiImagePickerTile`) → (3) resolution (Refund / Replace) + summary card → Submit.
- **Return status screen**: vertical timeline (Requested → Approved → Item sent back → Refunded), seller note card, refund method line.
- **States**: window expired (neutral card with policy), already requested (card linking to status).
- **Entry**: order detail, plus a "Returns" row on My Orders filter chips.

#### B-09 · Notification polish: ✅ done (closed in Phase 4)
- Group by **Today / Yesterday / Earlier** with sticky small headers. Add **swipe** (left = delete, right = mark read) via `SuqiSwipeRow`; keep the existing icon button as an accessible fallback.
- Header gear opens **Notification preferences**: switch rows (Order updates, Chat messages, Promotions & deals, Back in stock / price drops, Followed shops) and a **Quiet hours** row with two time fields. Preferences stored locally for now.
- Add a **System** tab if system notices exist; tab dots show unread counts per type.

#### B-10 · Address book polish: ✅ done (closed in Phase 4)
- Add a map preview card (static tile placeholder with an orange pin) and **Use current location** outline pill at the top of the add/edit sheet; pin does not need to be draggable until a map provider exists.
- Show an **In use** pill and block delete (with explanation) when an active order uses the address.
- Convert the bottom sheet to a full screen if the form grows beyond one scroll.

#### B-11 · Reviews 2.0: **P2**
- **Write review sheet**: product mini-card, 5 large tappable stars with label ("Poor … Great"), tag chips (Quality, Delivery, Packaging, Value), text, up to 4 photo tiles, anonymous switch, Submit pill.
- **List**: filter chips (All, With photos, 5★ … 1★), sort (Newest, Helpful), review card with avatar, name, stars, date, Verified-purchase pill, photo strip, **seller reply** block in shop tint, **Helpful (N)** toggle.
- Product details histogram links to the filtered list.

#### B-12 · Deals & flash sales: **P2**
- **Entry**: Home "Hot Deals" section header and carousel buttons.
- **Header** with orange flame badge and **countdown chips** (HH : MM : SS, 44dp white rounded boxes). Category chips, product grid with `SuqiProductCard` and a prominent discount badge, "68% claimed" progress bar on limited-stock items.
- **No active sale**: "Next sale starts in…" card with reminder bell toggle.
- **Depends on**: S-09 flash-sale builder (or driven by products with a discount until then).

#### B-13 · Wishlist 2.0: **P3**
- Collection chips (All + user lists), price-drop badge ("↓ ETB 500"), back-in-stock badge, long-press multi-select with "Move to cart", share list.

#### B-14 · Storefront completion: ✅ done (closed in Phase 4)
- Switch tabs to **underline style**: Products · Collections · Reviews · **About**.
- **About tab** (also reachable from an ⓘ in the header): description, **opening hours** (7 rows with Open now / Closed pill), address with map tile, delivery and pickup summary, return policy, accepted payment methods as icon chips, joined date, social links.
- Header: 72dp logo with white ring, verified check, rating, followers, Follow and Chat pills, ⋯ menu (Share, Report shop).
- **Depends on**: S-07 (hours, policies) so the tab has real content.

#### B-15 · Coupons & wallet: **P3**
- Account row "Coupons"; tabs Available · Used · Expired. `SuqiCouponTicket` card: notched edge, soft-orange block with percent or amount, code, conditions, expiry, **Copy** / **Use now**. Cart promo field gets a "View coupons" link opening the same list as a sheet.

#### B-17 · Help center 2.0: ✅ done (closed in Phase 4)
- Top search field, **2×3 topic grid** (Orders, Payments, Delivery, Returns, Account, Sellers) as soft-orange icon cards, each opening a filtered FAQ list.
- **My tickets** opens a thread view (chat style, status pill, reply box) instead of a flat history.
- Contact card: in-app message, call, Telegram. Legal documents get a table of contents and 15sp body at 1.5 line height.

#### B-18 · Settings completion: ✅ done (closed in Phase 4)
- Add: link to Notification preferences (B-09), data saver (no video autoload), clear cache with size shown, privacy toggles, **About** screen (version, licenses). Theme selector as three preview tiles.
- Keep "Offline Mode Simulator" and diagnostics visible **only when developer mode is unlocked**.

#### B-19 · Compare products: **P3**
- Floating "Compare (2)" pill on product cards (max 3). Screen with horizontally scrolling columns (image, name, price) and aligned spec rows from dynamic parameters; rows only where at least one product has a value; differences highlighted soft orange.

---

## 7. Sellers (Suqi shop owners): remaining work

### 7.1 Status of remaining items

| Item | Status | What is left |
|---|---|---|
| S-02 Analytics | 🔶 | Built in Phase 5; see P5-5 |
| S-03 Inventory manager | ✅ | Done (`SellerInventoryScreen`: SKU search, filter counts, variant expansion, steppers, bulk actions) |
| S-04 Product management | ✅ | Done (`ProductManagementScreen` + `AddEditProductScreen`: Draft tab, sort, duplicate, bulk actions, autosave draft, completeness chips, buyer preview) |
| S-06 Verification queue | ✅ | Done (tabs with counts, waiting-time badge, amount-match chip, `SuqiDocumentViewer` receipt) |
| S-07 Shop profile | ✅ | Done (`ShopSettingsScreen`: live header preview, 7-day hours, `SuqiImagePickerTile` gallery, `filesDir` image imports, socials/policies, scannable ZXing QR card with PNG Save/Share) |
| S-08 Delivery | ✅ | Done (`DeliverySetupScreen`: cut-off `SuqiTimeField`, grouped coverage chips with Select all / Clear, buyer checkout preview) |
| S-09 Promotions | 🔶 | Built in Phase 5 (V2, flash sales, backend validation); close-out pending |
| S-10 Reviews & Q&A | ⛔ | Full screen |
| S-11 Customers | ⛔ | Full screen |
| S-12 Earnings & payouts | 🔶 | Built in Phase 5; close-out pending |
| S-13 Returns handling | 🔶 | Built in Phase 5; close-out pending |
| S-14 Inbox | ✅ | Done (`SellerInboxScreen` + `ChatThreadScreen`: filter tabs, read/unread state, order chips, quick replies, order attachment cards, notification settings) |
| S-16 Subscription | 🔶 | Built in Phase 5 (V2); close-out pending |
| S-17 Collections manager | ⛔ | Full screen |
| S-18 Staff accounts | ⛔ | Full screen |

### 7.2 Plans

#### S-03 · Inventory manager: ✅ done (closed in Phase 3)
- Reached from Quick Actions, Seller Studio, and the Products tab ("Inventory" chip).
- Search (incl. SKU) + filter chips with counts (All, Low stock, Out of stock, Paused). Variant expansion, inline steppers (disabled on variant products), long-press multi-select bulk bar (Set stock with per-variant label when applicable, Pause, Mark out of stock).

#### S-04 · Product management completion: ✅ done (closed in Phase 3)
- Draft tab, sort sheet, duplicate, share, bulk actions (Pause, Archive, Delete with confirmation, Change category, Apply discount preserving `originalPrice`). Drafts/paused hidden from buyer `getProducts`.
- Add/Edit product: autosaved device-wide new-product draft banner (cleared on publish), unsaved-changes guard, completeness chips, live Preview as buyer.

#### S-07 · Shop profile & branding completion: ✅ done (closed in Phase 3)
- Live header preview, 7-day opening hours (`SuqiTimeField`) + Copy to all days, 6-photo gallery (`SuqiImagePickerTile`), cover/logo/gallery persisted to `filesDir/suqi_uploads/`, social/website fields, policies with 400-char counters.
- Share card: real ZXing QR code in `SuqiQrCard` with Copy Link, Save PNG image (Pictures/SAF), and Share PNG image (`FileProvider`).

#### S-02 · Analytics & reports: **P2**
- Header with date-range chip (range sheet) and export icon.
- Cards: Revenue (line), Orders by status (stacked bar), **Views → Cart → Orders funnel** with conversion %, Top products (ranked with sparkline), Customers by sub-city (horizontal bars), Payment-method split (donut).
- Free plan: 7 days only; longer ranges show a soft "Unlock with Pro" overlay linking to S-16.
- Empty (new shop): "Your numbers will appear after your first order" + Share shop button.

#### S-09 · Promotions 2.0: **P2**
- Ticket-style list: code, discount, scope, **uses/limit bar**, status pill, expiry, orders and revenue generated.
- **Create wizard** (3 steps): Type (Percent / Fixed / Free delivery) → Scope (whole shop / categories / chosen products) + conditions (min order, per-customer limit, usage cap) → Schedule (start/end) with preview of the buyer coupon card.
- **Flash-sale builder**: products, sale price, stock cap, time window; preview of the B-12 card.

#### S-14 · Inbox completion: ✅ done (closed in Phase 3)
- Filter tabs (All, Unread, Orders, Questions) with counts and unread dot; persisted read/unread state; order-context chips; editable persisted quick replies; order attachment picker with embedded order cards; notification settings sheet (stand-in persisted locally).

#### S-06 · Verification queue polish: ✅ done (closed in Phase 1)

#### S-08 · Delivery polish: ✅ done (closed in Phase 3)
- Cut-off `SuqiTimeField` pickers (persisted stand-in), grouped sub-city/hub coverage chips with Select all / Clear, and Preview as buyer checkout summary card.

#### S-10 · Reviews & Q&A management: **P2**
- Tabs Reviews · Questions. Summary card (average, histogram, response rate).
- Review card: stars, text, photos, product chip, **Reply** (inline field with templates), **Report** sheet. Filters: Unanswered, Low rating, With photos.
- Questions: unanswered first, quick-answer sheet. Unanswered counts feed the dashboard to-do list.

#### S-12 · Earnings & payouts: **P2**
- Header card: available balance (32sp), pending balance, **Request payout** pill (disabled below minimum).
- Ledger grouped by week: sales (+), commission (−), subscription fee (−), refunds (−), payouts; filter chips; row opens a receipt-like detail.
- Statements list (month, Download/Share). Payout method row (bank / Telebirr) with edit. Empty: "No earnings yet".

#### S-13 · Returns handling: **P2**
- Queue (Requested · Approved · Completed · Rejected) with buyer, item thumbnail, reason chip, evidence photo count, age.
- Detail: reason, buyer photos (gallery), order summary; actions **Approve**, **Offer replacement**, **Reject** (reason required); timeline after decision.

#### S-16 · Subscription & billing: **P2**
- Plan compare: two stacked cards (Free Pilot / Pro), orange check benefits, current plan marked, monthly/yearly toggle.
- Upgrade: choose plan → pay (reuse the B-06 "Pay to" card with Suqi's account, reference + receipt) → "Pending approval" → success. Billing: invoices list, renewal date, cancel/downgrade link.
- **Depends on**: A-13 pending-approval queue.

#### S-11 · Customers: **P3**
Searchable list (avatar, name, orders, total spent, last order), segment chips (New, Repeat, Inactive). Detail sheet: order history, private notes, Chat, Send promo code.

#### S-17 · Collections manager: **P3**
List with 4-product cover mosaic, name, count, drag reorder. Edit: name, cover (auto or upload), product multi-picker with search, "Visible on storefront" switch. Feeds the storefront Collections tab.

#### S-18 · Staff accounts: **P3**
Staff list (avatar, name, role pill: Manager / Order handler / Catalog), invite-by-phone sheet, permissions matrix (rows = capabilities, columns = roles, switches), remove with confirmation.

---

## 8. Owner / Admin / Developer: remaining work

**Tiers (one Admin shell, permissions differ)**: **Owner**: everything, plus dashboards and finance (A-02, A-14). **Admin**: queues, moderation, support, content. **Developer**: tools and diagnostics (A-22) and *act as* test sessions.

### 8.1 Status of remaining items

| Item | Status | What is left |
|---|---|---|
| A-02 Owner dashboard | ✅ | Done (`OwnerDashboardScreen`, real date-windowed metrics/charts, CSV export; flagged-payments/disputes alerts route to Console Queues until A-14 & A-08 land) |
| A-03 Console home | ✅ | Done (`AdminConsoleScreen`: 4 tabs, 6 queues, quick tools, collapsible switches, live Support Resolve, 15m lock; `Reported listings` & `Payout requests` queues use temporary review/payment stand-ins until A-07 & A-14) |
| A-04 Users | ⛔ | Full |
| A-05 Shops | ⛔ | Full |
| A-07 Listings moderation | ⛔ | Full |
| A-08 Orders & disputes | ⛔ | Full |
| A-09 Review moderation | 🔶 | Built in Phase 5 (V2); close-out pending |
| A-10 Categories | ✅ | Done (`AdminCategoriesScreen`: active count, tree, reorder, 48dp thumb, 3-lang names, icon grid, chip sub-cats, linked item types; Shop Types tab displays live read-only counts) |
| A-11 Item types | ✅ | Done (`AdminItemTypesScreen` backed by Room `item_types` synced into `ProductTemplates`, seller & buyer previews) |
| A-12 Banners | ✅ | Done (`AdminBannersScreen`: drag reorder, live `SuqiPromoCarousel` preview, destination/schedule/audience filters applied on Home, Home Sections order & visibility) |
| A-13 Plans | 🔶 | Built in Phase 5 (V2, pilot, approvals); close-out pending |
| A-14 Finance | 🔶 | Built in Phase 5; close-out pending |
| A-15 Broadcast | 🔶 | Built in Phase 5; close-out pending |
| A-16 Support inbox | 🔶 | Built in Phase 5; close-out pending |
| A-17 Locations | 🟠 | View hierarchy sheet in Console Quick Tools (`A-03`); full CRUD tree deferred to Phase 6 (`A-17`) |
| A-18 Payment providers | ⛔ | Full |
| A-19 Content manager | 🟠 | Help/Terms preview sheet in Console Quick Tools (`A-03`); full multi-language Markdown editor deferred to Phase 6 (`A-19`) |
| A-20 Audit log | ⛔ | Full |
| A-21 Reports & exports | ⛔ | Full |
| A-22 Developer tools | 🟠 | Environment, data tools, act-as, flags, logs, design gallery, maintenance |
| A-23 Platform switches | 🟠 | Grouped switch cards (4 localized toggles live in collapsible card; grouping expansion deferred to Phase 6) |

### 8.2 Plans (priority order)

#### A-03 · Console home 2.0: **P1**
- Replace the quick-action rows with **queue cards**: Seller verifications, Flagged reviews, Reported listings, Disputes, Support tickets, Payout requests. Each shows live count, oldest-item age, chevron; zero counts show a green "Clear" pill.
- Below: **Quick tools** icon grid (Banners, Categories, Item types, Broadcast, Locations, Content); then the existing toggles in a collapsible **Platform switches** card.
- Top segmented tabs per F-02 (Overview · Queues · Manage · Tools).

#### A-12 · Banners & homepage curator: **P1**
- **Editor**: live preview using the exact `SuqiPromoCarousel` slide; badge + icon, headline with highlight-word picker, subtitle, button text, **destination picker** (category / shop / product / deals / URL), schedule, audience (all / new users / Addis only).
- **List**: mini-carousel cards with status pill, drag reorder, active switch.
- **Home sections order**: reorder and show/hide Featured shops, New arrivals, Popular, Recommended, Recently viewed.

#### A-11 · Item types & parameter templates manager: **P1**
- **List** grouped by section: emoji, name, parameter count, usage count; "New item type".
- **Editor**: name, emoji, default category, section; parameter list (drag reorder, enabled-by-default, Required, "Buyers choose"); **Add parameter** (library search or custom with type chips: Text, Number, Yes/No, Single, Multiple, Color, Date, Measurement); choices/units editor.
- **Preview**: tabs "How sellers see it" (real seller form) and "How buyers see it" (specs card).
- Note: edits never remove values already saved on products.

#### A-02 · Owner dashboard: **P1**
- Date-range control; KPI cards (GMV, Revenue/commission, Orders, Active shops, New users, Conversion).
- Charts: GMV line with dashed previous-period overlay; orders by status; new users vs new shops bars; top categories donut; top shops ranked.
- **Alerts card**: verifications waiting > 24h, flagged payments, open disputes. Export icon (CSV/PDF sheet). Owner tier only.

#### A-10 · Categories manager 2.0: **P2**
- **Tree list** with expand/collapse, drag handles, 48dp image well, active switch, 16-section count header.
- **Edit sheet**: three-language names and cover picker (existing), **icon picker** grid, sub-categories as a **chip editor** (replace comma-separated field), linked item types (from A-11), visibility.

#### A-09 · Review moderation 2.0: **P2**
Tabs Reported · Hidden · Restored; side-by-side "Report" and "Review" cards; reason chips; bulk select; **Restore** action; decisions listed in audit log.

#### A-13 · Plans & monetization: **P2**
Replace pipe-separated features with a **benefit list editor** (add, remove, reorder). Add limits (products, promo codes, analytics days), "Most popular" flag. **Revenue card** (MRR, active subscribers), **Pilot mode** switch with end date, **Pending upgrade approvals** (receipt viewer + approve/reject).

#### A-16 · Support inbox: **P2**
Tabs Open · Pending · Resolved; priority and age chips; requester role pill. Thread: chat style with internal-note bubbles (peach), order context card, quick replies, assign-to, status changer, resolution summary. Feeds buyer "My tickets" (B-17).

#### A-08 · Orders & disputes: **P2**
Order search (reference/phone/shop) with status chips; read-only detail (S-05 layout, no seller actions). **Dispute tab**: case cards (buyer claim, seller response, evidence galleries), actions Refund buyer · Side with seller · Partial · Escalate, mandatory note, timeline.

#### A-04 · Users management: **P2**
Search + chips (All, Buyers, Sellers, Suspended, New this week). Detail: profile, activity (orders, spend, reviews, reports against), actions Suspend/Restore (reason required, red outline + confirm), Reset sessions, View orders.

#### A-05 · Shops management: **P2**
Search + chips (Verified, Pending, Suspended, Pro). Detail tabs Overview · Products · Orders · Finance · Notes. Actions: Feature on Home (switch + schedule), Suspend, Change plan, Message owner.

#### A-07 · Listings moderation: **P2**
Queue cards (image, name, shop, report-reason chips, count). Review uses the media gallery + specs preview. Actions: Keep, Request edit, Take down (reason), Suspend shop. **Needs** the buyer-side "Report listing" sheet (exists) to write to a queue.

#### A-14 · Finance: commissions & payouts: **P2**
Summary (platform revenue, commissions, subscription revenue, pending payouts); **Commission rules** (default %, per-category overrides); **Payout requests** queue (approve / mark paid with reference); ledger with filters and CSV export. Mirrors S-12.

#### A-15 · Notifications broadcast: **P2**
Compose: audience (All, Buyers, Sellers, Shop followers, City), title, body with counter, optional image and destination, **live phone preview**, schedule, "Send test to me". History with sent/opened counts; saved templates.

#### A-20 · Audit log: **P2**
Chronological list: actor avatar + role, sentence ("Admin Sara suspended shop *Tech Hub*"), target chip, time; filters (actor, action, date); detail sheet with before/after values. Every admin action above writes here.

#### A-22 · Developer tools completion: **P2**
Grouped list rows: **Environment** (Built-in vs Remote, base URL, test connection) · **Data tools** (seed demo data, reset database, export/import snapshot, schema version) · **Act as** (test Buyer / Seller / Admin without signing out) · **Feature flags** (raw list incl. media limits, max variants) · **Diagnostics** (device info, request log with status chips, recent errors) · **Design-system gallery** (buttons, chips, cards, sheets, charts, pills in light/dark) · **Maintenance mode & minimum version** (full-screen notice for users). Keep the existing table counts, role switch, offline and resync.

#### A-17 · Locations manager: **P3**
Region → City → Sub-city tree (add, rename, disable, default delivery zone flag); feeds location pickers and seller coverage chips.

#### A-18 · Payment providers directory: **P3**
Providers (Telebirr, CBE Birr, Awash…) with logo, display name, account-format rule, reference-format hint, active switch, live preview of the buyer "Pay to" card. Feeds checkout validation.

#### A-19 · Content manager: **P3**
Edit FAQs, help topics, Terms/Privacy, onboarding copy, empty-state text. EN / አማ / Oro tabs with amber "missing translation" dots; Markdown-lite editor with preview; draft/publish.

#### A-21 · Reports & exports: **P3**
Report cards (Sales, Users, Shops, Orders, Finance, Moderation) → configure → stacked-row preview → Export CSV/PDF/Share.

#### A-23 · Platform switches 2.0: **P3**
Grouped switch cards: Commerce (chat, external contact, reviews, Q&A), Media (video uploads, max files/size), Catalog (variants, custom parameters), Monetization (subscription mode, pilot end), Experience (flash sales, compare). One-line description and "Affects: Buyers / Sellers" per switch.

---

## 9. Roadmap (remaining work only)

| Phase | Goal | Items |
|---|---|---|
| **0 · Quick wins** ✅ | Finish what is built | H-2, `SuqiSwitchRow` / `SuqiListRow` settings pattern |
| **1 · Verification follow-ups** ✅ | Complete the trust loop | A-06 age badges, S-06 tabs/amount chip reusing `SuqiDocumentViewer` |
| **2 · Operate the platform** ✅ | Admin can run it | A-03, A-12, A-11, A-10, A-02 (stand-ins: `Reported listings` & `Payout requests` queues and owner alerts route via Console/Moderation until A-07, A-08, A-14 land) |
| **3 · Seller daily work** ✅ | Run a shop without leaving the app | S-03, S-04, S-07, S-14, S-08 (S-06 closed in Phase 1; stand-ins: device-wide new-product draft slot, unused notification switches & cut-off times, `shop.productCount` includes non-active listings) |
| **4 · Buyer completion** ✅ | Close the purchase loop | B-07 invoice, B-09 preferences, B-10, B-14 About, B-02 delete, B-18, B-17 (stand-ins: disabled Return item pill until B-08, unused notification preferences, device-wide buyer stores; carry-overs C-1 and C-2 closed in v3.9) |
| **5 · Money & retention** 🔶 built, close-out pending | Revenue, returns, growth | B-08 + S-13, S-12 + A-14, S-16 + A-13, S-09 + B-12, S-02, B-11, A-09, A-16, A-15 (all built in v3.11; stand-ins P5-3 to P5-6) |
| **6 · Long tail** | Depth | A-04, A-05, A-07, A-08, A-20 … A-23, A-17 … A-19, B-13, B-15, B-19, S-10, S-11, S-17, S-18, F-06 |

**Dependencies**: S-07 before B-14 About tab · S-09 before B-12 (or derive from discounted products) · S-16 needs A-13 approvals · B-08 needs S-13 · A-11 before A-10 "linked item types" · each new screen adds its localization keys in the same change so the string-catalog test keeps passing.

### Phase 5 close-out checklist (do before Phase 6)
| # | Step | Done when |
|---|---|---|
| 1 | **Build and run** `gradle :app:testDebugUnitTest` | Compiles; the 82 tests pass, including `Phase5LogicTest` and the string-catalog test. Fix whatever breaks (P5-1). |
| 2 | **Compose test per new screen** (P5-2) | One state-changing test each for return request, return status, seller returns decision, earnings payout request, admin finance payout decision, subscription upgrade, deals, analytics range switch, review helpful / reply, support inbox reply, broadcast send. |
| 3 | **Manual pass** on a device in EN, አማርኛ and Afaan Oromoo, light and dark | No clipped labels in the new tabs and chips; flows reachable from Order detail, Home, Seller More and Admin Console. |
| 4 | **Small fixes** P5-3 (`deliveredAt`) and P5-5 (analytics label), optionally P5-4 transition tables | Return window and analytics honest. |
Then update this plan to close Phase 5 and start Phase 6 (long tail): A-04, A-05, A-07, A-08, A-20 to A-23, A-17 to A-19, B-13, B-15, B-19, S-10, S-11, S-17, S-18, F-06.

---

## 10. Definition of Done (every screen above)

1. Uses shared scaffolds and `LocalSuqiColors`; correct in light **and** dark.
2. Has skeleton, empty, error and offline states.
3. All text from `SuqiStringsData` in English, አማርኛ and Afaan Oromoo (catalog test passes); works at 130% font scale.
4. Touch targets ≥ 48dp, icon buttons labelled, contrast checked.
5. Demo behavior reads from the single `DemoMode` flag; no toast-only placeholder actions; no URL text fields where an image picker belongs.
6. Cards 24dp, sheets 30dp, warm shadows; shared header pattern.
7. At least one Compose UI test for the primary action of the screen.
8. Visual review against the reference set (carousel, product card, category card, shop card) before merge.
