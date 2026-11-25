#!/usr/bin/env python3
"""사전 정의한 합성 상황을 실제 로컬 API로 순차 실행하고 원시 결과를 보존한다."""
import argparse
from datetime import datetime
import fcntl
import hashlib
import importlib.util
import json
from pathlib import Path
import sys
import time
import urllib.error
sys.dont_write_bytecode = True
spec = importlib.util.spec_from_file_location('lab', Path(__file__).with_name('rag-api.py'))
lab = importlib.util.module_from_spec(spec); spec.loader.exec_module(lab)
CASES = lab.ROOT / 'api/src/test/resources/verification/rag-api-evaluation-cases.json'
BASE = 'http://127.0.0.1:18080/api/v1/farming-records'


def payload(case, fixture):
    value = {'farmId': fixture['farmId'], 'cropId': fixture['crops'][case['crop']], 'workType': case['workType'],
             'workedAt': datetime.now().isoformat(timespec='seconds'), 'weatherCondition': '맑음', 'weatherTemperature': 32,
             'memo': case['memo'], 'entryMode': 'MANUAL', 'mediaIds': []}
    if case['detailKey'] and case['detail'] is not None:
        value[case['detailKey']] = json.loads(json.dumps(case['detail']).replace('$PESTICIDE_ID', fixture['pesticideId']).replace('$PEST_ID', fixture['pestId']))
    return value


def execute(case, folder, fixture, token):
    path = folder / (case['id'] + '.json')
    started = time.monotonic()
    before = set((lab.STATE / 'traces').glob('*.json'))
    value = payload(case, fixture)
    row = {'caseId': case['id'], 'crop': case['crop'], 'workType': case['workType'], 'split': case['split'],
           'startedAt': datetime.now().isoformat(), 'request': value, 'expectedSourcesAny': case['expectedSourcesAny'],
           'expectedChunkIdsAny': case['expectedChunkIdsAny'], 'expectedHttp': case['expectedHttp'],
           'dbRecordCountBefore': int(lab.sql('select count(*) from farming_record')),
           'semanticReview': 'PENDING', 'terminal': False, 'traceBefore': [p.name for p in before]}
    path.write_text(json.dumps(row, ensure_ascii=False, indent=2))
    try:
        response = lab.request(BASE, value, token)
    except urllib.error.HTTPError as error:
        row.update({'createHttp': error.code, 'error': json.loads(error.read()), 'terminal': True})
        row['dbRecordCountAfter'] = int(lab.sql('select count(*) from farming_record'))
        row['noPartialRecord'] = row['dbRecordCountBefore'] == row['dbRecordCountAfter']
        finalize(row, started, before, path)
        return row
    row.update({'createHttp': 200, 'recordId': response['data']['id']})
    path.write_text(json.dumps(row, ensure_ascii=False, indent=2))  # 조회 실패도 같은 기록부터 재개한다.
    poll(row, token, started)
    finalize(row, started, before, path)
    return row


def poll(row, token, started):
    states = []
    while time.monotonic() - started < 900:
        status = lab.request(BASE + '/' + row['recordId'] + '/feedback', token=token)['data']
        if not states or states[-1] != status['status']: states.append(status['status'])
        row['status'] = status
        if status['status'] in ('READY', 'FAILED', 'STALE'):
            row['terminal'] = True
            break
        time.sleep(2)
    row['observedStates'] = states
    row['recordResponse'] = lab.request(BASE + '/' + row['recordId'], token=token)['data']
    snapshot=lab.sql("select input_snapshot::text from record_feedback where record_id='" + row['recordId'] + "'")
    row['dbSnapshot'] = json.loads(snapshot) if snapshot else None
    if row['request'].get('pestControl'):
        product=row['request']['pestControl']['pesticideId']
        row['pesticideCatalog']=json.loads(lab.sql("select json_build_object('id',id,'itemName',item_name,'brandName',brand_name)::text from pesticide where id='"+product+"'"))


