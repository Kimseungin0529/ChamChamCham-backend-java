#!/usr/bin/env python3
import importlib.util
from pathlib import Path
import tempfile
import unittest
import sys
from types import SimpleNamespace
from unittest.mock import patch
sys.dont_write_bytecode = True

class SafetyTest(unittest.TestCase):
    def setUp(self):
        spec = importlib.util.spec_from_file_location('lab', Path(__file__).with_name('rag-api.py'))
        self.lab = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(self.lab)

    def test_initialization_is_only_allowed_before_any_product_table_exists(self):
        self.assertEqual('create-only', self.lab.database_mode('owned:EMPTY', 'owned', ['public'], ['lab_identity'], True))
        with self.assertRaisesRegex(RuntimeError, '초기화'):
            self.lab.database_mode('owned:EMPTY', 'owned', ['public'], ['lab_identity', 'member'], True)

    def test_restart_always_validates_and_never_creates_schema(self):
        self.assertEqual('validate', self.lab.database_mode('owned:READY', 'owned', ['public'], ['lab_identity', 'member'], False))
        with self.assertRaisesRegex(RuntimeError, '초기화'):
            self.lab.database_mode('owned:READY', 'owned', ['public'], ['lab_identity', 'member'], True)

    def test_failed_bootstrap_is_not_automatically_retried(self):
        for initialize in (True, False):
            with self.assertRaisesRegex(RuntimeError, 'INITIALIZING'):
                self.lab.database_mode('owned:INITIALIZING', 'owned', ['public'], ['lab_identity', 'member'], initialize)

    def test_foreign_identity_or_schema_is_rejected(self):
        for row, schemas in [('foreign:READY', ['public']), ('owned:READY', ['public', 'other'])]:
            with self.assertRaises(RuntimeError):
                self.lab.database_mode(row, 'owned', schemas, ['lab_identity', 'member'], False)

    def test_credentials_are_reused_and_private(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / '.env'
            first = self.lab.credentials(path)
            self.assertEqual(first, self.lab.credentials(path))
            self.assertEqual(0o600, path.stat().st_mode & 0o777)

    def test_missing_wrapper_does_not_hide_a_still_listening_api(self):
        with tempfile.TemporaryDirectory() as directory:
            state=Path(directory);pid=state/'api.pid';pid.write_text('12345')
            with patch.object(self.lab,'STATE',state), \
                 patch.object(self.lab.subprocess,'run',return_value=SimpleNamespace(stdout='',returncode=1)), \
                 patch.object(self.lab,'run',return_value=SimpleNamespace(stdout=b'')), \
                 patch.object(self.lab.socket,'socket') as socket, patch.object(self.lab.time,'sleep'):
                socket.return_value.__enter__.return_value.connect_ex.return_value=0
                with self.assertRaises(RuntimeError):self.lab.stop_api()
            self.assertTrue(pid.exists())

if __name__ == '__main__':
    unittest.main()
