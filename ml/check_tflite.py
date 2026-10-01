"""Quick check of an exported model before it goes in the app (Tech Stack "Quick check").

  python check_tflite.py ../app/src/main/assets/models/hiding_objects_yolo26n.tflite photo.jpg
"""
import sys

from ultralytics import YOLO

model = YOLO(sys.argv[1], task="detect")
for r in model.predict(sys.argv[2:], imgsz=640, conf=0.4):
    for box in r.boxes:
        print(f"{r.path}: {r.names[int(box.cls)]} {float(box.conf):.2f} {box.xyxyn.tolist()[0]}")
