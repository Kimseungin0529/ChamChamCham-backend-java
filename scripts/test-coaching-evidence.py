"""외부 통신 없이 출처/결과의 무결성과 경계를 검증한다."""
import copy
import hashlib
import importlib.util
import json
from pathlib import Path
import tempfile
import unittest
import sys

sys.dont_write_bytecode = True

MODULE = Path(__file__).with_name("coaching-evidence.py")


class SourceChecks(unittest.TestCase):
    def setUp(self):
        spec = importlib.util.spec_from_file_location("coaching_evidence", MODULE)
        self.tool = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(self.tool)
        self.tmp = tempfile.TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        self.root = Path(self.tmp.name)
        folder = self.root / ".local/rag-api/corpus"
        folder.mkdir(parents=True)
        (folder / "raw.html").write_bytes(b"original")
        (folder / "selected.txt").write_text("reviewed paragraph", encoding="utf-8")
        self.manifest = self.root / "manifest.json"
        self.data = {"sources": [{"sourceId": "one", "rawFile": ".local/rag-api/corpus/raw.html",
            "rawSha256": hashlib.sha256(b"original").hexdigest(),
            "selectedFile": ".local/rag-api/corpus/selected.txt",
            "selectedSha256": hashlib.sha256(b"reviewed paragraph").hexdigest()}]}
        self.save()

    def save(self):
        self.manifest.write_text(json.dumps(self.data), encoding="utf-8")

    def test_original_and_new_source_roots_are_allowed(self):
        added = self.root / ".local/rag-api/evidence-selection/sources"
        added.mkdir(parents=True)
        row = copy.deepcopy(self.data["sources"][0])
        row["sourceId"] = "two"
        for field in ["rawFile", "selectedFile"]:
            old = self.root / row[field]
            target = added / old.name
            target.write_bytes(old.read_bytes())
            row[field] = str(target.relative_to(self.root))
        self.data["sources"].append(row)
        self.save()
        self.assertEqual(self.tool.check_sources(self.manifest, self.root)["verified"], 2)

    def test_raw_hash_mismatch_is_an_error(self):
        (self.root / self.data["sources"][0]["rawFile"]).write_bytes(b"changed")
        with self.assertRaisesRegex(ValueError, "hash"):
            self.tool.check_sources(self.manifest, self.root)

    def test_missing_selected_is_an_error(self):
        (self.root / self.data["sources"][0]["selectedFile"]).unlink()
        with self.assertRaisesRegex(ValueError, "missing"):
            self.tool.check_sources(self.manifest, self.root)

    def test_parent_escape_and_symlink_escape_are_rejected(self):
        for path in ["../raw.html", ".local/rag-api/corpus/escape"]:
            if path.endswith("escape"):
                (self.root / path).symlink_to(self.manifest)
            self.data["sources"][0]["rawFile"] = path
            self.save()
            with self.assertRaisesRegex(ValueError, "outside"):
                self.tool.check_sources(self.manifest, self.root)

    def test_duplicate_source_id_is_rejected_even_with_matching_bytes(self):
        self.data["sources"].append(copy.deepcopy(self.data["sources"][0]))
        self.save()
        with self.assertRaisesRegex(ValueError, "duplicate"):
            self.tool.check_sources(self.manifest, self.root)

    def corpus_fixture(self):
        folder = self.root / ".local/rag-api/evidence-selection"
        folder.mkdir(parents=True)
        corpus = folder / "corpus.json"
        rules = self.root / "rules.json"
        document = {"id": "doc", "text": "reviewed paragraph", "metadata": {"sourceId": "one"}}
        rule = {"documentId": "doc", "textSha256": hashlib.sha256(b"reviewed paragraph").hexdigest(),
                "constraints": {"harvestPart": {"state": "EXPLICIT", "allowed": ["LEAF"]}},
                "notStated": ["cropName", "workType", "species", "variety", "growthStage", "cultivation", "propagation", "region"],
                "unresolvedScope": False, "caveats": ["합성 주석 검토"],
                "reference": {"sourceId": "one", "selectedFile": self.data["sources"][0]["selectedFile"],
                              "quote": "reviewed", "offset": 0}}
        self.data.update(corpusFile=str(corpus.relative_to(self.root)), rulesFile="rules.json")
        self.save()
        corpus.write_text(json.dumps({"documents": [document]}))
        rules.write_text(json.dumps({"rules": [rule]}))
        return corpus, rules, document, rule

    def test_corpus_binding_checks_duplicate_id_text_hash_and_quote_position(self):
        corpus, rules, document, rule = self.corpus_fixture()
        self.assertEqual(self.tool.check_sources(self.manifest, self.root)["documents"], 1)
        for bad_rule in [dict(rule, textSha256="wrong"),
                         dict(rule, reference=dict(rule["reference"], offset=2))]:
            rules.write_text(json.dumps({"rules": [bad_rule]}))
            with self.assertRaises(ValueError):
                self.tool.check_sources(self.manifest, self.root)
        rules.write_text(json.dumps({"rules": [rule]}))
        corpus.write_text(json.dumps({"documents": [document, dict(document, text="other")]}))
        with self.assertRaisesRegex(ValueError, "duplicate"):
            self.tool.check_sources(self.manifest, self.root)

    def test_incomplete_or_wrongly_typed_annotation_is_rejected(self):
        corpus, rules, document, original = self.corpus_fixture()
        variants = []
        missing_constraint = copy.deepcopy(original)
        missing_constraint["constraints"].pop("harvestPart")
        variants.append(missing_constraint)
        for field in ["notStated", "unresolvedScope", "constraints", "caveats"]:
            row = copy.deepcopy(original); row.pop(field); variants.append(row)
        for value in [None, "false", 0]:
            row = copy.deepcopy(original); row["unresolvedScope"] = value; variants.append(row)
        row = copy.deepcopy(original); row["notStated"].append("harvestPart"); variants.append(row)
        row = copy.deepcopy(original); row["constraints"]["harvestPart"]["allowed"] = "LEAF"; variants.append(row)
        for row in variants:
            with self.subTest(row=row):
                rules.write_text(json.dumps({"rules": [row]}))
                with self.assertRaisesRegex(ValueError, "annotation"):
                    self.tool.check_sources(self.manifest, self.root)

    def test_summary_refuses_changed_inputs_and_result(self):
        run = self.root / ".local/rag-api/evidence-selection/runs/one"
        run.mkdir(parents=True)
        paths = ["manifest.json", self.data["sources"][0]["rawFile"], self.data["sources"][0]["selectedFile"]]
        payload = {"complete": True, "inputHashes": {name: self.tool.sha(self.root / name) for name in paths},
                   "summary": {"situations": 1}}
        result = run / "result.json"
        result.write_text(json.dumps(payload), encoding="utf-8")
        seal = run / "identity.json"
        seal.write_text(json.dumps({"resultSha256": self.tool.sha(result)}), encoding="utf-8")
        self.assertEqual(self.tool.read_summary(run)["situations"], 1)
        raw = self.root / paths[1]; raw.write_bytes(b"changed raw")
        with self.assertRaisesRegex(ValueError, "input"):
            self.tool.read_summary(run)
        raw.write_bytes(b"original")
        selected = self.root / paths[2]; selected.unlink()
        with self.assertRaisesRegex(ValueError, "input"):
            self.tool.read_summary(run)
        selected.write_text("reviewed paragraph", encoding="utf-8")
        self.manifest.write_text("changed", encoding="utf-8")
        with self.assertRaisesRegex(ValueError, "input"):
            self.tool.read_summary(run)
        result.write_text("{}", encoding="utf-8")
        with self.assertRaisesRegex(ValueError, "result"):
            self.tool.read_summary(run)

    def test_summary_rejects_outside_run(self):
        with self.assertRaisesRegex(ValueError, "run"):
            self.tool.read_summary(self.root)


if __name__ == "__main__":
    unittest.main()
