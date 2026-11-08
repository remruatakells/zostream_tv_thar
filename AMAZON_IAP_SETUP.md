# Amazon IAP setup

The existing QR payment flow remains the default. Amazon IAP is used for subscriptions only, and only when both switches are true:

- `BuildConfig.AMAZON_IAP_ENABLED` in `app/build.gradle.kts`
- Firebase Realtime Database `payment_features/amazon_tv/iap_enabled`

If either value is false or the Firebase read fails, subscription payments continue through the existing QR flow. All PPV movie, episode, and season rentals always use the QR flow.

## Amazon products

The Amazon subscription uses this exact, case-sensitive parent SKU:

- Parent: `zostream_sub`

The app maps ZoStream TV plans to these Amazon child/term SKUs:

- `Kar 1` → `zostream.week`
- `Thla 1` → `zostream.month`
- `Thla 4` → `zostream.4months`
- `Thla 6` → `zostream.6months`
- `Kum 1` → `zostream.year`

The parent SKU is included when product data is validated; its child term SKU starts the purchase and supplies the displayed price. The API verifies both the receipt's parent `productId` and child `termSku`.

## API configuration

Set these values on the ZoStream API server:

```dotenv
AMAZON_IAP_SHARED_SECRET=<Amazon Developer Console shared key>
AMAZON_IAP_SANDBOX=true
AMAZON_IAP_PARENT_SKU=zostream_sub
AMAZON_IAP_WEEK_SKU=zostream.week
AMAZON_IAP_MONTH_SKU=zostream.month
AMAZON_IAP_FOUR_MONTHS_SKU=zostream.4months
AMAZON_IAP_SIX_MONTHS_SKU=zostream.6months
AMAZON_IAP_YEAR_SKU=zostream.year
```

Use `AMAZON_IAP_SANDBOX=true` with Amazon App Tester/Cloud Sandbox. Change it to `false` for the published Appstore build and clear the Laravel configuration cache after changing environment values.

The authenticated admin API can manage the Firebase switch:

```text
GET /api/v4/admin/realtime/amazon-iap
PUT /api/v4/admin/realtime/amazon-iap
Body: { "iap_enabled": true }
```

The app sends each Amazon receipt to `/api/v4/billing/payments/amazon/verify`. The API grants access only after Amazon RVS validates the user, receipt, SKU, product type, and cancellation status.
