"""Build one tiny APK per Fire TV input type and drop them in app/src/main/assets/stubs/.

Why: Projectivy (and the Fire TV launcher) show ONE card per package, so activity-aliases in
the main app never appear as separate TV apps. A separate package per input does. Each stub
is the same classes.dex (tools/stub/Trampoline.java) with its own package name, label,
banner/icon and a meta-data entry naming the input; the picker installs/uninstalls them as root.

Needs the SDK from tools/env.ps1 (build-tools 34: aapt2, d8, zipalign, apksigner) and the
Android debug keystore. Run from the repo root:  python -I tools/build_stubs.py
"""
import json
import os
import shutil
import subprocess
import sys
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SDK = Path(os.environ.get("ANDROID_HOME") or Path.home() / "scoop/apps/android-clt/current")
BT = SDK / "build-tools" / "34.0.0"
ANDROID_JAR = SDK / "platforms" / "android-34" / "android.jar"
JAVA_HOME = Path(os.environ.get("JAVA_HOME") or Path.home() / "scoop/apps/temurin17-jdk/current")
KEYSTORE = Path.home() / ".android" / "debug.keystore"
TYPES_JSON = ROOT / "tools" / "input_types.json"
RES_PNG = ROOT / "app" / "src" / "main" / "res" / "drawable-xhdpi"
OUT = ROOT / "app" / "src" / "main" / "assets" / "stubs"
WORK = ROOT / "build" / "stubs"
STUB_PKG_PREFIX = "pt.aadf.sourcesswitcher.input."

MANIFEST = """<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="{pkg}" android:versionCode="{vc}" android:versionName="{vn}">
    <uses-sdk android:minSdkVersion="25" android:targetSdkVersion="34" />
    <uses-feature android:name="android.software.leanback" android:required="false" />
    <uses-feature android:name="android.hardware.touchscreen" android:required="false" />
    <application android:label="@string/label" android:icon="@drawable/ic"
        android:banner="@drawable/banner" android:allowBackup="false">
        <activity android:name="pt.aadf.sourcesswitcher.stub.Trampoline" android:exported="true"
            android:theme="@android:style/Theme.NoDisplay" android:noHistory="true"
            android:excludeFromRecents="true">
            <meta-data android:name="pt.aadf.input" android:value="{type}" />
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LEANBACK_LAUNCHER" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
"""


def run(*cmd, **kw):
    r = subprocess.run([str(c) for c in cmd], capture_output=True, text=True, **kw)
    if r.returncode != 0:
        sys.exit(f"command failed: {' '.join(str(c) for c in cmd)}\n{r.stdout}\n{r.stderr}")
    return r


def build_dex():
    cls = WORK / "classes"
    shutil.rmtree(cls, ignore_errors=True)
    cls.mkdir(parents=True)
    run(JAVA_HOME / "bin" / "javac", "-source", "8", "-target", "8", "-bootclasspath", ANDROID_JAR,
        "-d", cls, ROOT / "tools" / "stub" / "Trampoline.java")
    dexdir = WORK / "dex"
    shutil.rmtree(dexdir, ignore_errors=True)
    dexdir.mkdir()
    run(BT / "d8.bat", "--release", "--min-api", "25", "--lib", ANDROID_JAR, "--output", dexdir,
        *cls.rglob("*.class"))
    return dexdir / "classes.dex"


def build_stub(t, label, dex, vc, vn, labels=None):
    key = t.lower()
    d = WORK / key
    shutil.rmtree(d, ignore_errors=True)
    (d / "res" / "values").mkdir(parents=True)
    (d / "res" / "drawable-xhdpi").mkdir(parents=True)
    esc = label.replace("&", "&amp;").replace("<", "&lt;").replace("'", "\\'")
    (d / "res" / "values" / "strings.xml").write_text(
        f'<resources><string name="label">{esc}</string></resources>', encoding="utf-8")
    for qual, l in (labels or {}).items():
        # Localised shortcut name: launchers show it in the system language.
        ld = d / "res" / f"values-{qual}"
        ld.mkdir()
        e = l.replace("&", "&amp;").replace("<", "&lt;").replace("'", "\'")
        (ld / "strings.xml").write_text(f'<resources><string name="label">{e}</string></resources>', encoding="utf-8")
    shutil.copy(RES_PNG / f"banner_{key}.png", d / "res" / "drawable-xhdpi" / "banner.png")
    shutil.copy(RES_PNG / f"ic_{key}.png", d / "res" / "drawable-xhdpi" / "ic.png")
    (d / "AndroidManifest.xml").write_text(MANIFEST.format(pkg=STUB_PKG_PREFIX + key, type=t, vc=vc, vn=vn), encoding="utf-8")
    comp = d / "compiled"
    comp.mkdir()
    run(BT / "aapt2.exe", "compile", "--dir", d / "res", "-o", comp / "res.zip")
    unsigned = d / "unsigned.apk"
    run(BT / "aapt2.exe", "link", "-o", unsigned, "-I", ANDROID_JAR, "--manifest", d / "AndroidManifest.xml",
        comp / "res.zip")
    with zipfile.ZipFile(unsigned, "a", compression=zipfile.ZIP_DEFLATED) as z:
        z.write(dex, "classes.dex")
    aligned = d / "aligned.apk"
    run(BT / "zipalign.exe", "-f", "4", unsigned, aligned)
    final = OUT / f"{key}.apk"
    run(BT / "apksigner.bat", "sign", "--ks", KEYSTORE, "--ks-pass", "pass:android", "--key-pass", "pass:android",
        "--v4-signing-enabled", "false", "--out", final, aligned)
    return final


def main():
    if not KEYSTORE.exists():
        sys.exit(f"missing {KEYSTORE}: run a gradle debug build once")
    types = json.loads(TYPES_JSON.read_text(encoding="utf-8"))
    vc, vn = types["versionCode"], types["versionName"]
    OUT.mkdir(parents=True, exist_ok=True)
    for old in list(OUT.glob("*.apk")) + list(OUT.glob("*.idsig")):
        old.unlink()
    dex = build_dex()
    total = 0
    for entry in types["types"]:
        apk = build_stub(entry["type"], entry["label"], dex, vc, vn, entry.get("labels"))
        total += apk.stat().st_size
    print(f"built {len(types['types'])} stub APKs, {total // 1024} KB total -> {OUT}")


if __name__ == "__main__":
    main()
