from __future__ import annotations

import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

from scripts import check_beta_bump as guard


class CheckBetaBumpTest(unittest.TestCase):
    def test_tuple_version_orders_patch_versions(self) -> None:
        self.assertLess(guard.tuple_version("0.17.3"), guard.tuple_version("0.17.4"))
        self.assertLess(guard.tuple_version("0.17.9"), guard.tuple_version("0.18.0"))

    def test_tuple_version_rejects_invalid_values(self) -> None:
        with self.assertRaises(ValueError):
            guard.tuple_version("0.17")
        with self.assertRaises(ValueError):
            guard.tuple_version("v0.17.4.beta")

    def test_current_beta_version_reads_only_beta_flavor(self) -> None:
        source = """
android {
    defaultConfig {
        versionName = "0.14.0"
    }
    productFlavors {
        create("beta") {
            versionCode = 1704
            versionName = "0.17.4"
        }
        create("play") {
            versionName = "9.9.9"
        }
    }
}
""".strip()
        with tempfile.TemporaryDirectory() as tmp:
            gradle = Path(tmp) / "build.gradle.kts"
            gradle.write_text(source, encoding="utf-8")
            with patch.object(guard, "GRADLE", gradle):
                self.assertEqual("0.17.4", guard.current_beta_version())

    def test_beta_tags_ignore_unrelated_and_malformed_tags(self) -> None:
        tags = "\n".join(
            [
                "v0.17.2.beta",
                "v0.17.4.beta",
                "v0.17.beta",
                "vbanana.beta",
                "v0.16.9.beta",
            ]
        )
        with patch.object(guard, "run", return_value=tags):
            parsed = guard.beta_tags()

        self.assertEqual(
            [
                ((0, 16, 9), "v0.16.9.beta"),
                ((0, 17, 2), "v0.17.2.beta"),
                ((0, 17, 4), "v0.17.4.beta"),
            ],
            parsed,
        )


if __name__ == "__main__":
    unittest.main()
