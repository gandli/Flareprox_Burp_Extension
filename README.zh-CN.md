# Flareprox [![Build and Release](https://github.com/gandli/Flareprox_Burp_Extension/actions/workflows/build.yml/badge.svg)](https://github.com/gandli/Flareprox_Burp_Extension/actions/workflows/build.yml)

Flareprox 是一款 Burp Suite 扩展，能够按需部署 Cloudflare Worker 代理，并通过 Cloudflare 边缘网络转发流量。它帮助红队、渗透测试人员和安全研究者从不同的 Cloudflare PoP 发起请求，无需改动本地代理配置，便于与 Cloudflare 流量混淆或在测试期间轮换出口 IP。

## 项目概述
- **问题场景**：许多安全测试场景需要出口 IP 轮换或与 Cloudflare 流量对齐，以规避基于 IP 的限流和地理围栏。手动部署 Worker 并更新 Burp 路由规则容易出错。
- **解决方案**：Flareprox 通过 Montoya API 直接集成到 Burp Suite，自动化 Worker 创建，并在 Burp 界面中管理代理端点的完整生命周期。
- **工作流程**：一次性配置 Cloudflare 凭证，部署任意数量的 Worker 端点，探测其出口 IP，并将 Burp 流量路由到所选代理。

## 核心功能
- **一键 Worker 部署**：从 Burp 部署 Cloudflare Worker 端点，支持模块 Worker（默认）或经典 Worker（旧账户）。
- **端点清单与清理**：列出所有活动的 `flareprox-*` 部署，复制 URL，并在完成后一键删除所有代理。
- **自动出口 IP 探测**：后台任务通过 httpbin 确定每个端点的当前出口 IP，并直接在 UI 中显示。
- **Swing 设置选项卡**：专用的 Montoya 选项卡提供凭证管理、部署/列表/清理控件，以及带剪贴板快捷方式的可排序端点表格。
- **凭证持久化**：Cloudflare API Token 和 Account ID 通过 Burp 持久化 API 存储，支持校验和脱敏输入切换。
- **上下文菜单集成**：添加"轮换 IP 代理"上下文菜单条目，帮助在测试时检查实际请求 URL。

## 技术栈与架构
- **语言与构建工具**：Java 21、Gradle（Kotlin DSL）和 Burp Suite Montoya API（`net.portswigger.burp.extensions:montoya-api`）。
- **Cloudflare 集成**：`SimpleCloudflareService` 使用 Java 的 `HttpClient` 上传 Worker 脚本、启用自定义子域、枚举部署并通过 Cloudflare REST API 执行清理。
- **Worker 生成**：`CloudflareWorkerUtils` 生成模块和经典 Worker 脚本，并处理 multipart 上传负载。
- **扩展外壳**：`Extension` 连接 Montoya 钩子、构建 Swing UI、持久化凭证，并通过 `FlareProx` 编排操作（部署、列表、删除、IP 探测）。
- **并发辅助**：后台执行器服务执行出口 IP 探测，使 UI 在部署完成期间保持响应。

## 环境要求
- `Burp Suite`（社区版或专业版），支持 Montoya API。
- `Java 21`（或更高版本）运行时，在 Burp 中选择（`Extender` → `Options` → `Java Environment`）。
- 具有创建 Worker 权限的 `Cloudflare` 账户，以及范围为"编辑 Cloudflare Workers"的 API Token。

## 快速开始
1. 构建扩展：`./gradlew build`。
2. 在 Burp 中进入 `Extender` → `Extensions` → `Add`，选择 `Java` 并加载 `build/libs` 下的 JAR。
3. 打开 **FlareProx** 设置选项卡，输入 Cloudflare API Token 和 Account ID，让 Burp 持久化它们。
4. 使用 **Operations** 面板部署所需数量的 Worker 端点，列出现有端点或在完成后清理它们。
5. 将流量路由到生成的 Worker URL；出站请求将从 Cloudflare 边缘位置发起。

## Cloudflare Worker 模板
Flareprox 附带预设的 Worker 脚本并自动上传，但您可以根据需要提供自己的 Worker。

### 模块 Worker（默认）
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

### 经典 Worker（旧账户）
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

## 故障排查
- 确认 Burp 配置为使用 Java 21+ 运行时（`Extender` → `Options` → `Java Environment`）。
- 确认 Cloudflare API Token 包含 `Account.Workers Scripts` 权限并与使用的 Account ID 匹配。
- 如果部署失败或 IP 探测返回 `n/a`，请检查 Burp 的 **Errors** 选项卡和 Cloudflare Worker 日志（`wrangler tail` 或控制台）。

英文文档请见 `README.md`。
