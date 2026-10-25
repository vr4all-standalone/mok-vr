# MOK validation

Standalone edition derived from shared-menu Lupin revision bcb7fb050. MOK's camera, field-of-view patch, calibration and native staff extraction were already implemented upstream; this edition reuses them.

## Automated evidence

- Native loaded-title policy: 9 checks in each of enabled, disabled and explicitly-zero policy modes, all pass.
- Shared production settings/held-input functions: 132 checks pass. Preparation release gating adds 835 checks across all held-button combinations. The held Start/B leak was reproduced before the fix and is absent in the same simulator case afterward.
- Staff cache: 284 checks pass, including every truncation of a synthetic valid payload, wrong version/magic, NaN/Inf, invalid texture/mesh/vertex bounds and trailing bytes. The actual extracted staff also loads successfully.
- Java: 67 MOK file/setup tests pass, with 67 inherited Lupin tests also green. Wrong discs, malformed CHD headers/maps, partial raw tracks, missing PIC/BIOS, bounded search and direct-launch bypass are covered.
- Release package validator: 40 checks pass. Identity, sole launcher, version/source provenance, portable PC defaults, file exclusion, model credits and GPL/cgltf notices are checked again against the final archives.
- VR4ALL full suite: 328 passed, 2 skipped, 0 failed when run sequentially without competing native compilation. The standalone card keeps the existing mok/chd/naomi mapping keys and destinations. Its new package is independent of HOTD2. Selector and rescan regressions pass.

## Runtime evidence

Fresh simulator preparation captured the original hero's staff at 31 game seconds, about 16 wall seconds on this development PC. Generated cache: 2,157,820 bytes. The next launch logged a valid cache reuse and selected In-game staff. The renderer displays the native staff and hand geometry; solo left/right triggers transfer it.

The shared weapon menu now selects native staff, Arcade, Zapper and compatible custom GLBs. Cache validation is shared by preparation and renderer loading. Skipping preparation disables further capture for that session; a later start retries.

Further runtime scenarios and final artifact/source hashes are recorded in the local dist/build-verification.json after final packaging. Generated game files, screenshots, saves and ROMs remain outside published release archives.

## Limits

Physical headset validation and later-stage coverage remain pending. Raw CUE/GDI structural recognition is tested, but actual raw-disc boot is untested. This port does not add a newly measured MOK native magazine/accepted-shot model or a cutscene detector. See mok-headset-checklist.md.
