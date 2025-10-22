# Flareprox [![Build and Release](https://github.com/gandli/Flareprox_Burp_Extension/actions/workflows/build.yml/badge.svg)](https://github.com/gandli/Flareprox_Burp_Extension/actions/workflows/build.yml)

A Burp Suite extension that routes traffic through Cloudflare Workers as a pass-through proxy, letting your requests originate from Cloudflare's edge and potentially changing IP per request.

## Features
- Route requests via Cloudflare Workers to the target host.
- Simple setup, no local proxy changes beyond Burp.
- Designed for Burp Suite Montoya API.

## Requirements
- `Burp Suite` (Community or Professional).
- `Java 17+` at runtime for the extension.
- A `Cloudflare` account to deploy a Worker.

## Quick Start
1. Build the plugin: `./gradlew build`
2. In Burp, go to `Extender` → `Extensions` → `Add`, choose `Java` and select the JAR from `build/libs`.
3. Open the extension's settings and set your Worker endpoint (e.g. `https://your-worker.workers.dev`).
4. Use Burp as usual; outbound requests will be proxied through the Worker.

## Cloudflare Worker (minimal template)
Your Worker should forward the incoming request to the original destination. Here is a minimal template you can adapt:

```js
export default {
  async fetch(request) {
    const target = request.headers.get('X-Forwarded-Url');
    if (!target) return new Response('Missing X-Forwarded-Url', { status: 400 });
    const url = new URL(target);
    return fetch(url, request);
  }
}
```

Note: Adjust this to match the extension’s forwarding format. See `docs/resources.md` for patterns and options.

## Development
- Gradle Kotlin DSL project; source under `src/main/java`.
- See `docs/development-best-practices.md` and `docs/montoya-api-examples.md`.

## Troubleshooting
- Ensure Java 17+ is selected in Burp: `Extender` → `Options` → `Java Environment`.
- Verify your Worker URL is reachable from your network.
- Check Burp's `Errors` tab and the Worker logs for failures.

For Chinese documentation, see `README.zh-CN.md`.
