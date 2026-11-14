import importlib.util
import os
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest

spec = importlib.util.spec_from_file_location('runner', Path(__file__).with_name('verify-rag-local.py'))
runner = importlib.util.module_from_spec(spec)
spec.loader.exec_module(runner)

class RunnerTest(unittest.TestCase):
    def test_hung_verification_is_terminated_before_cleanup(self):
        with tempfile.TemporaryDirectory() as directory, tempfile.TemporaryFile(mode='w+') as log:
            pidfile = Path(directory) / 'pid'
            program = 'import os,sys,time; from pathlib import Path; Path(sys.argv[1]).write_text(str(os.getpid())); time.sleep(30)'
            with self.assertRaises(subprocess.TimeoutExpired):
                runner.run_verification([sys.executable, '-c', program, str(pidfile)], {}, log, timeout=.5)
            pid = int(pidfile.read_text())
            with self.assertRaises(ProcessLookupError):
                os.kill(pid, 0)

if __name__ == '__main__':
    unittest.main()
