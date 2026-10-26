import json
import os
import shutil
import subprocess
import sys
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SDK = Path(os.environ.get("ANDROID_HOME") or Path.home() / "scoop/apps/android-clt/current")
BUILD_TOOLS = SDK / "build-tools" / "34.0.0"
ANDROID_JAR = SDK / "platforms" / "android-34" / "android.jar"
JAVA_HOME = Path(os.environ.get("JAVA_HOME") or Path.home() / "scoop/apps/temurin17-jdk/current")
DEBUG_KEYSTORE = Path.home() / ".android" / "debug.keystore"
KEYSTORE_PROPERTIES = ROOT / "keystore.properties"
VERSION_PROPERTIES = ROOT / "version.properties"
TYPES_JSON = ROOT / "tools" / "input_types.json"
RES_PNG = ROOT / "app" / "src" / "main" / "res" / "drawable-xhdpi"
OUT = ROOT / "app" / "src" / "main" / "assets" / "stubs"
WORK = ROOT / "build" / "stubs"
STUB_PACKAGE_PREFIX = "pt.aadf.sourcesswitcher.input."

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


def run(*cmd):
    r = subprocess.run([str(c) for c in cmd], capture_output=True, text=True)
    if r.returncode != 0:
        sys.exit(f"command failed: {' '.join(str(c) for c in cmd)}\n{r.stdout}\n{r.stderr}")
    return r


def read_properties(path):
    props = {}
    for line in path.read_text(encoding="utf-8").splitlines():
        line = line.strip()
        if line and not line.startswith("#") and "=" in line:
            k, v = line.split("=", 1)
            props[k.strip()] = v.strip()
    return props


def signing():
    if KEYSTORE_PROPERTIES.exists():
        p = read_properties(KEYSTORE_PROPERTIES)
        return Path(p["storeFile"]), p["storePassword"], p["keyAlias"], p["keyPassword"]
    if not DEBUG_KEYSTORE.exists():
        sys.exit(f"missing {DEBUG_KEYSTORE}: run a gradle debug build once")
    return DEBUG_KEYSTORE, "android", "androiddebugkey", "android"


def xml_text(s):
    return s.replace("&", "&amp;").replace("<", "&lt;").replace("'", "\\'")


def build_dex():
    classes = WORK / "classes"
    shutil.rmtree(classes, ignore_errors=True)
    classes.mkdir(parents=True)
    run(JAVA_HOME / "bin" / "javac", "-source", "8", "-target", "8", "-bootclasspath", ANDROID_JAR,
        "-d", classes, ROOT / "tools" / "stub" / "Trampoline.java")
    dex_dir = WORK / "dex"
    shutil.rmtree(dex_dir, ignore_errors=True)
    dex_dir.mkdir()
    run(BUILD_TOOLS / "d8.bat", "--release", "--min-api", "25", "--lib", ANDROID_JAR, "--output", dex_dir,
        *classes.rglob("*.class"))
    return dex_dir / "classes.dex"


def write_stub_resources(d, label, labels_by_qualifier):
    (d / "res" / "values").mkdir(parents=True)
    (d / "res" / "values" / "strings.xml").write_text(
        f'<resources><string name="label">{xml_text(label)}</string></resources>', encoding="utf-8")
    for qualifier, localized in labels_by_qualifier.items():
        ld = d / "res" / f"values-{qualifier}"
        ld.mkdir()
        (ld / "strings.xml").write_text(
            f'<resources><string name="label">{xml_text(localized)}</string></resources>', encoding="utf-8")


def build_stub(entry, dex, version, key):
    t = entry["type"]
    name = t.lower()
    d = WORK / name
    shutil.rmtree(d, ignore_errors=True)
    write_stub_resources(d, entry["label"], entry.get("labels", {}))
    (d / "res" / "drawable-xhdpi").mkdir(parents=True)
    shutil.copy(RES_PNG / f"banner_{name}.png", d / "res" / "drawable-xhdpi" / "banner.png")
    shutil.copy(RES_PNG / f"ic_{name}.png", d / "res" / "drawable-xhdpi" / "ic.png")
    (d / "AndroidManifest.xml").write_text(
        MANIFEST.format(pkg=STUB_PACKAGE_PREFIX + name, type=t, vc=version["VERSION_CODE"], vn=version["VERSION_NAME"]),
        encoding="utf-8")
    compiled = d / "compiled"
    compiled.mkdir()
    run(BUILD_TOOLS / "aapt2.exe", "compile", "--dir", d / "res", "-o", compiled / "res.zip")
    unsigned = d / "unsigned.apk"
    run(BUILD_TOOLS / "aapt2.exe", "link", "-o", unsigned, "-I", ANDROID_JAR, "--manifest", d / "AndroidManifest.xml",
        compiled / "res.zip")
    with zipfile.ZipFile(unsigned, "a", compression=zipfile.ZIP_DEFLATED) as z:
        z.write(dex, "classes.dex")
    aligned = d / "aligned.apk"
    run(BUILD_TOOLS / "zipalign.exe", "-f", "4", unsigned, aligned)
    store, store_pass, alias, key_pass = key
    final = OUT / f"{name}.apk"
    run(BUILD_TOOLS / "apksigner.bat", "sign", "--ks", store, "--ks-pass", f"pass:{store_pass}",
        "--ks-key-alias", alias, "--key-pass", f"pass:{key_pass}", "--v4-signing-enabled", "false",
        "--out", final, aligned)
    return final


def main():
    version = read_properties(VERSION_PROPERTIES)
    key = signing()
    types = json.loads(TYPES_JSON.read_text(encoding="utf-8"))["types"]
    OUT.mkdir(parents=True, exist_ok=True)
    for old in list(OUT.glob("*.apk")) + list(OUT.glob("*.idsig")):
        old.unlink()
    dex = build_dex()
    total = sum(build_stub(entry, dex, version, key).stat().st_size for entry in types)
    print(f"built {len(types)} stub APKs, {total // 1024} KB total, signed with {key[0].name} -> {OUT}")


if __name__ == "__main__":
    main()
