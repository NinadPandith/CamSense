"""Tune lens-glint thresholds on a webcam or recorded clips (Implementation Plan, Day 2).

Examples:
  python glint_prototype.py                       # laptop webcam, live view
  python glint_prototype.py clips/hidden_1m.mp4    # one clip, live view
  python glint_prototype.py --report clips/*.mp4   # no window; frames with confirmed glints per clip
  python glint_prototype.py --contrast 90 --confirm 8 --report clips/empty_*.mp4

Gate for Day 2: the lens is found in 9 of 10 hidden-phone clips at 1-2 m, and the
empty-room clips report 0 confirmed glints. Copy the values that pass into
app/src/main/java/com/camsense/scoring/ScanConfig.kt.
"""
from __future__ import annotations

import argparse
import sys

import cv2

from glint_core import BlobTracker, GlintConfig, bright_blobs


def upright_720p(frame):
    h, w = frame.shape[:2]
    scale = 1280 / max(w, h)
    if scale < 1:
        frame = cv2.resize(frame, (int(w * scale), int(h * scale)), interpolation=cv2.INTER_AREA)
    return frame


def run(source, cfg: GlintConfig, show: bool) -> tuple[int, int, int]:
    cap = cv2.VideoCapture(0 if source is None else source)
    if not cap.isOpened():
        sys.exit(f"Can't open {source or 'webcam'}")
    tracker = BlobTracker(cfg)
    frames = frames_with_glint = confirmed_total = 0
    while True:
        ok, frame = cap.read()
        if not ok:
            break
        frame = upright_720p(frame)
        gray = cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY)
        blobs = bright_blobs(gray, cfg)
        confirmed_total += len(tracker.update(blobs))
        visible = tracker.visible_confirmed()
        frames += 1
        frames_with_glint += bool(visible)
        if show:
            h, w = gray.shape
            for b in blobs:
                cv2.circle(frame, (int(b.x * w), int(b.y * h)), 4, (128, 128, 128), 1)
            for t in visible:
                cv2.circle(frame, (int(t.x * w), int(t.y * h)), 18, (68, 68, 239), 2)  # app's red #EF4444
            cv2.putText(frame, f"blobs {len(blobs)}  confirmed {len(visible)}", (12, 28),
                        cv2.FONT_HERSHEY_SIMPLEX, 0.7, (53, 230, 163), 2)
            cv2.imshow("CamSense glint prototype (q to quit)", frame)
            if cv2.waitKey(1) & 0xFF == ord("q"):
                break
    cap.release()
    if show:
        cv2.destroyAllWindows()
    return frames, frames_with_glint, confirmed_total


def main():
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    p.add_argument("sources", nargs="*", help="video files; none = webcam")
    p.add_argument("--report", action="store_true", help="no window, print a line per clip")
    p.add_argument("--min-luma", type=float, default=GlintConfig.min_luma)
    p.add_argument("--contrast", type=float, default=GlintConfig.min_contrast)
    p.add_argument("--min-d", type=float, default=GlintConfig.min_diameter_px)
    p.add_argument("--max-d", type=float, default=GlintConfig.max_diameter_px)
    p.add_argument("--circularity", type=float, default=GlintConfig.min_circularity)
    p.add_argument("--confirm", type=int, default=GlintConfig.confirm_frames)
    a = p.parse_args()
    cfg = GlintConfig(min_luma=a.min_luma, min_contrast=a.contrast, min_diameter_px=a.min_d, max_diameter_px=a.max_d,
                      min_circularity=a.circularity, confirm_frames=a.confirm)
    for src in a.sources or [None]:
        frames, with_glint, confirmed = run(src, cfg, show=not a.report)
        print(f"{src or 'webcam'}: {frames} frames, {with_glint} with a confirmed glint, {confirmed} glints confirmed")


if __name__ == "__main__":
    main()
