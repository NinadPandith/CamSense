"""Synthetic checks that the Python prototype behaves like BlobTracker.kt. Run: pytest"""
import numpy as np
import cv2

from glint_core import BlobTracker, GlintConfig, bright_blobs


def frame_with_spot(x, y, radius, size=(720, 1280)):
    img = np.full(size, 40, np.uint8)
    cv2.circle(img, (x, y), radius, 255, -1)
    return img


def test_small_round_spot_is_found():
    blobs = bright_blobs(frame_with_spot(640, 360, 3), GlintConfig())
    assert len(blobs) == 1
    assert abs(blobs[0].x - 0.5) < 0.01


def test_large_reflection_is_ignored():
    assert bright_blobs(frame_with_spot(640, 360, 30), GlintConfig()) == []


def test_elongated_streak_is_ignored():
    img = np.full((720, 1280), 40, np.uint8)
    cv2.line(img, (600, 360), (612, 360), 255, 2)
    assert bright_blobs(img, GlintConfig()) == []


def test_steady_spot_confirms_after_five_frames():
    cfg = GlintConfig()
    tracker = BlobTracker(cfg)
    confirmed = 0
    for i in range(5):
        confirmed += len(tracker.update(bright_blobs(frame_with_spot(640 + i * 3, 360, 3), cfg)))
    assert confirmed == 1


def test_flicker_never_confirms():
    cfg = GlintConfig()
    tracker = BlobTracker(cfg)
    confirmed = 0
    for i in range(12):
        img = frame_with_spot(640, 360, 3) if i % 5 == 0 else np.full((720, 1280), 40, np.uint8)
        confirmed += len(tracker.update(bright_blobs(img, cfg)))
    assert confirmed == 0


def test_discs_of_every_size_pass_circularity():
    cfg = GlintConfig()
    for r in range(1, 8):
        assert len(bright_blobs(frame_with_spot(640, 360, r), cfg)) == 1, f"radius {r}"
