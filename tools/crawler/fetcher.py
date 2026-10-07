from __future__ import annotations

import time
import logging

try:
    from curl_cffi import requests as cureq
    HAS_CURL_CFFI = True
except ImportError:
    import cloudscraper
    HAS_CURL_CFFI = False
    _scraper = cloudscraper.create_scraper()

LOG = logging.getLogger("fetcher")

DEFAULT_HEADERS = {
    "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36",
    "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8",
    "Accept-Language": "en-US,en;q=0.9",
    "Sec-Ch-Ua": '"Chromium";v="128", "Not;A=Brand";v="24", "Google Chrome";v="128"',
    "Sec-Ch-Ua-Mobile": "?0",
    "Sec-Ch-Ua-Platform": '"Windows"',
    "Sec-Fetch-Dest": "document",
    "Sec-Fetch-Mode": "navigate",
    "Sec-Fetch-Site": "none",
    "Sec-Fetch-User": "?1",
    "Upgrade-Insecure-Requests": "1",
}


def fetch_html(url: str, max_retries: int = 3, timeout: int = 25) -> str | None:
    """
    Fetch raw HTML from URL using Chrome impersonation with automatic retries.
    Returns HTML string on success, or None on failure / 404.
    """
    for attempt in range(1, max_retries + 1):
        try:
            if HAS_CURL_CFFI:
                resp = cureq.get(
                    url,
                    headers=DEFAULT_HEADERS,
                    impersonate="chrome124",
                    timeout=timeout
                )
            else:
                resp = _scraper.get(
                    url,
                    headers=DEFAULT_HEADERS,
                    timeout=timeout
                )

            if resp.status_code == 200:
                return resp.text
            elif resp.status_code == 404:
                LOG.warning("HTTP 404 Not Found: %s", url)
                return None
            elif resp.status_code in (403, 429):
                LOG.warning("Rate limited (HTTP %s) on attempt %d for %s", resp.status_code, attempt, url)
                time.sleep(2.0 * attempt)
            else:
                LOG.warning("HTTP %s on attempt %d for %s", resp.status_code, attempt, url)
                time.sleep(1.0)
        except Exception as e:
            LOG.warning("Network exception on attempt %d for %s: %s", attempt, url, e)
            time.sleep(1.0)

    return None
