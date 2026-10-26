"""Generate everything that depends on Fire TV's fixed list of input ("activity") types:

  - app/src/main/res/values/input_types.xml   : one label string per type (Amazon's own names)
  - app/src/main/AndroidManifest.xml          : one <activity-alias> per type between the markers
  - app/src/main/res/drawable-xhdpi/banner_<type>.png (640x360) and ic_<type>.png (216x216)

The type list is Definitions.ActivityType from the decompiled com.amazon.tv.devicecontrol
(re/NOTES.md). A launcher shortcut's label and banner must be static manifest resources, so
every possible type gets an alias; the app only enables the ones that exist on this TV.

Run from the repo root:  python -I tools/gen_inputs.py
"""
import os
import re
import sys
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parent.parent
RES = ROOT / "app" / "src" / "main" / "res"
MANIFEST = ROOT / "app" / "src" / "main" / "AndroidManifest.xml"

# (activityType, Amazon display name, short badge text)
TYPES = [
    ("STARK", "Fire TV", "FTV"),
    ("HDMI_1", "HDMI 1", "1"), ("HDMI_2", "HDMI 2", "2"), ("HDMI_3", "HDMI 3", "3"), ("HDMI_4", "HDMI 4", "4"),
    ("COAX", "Coax", "COAX"), ("ANTENA", "Antenna", "ANT"), ("CABLE", "Cable", "CAB"),
    ("COMPOSITE", "Composite", "AV"), ("COMPONENT", "Component", "YPbPr"),
    ("GAME_CONSOLE", "Game Console", "GAME"), ("DVD_PLAYER", "DVD Player", "DVD"), ("BLU_RAY", "Blu-ray", "BD"),
    ("CABLE_TV", "Cable TV", "CATV"), ("SATELLITE", "Satellite", "SAT"), ("DVR", "DVR", "DVR"),
    ("MEDIA_PLAYER", "Media Player", "MP"), ("APPLE_TV", "Apple TV", "ATV"),
    ("PLAY_STATION", "PlayStation", "PS"), ("PLAY_STATION_2", "PlayStation 2", "PS2"),
    ("PLAY_STATION_3", "PlayStation 3", "PS3"), ("PLAY_STATION_4", "PlayStation 4", "PS4"),
    ("ROKU", "Roku", "ROKU"), ("XBOX", "Xbox", "XBOX"), ("XBOX_ONE", "Xbox One", "XB1"), ("XBOX_360", "Xbox 360", "X360"),
    ("WII", "Nintendo Wii", "WII"), ("WII_U", "Nintendo Wii U", "WIIU"), ("GAME_CUBE", "GameCube", "GC"),
    ("TIVO", "TiVo", "TIVO"), ("BS", "BS", "BS"), ("CS", "CS", "CS"), ("BS4K", "BS4K", "BS4K"),
    ("BLUETOOTH", "Bluetooth", "BT"), ("OPTICAL", "Optical", "OPT"), ("CS4K", "CS4K", "CS4K"), ("VGA", "VGA", "VGA"),
    ("RECORDING_LIST", "Recording List", "REC"), ("ARC", "HDMI ARC", "ARC"),
    ("NINTENDO_SWITCH", "Nintendo Switch", "NSW"), ("CHROMECAST", "Chromecast", "CC"), ("FOUR_K", "4K", "4K"),
    ("HDMI", "HDMI", "HDMI"), ("DVB_S", "DVB-S", "DVB-S"), ("DVB_C", "DVB-C", "DVB-C"), ("DVB_T", "DVB-T", "DVB-T"),
    ("SAT_IP", "SAT>IP", "SAT"), ("DLNA_SOURCE", "DLNA", "DLNA"),
]

# Palette shared with res/values/colors.xml (dark, slightly blue-biased neutrals, amber lamp accent).
BG = (14, 18, 24)
BG2 = (23, 30, 40)
LINE = (42, 54, 69)
FG = (238, 242, 246)
MUTED = (140, 153, 170)
ACCENT = (242, 169, 59)


def font(size, bold=True):
    candidates = ["bahnschrift.ttf", "arialbd.ttf" if bold else "arial.ttf", "segoeuib.ttf" if bold else "segoeui.ttf"]
    for name in candidates:
        p = Path(os.environ.get("WINDIR", r"C:\Windows")) / "Fonts" / name
        if p.exists():
            f = ImageFont.truetype(str(p), size)
            if name == "bahnschrift.ttf":
                try:
                    f.set_variation_by_name("SemiBold Condensed" if bold else "Condensed")
                except Exception:
                    pass
            return f
    return ImageFont.load_default()


