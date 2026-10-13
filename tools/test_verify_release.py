"""Package integrity checks, no compiler or GL needed: python3 -m unittest discover -s tools -p 'test_verify_release.py'."""
import hashlib
import io
import unittest
import zipfile
from verify_release import REQUIRED, inspect_payload


def payload(version="6.2", source="expected", tamper=False):
    data = {"bc/" + c + ".class": c.encode() for c in REQUIRED}
    text = "version=%s\nsource.sha256=%s\n" % (version, source)
    for c in REQUIRED:
        text += "class.%s.sha256=%s\n" % (c, hashlib.sha256(data["bc/" + c + ".class"]).hexdigest())
    data["bc-build.properties"] = text.encode()
    if tamper:
        data["bc/Renderer.class"] = b"old-renderer"
    out = io.BytesIO()
    with zipfile.ZipFile(out, "w") as z:
        for name, b in data.items():
            z.writestr(name, b)
    return out.getvalue()


class ReleaseTests(unittest.TestCase):
    def test_current_embedded_game(self):
        jar = payload()
        receipt = inspect_payload(b"launcher-stub" + jar, jar, "6.2", "expected")
        self.assertEqual(receipt["version"], "6.2")

    def test_old_exe_next_to_new_jar_rejected(self):
        with self.assertRaisesRegex(ValueError, "disagree"):
            inspect_payload(payload("6.1"), payload(), "6.2", "expected")

    def test_renamed_old_release_rejected(self):
        jar = payload("6.1")
        with self.assertRaisesRegex(ValueError, "version"):
            inspect_payload(jar, jar, "6.2", "expected")

    def test_same_version_stale_sources_rejected(self):
        jar = payload(source="older-sources")
        with self.assertRaisesRegex(ValueError, "stale"):
            inspect_payload(jar, jar, "6.2", "expected")

    def test_old_renderer_with_new_receipt_rejected(self):
        jar = payload(tamper=True)
        with self.assertRaisesRegex(ValueError, "visual update class"):
            inspect_payload(jar, jar, "6.2", "expected")


if __name__ == "__main__":
    unittest.main()
