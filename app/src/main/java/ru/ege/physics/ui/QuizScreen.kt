package ru.ege.physics.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ru.ege.physics.data.PhysicsData
import kotlin.math.min

private const val QUESTIONS_PER_QUIZ = 10

@Composable
fun QuizScreen(
    sectionIndex: Int,
    quizType: QuizType,
    onBack: () -> Unit
) {
    // Перемешанные вопросы (создаются при заходе и при рестарте)
    var questions by remember(sectionIndex, quizType) {
        mutableStateOf(buildQuestions(sectionIndex, quizType))
    }

    var index by remember { mutableIntStateOf(0) }
    var score by remember { mutableIntStateOf(0) }
    var answered by remember { mutableStateOf<String?>(null) } // выбранный вариант / текст ответа
    var answeredCorrect by remember { mutableStateOf(false) }   // был ли ответ верным (для режима «вписать»)
    var finished by remember { mutableStateOf(false) }

    Scaffold(topBar = { TopBarWithBack("Тренировка", onBack) }) { paddings ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddings)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            if (finished) {
                ResultView(
                    score = score,
                    total = questions.size,
                    onRestart = {
                        questions = buildQuestions(sectionIndex, quizType)
                        index = 0; score = 0; answered = null; answeredCorrect = false; finished = false
                    },
                    onExit = onBack
                )
                return@Column
            }

            val q = questions[index]

            // Прогресс
            LinearProgressIndicator(
                progress = { (index + 1f) / questions.size },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
            )
            Text(
                "Вопрос ${index + 1} из ${questions.size}   ·   Верно: $score",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
            )

            when (quizType) {
                QuizType.FORMULA_TO_NAME, QuizType.NAME_TO_FORMULA -> {
                    QuestionCard(q)
                    Spacer(Modifier.height(16.dp))
                    q.options.forEachIndexed { i, option ->
                        val isChosen = answered == option
                        val isCorrect = option == q.correct
                        OptionButton(
                            text = option,
                            enabled = answered == null,
                            chosen = isChosen,
                            correct = isCorrect && answered != null,
                            onClick = {
                                answered = option
                                if (isCorrect) score++
                            }
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                }
                QuizType.WRITE -> WriteMode(q, answered, onCheck = { text ->
                    val ok = isAnswerCorrect(q, text)
                    if (ok) score++
                    answeredCorrect = ok
                    answered = text
                })
            }

            Spacer(Modifier.height(12.dp))

            // После ответа — пояснение и кнопка «Далее»
            if (answered != null) {
                val ok = when (quizType) {
                    QuizType.WRITE -> answeredCorrect
                    else -> answered == q.correct
                }
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (ok)
                            Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            if (ok) "✅ Верно!" else "❌ Неверно. Правильный ответ: ${q.correct}",
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(q.explanation, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = {
                        if (index + 1 >= questions.size) finished = true
                        else { index++; answered = null }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (index + 1 >= questions.size) "Результаты" else "Далее →")
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

// ──────────────────── Логика вопросов ────────────────────

class Question(
    val prompt: String,          // что показываем
    val options: List<String>,   // варианты (для выбора)
    val correct: String,         // правильный ответ
    val explanation: String,     // пояснение после ответа
    val keywords: List<String>   // для режима «вписать самому»
)

private fun normalize(s: String) = s.lowercase()
    .replace('ё', 'е')
    .filter { !it.isWhitespace() }

private fun buildQuestions(sectionIndex: Int, type: QuizType): List<Question> {
    val all = PhysicsData.sections.map { it.formulas }.flatten()
    val pool = PhysicsData.sections[sectionIndex].formulas.shuffled()
    val n = min(QUESTIONS_PER_QUIZ, pool.size)

    return pool.take(n).map { f ->
        when (type) {
            QuizType.FORMULA_TO_NAME -> {
                val wrong = all.filter { it.name != f.name }
                    .shuffled().take(3).map { it.name }
                Question(
                    prompt = "Как называется эта формула?\n\n${f.formula}",
                    options = (wrong + f.name).shuffled(),
                    correct = f.name,
                    explanation = "${f.name}\n${f.definition}",
                    keywords = listOf(f.formula)
                )
            }
            QuizType.NAME_TO_FORMULA -> {
                val wrong = all.filter { it.formula != f.formula }
                    .shuffled().take(3).map { it.formula }
                Question(
                    prompt = "Какая формула соответствует?\n\n«${f.name}»",
                    options = (wrong + f.formula).shuffled(),
                    correct = f.formula,
                    explanation = "${f.formula}\n${f.definition}",
                    keywords = listOf(f.formula)
                )
            }
            QuizType.WRITE -> {
                val writeFormula = f.formula.length <= 16
                if (writeFormula) {
                    Question(
                        prompt = "Напиши формулу:\n\n«${f.name}»",
                        options = emptyList(),
                        correct = f.formula,
                        explanation = "${f.formula}\n${f.definition}",
                        keywords = keywordsFor(f.formula)
                    )
                } else {
                    // Длинную формулу писать неудобно — просим написать название
                    Question(
                        prompt = "Напиши название этой формулы:\n\n${f.formula}",
                        options = emptyList(),
                        correct = f.name,
                        explanation = "${f.name}\n${f.definition}",
                        keywords = listOf(f.name)
                    )
                }
            }
        }
    }
}

/** Упрощённая копия с заменой prompt/correct/keywords */

private fun keywordsFor(formula: String): List<String> {
    // Ключевые символы формулы: без пробелов, значимые части
    val parts = formula.split("=").map { it.trim() }
    val left = parts.firstOrNull()?.split(Regex("[+−\\-*/()]"))?.map { it.trim() }
        ?.filter { it.isNotBlank() } ?: emptyList()
    return left.ifEmpty { listOf(formula.replace(" ", "")) }
}

private fun isAnswerCorrect(q: Question, answer: String): Boolean {
    return if (q.options.isEmpty()) {
        // Режим «вписать самому»: допускаем опечатки — считаем верным ответ,
        // если совпадает точная нормализация либо ≥70% символов эталона присутствуют.
        val norm = normalize(answer)
        val target = normalize(q.correct)
        if (norm == target) return true
        if (target.isNotEmpty() && q.keywords.isNotEmpty() &&
            q.keywords.all { normalize(it) in norm }) return true
        val matched = target.count { it in norm }
        matched.toDouble() / target.length >= 0.7 && norm.isNotEmpty()
    } else {
        answer == q.correct
    }
}

// ──────────────────── UI-компоненты ────────────────────

@Composable
private fun QuestionCard(q: Question) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Text(
            q.prompt,
            modifier = Modifier.padding(20.dp),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OptionButton(
    text: String,
    enabled: Boolean,
    chosen: Boolean,
    correct: Boolean,
    onClick: () -> Unit
) {
    val container = when {
        !enabled && correct -> Color(0xFFC8E6C9)
        chosen && !correct -> Color(0xFFFFCDD2)
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.outlinedButtonColors(containerColor = container),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(text, textAlign = TextAlign.Start)
    }
}

@Composable
private fun WriteMode(q: Question, answered: String?, onCheck: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp)) {
            Text(q.prompt, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
    }
    Spacer(Modifier.height(16.dp))
    OutlinedTextField(
        value = text,
        onValueChange = { if (answered == null) text = it },
        label = { Text("Твой ответ…") },
        modifier = Modifier.fillMaxWidth(),
        enabled = answered == null
    )
    Spacer(Modifier.height(8.dp))
    if (answered == null) {
        Button(
            onClick = { onCheck(text) },
            enabled = text.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) { Text("Проверить") }
    }
}

@Composable
private fun ResultView(score: Int, total: Int, onRestart: () -> Unit, onExit: () -> Unit) {
    val pct = if (total == 0) 0 else score * 100 / total
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("🏁 Результат", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(12.dp))
        Text("$score из $total  ($pct%)", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text(
            when {
                pct >= 90 -> "Отлично! Формулы знаешь отлично 🔥"
                pct >= 70 -> "Хорошо! Ещё немного практики 💪"
                pct >= 50 -> "Неплохо, но стоит повторить материал 📖"
                else -> "Загляни в раздел «Материал» и попробуй снова 📚"
            },
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(Modifier.height(28.dp))
        Button(onClick = onRestart, modifier = Modifier.fillMaxWidth()) { Text("Пройти ещё раз") }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = onExit, modifier = Modifier.fillMaxWidth()) { Text("В меню раздела") }
    }
}
