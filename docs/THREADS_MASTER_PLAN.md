# 🏛️ Melo x Threads: Authentic Architecture Master Plan

> **Codename**: *Melo Fluid Threads*  
> **Source Evidence Base**: Extracted from official Meta Threads APK (`learning/threads_decoded/` & `learning/extracted_threads_animation/`)  
> **Target App**: Melo Android Music Player (`com.niksoriginals.melo`)

---

## 1. Ground Truth: Why Previous Code Was a "Workaround"

| Aspect | Previous "Workaround" Implementation | Official Meta Threads Architecture |
|---|---|---|
| **Letter Rendering** | Naked raw paths drawn with `StrokeCap.Round` (looks like thick marker drawings). | Compound **`<clip-path>` canal contour** + centerline Bézier ink flow (looks like carved typography flooded with liquid). |
| **Typography** | 4 straight lines for 'M', copied 'e', generic stick 'l', two arcs for 'o' (zero typographic harmony). | Bespoke geometric typography derived from **Instagram Sans Display** / **Prism Sans** with exact optical heights. |
| **Logo Design** | 6 disconnected static vertical sticks force-fitted into curve slots. | **Option B: True Carved Equalizer Canals** with rounded capsule boundaries and sequential liquid filling. |
| **Scroll Collapse** | Shrinking Compose Box width ($130\text{dp} \to 33.7\text{dp}$) with `.clip(RoundedCornerShape)` (caused black shutter bug). | **Single Fixed Canvas** ($130\text{dp} \times 36.25\text{dp}$) with native GPU translation (`logoShiftX`). Zero clipping, zero black shutter. |
| **Timing Easing** | Standard generic Compose `FastOutSlowInEasing`. | Meta's authentic **`CubicBezierEasing(0.76f, 0.0f, 0.24f, 1.0f)`**. |

---

## 2. Extracted Specifications Matrix

| Metric | Value | Reference File in Threads |
|---|---|---|
| **Container Dimensions** | **$130.0\text{dp} \times 36.25\text{dp}$** | Scaled from $186\text{dp} \times 36.25\text{dp}$ (`barcelona_wordmark_launch_vector.xml`) |
| **Viewport Ratio** | **$3.6111 \times 1.0000$** | Scaled from Threads $5.1303 \times 1.0000$ |
| **Logo Bounding Width** | **$32.0\text{dp}$** ($x = [0.000 \dots 0.885]$) | Threads '@' width = $32.7\text{dp}$ |
| **Kerning Gap (Logo to Text)** | **$6.0\text{dp}$** | Matches Threads kerning gap |
| **Cap Height ('M', 'l')** | **$29.7\text{dp}$** ($82\%$ of container height) | Threads 'T', 'h', 'd' ascender height |
| **x-Height ('e', 'o')** | **$21.7\text{dp}$** ($60\%$ of container height) | Threads 'e', 'a', 's' x-height |
| **Optical Stem Thickness** | **$5.0\text{dp}$** (Semi-Bold weight) | Threads stem width |
| **Centerline Stroke Width** | **$0.1900f$** (viewport units) | Threads `android:strokeWidth="0.1815f..0.2020f"` |
| **Cubic Bézier Interpolator** | **`(0.76, 0.0, 0.24, 1.0)`** | `$barcelona_wordmark_launch_stroke_00__0.xml` |
| **Audio SFX Asset** | `barcelona_refresh_sfx_pencap_01.m4a` | 120ms suction pop at release |
| **Total Refill Duration** | **$600\text{ms} \dots 700\text{ms}$** | Scaled from Threads 1000ms launch animation |

---

## 3. The 4 Architecture Pillars

```
┌────────────────────────────────────────────────────────────────────────┐
│  PILLAR 1: Vector Canal Blueprint (Clip-Path Contours + Centerlines)   │
│  - 6 Equalizer Bars + Dot defined as closed capsule Clip-Paths         │
│  - 'M', 'e', 'l', 'o' defined with Instagram Sans Display contours     │
│  - Centerline stroke paths running down the middle of every canal      │
├────────────────────────────────────────────────────────────────────────┤
│  PILLAR 2: Two-Layer Canal Rendering Engine                            │
│  - Layer 1 (Mould): Translucent canal grooves (alpha = 0.20f)          │
│  - Layer 2 (Ink): clipPath(canalOutline) { drawPath(centerline) }      │
│  - CubicBezierEasing(0.76f, 0f, 0.24f, 1f) on all stroke transitions   │
├────────────────────────────────────────────────────────────────────────┤
│  PILLAR 3: Single Fixed-Canvas Navigation Architecture                 │
│  - Container is strictly fixed at 130dp x 36.25dp                      │
│  - Zero box resizing, zero .clip(RoundedCornerShape)                   │
│  - Logo smoothly glides to dead center via logoShiftX translation      │
├────────────────────────────────────────────────────────────────────────┤
│  PILLAR 4: Fluid Sensory Lifecycle & Gesture Binding                   │
│  - Pull-down: Text drains first, Logo drains second (empty at 100%)    │
│  - Push-up: Instant real-time refill tracking user's finger            │
│  - Release: Haptic tick + pen-cap pop sound + 600ms live refill        │
│  - Feed Scroll: Logo stays 100% solid, glides to dead center           │
└────────────────────────────────────────────────────────────────────────┘
```

