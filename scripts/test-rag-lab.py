#!/usr/bin/env python3
"""Docker 없이 실행하는 실습 도구의 입력·자료 보존 회귀 검사."""
import importlib.util
import json
from pathlib import Path
import shutil
import socket
import sys
import tempfile
import unittest
from unittest.mock import patch

sys.dont_write_bytecode = True

SCRIPT = Path(__file__).with_name('rag-lab.py')


class LabSafetyTest(unittest.TestCase):
    def setUp(self):
        self.assertTrue(SCRIPT.exists(), '보존형 실습 도구가 아직 없음')
        spec = importlib.util.spec_from_file_location('rag_lab', SCRIPT)
        self.lab = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(self.lab)

    def test_credentials_survive_second_initialization_and_are_private(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / '.env'
            first = self.lab.credentials(path)
            self.assertEqual(first, self.lab.credentials(path))
            self.assertEqual(0o600, path.stat().st_mode & 0o777)
            self.assertGreaterEqual(len(first['POSTGRES_PASSWORD']), 32)

    def test_busy_port_is_rejected_without_closing_the_listener(self):
        with socket.socket() as listener:
            listener.bind(('127.0.0.1', 0))
            listener.listen()
            with self.assertRaisesRegex(RuntimeError, '사용 중'):
                self.lab.require_free_port(listener.getsockname()[1])
            self.assertGreater(listener.fileno(), 0)

    def test_missing_credentials_for_an_existing_volume_are_not_recreated(self):
        with tempfile.TemporaryDirectory() as directory:
            state = Path(directory)
            with patch.object(self.lab, 'STATE', state), patch.object(self.lab, 'CORPUS', state / 'corpus'), \
                    patch.object(self.lab.subprocess, 'run') as docker:
                docker.return_value.returncode = 0  # 이미 존재하는 영구 DB 볼륨
                with self.assertRaisesRegex(RuntimeError, '자격 증명'):
                    self.lab.initialize()
                self.assertFalse((state / '.env').exists())

    def test_closed_connection_does_not_look_like_a_running_service(self):
        with socket.socket() as listener:
            listener.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
            listener.bind(('127.0.0.1', 0))
            port = listener.getsockname()[1]
            listener.listen()
            with socket.create_connection(('127.0.0.1', port)) as client:
                peer, _ = listener.accept()
                peer.close()
                self.assertEqual(b'', client.recv(1))
        self.lab.require_free_port(port)

    def test_document_and_chunk_tampering_is_rejected(self):
        source = Path('/private/tmp/chamchamcham-rag-retrieval')
        if not source.joinpath('manifest.json').exists():
            self.skipTest('이 컴퓨터의 검토된 원문 캐시가 없음; 실제 적재 검증에서 별도 확인')
        with tempfile.TemporaryDirectory() as directory:
            target = Path(directory)
            for name in ('manifest.json', 'chunks.json', '100000732652.html',
                         '100000802838.html', '100000732652.agriculture.txt',
                         '100000802838.agriculture.txt'):
                shutil.copy2(source / name, target / name)
            self.lab.validate_corpus(target)
            chunks = json.loads((target / 'chunks.json').read_text())
            chunks[0]['metadata']['cropName'] = '임의 변경 작물'
            (target / 'chunks.json').write_text(json.dumps(chunks))
            with self.assertRaisesRegex(RuntimeError, '조각'):
                self.lab.validate_corpus(target)
            shutil.copy2(source / 'chunks.json', target / 'chunks.json')
            with (target / '100000732652.html').open('ab') as file:
                file.write(b'changed')
            with self.assertRaisesRegex(RuntimeError, '원문'):
                self.lab.validate_corpus(target)

    def test_foreign_schema_is_rejected_before_any_ddl(self):
        queries = []
        def sql(query):
            queries.append(query)
            if 'pg_namespace' in query:
                return 'public\nforeign_schema'
            if 'pg_tables' in query:
                return ''
            return 'token:1'
        with patch.object(self.lab, 'settings', return_value={'LAB_TOKEN': 'token'}), patch.object(self.lab, 'sql', sql):
            with self.assertRaisesRegex(RuntimeError, '스키마'):
                self.lab.guard()
        self.assertFalse(any('CREATE' in query or 'INSERT' in query for query in queries))

    def test_precheck_failure_invalidates_previous_result(self):
        with tempfile.TemporaryDirectory() as directory:
            state = Path(directory)
            (state / 'results').mkdir()
            output = state / 'results/search.json'
            output.write_text('{"oldSuccess": true}')
            with patch.object(self.lab, 'STATE', state), patch.object(self.lab, 'guard', side_effect=RuntimeError('guard failure')):
                with self.assertRaisesRegex(RuntimeError, 'guard failure'):
                    self.lab.java('search')
            self.assertFalse(output.exists(), '실패 후 이전 성공 결과가 남음')

    def test_interrupted_initial_copy_can_be_retried_without_partial_corpus(self):
        source = Path('/private/tmp/chamchamcham-rag-retrieval')
        if not source.joinpath('manifest.json').exists():
            self.skipTest('검토된 로컬 원문 캐시 없음')
        with tempfile.TemporaryDirectory() as directory:
            state = Path(directory)
            self.lab.credentials(state / '.env')
            original = shutil.copy2
            copied = []
            def copy(src, dst):
                copied.append(src)
                if len(copied) == 2:
                    raise OSError('복사 중단 재현')
                return original(src, dst)
            with patch.object(self.lab, 'STATE', state), patch.object(self.lab, 'CORPUS', state / 'corpus'):
                with patch.object(self.lab.shutil, 'copy2', copy):
                    with self.assertRaisesRegex(OSError, '복사 중단'):
                        self.lab.initialize()
                self.assertFalse((state / 'corpus').exists(), '불완전 자료를 정식 자료로 노출함')
                self.lab.initialize()
                self.lab.validate_corpus(state / 'corpus')


if __name__ == '__main__':
    unittest.main()
