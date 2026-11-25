package ru.ege.physics.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.ege.physics.data.PhysicsData

enum class QuizType(val title: String, val desc: String) {
    FORMULA_TO_NAME("Формула → название", "Показана формула. Выбери её название из 4 вариантов"),
    NAME_TO_FORMULA("Название → формула", "Показано название. Выбери правильную формулу из 4 вариантов"),
    WRITE("Вписать самому", "Напиши формулу или определение вручную — проверим ключевые символы")
}

/** Меню выбора типа тренировки в разделе */
@Composable
fun QuizMenuScreen(
    sectionIndex: Int,
    onBack: () -> Unit,
    onStartQuiz: (QuizType) -> Unit
) {
    val section = PhysicsData.sections[sectionIndex]
    Scaffold(topBar = { TopBarWithBack(section.title, onBack) }) { paddings ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddings)
                .padding(20.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Как будем повторять?", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(24.dp))
            QuizType.entries.forEach { type ->
                OutlinedCard(
                    onClick = { onStartQuiz(type) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Column(Modifier.padding(18.dp)) {
                        Text(type.title, style = MaterialTheme.typography.titleMedium)
                        Text(
                            type.desc,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