def finalize(row, started, before, path):
    elapsed = round(time.monotonic() - started, 3)
    row['seconds'] = None if row.get('resumed') else elapsed
    if row.get('resumed'): row['resumeObservationSeconds'] = elapsed
    files = sorted(set((lab.STATE / 'traces').glob('*.json')) - before)
    row['traceFiles'] = [str(p.relative_to(lab.ROOT)) for p in files]
    retrieved = [doc for p in files if '-retrieval-' in p.name for doc in json.loads(p.read_text())['documents']]
    row['retrievedSources'] = sorted({d['metadata']['sourceId'] for d in retrieved})
    row['expectedEvidenceRetrieved'] = (bool(set(row['expectedSourcesAny']) & set(row['retrievedSources'])) if row['expectedSourcesAny'] else None)
    row['expectedPassageRetrieved'] = (bool(set(row.get('expectedChunkIdsAny',[])) & {d['id'] for d in retrieved}) if row.get('expectedChunkIdsAny') else None)
    row['wrongCropDocuments'] = sorted({d['metadata']['cropName'] for d in retrieved if d['metadata']['cropName'] not in (row['crop'], 'GENERAL')})
    row['httpMatchesExpected'] = row.get('createHttp') == row.get('expectedHttp')
    if row.get('dbSnapshot'):
        row['preservationChecks']=preservation_checks(row['request'],row['recordResponse'],row['dbSnapshot'],row.get('pesticideCatalog'))
    path.write_text(json.dumps(row, ensure_ascii=False, indent=2) + '\n')

