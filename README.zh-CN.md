# Flareprox [![Build and Release](https://github.com/gandli/Flareprox_Burp_Extension/actions/workflows/build.yml/badge.svg)](https://github.com/gandli/Flareprox_Burp_Extension/actions/workflows/build.yml)

一个用于 Burp Suite 的扩展，它通过 Cloudflare Workers 作为透传代理，将请求从 Cloudflare 边缘发出，并在不同请求间可能获取不同的出口 IP。

## 特性
- 通过 Cloudflare Workers 将请求转发到目标主机。
- 配置简单，不需要额外的本地代理改动。
- 基于 Burp Suite Montoya API 设计。

## 环境要求
- `Burp Suite`（社区版或专业版）。
- 运行环境为 `Java 17+`。
- 一个 `Cloudflare` 账户用于部署 Worker。

## 快速开始
1. 构建插件：`./gradlew build`
2. 在 Burp 中进入 `Extender` → `Extensions` → `Add`，选择 `Java` 并加载 `build/libs` 下生成的 JAR。
3. 在扩展设置中配置你的 Worker 入口（例如 `https://your-worker.workers.dev`）。
4. 正常使用 Burp；所有出站请求将经由该 Worker 代理。

## Cloudflare Worker（最小模板）
你的 Worker 需要把进入的请求转发回原始目标。以下是一个可根据需要调整的最小示例：

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

说明：请根据扩展的转发格式进行调整。更多模板与选项请参考 `docs/resources.md`。

## 开发
- 项目使用 Gradle Kotlin DSL；源码位于 `src/main/java`。
- 参考 `docs/development-best-practices.md` 与 `docs/montoya-api-examples.md`。

## 故障排查
- 确认 Burp 使用 Java 17+：`Extender` → `Options` → `Java Environment`。
- 确认你的 Worker URL 在当前网络环境中可访问。
- 在 Burp 的 `Errors` 标签以及 Worker 日志中查看错误细节。

英文文档请见 `README.md`。