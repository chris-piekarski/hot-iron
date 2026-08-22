# Documentation

This directory contains the first-class documentation for **HotIron**: a HackRF desktop with Sweep / Listen / Watch, QSY to live hits, and a live **MCP** interface so agents copy the RF bins.

## Quick Navigation

- [MCP for AI agents](agents.md) — why MCP, tools, stdio proxy, what v1 will not do
- [Getting Started](getting-started.md)
- [Building & Running](building.md)
- [Development Guide](develop.md) (including testing and linting)
- [HackRF Hardware Setup](hardware.md)
- [Usage](operator.md) — operator UI (Quick Select, auto-gain, waterfall)
- [Architecture](architecture.md)
- [Repository stats](stats.md) — first-party LOC, packages, tests (`make stats`)
- [Contributing](contributing.md)
- [Plans](plans/README.md) — living implementation plans (keep status/checklists current)

## Diagrams

This documentation uses [Mermaid](https://mermaid.js.org/) for UML-style diagrams (flowcharts, sequence diagrams, class diagrams). These render natively on GitHub.

Current diagrams cover:
- Architecture (high-level, data flow, class, package, build-to-user)
- Build pipeline
- Development and test workflows
- Contributing process
- Getting started / radio setup
- Usage interaction loop
- Generated pies in [stats.md](stats.md)

`make mermaid` extracts every fence and parses it with mermaid-cli when `mmdc` is on `PATH` (Mermaid 11, matching current GitHub). Avoid `deploymentDiagram` (dropped in Mermaid 11) and unquoted `>` in sequence `Note` lines.

## Root-Level Files

See these files in the repository root:

- `README.md` — Project overview and quick start
- `AGENTS.md` — Guidance for AI coding agents
- `CONTRIBUTING.md` — Contribution process
- `LICENSE`

## Building the Docs

These are plain Markdown files. They are rendered nicely on GitHub and are meant to be read directly or via the repo's GitHub Pages / wiki if set up later.

When making changes:
- Update the relevant doc under `docs/`
- Keep examples up-to-date with current `make` targets (run `make help`)
- Update `AGENTS.md` and root `README.md` when adding significant new processes or targets

For the most up-to-date build instructions, always prefer `make help` over static docs.