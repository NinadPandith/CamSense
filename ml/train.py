"""Fine-tune YOLO26-nano on the 8 hiding-object classes (Implementation Plan, Day 6).

  python train.py                 # 100 epochs at 640 px from yolo26n.pt
  python train.py --epochs 50 --imgsz 320

Augmentation mimics dark rooms and shaky hands (brightness, blur, rotation, noise).
Log mAP@50 from the printed metrics in the README results table.
"""
import argparse

from ultralytics import YOLO


def main():
    p = argparse.ArgumentParser()
    p.add_argument("--model", default="yolo26n.pt", help="fall back to yolo11n.pt if YOLO26 export gives trouble")
    p.add_argument("--data", default="data.yaml")
    p.add_argument("--epochs", type=int, default=100)
    p.add_argument("--imgsz", type=int, default=640)
    p.add_argument("--batch", type=int, default=16)
    a = p.parse_args()

    model = YOLO(a.model)
    model.train(
        data=a.data,
        epochs=a.epochs,
        imgsz=a.imgsz,
        batch=a.batch,
        # Dark rooms and shaky hands (PRD dataset plan).
        hsv_v=0.6,        # brightness
        degrees=10.0,     # rotation
        translate=0.1,
        scale=0.5,
        fliplr=0.5,
        mosaic=1.0,
        patience=30,
    )
    metrics = model.val(data=a.data, split="test")
    print(f"mAP@50 on the test split: {metrics.box.map50:.3f} (target 0.70; 0.60 is fine for the first demo)")


if __name__ == "__main__":
    main()
