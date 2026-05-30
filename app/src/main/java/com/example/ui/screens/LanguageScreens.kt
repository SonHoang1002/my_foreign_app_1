package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BookmarkedWord
import com.example.data.model.DailySuggestedWord
import com.example.data.model.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.LanguageViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

sealed class TabScreen {
    object Dashboard : TabScreen()
    object Dictionary : TabScreen()
    object Certificate : TabScreen()
    object Settings : TabScreen()
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun LanguageLearningApp(viewModel: LanguageViewModel) {
    var activeTab by remember { mutableStateOf<TabScreen>(TabScreen.Dashboard) }
    
    val config by viewModel.languageConfig.collectAsState()
    val activeQuestionList by viewModel.activeQuestions.collectAsState()
    
    // Gradient background brush
    val mainGradient = Brush.verticalGradient(
        colors = listOf(
            SoftIndigoBg.copy(alpha = 0.5f),
            SoftTealBg.copy(alpha = 0.2f),
            Color.White
        )
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (activeQuestionList.isEmpty()) {
                NavigationBar {
                    NavigationBarItem(
                        selected = activeTab == TabScreen.Dashboard,
                        onClick = { activeTab = TabScreen.Dashboard },
                        icon = { Icon(Icons.Default.Home, contentDescription = "Trang chủ") },
                        label = { Text("Học Tập", fontWeight = FontWeight.Bold) }
                    )
                    NavigationBarItem(
                        selected = activeTab == TabScreen.Dictionary,
                        onClick = { activeTab = TabScreen.Dictionary },
                        icon = { Icon(Icons.Default.Book, contentDescription = "Từ điển") },
                        label = { Text("Từ Điển", fontWeight = FontWeight.Bold) }
                    )
                    NavigationBarItem(
                        selected = activeTab == TabScreen.Certificate,
                        onClick = { activeTab = TabScreen.Certificate },
                        icon = { Icon(Icons.Default.WorkspacePremium, contentDescription = "Chứng chỉ") },
                        label = { Text("Thi Thử", fontWeight = FontWeight.Bold) }
                    )
                    NavigationBarItem(
                        selected = activeTab == TabScreen.Settings,
                        onClick = { activeTab = TabScreen.Settings },
                        icon = { Icon(Icons.Default.Translate, contentDescription = "Cài đặt") },
                        label = { Text("Ngôn Ngữ", fontWeight = FontWeight.Bold) }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(mainGradient)
                .padding(innerPadding)
        ) {
            if (config == null) {
                // Splash loading until config is initialized in DB
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = IndigoPrimary)
                }
            } else {
                AnimatedContent(
                    targetState = activeQuestionList.isNotEmpty(),
                    label = "QuestionModeTransition"
                ) { isTesting ->
                    if (isTesting) {
                        ActiveQuizScreen(viewModel = viewModel)
                    } else {
                        when (activeTab) {
                            TabScreen.Dashboard -> DashboardScreen(
                                viewModel = viewModel,
                                onNavigateToTab = { tab -> activeTab = tab }
                            )
                            TabScreen.Dictionary -> DictionaryScreen(viewModel = viewModel)
                            TabScreen.Certificate -> CertificateScreen(viewModel = viewModel)
                            TabScreen.Settings -> LanguageSelectionScreen(viewModel = viewModel)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DashboardScreen(viewModel: LanguageViewModel, onNavigateToTab: (TabScreen) -> Unit) {
    val config by viewModel.languageConfig.collectAsState()
    val dailyWords by viewModel.dailyVocabulary.collectAsState()
    val dailyLoading by viewModel.dailyLoading.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Welcoming & Profile header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Xin chào Người Học! 👋",
                        style = MaterialTheme.typography.titleMedium,
                        color = SlateSubtitle
                    )
                    Text(
                        text = "Hôm nay tuyệt vời để học ${config?.selectedLanguageName}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = SlateText
                    )
                }
                
                // Cute Flag placeholder
                Card(
                    shape = CircleShape,
                    colors = CardDefaults.cardColors(containerColor = IndigoPrimary.copy(alpha = 0.1f)),
                    modifier = Modifier.size(52.dp),
                    border = BorderStroke(2.dp, IndigoPrimary)
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = when (config?.selectedLanguageCode) {
                                "ja" -> "🇯🇵"
                                "zh" -> "🇨🇳"
                                "ko" -> "🇰🇷"
                                "en" -> "🇬🇧"
                                else -> "🌐"
                            },
                            fontSize = 24.sp
                        )
                    }
                }
            }
        }

        // Streak & target credentials card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkCardSurface),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Mục tiêu: ${config?.targetCertificate}",
                            color = Pink80,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Trình độ hiện tại: ${config?.selectedLevel}",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.LocalFireDepartment,
                                contentDescription = "Chuỗi ngày học",
                                tint = AmberAward,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Chuỗi liên tục: ${config?.streakCount} ngày 🔥",
                                color = SoftIndigoBg,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }

                    Button(
                        onClick = { onNavigateToTab(TabScreen.Settings) },
                        colors = ButtonDefaults.buttonColors(containerColor = Purple80),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Thay đổi", color = DarkBackground, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Daily vocabulary section
        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.CalendarToday,
                            contentDescription = "Gợi ý hàng ngày",
                            tint = TealAccent,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Từ vựng gợi ý theo ngày",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = SlateText
                        )
                    }
                    
