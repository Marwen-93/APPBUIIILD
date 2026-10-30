package com.example.arabicmath

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class Op(val label: String) {
    ADD("الجمع  ➕"), SUB("الطرح  ➖"), MUL("الضرب  ✖️"), DIV("القسمة  ➗"), MIX("تمارين منوّعة  🎲")
}

data class Question(val text: String, val answer: Int, val options: List<Int>)

const val TOTAL = 10

fun Int.ar(): String = toString().map { if (it in '0'..'9') '٠' + (it - '0') else it }.joinToString("")

fun makeQuestion(op: Op, level: Int): Question {
    val max = when (level) { 1 -> 10; 2 -> 20; else -> 100 }
    val tm = when (level) { 1 -> 5; 2 -> 10; else -> 12 }
    val o = if (op == Op.MIX) listOf(Op.ADD, Op.SUB, Op.MUL, Op.DIV).random() else op
    val a: Int; val b: Int; val ans: Int; val sym: String
    when (o) {
        Op.ADD -> { a = (1..max).random(); b = (1..max).random(); ans = a + b; sym = "+" }
        Op.SUB -> { a = (2..max).random(); b = (1..a).random(); ans = a - b; sym = "−" }
        Op.MUL -> { a = (1..tm).random(); b = (1..tm).random(); ans = a * b; sym = "×" }
        else -> { b = (2..tm.coerceAtLeast(3)).random(); ans = (1..tm).random(); a = b * ans; sym = "÷" }
    }
    val opts = mutableSetOf(ans)
    while (opts.size < 4) {
        val d = ans + (-5..5).random()
        if (d >= 0) opts.add(d)
    }
    return Question("${a.ar()} $sym ${b.ar()} = ؟", ans, opts.shuffled())
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Surface(Modifier.fillMaxSize(), color = Color(0xFFF4F7FB)) { App() }
                }
            }
        }
    }
}

@Composable
fun App() {
    var screen by remember { mutableStateOf("menu") }
    var op by remember { mutableStateOf(Op.ADD) }
    var level by remember { mutableIntStateOf(1) }
    var score by remember { mutableIntStateOf(0) }

    when (screen) {
        "menu" -> MenuScreen(level, { level = it }) { chosen ->
            op = chosen; score = 0; screen = "quiz"
        }
        "quiz" -> QuizScreen(op, level, { score = it; screen = "result" }) { screen = "menu" }
        else -> ResultScreen(score, { score = 0; screen = "quiz" }) { screen = "menu" }
    }
}

@Composable
fun MenuScreen(level: Int, onLevel: (Int) -> Unit, onStart: (Op) -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Spacer(Modifier.height(24.dp))
        Text("🧮", fontSize = 56.sp)
        Text("تمارين الرياضيات", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E3A8A))
        Text("اختر المستوى", fontSize = 18.sp, modifier = Modifier.padding(top = 8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(1 to "سهل", 2 to "متوسط", 3 to "صعب").forEach { (l, name) ->
                FilterChip(selected = level == l, onClick = { onLevel(l) }, label = { Text(name, fontSize = 16.sp) })
            }
        }
        Text("اختر نوع التمرين", fontSize = 18.sp, modifier = Modifier.padding(top = 8.dp))
        Op.values().forEach { o ->
            Button(
                onClick = { onStart(o) },
                modifier = Modifier.fillMaxWidth().height(60.dp)
            ) { Text(o.label, fontSize = 20.sp) }
        }
    }
}

@Composable
fun QuizScreen(op: Op, level: Int, onFinish: (Int) -> Unit, onBack: () -> Unit) {
    var index by remember { mutableIntStateOf(0) }
    var score by remember { mutableIntStateOf(0) }
    var q by remember { mutableStateOf(makeQuestion(op, level)) }
    var picked by remember { mutableStateOf<Int?>(null) }

    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = onBack) { Text("الرئيسية", fontSize = 16.sp) }
            Text("النتيجة: ${score.ar()}", fontSize = 18.sp, modifier = Modifier.padding(top = 10.dp))
        }
        Text("السؤال ${(index + 1).ar()} من ${TOTAL.ar()}", fontSize = 18.sp)
        LinearProgressIndicator(progress = { index / TOTAL.toFloat() }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(16.dp))
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Text(q.text, fontSize = 44.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, color = Color(0xFF1E3A8A))
        }
        Spacer(Modifier.height(16.dp))
        q.options.forEach { opt ->
            val color = when {
                picked == null -> ButtonDefaults.buttonColors()
                opt == q.answer -> ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A))
                opt == picked -> ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                else -> ButtonDefaults.buttonColors(containerColor = Color.Gray)
            }
            Button(
                onClick = { if (picked == null) { picked = opt; if (opt == q.answer) score++ } },
                colors = color,
                modifier = Modifier.fillMaxWidth().height(58.dp)
            ) { Text(opt.ar(), fontSize = 24.sp) }
        }
        if (picked != null) {
            Text(
                if (picked == q.answer) "أحسنت! إجابة صحيحة 🎉" else "الإجابة الصحيحة: ${q.answer.ar()}",
                fontSize = 20.sp, fontWeight = FontWeight.Bold,
                color = if (picked == q.answer) Color(0xFF16A34A) else Color(0xFFDC2626)
            )
            Button(onClick = {
                if (index + 1 >= TOTAL) onFinish(score)
                else { index++; q = makeQuestion(op, level); picked = null }
            }) { Text(if (index + 1 >= TOTAL) "عرض النتيجة" else "السؤال التالي", fontSize = 18.sp) }
        }
    }
}

@Composable
fun ResultScreen(score: Int, onRetry: () -> Unit, onMenu: () -> Unit) {
    val msg = when {
        score >= 9 -> "ممتاز! 🌟"
        score >= 6 -> "جيد جدًا 👏"
        score >= 4 -> "جيد، حاول مرة أخرى 💪"
        else -> "لا بأس، التدريب يصنع الإتقان 📚"
    }
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(msg, fontSize = 32.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(16.dp))
        Text("نتيجتك: ${score.ar()} من ${TOTAL.ar()}", fontSize = 26.sp)
        Spacer(Modifier.height(32.dp))
        Button(onClick = onRetry, modifier = Modifier.fillMaxWidth().height(56.dp)) { Text("إعادة المحاولة", fontSize = 20.sp) }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = onMenu, modifier = Modifier.fillMaxWidth().height(56.dp)) { Text("القائمة الرئيسية", fontSize = 20.sp) }
    }
}
