import json
import os
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parent.parent
RES = ROOT / "app" / "src" / "main" / "res"
TYPES_JSON = ROOT / "tools" / "input_types.json"

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

LABELS_I18N = {
    "de": {"ANTENA": "Antenne", "CABLE": "Kabel", "COMPOSITE": "Composite", "COMPONENT": "Component",
           "GAME_CONSOLE": "Spielkonsole", "DVD_PLAYER": "DVD-Player", "CABLE_TV": "Kabelfernsehen",
           "SATELLITE": "Satellit", "MEDIA_PLAYER": "Medienplayer", "RECORDING_LIST": "Aufnahmeliste",
           "OPTICAL": "Optisch"},
    "es": {"ANTENA": "Antena", "CABLE": "Cable", "COMPOSITE": "Compuesto", "COMPONENT": "Componentes",
           "GAME_CONSOLE": "Consola de juegos", "DVD_PLAYER": "Reproductor de DVD", "CABLE_TV": "TV por cable",
           "SATELLITE": "Satélite", "MEDIA_PLAYER": "Reproductor multimedia", "RECORDING_LIST": "Lista de grabaciones",
           "OPTICAL": "Óptico"},
    "fr": {"ANTENA": "Antenne", "CABLE": "Câble", "COMPOSITE": "Composite", "COMPONENT": "Composantes",
           "GAME_CONSOLE": "Console de jeu", "DVD_PLAYER": "Lecteur DVD", "CABLE_TV": "Télévision par câble",
           "SATELLITE": "Satellite", "MEDIA_PLAYER": "Lecteur multimédia", "RECORDING_LIST": "Liste des enregistrements",
           "OPTICAL": "Optique"},
    "it": {"ANTENA": "Antenna", "CABLE": "Cavo", "COMPOSITE": "Composito", "COMPONENT": "Component",
           "GAME_CONSOLE": "Console di gioco", "DVD_PLAYER": "Lettore DVD", "CABLE_TV": "TV via cavo",
           "SATELLITE": "Satellite", "MEDIA_PLAYER": "Lettore multimediale", "RECORDING_LIST": "Elenco registrazioni",
           "OPTICAL": "Ottico"},
    "ja": {"ANTENA": "アンテナ", "CABLE": "ケーブル", "COMPOSITE": "コンポジット", "COMPONENT": "コンポーネント",
           "GAME_CONSOLE": "ゲーム機", "DVD_PLAYER": "DVDプレーヤー", "CABLE_TV": "ケーブルテレビ",
           "SATELLITE": "衛星放送", "MEDIA_PLAYER": "メディアプレーヤー", "RECORDING_LIST": "録画リスト",
           "OPTICAL": "光デジタル"},
    "pt": {"ANTENA": "Antena", "CABLE": "Cabo", "COMPOSITE": "Composto", "COMPONENT": "Componentes",
           "GAME_CONSOLE": "Consola de jogos", "DVD_PLAYER": "Leitor de DVD", "CABLE_TV": "TV por cabo",
           "SATELLITE": "Satélite", "MEDIA_PLAYER": "Leitor multimédia", "RECORDING_LIST": "Lista de gravações",
           "OPTICAL": "Ótico"},
    "pt-rBR": {"ANTENA": "Antena", "CABLE": "Cabo", "COMPOSITE": "Composto", "COMPONENT": "Componente",
               "GAME_CONSOLE": "Console de jogos", "DVD_PLAYER": "DVD player", "CABLE_TV": "TV a cabo",
               "SATELLITE": "Satélite", "MEDIA_PLAYER": "Media player", "RECORDING_LIST": "Lista de gravações",
               "OPTICAL": "Óptico"},
    "hi": {"ANTENA": "एंटीना", "CABLE": "केबल", "COMPOSITE": "कंपोज़िट", "COMPONENT": "कंपोनेंट",
           "GAME_CONSOLE": "गेम कंसोल", "DVD_PLAYER": "DVD प्लेयर", "CABLE_TV": "केबल टीवी",
           "SATELLITE": "सैटेलाइट", "MEDIA_PLAYER": "मीडिया प्लेयर", "RECORDING_LIST": "रिकॉर्डिंग सूची",
           "OPTICAL": "ऑप्टिकल"},
}