def preservation_checks(submitted, stored, snapshot, pesticide_catalog=None):
    checks={key:submitted[key]==stored[key] for key in ('farmId','cropId','workType','workedAt','weatherCondition','weatherTemperature','memo')}
    checks.update({'snapshotMemo':snapshot['record']['memo']==submitted['memo'],
                   'snapshotCrop':snapshot['crop']['cropId']==submitted['cropId'],
                   'snapshotWork':snapshot['record']['workType']==submitted['workType']})
    key=next((k for k in ('planting','watering','fertilizing','pestControl','weeding','harvest') if k in submitted),None)
    detail=snapshot['record']['detail']
    if key and key!='harvest':
        checks['detailValues']=all(stored[key].get(k)==v for k,v in submitted[key].items())
        checks['snapshotDetailValues']=all(detail.get(k)==v for k,v in submitted[key].items() if k not in ('pesticideId','pestId'))
        if key=='pestControl':
            if pesticide_catalog:
                checks['responseProductItemName']=stored[key]['pesticideName']==pesticide_catalog['itemName']
                checks['snapshotProductBrandName']=detail['pesticideName']==pesticide_catalog['brandName']
            checks['snapshotPestName']=detail['pestName']==stored[key].get('pestName')
    if key=='harvest':
        checks['harvestUnknown']=stored[key]['amountUnknown']==submitted[key]['harvestAmountUnknown']
        checks['harvestAmount']=stored[key]['harvestAmount']==submitted[key]['harvestAmount']
        checks['harvestPart']=stored[key]['medicinalPart']==submitted[key]['medicinalPart']
        checks['snapshotHarvestUnknown']=detail['amountUnknown']==submitted[key]['harvestAmountUnknown'] and detail['harvestAmount']==submitted[key]['harvestAmount']
        checks['snapshotHarvestPart']=detail['medicinalPart']==submitted[key]['medicinalPart']
        checks['harvestOtherFields']=all(stored[key][k]==submitted[key][k] and detail[k]==submitted[key][k] for k in ('harvestSource','growthPeriod','isLastHarvest'))
        checks['growthPeriodUnit']=detail['growthPeriodUnit']=='MONTH'
    if submitted['workType'] in ('WATERING','WEEDING') and key is None:
        omitted='watering' if submitted['workType']=='WATERING' else 'weeding'
        fields=('irrigationAmount','irrigationMethod') if omitted=='watering' else ('weedingMethod',)
        checks['optionalDetailNotInvented']=stored[omitted] is None and all(detail.get(k) is None for k in fields)
    return checks


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--split', choices=['dev', 'evaluation'], default='dev')
    parser.add_argument('--case')
    parser.add_argument('--run')
    parser.add_argument('--export', action='store_true', help='수정 가능한 요청 JSON 준비. 기존 파일은 보존한다')
    parser.add_argument('--limit', type=int)
    args = parser.parse_args()
    if args.export:
        fixture=json.loads((lab.STATE/'fixtures.json').read_text())
        folder=lab.STATE/'requests';folder.mkdir(exist_ok=True)
        for case in json.loads(CASES.read_text())['cases']:
            path=folder/(case['id']+'.json')
            if not path.exists():path.write_text(json.dumps(payload(case,fixture),ensure_ascii=False,indent=2)+'\n')
        print('요청 JSON 준비: .local/rag-api/requests/ (기존 수정 파일 보존)');return
    if not args.run:parser.error('--run 또는 --export가 필요합니다')
    if not args.run.replace('-', '').replace('_', '').isalnum(): raise ValueError('결과 폴더 이름이 올바르지 않습니다')
    lab.guard()
    folder = lab.STATE / 'evaluations' / args.run; folder.mkdir(parents=True, exist_ok=True)
    fingerprint = hashlib.sha256(CASES.read_bytes()).hexdigest()
    identity = folder / 'cases.sha256'
    if identity.exists() and identity.read_text() != fingerprint: raise RuntimeError('사례가 바뀌었습니다. 기존 결과를 덮어쓰지 않습니다')
    identity.write_text(fingerprint)
    actual=lab.STATE/'runtime-options.json'
    if actual.exists():
        options=folder/'runtime-options.json'
        current=json.loads(actual.read_text())
        current['corpusChunksSha256']=hashlib.sha256((lab.STATE/'corpus/chunks.json').read_bytes()).hexdigest()
        if options.exists() and json.loads(options.read_text())!=current:
            raise RuntimeError('실행 모델/프롬프트/자료가 다릅니다. 기존 결과를 섞지 않고 새 --run을 사용하세요')
        options.write_text(json.dumps(current,ensure_ascii=False,indent=2)+'\n')
    with (lab.STATE / 'command.lock').open('w') as lock:
        fcntl.flock(lock, fcntl.LOCK_EX | fcntl.LOCK_NB)
        token = lab.login();fixture=json.loads((lab.STATE/'fixtures.json').read_text())
        cases = [c for c in json.loads(CASES.read_text())['cases'] if c['split']==args.split and (not args.case or c['id']==args.case)]
        if args.limit: cases = cases[:args.limit]
        for case in cases:
            path = folder / (case['id'] + '.json')
            if path.exists():
                row = json.loads(path.read_text())
                if row['terminal']:
                    print(case['id'], '기존 결과 보존', flush=True);continue
                if row.get('recordId'):
                    row['resumed']=True
                    started=time.monotonic();before={lab.STATE/'traces'/name for name in row.get('traceBefore',[])}
                    poll(row,token,started);finalize(row,started,before,path)
                    print(case['id'], '기존 생성 기록 조회 재개', row.get('status',{}).get('status'), flush=True);continue
                raise RuntimeError('기존 생성 요청의 완료 여부를 먼저 확인하세요')
            row = execute(case, folder, fixture, token)
            print(case['id'], row.get('createHttp'), row.get('status',{}).get('status'), row.get('status',{}).get('failureCode'), row['seconds'], flush=True)

if __name__ == '__main__':
    try: main()
    except lab.subprocess.CalledProcessError as error:
        print('전용 DB/도구 실행 실패 반환 코드:',error.returncode,file=sys.stderr);sys.exit(1)
