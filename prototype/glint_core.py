"""Lens-glint detection, the same algorithm as the Android app.

Mirrors FrameOps.brightBlobs and BlobTracker in app/src/main/java/com/camsense,
with the same default numbers as scoring/ScanConfig.kt. Tune here on recorded
clips, then copy the values that work into ScanConfig.kt.
"""
from __future__ import annotations

import math
from dataclasses import dataclass, field

import cv2
import numpy as np


@dataclass
class GlintConfig:
    min_luma: float = 200.0           # GLINT_MIN_LUMA
    min_contrast: float = 70.0        # GLINT_MIN_CONTRAST
    window_px: int = 31               # GLINT_WINDOW_PX
    min_diameter_px: float = 2.0      # GLINT_MIN_DIAMETER_PX
    max_diameter_px: float = 15.0     # GLINT_MAX_DIAMETER_PX
    min_circularity: float = 0.7      # GLINT_MIN_CIRCULARITY
    circularity_min_area: float = 12  # GLINT_CIRCULARITY_MIN_AREA
    min_aspect: float = 0.5           # GLINT_MIN_ASPECT
    max_spots: int = 25               # GLINT_MAX_SPOTS
    confirm_frames: int = 5           # GLINT_CONFIRM_FRAMES
    max_missed_frames: int = 3        # GLINT_MAX_MISSED_FRAMES
    match_distance: float = 0.04      # GLINT_MATCH_DISTANCE (fraction of frame)
    max_size_ratio: float = 2.5       # GLINT_MAX_SIZE_RATIO


@dataclass
class Blob:
    x: float  # 0-1 of frame width
    y: float  # 0-1 of frame height
    diameter: float
    strength: float = 0.0  # how far above its surroundings (0-255)


def pixel_circularity(contour_area: float, perimeter: float) -> float:
    """4*pi*A/P^2, corrected for small blobs.

    findContours runs through pixel centres, so a 7 px disc measures 0.67 and
    would fail a 0.7 test. Adding back the half-pixel border (A + P/2 + 1, and
    P + pi) gives 0.72-0.83 for discs of any size, and still about 0.4 for streaks.
    """
    if perimeter <= 0:
        return 1.0
    return 4 * math.pi * (contour_area + perimeter / 2 + 1) / (perimeter + math.pi) ** 2


def bright_blobs(gray: np.ndarray, cfg: GlintConfig) -> list[Blob]:
    """Small round spots much brighter than their surroundings (FrameOps.brightSpots).

    Local contrast = pixel minus the mean of a window_px box around it. With the torch
    on, auto exposure darkens the frame, so a lens reflection may only reach ~220 but
    still stands far above its background; large bright areas have almost no local
    contrast. The strongest max_spots candidates are kept, so a busy frame never
    hides the lens.
    """
    background = cv2.blur(gray, (cfg.window_px, cfg.window_px))
    contrast = cv2.subtract(gray, background)  # saturates at 0
    _, contrast_mask = cv2.threshold(contrast, cfg.min_contrast, 255, cv2.THRESH_BINARY)
    _, luma_mask = cv2.threshold(gray, cfg.min_luma, 255, cv2.THRESH_BINARY)
    mask = cv2.bitwise_and(contrast_mask, luma_mask)
    contours, _ = cv2.findContours(mask, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_NONE)
    h, w = gray.shape[:2]
    blobs = []
    for c in contours:
        x, y, bw, bh = cv2.boundingRect(c)
        diameter = float(max(bw, bh))
        if diameter < cfg.min_diameter_px or diameter > cfg.max_diameter_px:
            continue
        if min(bw, bh) / max(bw, bh) < cfg.min_aspect:
            continue  # streaks and smears, not a point
        area = max(cv2.contourArea(c), 1.0)
        if area >= cfg.circularity_min_area:
            if pixel_circularity(area, cv2.arcLength(c, True)) < cfg.min_circularity:
                continue
        strength = float(contrast[y:y + bh, x:x + bw].max())
        blobs.append(Blob((x + bw / 2) / w, (y + bh / 2) / h, diameter, strength))
    return sorted(blobs, key=lambda b: -b.strength)[:cfg.max_spots]


@dataclass
class Track:
    id: int
    x: float
    y: float
    diameter: float
    min_d: float
    max_d: float
    hits: int = 1
    missed: int = 0
    confirmed: bool = False
    rejected: bool = False


@dataclass
class BlobTracker:
    """Same rules as BlobTracker.kt: confirm after N steady sightings, drop after M misses."""
    cfg: GlintConfig
    tracks: list[Track] = field(default_factory=list)
    next_id: int = 1

    def update(self, blobs: list[Blob]) -> list[Track]:
        cfg = self.cfg
        pairs = sorted(
            (math.hypot(t.x - b.x, t.y - b.y), ti, bi)
            for ti, t in enumerate(self.tracks) for bi, b in enumerate(blobs)
            if math.hypot(t.x - b.x, t.y - b.y) <= cfg.match_distance
        )
        used_t, used_b, updated, newly = set(), set(), [], []
        for _, ti, bi in pairs:
            if ti in used_t or bi in used_b:
                continue
            used_t.add(ti)
            used_b.add(bi)
            t, b = self.tracks[ti], blobs[bi]
            min_d, max_d = min(t.min_d, b.diameter), max(t.max_d, b.diameter)
            rejected = t.rejected or b.diameter > cfg.max_diameter_px or (min_d > 0 and max_d / min_d > cfg.max_size_ratio)
            hits = t.hits + 1
            confirmed = t.confirmed or (not rejected and hits >= cfg.confirm_frames)
            nt = Track(t.id, b.x, b.y, b.diameter, min_d, max_d, hits, 0, confirmed, rejected)
            if confirmed and not t.confirmed:
                newly.append(nt)
            updated.append(nt)
        for ti, t in enumerate(self.tracks):
            if ti not in used_t and t.missed + 1 <= cfg.max_missed_frames:
                t.missed += 1
                updated.append(t)
        for bi, b in enumerate(blobs):
            if bi not in used_b:
                too_big = b.diameter > cfg.max_diameter_px
                updated.append(Track(self.next_id, b.x, b.y, b.diameter, b.diameter, b.diameter, rejected=too_big))
                self.next_id += 1
        self.tracks = updated
        return newly

    def visible_confirmed(self) -> list[Track]:
        return [t for t in self.tracks if t.confirmed and t.missed == 0]
