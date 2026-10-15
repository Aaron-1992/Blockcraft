"""Package integrity checks, no compiler or GL needed: python3 -m unittest discover -s tools -p 'test_verify_release.py'."""
import hashlib
import io
import unittest
import zipfile
from verify_release import required, inspect_payload


def payload(version="6.5", source="expected", tamper=False, tamper_media=False):
    data = {"bc/" + c + ".class": c.encode() for c in required(version)}
    text = "version=%s\nsource.sha256=%s\n" % (version, source)
    for c in required(version):
        text += "class.%s.sha256=%s\n" % (c, hashlib.sha256(data["bc/" + c + ".class"]).hexdigest())
    if tuple(map(int, version.split("."))) >= (6, 10):
        data["media/shadow-signal.bcv"] = b"original-cartridge"
        text += "media.shadow-signal.sha256=" + hashlib.sha256(data["media/shadow-signal.bcv"]).hexdigest() + "\n"
        if tamper_media: data["media/shadow-signal.bcv"] = b"changed-cartridge"
    data["bc-build.properties"] = text.encode()
    if tamper:
        data["bc/Renderer.class"] = b"old-renderer"
    out = io.BytesIO()
    with zipfile.ZipFile(out, "w") as z:
        for name, b in data.items():
            z.writestr(name, b)
    return out.getvalue()


class ReleaseTests(unittest.TestCase):
    def test_original_cartridge_receipt(self):
        jar=payload("6.10")
        self.assertEqual(inspect_payload(jar,jar,"6.10","expected")["version"],"6.10")
        bad=payload("6.10",tamper_media=True)
        with self.assertRaisesRegex(ValueError,"media cartridge"):
            inspect_payload(bad,bad,"6.10","expected")

    def test_current_embedded_game(self):
        jar = payload()
        receipt = inspect_payload(b"launcher-stub" + jar, jar, "6.5", "expected")
        self.assertEqual(receipt["version"], "6.5")

    def test_old_exe_next_to_new_jar_rejected(self):
        with self.assertRaisesRegex(ValueError, "disagree|different game"):
            inspect_payload(payload("6.1"), payload(), "6.5", "expected")

    def test_renamed_old_release_rejected(self):
        jar = payload("6.1")
        with self.assertRaisesRegex(ValueError, "version"):
            inspect_payload(jar, jar, "6.5", "expected")

    def test_same_version_stale_sources_rejected(self):
        jar = payload(source="older-sources")
        with self.assertRaisesRegex(ValueError, "stale"):
            inspect_payload(jar, jar, "6.5", "expected")

    def test_old_renderer_with_new_receipt_rejected(self):
        jar = payload(tamper=True)
        with self.assertRaisesRegex(ValueError, "visual update class"):
            inspect_payload(jar, jar, "6.5", "expected")


if __name__ == "__main__":
    unittest.main()