                    IconButton(
                        onClick = { viewModel.generateDailyWords() },
                        enabled = !dailyLoading
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Làm mới gợi ý",
                            tint = IndigoPrimary
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))

                if (dailyLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = IndigoPrimary)
                    }
                } else {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(dailyWords) { word ->
                            DailySuggestedWordCard(
                                suggestedWord = word,
                                viewModel = viewModel,
                                onWordClick = {
                                    onNavigateToTab(TabScreen.Dictionary)
                                    viewModel.searchDictionaryWord(word.word)
                                }
                            )
                        }
                    }
                }
            }
        }

        // 4 Key Skills Panel Grid
        item {
            Column {
                Text(
                    text = "Luyện tập 4 kỹ năng chính",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = SlateText
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    SkillCategoryButton(
                        title = "NGHE (Listening)",
                        icon = Icons.Default.Hearing,
                        subtitle = "Hiểu hội thoại bản xứ",
                        color = SoftIndigoBg,
                        borderColor = IndigoPrimary,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.startSkillPractice("listening") }
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    SkillCategoryButton(
                        title = "NÓI (Speaking)",
                        icon = Icons.Default.Mic,
                        subtitle = "Chấm điểm phát âm AI",
                        color = SoftTealBg,
                        borderColor = TealAccent,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.startSkillPractice("speaking") }
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    SkillCategoryButton(
                        title = "ĐỌC (Reading)",
                        icon = Icons.Default.ChromeReaderMode,
                        subtitle = "Ngữ pháp & từ điền khuyết",
                        color = Color(0xFFFEF3C7),
                        borderColor = AmberAward,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.startSkillPractice("reading") }
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    SkillCategoryButton(
                        title = "VIẾT (Stroke Play)",
                        icon = Icons.Default.Gesture,
                        subtitle = "Đồ nét chữ ký tự",
                        color = Color(0xFFFCE7F3),
                        borderColor = Pink40,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.startSkillPractice("writing") }
                    )
                }
            }
        }

        // Fast Quick-Action Mock exam banner
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToTab(TabScreen.Certificate) },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = IndigoPrimary.copy(alpha = 0.08f)),
                border = BorderStroke(1.dp, IndigoPrimary.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(IndigoPrimary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.School,
                            contentDescription = "Thi Thử",
                            tint = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Thi thử chứng chỉ quốc tế",
                            fontWeight = FontWeight.Bold,
                            color = IndigoPrimary,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "Bám sát đề thực tế của ${config?.targetCertificate}",
                            color = SlateSubtitle,
                            fontSize = 12.sp
                        )
                    }
                    Icon(
                        Icons.Default.ArrowForwardIos,
                        contentDescription = "Chuyển hướng",
                        tint = IndigoPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun DailySuggestedWordCard(
    suggestedWord: DailySuggestedWord,
    viewModel: LanguageViewModel,
    onWordClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(200.dp)
            .height(135.dp)
            .clickable { onWordClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .background(SoftIndigoBg, RoundedCornerShape(8.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = suggestedWord.partOfSpeech,
                        fontSize = 10.sp,
                        color = IndigoPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                IconButton(
                    modifier = Modifier.size(24.dp),
                    onClick = { viewModel.playTts(suggestedWord.word) }
                ) {
                    Icon(
                        Icons.Default.VolumeUp,
                        contentDescription = "Phát âm tiếng",
                        tint = IndigoPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            
            Text(
                text = suggestedWord.word,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = SlateText,
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = suggestedWord.transcription,
                fontSize = 11.sp,
                color = SlateSubtitle,
                fontWeight = FontWeight.Light
            )

            Text(
                text = suggestedWord.translation,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = TealAccent,
                maxLines = 1,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun SkillCategoryButton(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    subtitle: String,
    color: Color,
    borderColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(115.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = color),
        border = BorderStroke(1.dp, borderColor.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = borderColor,
                modifier = Modifier.size(28.dp)
            )
            
            Column {
                Text(
                    text = title,
                    fontWeight = FontWeight.ExtraBold,
                    color = SlateText,
                    fontSize = 12.sp,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = subtitle,
                    color = SlateSubtitle,
                    fontSize = 10.sp,
                    lineHeight = 11.sp
                )
            }
        }
    }
}

// 2. Dictionary Search lookup Screen
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DictionaryScreen(viewModel: LanguageViewModel) {
    var query by remember { mutableStateOf("") }
    val dictResult by viewModel.dictionaryResult.collectAsState()
    val dictLoading by viewModel.dictionaryLoading.collectAsState()
    val isStarred by viewModel.wordBookmarked.collectAsState()
    val searchLogs by viewModel.searchHistory.collectAsState()
    val bookmarks by viewModel.bookmarksList.collectAsState()

    val keyboardController = LocalSoftwareKeyboardController.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Từ Điển Đa Ngôn Ngữ AI 📖",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
            color = SlateText
        )
        Text(
            text = "Tra cứu thông minh và xem các ví dụ trực quan sinh động",
            style = MaterialTheme.typography.bodyMedium,
            color = SlateSubtitle
        )
        
        Spacer(modifier = Modifier.height(14.dp))

        // Search Bar field
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("dictionary_search_input"),
            placeholder = { Text("Nhập từ vựng cần tra cứu...") },
            trailingIcon = {
                if (query.isNotBlank()) {
                    IconButton(onClick = { query = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "Xóa")
                    }
                } else {
                    Icon(Icons.Default.Search, contentDescription = "Tìm kiếm")
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = {
                keyboardController?.hide()
                viewModel.searchDictionaryWord(query)
            }),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Box(modifier = Modifier.weight(1f)) {
            if (dictLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = IndigoPrimary)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Trí tuệ nhân tạo đang tra cúu mẫu câu...", color = SlateSubtitle, fontSize = 12.sp)
                    }
                }
            } else if (dictResult != null) {
                // Show AI Dictionary query details
                DictionaryDetailsCard(
                    details = dictResult!!,
                    isStarred = isStarred,
                    viewModel = viewModel
                )
            } else {
                // Show bookmarks and history
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (bookmarks.isNotEmpty()) {
                        item {
                            Text(
                                text = "Từ vựng của tôi (${bookmarks.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = SlateText
                            )
                        }
                        items(bookmarks) { b ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        query = b.word
                                        viewModel.searchDictionaryWord(b.word)
                                    },
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .padding(14.dp)
                                        .fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = b.word,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            color = SlateText
                                        )
                                        Text(
                                            text = "${b.transcription}  •  ${b.definition}",
                                            fontSize = 12.sp,
                                            color = SlateSubtitle
                                        )
                                    }
                                    IconButton(onClick = { viewModel.playTts(b.word) }) {
                                        Icon(Icons.Default.VolumeUp, "Nghe", tint = IndigoPrimary)
                                    }
                                }
                            }
                        }
                    }

                    if (searchLogs.isNotEmpty()) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Lịch sử tìm kiếm",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = SlateText
                                )
                                TextButton(onClick = { viewModel.clearSearchHistory() }) { // Simplistic override
                                    Text("Xóa hết", color = SlateSubtitle)
                                }
                            }
                        }
                        items(searchLogs) { log ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        query = log.keyword
                                        viewModel.searchDictionaryWord(log.keyword)
                                    }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.History, "Lịch sử", tint = SlateSubtitle, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(log.keyword, color = SlateText, fontSize = 14.sp)
                            }
                        }
                    }
                    
                    if (bookmarks.isEmpty() && searchLogs.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 40.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        Icons.Default.Search,
                                        contentDescription = "Chưa tìm",
                                        tint = SlateSubtitle,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "Nhập cụm từ bất kỳ để giải nghĩa sâu sắc cùng AI",
                                        textAlign = TextAlign.Center,
                                        color = SlateSubtitle,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DictionaryDetailsCard(
    details: DictDetails,
    isStarred: Boolean,
    viewModel: LanguageViewModel
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White, RoundedCornerShape(16.dp))
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = details.word,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = SlateText
                    )
                    Text(
                        text = "${details.transcription}  •  ${details.pos}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SlateSubtitle,
                        fontWeight = FontWeight.Medium
                    )
                }

                Row {
                    IconButton(onClick = { viewModel.playTts(details.word) }) {
                        Icon(
                            Icons.Default.VolumeUp,
                            contentDescription = "Phát âm giọng nói",
                            tint = IndigoPrimary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    IconButton(onClick = { viewModel.toggleBookmarkCurrentWord() }) {
                        Icon(
                            imageVector = if (isStarred) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = "Starred",
                            tint = if (isStarred) AmberAward else SlateSubtitle,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }

        item {
            Divider(color = Color(0xFFF1F5F9))
        }

        // Translation
        item {
            Column {
                Text(
                    text = "Dịch nghĩa & Giải thích",
                    fontSize = 12.sp,
                    color = IndigoPrimary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = details.translation,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TealAccent
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = details.definition,
                    fontSize = 14.sp,
                    color = SlateText
                )
            }
        }

        // Grammatical breakdown details
        if (details.details.isNotBlank()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SoftIndigoBg.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "Ghi chú Ngữ pháp / Phân loại",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = IndigoPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = details.details,
                            fontSize = 13.sp,
                            color = SlateText,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }

        // Stroke order info for CJK
        if (details.strokeGuidance.isNotBlank()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SoftTealBg.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Gesture, "Stroke order", tint = TealAccent, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Cách Viết & Thứ tự Nét vẽ",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TealAccent
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = details.strokeGuidance,
                            fontSize = 13.sp,
                            color = SlateText,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }

        // Visual Examples (Xem các ví dụ trực quan)
        if (details.examples.isNotEmpty()) {
            item {
                Text(
                    text = "Ví dụ Trực quan trong Ngữ cảnh",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = SlateText
                )
            }

            items(details.examples) { ex ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                    border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = ex.sentence,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = SlateText,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { viewModel.playTts(ex.sentence) }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.VolumeUp, "Speak sentence", tint = IndigoPrimary, modifier = Modifier.size(16.dp))
                            }
                        }
                        Text(
                            text = ex.translation,
                            fontSize = 13.sp,
                            color = SlateSubtitle,
                            fontWeight = FontWeight.Medium
                        )
                        
                        if (ex.visualPrompt.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            // Mind visual guide (Trực quan trí não) - fulfilling visual requirement elegantly
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Purple80.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                    .padding(8.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    Icons.Default.Image,
                                    contentDescription = "Visual clue",
                                    tint = IndigoPrimary, // wait, CrimsonAccent ? we have Pink40, or TealAccent, AmberAward. Let's use IndigoPrimary
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "MINH HỌA TRỰC QUAN (AI Visual Guide):",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = IndigoPrimary
                                    )
                                    Text(
                                        text = ex.visualPrompt,
                                        fontSize = 11.sp,
                                        color = SlateText,
                                        lineHeight = 15.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CertificateScreen(viewModel: LanguageViewModel) {
    val results by viewModel.examResults.collectAsState()
    val config by viewModel.languageConfig.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Kỳ Thi Thử Chứng Chỉ Quốc Tế 🏅",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = SlateText
            )
            Text(
                text = "Cấp độ mô phỏng theo chuẩn ngôn ngữ đang lựa chọn",
                style = MaterialTheme.typography.bodyMedium,
                color = SlateSubtitle
            )
        }

        // Active certificate selector indicator
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "CHỨNG CHỈ ĐANG THEO ĐUỔI",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = IndigoPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${config?.targetCertificate} - Mức độ ${config?.selectedLevel}",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = SlateText
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Gồm các câu hỏi tuyển chọn sát đề cương thực tế nhất của các năm gần đây.",
                        fontSize = 13.sp,
                        color = SlateSubtitle
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    
                    Button(
                        onClick = { viewModel.startCertificateMockExam() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("start_mock_exam_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, "Bắt đầu")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("BẮT ĐẦU THI THỬ NGAY", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Historical attempts listing (Thi thử)
        item {
            Text(
                text = "Lịch sử làm bài thi thử",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = SlateText
            )
        }

        if (results.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFFF1F5F9))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.AssignmentLate, "Báo cáo", tint = SlateSubtitle)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Chưa có kết quả lịch sử. Hãy tạo bài thi thử đầu tiên!", color = SlateSubtitle, fontSize = 12.sp)
                        }
                    }
                }
            }
        } else {
            items(results) { res ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Row(
                        modifier = Modifier
                            .padding(14.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Thi thử ${res.levelName}",
                                fontWeight = FontWeight.Bold,
                                color = SlateText
                            )
                            Text(
                                text = "Kỹ năng: ${res.category}",
                                fontSize = 11.sp,
                                color = SlateSubtitle
                            )
                            Text(
                                text = "Nhận xét: ${res.feedback}",
                                fontSize = 12.sp,
                                color = SlateSubtitle,
                                lineHeight = 15.sp
                            )
                        }
                        
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .background(
                                    if (res.score >= res.maxScore / 2) SoftTealBg else Color(0xFFFEE2E2),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${res.score}/${res.maxScore}",
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp,
                                color = if (res.score >= res.maxScore / 2) EmeraldSuccess else Color(0xFFDC2626)
                            )
                        }
                    }
                }
            }
        }
    }
}

