package ru.ege.physics.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ru.ege.physics.data.PhysicsData
import ru.ege.physics.data.Section

/**
 * Простая навигация по экранам без сторонних библиотек.
 */
@Composable
fun AppNav() {
    MaterialTheme {
        var screen by remember { mutableStateOf<Screen>(Screen.Home) }

        when (val s = screen) {
            Screen.Home -> HomeScreen(
                onOpen = { mode -> screen = Screen.Sections(mode) }
            )
            is Screen.Sections -> SectionsScreen(
                mode = s.mode,
                onBack = { screen = Screen.Home },
                onPickSection = { sectionIndex ->
                    screen = if (s.mode == Mode.LEARN)
                        Screen.Material(sectionIndex)
                    else
                        Screen.QuizMenu(sectionIndex)
                }
            )
            is Screen.Material -> MaterialScreen(
                sectionIndex = s.sectionIndex,
                onBack = { screen = Screen.Sections(Mode.LEARN) }
            )
            is Screen.QuizMenu -> QuizMenuScreen(
                sectionIndex = s.sectionIndex,
                onBack = { screen = Screen.Sections(Mode.REPEAT) },
                onStartQuiz = { type -> screen = Screen.Quiz(s.sectionIndex, type) }
            )
            is Screen.Quiz -> QuizScreen(
                sectionIndex = s.sectionIndex,
                quizType = s.type,
                onBack = { screen = Screen.QuizMenu(s.sectionIndex) }
            )
        }
    }
}

enum class Mode { LEARN, REPEAT }

sealed class Screen {
    object Home : Screen()
    data class Sections(val mode: Mode) : Screen()
    data class Material(val sectionIndex: Int) : Screen()
    data class QuizMenu(val sectionIndex: Int) : Screen()
    data class Quiz(val sectionIndex: Int, val type: QuizType) : Screen()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBarWithBack(title: String, onBack: () -> Unit) {
    CenterAlignedTopAppBar(
        title = { Text(title) },
        navigationIcon = {
            TextButton(onClick = onBack) { Text("← Назад") }
        }
    )
}

/** Главный экран: две большие кнопки — Материал и Повторение */
@Composable
fun HomeScreen(onOpen: (Mode) -> Unit) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Физика · ЕГЭ",
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Учим формулы и проверяем себя",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(40.dp))
            Button(
                onClick = { onOpen(Mode.LEARN) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
            ) {
                Text("📖  Материал", fontSize = MaterialTheme.typography.titleLarge.fontSize)
            }
            Spacer(Modifier.height(16.dp))
            OutlinedButton(
                onClick = { onOpen(Mode.REPEAT) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
            ) {
                Text("🔁  Повторение", fontSize = MaterialTheme.typography.titleLarge.fontSize)
            }
        }
    }
}

/** Выбор раздела физики */
@Composable
fun SectionsScreen(
    mode: Mode,
    onBack: () -> Unit,
    onPickSection: (Int) -> Unit
) {
    val sections = PhysicsData.sections
    Scaffold(topBar = {
        TopBarWithBack(if (mode == Mode.LEARN) "Материал" else "Повторение", onBack)
    }) { paddings ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddings)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)
        ) {
            itemsIndexed(sections) { index, section ->
                Card(onClick = { onPickSection(index) }, modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .padding(20.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(section.emoji, style = MaterialTheme.typography.headlineSmall)
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text(section.title, style = MaterialTheme.typography.titleMedium)
                            Text(
                                "${section.formulas.size} формул",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
