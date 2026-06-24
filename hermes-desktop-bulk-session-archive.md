Title: Fixing the Session Archive Problem in Hermes Desktop
Date: 2026-06-19
Tags: hermes, desktop, devtools, fix, opensource
Description: Built a bulk archive endpoint for Hermes sessions. Documented the full PR workflow: issue → branch → test → push → PR. Also found a terminal approval bypass bug.

---

## The Problem: 37 Sessions and No Way to Clean Them

Hermes Desktop sidebar showed 37 sessions. Each needed clicking the kebab menu → Archive. No multi-select, no bulk action.

I use Hermes for everything - blog drafts, job hunt tracking, Clojure experiments. The session list grows fast.

Need: Select multiple sessions and archive them in one click.

## PR Workflow

```d2
# Diagram 117
```

## The Terminal Approval Bug

```d2
# Diagram 118
```

## Implementation Details

### Backend

Added `POST /api/sessions/bulk-archive` endpoint in `hermes_cli/web_server.py`:
```python
class BulkArchiveSessions(BaseModel):
    ids: List[str]
    archived: bool = True
    profile: Optional[str] = None
```

Same pattern as bulk-delete, with 500-item safety cap.

### Frontend

Added helper in `apps/desktop/src/hermes.ts`:
```typescript
export function archiveSessions(ids: string[], archived: boolean, profile?: string) {
  return window.hermesDesktop.api(...) // calls backend endpoint
}
```

### Tests

Added `test_bulk_archive_sessions` and `test_bulk_unarchive_sessions` in `tests/hermes_cli/test_web_server.py`.

## Remaining Work

- **UI checkboxes** in sessions sidebar (React selection state + bulk action bar)
- **Terminal approval prompt** - desktop should block terminal commands until user approves

## Links

- [Issue #48843](https://github.com/NousResearch/hermes-agent/issues/48843)
- [PR #2](https://gitlab.com/nurazhar/hermes-agent/pull/2)