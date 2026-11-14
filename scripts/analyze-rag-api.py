#!/usr/bin/env python3
"""실제 실행 파일에서 분모와 미실행을 분리한 통계를 만든다. 의미 평가를 자동 통과시키지 않는다."""
import argparse
from collections import Counter
import csv
import hashlib
import importlib.util
import json
from pathlib import Path
import statistics
import sys
sys.dont_write_bytecode=True
spec=importlib.util.spec_from_file_location('evaluation',Path(__file__).with_name('evaluate-rag-api.py'))
evaluation=importlib.util.module_from_spec(spec);spec.loader.exec_module(evaluation)
lab=evaluation.lab

def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--run',required=True)
    parser.add_argument('--split',choices=['dev','evaluation'],required=True)
    args=parser.parse_args()
    if not args.run.replace('-','').replace('_','').isalnum():raise ValueError('실행명 확인 필요')
    folder=lab.STATE/'evaluations'/args.run
    if (folder/'cases.sha256').read_text()!=hashlib.sha256(evaluation.CASES.read_bytes()).hexdigest():
        raise RuntimeError('실행 당시 사례와 현재 사례가 다릅니다. 결과를 혼합하지 않습니다')
    cases=[c for c in json.loads(evaluation.CASES.read_text())['cases'] if c['split']==args.split]
    annotations_path=folder/'semantic-reviews.json'
    annotations=json.loads(annotations_path.read_text()) if annotations_path.exists() else {}
    rows=[];missing=[];incomplete=[];results=[]
    for case in cases:
        path=folder/(case['id']+'.json')
        if not path.exists():missing.append(case['id']);continue
        try:result=json.loads(path.read_text())
        except json.JSONDecodeError:incomplete.append(case['id']);continue
        if not result['terminal']:incomplete.append(case['id']);continue
        results.append(result)
        reference=result.get('pesticideCatalog')
        catalog_path=lab.STATE/'catalog-reference.json'
        if reference is None and result['request'].get('pestControl') and catalog_path.exists():
            reference=json.loads(catalog_path.read_text())['products'][result['request']['pestControl']['pesticideId']]
        checks=evaluation.preservation_checks(result['request'],result['recordResponse'],result['dbSnapshot'],reference) if result.get('dbSnapshot') else {}
        if result.get('dbSnapshot'):
            s=result['dbSnapshot'];p=result['request']
            checks.update({'snapshotFarm':s['farm']['farmId']==p['farmId'],'snapshotWorkedAt':s['record']['workedAt']==p['workedAt'],
                'snapshotWeather':s['record']['recordedWeatherCondition']==p['weatherCondition'] and s['record']['recordedTemperatureC']==p['weatherTemperature']})
        retrieval=[d for name in result.get('traceFiles',[]) if '-retrieval-' in name for d in json.loads((lab.ROOT/name).read_text())['documents']]
        hit=bool(set(case['expectedChunkIdsAny']) & {d['id'] for d in retrieval}) if case['expectedChunkIdsAny'] and result.get('createHttp')==200 else None
        status=result.get('status',{}).get('status','API_REJECTED')
        rows.append({'caseId':case['id'],'crop':case['crop'],'workType':case['workType'],'expectedHttp':case['expectedHttp'],
            'actualHttp':result.get('createHttp'),'httpMatchesExpected':case['expectedHttp']==result.get('createHttp'),
            'generationStatus':status,'failureCode':result.get('status',{}).get('failureCode'),
            'valuesPreserved':all(checks.values()) if checks else None,'failedValueChecks':','.join(k for k,v in checks.items() if not v),
            'expectedPassageRetrieved':hit,'retrievedUniqueDocuments':len({d['id'] for d in retrieval}),
            'wrongCropMetadataDocuments':len({d['id'] for d in retrieval if d['metadata']['cropName'] not in (case['crop'],'GENERAL')}),
            'noPartialRecord':result.get('noPartialRecord'),'seconds':result.get('seconds'),
            'observedProductNameDifference':bool(reference and result['recordResponse']['pestControl']['pesticideName']!=result['dbSnapshot']['record']['detail']['pesticideName']),
            'semanticReview':annotations.get(case['id'],{}).get('label','PENDING')})
    eligible=[r for r in rows if r['actualHttp']==200]
    retrieval_eligible=[r for r in rows if r['expectedPassageRetrieved'] is not None]
    comparable=[r for r in rows if r['valuesPreserved'] is not None]
    times=sorted(r['seconds'] for r in eligible if r['seconds'] is not None)
    summary={'run':args.run,'split':args.split,'planned':len(cases),'completed':len(rows),'missing':missing,'incomplete':incomplete,
       'httpExpected':{'passed':sum(r['httpMatchesExpected'] for r in rows),'denominator':len(rows)},
       'generation':{'ready':sum(r['generationStatus']=='READY' for r in eligible),'acceptedRequests':len(eligible),'statusCounts':dict(Counter(r['generationStatus'] for r in eligible))},
       'rejection':{'expectedRejected':sum(r['actualHttp']==r['expectedHttp']==400 for r in rows),'partialStoragePrevented':sum(r['noPartialRecord'] is True for r in rows)},
       'preservation':{'passed':sum(r['valuesPreserved'] for r in comparable),'denominator':len(comparable)},
       'retrieval':{'passageHits':sum(r['expectedPassageRetrieved'] for r in retrieval_eligible),'eligibleWithExpectedPassage':len(retrieval_eligible),'wrongCropMetadataDocuments':sum(r['wrongCropMetadataDocuments'] for r in rows)},
       'latencySeconds':{'measuredAcceptedRequests':len(times),'excludedResumedOrUnmeasured':len(eligible)-len(times),
           'medianAcceptedRequest':statistics.median(times) if times else None,'min':min(times) if times else None,'max':max(times) if times else None},
       'semanticReviewCounts':dict(Counter(r['semanticReview'] for r in rows)),
       'limits':['합성 상황/선택 문헌 범위. 전체 농업 정확도 아님.','READY는 형식 검증, 문헌 일치는 별도 검토.','다른 작물 문서 수는 메타데이터 기준. 본문 적용 조건은 별도 검토.','반복 실행은 독립 사례 수에 더하지 않음.']}
    (folder/'summary.json').write_text(json.dumps(summary,ensure_ascii=False,indent=2)+'\n')
    if rows:
        with (folder/'comparisons.csv').open('w',newline='',encoding='utf-8-sig') as file:
            writer=csv.DictWriter(file,fieldnames=rows[0].keys());writer.writeheader();writer.writerows(rows)
    print(json.dumps(summary,ensure_ascii=False,indent=2))
if __name__=='__main__':main()
