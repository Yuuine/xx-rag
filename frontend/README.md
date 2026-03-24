# xx-rag frontend

独立前端模块（Vue 3 + TypeScript + Vite），用于对接 `xx-rag` 后端 API 和 WebSocket。

## 开发启动

```bash
npm install
npm run dev
```

默认前端地址：`http://localhost:5173`

## 后端代理

开发模式下由 Vite 代理：

- `/xx` -> `http://localhost:8081`
- `/ws-chat` -> `ws://localhost:8081`

可通过环境变量覆盖后端地址：

```bash
BACKEND_URL=http://localhost:8081 npm run dev
```

## 构建

```bash
npm run build
npm run preview
```

## 页面路由

- `/` 聊天页面（上传、会话管理、流式问答）
- `/docs` 文档列表页面（搜索、分页、删除）
