# CLAUDE.md

See `AGENTS.md` for dev commands and conventions.

## UI/UX
- Design standards: [docs/specs/ui-ux.md](docs/specs/ui-ux.md)
- Clickable prototype of the Thymeleaf UI: `docs/prototype/` (sources in `_gen/`, build with `python3 docs/prototype/_gen/interact.py --all --strict`)

## Safety rules
Full text in [.claude/rules/code-rules.md](.claude/rules/code-rules.md).
- **Database Safety:** agents never run SQL against any database; write the statement in chat prefixed `-- NOT run by Claude — cần DBA review`. Read-only `SELECT` is exempt.
- **Git Safety:** no `git commit` without explicit confirmation each time; no push, force-push, `reset --hard`, branch or tag changes unless asked.
- **Workflow Safety:** analyse and write a plan first; code only after the plan is approved.

## Draft
`draft/` chứa file nháp tạm, bị `.gitignore` bỏ qua hoàn toàn (không bao giờ commit); `draft/docs/` chứa nháp tài liệu.
