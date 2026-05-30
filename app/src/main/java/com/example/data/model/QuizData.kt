package com.example.data.model

data class Question(
    val id: Int,
    val type: String, // "listening", "speaking", "reading", "writing"
    val questionPrompt: String, // Prompt or reading instruction
    val audioSpeakerText: String = "", // Text to speak via TTS if a listening question
    val options: List<String> = emptyList(), // For choice selection
    val correctIndex: Int = -1, // Valid if multiple choice
    val correctText: String = "", // Correct typed text or key
    val strokeReference: String = "", // Chinese/Japanese Kanji stroke target Character e.g. "学"
    val explanation: String = "" // Grammar or vocabulary tips
)

data class CertificateMockExam(
    val languageCode: String,
    val certName: String, // e.g., "HSK 1", "JLPT N5", "TOEIC", "TOPIK I"
    val durationMinutes: Int = 20,
    val description: String,
    val questions: List<Question>
)

object QuizDataProvider {

    fun getMockExam(languageCode: String, level: String): CertificateMockExam {
        val questions = when (languageCode) {
            "ja" -> getJapaneseQuestions(level)
            "zh" -> getChineseQuestions(level)
            "ko" -> getKoreanQuestions(level)
            else -> getEnglishQuestions(level)
        }
        return CertificateMockExam(
            languageCode = languageCode,
            certName = "$level Exam Base",
            durationMinutes = 15,
            description = "Đề thi thử tiêu chuẩn cấu trúc rút gọn mô phỏng kỳ thi chính thức $level.",
            questions = questions
        )
    }

    fun getQuestionsBySkill(languageCode: String, skill: String, level: String): List<Question> {
        val allQs = when (languageCode) {
            "ja" -> getJapaneseQuestions(level)
            "zh" -> getChineseQuestions(level)
            "ko" -> getKoreanQuestions(level)
            else -> getEnglishQuestions(level)
        }
        val skillFiltered = allQs.filter { it.type.lowercase() == skill.lowercase() }
        if (skillFiltered.isNotEmpty()) return skillFiltered

        // Fallback or generator if empty
        return when (skill.lowercase()) {
            "listening" -> listOf(
                Question(
                    id = 101,
                    type = "listening",
                    questionPrompt = "Nghe và chọn nghĩa đúng của câu:",
                    audioSpeakerText = if (languageCode == "ja") "お元気ですか。" else if (languageCode == "zh") "最近怎么样？" else if (languageCode == "ko") "어디에 가요?" else "Where are you going?",
                    options = listOf("Bạn khỏe không?", "Hẹn gặp lại", "Chúc ngủ ngon", "Xin cảm ơn"),
                    correctIndex = 0,
                    explanation = "Câu hỏi giao tiếp thông thường hỏi về sức khỏe."
                )
            )
            "speaking" -> listOf(
                Question(
                    id = 102,
                    type = "speaking",
                    questionPrompt = "Hãy nhấn biểu tượng Micro để đọc to câu dưới đây:",
                    correctText = if (languageCode == "ja") "おやすみなさい" else if (languageCode == "zh") "今天天气很好" else if (languageCode == "ko") "한국어 아주 재미있어요" else "Artificial intelligence is changing language study.",
                    explanation = "Đọc chuẩn âm điệu và ngữ điệu để AI chấm điểm."
                )
            )
            "reading" -> listOf(
                Question(
                    id = 103,
                    type = "reading",
                    questionPrompt = "Chọn từ thích hợp điền vào khoảng trống:",
                    options = if (languageCode == "ja") listOf("は", "が", "を", "に") else listOf("的", "得", "地", "和"),
                    correctIndex = 0,
                    explanation = "Sử dụng trợ từ hoặc phó từ ngữ pháp cơ bản."
                )
            )
            else -> listOf(
                Question(
                    id = 104,
                    type = "writing",
                    questionPrompt = "Luyện viết nét ký tự chữ sau đây lên bảng vẽ:",
                    strokeReference = if (languageCode == "ja") "水" else if (languageCode == "zh") "学" else if (languageCode == "ko") "한" else "Pen",
                    correctText = if (languageCode == "ja") "Mizu" else if (languageCode == "zh") "Xue" else if (languageCode == "ko") "Han" else "Pen",
                    explanation = "Vẽ đúng tỉ lệ nét đứt và thứ tự trái qua phải."
                )
            )
        }
    }

