# Melo Wordmark Vector & Animation Design Specification

## Overview
This document specifies the finalized vector geometry, normalized coordinates, and animation parameters for the **Melo App Bar Wordmark** (Wave Equalizer Logo + "Melo" Wordmark).

- **Implementation File:** [`MeloWordmark.kt`](../app/src/main/kotlin/com/niksoriginals/melo/ui/component/MeloWordmark.kt)
- **Header Controller:** [`MeloThreadsHeader.kt`](../app/src/main/kotlin/com/niksoriginals/melo/ui/component/MeloThreadsHeader.kt)
- **Viewport Canvas:** `Width: 3.6111f`, `Height: 1.0f` (Aspect ratio: `130dp / 36dp`).
- **Watermark Mould Alpha:** `0.24f`

---

## 1. Vector Geometry & Coordinate System

### A. Wave-Sticks Equalizer Logo (Strokes 0 to 6)
- **Spacing (Pitch):** Mathematically exact **$9.60\text{ px}$ ($0.0960f$)** between each stick.
- **Center X:** Exact midpoint at $X = 0.7350f$.
- **Stick Thickness:** $0.0850f$ ($8.5\text{ px}$).
- **Cap:** `StrokeCap.Round`.

| Index | Element | Path Data (Normalized) | Width | Cap | Start $\to$ End |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **0** | Bar 1 (Left Tall) | `M0.4950,0.2700 L0.4950,0.7300` | `0.0850f` | `Round` | `0.000f` $\to$ `0.120f` |
| **1** | Bar 2 (Mid-Left) | `M0.5910,0.3700 L0.5910,0.5900` | `0.0850f` | `Round` | `0.030f` $\to$ `0.150f` |
| **2** | Bar 3 (Center-L Short) | `M0.6870,0.4400 L0.6870,0.5300` | `0.0850f` | `Round` | `0.060f` $\to$ `0.180f` |
| **3** | Bar 4 (Center-R Short) | `M0.7830,0.4400 L0.7830,0.5300` | `0.0850f` | `Round` | `0.090f` $\to$ `0.210f` |
| **4** | Bar 5 (Mid-Right) | `M0.8790,0.3700 L0.8790,0.5900` | `0.0850f` | `Round` | `0.120f` $\to$ `0.240f` |
| **5** | Bar 6 (Right Tall) | `M0.9750,0.2500 L0.9750,0.7300` | `0.0850f` | `Round` | `0.150f` $\to$ `0.270f` |
| **6** | Base Dot (Center) | `M0.7350,0.6700 L0.7350,0.6900` | `0.0850f` | `Round` | `0.180f` $\to$ `0.300f` |

---

### B. Typography: "Melo" Wordmark (Strokes 7 to 10)

#### 1. Letter 'M' (Index 7)
- **Polyline:** Clean continuous polyline with smooth rounded joints (`StrokeJoin.Round`).
- **Figma Coordinates:** `M120 74.5 L125 47.5 L130.5 20 L147 65.5 L172 20.5 L176.2 46.6 L180.4 72.4`
- **Path Data (Normalized):**
  ```
  M1.2000,0.7450 L1.2500,0.4750 L1.3050,0.2000 L1.4700,0.6550 L1.7200,0.2050 L1.7620,0.4660 L1.8040,0.7240
  ```
- **Stroke Width:** `0.1500f` ($15\text{ px}$)
- **Cap / Join:** `StrokeCap.Round` / `StrokeJoin.Round`
- **Timing:** `0.300f` $\to$ `0.540f`

#### 2. Letter 'e' (Index 8)
- **Geometry:** Modern sans-serif tilted loop with authentic counter axis (continuous single-contour curve).
- **Figma Coordinates:**
  `M195 60.72 C195 49.16 204.081 41 212.405 41 C219.973 41 223.757 47.8 220.73 54.6 C217.703 60.72 205.595 60.72 195 60.72 C195 69.56 201.811 75 210.892 75 C216.189 75 219.973 72.28 223 68.2`
- **Path Data (Normalized):**
  ```
  M1.9500,0.6072 C1.9500,0.4916 2.0408,0.4100 2.1241,0.4100 C2.1997,0.4100 2.2376,0.4780 2.2073,0.5460 C2.1770,0.6072 2.0560,0.6072 1.9500,0.6072 C1.9500,0.6956 2.0181,0.7500 2.1089,0.7500 C2.1619,0.7500 2.1997,0.7228 2.2300,0.6820
  ```
- **Stroke Width:** `0.1100f` ($11\text{ px}$)
- **Cap / Join:** `StrokeCap.Round` / `StrokeJoin.Round`
- **Timing:** `0.500f` $\to$ `0.700f`

#### 3. Letter 'l' (Index 9)
- **Stem:** Upright slanted vertical bar.
- **Figma Coordinates:** `M237 79 L252 21`
- **Path Data (Normalized):** `M2.3700,0.7900 L2.5200,0.2100`
- **Stroke Width:** `0.1581f` ($15.81\text{ px}$)
- **Cap:** `StrokeCap.Butt`
- **Timing:** `0.660f` $\to$ `0.820f`

