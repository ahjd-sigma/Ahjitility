# Hypixel Skyblock API Documentation

## Overview
This document explains all the APIs we're using for the Hypixel Skyblock profit calculator app.

---

## 1. Pet Upgrades API (Coflnet)

**Endpoint:** `https://sky.coflnet.com/api/kat/data`

**Rate Limit:**
- Maximum 100 requests per minute
- Maximum 30 requests per 10 seconds (recommended to avoid full minute cooldown)

**Method:** GET

**Response Format:** Array of pet upgrade objects

### Response Structure
```json
[
  {
    "name": "Baby_Yeti",
    "baseRarity": "EPIC",
    "hours": 288,
    "cost": 20000000,
    "material": "ENCHANTED_RAW_SALMON",
    "amount": 16,
    "materials": {
      "ENCHANTED_RAW_SALMON": 16
    },
    "itemTag": "PET_BABY_YETI"
  }
]
```

### Key Fields
- `name`: Pet name
- `baseRarity`: Starting rarity for this upgrade step (COMMON, UNCOMMON, RARE, EPIC, LEGENDARY)
- `hours`: Time required for Kat upgrade
- `cost`: Coin cost paid to Kat
- `materials`: Object containing all required materials (item_id: quantity)
- `itemTag`: Unique pet identifier

### Notes
- To get full upgrade path (e.g., Common → Legendary), chain all entries for same pet
- Some upgrades have empty `materials: {}` (only require coins)
- Response size: ~33KB

---

## 2. Bazaar Prices API (Hypixel Official)

**Endpoint:** `https://api.hypixel.net/v2/skyblock/bazaar`

**Rate Limit:** No limit

**Method:** GET

**Response Format:** Nested object with all bazaar items

### Response Structure
```json
{
  "success": true,
  "lastUpdated": 1767276381574,
  "products": {
    "ENCHANTED_EGG": {
      "product_id": "ENCHANTED_EGG",
      "sell_summary": [
        {
          "amount": 560,
          "pricePerUnit": 2319.7,
          "orders": 1
        }
      ],
      "buy_summary": [
        {
          "amount": 12345,
          "pricePerUnit": 2300.0,
          "orders": 3
        }
      ],
      "quick_status": {
        "productId": "ENCHANTED_EGG",
        "sellPrice": 2319.7,
        "sellVolume": 75000,
        "sellMovingWeek": 5000000,
        "sellOrders": 50,
        "buyPrice": 2300.0,
        "buyVolume": 100000,
        "buyMovingWeek": 6000000,
        "buyOrders": 100
      }
    }
  }
}
```

### Key Fields
- `products`: Object where keys are item IDs
- `sell_summary[0].pricePerUnit`: **Instant buy price** (what you pay to buy instantly)
- `buy_summary[0].pricePerUnit`: **Buy order price** (what you pay if you place order)
- `quick_status.sellPrice`: Same as sell_summary[0], easier access
- `quick_status.buyPrice`: Same as buy_summary[0], easier access

### Usage
**For buying materials:**
- Use `sell_summary[0].pricePerUnit` for instant buy
- Use `buy_summary[0].pricePerUnit` for buy order (cheaper but slower)

**For selling crafted items:**
- Use `buy_summary[0].pricePerUnit` for instant sell
- Use `sell_summary[0].pricePerUnit` for sell order (more profit but slower)

### Notes
- Response size: ~3MB
- Contains ALL bazaar items in single request
- No API key required

---

## 3. Auction House - All Lowest BINs (Moulberry)

**Endpoint:** `https://moulberry.codes/lowestbin.json`

**Rate Limit:** Unknown (use sparingly)

**Method:** GET

**Response Format:** Simple key-value object

### Response Structure
```json
{
  "REAPER_SCYTHE": 25000000.0,
  "ENCHANTED_EGG": 2320.0,
  "ASPECT_OF_THE_END": 150000.0
}
```

### Key Fields
- Keys are item IDs
- Values are lowest BIN prices in coins

### Usage
**Pros:**
- Single request gets all auction house items
- Very simple to parse
- Fast lookups

**Cons:**
- Doesn't include item details (enchantments, reforges, etc.)
- May not be as up-to-date as Coflnet
- Only gives single price per item (vanilla version)

### Notes
- Response size: ~117KB
- Good for bulk price lookups

---


## API Call Optimization

### Recommendations
1. **Cache bazaar data** for 1-5 minutes (updates frequently but not per-second)
2. **Batch Coflnet requests** - stay under 30 requests per 10 seconds
3. **Use Moulberry for initial loads** - single request gets all AH prices
4. **Use Coflnet per-item** - only when user searches specific items

### Rate Limiting Implementation
```kotlin
// Example: Simple rate limiter for Coflnet
var requestCount = 0
var windowStart = System.currentTimeMillis()

fun checkRateLimit() {
    val now = System.currentTimeMillis()
    if (now - windowStart > 10000) {
        // Reset window
        requestCount = 0
        windowStart = now
    }
    
    if (requestCount >= 30) {
        val waitTime = 10000 - (now - windowStart)
        Thread.sleep(waitTime)
        requestCount = 0
        windowStart = System.currentTimeMillis()
    }
    
    requestCount++
}
```