    private fun getEnglishQuestions(level: String): List<Question> {
        return listOf(
            Question(
                id = 1,
                type = "listening",
                questionPrompt = "Nghe câu sau và chọn đáp án chính xác nhất mô tả tình huống:",
                audioSpeakerText = "The quarterly financial report was published on the company website yesterday.",
                options = listOf(
                    "Báo cáo tài chính quý được đăng lên trang web vào hôm qua.",
                    "Báo cáo tuần đã chuẩn bị xong.",
                    "Họ đang họp để viết báo cáo trực tuyến.",
                    "Báo cáo bị hoãn lại vì sai số liệu."
                ),
                correctIndex = 0,
                explanation = "Quarterly financial report có nghĩa là báo cáo tài chính quý."
            ),
            Question(
                id = 2,
                type = "reading",
                questionPrompt = "Choose the word that best fits the blank: 'The manager decided to ______ the meeting until next Monday due to scheduling conflicts.'",
                options = listOf("cancel", "postpone", "convene", "accelerate"),
                correctIndex = 1,
                explanation = "Postpone có nghĩa là hoãn lại, phù hợp ngữ cảnh hoãn buổi họp đến thứ Hai tới."
            ),
            Question(
                id = 3,
                type = "speaking",
                questionPrompt = "Nhấn micro và đọc to để luyện phát âm từ nối chuẩn bản xứ:",
                correctText = "I would appreciate your feedback on the contract draft.",
                explanation = "Lưu ý nối âm 'appreciate your' khẽ nhịp rung vòm họng."
            ),
            Question(
                id = 4,
                type = "writing",
                questionPrompt = "Dịch câu sau sang tiếng Anh: 'Tôi mong đợi được làm việc cùng dự án với bạn.'",
                correctText = "I look forward to working with you on this project",
                options = listOf(),
                correctIndex = -1,
                explanation = "Cấu trúc: 'look forward to + V-ing' nghĩa là mong đợi làm điều gì đó."
            )
        )
    }

    private fun getJapaneseQuestions(level: String): List<Question> {
        return listOf(
            Question(
                id = 1,
                type = "listening",
                questionPrompt = "Nghe âm thanh tiếng Nhật và chọn ý nghĩa tương thích:",
                audioSpeakerText = "日本の食べ物の中では何が一番好きですか。",
                options = listOf(
                    "Bạn thích ăn món ăn Nhật nào nhất?",
                    "Khi nào bạn sẽ đi Nhật ăn uống?",
                    "Người Nhật có thích thức ăn nước ta không?",
                    "Bạn mua đồ ăn Nhật Bản ở siêu thị nào?"
                ),
                correctIndex = 0,
                explanation = "「日本の食べ物の中では」 trong các món Nhật, 「何が一番好きですか」 thích cái gì nhất."
            ),
            Question(
                id = 2,
                type = "reading",
                questionPrompt = "Điền trợ từ thích hợp vào chỗ trống: 私______昨日デパートへ行って、新しい靴を買いました。",
                options = listOf("は", "の", "が", "に"),
                correctIndex = 0,
                explanation = "Trợ từ は biểu thị chủ ngữ đề tài chính của câu 'Tôi thì hôm qua...'"
            ),
            Question(
                id = 3,
                type = "writing",
                questionPrompt = "Tập viết chữ KANJI biểu tượng cho từ 'MƯA' (Ame/Rain). Di chuyển ngón tay đồ nét vẽ:",
                strokeReference = "雨",
                correctText = "あめ",
                explanation = "Chữ Vũ (雨) mô phỏng bầu trời có các hạt mưa rơi xuống."
            ),
            Question(
                id = 4,
                type = "speaking",
                questionPrompt = "Nhấn nút và nói câu tiếng Nhật xã giao dưới đây:",
                correctText = "はじめまして、宜しくお願いします",
                explanation = "Hajimemashite, yoroshiku onegai shimasu. Đọc âm 'shite' và 'shimasu' mượt hơi gió."
            )
        )
    }