---

## 4. Task-by-Task Implementation Roadmap

### 📌 TASK 1: Vector Canal Blueprint Construction
- [ ] **1.1**: Define SVG `clipPath` contours for all 6 Equalizer Canals + 1 Dot (rounded-capsule outer boundaries).
- [ ] **1.2**: Define SVG `clipPath` contours for 'M', 'e', 'l', 'o' matching Instagram Sans Display metrics ($29.7\text{dp}$ cap height, $21.7\text{dp}$ x-height, $5.0\text{dp}$ stem width).
- [ ] **1.3**: Define centerline fluid paths for each canal with normalized coordinates ($x = [0 \dots 3.6111]$, $y = [0 \dots 1.0]$).
- [ ] **1.4**: Unit-test path parsing and verify zero syntax errors.

### 📌 TASK 2: Two-Layer Canal Renderer Engine (`MeloWordmark.kt`)
- [ ] **2.1**: Implement Layer 1 (The Mould): Render all canal contours with translucent watermark alpha (`0.20f`).
- [ ] **2.2**: Implement Layer 2 (The Ink): Render fluid strokes inside their respective `clipPath` canals.
- [ ] **2.3**: Implement Meta's authentic `ThreadsEasing = CubicBezierEasing(0.76f, 0.0f, 0.24f, 1.0f)`.
- [ ] **2.4**: Stagger the 0.0..1.0 timeline so that:
  - Text occupies $0.35 \dots 1.00$
  - Logo Canals occupy $0.00 \dots 0.35$

### 📌 TASK 3: Fixed-Size Header Architecture (`MeloThreadsHeader.kt`)
- [ ] **3.1**: Enforce fixed width ($130\text{dp}$) and fixed height ($36.25\text{dp}$).
- [ ] **3.2**: Delete all `headerWidth by animateDpAsState` and `.clip(RoundedCornerShape)` (completely eliminates the black shutter bug).
- [ ] **3.3**: Implement `logoOffsetFraction` inside Canvas: glides the Equalizer Canals by $\Delta x = 1.33755f$ into the exact dead center of the screen ($0.000\text{dp}$ offset error).
- [ ] **3.4**: Wire bidirectional pull tracking: `currentProgress = (1f - pullFraction).coerceIn(0f, 1f)`.
- [ ] **3.5**: Wire release trigger: Pen-cap pop audio (`R.raw.barcelona_refresh_sfx_pencap_01`) + haptic tick + 600ms autonomous refill.

### 📌 TASK 4: Compilation, Deployment & On-Device Testing
- [ ] **4.1**: Compile arm64 debug APK (`gradlew assembleArm64FossDebug`).
- [ ] **4.2**: Install APK via ADB to test device (`adb install ...`).
- [ ] **4.3**: Verify On-Device Test Scenarios:
  - Scenario A: Slow pull down $\to$ text drains first, logo canals drain second, completely vanishes at 100%.
  - Scenario B: Half pull $\to$ push back up $\to$ ink refills directly with finger.
  - Scenario C: Release at 100% $\to$ Pop sound plays, haptic ticks, ink refills from canal 0 to 'o'.
  - Scenario D: Feed scroll down $\to$ Logo glides to exact dead center, zero black shutter, text cleanly wipes away.
  - Scenario E: Feed scroll up $\to$ Logo glides back to left, text writes itself live ($0 \to 1$).

### 📌 TASK 5: Dead Asset Cleanup & Final Git Commit
- [ ] **5.1**: Delete 42 unused decompiled XML assets in `app/src/main/res/animator/` and `app/src/main/res/drawable/`.
- [ ] **5.2**: Clean up unused imports in `HomeScreen.kt`.
- [ ] **5.3**: Commit and push clean milestone to `origin/main`.
