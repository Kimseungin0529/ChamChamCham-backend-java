#!/usr/bin/env python3
"""검색 실습의 원문 결속·격리·불변 결과 계약을 검사한다. 서비스는 시작하지 않는다."""
import copy
import hashlib
import importlib.util
import json
from pathlib import Path
import tempfile
import unittest
import sys

sys.dont_write_bytecode = True

loader = importlib.util.spec_from_file_location('retrieval', Path(__file__).with_name('coaching-retrieval.py'))


class RetrievalTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.folder = self.root / '.local/rag-api/retrieval-expansion'
        self.folder.mkdir(parents=True)
        self.source = self.folder / 'sources/a.txt'
        self.source.parent.mkdir()
        self.source.write_text('첫 문장. 둘째 문장.')
        self.manifest = {'sources': [{'sourceId': 'a', 'rawFile': str(self.source.relative_to(self.root)),
                                     'rawSha256': hashlib.sha256(self.source.read_bytes()).hexdigest(),
                                     'selectedFile': str(self.source.relative_to(self.root)),
                                     'selectedSha256': hashlib.sha256(self.source.read_bytes()).hexdigest()}], 'preservedHashes': {}}
        self.spec = {'segments': [{'id': '00000000-0000-0000-0000-000000000001', 'parentId': None,
                     'sourceId': 'a', 'offset': 0, 'length': 5, 'textSha256': hashlib.sha256('첫 문장.'.encode()).hexdigest(),
                     'groups': ['expanded'], 'metadata': {'cropName': '상추', 'sourceType': 'TECH_DOCUMENT'},
                     'constraints': {'cropName': {'state': 'EXPLICIT', 'allowed': ['상추']}},
                     'notStated': ['workType','species','variety','harvestPart','growthStage','cultivation','propagation','region'],
                     'unresolvedScope': False, 'caveats': ['실습']}]}

    def test_exact_source_range_becomes_document(self):
        result = lab.build_corpus(self.root, self.manifest, self.spec)
        self.assertEqual(result['expanded'][0]['text'], '첫 문장.')
        self.assertEqual(result['expanded'][0]['metadata']['sourceId'], 'a')

    def test_changed_raw_source_is_rejected(self):
        self.source.write_text('변경')
        with self.assertRaisesRegex(ValueError, 'hash'):
            lab.build_corpus(self.root, self.manifest, self.spec)

    def test_range_cannot_silently_select_different_sentence(self):
        self.spec['segments'][0]['offset'] = 1
        with self.assertRaisesRegex(ValueError, 'range|hash'):
            lab.build_corpus(self.root, self.manifest, self.spec)

    def test_missing_condition_cannot_promote_document(self):
        self.spec['segments'][0]['notStated'].remove('variety')
        with self.assertRaisesRegex(ValueError, 'annotation'):
            lab.build_corpus(self.root, self.manifest, self.spec)

    def test_source_symlink_escape_is_rejected(self):
        outside = self.root / 'outside.txt'; outside.write_text('secret')
        self.source.unlink(); self.source.symlink_to(outside)
        with self.assertRaisesRegex(ValueError, 'path'):
            lab.build_corpus(self.root, self.manifest, self.spec)

    def test_duplicate_segment_is_rejected(self):
        self.spec['segments'].append(copy.deepcopy(self.spec['segments'][0]))
        with self.assertRaisesRegex(ValueError, 'duplicate'):
            lab.build_corpus(self.root, self.manifest, self.spec)

    def test_source_folder_symlink_cannot_expand_allowed_boundary(self):
        outside=self.root/'external';outside.mkdir()
        target=outside/'a.txt';target.write_bytes(self.source.read_bytes())
        self.source.unlink();self.source.parent.rmdir();self.source.parent.symlink_to(outside,target_is_directory=True)
        with self.assertRaisesRegex(ValueError,'path'):
            lab.build_corpus(self.root,self.manifest,self.spec)

    def test_wrong_database_identity_or_foreign_table_refuses_writes(self):
        self.assertEqual(lab.database_mode('abc', 'abc', ['lab_identity'], ['public']), 'EMPTY')
        self.assertEqual(lab.database_mode('abc', 'abc', ['lab_identity','baseline','split','expanded'], ['public']), 'READY')
        for identity, tables, schemas in [('other',['lab_identity'],['public']), ('abc',['member'],['public']), ('abc',['lab_identity'],['public','other'])]:
            with self.subTest(identity=identity,tables=tables,schemas=schemas):
                with self.assertRaises(ValueError): lab.database_mode(identity,'abc',tables,schemas)

    def test_run_names_and_existing_failure_are_not_overwritten(self):
        run = lab.new_run(self.root, 'sample-1')
        (run/'failure.json').write_text('{}')
        with self.assertRaises(FileExistsError): lab.new_run(self.root, 'sample-1')
        for name in ['../escape','x/y','UPPER','']:
            with self.assertRaises(ValueError): lab.new_run(self.root,name)

    def test_changed_result_or_input_is_not_current_summary(self):
        run=lab.new_run(self.root,'summary-1'); source=self.source.relative_to(self.root).as_posix()
        result={'complete':True,'inputHashes':{source:lab.sha(self.source)},'summary':{'cases':2}}
        (run/'result.json').write_text(json.dumps(result))
        (run/'identity.json').write_text(json.dumps({'resultSha256':lab.sha(run/'result.json')}))
        self.assertEqual(lab.read_summary(self.root,'summary-1'),{'cases':2})
        self.source.write_text('changed')
        with self.assertRaisesRegex(ValueError,'input'):lab.read_summary(self.root,'summary-1')

    def test_prepared_rules_must_match_authoritative_source_spec(self):
        prepared=lab.build_corpus(self.root,self.manifest,self.spec)
        self.spec['segments'][0]['unresolvedScope']=True
        with self.assertRaisesRegex(ValueError,'prepared'):
            lab.verify_prepared(self.root,self.manifest,self.spec,prepared)

    def test_only_new_empty_volume_can_receive_database_identity(self):
        self.assertTrue(lab.can_initialize(True,[],['public']))
        for is_new,relations in [(False,[]),(True,['foreign_view']),(False,['foreign_view'])]:
            with self.assertRaises(ValueError):lab.can_initialize(is_new,relations,['public'])

    def test_changed_embedding_or_another_loaded_model_prevents_run(self):
        good=[{'name':'bge-m3:latest','digest':lab.EMBED_DIGEST}]
        self.assertIsNone(lab.validate_model(good,[]))
        for tags,loaded in [([{'name':'bge-m3:latest','digest':'changed'}],[]),(good,[{'name':'other:latest'}])]:
            with self.assertRaises(ValueError):lab.validate_model(tags,loaded)

    def test_unknown_split_or_missing_case_is_rejected_before_execution(self):
        data=json.loads((Path(__file__).resolve().parents[1]/lab.CASES).read_text())
        self.assertEqual(lab.validate_cases(data),{'dev':24,'evaluation':96})
        for mutated in [dict(data,cases=data['cases'][:-1]),copy.deepcopy(data)]:
            if len(mutated['cases'])==120:mutated['cases'][0]['split']='typo'
            with self.assertRaisesRegex(ValueError,'case|split|matrix'):lab.validate_cases(mutated)


if __name__ == '__main__':
    try:
        lab=importlib.util.module_from_spec(loader); loader.loader.exec_module(lab)
    except FileNotFoundError:
        raise SystemExit('RED: coaching-retrieval.py is missing')
    unittest.main()