BG = (14, 18, 24)
BG2 = (23, 30, 40)
LINE = (42, 54, 69)
FG = (238, 242, 246)
MUTED = (140, 153, 170)
ACCENT = (242, 169, 59)

BANNER_SIZE = (640, 360)
BANNER_PANEL_WIDTH = 236
ICON_SIZE = 216


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


def hdmi_plug_outline(d, x, y, w, h, color):
    inset = int(w * 0.14)
    d.polygon([(x, y), (x + w, y), (x + w - inset, y + h), (x + inset, y + h)], outline=color, width=max(2, w // 40))
    pins = 9
    px0 = x + int(w * 0.18)
    px1 = x + w - int(w * 0.18)
    py = y + int(h * 0.38)
    for i in range(pins):
        px = px0 + (px1 - px0) * i // (pins - 1)
        d.line([(px, py), (px, py + int(h * 0.22))], fill=color, width=max(2, w // 60))


def badge_font(short, large, medium, small):
    return font(large if len(short) <= 2 else medium if len(short) <= 4 else small)


def banner(label, short, path):
    W, H = BANNER_SIZE
    im = Image.new("RGB", (W, H), BG)
    d = ImageDraw.Draw(im)
    d.rectangle([0, 0, BANNER_PANEL_WIDTH, H], fill=BG2)
    d.line([(BANNER_PANEL_WIDTH, 0), (BANNER_PANEL_WIDTH, H)], fill=LINE, width=2)
    hdmi_plug_outline(d, 48, 72, 140, 60, LINE)
    bf = badge_font(short, 96, 64, 46)
    bw, bh = d.textbbox((0, 0), short, font=bf)[2:]
    d.text(((BANNER_PANEL_WIDTH - bw) // 2, 190 - bh // 2 + 20), short, font=bf, fill=ACCENT)
    d.text((276, 86), "SWITCH INPUT", font=font(26), fill=MUTED)
    lf = font(72 if len(label) <= 10 else 56 if len(label) <= 14 else 44)
    d.text((272, 136), label, font=lf, fill=FG)
    d.rectangle([276, 300, 276 + 60, 304], fill=ACCENT)
    im.save(path, optimize=True)


def icon(short, path):
    S = ICON_SIZE
    im = Image.new("RGB", (S, S), BG2)
    d = ImageDraw.Draw(im)
    d.rectangle([0, 0, S - 1, S - 1], outline=LINE, width=4)
    bf = badge_font(short, 104, 72, 50)
    bw, bh = d.textbbox((0, 0), short, font=bf)[2:]
    d.text(((S - bw) // 2, (S - bh) // 2 - 12), short, font=bf, fill=ACCENT)
    d.rectangle([S // 2 - 24, S - 40, S // 2 + 24, S - 36], fill=ACCENT)
    im.save(path, optimize=True)


def xml_escape(s):
    return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("'", "\\'")


def write_strings(path, labels_by_type):
    lines = ['<?xml version="1.0" encoding="utf-8"?>', "<resources>"]
    lines += [f'    <string name="input_{t.lower()}">{xml_escape(l)}</string>' for t, l in labels_by_type.items()]
    lines.append("</resources>")
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text("\n".join(lines) + "\n", encoding="utf-8")


def main():
    pngs = RES / "drawable-xhdpi"
    pngs.mkdir(parents=True, exist_ok=True)
    for t, label, short in TYPES:
        banner(label, short, pngs / f"banner_{t.lower()}.png")
        icon(short, pngs / f"ic_{t.lower()}.png")
    write_strings(RES / "values" / "input_types.xml", {t: label for t, label, _ in TYPES})
    for qualifier, labels in LABELS_I18N.items():
        write_strings(RES / f"values-{qualifier}" / "input_types.xml", labels)
    TYPES_JSON.write_text(json.dumps({
        "types": [{"type": t, "label": label, "short": short,
                   "labels": {q: m[t] for q, m in LABELS_I18N.items() if t in m}} for t, label, short in TYPES],
    }, indent=1), encoding="utf-8")
    print(f"generated {len(TYPES)} types")


if __name__ == "__main__":
    main()
