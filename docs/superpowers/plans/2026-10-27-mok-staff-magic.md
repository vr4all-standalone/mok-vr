# MOK staff magic implementation plan

> Implement inline with the executing-plans workflow. The user asked for a little game-themed magical effect after publishing1.0.4; that release is already public and download-hash verified.

**Goal:** replace the native staff's smoke and orange flash with an emerald pulse and small gold magical motes in a separate1.0.5 test build.

**Architecture:** expose the loaded native staff identity in HandsModel. Reuse the existing procedural OpenGL glow renderer, adding two inexpensive shapes and bounded per-player burst state. The pulse follows the current crystal tip; motes leave it in room space. Arcade/custom models retain their firing effects. No game mechanics, input or licensed asset files change.

**Tech stack:** existing C++17, GLM, desktop OpenGL/OpenGL ES shaders, Visual Studio/Android builds.

- [x] Publish1.0.4 first; preserve its exact APK/ZIP/checksums locally and its source tag remotely.
- [x] Add failing production-renderer tests for no staff smoke/flames, green/gold emission, stereo idempotence, P2, idle/menu gating, legacy models, bounded storage and context reset.
- [x] Implement native staff identity, procedural magical shapes/bursts and green local illumination.
- [x] Run renderer and MOK input regressions, inspect captured spell frames and review the change.
- [ ] Build a signed1.0.5 test APK and matching PC archive; record final hashes and device status in the ignored build verification report.
- [ ] Keep the next build available for user review; do not replace the published1.0.4 assets.
