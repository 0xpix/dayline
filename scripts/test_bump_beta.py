from __future__ import annotations

import unittest

from scripts.bump_beta import beta_code, replace_once


class BumpBetaTest(unittest.TestCase):
    def test_version_code_convention(self) -> None:
        self.assertEqual(1701, beta_code("0.17.1"))
        self.assertEqual(1800, beta_code("0.18.0"))
        self.assertEqual(1_000_000, beta_code("1.0.0"))

    def test_multiline_beta_flavor_is_updated(self) -> None:
        source = """
android {
    productFlavors {
        create("beta") {
            dimension = "distribution"
            applicationIdSuffix = ".beta"
            versionCode = 1701
            versionName = "0.17.1"
        }
    }
}
""".strip()

        updated = replace_once(
            source,
            r'(create\("beta"\)\s*\{.*?versionCode\s*=\s*)\d+',
            r'\g<1>1702',
            "beta versionCode",
        )
        updated = replace_once(
            updated,
            r'(create\("beta"\)\s*\{.*?versionName\s*=\s*")[^"]+(")',
            r'\g<1>0.17.2\g<2>',
            "beta versionName",
        )

        self.assertIn("versionCode = 1702", updated)
        self.assertIn('versionName = "0.17.2"', updated)

    def test_invalid_version_is_rejected(self) -> None:
        with self.assertRaises(SystemExit):
            beta_code("0.17")


if __name__ == "__main__":
    unittest.main()
