"""Check what a camera sees of an IR LED (TV remote) in the dark (PRD FR-2.1-2.3).

  python ir_prototype.py            # webcam: point a TV remote at it and press a button
  python ir_prototype.py clip.mp4

Shows the frame brightness (the app only looks for glows when it is at or below
IR_DARK_MAX_LUMA) and circles small bright spots like the app's IR check does.
"""
from __future__ import annotations

import sys

import cv2

from glint_core import BlobTracker, GlintConfig, bright_blobs

IR_DARK_MAX_LUMA = 60.0   # ScanConfig.IR_DARK_MAX_LUMA
IR_CFG = GlintConfig(min_luma=160.0, min_contrast=80.0, window_px=51, min_diameter_px=2, max_diameter_px=25,
                     min_circularity=0.5, min_aspect=0.4, max_spots=15, confirm_frames=4)


def main():
    src = sys.argv[1] if len(sys.argv) > 1 else 0
    cap = cv2.VideoCapture(src)
    tracker = BlobTracker(IR_CFG)
    rising, visible_before = 0, False
    while True:
        ok, frame = cap.read()
        if not ok:
            break
        gray = cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY)
        luma = float(gray.mean())
        dark = luma <= IR_DARK_MAX_LUMA
        blobs = bright_blobs(gray, IR_CFG) if dark else []
        tracker.update(blobs)
        visible = bool(blobs)
        rising += visible and not visible_before   # calibration counts flicker on/off
        visible_before = visible
        h, w = gray.shape
        for t in tracker.visible_confirmed():
            cv2.circle(frame, (int(t.x * w), int(t.y * h)), 18, (11, 158, 245), 2)  # app's amber #F59E0B
        cv2.putText(frame, f"luma {luma:.0f} {'dark' if dark else 'too bright'}  flickers {rising}",
                    (12, 28), cv2.FONT_HERSHEY_SIMPLEX, 0.7, (53, 230, 163), 2)
        cv2.imshow("CamSense IR prototype (q to quit)", frame)
        if cv2.waitKey(1) & 0xFF == ord("q"):
            break
    print(f"Flickers seen: {rising} (calibration passes at 3)")


if __name__ == "__main__":
    main()
