"""Export a trained (or the stock) model to LiteRT and drop it into the app.

  python export.py runs/detect/train/weights/best.pt      # custom 8-class model, INT8
  python export.py yolo26n.pt --stock                     # stock COCO model (Day 4)
  python export.py best.pt --imgsz 320                    # if the phone runs below 15 fps

Writes app/src/main/assets/models/hiding_objects_yolo26n.tflite and labels.txt,
keeping labels.txt in the model's class order (or the app shows wrong names).
"""
import argparse
import shutil
from pathlib import Path

from ultralytics import YOLO

ASSETS = Path(__file__).resolve().parent.parent / "app/src/main/assets/models"


def main():
    p = argparse.ArgumentParser()
    p.add_argument("weights")
    p.add_argument("--imgsz", type=int, default=640)
    p.add_argument("--stock", action="store_true", help="stock COCO model: exported as float (INT8 needs real calibration images)")
    p.add_argument("--float", action="store_true", help="float32 instead of INT8 (bigger, simpler)")
    p.add_argument("--data", default="data.yaml")
    a = p.parse_args()

    model = YOLO(a.weights)
    # Ultralytics 8.4 exports through LiteRT-Torch; input stays [1, 3, H, W] (the app handles both).
    kwargs = dict(format="litert", imgsz=a.imgsz)
    if not a.float and not a.stock:
        # INT8 is ~3.5x smaller and ~2x faster, but needs your dataset for calibration:
        # calibrated on only a few images it flattened every confidence to about 0.70.
        kwargs["quantize"] = 8
        kwargs["data"] = a.data
    out = Path(model.export(**kwargs))
    if out.is_dir():  # some versions return the saved_model folder
        candidates = sorted(out.glob("*int8.tflite" if not a.float else "*float32.tflite")) or sorted(out.glob("*.tflite"))
        out = candidates[0]

    ASSETS.mkdir(parents=True, exist_ok=True)
    shutil.copy(out, ASSETS / "hiding_objects_yolo26n.tflite")
    names = model.names
    (ASSETS / "labels.txt").write_text("\n".join(names[i] for i in sorted(names)) + "\n")
    size_mb = (ASSETS / "hiding_objects_yolo26n.tflite").stat().st_size / 1e6
    print(f"Copied {out.name} ({size_mb:.1f} MB) and {len(names)} labels into {ASSETS}")


if __name__ == "__main__":
    main()