// Settings Screen - Configure Target Language & Certificate
@Composable
fun LanguageSelectionScreen(viewModel: LanguageViewModel) {
    val languages = listOf(
        Triple("en", "Tiếng Anh", "TOEIC / IELTS"),
        Triple("ja", "Tiếng Nhật", "JLPT (N5 - N1)"),
        Triple("zh", "Tiếng Trung", "HSK (1 - 6)"),
        Triple("ko", "Tiếng Hàn", "TOPIK (I - II)")
    )

    val levelsMap = mapOf(
        "en" to listOf("A2", "B1", "B2", "C1", "IELTS"),
        "ja" to listOf("JLPT N5", "JLPT N4", "JLPT N3", "JLPT N2", "JLPT N1"),
        "zh" to listOf("HSK 1", "HSK 2", "HSK 3", "HSK 4", "HSK 5", "HSK 6"),
        "ko" to listOf("TOPIK I Sơ cấp", "TOPIK II Trung-Cao cấp")
    )

    val activeConfig by viewModel.languageConfig.collectAsState()

    var tempSelectedLang by remember { mutableStateOf(activeConfig?.selectedLanguageCode ?: "en") }
    var tempLangName by remember { mutableStateOf(activeConfig?.selectedLanguageName ?: "Tiếng Anh") }
    var tempCert by remember { mutableStateOf(activeConfig?.targetCertificate ?: "TOEIC") }
    var tempLevel by remember { mutableStateOf(activeConfig?.selectedLevel ?: "B1") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Lựa Chọn Ngoại Ngữ 🌐",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            color = SlateText
        )
        Text(
            text = "Thay đổi ngôn ngữ và mức độ mục tiêu chứng chỉ của bạn",
            style = MaterialTheme.typography.bodyMedium,
            color = SlateSubtitle
        )

        Divider(color = Color(0xFFE2E8F0))

        Text(
            text = "Mục 1: Chọn ngôn ngữ mới để học tập",
            fontWeight = FontWeight.Bold,
            color = SlateText,
            fontSize = 15.sp
        )

        languages.forEach { (code, name, summary) ->
            val isSelected = tempSelectedLang == code
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        tempSelectedLang = code
                        tempLangName = name
                        tempCert = summary.split(" / ").first()
                        tempLevel = levelsMap[code]?.firstOrNull() ?: ""
                    },
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) SoftIndigoBg else Color.White
                ),
                border = BorderStroke(2.dp, if (isSelected) IndigoPrimary else Color(0xFFE2E8F0)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = name,
                            fontWeight = FontWeight.ExtraBold,
                            color = SlateText,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "Chứng chỉ: $summary",
                            fontSize = 12.sp,
                            color = SlateSubtitle
                        )
                    }
                    if (isSelected) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "Selected",
                            tint = IndigoPrimary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Mục 2: Chọn trình độ/cấp chứng chỉ tương ứng",
            fontWeight = FontWeight.Bold,
            color = SlateText,
            fontSize = 15.sp
        )

        val levelsList = levelsMap[tempSelectedLang] ?: emptyList()
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(levelsList) { level ->
                val isLevelSelected = tempLevel == level
                FilterChip(
                    selected = isLevelSelected,
                    onClick = {
                        tempLevel = level
                        tempCert = level
                    },
                    label = { Text(level, fontWeight = FontWeight.Bold) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = IndigoPrimary,
                        selectedLabelColor = Color.White
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                viewModel.changeLearningLanguage(
                    code = tempSelectedLang,
                    name = tempLangName,
                    cert = tempCert,
                    level = tempLevel
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = TealAccent),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Save, "Save changes")
            Spacer(modifier = Modifier.width(8.dp))
            Text("XÁC NHẬN THAY ĐỔI", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
        }
    }
}

