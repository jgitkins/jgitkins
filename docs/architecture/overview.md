# Architecture

## Request Flow Summary
- A user sends a request to the jgitkins platform.
- The platform routes the request to `jgitkins-web` or `jgitkins-server` based on the request path.
- Most requests go to `jgitkins-web`, which queries `jgitkins-server` when needed.

## Diagram
```mermaid
flowchart LR
    U[User] -->|"HTTP request (most)"| W
    U[User] -->|"HTTP request (some)"| S
    subgraph JK[jgitkins]
        W[jgitkins-web]
        S[jgitkins-server]
    end
    W -->|API query| S
```