    private fun getChineseQuestions(level: String): List<Question> {
        return listOf(
            Question(
                id = 1,
                type = "listening",
                questionPrompt = "Nghe câu hỏi tiếng Trung và chọn phản hồi phù hợp nhất:",
                audioSpeakerText = "你今天中午想去哪里吃午饭？",
                options = listOf(
                    "我想去学校附近的川菜馆。",
                    "我昨天已经吃过面条了。",
                    "我的汉语老师会写汉字。",
                    "明天上午我们要去买东西。"
                ),
                correctIndex = 0,
                explanation = "Câu hỏi hỏi trưa nay muốn đi đâu ăn cơm trưa. Trả lời muốn đi quán Tứ Xuyên gần trường là đúng trọng tâm nhất."
            ),
            Question(
                id = 2,
                type = "reading",
                questionPrompt = "Chọn chữ điền vào chỗ trống: 他跳舞跳得非______好。",
                options = listOf("常", "唱", "长", "场"),
                correctIndex = 0,
                explanation = "非常 (fēicháng) nghĩa là vô cùng, rất. Cụm từ 非常好 là vô cùng tốt."
            ),
            Question(
                id = 3,
                type = "writing",
                questionPrompt = "Tập vẽ nét chữ Hán tự 'XUÂN' (Chūn / Mùa xuân). Di nét ngón tay theo hình:",
                strokeReference = "春",
                correctText = "Chun",
                explanation = "Chữ Xuân gồm bộ Tam đè lên bộ Nhân và bộ Nhật ở đáy phía dưới."
            ),
            Question(
                id = 4,
                type = "speaking",
                questionPrompt = "Nhấn micro để phát âm chuẩn câu chào khách khí:",
                correctText = "认识你我也很高兴",
                explanation = "Rèn luyện chuyển âm nhẹ giữa 'shi' và cách giữ hơi mũi chữ 'xing'."
            )
        )
    }

    private fun getKoreanQuestions(level: String): List<Question> {
        return listOf(
            Question(
                id = 1,
                type = "listening",
                questionPrompt = "Nghe hội thoại Hàn ngữ và chọn câu trả lời thích đáng:",
                audioSpeakerText = "주말에 시간이 있으면 명동에 같이 쇼핑하러 가요.",
                options = listOf(
                    "좋아요, 일요일 오후 2시에 지하철역에서 만나요.",
                    "한국 김치는 정말 매워요.",
                    "저는 서울 대학교 학생입니다.",
                    "어제 늦게까지 회사에서 야근을 했습니다."
                ),
                correctIndex = 0,
                explanation = "Rủ cuối tuần có thời gian đi Myeongdong mua sắm. Trả lời 'Được thôi, gặp nhau ở ga tàu lúc 2h chiều Chủ Nhật' là cuộc hội thoại logic nhất."
            ),
            Question(
                id = 2,
                type = "reading",
                questionPrompt = "Điền tiểu từ thích hợp: 한국어______ 아주 재미있지만 발음이 어렵습니다.",
                options = listOf("는", "가", "를", "에"),
                correctIndex = 1,
                explanation = "한국어가 은/는/이/가 thường dùng '한국어가 재미있다' nhấn mạnh tính từ."
            ),
            Question(
                id = 3,
                type = "writing",
                questionPrompt = "Tập viết chữ Hàn Hangul mang nghĩa 'HỌC TẬP' (Bao gồm nét tròn ngộ nghĩnh):",
                strokeReference = "배",
                correctText = "Bae",
                explanation = "Chữ 'Bae(배)' trong 'Baewuda(배우다)' nghĩa là học hỏi."
            ),
            Question(
                id = 4,
                type = "speaking",
                questionPrompt = "Hãy bấm phím thu âm và đọc rành mạch câu khẩu ngữ Hàn Quốc:",
                correctText = "오늘 날씨가 정말 따뜻하네요",
                explanation = "Tập trung biến âm nhẹ chữ '따뜻하네요' thành /따뜨타네용/."
            )
        )
    }
}
