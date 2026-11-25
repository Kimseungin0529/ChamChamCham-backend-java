#!/usr/bin/env python3
"""평가와 분리하여 실제 인증·소유권·수정 경합·실패 재생성을 확인한다."""
import fcntl
import importlib.util
import json
from pathlib import Path
import sys
import time
import urllib.error
sys.dont_write_bytecode=True
spec=importlib.util.spec_from_file_location('evaluation',Path(__file__).with_name('evaluate-rag-api.py'))
eval=importlib.util.module_from_spec(spec);spec.loader.exec_module(eval)
lab=eval.lab
BASE=eval.BASE


def denied(method,url,token,expected,body=None):
    try:lab.request(url,body,token,method)
    except urllib.error.HTTPError as error:
        assert error.code==expected,(method,url,error.code,expected)
        return {'method':method,'expectedHttp':expected,'actualHttp':error.code,'error':json.loads(error.read())}
    raise AssertionError('거부해야 하는 요청이 성공했습니다')


def wait(record,token,revision=None):
    started=time.monotonic();states=[]
    while time.monotonic()-started<900:
        result=lab.request(BASE+'/'+record+'/feedback',token=token)['data']
        if not states or states[-1]!=result['status']:states.append(result['status'])
        if revision is not None:assert result['sourceRevision']==revision
        if result['status'] in ('READY','FAILED','STALE'):return result,states
        time.sleep(1)
    raise AssertionError('코칭 완료 대기 900초 만료')


def main():
    lab.guard();folder=lab.STATE/'lifecycle';folder.mkdir(exist_ok=True)
    with (lab.STATE/'command.lock').open('w') as lock:
        fcntl.flock(lock,fcntl.LOCK_EX|fcntl.LOCK_NB)
        path=folder/'result.json'
        if path.exists():raise RuntimeError('기존 라이프사이클 결과가 있습니다. 중복 생성하지 않고 보존합니다')
        journal=folder/'journal.json'
        if journal.exists():raise RuntimeError('이전 실행 중단 기록이 있습니다. 생성된 ID부터 확인하세요')
        token=lab.login();other=lab.login(True)
        case=next(c for c in json.loads(eval.CASES.read_text())['cases'] if c['id']=='dev-1-watering-0')
        fixture=json.loads((lab.STATE/'fixtures.json').read_text());payload=eval.payload(case,fixture)
        result={'stages':[],'synthetic':True}
        journal.write_text(json.dumps({'phase':'before-create'},indent=2))
        record=lab.request(BASE,payload,token)['data']['id']
        journal.write_text(json.dumps({'recordId':record,'phase':'created'},indent=2))
        first,states=wait(record,token,1);assert first['status']=='READY',first
        result['recordId']=record;result['initial']={'response':first,'states':states}
        url=BASE+'/'+record
        result['authorization']=[denied('GET',url,None,401),denied('GET',url,'invalid-token',401),
            denied('GET',url,other,403),denied('GET',url+'/feedback',other,404),
            denied('PATCH',url,other,403,payload),denied('POST',url+'/feedback/regenerate',other,404,{})]
        result['readyRegenerationDenied']=denied('POST',url+'/feedback/regenerate',token,409,{})
        # 첫 수정의 준비가 끝난 뒤 다시 수정해, 이전 결과가 최신 revision을 덮어쓰지 못하는지 확인한다.
        payload['memo']='첫 수정에서 흙이 마르는 부분을 살피고 적은 양의 물을 주었습니다. 이 수정은 뒤의 최신 기록으로 대체됩니다.'
        payload['watering']={'irrigationAmount':'LOW','irrigationMethod':'DRIP'}
        lab.request(url,payload,token,'PATCH')
        until=time.monotonic()+60
        while True:
            prepared=lab.request(url+'/feedback',token=token)['data']
            if prepared['inputPrepared'] or prepared['status']!='PENDING':break
            if time.monotonic()>until:raise AssertionError('수정 문맥 준비 대기 만료')
            time.sleep(.1)
        payload['memo']='최신 수정에서는 천궁 밭의 흙 상태와 물길을 확인했습니다. 물 준 양과 방법은 더 이상 상세로 남기지 않은 합성 기록입니다.'
        payload.pop('watering')
        lab.request(url,payload,token,'PATCH')
        latest,states=wait(record,token,3);assert latest['status']=='READY',latest
        # 큐에 늦게 도착한 이전 작업까지 끝나도 최신 답변이 유지되는지 확인한다.
        history=lab.sql("select source_revision || ':' || status from record_feedback where record_id='"+record+"' order by source_revision").splitlines()
        assert history==['1:STALE','2:STALE','3:READY'],history
        response=lab.request(url,token=token)['data']
        snapshot=json.loads(lab.sql("select input_snapshot::text from record_feedback where record_id='"+record+"' and source_revision=3"))
        checks=eval.preservation_checks(payload,response,snapshot)
        assert all(checks.values()),checks
        result['updated']={'response':latest,'states':states,'history':history,'preservationChecks':checks,'snapshot':snapshot}
        journal.write_text(json.dumps({'recordId':record,'phase':'updated'},indent=2))
        # 과거 실제 CPU 타임아웃으로 FAILED가 된 동일 합성 기록을 기존 재생성 API로 복구한다.
        failed=json.loads((lab.STATE/'evaluations/cpu-pilot/dev-1-watering-0.json').read_text())['recordId']
        before=lab.request(BASE+'/'+failed+'/feedback',token=token)['data']
        assert before['status']=='FAILED',before
        queued=lab.request(BASE+'/'+failed+'/feedback/regenerate',{},token)['data']
        assert queued['status']=='PENDING' and queued['sourceRevision']==before['sourceRevision']
        regenerated,states=wait(failed,token,before['sourceRevision']);assert regenerated['status']=='READY',regenerated
        assert regenerated['feedbackId']==before['feedbackId']
        result['regenerated']={'recordId':failed,'before':before,'queued':queued,'after':regenerated,'states':states}
        path.write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n')
        journal.write_text(json.dumps({'recordId':record,'phase':'complete'},indent=2))
        print('실제 인증·소유권6종·READY 재생성 거부·수정 revision 경합·상세 제거·FAILED 재생성 통과')

if __name__=='__main__':main()
