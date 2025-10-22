# Flareprox_Burp_Extension [![Build and Release](https://github.com/gandli/Flareprox_Burp_Extension/actions/workflows/build.yml/badge.svg)](https://github.com/gandli/Flareprox_Burp_Extension/actions/workflows/build.yml)

Flareprox is a Burp Suite extension that provisions on-demand Cloudflare Worker proxies and tunnels your traffic through Cloudflare's edge network. It helps red teamers, penetration testers, and researchers originate requests from diverse Cloudflare PoPs without changing their local proxy layout, making it easier to blend in with Cloudflare-originated traffic or rotate IPs during testing.

## Overview
- **Problem space**: Many assessments require egress IP rotation or Cloudflare-aligned traffic to avoid IP-based throttling and geofencing. Manually deploying Workers and updating Burp routing rules is error-prone.
- **Solution**: Flareprox integrates directly with Burp Suite via the Montoya API, automates Worker creation, and manages the entire lifecycle of proxy endpoints from within Burp's UI.
- **Workflow**: Configure Cloudflare credentials once, deploy any number of Worker-backed endpoints, probe their exit IPs, and route Burp traffic through selected proxies.

## Core Features
- **One-click Worker provisioning**: Deploy Cloudflare Worker endpoints from Burp, using either module Workers (default) or classic Workers for older accounts.
- **Endpoint inventory & cleanup**: List active `flareprox-*` deployments, copy URLs, and remove all proxies when you are done.
- **Automated exit IP probing**: Background tasks hit httpbin to determine each endpoint's current egress IP and surface it directly in the UI.
- **Swing-based settings tab**: A dedicated Montoya tab provides credential management, deploy/list/cleanup controls, and a sortable table of endpoints with clipboard shortcuts.
- **Credential persistence**: Cloudflare API tokens and account IDs are stored using Burp's persistence API, with validation and masked input toggles.
- **Context menu integration**: Adds a "Rotating IP Proxy" context menu entry to help inspect the effective request URL while testing.

## Tech Stack & Architecture
- **Languages & build tooling**: Java 21, Gradle (Kotlin DSL), and the Burp Suite Montoya API (`net.portswigger.burp.extensions:montoya-api`).
- **Cloudflare integration**: `SimpleCloudflareService` uses Java's `HttpClient` to upload Worker scripts, enable custom subdomains, enumerate deployments, and perform cleanup through Cloudflare's REST APIs.
- **Worker generation**: `CloudflareWorkerUtils` produces both module and classic Worker scripts and handles multipart upload payloads.
- **Extension shell**: `Extension` wires Montoya hooks, builds the Swing UI, persists credentials, and orchestrates actions (deploy, list, delete, IP probe) with `FlareProx`.
- **Concurrency helpers**: Background executor services perform exit-IP probes so the UI stays responsive while deployments complete.

## Requirements
- `Burp Suite` (Community or Professional) with Montoya API support.
- `Java 21` (or newer) runtime selected in Burp (`Extender` → `Options` → `Java Environment`).
- A `Cloudflare` account with permissions to create Workers and an API token scoped to "Edit Cloudflare Workers".

## Quick Start
1. Build the extension: `./gradlew build`.
2. In Burp, navigate to `Extender` → `Extensions` → `Add`, choose `Java`, and load the JAR from `build/libs`.
3. Open the **FlareProx** settings tab, enter your Cloudflare API token and account ID, and let Burp persist them.
4. Use the **Operations** panel to deploy the desired number of Worker endpoints, list existing ones, or clean them up when finished.
5. Route traffic through the generated Worker URLs; outbound requests will originate from Cloudflare edge locations.

## Cloudflare Worker Templates
Flareprox ships with opinionated Worker scripts and automatically uploads them for you, but you can supply your own Worker if preferred.

### Module Worker (default)
```js
export default {
  async fetch(request, env, ctx) {
    const targetUrl = request.headers.get("X-Target-URL")
      ?? new URL(request.url).searchParams.get("url");
    if (!targetUrl) {
      return new Response(JSON.stringify({ error: "No target URL" }), {
        status: 400,
        headers: { "Content-Type": "application/json" },
      });
    }

    const target = new URL(targetUrl);
    const proxied = new Request(target, {
      method: request.method,
      headers: request.headers,
      body: ["GET", "HEAD"].includes(request.method) ? undefined : request.body,
    });

    const response = await fetch(proxied);
    const headers = new Headers(response.headers);
    headers.set("Access-Control-Allow-Origin", "*");
    headers.set("Access-Control-Allow-Headers", "*");
    return new Response(response.body, {
      status: response.status,
      statusText: response.statusText,
      headers,
    });
  },
};
```

### Classic Worker (legacy accounts)
```js
addEventListener('fetch', event => event.respondWith(handleRequest(event.request)));
async function handleRequest(request) {
  const url = new URL(request.url);
  const targetUrl = url.searchParams.get('url');
  if (!targetUrl) {
    return new Response(JSON.stringify({ error: 'No target URL' }), {
      status: 400,
      headers: { 'Content-Type': 'application/json' },
    });
  }
  const response = await fetch(targetUrl, request);
  const headers = new Headers(response.headers);
  headers.set('Access-Control-Allow-Origin', '*');
  headers.set('Access-Control-Allow-Headers', '*');
  return new Response(response.body, {
    status: response.status,
    statusText: response.statusText,
    headers,
  });
}
```

## Troubleshooting
- Verify Burp is configured to use Java 21+ under `Extender` → `Options` → `Java Environment`.
- Confirm your Cloudflare API token includes `Account.Workers Scripts` permissions and matches the account ID in use.
- Check Burp's **Errors** tab and the Cloudflare Worker logs (`wrangler tail` or dashboard) if deployments fail or IP probes return `n/a`.

For Chinese documentation, see `README.zh-CN.md`.