// 4. ACTIVE QUESTIONS SCREEN (NGHE, NOI, DOC, VIET)
@Composable
fun ActiveQuizScreen(viewModel: LanguageViewModel) {
    val activeQuestions by viewModel.activeQuestions.collectAsState()
    val currentIndex by viewModel.currentQuestionIndex.collectAsState()
    val isFinished by viewModel.quizFinished.collectAsState()
    val score by viewModel.calculatedScore.collectAsState()
    val examName by viewModel.currentActiveExamName.collectAsState()

    val selectionMap by viewModel.selectedAnswers.collectAsState()
    val typedMap by viewModel.typedAnswers.collectAsState()

    if (isFinished) {
        val totalQ = activeQuestions.size
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        Icons.Default.Verified,
                        contentDescription = "Success",
                        tint = EmeraldSuccess,
                        modifier = Modifier.size(72.dp)
                    )
                    
                    Text(
                        text = "KẾT QUẢ KIỂM TRA",
                        fontWeight = FontWeight.Black,
                        fontSize = 24.sp,
                        color = SlateText
                    )
                    
                    Text(
                        text = examName,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        color = SlateSubtitle
                    )

                    Divider()

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Kết quả chung cuộc:",
                            fontWeight = FontWeight.Bold,
                            color = SlateSubtitle,
                            fontSize = 16.sp
                        )
                        Box(
                            modifier = Modifier
                                .background(SoftTealBg, CircleShape)
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "$score / $totalQ điểm",
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp,
                                color = EmeraldSuccess
                            )
                        }
                    }

                    Text(
                        text = if (score >= totalQ * 0.8) {
                            "Tuyệt hảo! Trình độ của bạn đang đạt mức chín muồi, đáp ứng mục tiêu đề ra."
                        } else if (score >= totalQ * 0.5) {
                            "Khá lắm! Cố gắng luyện tập thêm phần thi từ mới để cải thiện điểm số tối ưu."
                        } else {
                            "Hãy chăm chỉ xem lại từ điển AI và luyện từng kỹ năng hàng ngày bạn nhé!"
                        },
                        textAlign = TextAlign.Center,
                        fontSize = 14.sp,
                        color = SlateText,
                        lineHeight = 20.sp,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { viewModel.resetQuiz() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("QUAY LẠI TRANG CHỦ", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    } else {
        val activeQ = activeQuestions.getOrNull(currentIndex)
        if (activeQ != null) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header of testing progress
                item {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = examName,
                                fontSize = 12.sp,
                                color = IndigoPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Câu ${currentIndex + 1}/${activeQuestions.size}",
                                fontSize = 14.sp,
                                color = SlateSubtitle,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        LinearProgressIndicator(
                            progress = (currentIndex + 1).toFloat() / activeQuestions.size,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            color = IndigoPrimary,
                            trackColor = SoftIndigoBg
                        )
                    }
                }

                // Question Core panel Card
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // Badge indicating skill type
                            Box(
                                modifier = Modifier
                                    .background(
                                        when (activeQ.type) {
                                            "listening" -> Color(0xFFEEF2FF)
                                            "speaking" -> Color(0xFFECFDF5)
                                            "reading" -> Color(0xFFFEF3C7)
                                            else -> Color(0xFFFCE7F3)
                                        },
                                        RoundedCornerShape(8.dp)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = activeQ.type.uppercase(),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = when (activeQ.type) {
                                        "listening" -> IndigoPrimary
                                        "speaking" -> TealAccent
                                        "reading" -> AmberAward
                                        else -> Pink40
                                    }
                                )
                            }
                            
                            Text(
                                text = activeQ.questionPrompt,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = SlateText
                            )

                            // 1. LISTENING SKILL (NGHE) Interface helper
                            if (activeQ.type == "listening") {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(SoftIndigoBg, RoundedCornerShape(12.dp))
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        IconButton(
                                            onClick = { viewModel.playTts(activeQ.audioSpeakerText) },
                                            modifier = Modifier
                                                .size(54.dp)
                                                .background(IndigoPrimary, CircleShape)
                                        ) {
                                            Icon(
                                                Icons.Default.PlayArrow,
                                                contentDescription = "Nghe",
                                                tint = Color.White,
                                                modifier = Modifier.size(32.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text("Bấm vào nút để nghe đoạn đọc âm thanh", fontSize = 12.sp, color = SlateSubtitle)
                                    }
                                }
                            }

                            // 2. SPEAKING SKILL (NÓI) Interface helper (Sử dụng trí tuệ AI)
                            if (activeQ.type == "speaking") {
                                SpeakingIntegrationBox(
                                    targetText = activeQ.correctText,
                                    viewModel = viewModel
                                )
                            }

                            // 3. WRITING SKILL (VIẾT / STROKE GUIDANCE) Touch-interactive drawing canvas
                            if (activeQ.type == "writing") {
                                WritingStrokeCanvasBox(
                                    targetCharacter = activeQ.strokeReference,
                                    viewModel = viewModel,
                                    hint = activeQ.explanation
                                )
                            }
                        }
                    }
                }

                // 4. MULTIPLE CHOICES (LIS/RED)
                if (activeQ.type == "listening" || activeQ.type == "reading") {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            activeQ.options.forEachIndexed { optIndex, optionText ->
                                val isSelected = selectionMap[currentIndex] == optIndex
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.selectOptionForQuestion(currentIndex, optIndex) },
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) SoftIndigoBg else Color.White
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(2.dp, if (isSelected) IndigoPrimary else Color(0xFFE2E8F0))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .background(
                                                    if (isSelected) IndigoPrimary else Color(0xFFE2E8F0),
                                                    CircleShape
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = ('A' + optIndex).toString(),
                                                color = if (isSelected) Color.White else SlateSubtitle,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = optionText,
                                            color = SlateText,
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 14.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Bottom actions buttons forward/done
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { viewModel.resetQuiz() }) {
                            Text("Hủy bài thi", color = SlateSubtitle)
                        }

                        Button(
                            onClick = { viewModel.navigateQuestionNext() },
                            colors = ButtonDefaults.buttonColors(containerColor = TealAccent),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.height(48.dp)
                        ) {
                            Text(
                                text = if (currentIndex == activeQuestions.size - 1) "NỘP BÀI THI" else "CÂU TIẾP THEO",
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(Icons.Default.ArrowForward, contentDescription = "Next")
                        }
                    }
                }
            }
        }
    }
}

