import importlib.util
from pathlib import Path
import unittest
spec = importlib.util.spec_from_file_location('corpus', Path(__file__).with_name('prepare-rag-corpus.py'))
corpus = importlib.util.module_from_spec(spec)
spec.loader.exec_module(corpus)

class CorpusTest(unittest.TestCase):
    def test_chunks_preserve_all_text_and_bounded_overlap(self):
        text = ['가' * 800, '나' * 700, '다' * 300]
        result = corpus.chunks(text)
        self.assertTrue(result, '문단이 누락되면 안 된다')
        self.assertTrue(all(len(row['text']) <= 1200 for row in result))
        self.assertEqual(result[0]['text'], text[0])
        self.assertEqual(result[1]['text'][:160], '가' * 160)
        self.assertEqual(result[-1]['paragraphEnd'], 3)
        for paragraph in text:
            self.assertIn(paragraph, '\n'.join(row['text'] for row in result))

    def test_oversized_paragraph_is_rejected_instead_of_silently_truncated(self):
        with self.assertRaises(ValueError):
            corpus.chunks(['가' * 1201])

if __name__ == '__main__':
    unittest.main()
