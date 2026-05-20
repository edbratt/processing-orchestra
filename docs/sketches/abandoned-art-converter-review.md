# AbandonedArt Converter Review

Review date: 2026-04-20

Scope:
- Reviewed local copies of sketches from Matt Pearson's 100 Abandoned Artworks project.
- Original project URL: [abandonedart.org](http://abandonedart.org)
- Public reference: [100 Abandoned Artworks - VICE](https://www.vice.com/en/article/100-abandoned-artworks/)
- Focused on converter-readiness after adding:
  - top-level helper type support
  - `size(..., renderer)` preservation
  - tests/fixtures for helper classes plus `P3D`

Notes:
- All 15 sketches in the reviewed subfolders now convert mechanically with the current converter.
- "Top-level types" means PDE helper classes such as `class Particle { ... }`.
- "Best use of new feature" means the sketch meaningfully exercises the new helper-type and/or `P3D` preservation path, not just the old basic field/method path.

## Summary Table

| Sketch | Top-level types | Renderer | Mouse usage | Mouse handlers | Assets | Best use of new feature? | Suggested next step |
|---|---:|---|---|---|---|---|---|
| `AbandonedArtworks_75` | No | `P3D` | None | None | None | Medium | Good `P3D` preservation smoke test |
| `AbandonedArtworks_77` | Yes | Default | None | None | None | High | Strong top-level-type regression test |
| `AbandonedArtworks_78` | Yes | Default | None | None | None | High | Stronger top-level-type stress test than `77` |
| `AbandonedArtworks_79` | No | `P3D` | None | None | None | Medium | Good `P3D` preservation test |
| `AbandonedArtworks_80` | Yes | Default | None | `mouseReleased` | None | Medium | Good helper-type test, but no dedicated browser mapping for `mouseReleased` yet |
| `AbandonedArtworks_82` | Yes | Default | None | `mousePressed` | None | Medium | Good helper-type plus touch-press candidate |
| `AbandonedArtworks_83` | Yes | Default | None | `mousePressed` | None | Medium | Good helper-type plus touch-press candidate |
| `AbandonedArtworks_87` | No | `P3D` | None | None | `PImage` | Medium | Good `P3D` test, but also exercises image asset dependency |
| `AbandonedArtworks_88` | Yes | `P3D` | None | None | `PImage` | Very high | Best combined test after `90`: helper types + `P3D` + assets |
| `AbandonedArtworks_89` | No | Default | None | `mousePressed` | None | Low | Uses old converter path only |
| `AbandonedArtworks_90` | Yes | `P3D` | None | `mousePressed` | None | Very high | Primary validation case for the new converter work |
| `AbandonedArtworks_96` | No | Default | None | `mousePressed` | None | Low | Uses old converter path only |
| `AbandonedArtworks_97` | Yes | Default | `mouseX`, `mouseY` | `mousePressed` | None | High | Good follow-up for helper types plus mouse-to-touch decisions |
| `AbandonedArtworks_98` | Yes | Default | None | None | `PImage` | Medium | Good helper-type test, but asset handling may dominate |
| `AbandonedArtworks_100` | Yes | Default | `mouseX`, `mouseY` | `mousePressed` | None | High | Good follow-up for helper types plus mouse-to-touch decisions |

## Best Candidates To Try Next

Recommended order:

1. `AbandonedArtworks_88`
   - Exercises the new converter path well.
   - Uses helper classes and `P3D`.
   - Also reveals whether image-dependent sketches need extra migration notes.

2. `AbandonedArtworks_77`
   - Clean helper-type test without image dependencies.
   - Good for validating nested class emission stays stable.

3. `AbandonedArtworks_78`
   - Similar to `77`, but denser object structure.
   - Better stress test for helper-type preservation.

4. `AbandonedArtworks_100`
   - Useful because it mixes a helper class with `mouseX`/`mouseY` and `mousePressed()`.
   - Good candidate for future browser-touch refinement work.

5. `AbandonedArtworks_97`
   - Similar to `100`, with helper classes plus mouse-driven behavior.
   - Good secondary test for mouse-oriented migration decisions.

## Lower Priority For The New Feature

- `AbandonedArtworks_89`
  - Converts fine, but does not exercise top-level types or renderer preservation.

- `AbandonedArtworks_96`
  - Same as `89`; still useful for touch-press work, but not for the new converter extension.

## Interaction Mode Fit

| Sketch | Likely `browser-touch` | Likely `browser-touch-press` | Likely `browser-touch-drag` | Likely custom adaptation needed | Notes |
|---|---|---|---|---|---|
| `75` | No | No | No | Yes | No local input; best treated as designed adaptation if browser control is desired |
| `77` | No | No | No | Yes | No local input; helper-type preservation is useful, but interaction mapping is manual |
| `78` | No | No | No | Yes | Same as `77`; visually rich but not input-driven |
| `79` | No | No | No | Yes | No local input; better as a touch/motion redesign than a mode conversion |
| `80` | No | No | No | Yes | Has `mouseReleased`, which the converter does not map specially yet |
| `82` | No | Yes | No | Maybe | Simple `mousePressed` path plus helper class makes this a good touch-press candidate |
| `83` | No | Yes | No | Maybe | Similar to `82`; likely clean touch-press conversion |
| `87` | No | No | No | Yes | No local input and uses image assets; best as a custom adaptation |
| `88` | No | No | No | Yes | Good structural converter test, but browser interaction design is still manual |
| `89` | No | Yes | No | Maybe | Simple `mousePressed` behavior fits touch-press well |
| `90` | No | Yes | No | Maybe | Structure now converts; browser-touch-press is plausible, but richer adaptation still likely |
| `96` | No | Yes | No | Maybe | Already a clean touch-press style sketch |
| `97` | Partial | No | No | Yes | Uses `mouseX`/`mouseY` plus `mousePressed`; touch-position mapping may be partial, not fully automatic |
| `98` | No | No | No | Yes | No local input and uses image assets; best as a custom adaptation |
| `100` | Partial | No | No | Yes | Same class as `97`: helper type plus `mouseX`/`mouseY` plus `mousePressed` means partial touch fit only |

Mode guidance:
- `browser-touch`
  - best for sketches that mainly use `mouseX` and `mouseY` without mouse-handler methods
- `browser-touch-press`
  - best for sketches with a simple `mousePressed()` entry point and no drag/release complexity
- `browser-touch-drag`
  - best for sketches whose main interaction is `mouseDragged()`
- custom adaptation
  - best when the sketch has no local input, uses unsupported handler shapes, or needs a more intentional mapping to touch, motion, sliders, or multi-user behavior

## Raw Review Signals

| Sketch | Helper types | `P3D` | `mouseX`/`mouseY` | `pmouse*` | `mousePressed` | `mouseDragged` | `mouseReleased` | `PImage`/`loadImage` |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| `75` | No | Yes | No | No | No | No | No | No |
| `77` | Yes | No | No | No | No | No | No | No |
| `78` | Yes | No | No | No | No | No | No | No |
| `79` | No | Yes | No | No | No | No | No | No |
| `80` | Yes | No | No | No | No | No | Yes | No |
| `82` | Yes | No | No | No | Yes | No | No | No |
| `83` | Yes | No | No | No | Yes | No | No | No |
| `87` | No | Yes | No | No | No | No | No | Yes |
| `88` | Yes | Yes | No | No | No | No | No | Yes |
| `89` | No | No | No | No | Yes | No | No | No |
| `90` | Yes | Yes | No | No | Yes | No | No | No |
| `96` | No | No | No | No | Yes | No | No | No |
| `97` | Yes | No | Yes | No | Yes | No | No | No |
| `98` | Yes | No | No | No | No | No | No | Yes |
| `100` | Yes | No | Yes | No | Yes | No | No | No |

## Current Conclusion

The new converter feature is already justified by `AbandonedArtworks_90`, but the strongest broader validation set is:

- `88`
- `77`
- `78`
- `100`
- `97`

That group gives a good spread across:
- helper-type-only sketches
- helper-types plus `P3D`
- helper-types plus mouse-driven logic
- helper-types plus asset loading

## Current Browser-Control Candidates

Strongest candidates for direct converter modes:
- `82` -> likely `browser-touch-press`
- `83` -> likely `browser-touch-press`
- `89` -> likely `browser-touch-press`
- `90` -> likely `browser-touch-press`, though custom adaptation may be more interesting
- `96` -> likely `browser-touch-press`

Best candidates for custom browser-native redesign:
- `75`
- `77`
- `78`
- `79`
- `88`
- `98`

Sketches worth revisiting if `browser-touch` becomes more tolerant of mixed mouse usage:
- `97`
- `100`
