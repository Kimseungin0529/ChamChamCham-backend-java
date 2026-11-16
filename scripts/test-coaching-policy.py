import copy
import importlib.util
import json
from pathlib import Path
import sys
import tempfile
import unittest
sys.dont_write_bytecode = True
path = Path(__file__).with_name('coaching-policy.py')
spec = importlib.util.spec_from_file_location('policy', path)
m = importlib.util.module_from_spec(spec)
if path.exists(): spec.loader.exec_module(m)

ITEM = {'basis': '기록에서 직접 확인', 'text': '흙이 마른 부분부터 살펴보세요.', 'evidenceRefs': ['record:r']}
ACTION = {**ITEM, 'category': 'IRRIGATION', 'due': 'NEXT_CHECK'}
CASE = {'id': 'example', 'documentCitationIds': ['doc:1'], 'allowedCitationIds': ['record:r', 'doc:1'],
        'prompt': {'system': '기존 정책', 'user': '기록과 근거'}, 'expectation': {'mode': 'ANSWER'}}
class PolicyTest(unittest.TestCase):
    def test_policy_can_abstain_without_inventing_praise_or_actions(self):
        value = {'decision': 'ABSTAIN', 'goodPoint': None, 'nextActions': [],
                 'questions': [], 'missingInformation': ['해당 부위 재배 문서']}
        self.assertEqual([], m.validate_content(value, 'candidate', CASE))
        self.assertTrue(m.validate_content(value, 'baseline', CASE))

    def test_existing_variant_rejects_missing_actions_and_unknown_citation(self):
        value = {'goodPoint': ITEM, 'nextActions': [ACTION, {**ACTION, 'evidenceRefs': ['fake']}]}
        self.assertIn('unknown_evidence', m.validate_content(value, 'baseline', CASE))
        value['nextActions'] = []
        self.assertIn('action_count', m.validate_content(value, 'baseline', CASE))

    def test_clarification_must_state_a_question_and_unknown_information(self):
        value = {'decision': 'CLARIFY', 'goodPoint': None, 'nextActions': [], 'questions': [], 'missingInformation': []}
        warnings = m.validate_content(value, 'candidate', CASE)
        self.assertIn('clarify_without_question', warnings)
        self.assertIn('missing_information_required', warnings)

    def test_abstention_cannot_hide_recommendations(self):
        value = {'decision': 'ABSTAIN', 'goodPoint': ITEM, 'nextActions': [ACTION], 'questions': [], 'missingInformation': ['품종']}
        self.assertIn('abstention_with_advice', m.validate_content(value, 'candidate', CASE))

    def test_changed_run_identity_does_not_overwrite_existing_results(self):
        with tempfile.TemporaryDirectory() as d:
            folder = Path(d)
            m.bind_run(folder, {'model': 'a'})
            with self.assertRaises(RuntimeError): m.bind_run(folder, {'model': 'b'})
            self.assertEqual({'model': 'a'}, json.loads((folder/'manifest.json').read_text()))

    def test_transport_failure_is_terminal_and_resume_preserves_it(self):
        with tempfile.TemporaryDirectory() as d:
            folder = Path(d)
            def failing(body): raise TimeoutError('private text must not be logged')
            row = m.execute(CASE, 'baseline', folder, {'model': 'local'}, failing)
            self.assertEqual('MODEL_ERROR', row['status'])
            self.assertNotIn('private text', json.dumps(row))
            def forbidden(body): self.fail('completed failure was replayed')
            self.assertEqual(row, m.execute(CASE, 'baseline', folder, {'model': 'local'}, forbidden))

    def test_request_pair_uses_identical_user_data_without_gold_leakage(self):
        baseline = m.make_request(CASE, 'baseline', {'model': 'qwen', 'baselineSchema': {}}, 'candidate policy')
        candidate = m.make_request(CASE, 'candidate', {'model': 'qwen', 'candidateSchema': {}}, 'candidate policy')
        self.assertEqual('기록과 근거', baseline['messages'][1]['content'])
        self.assertEqual(baseline['messages'][1], candidate['messages'][1])
        self.assertNotIn('expectation', json.dumps(candidate))

    def test_inflight_resume_requires_explicit_retry_and_keeps_attempt(self):
        with tempfile.TemporaryDirectory() as d:
            folder = Path(d);p=folder/'example-baseline.json'
            p.write_text(json.dumps({'terminal': False, 'attempt': 1}))
            with self.assertRaises(RuntimeError): m.execute(CASE, 'baseline', folder, {}, lambda _: {})
            self.assertFalse(json.loads(p.read_text())['terminal'])

    def test_summary_never_counts_unreviewed_or_model_failed_as_semantic_success(self):
        rows = [{'caseId':'a','variant':'candidate','status':'VALID','seconds':3,'terminal':True},
                {'caseId':'b','variant':'candidate','status':'MODEL_ERROR','seconds':4,'terminal':True}]
        summary = m.summarize(rows, {})['candidate']
        self.assertEqual(2, summary['executed'])
        self.assertEqual(1, summary['formatValid'])
        self.assertEqual(0, summary['reviewed'])
        self.assertEqual(0, summary['supported'])
        self.assertEqual(1, summary['semanticPending'])

    def test_review_must_match_response_hash_and_include_a_reason(self):
        row = {'status':'VALID','response':{'goodPoint': ITEM},'caseId':'a','variant':'baseline'}
        with self.assertRaises(ValueError): m.check_review(row, {'label':'SUPPORTED','responseSha256':'wrong','reason':'x','flags':[]})

