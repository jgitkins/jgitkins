# JGitkins Server

**A Self-Hosted Git Server & CI Coordinator**

JGitkins Server is a Spring Boot application powered by **Eclipse JGit**. It serves as both a lightweight Git server and the central API coordinator for CI/CD pipelines.

## Target Audience
This project is designed for:
- Organizations requiring a **Self-Hosted** Git repository management solution.
- Teams looking for a customizable CI coordinator that integrates seamlessly with [jgitkins-runner](https://github.com/jgitkins/jgitkins-runner).
- Developers who need a Java-based, extensible Git server architecture.

## Architecture & Design

### Central Coordinator (Like GitLab Server)
JGitkins Server acts as the "Brain" of the system:
- **Git Server**: Manages repositories and supports standard Git operations via HTTP.
- **CI Coordinator**: It parses `Jenkinsfile` from repositories, instantiates build jobs, and dispatches them to connected Runners.
- It provides the API layer that **JGitkins Runners** poll to receive work.

### Tech Stack
- **Framework**: Spring Boot
- **Core Engine**: [Eclipse JGit](https://www.eclipse.org/jgit/) (JGit Server)
- **Protocol**: Smart HTTP (via JGit Servlet)

---

## 📋 Project Status
<!-- TASK-LIST-START -->

_No tasks found._

<!-- TASK-LIST-END -->

---

## Features & Roadmap

### Implemented (POC)
- [x] **Repository Management**: API to create and manage Bare Repositories.
- [x] **Smart HTTP Support**: Full support for `git clone`, `git fetch`, and `git push` via JGit Servlet.
- [x] **Branch Management**: API to list and manage branches.
- [x] **History & Tree**: APIs to retrieve commit history (hashes) and file trees.
- [x] **File Operations**: API to upload files (creates a commit) and view file content.

### Planned Features (Roadmap)

#### CI/CD Orchestration
- [ ] **Pipeline Instantiation**: Logic to read `Jenkinsfile` from the repository and create a Job instance.
- [ ] **Runner Dispatch**: Mechanism to assign created Jobs to connected **JGitkins Runners**.

#### Runner Management
- [ ] **Runner Registration API**: Endpoints for Runners to register themselves and report status.

#### Advanced Git Features
- [ ] **Webhooks**: Trigger events on push/merge to notify external systems or start pipelines.

## Tasks
<!-- TASKS-TABLE:START -->

| ID | Title | Status | Priority | Subtasks |
| --- | --- | --- | --- | --- |
| 1 | 신규기능 | in-progress | high | 25 |
| 2 | 리팩토링 | in-progress | high | 42 |
| 3 | 보안 | done | high | 3 |
| 4 | 운영/인프라 | pending | high | 0 |
| 5 | 버그/핫픽스 | in-progress | high | 6 |

<!-- TASKS-TABLE:END -->


---

## ❓ FAQ / Design Decisions

**Q: Why JGit instead of calling native Git?**
A: JGit allows us to embed the Git server logic directly into the Java application (Spring Boot), making it portable and easier to integrate with other Java-based services without relying on system-level Git binaries.
