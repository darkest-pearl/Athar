# Provisional design system
Recommended: Garden. Warm ivory #FAF7EF, dark green ink #182E27, green action #236247, soft green panels #E4EBDD. Spacious reading and restrained accents fit audio-led study and older readers. Serif English titles introduce warmth; body uses native fallback. Typography is provisional until Ethiopic text review.

Alternative: Editorial. Lavender-white #F5F2FF, cobalt action #2848A0, yellow panels #FFD66E. Stronger contrast and magazine hierarchy are more energetic. One component tree and shared semantics; Settings changes tokens. Six Compose previews cover the three requested screens. Actual emulator captures are in evidence/ when available.

Spacing: 4/8dp grid, 24dp page gutters and panel padding, 16/24dp section gaps. Buttons at least 48dp, primary action 56dp; wrapping labels, vertical scroll, system inset clearance. Body 18sp / 28sp, scalable. Feedback uses text as well as color. Native Button selection/disabled semantics. Primary actions stay within content; bottom navigation reserves space.

Milestone 2 polish: Today puts the next action ahead of the compact study-day card; due practice precedes it when present. Four persistent text destinations have a selected surface and semantics, with Lesson/Quiz/Done mapped to Learn. Submitted answers become readable static cards labeled as correct or selected, while saving disables active controls. Lesson audio keeps Play, Replay, and local selection in view and folds technical identity/source switches into an expandable control. Long-form content is capped to a readable 640dp measure on wide screens; a short viewport remains scrollable. Actual emulator evidence and constraints are in `evidence/milestone2-polish.md`.

States: empty progress; development labels; disabled submission while saving; inline storage/media error; missing audio can use bundled fixture or document picker. Offline is the baseline; no network permission.

Motion: native press feedback plus a short feedback-panel fade in the second milestone. Compose follows the system animator-duration scale, including zero for reduced motion. No sound effects or autoplay; reduced motion never blocks an action.

Scripts: Ethiopic alphabet and Arabic alphabet appear only in a labeled technical test. They are not Tigrinya translations or religious passages. Arabic has explicit RTL text direction/right alignment. System fallback glyph rendering can be inspected, but real reviewed Tigrinya labels and native reading comfort remain unverified. Adopt an Ethiopic font only after glyph and licensing checks.

Tools: UI UX Pro Max CLI 2.15.0 installed project-locally under .agents/skills/ui-ux-pro-max. Read SKILL.md and used search.py design-system plus jetpack-compose accessibility/state query. Selected native semantics and 48dp targets; rejected its web landing-page/testimonial suggestion as unsuitable here. Not using its version claims as dependency authority. Impeccable not installed; official instructions inspected, browser detector is not native Android evidence.

Official setup references: https://github.com/nextlevelbuilder/ui-ux-pro-max-skill and https://github.com/pbakaus/impeccable. Installed skill files stay local and ignored, preserving them without adding the installer’s unrelated skill bundles to the application repository. Reinstall with npx --yes ui-ux-pro-max-cli@2.15.0 init --ai codex; no global skill files changed.
