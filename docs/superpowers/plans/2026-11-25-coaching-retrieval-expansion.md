# 실제 검색 확대 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans. 사용자가 범위와 조건부 커밋·PR을 승인했다.

**Goal:** 혼합 문단과 자료 범위를 분리하여 6작물의 실제 근거 검색을 비교하고 재현한다.
**Architecture:** 기존 corpus 불변, 별도 버전 corpus와 격리 DB, 기존 제품 질의와 test 선택기, 불변 실행/입력 해시.
**Tech Stack:** Java21, SpringAI1.1.8, PostgreSQL16/pgvector, bge-m3, Python표준라이브러리, DockerCompose.
**Spec:** docs/superpowers/specs/2026-11-25-coaching-retrieval-expansion-design.md

## Global Constraints
- 제품src/main·기존DB/원문/모델/결과·API계약 변경없음. 생성호출/새다운로드/유료/배포/병합없음.
- 전용PG15434와보존bge만실행. 기본test는서비스를시작하지않음.
- 결과는검색평가이며답변정확도아님. 새96상황은합성·자체주석이며534작물대표아님.
- 커밋과개인origin DraftPR은검증/리뷰조건충족후에만. 태스크중간에는커밋대신ledger로추적한다.

## Review Focus
- 원문범위·조건해시가바뀌면재사용/적재를거부한다.
- 기존DB/다른포트/예상외스키마에는쓰기하지않는다.
- 수확부위UNKNOWN·품종미상·부정메모를확정사실로채우지않는다.
- 같은후보에서조건필터효과와corpus분할/확대효과를분리한다.
- 평가기대값을검색기/선택기에전달하지않고누락/미실행도분모에남긴다.

### Task 1: 자료와 사전 고정 평가
**Files:** docs/verification/coaching-retrieval-sources.json, api/src/test/resources/verification/coaching-retrieval-corpus.json, coaching-retrieval-cases.json; scripts/test-coaching-retrieval.py.
**Interfaces:** corpus는sourceId/parentId/원문선택offset,length/metadata/조건;cases는context,facts,expected,gaps,split. 기존모든파일은읽기만.
- [x] 기존30문단/혼합12를읽고공식추가자료확보·출처대조·분할범위고정.
- [x] 경로탈출·해시변경·중복ID·조건누락·범위오류거부검사를먼저실패시킨다.
- [x] 최소자료검사/prepare구현과Python검사통과. 새평가최대96상황의근거/금지/미확보를실행전에고정한다.

### Task 2: 격리 실행과 실제 검색기
**Files:** compose.coaching-retrieval.yml, scripts/coaching-retrieval.py; api/src/test/java/com/chamchamcham/lab/CoachingRetrievalLab.java, CoachingRetrievalLabTest.java; api/build.gradle.kts.
**Interfaces:** Java main은환경고정DB/로컬Ollama와준비된corpus/cases를읽고raw/result를새run에작성. Python이식별정보·입력해시잠금/summary를담당.
- [x] DB/URL/스키마가드,조건선별보존,분모집계,불변run검사를RED→GREEN.
- [x] PostgreSQL만기동하고기존bge식별값대조. 새평가DB에A(기존26)/B(같은자료분할)/C(추가자료확장)별테이블적재.
- [x] 제품QueryPlanner/실제PgVectorStore로후보수집,같은C후보에만선택기적용. 모델생성은호출불가.
- [x] 재적재중복/본문메타벡터보존확인. 자격증명은출력/추적하지않음.

### Task 3: 실행·분석·검사
**Files:** docs/verification/coaching-retrieval-results.md, coaching-retrieval-lab.md, coaching-retrieval-support.csv, coaching-retrieval-evaluation.json.
- [x] 입력/기대값해시고정후dev→새평가실행. 예외/무근거/조건부/필요근거손실을공개.
- [x] 쌍별실제검색과수동근거주석비교,원문재검토. 결과에맞춘기대값변경은하지않음.
- [x] 관련검사·독립리뷰·최종./gradlew test·git diff --check와변경범위를확인.
- [x] 자신이기동한서비스중지·데이터보존,continuation작성. 내부품질조건충족시역할별커밋·새브랜치push·dev대상DraftPR·CI확인;불충족시이유와미완료보존.

## 자체 점검
Task1의 버전·출처·조건을 Task2의 적재·검색에 연결하고 Task3은 동결된 동일 입력으로만 집계한다. 자동의미추출·전문가판정·답변품질수치추가는없다. 기존실험을독립새사례로합산하지않는다.

## 완료 근거
2026-11-25 실행·리뷰6Important보완·검사완료. source17검토/16적재, corpus26/63/82. 96회귀의선별25/36, 적용가능25/29, 지정부적합7→0, 빈53/96. 제품미채택. 상세는docs/verification/coaching-retrieval-results.md. 커밋/개인DraftPR/CI는최종조건확인후처리하며병합하지않는다.

Git: 5f36012/c0eb77a,개인origin DraftPR21→dev. CI상태확인은PR의Checks를따른다. 기존native서버유지/로드모델0,전용PG중지,볼륨보존.
