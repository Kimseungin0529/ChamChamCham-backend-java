#!/usr/bin/env python3
"""보존 자료 확인과 결과 읽기 전용 도구. 네트워크/DB/모델 호출 없음."""
import argparse
import hashlib
import json
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parent.parent
LAB = Path(".local/rag-api/evidence-selection")
RUN_NAME = re.compile(r"[a-z0-9][a-z0-9-]{0,63}\Z")
FIELDS = {"cropName", "workType", "species", "variety", "harvestPart", "growthStage", "cultivation", "propagation", "region"}


def sha(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def source_path(root: Path, relative: str) -> Path:
    root = root.resolve()
    candidate = (root / relative).resolve()
    allowed = [root / ".local/rag-api/corpus", root / LAB / "sources"]
    if not any(candidate.is_relative_to(folder) for folder in allowed):
        raise ValueError("source path outside approved folders")
    if not candidate.is_file():
        raise ValueError("missing source file")
    return candidate


def check_sources(manifest: Path, root: Path) -> dict:
    data = json.loads(manifest.read_text(encoding="utf-8"))
    rows = data["sources"]
    ids = set()
    for row in rows:
        if row["sourceId"] in ids:
            raise ValueError("duplicate source id")
        ids.add(row["sourceId"])
        for name in ["raw", "selected"]:
            path = source_path(root, row[name + "File"])
            if sha(path) != row[name + "Sha256"]:
                raise ValueError("source hash mismatch: " + row["sourceId"])
    for name, expected in data.get("preservedHashes", {}).items():
        if sha(source_path(root, name)) != expected:
            raise ValueError("preserved corpus hash mismatch")
    result = {"verified": len(ids), "failed": 0, "sourceIds": sorted(ids)}
    if "corpusFile" in data:
        def local(name):
            path = (root / name).resolve()
            if not path.is_relative_to(root.resolve()):
                raise ValueError("path outside root")
            return json.loads(path.read_text(encoding="utf-8"))
        documents = local(data["corpusFile"])["documents"]
        rules = local(data["rulesFile"])["rules"]
        by_id = {row["documentId"]: row for row in rules}
        if len(by_id) != len(rules):
            raise ValueError("duplicate rule id")
        seen = set()
        by_source = {row["sourceId"]: row for row in rows}
        for document in documents:
            did = document["id"]
            if did in seen:
                raise ValueError("duplicate document id")
            seen.add(did)
            rule = by_id[did]
            constraints, unstated = rule.get("constraints"), rule.get("notStated")
            if (not isinstance(constraints, dict) or not isinstance(unstated, list)
                    or not all(isinstance(field, str) for field in unstated)
                    or len(set(unstated)) != len(unstated)
                    or set(constraints) & set(unstated) or set(constraints) | set(unstated) != FIELDS
                    or type(rule.get("unresolvedScope")) is not bool
                    or not isinstance(rule.get("caveats"), list) or not rule["caveats"]
                    or not all(isinstance(value, str) and value.strip() for value in rule["caveats"])):
                raise ValueError("incomplete or invalid annotation")
            for constraint in constraints.values():
                if not isinstance(constraint, dict):
                    raise ValueError("invalid annotation constraint")
                state, allowed = constraint.get("state"), constraint.get("allowed")
                if (state not in {"EXPLICIT", "NOT_STATED", "UNRESTRICTED"} or not isinstance(allowed, list)
                        or not all(isinstance(value, str) and value.strip() for value in allowed)
                        or (state == "EXPLICIT") != bool(allowed)):
                    raise ValueError("invalid annotation constraint")
            text = document["text"]
            if hashlib.sha256(text.encode()).hexdigest() != rule["textSha256"]:
                raise ValueError("document hash mismatch")
            reference = rule["reference"]
            source = by_source[document["metadata"]["sourceId"]]
            if reference["sourceId"] != source["sourceId"] or reference["selectedFile"] != source["selectedFile"]:
                raise ValueError("source binding mismatch")
            selected = source_path(root, source["selectedFile"]).read_text(encoding="utf-8")
            offset, quote = reference["offset"], reference["quote"]
            if (not text or text not in selected or not quote or quote not in text
                    or offset < 0 or selected[offset:offset + len(text)] != text):
                raise ValueError("selected quote/offset mismatch")
        if set(by_id) != seen:
            raise ValueError("unbound rules")
        result["documents"] = len(seen)
    return result


def read_summary(run: Path) -> dict:
    run = run.resolve()
    if (not RUN_NAME.fullmatch(run.name) or run.parent.name != "runs"
            or list(p.name for p in list(run.parents)[1:4]) != ["evidence-selection", "rag-api", ".local"]):
        raise ValueError("invalid run path")
    root = run.parents[4]
    result = run / "result.json"
    identity = json.loads((run / "identity.json").read_text(encoding="utf-8"))
    if sha(result) != identity["resultSha256"]:
        raise ValueError("result hash mismatch")
    data = json.loads(result.read_text(encoding="utf-8"))
    if not data.get("complete") or not data.get("inputHashes"):
        raise ValueError("incomplete result or missing input hashes")
    for name, expected in data["inputHashes"].items():
        path = (root / name).resolve()
        if not path.is_relative_to(root) or not path.is_file() or sha(path) != expected:
            raise ValueError("input changed; create a new run")
    return data["summary"]


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    sub = parser.add_subparsers(dest="command", required=True)
    sub.add_parser("check-sources")
    summary = sub.add_parser("summary")
    summary.add_argument("--run", required=True)
    args = parser.parse_args()
    if args.command == "check-sources":
        result = check_sources(ROOT / "docs/verification/coaching-evidence-sources.json", ROOT)
    else:
        if not RUN_NAME.fullmatch(args.run):
            parser.error("invalid run name")
        result = read_summary(ROOT / LAB / "runs" / args.run)
    print(json.dumps(result, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    try:
        main()
    except (ValueError, KeyError, OSError) as error:
        raise SystemExit("확인 실패: " + str(error))