class GuardTest(unittest.TestCase):
    def test_pending_or_wrong_candidate_development_cannot_unlock_evaluation(self):
        with tempfile.TemporaryDirectory() as d:
            folder=Path(d);m.bind_run(folder,{'split':'dev','candidateSha256':'old'})
            with self.assertRaises(RuntimeError):m.require_development(folder,'new')
            m.write(folder/'summary.json',{'baseline':{'executed':24,'reviewed':24},'candidate':{'executed':23,'reviewed':23}})
            with self.assertRaises(RuntimeError):m.require_development(folder,'old')

    def test_stale_prepared_sources_are_rejected_before_model_calls(self):
        with tempfile.TemporaryDirectory() as d:
            folder=Path(d);(folder/'source').write_text('changed')
            with self.assertRaises(RuntimeError):m.verify_sources({'source':'wrong'},folder)

class EvaluationIdentityTest(unittest.TestCase):
    def test_old_development_cannot_unlock_new_schema_or_case_set(self):
        with tempfile.TemporaryDirectory() as d:
            folder=Path(d)
            m.bind_run(folder,{'split':'dev','candidateSha256':'same','preparedSha256':'old'})
            with self.assertRaises(RuntimeError):
                m.require_development(folder,'same',{'preparedSha256':'new'},[])

    def test_development_requires_the_exact_twenty_four_case_ids(self):
        with tempfile.TemporaryDirectory() as d:
            folder=Path(d)
            m.bind_run(folder,{'split':'dev','candidateSha256':'same'})
            with self.assertRaises(RuntimeError):
                m.require_development(folder,'same',{},[{'id':'expected'}])

class EmptyEvidenceTest(unittest.TestCase):
    def test_existing_policy_preserves_product_no_document_gate_without_calling_model(self):
        case=copy.deepcopy(CASE);case['documentCitationIds']=[]
        with tempfile.TemporaryDirectory() as d:
            def forbidden(body): self.fail('product guard must prevent model generation')
            row=m.execute(case,'baseline',Path(d),{'model':'local'},forbidden)
            self.assertEqual('INSUFFICIENT_EVIDENCE',row['status'])
            self.assertFalse(row['modelCalled'])

class ReuseCountingTest(unittest.TestCase):
    def test_reused_response_is_not_counted_as_a_new_model_call(self):
        row={'caseId':'a','variant':'baseline','status':'VALID','terminal':True,'seconds':3,
             'reusedFrom':{'run':'prior','newModelCall':False}}
        summary=m.summarize([row],{})['baseline']
        self.assertEqual(0,summary['modelCalls'])
        self.assertEqual(1,summary['reusedResponses'])

if __name__ == '__main__': unittest.main()
