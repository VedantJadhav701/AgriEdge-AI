"""
Sync mandi (wholesale market) prices for banana and papaya in Maharashtra into
app/src/main/assets/data/market_prices.json.

Source: Agmarknet daily mandi prices, published on data.gov.in.
  1. Create a free account at https://data.gov.in and copy your API key.
  2. Find the resource ID for "Current Daily Price of Various Commodities from Various Markets (Mandi)".
     Put it in RESOURCE_ID below (it is a placeholder until you do).
  3. Respect the portal's terms of use and rate limits.

Run:
  export DATAGOV_API_KEY=your_key
  python fetch_market_prices.py
"""
import json
import os
from datetime import datetime, timezone
from pathlib import Path

import requests

RESOURCE_ID = "REPLACE_WITH_AGMARKNET_RESOURCE_ID"
API = f"https://api.data.gov.in/resource/{RESOURCE_ID}"
TARGET = Path(__file__).resolve().parent.parent / "app/src/main/assets/data/market_prices.json"
COMMODITIES = {"Banana": "banana", "Papaya": "papaya"}
STATE = "Maharashtra"


def _to_int(v):
    try:
        return int(float(v))
    except (TypeError, ValueError):
        return 0


def fetch():
    key = os.environ["DATAGOV_API_KEY"]
    rows = []
    for commodity, crop in COMMODITIES.items():
        params = {
            "api-key": key,
            "format": "json",
            "limit": 500,
            "filters[State]": STATE,
            "filters[Commodity]": commodity,
        }
        r = requests.get(API, params=params, timeout=30)
        r.raise_for_status()
        for rec in r.json().get("records", []):
            rows.append({
                "crop": crop,
                "district": rec.get("District", ""),
                "market": rec.get("Market", ""),
                "min_rs": _to_int(rec.get("Min_Price")),
                "max_rs": _to_int(rec.get("Max_Price")),
                "modal_rs": _to_int(rec.get("Modal_Price")),
                "unit": "quintal",
                "date": rec.get("Arrival_Date", ""),
            })
    return rows


def main():
    prices = fetch()
    doc = {
        "synced_at": datetime.now(timezone.utc).isoformat(timespec="minutes"),
        "source": "Agmarknet via data.gov.in",
        "unit": "quintal",
        "prices": prices,
    }
    TARGET.write_text(json.dumps(doc, ensure_ascii=False, indent=2), encoding="utf-8")
    print(f"Wrote {len(prices)} price rows -> {TARGET}")


if __name__ == "__main__":
    main()
