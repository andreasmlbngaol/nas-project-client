#!/usr/bin/env python3
"""Replace empty stub jars inside a Kotlin Toolchain executable-jar.

The toolchain bundles some KMP artifacts whose Maven Central jar is an empty
redirect (e.g. org.jetbrains.androidx.lifecycle:lifecycle-common-jvm), while the
real classes live under a different groupId but the same file name. Both end up
as BOOT-INF/lib/<name>.jar, the empty one wins, and the desktop app crashes with
NoClassDefFoundError at launch. This swaps each empty stub for the real jar of
the same file name from the shared Maven cache.
"""
import io
import os
import sys
import zipfile

def class_count(path):
    try:
        with zipfile.ZipFile(path) as z:
            return sum(1 for n in z.namelist() if n.endswith(".class"))
    except zipfile.BadZipFile:
        return 0

def find_real(cache, base):
    best, best_n = None, 0
    for root, _, files in os.walk(cache):
        if base in files:
            candidate = os.path.join(root, base)
            n = class_count(candidate)
            if n > best_n:
                best, best_n = candidate, n
    return best

def main():
    if len(sys.argv) != 3:
        sys.exit("usage: patch-executable-jar.py <jar> <m2-cache-dir>")
    jar, cache = sys.argv[1], sys.argv[2]
    tmp = jar + ".patched"

    with zipfile.ZipFile(jar) as zin:
        stubs = {}
        for item in zin.infolist():
            if item.filename.startswith("BOOT-INF/lib/") and item.filename.endswith(".jar"):
                if class_count_in(zin, item) == 0:
                    stubs[item.filename] = find_real(cache, os.path.basename(item.filename))

        with zipfile.ZipFile(tmp, "w", zipfile.ZIP_STORED) as zout:
            for item in zin.infolist():
                replacement = stubs.get(item.filename)
                if replacement:
                    data = open(replacement, "rb").read()
                    print(f"patch {item.filename} <- {replacement}")
                else:
                    data = zin.read(item.filename)
                out = zipfile.ZipInfo(item.filename)
                out.compress_type = item.compress_type
                out.date_time = item.date_time
                out.external_attr = item.external_attr
                zout.writestr(out, data)

    os.replace(tmp, jar)

def class_count_in(zf, item):
    try:
        with zipfile.ZipFile(io.BytesIO(zf.read(item))) as inner:
            return sum(1 for n in inner.namelist() if n.endswith(".class"))
    except zipfile.BadZipFile:
        return 0

if __name__ == "__main__":
    main()
