// Stop hook: nudges Claude to keep project memory current before ending a turn.
//
// Reads the hook payload on stdin. If the turn is already a continuation
// triggered by this same hook (stop_hook_active === true), it allows the stop
// so we never loop forever. Otherwise it blocks the stop once and feeds a
// reminder back to Claude to update memory/ + MEMORY.md per CLAUDE.md.
//
// Cross-platform on purpose: invoked as `node ...` so the command is identical
// in bash and PowerShell, and needs no jq.

let raw = "";
process.stdin.on("data", (chunk) => {
  raw += chunk;
});
process.stdin.on("end", () => {
  // Strip a leading UTF-8 BOM (some shells prepend one) before parsing.
  let cleaned = raw.trim();
  if (cleaned.charCodeAt(0) === 0xfeff) {
    cleaned = cleaned.slice(1);
  }

  let active = false;
  try {
    active = JSON.parse(cleaned).stop_hook_active === true;
  } catch (_) {
    // Malformed/empty payload: treat as not-active so the reminder still fires.
  }

  if (active) {
    // Reminder already delivered this turn — let the turn end.
    process.exit(0);
  }

  process.stdout.write(
    JSON.stringify({
      decision: "block",
      reason:
        "Project Memory check (CLAUDE.md rule): did anything memory-worthy happen this turn — a decision, gotcha, agreed convention, or a preference the user stated? " +
        "If yes, write or update the matching file under memory/ and refresh the pointer in memory/MEMORY.md now, then stop. " +
        "Do NOT duplicate what code, git, or CLAUDE.md already records. " +
        "If nothing is memory-worthy, reply in one short line that there is nothing to record, then stop.",
    })
  );
  process.exit(0);
});
