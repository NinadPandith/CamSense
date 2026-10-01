"""Catches strings.xml mistakes that only fail in aapt2 (mergeDebugResources):
unescaped apostrophes or quotes, values starting with @ or ?, bad format specifiers,
duplicate names, and XML that doesn't parse."""
import re
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

path = Path(__file__).resolve().parents[2] / "app/src/main/res/values/strings.xml"
src = path.read_text(encoding="utf-8")
errors = []
try:
    ET.fromstring(src)
except ET.ParseError as e:
    errors.append(f"XML: {e}")
names = re.findall(r'<string name="([^"]+)"', src)
errors += [f"{n}: duplicate name" for n in sorted({n for n in names if names.count(n) > 1})]
for name, value in re.findall(r'<string name="([^"]+)">(.*?)</string>', src, re.S):
    if re.search(r"(?<!\\)'", value):
        errors.append(f"{name}: unescaped apostrophe (write \\')")
    if re.search(r'(?<!\\)"', value):
        errors.append(f'{name}: unescaped quote (write \\")')
    if value[:1] in "@?":
        errors.append(f"{name}: starts with {value[0]} (escape it)")
    for spec in re.findall(r"%(?!%)(?:\d+\$)?[-#+ 0,(]*\d*(?:\.\d+)?([a-zA-Z]?)", value.replace("%%", "")):
        if not spec:
            errors.append(f"{name}: stray % (write %% for a literal percent)")
    if len(re.findall(r"%(?!%)[a-zA-Z]", value.replace("%%", ""))) > 1:
        errors.append(f"{name}: several unnumbered %s/%d; use %1$s, %2$d")
if errors:
    print("strings.xml problems:\n  " + "\n  ".join(errors))
    sys.exit(1)
print(f"strings.xml OK ({len(names)} strings)")
