#!/usr/bin/env python3
"""공식 RDA 원문을 보존하고 사전 검토한 재배 문단을 로컬 검색 자료로 만든다."""
import argparse
from datetime import datetime, timezone
import hashlib
from html.parser import HTMLParser
import json
from pathlib import Path
import urllib.request
import uuid

SOURCES = [
    ('100000732652', '천궁·당귀 재배 전에 안전 재배지인지 확인하세요', '2017-03-23',
     ['천궁·당귀는 여름철', '- 저위 생산지는', '- 재배 적합지는', '농촌진흥청 약용작물과 김용일 농업연구사는']),
    ('100000802838', '고온 취약한 약용작물, 재배 관리 필수', '2025-05-27',
     ['약용작물 가운데', '천궁*은', '고온기에는', '* 증발산이란', '또한, 호흡작용이', '진딧물·응애류', '기온이 25~30도'])
]

class VisibleText(HTMLParser):
    def __init__(self):
        super().__init__()
        self.hidden = 0
        self.parts = []
    def handle_starttag(self, tag, attrs):
        if tag in ('script', 'style'):
            self.hidden += 1
    def handle_endtag(self, tag):
        if tag in ('script', 'style'):
            self.hidden -= 1
    def handle_data(self, data):
        if not self.hidden and data.strip():
            self.parts.append(' '.join(data.split()))

def chunks(paragraphs, limit=1200, overlap=160):
    result = []
    text = ''
    start = 1
    for number, paragraph in enumerate(paragraphs, 1):
        if len(paragraph) > limit - overlap:
            raise ValueError('한 문단이 너무 깁니다. 수동 문단 검토가 필요합니다.')
        candidate = text + ('\n' if text else '') + paragraph
        if len(candidate) > limit:
            result.append({'text': text, 'paragraphStart': start, 'paragraphEnd': number - 1})
            text = text[-overlap:] + '\n' + paragraph
            start = number  # 직전 조각 마지막 160자는 overlap으로 별도 표시한다.
        else:
            text = candidate
    if text:
        result.append({'text': text, 'paragraphStart': start, 'paragraphEnd': len(paragraphs)})
    return result

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    args.output.mkdir(parents=True, exist_ok=True)
    manifest, documents = [], []
    for source_id, title, published, prefixes in SOURCES:
        url = f'https://www.rda.go.kr/board/board.do?dataNo={source_id}&mode=view&prgId=day_farmprmninfoEntry'
        rawfile = args.output / (source_id + '.html')
        if not rawfile.exists():
            rawfile.write_bytes(urllib.request.urlopen(url, timeout=60).read())
        raw = rawfile.read_bytes()
        visible = VisibleText()
        visible.feed(raw.decode('utf-8'))
        paragraphs = []
        for prefix in prefixes:
            matches = [p for p in visible.parts if p.startswith(prefix)]
            if len(matches) != 1:
                raise ValueError(f'{source_id}: 문단 선택이 유일하지 않음: {prefix}: {len(matches)}')
            paragraphs.append(matches[0])
        derived = '\n'.join(paragraphs)
        (args.output / (source_id + '.agriculture.txt')).write_text(derived + '\n')
        sha = hashlib.sha256(raw).hexdigest()
        selected_sha = hashlib.sha256(derived.encode()).hexdigest()
        manifest.append({'sourceId': source_id, 'title': title, 'url': url, 'published': published,
                         'retrievedAt': datetime.fromtimestamp(rawfile.stat().st_mtime, timezone.utc).isoformat(),
                         'rawSha256': sha, 'selectedSha256': selected_sha, 'selectedParagraphs': paragraphs,
                         'exclusions': '탐색 메뉴, 연락처, 인체 효능 각주, 당시 기상 전망·웹서비스 현황. 위 명시 문단 외 모두 제외.',
                         'license': '원문 공공누리 표시 및 농촌진흥청 출처 유지. 웹 문서이므로 page 없음.'})
        for i, chunk in enumerate(chunks(paragraphs)):
            ident = str(uuid.uuid5(uuid.NAMESPACE_URL, url + ':' + selected_sha + ':' + str(i)))
            documents.append({'id': ident, 'text': chunk['text'], 'metadata': {
                'sourceType': 'TECH_DOCUMENT', 'cropName': 'GENERAL', 'documentTitle': title,
                'sourceUrl': url, 'sourceId': source_id, 'published': published,
                'rawSha256': sha, 'selectedSha256': selected_sha,
                'paragraphStart': chunk['paragraphStart'], 'paragraphEnd': chunk['paragraphEnd'],
                'chunkIndex': i, 'overlapChars': 0 if i == 0 else 160}})
    (args.output / 'manifest.json').write_text(json.dumps(manifest, ensure_ascii=False, indent=2) + '\n')
    (args.output / 'chunks.json').write_text(json.dumps(documents, ensure_ascii=False, indent=2) + '\n')
    print(f'문서 {len(manifest)}개, 조각 {len(documents)}개, 원문과 선택 문단 해시 기록 완료')

if __name__ == '__main__':
    main()
