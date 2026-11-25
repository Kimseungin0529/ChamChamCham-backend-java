import importlib.util
from pathlib import Path
import sys
import unittest
import tempfile
from unittest.mock import patch
import urllib.error
import io
import json
import time
sys.dont_write_bytecode = True
s=importlib.util.spec_from_file_location('evaluation',Path(__file__).with_name('evaluate-rag-api.py'))
m=importlib.util.module_from_spec(s);s.loader.exec_module(m)
class PayloadTest(unittest.TestCase):
    def test_successful_creation_remains_resumable_when_status_lookup_fails(self):
        case={'id':'lookup-failure','crop':'천궁','workType':'WATERING','split':'dev','memo':'합성 기록',
              'detailKey':'watering','detail':None,'expectedSourcesAny':[],'expectedChunkIdsAny':[],'expectedHttp':200}
        fixture={'farmId':'farm','crops':{'천궁':'crop'}}
        with tempfile.TemporaryDirectory() as directory:
            folder=Path(directory);(folder/'traces').mkdir()
            with patch.object(m.lab,'STATE',folder), patch.object(m.lab,'sql',return_value='0'), \
                 patch.object(m.lab,'request',return_value={'data':{'id':'created-record'}}), \
                 patch.object(m,'poll',side_effect=urllib.error.HTTPError('http://localhost/status',503,'unavailable',{},io.BytesIO(b'{}'))), \
                 patch.object(m,'finalize'):
                with self.assertRaises(urllib.error.HTTPError):m.execute(case,folder,fixture,'test-token')
            import json
            journal=json.loads((folder/'lookup-failure.json').read_text())
            self.assertEqual(200,journal['createHttp'])
            self.assertEqual('created-record',journal['recordId'])
            self.assertFalse(journal['terminal'])

    def test_resumed_observation_is_excluded_from_whole_request_latency(self):
        row={'resumed':True,'crop':'천궁','expectedSourcesAny':[],'expectedChunkIdsAny':[],
             'createHttp':200,'expectedHttp':200}
        with tempfile.TemporaryDirectory() as directory:
            folder=Path(directory);(folder/'traces').mkdir()
            with patch.object(m.lab,'STATE',folder):
                m.finalize(row,time.monotonic()-2,set(),folder/'result.json')
            self.assertIsNone(row['seconds'])
            self.assertGreaterEqual(row['resumeObservationSeconds'],2)

    def test_text_input_uses_the_existing_manual_entry_mode(self):
        case={'crop':'천궁','workType':'WATERING','memo':'합성 검증 기록을 충분한 길이로 남기고 현재 상태를 확인하려고 합니다.','detailKey':'watering','detail':None}
        fixture={'farmId':'a','crops':{'천궁':'b'}}
        self.assertEqual('MANUAL',m.payload(case,fixture)['entryMode'])

    def test_pesticide_mass_and_final_spray_volume_are_compared_separately(self):
        request={'farmId':'f','cropId':'c','workType':'PEST_CONTROL','workedAt':'now','weatherCondition':'맑음','weatherTemperature':32,'memo':'합성',
                 'pestControl':{'pesticideAmount':2.5,'pesticideAmountUnit':'G','totalSprayAmount':1500,'totalSprayAmountUnit':'ML'}}
        response={**request,'pestControl':{**request['pestControl'],'pesticideName':'합성 제품','pestName':None}}
        snapshot={'crop':{'cropId':'c'},'record':{'memo':'합성','workType':'PEST_CONTROL','detail':{**response['pestControl'],'pesticideAmountUnit':'ML'}}}
        result=m.preservation_checks(request,response,snapshot)
        self.assertTrue(result['detailValues'])
        self.assertFalse(result.get('snapshotDetailValues',True))

    def test_unrecorded_watering_does_not_gain_an_amount_in_the_snapshot(self):
        request={'farmId':'f','cropId':'c','workType':'WATERING','workedAt':'now','weatherCondition':'맑음','weatherTemperature':32,'memo':'합성'}
        response={**request,'watering':None}
        snapshot={'crop':{'cropId':'c'},'record':{'memo':'합성','workType':'WATERING','detail':{'type':'WATERING','irrigationAmount':'LOW','irrigationMethod':None}}}
        self.assertFalse(m.preservation_checks(request,response,snapshot).get('optionalDetailNotInvented',True))

    def test_unknown_harvest_is_not_zero_in_response_or_ai_snapshot(self):
        detail={'harvestAmount':None,'harvestAmountUnknown':True,'medicinalPart':'LEAF','growthPeriod':3,'isLastHarvest':False,'harvestSource':'CULTIVATED'}
        request={'farmId':'f','cropId':'c','workType':'HARVEST','workedAt':'now','weatherCondition':'맑음','weatherTemperature':32,'memo':'합성','harvest':detail}
        response={**request,'harvest':{**detail,'amountUnknown':True}}
        snapshot={'crop':{'cropId':'c'},'record':{'memo':'합성','workType':'HARVEST','detail':{'harvestAmount':0,'amountUnknown':False,'medicinalPart':'LEAF','growthPeriod':3,'growthPeriodUnit':'MONTH','isLastHarvest':False,'harvestSource':'CULTIVATED'}}}
        self.assertFalse(m.preservation_checks(request,response,snapshot).get('snapshotHarvestUnknown',True))
if __name__=='__main__':unittest.main()
