# NBD Payments — Payment Gateway Simulator

**NBD Payments** is a hosted payment gateway simulator that allows merchants to integrate payment processing into their applications through REST APIs and a hosted checkout page.

It provides merchant authentication, payment creation, secure checkout tokens, simulated card transactions, payment status verification, transaction tracking, and predefined test scenarios.

Built using Java 21, Spring Boot, PostgreSQL, and Spring Security.

> **Important:** NBD Payments is currently a payment gateway simulator. It does not connect to real banks, card networks, or payment processors. All transactions and payment outcomes are simulated. No real money is collected, transferred, or settled.

---

## Table of Contents

1. [Overview](#1-overview)
2. [How NBD Payments Works](#2-how-nbd-payments-works)
3. [Technology Stack](#3-technology-stack)
4. [Merchant Onboarding](#4-merchant-onboarding)
5. [Integration Requirements](#5-integration-requirements)
6. [API Reference](#6-api-reference)
7. [Payment Statuses and Attempts](#7-payment-statuses-and-attempts)
8. [Test Card Numbers](#8-test-card-numbers)
9. [Merchant Integration Guide](#9-merchant-integration-guide)
10. [Security Requirements](#10-security-requirements)
11. [Error Handling](#11-error-handling)
12. [Payment Flow Examples](#12-payment-flow-examples)
13. [Integration Checklist](#13-integration-checklist)
14. [Limitations](#14-limitations)

---

## 1. Overview

NBD Payments provides a standard hosted-checkout integration.

A merchant does not need to build or maintain its own card-entry page. Instead, the merchant creates a payment through the NBD Payments API and redirects the customer to the NBD-hosted checkout page.

After the customer completes the payment, NBD Payments returns the customer to the merchant's registered return URL.

The merchant then verifies the payment directly through the NBD Payments API before confirming the order.

### Features

- Merchant account management.
- Merchant API-key authentication.
- REST-based payment creation.
- Hosted checkout page.
- Secure, randomly generated checkout tokens.
- Simulated card payment processing.
- Predefined success and failure scenarios.
- Payment attempt tracking.
- Five-attempt payment limit.
- Fifteen-minute checkout expiry.
- Idempotent payment creation.
- Merchant-specific payment status verification.
- Trusted return URL validation.
- Transaction history and merchant dashboard.
- Payment refund simulation.

---

## 2. How NBD Payments Works

NBD Payments follows a redirect-based hosted checkout architecture.

### Complete payment lifecycle

1. The customer selects NBD Payments at the merchant's checkout.
2. The merchant backend calculates the payable amount.
3. The merchant backend sends a payment creation request to NBD Payments.
4. NBD Payments authenticates the merchant API key.
5. NBD Payments creates a payment and generates a unique payment ID and checkout token.
6. NBD Payments returns the payment details to the merchant backend.
7. The merchant constructs the hosted checkout URL using the checkout token.
8. The merchant frontend redirects the customer to NBD Payments.
9. The customer enters card details on the NBD-hosted checkout page.
10. NBD Payments evaluates the card against its predefined test scenarios.
11. NBD Payments updates the payment and payment-attempt records.
12. After a successful payment or terminal failure, the customer is redirected to the registered merchant return URL.
13. The merchant frontend extracts the payment ID.
14. The merchant backend independently verifies the payment using the NBD Payments API.
15. The merchant confirms the order only after successful verification.

### Architecture

```text
                 CUSTOMER
                    |
                    v
            MERCHANT APPLICATION
                    |
                    v
              MERCHANT BACKEND
                    |
                    | POST /api/v1/payments
                    | X-API-Key
                    | Idempotency-key
                    v
              NBD PAYMENTS API
                    |
                    | Creates payment
                    | Generates paymentId
                    | Generates checkoutToken
                    v
              MERCHANT BACKEND
                    |
                    | Returns checkout URL
                    v
             MERCHANT FRONTEND
                    |
                    | Redirect
                    v
             NBD HOSTED CHECKOUT
                    |
                    | Card details
                    v
            PAYMENT SCENARIO SIMULATOR
                    |
                    | SUCCESS / FAILURE
                    v
              NBD PAYMENTS API
                    |
                    | Updates payment status
                    | Stores payment attempt
                    v
             MERCHANT RETURN URL
                    |
                    | paymentId
                    v
              MERCHANT BACKEND
                    |
                    | GET /api/v1/payments/{paymentId}
                    | X-API-Key
                    v
              NBD PAYMENTS API
                    |
                    | Verified payment status
                    v
              MERCHANT BACKEND
                    |
                    | Confirm order only on SUCCESS
                    v
             MERCHANT APPLICATION
```

### Important distinction

NBD Payments is responsible for processing and reporting the simulated payment.

The merchant application remains responsible for:

- Customer authentication.
- Cart and checkout management.
- Calculating the payable amount.
- Storing its own order and checkout records.
- Associating the NBD payment ID with its checkout.
- Verifying payment status.
- Finalizing the order.

NBD Payments does not automatically create orders in merchant applications.

---

## 3. Technology Stack

| Component | Technology |
|---|---|
| Backend | Java 21 |
| Framework | Spring Boot 3.5.7 |
| API | Spring Web / REST |
| Database | PostgreSQL |
| ORM | Spring Data JPA / Hibernate |
| Database migrations | Flyway |
| Authentication | Spring Security |
| Password hashing | Spring Security PasswordEncoder |
| API-key hashing | SHA-256 |
| Checkout token generation | SecureRandom |
| Frontend | HTML, CSS, JavaScript |
| Build tool | Maven |

---

## 4. Merchant Onboarding

A merchant must have an active NBD Payments merchant account before using the payment APIs.

### Step 1: Merchant registration

Merchants can register through the NBD Payments merchant registration flow.

The registration API is:

```http
POST /api/v1/merchants
Content-Type: application/json
```

Request:

```json
{
  "businessName": "Example Store",
  "email": "merchant@example.com",
  "password": "your-secure-password"
}
```

Merchant registration requires email verification and subsequent account activation.

### Step 2: Merchant activation

A merchant account must have:

```text
status = ACTIVE
```

Only active merchants can generate API keys and create payments.

### Step 3: Generate merchant API key

After merchant activation and login, the merchant can generate its API key through the merchant portal.

The corresponding API endpoint is:

```http
POST /api/v1/merchants/api-key
```

This endpoint uses the authenticated merchant session.

Example response:

```json
{
  "apiKey": "nbd_test_YOUR_GENERATED_API_KEY",
  "message": "Store this key securely. It will not be shown again."
}
```

**Important:**

- The API key is generated for an individual merchant.
- The API key is displayed only once.
- Store it securely on the merchant backend.
- Do not expose it in frontend applications.
- An active merchant can generate one API key through the current implementation.

### Step 4: Register the merchant return URL

The merchant must have a registered return URL configured in its NBD Payments merchant account.

Example:

```text
https://merchant.example.com/nbd-payment-return
```

The return URL supplied during payment creation must exactly match the URL registered for that merchant.

Return URL configuration is part of merchant onboarding; it is not a public payment-creation API operation.

---

## 5. Integration Requirements

Before implementation, the merchant needs the following:

| Requirement | Description |
|---|---|
| NBD API base URL | Base URL of the deployed NBD Payments service |
| Merchant API key | Merchant-specific API key |
| Registered return URL | URL where customers return after checkout |
| Backend integration | Server-side payment creation and verification |
| Frontend integration | Hosted checkout redirect and return page |

### Environment variables

Configure these values on the merchant backend:

```env
NBD_API_BASE_URL=https://your-nbd-payments-domain.com
NBD_API_KEY=your_merchant_api_key
NBD_RETURN_URL=https://merchant.example.com/nbd-payment-return
```

These are example merchant-side variable names. They are not variables automatically required by the NBD Payments application itself.

Never expose `NBD_API_KEY` to the browser.

---

## 6. API Reference

The following endpoints are used in the merchant payment integration.

### 6.1 Create Payment

Creates a new payment for an authenticated merchant.

**Endpoint**

```http
POST /api/v1/payments
```

**Headers**

```http
Content-Type: application/json
X-API-Key: YOUR_MERCHANT_API_KEY
Idempotency-key: UNIQUE_REQUEST_KEY
```

Both headers are required.

**Request body**

```json
{
  "amountPaise": 150000,
  "currency": "INR",
  "returnUrl": "https://merchant.example.com/nbd-payment-return"
}
```

**Request parameters**

| Parameter | Type | Required | Description |
|---|---|---|---|
| amountPaise | Long | Yes | Payment amount in paise |
| currency | String | Yes | Currently supports INR only |
| returnUrl | String | No | Must match the registered merchant return URL when supplied |

**Amount validation**

The minimum payment amount is 100 paise (₹1.00).

Examples:

| Amount | Value in paise |
|---|---:|
| ₹1 | 100 |
| ₹100 | 10000 |
| ₹500 | 50000 |
| ₹1,500 | 150000 |
| ₹10,000 | 1000000 |

**Successful response — HTTP 201**

```json
{
  "paymentId": "PAY-XXXXXXXX",
  "checkoutToken": "64_CHARACTER_HEX_TOKEN",
  "amountPaise": 150000,
  "currency": "INR",
  "status": "PENDING",
  "errorCode": null,
  "createdAt": "2026-11-19T10:30:00Z"
}
```

The values above are illustrative. Actual payment IDs, tokens, and timestamps are generated by NBD Payments.

**Important:** The create-payment API does not return a `checkoutUrl`. The merchant must construct the hosted checkout URL using the returned `checkoutToken`.

### 6.2 Construct Hosted Checkout URL

The hosted checkout page is served by the NBD Payments application:

```text
https://your-nbd-payments-domain.com/payment.html?token=CHECKOUT_TOKEN
```

Example JavaScript:

```javascript
const checkoutUrl =
  `${NBD_API_BASE_URL}/payment.html?token=${encodeURIComponent(checkoutToken)}`;

window.location.href = checkoutUrl;
```

The checkout token is generated by NBD Payments and is associated with the payment.

Do not generate or modify this token yourself.

### 6.3 Get Checkout Details

The hosted checkout page uses this endpoint to retrieve payment and merchant information.

**Endpoint**

```http
GET /api/v1/payments/checkout/{checkoutToken}
```

This is a public token-based checkout endpoint. It does not require a merchant API key.

**Example**

```http
GET /api/v1/payments/checkout/64_CHARACTER_HEX_TOKEN
```

**Response**

```json
{
  "merchantName": "Example Store",
  "amountPaise": 150000,
  "currency": "INR",
  "paymentId": "PAY-XXXXXXXX",
  "attemptsUsed": 0,
  "attemptsRemaining": 5,
  "returnUrl": "https://merchant.example.com/nbd-payment-return"
}
```

The `returnUrl` is included when configured for the payment.

This endpoint only provides checkout details for a valid, pending, non-expired payment.

### 6.4 Process Card Payment

Processes a simulated card transaction.

**Endpoint**

```http
POST /api/v1/payments/checkout/{checkoutToken}/pay
```

**Headers**

```http
Content-Type: application/json
```

**Request body**

```json
{
  "cardNumber": "4111111111111111",
  "expiry": "12/28",
  "cvv": "123"
}
```

**Request parameters**

| Parameter | Type | Description |
|---|---|---|
| cardNumber | String | 16-digit test card number |
| expiry | String | MM/YY format |
| cvv | String | Three-digit CVV |

The expiry date must be valid and must not be in the past.

**Successful response**

```json
{
  "paymentId": "PAY-XXXXXXXX",
  "attemptId": "ATT-XXXXXXXX",
  "status": "SUCCESS",
  "attemptStatus": "SUCCESS",
  "errorCode": null,
  "message": "Payment successful",
  "attemptNumber": 1,
  "attemptsRemaining": 4
}
```

**Failed attempt response**

```json
{
  "paymentId": "PAY-XXXXXXXX",
  "attemptId": "ATT-XXXXXXXX",
  "status": "PENDING",
  "attemptStatus": "FAILED",
  "errorCode": "INSUFFICIENT_FUNDS",
  "message": "You do not have enough balance to make this transaction.",
  "attemptNumber": 1,
  "attemptsRemaining": 4
}
```

A failed card attempt does not necessarily mean the payment itself has permanently failed.

The overall payment can remain `PENDING` while retry attempts are available.

### 6.5 Verify Payment Status

This is the most important endpoint for merchant integration.

The merchant backend must independently retrieve the payment status from NBD Payments.

**Endpoint**

```http
GET /api/v1/payments/{paymentId}
```

**Headers**

```http
X-API-Key: YOUR_MERCHANT_API_KEY
```

**Example**

```http
GET /api/v1/payments/PAY-XXXXXXXX
```

**Successful payment response**

```json
{
  "paymentId": "PAY-XXXXXXXX",
  "amountPaise": 150000,
  "currency": "INR",
  "status": "SUCCESS",
  "errorCode": null
}
```

**Failed payment response**

```json
{
  "paymentId": "PAY-XXXXXXXX",
  "amountPaise": 150000,
  "currency": "INR",
  "status": "FAILED",
  "errorCode": "INSUFFICIENT_FUNDS"
}
```

**Refunded payment response**

```json
{
  "paymentId": "PAY-XXXXXXXX",
  "amountPaise": 150000,
  "currency": "INR",
  "status": "REFUNDED",
  "errorCode": null
}
```

The API verifies that the payment belongs to the merchant associated with the supplied API key.

If the payment does not belong to that merchant, it is not returned as that merchant's payment.

### Merchant-side verification rules

Before confirming an order, validate all of the following:

1. Payment ID matches the payment stored against the merchant's checkout/order intent.
2. Payment belongs to the expected merchant.
3. Amount matches the original amount calculated by the merchant backend.
4. Currency matches the original payment currency.
5. Payment status is exactly `SUCCESS`.

Only then should the merchant finalize the order.

---

## 7. Payment Statuses and Attempts

### Payment statuses

| Status | Meaning |
|---|---|
| PENDING | Payment has been created and is awaiting a terminal outcome |
| SUCCESS | Payment completed successfully |
| FAILED | Payment reached a terminal failure |
| REFUNDED | Successful payment was refunded |

### Attempt statuses

| Attempt status | Meaning |
|---|---|
| SUCCESS | Card payment succeeded |
| FAILED | Card attempt failed |
| AUTHENTICATION_FAILED | Simulated authentication failure |

### Payment attempt rules

NBD Payments currently implements the following rules:

- Maximum five card payment attempts per payment.
- A checkout token expires after 15 minutes from payment creation.
- Successful payment immediately changes the payment status to `SUCCESS`.
- A failed attempt before the fifth attempt keeps the payment status `PENDING`.
- The fifth unsuccessful attempt changes the payment status to `FAILED`.
- Once payment succeeds or reaches terminal failure, it cannot be processed again.
- A new payment must be created if the customer needs another payment link after terminal failure.

### Example attempt progression

| Attempt | Result | Overall payment status | Remaining attempts |
|---|---|---|---:|
| 1 | FAILED | PENDING | 4 |
| 2 | FAILED | PENDING | 3 |
| 3 | FAILED | PENDING | 2 |
| 4 | FAILED | PENDING | 1 |
| 5 | FAILED | FAILED | 0 |

A successful payment on any available attempt changes the overall status to `SUCCESS`.

---

## 8. Test Card Numbers

NBD Payments uses predefined card numbers to simulate payment outcomes.

Use these card numbers in the hosted checkout page.

| Card number | Simulated outcome |
|---|---|
| `4111111111111111` | SUCCESS |
| `4000000000000002` | INSUFFICIENT_FUNDS |
| `4000000000000010` | AUTHENTICATION_FAILED |
| `4000000000000028` | LIMIT_EXCEEDED |
| `4000000000000036` | FRAUD_SUSPECTED |
| `4000000000000044` | ACCOUNT_RESTRICTED |
| `4000000000000051` | TRANSACTION_NOT_ALLOWED |
| `4000000000000069` | BLACKLISTED_BIN |
| `4000000000000077` | SERVER_UNAVAILABLE |
| `4000000000000085` | NETWORK_TIMEOUT |
| `4000000000000093` | MERCHANT_CONFIGURATION_ERROR |

### Additional validation scenarios

The simulator also supports the following outcomes:

| Condition | Error code |
|---|---|
| Invalid card number or unsupported card | INVALID_CARD |
| Invalid expiry format | INVALID_CARD |
| Invalid CVV format | INVALID_CARD |
| Expired card | EXPIRED_CARD |

For card scenarios, the simulator evaluates the card number, expiry date, and CVV.

For testing, use:

- A valid 16-digit card number.
- A future expiry date in MM/YY format.
- A three-digit CVV.

The simulator does not perform real card authorization or contact a bank.

---

## 9. Merchant Integration Guide

This section explains what a merchant needs to implement in its own application.

The integration is backend-independent. The merchant can use Node.js, Java, PHP, Python, .NET, or any other technology capable of making HTTP requests.

### Step 1: Configure credentials

Store the following values on the merchant backend:

```env
NBD_API_BASE_URL=https://your-nbd-payments-domain.com
NBD_API_KEY=your_merchant_api_key
NBD_RETURN_URL=https://merchant.example.com/nbd-payment-return
```

Never expose the API key to the frontend.

### Step 2: Create a payment from the merchant backend

When the customer selects NBD Payments:

1. Authenticate the customer.
2. Load the customer's cart or checkout data.
3. Calculate the final payable amount on the backend.
4. Generate a unique idempotency key for this payment operation.
5. Call the NBD create-payment endpoint.
6. Store the returned payment ID and checkout token against the merchant's own checkout/order intent.

Example using Node.js:

```javascript
async function createNbdPayment({
  amountPaise,
  idempotencyKey
}) {
  const response = await fetch(
    `${process.env.NBD_API_BASE_URL}/api/v1/payments`,
    {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'X-API-Key': process.env.NBD_API_KEY,
        'Idempotency-key': idempotencyKey
      },
      body: JSON.stringify({
        amountPaise,
        currency: 'INR',
        returnUrl: process.env.NBD_RETURN_URL
      })
    }
  );

  const data = await response.json();

  if (!response.ok) {
    throw new Error(
      data.message || 'Unable to create NBD payment'
    );
  }

  return data;
}
```

Example response:

```json
{
  "paymentId": "PAY-XXXXXXXX",
  "checkoutToken": "64_CHARACTER_HEX_TOKEN",
  "amountPaise": 150000,
  "currency": "INR",
  "status": "PENDING",
  "errorCode": null,
  "createdAt": "2026-11-19T10:30:00Z"
}
```

### Step 3: Return the checkout URL to the frontend

The merchant backend should construct the hosted checkout URL:

```javascript
const checkoutUrl =
  `${process.env.NBD_API_BASE_URL}/payment.html?token=${encodeURIComponent(payment.checkoutToken)}`;
```

Return the URL to the frontend.

Example:

```json
{
  "success": true,
  "paymentId": "PAY-XXXXXXXX",
  "checkoutUrl": "https://your-nbd-payments-domain.com/payment.html?token=64_CHARACTER_HEX_TOKEN"
}
```

The response above is an example of a merchant application's response, not the native NBD create-payment response.

### Step 4: Redirect the customer

The merchant frontend redirects the customer:

```javascript
window.location.href = checkoutUrl;
```

The customer now completes payment on the NBD-hosted checkout page.

The merchant application does not need to collect the card details itself.

### Step 5: Handle the merchant return URL

After successful payment or terminal failure, NBD Payments redirects the customer to the registered merchant return URL.

Example:

```text
https://merchant.example.com/nbd-payment-return?paymentId=PAY-XXXXXXXX
```

The merchant frontend extracts the payment ID:

```javascript
const params = new URLSearchParams(
  window.location.search
);

const paymentId = params.get('paymentId');
```

The frontend must send this payment ID to its own backend.

Do not call the NBD status API directly from the browser.

### Step 6: Verify payment from the merchant backend

Example:

```javascript
async function verifyNbdPayment(paymentId) {
  const response = await fetch(
    `${process.env.NBD_API_BASE_URL}/api/v1/payments/${encodeURIComponent(paymentId)}`,
    {
      method: 'GET',
      headers: {
        'X-API-Key': process.env.NBD_API_KEY
      }
    }
  );

  const data = await response.json();

  if (!response.ok) {
    throw new Error(
      data.message || 'Unable to verify NBD payment'
    );
  }

  return data;
}
```

### Step 7: Validate and finalize the order

The merchant backend must compare the NBD response with its own stored checkout intent.

Example:

```javascript
const payment = await verifyNbdPayment(paymentId);

if (
  payment.paymentId !== checkout.nbdPaymentId ||
  payment.amountPaise !== checkout.amountPaise ||
  payment.currency !== checkout.currency
) {
  throw new Error('Payment details do not match');
}

if (payment.status === 'SUCCESS') {
  // Finalize the merchant order.
} else if (payment.status === 'PENDING') {
  // Keep the checkout pending.
} else if (payment.status === 'FAILED') {
  // Mark the payment attempt as failed.
} else if (payment.status === 'REFUNDED') {
  // Apply the merchant's refund/order policy.
}
```

The merchant must implement order finalization in its own application and make it idempotent to prevent duplicate orders.

---

## 10. Security Requirements

Security is an essential part of payment integration.

Merchants must follow these requirements:

### API key protection

- Store API keys in backend environment variables or a secrets manager.
- Never expose API keys in frontend JavaScript.
- Never store API keys in public repositories.
- Never send API keys in customer-facing browser requests.

### Payment verification

- Never trust the payment status supplied by the frontend.
- Never treat a successful redirect as proof of payment.
- Always verify payment status using the NBD API.
- Validate payment ID, amount, currency, and status.
- Verify that the payment is associated with the merchant's own checkout intent.

### Idempotency

The create-payment endpoint requires an `Idempotency-key` header.

The key:

- Must be unique for a new payment operation.
- Can be reused when retrying the same creation request.
- Must not be reused with different payment details or a different return URL.
- Must be generated and managed by the merchant backend.

If the same key is reused with different payment details, NBD Payments returns a conflict response.

### Return URL validation

NBD Payments validates the return URL against the merchant's registered return URL.

- The requested URL must exactly match the registered URL.
- HTTPS is required for non-localhost addresses.
- HTTP is permitted for `localhost` and `127.0.0.1` during local development.
- URLs containing user information or fragments are rejected.

Use HTTPS for deployed merchant applications.

---

## 11. Error Handling

The merchant should handle API errors gracefully.

Common HTTP responses include:

| HTTP status | Meaning | Recommended handling |
|---|---|---|
| 400 | Invalid request, missing idempotency key, or invalid return URL | Correct the request |
| 401 | Invalid or missing API credentials | Check merchant API key |
| 403 | Merchant is inactive or not authorized | Contact NBD Payments administrator |
| 404 | Payment or checkout not found | Check payment ID or checkout token |
| 409 | Payment conflict, expired state, or unavailable payment | Check payment state before retrying |
| 410 | Checkout expired | Create a new payment |
| 422 | Request validation failure, where returned | Correct the submitted fields |

The exact error response body may vary depending on the exception and validation path.

### Important distinction

There are two different types of failures:

**Payment failures**

These are simulated card outcomes, such as:

```text
INSUFFICIENT_FUNDS
LIMIT_EXCEEDED
FRAUD_SUSPECTED
AUTHENTICATION_FAILED
```

They are returned as payment or attempt status information.

**API failures**

These occur when an API request cannot be processed, such as:

```text
INVALID_API_KEY
PAYMENT_NOT_FOUND
CHECKOUT_EXPIRED
INVALID_RETURN_URL
```

The merchant should handle API failures separately from card payment failures.

---

## 12. Payment Flow Examples

### Scenario A: Successful payment

1. Merchant creates a payment.
2. NBD returns `paymentId` and `checkoutToken`.
3. Customer opens hosted checkout.
4. Customer enters test card `4111111111111111`.
5. NBD processes the payment successfully.
6. Payment status becomes `SUCCESS`.
7. Customer is redirected to the merchant return URL.
8. Merchant verifies payment through the status API.
9. Merchant confirms the order.

Expected final payment status:

```text
SUCCESS
```

### Scenario B: Failed payment

1. Merchant creates a payment.
2. Customer opens hosted checkout.
3. Customer enters test card `4000000000000002`.
4. NBD returns `INSUFFICIENT_FUNDS`.
5. The first failed attempt leaves the payment `PENDING`.
6. The customer can retry with another card while attempts remain.
7. After five unsuccessful attempts, payment status becomes `FAILED`.
8. Customer is redirected to the merchant return URL.
9. Merchant verifies the payment.
10. Merchant does not confirm the order as paid.

Expected terminal payment status:

```text
FAILED
```

Expected error code:

```text
INSUFFICIENT_FUNDS
```

### Scenario C: Pending payment

If the payment remains `PENDING`, the merchant must not treat it as successful or failed.

The merchant should retain its checkout state and allow an appropriate retry or status-check workflow.

A payment that remains pending after an earlier failed card attempt is still eligible for another attempt while its checkout is valid.

### Scenario D: Expired checkout

A checkout token is valid for 15 minutes from payment creation.

If the customer attempts to use an expired checkout:

```text
HTTP 410 - Checkout expired
```

The merchant should create a new payment rather than reusing the expired checkout.

---

## 13. Integration Checklist

### Merchant onboarding

- [ ] Merchant account registered.
- [ ] Merchant email verified.
- [ ] Merchant account activated.
- [ ] API key generated.
- [ ] API key stored securely.
- [ ] Return URL registered.

### Backend integration

- [ ] NBD API base URL configured.
- [ ] Create-payment API implemented.
- [ ] Idempotency key implemented.
- [ ] Payment ID stored against merchant checkout intent.
- [ ] Checkout token stored securely.
- [ ] Hosted checkout URL constructed.
- [ ] Payment verification API implemented.
- [ ] Payment ID, amount, currency, and status validated.
- [ ] Duplicate order finalization prevented.

### Frontend integration

- [ ] NBD Payments option available at checkout.
- [ ] Merchant backend initialization endpoint connected.
- [ ] Hosted checkout redirect implemented.
- [ ] Merchant return page implemented.
- [ ] Payment ID extracted from return URL.
- [ ] Merchant backend verification triggered.
- [ ] Success, failure, and pending states handled.
- [ ] Order confirmation displayed only after successful verification.

### Testing

- [ ] Successful payment tested.
- [ ] Insufficient funds tested.
- [ ] Authentication failure tested.
- [ ] Other predefined failure scenarios tested.
- [ ] Five-attempt limit tested.
- [ ] Checkout expiry tested.
- [ ] Duplicate payment creation tested.
- [ ] Payment verification tested.
- [ ] Failed payment does not create a paid order.
- [ ] Successful payment creates the correct merchant order.

---

## 14. Limitations

NBD Payments is currently a simulator and has the following limitations:

- Only INR is supported.
- Minimum payment amount is ₹1.00.
- Payments are simulated using predefined test card numbers.
- Checkout sessions expire after 15 minutes.
- Maximum five card attempts per payment.
- No real banking or card-network authorization.
- No real fund transfer or settlement.
- No real OTP or 3-D Secure authentication flow.
- Merchant order management remains the responsibility of the integrating application.

The simulator should be used for development, integration testing, and payment-flow demonstrations. It should not be represented as a live payment-processing service.

---

## Final Integration Principle

**Create payment → Redirect to hosted checkout → Process payment → Return to merchant → Verify payment → Finalize order.**

NBD Payments handles the simulated payment lifecycle. The merchant independently verifies the payment and controls its own order lifecycle.

---

**Repository:** https://github.com/akshay-yadavnbd/nbd-payment-gateway-simulator

**Project:** NBD Payments — Payment Gateway Simulator