"""Unit tests for PE signing configuration: python3 android-pe/tools/test_build_pe_apk.py."""
import base64
import os
import stat
import tempfile
import unittest

from build_pe_apk import (
    certificate_sha256_from_apksigner,
    resolve_signing_config,
    verify_apk_signing_identity,
    write_private_file,
)


class SigningConfigTests(unittest.TestCase):
    def setUp(self):
        self.complete = {
            "BLOCKCRAFT_PE_KEYSTORE_B64": base64.b64encode(b"test-keystore-bytes").decode("ascii"),
            "BLOCKCRAFT_PE_STORE_PASSWORD": "store-secret",
            "BLOCKCRAFT_PE_KEY_ALIAS": "pe-release",
            "BLOCKCRAFT_PE_KEY_PASSWORD": "key-secret",
            "BLOCKCRAFT_PE_EXPECTED_CERT_SHA256": "ab" * 32,
        }

    def test_local_build_without_secrets_uses_disposable_signer(self):
        self.assertIsNone(resolve_signing_config({}))

    def test_release_build_refuses_to_fall_back_to_disposable_signer(self):
        env = {"BLOCKCRAFT_PE_REQUIRE_PERSISTENT_SIGNING": "true"}
        with self.assertRaisesRegex(RuntimeError, "Persistent PE signing is required"):
            resolve_signing_config(env)

    def test_partial_persistent_configuration_is_rejected(self):
        with self.assertRaisesRegex(RuntimeError, "configuration is incomplete"):
            resolve_signing_config({"BLOCKCRAFT_PE_KEY_ALIAS": "pe-release"})

    def test_complete_persistent_configuration_decodes_keystore(self):
        config = resolve_signing_config(self.complete)
        self.assertEqual(config["keystore_data"], b"test-keystore-bytes")
        self.assertEqual(config["key_alias"], "pe-release")
        self.assertEqual(config["store_password"], "store-secret")
        self.assertEqual(config["key_password"], "key-secret")
        self.assertEqual(config["expected_cert_sha256"], "ab" * 32)

    def test_release_config_requires_pinned_signing_certificate(self):
        env = dict(self.complete)
        env.pop("BLOCKCRAFT_PE_EXPECTED_CERT_SHA256")
        env["BLOCKCRAFT_PE_REQUIRE_PERSISTENT_SIGNING"] = "1"
        with self.assertRaisesRegex(RuntimeError, "repository variable"):
            resolve_signing_config(env)

    def test_invalid_certificate_fingerprint_is_rejected(self):
        env = dict(self.complete)
        env["BLOCKCRAFT_PE_EXPECTED_CERT_SHA256"] = "sha256:too-short"
        with self.assertRaisesRegex(RuntimeError, "exactly 64 hexadecimal digits"):
            resolve_signing_config(env)

    def test_apksigner_fingerprint_is_normalized_and_pinned(self):
        fingerprint = "ab" * 32
        colon_fingerprint = ":".join(fingerprint[i:i + 2] for i in range(0, len(fingerprint), 2)).upper()
        output = "Signer #1 certificate SHA-256 digest: " + colon_fingerprint + "\n"
        self.assertEqual(certificate_sha256_from_apksigner(output), fingerprint)
        self.assertEqual(verify_apk_signing_identity(output, fingerprint), fingerprint)
        with self.assertRaisesRegex(RuntimeError, "does not match"):
            verify_apk_signing_identity(output, "cd" * 32)

    def test_apksigner_must_report_certificate_fingerprint(self):
        with self.assertRaisesRegex(RuntimeError, "did not report"):
            certificate_sha256_from_apksigner("Verified using v2 scheme (APK Signature Scheme v2): true\n")

    def test_invalid_base64_is_rejected(self):
        env = dict(self.complete)
        env["BLOCKCRAFT_PE_KEYSTORE_B64"] = "not base64!"
        with self.assertRaisesRegex(RuntimeError, "not valid base64"):
            resolve_signing_config(env)

    def test_keystore_file_is_written_private(self):
        with tempfile.TemporaryDirectory() as temp_dir:
            path = os.path.join(temp_dir, "release.jks")
            write_private_file(path, b"private-key-material")
            with open(path, "rb") as stream:
                self.assertEqual(stream.read(), b"private-key-material")
            if os.name == "posix":
                self.assertEqual(stat.S_IMODE(os.stat(path).st_mode), 0o600)


if __name__ == "__main__":
    unittest.main()
