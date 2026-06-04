import json
import unittest

from python_tasks.run_task import TaskExecutionError, execute


class PythonTaskExecutionTest(unittest.TestCase):
    def test_python_echo_returns_message(self):
        self.assertEqual("hello", execute("PYTHON_ECHO", {"message": "hello"}))

    def test_python_echo_requires_text_message(self):
        with self.assertRaisesRegex(
            TaskExecutionError,
            "PYTHON_ECHO task requires input_data.message as text",
        ):
            execute("PYTHON_ECHO", {"message": 42})

    def test_text_stats_counts_characters_words_and_lines(self):
        result = json.loads(execute("TEXT_STATS", {"text": "One line.\nSecond line."}))

        self.assertEqual(
            {"character_count": 22, "word_count": 4, "line_count": 2},
            result,
        )

    def test_text_stats_handles_empty_text(self):
        result = json.loads(execute("TEXT_STATS", {"text": ""}))

        self.assertEqual(
            {"character_count": 0, "word_count": 0, "line_count": 0},
            result,
        )

    def test_keyword_count_is_case_insensitive_and_matches_whole_words(self):
        result = json.loads(
            execute(
                "KEYWORD_COUNT",
                {"text": "Agent agents agentic AGENT", "keyword": "agent"},
            )
        )

        self.assertEqual({"keyword": "agent", "count": 2}, result)

    def test_keyword_count_requires_non_empty_keyword(self):
        with self.assertRaisesRegex(
            TaskExecutionError,
            "KEYWORD_COUNT task requires input_data.keyword as non-empty text",
        ):
            execute("KEYWORD_COUNT", {"text": "hello", "keyword": ""})

    def test_unsupported_task_type_is_rejected(self):
        with self.assertRaisesRegex(
            TaskExecutionError,
            "Unsupported Python task type: UNKNOWN_TASK",
        ):
            execute("UNKNOWN_TASK", {})


if __name__ == "__main__":
    unittest.main()
