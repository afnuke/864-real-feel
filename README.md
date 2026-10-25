# 864 Real Feel

Weather, made simple — for Greenville County, South Carolina.

The thermometer gives you a number. This app tells you what it means: what you're actually walking into.

**[⬇ Download the app (APK)](https://raw.githubusercontent.com/afnuke/864-real-feel/main/864%20Real%20Feel%20APK.apk%20(1))** — free. Android 8.0+.

## What it does

One screen, readable in one glance, half-asleep:

- **Two numbers** — the actual temperature and the real feel (apparent temperature), with a plain verdict: *"Feels cooler than the thermometer says."*
- **The coat line** — below 55°F real feel is coat weather. If it drops below 55 after dark, it says so before you leave without one: *"Coat weather after dark."*
- **Tonight after dark** — the lowest real feel between sunset and 7 AM, because the cold that matters is the one that meets you in the morning.
- **The heat line** — real feel at 95°F or above: *"Dangerously hot — shade, water, and breaks."*
- **The rain line** — *"Dry all day."* or *"Rain likely after 3 PM — umbrella weather."*
- **Faded Pixel-style backdrop** that matches the sky, gentle elevator music synthesized in code (a nod to the old Weather Channel), and honest data labels — if the numbers are stale, the screen says so.

## By design

- **Fixed location.** This app serves Greenville County, SC only (34.8526, -82.3940). It asks for **no location permission** — the only permission it uses is Internet, to fetch the weather.
- **Data:** [Open-Meteo](https://open-meteo.com) (free, no account, no tracking).
- No ads, no accounts, no analytics.

## Version

**v1.0 — first public build, October 25, 2026.** Designed and built in one day in Greenville, SC.

## Source notes

Full Kotlin/Jetpack Compose source is in this repo. A few binary assets (launcher icon PNGs, the bundled font) live inside the APK itself rather than this text push.

---

Built by Bryan Lee (Afnuke), in AI collaboration. © 2026 Afnuke.