// Draw touch coordinates canvas
@Composable
fun WritingStrokeCanvasBox(
    targetCharacter: String,
    viewModel: LanguageViewModel,
    hint: String
) {
    val strokes by viewModel.canvasStrokes.collectAsState()
    var currentPath = remember { mutableStateOf(emptyList<Offset>()).value }
    val scope = rememberCoroutineScope()

    var drawnCount by remember { mutableStateOf(0) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Bảng tập viết chữ cổ truyền / nét đứt", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = SlateText)
                if (drawnCount > 0) {
                    Text("Đã vẽ: $drawnCount nét vẽ", fontSize = 11.sp, color = TealAccent)
                }
            }
            TextButton(
                onClick = {
                    viewModel.clearCanvasStrokes()
                    drawnCount = 0
                }
            ) {
                Icon(Icons.Default.DeleteSweep, "Xóa")
                Spacer(modifier = Modifier.width(4.dp))
                Text("Xóa nét vẽ", color = SlateSubtitle)
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .background(Color(0xFFFCFCFD), RoundedCornerShape(12.dp))
                .border(2.dp, Color(0xFFF1F5F9), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            // Target character faint background trace guide
            Text(
                text = targetCharacter,
                fontSize = 150.sp,
                fontWeight = FontWeight.ExtraBold,
                color = SlateSubtitle.copy(alpha = 0.08f),
                textAlign = TextAlign.Center
            )

            // Draw grid guidance lines
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                // draw dashes horizontal-vertical lines
                drawLine(
                    color = Color.LightGray.copy(alpha = 0.5f),
                    start = Offset(0f, h / 2),
                    end = Offset(w, h / 2),
                    strokeWidth = 2f,
                    pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
                )
                drawLine(
                    color = Color.LightGray.copy(alpha = 0.5f),
                    start = Offset(w / 2, 0f),
                    end = Offset(w / 2, h),
                    strokeWidth = 2f,
                    pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
                )
            }

            // Real Interactive Canvas
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                currentPath = listOf(offset)
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                currentPath = currentPath + change.position
                            },
                            onDragEnd = {
                                if (currentPath.isNotEmpty()) {
                                    viewModel.addCanvasStrokeLine(currentPath)
                                    currentPath = emptyList()
                                    drawnCount++
                                }
                            }
                        )
                    }
            ) {
                // Draw past saved strokes
                strokes.forEach { stroke ->
                    val path = Path().apply {
                        if (stroke.isNotEmpty()) {
                            moveTo(stroke[0].x, stroke[0].y)
                            for (i in 1 until stroke.size) {
                                lineTo(stroke[i].x, stroke[i].y)
                            }
                        }
                    }
                    drawPath(
                        path = path,
                        color = IndigoPrimary,
                        style = Stroke(width = 8f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                }

                // Draw active dragging stroke
                if (currentPath.isNotEmpty()) {
                    val path = Path().apply {
                        moveTo(currentPath[0].x, currentPath[0].y)
                        for (i in 1 until currentPath.size) {
                            lineTo(currentPath[i].x, currentPath[i].y)
                        }
                    }
                    drawPath(
                        path = path,
                        color = TealAccent,
                        style = Stroke(width = 8f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                }
            }
        }

        Text(
            text = "Hướng dẫn: $hint",
            fontSize = 11.sp,
            color = SlateSubtitle,
            lineHeight = 14.sp
        )
    }
}

// Speak evaluation block
@Composable
fun SpeakingIntegrationBox(
    targetText: String,
    viewModel: LanguageViewModel
) {
    val isRecording by viewModel.isRecordingSpoken.collectAsState()
    val speakingResult by viewModel.speakingResult.collectAsState()
    val textSpokenResult by viewModel.recordedTranspText.collectAsState()
    val feedbackLoading by viewModel.speakingFeedbackLoading.collectAsState()
    
    val scope = rememberCoroutineScope()

    var animatedCycles by remember { mutableStateOf(0f) }

    // Waveform simulation
    LaunchedEffect(isRecording) {
        if (isRecording) {
            while (true) {
                animatedCycles = (10..50).random().toFloat()
                delay(120)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SoftTealBg, RoundedCornerShape(12.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "YÊU CẦU: HÃY PHÁT ÂM CÂU SAU ĐÂY",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = SlateSubtitle
        )
        Text(
            text = targetText,
            fontSize = 18.sp,
            fontWeight = FontWeight.ExtraBold,
            color = SlateText
        )
        
        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { viewModel.playTts(targetText) },
                modifier = Modifier
                    .background(Color.White, CircleShape)
                    .size(40.dp)
            ) {
                Icon(Icons.Default.VolumeUp, "Nghe mẫu", tint = TealAccent)
            }

            if (isRecording) {
                Button(
                    onClick = { viewModel.stopRecordingAndSubmit(targetText) },
                    colors = ButtonDefaults.buttonColors(containerColor = Pink40),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Icon(Icons.Default.MicOff, "Mic")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Đang Thu Âm...", fontWeight = FontWeight.Bold)
                    
                    // Wavelet drawing simulation
                    Spacer(modifier = Modifier.width(8.dp))
                    Canvas(modifier = Modifier.size(20.dp)) {
                        drawCircle(color = Color.White, radius = animatedCycles / 3)
                    }
                }
            } else {
                Button(
                    onClick = { viewModel.startMicrophoneRecording() },
                    colors = ButtonDefaults.buttonColors(containerColor = TealAccent),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Icon(Icons.Default.Mic, "Mic")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Bắt đầu nói", fontWeight = FontWeight.Bold)
                }
            }
        }

        if (feedbackLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = TealAccent, modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Trí tuệ AI đang chấm điểm phát âm...", color = SlateSubtitle, fontSize = 11.sp)
                }
            }
        } else if (speakingResult != null) {
            val score = speakingResult!!["score"] as? Int ?: 80
            val fluency = speakingResult!!["fluency"] as? String ?: "Tốt"
            val feedback = speakingResult!!["feedback"] as? String ?: ""
            val corrections = speakingResult!!["corrections"] as? String ?: ""

            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFFD1FAE5)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "KẾT QUẢ PHÂN TÍCH AI",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldSuccess
                        )
                        Box(
                            modifier = Modifier
                                .background(SoftTealBg, CircleShape)
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Điểm số: $score/100 ($fluency)",
                                fontWeight = FontWeight.Bold,
                                color = EmeraldSuccess,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Text(
                        text = "Văn bản AI ghi nhận được:\n\"$textSpokenResult\"",
                        fontSize = 12.sp,
                        color = SlateSubtitle,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )

                    Text(
                        text = "Nhận xét: $feedback",
                        fontSize = 12.sp,
                        color = SlateText,
                        lineHeight = 16.sp
                    )
                    
                    if (corrections.isNotBlank()) {
                        Text(
                            text = corrections,
                            fontSize = 12.sp,
                            color = IndigoPrimary,
                            lineHeight = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}