def port_glyph(d, x, y, w, h, color):
    """A simplified HDMI plug outline: trapezoid with a notch row of pins."""
    inset = int(w * 0.14)
    d.polygon([(x, y), (x + w, y), (x + w - inset, y + h), (x + inset, y + h)], outline=color, width=max(2, w // 40))
    pins = 9
    px0 = x + int(w * 0.18)
    px1 = x + w - int(w * 0.18)
    py = y + int(h * 0.38)
    for i in range(pins):
        px = px0 + (px1 - px0) * i // (pins - 1)
        d.line([(px, py), (px, py + int(h * 0.22))], fill=color, width=max(2, w // 60))


def banner(t, label, short, path):
    W, H = 640, 360
    im = Image.new("RGB", (W, H), BG)
    d = ImageDraw.Draw(im)
    # left dark panel with the badge, right side the name
    d.rectangle([0, 0, 236, H], fill=BG2)
    d.line([(236, 0), (236, H)], fill=LINE, width=2)
    port_glyph(d, 48, 72, 140, 60, LINE)
    bf = font(96 if len(short) <= 2 else 64 if len(short) <= 4 else 46)
    bw, bh = d.textbbox((0, 0), short, font=bf)[2:]
    d.text(((236 - bw) // 2, 190 - bh // 2 + 20), short, font=bf, fill=ACCENT)
    d.text((276, 86), "SWITCH INPUT", font=font(26), fill=MUTED)
    lf = font(72 if len(label) <= 10 else 56 if len(label) <= 14 else 44)
    d.text((272, 136), label, font=lf, fill=FG)
    d.rectangle([276, 300, 276 + 60, 304], fill=ACCENT)
    im.save(path, optimize=True)


def icon(t, label, short, path):
    S = 216
    im = Image.new("RGB", (S, S), BG2)
    d = ImageDraw.Draw(im)
    d.rectangle([0, 0, S - 1, S - 1], outline=LINE, width=4)
    bf = font(104 if len(short) <= 2 else 72 if len(short) <= 4 else 50)
    bw, bh = d.textbbox((0, 0), short, font=bf)[2:]
    d.text(((S - bw) // 2, (S - bh) // 2 - 12), short, font=bf, fill=ACCENT)
    d.rectangle([S // 2 - 24, S - 40, S // 2 + 24, S - 36], fill=ACCENT)
    im.save(path, optimize=True)


def xml_escape(s):
    return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("'", "\\'")


def main():
    out = RES / "drawable-xhdpi"
    out.mkdir(parents=True, exist_ok=True)
    strings = ['<?xml version="1.0" encoding="utf-8"?>', "<!-- Generated by tools/gen_inputs.py; do not edit. -->", "<resources>"]
    aliases = []
    for t, label, short in TYPES:
        key = t.lower()
        banner(t, label, short, out / f"banner_{key}.png")
        icon(t, label, short, out / f"ic_{key}.png")
        strings.append(f'    <string name="input_{key}">{xml_escape(label)}</string>')
        aliases.append(
            f'        <activity-alias android:name=".Shortcut_{t}" android:targetActivity=".SwitchActivity"\n'
            f'            android:label="@string/input_{key}" android:icon="@drawable/ic_{key}"\n'
            f'            android:banner="@drawable/banner_{key}" android:enabled="false" android:exported="true">\n'
            f'            <intent-filter>\n'
            f'                <action android:name="android.intent.action.MAIN" />\n'
            f'                <category android:name="android.intent.category.LEANBACK_LAUNCHER" />\n'
            f'                <category android:name="android.intent.category.LAUNCHER" />\n'
            f'            </intent-filter>\n'
            f'        </activity-alias>'
        )
    strings.append("</resources>")
    (RES / "values" / "input_types.xml").write_text("\n".join(strings) + "\n", encoding="utf-8")

    m = MANIFEST.read_text(encoding="utf-8")
    block = "        <!-- BEGIN GENERATED ALIASES (tools/gen_inputs.py) -->\n" + "\n".join(aliases) + "\n        <!-- END GENERATED ALIASES -->"
    new, n = re.subn(r"        <!-- BEGIN GENERATED ALIASES.*?<!-- END GENERATED ALIASES -->", lambda _: block, m, flags=re.S)
    if n != 1:
        sys.exit("manifest markers not found")
    MANIFEST.write_text(new, encoding="utf-8")
    print(f"generated {len(TYPES)} types")


if __name__ == "__main__":
    main()