#### 4. Letter 'o' (Index 10)
- **Closed Loop:** Single continuous 360° ring (eliminating overlapping seam points).
- **Figma Coordinates:** `M278 43 C286.83 43 294 50.17 294 59 C294 67.83 286.83 75 278 75 C269.17 75 262 67.83 262 59 C262 50.17 269.17 43 278 43 Z`
- **Path Data (Normalized):**
  ```
  M2.7800,0.4300 C2.8683,0.4300 2.9400,0.5017 2.9400,0.5900 C2.9400,0.6783 2.8683,0.7500 2.7800,0.7500 C2.6917,0.7500 2.6200,0.6783 2.6200,0.5900 C2.6200,0.5017 2.6917,0.4300 2.7800,0.4300 Z
  ```
- **Stroke Width:** `0.1500f` ($15\text{ px}$)
- **Cap / Join:** `StrokeCap.Round` / `StrokeJoin.Round`
- **Timing:** `0.780f` $\to$ `1.000f`

---

## 2. Master Figma Production SVG Reference
```xml
<svg width="360" height="100" viewBox="0 0 360 100" fill="none" xmlns="http://www.w3.org/2000/svg">
<g clip-path="url(#clip0_8_80)">
<path d="M360 0H0V100H360V0Z" fill="black"/>
<path d="M49.5 27V73" stroke-width="8.5" stroke-linecap="round" stroke="white"/>
<path d="M59.1 37V59" stroke-width="8.5" stroke-linecap="round" stroke="white"/>
<path d="M68.7 44V53" stroke-width="8.5" stroke-linecap="round" stroke="white"/>
<path d="M78.3 44V53" stroke-width="8.5" stroke-linecap="round" stroke="white"/>
<path d="M87.9 37V59" stroke-width="8.5" stroke-linecap="round" stroke="white"/>
<path d="M97.5 25V73" stroke-width="8.5" stroke-linecap="round" stroke="white"/>
<path d="M73.5 67V69" stroke-width="8.5" stroke-linecap="round" stroke="white"/>
<path d="M120 74.5L125 47.5L130.5 20L147 65.5L172 20.5L176.2 46.6L180.4 72.4" stroke-width="15" stroke-linecap="round" stroke-linejoin="round" stroke="white"/>
<path d="M195 60.72C195 49.16 204.081 41 212.405 41C219.973 41 223.757 47.8 220.73 54.6C217.703 60.72 205.595 60.72 195 60.72ZM195 60.72C195 69.56 201.811 75 210.892 75C216.189 75 219.973 72.28 223 68.2" stroke-width="11" stroke="white"/>
<path d="M237 79L252 21" stroke-width="15.81" stroke="white"/>
<path d="M278 43C286.83 43 294 50.17 294 59C294 67.83 286.83 75 278 75C269.17 75 262 67.83 262 59C262 50.17 269.17 43 278 43Z" stroke-width="15" stroke-linejoin="round" stroke="white"/>
</g>
<defs>
<clipPath id="clip0_8_80">
<rect width="360" height="100" fill="white"/>
</clipPath>
</defs>
</svg>
```

---

## 3. Animation & Mechanics

### Pull-to-Refresh (Dynamic Network-Synced Progressive Refill):
- **Pulling Down (1.0 $\to$ 0.0):** Real-time finger tracking. Ink empties from right to left:
  `'o'` $\to$ `'l'` $\to$ `'e'` $\to$ `'M'` $\to$ `Logo Bars`.
- **100% Threshold:** All ink vanishes into the empty `0.24` alpha mould groove with haptic click.
- **Release (Network Loading Phase):**
  - Pen-cap pop SFX plays.
  - While waiting for the network response (`isRefreshing == true`), ink refills progressively and steadily across the equalizer bars and through `'M'`, `'e'`, and `'l'` (0.0 $\to$ 0.82 over 2.4s, `LinearOutSlowInEasing`), acting as a living liquid loader.
- **Network Complete (100% Snap Fill & Phantom Drain Lock):**
  - As soon as network refresh completes (`isRefreshing` becomes `false`), the remaining ink decisively and smoothly completes to 100% (`'o'`) over 280ms (`FastOutSlowInEasing`).
  - While the pull indicator springs back to top, wordmark ink is locked at 100% (`isAwaitingPullReset`), completely eliminating phantom re-drains or stutter glitches.

### Feed Scroll Collapse (Synchronized Unified Shift & Drain):
- **Immediate Groove Hide:** As soon as scrolling begins ($s > 0$), the watermark groove disappears completely (`textMouldAlphaMultiplier = 0f`), keeping the animation clean and free of ghost outlines.
- **Synchronized Shift + Drain ($s: 0.00 \to 1.00$, 750ms `CubicBezier(0.35, 0.0, 0.25, 1.0)`):**
  - **Single Unit Movement:** Logo and "Melo" text shift horizontally to the right together (`logoOffsetFraction = s`).
  - **Simultaneous Right-to-Left Drain:** As the wordmark glides towards the center, text ink drains continuously from the right (`textInk = 1 - s`): `'o'` drains first $\to$ then `'l'` $\to$ then `'e'` $\to$ then `'M'`.
  - **Perfect Alignment:** At the exact moment the logo arrives dead-center ($s = 1.00$), the final stroke of `'M'` completes its drain and vanishes, leaving only the solid equalizer logo floating in the center.
- **Return to Top (Synchronized Reversal):**
  - $s: 1.00 \to 0.00$: Logo and text move back to the left together while text refills live from left to right (`'M'` $\to$ `'e'` $\to$ `'l'` $\to$ `'o'`).
  - At the exact moment the logo reaches its home left position ($s = 0.00$), `'o'` completes drawing, the entire wordmark is 100% full, and the resting groove is restored.
