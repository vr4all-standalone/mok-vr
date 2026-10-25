# MOK validation

Standalone edition derived from shared-menu Lupin revision bcb7fb050. MOK's camera, field-of-view patch, calibration and native staff extraction were already implemented upstream; this edition reuses them.

## Automated evidence

- Native loaded-title policy: 9 checks in each of enabled, disabled and explicitly-zero policy modes, all pass.
- Shared production settings/held-input functions: 132 checks pass. Preparation release gating adds 835 checks across all held-button combinations. The held Start/B leak was reproduced before the fix and is absent in the same simulator case afterward.
- MOK reload/profile/rendering regression checks extract the production branches and draw commands. The original source fails the no-implicit-floor-reload and spare-hand checks; the corrected source passes. Native MOK transport, B/Y, point-mode arming and pistol/custom-model rendering are covered.
- Staff cache: 284 checks pass, including every truncation of a synthetic valid payload, wrong version/magic, NaN/Inf, invalid texture/mesh/vertex bounds and trailing bytes. The actual extracted staff also loads successfully.
- Java: 67 MOK file/setup tests pass, with 67 inherited Lupin tests also green. Wrong discs, malformed CHD headers/maps, partial raw tracks, missing PIC/BIOS, bounded search and direct-launch bypass are covered.
- Release package validator: 40 checks pass. Identity, sole launcher, version/source provenance, portable PC defaults, file exclusion, model credits and GPL/cgltf notices are checked again against the final archives.
- VR4ALL full suite: 328 passed, 2 skipped, 0 failed when run sequentially without competing native compilation. The standalone card keeps the existing mok/chd/naomi mapping keys and destinations. Its new package is independent of HOTD2. Selector and rescan regressions pass.

## Runtime evidence

Fresh simulator preparation captured the original hero's staff at 31 game seconds, about 16 wall seconds on this development PC. Generated cache: 2,157,820 bytes. The next launch logged a valid cache reuse and selected In-game staff. The renderer displays the native staff without a spare open hand; solo left/right triggers still transfer it.

The shared weapon menu now selects native staff, Arcade, Zapper and compatible custom GLBs. Cache validation is shared by preparation and renderer loading. Skipping preparation disables further capture for that session; a later start retries.

Native MOK gameplay confirms that pointing off screen without a trigger leaves ammo unchanged, while cabinet B refills it: the observed sequence is 5, 5, 4, 6, 5, 6 for shot, point-away, shot, B, shot, off-screen trigger. MOK retains that native transport. The mismatch is the inherited two-player point-down shortcut firing in Shoot mode; it is disabled for the MOK profile, including when a custom weapon is chosen. Point mode retains its once-per-screen-exit reload. Captures and final archive checks remain in local build/test artifacts.

Further runtime scenarios and final artifact/source hashes are recorded in the local dist/build-verification.json after final packaging. Generated game files, screenshots, saves and ROMs remain outside published release archives.

## Limits

The signed test build was installed on Quest 3S; physical play and later-stage validation remain pending. The user-requested staff cache reset was verified, with saves, settings and bundled controller models preserved. Raw CUE/GDI structural recognition is tested, but actual raw-disc boot is untested. This port does not add a newly measured MOK native magazine/accepted-shot model or a cutscene detector. See mok-headset-checklist.md.
