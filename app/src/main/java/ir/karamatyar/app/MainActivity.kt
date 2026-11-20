package ir.karamatyar.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import ir.karamatyar.app.data.LearningContent
import ir.karamatyar.app.data.Tip
import ir.karamatyar.app.data.Scenario
import ir.karamatyar.app.data.Question
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.runtime.CompositionLocalProvider

private val Ink = Color(0xFFF8F5FF)
private val Muted = Color(0xFFB8B2CC)
private val Bg = Color(0xFF17162B)
private val Panel = Color(0xFF292541)
private val Panel2 = Color(0xFF342F4E)
private val Yellow = Color(0xFFF4C84A)
private val Mint = Color(0xFF70D6C3)
private val Coral = Color(0xFFFF8D80)
private val Blue = Color(0xFF81B7F4)
private val Lilac = Color(0xFFC6A5F6)
private val Green = Color(0xFF7ED6A7)

private enum class Page { HOME, TIPS, TIP_DETAIL, SCENARIOS, QUIZ, RESULT, PROGRESS, SOURCES }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(23,22,43)
        window.navigationBarColor = android.graphics.Color.rgb(23,22,43)
        setContent { KaramatTheme { KaramatApp() } }
    }
}

@Composable
private fun KaramatTheme(content: @Composable () -> Unit) {
    val regular = FontFamily(Font(R.font.shabnam, FontWeight.Normal), Font(R.font.shabnammedium, FontWeight.Medium))
    val typography = Typography(
        displayLarge = androidx.compose.ui.text.TextStyle(fontFamily=regular, fontWeight=FontWeight.Bold),
        headlineLarge = androidx.compose.ui.text.TextStyle(fontFamily=regular, fontWeight=FontWeight.Bold),
        headlineMedium = androidx.compose.ui.text.TextStyle(fontFamily=regular, fontWeight=FontWeight.Bold),
        titleLarge = androidx.compose.ui.text.TextStyle(fontFamily=regular, fontWeight=FontWeight.Bold),
        titleMedium = androidx.compose.ui.text.TextStyle(fontFamily=regular, fontWeight=FontWeight.Medium),
        bodyLarge = androidx.compose.ui.text.TextStyle(fontFamily=regular),
        bodyMedium = androidx.compose.ui.text.TextStyle(fontFamily=regular),
        labelLarge = androidx.compose.ui.text.TextStyle(fontFamily=regular, fontWeight=FontWeight.Medium)
    )
    MaterialTheme(colorScheme=darkColorScheme(primary=Yellow, secondary=Mint, background=Bg, surface=Panel, onPrimary=Bg, onSecondary=Bg, onBackground=Ink, onSurface=Ink), typography=typography, content=content)
}

@Composable
private fun KaramatApp() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("karamatyar_progress", 0) }
    var page by rememberSaveable { mutableStateOf(Page.HOME.name) }
    var selectedTip by rememberSaveable { mutableIntStateOf(1) }
    var selectedScenario by rememberSaveable { mutableIntStateOf(1) }
    var questionIndex by rememberSaveable { mutableIntStateOf(0) }
    var selectedAnswer by rememberSaveable { mutableIntStateOf(-1) }
    var learned by remember { mutableStateOf(prefs.getStringSet("learned", emptySet())?.toSet() ?: emptySet()) }
    var scores by remember { mutableStateOf(prefs.getString("scores", "") ?: "") }
    var answers by remember { mutableStateOf(listOf<Int>()) }
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf("همه") }

    fun go(p: Page) { page = p.name }
    fun passed(id: Int): Boolean = scores.split(",").any { it.startsWith("$id:") && (it.substringAfter(":").toIntOrNull() ?: 0) >= 60 }
    fun scoreFor(id: Int): Int? = scores.split(",").firstOrNull { it.startsWith("$id:") }?.substringAfter(":")?.toIntOrNull()
    fun saveScore(id:Int, score:Int) {
        val all = scores.split(",").filter { it.isNotBlank() && !it.startsWith("$id:") }.toMutableList()
        all.add("$id:$score"); scores=all.joinToString(","); prefs.edit().putString("scores", scores).apply()
    }
    val scenario = LearningContent.scenarios.first { it.id == selectedScenario }
    val question = scenario.questions[questionIndex]
    val score = if (answers.isEmpty()) 0 else answers.indices.count { answers[it] == scenario.questions[it].answer } * 100 / scenario.questions.size

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            containerColor = Bg,
            bottomBar = {
                if (page in listOf(Page.HOME.name, Page.TIPS.name, Page.SCENARIOS.name, Page.PROGRESS.name)) {
                    NavigationBar(containerColor=Color(0xFF1D1B32), tonalElevation=0.dp) {
                        NavItem("خانه", Icons.Default.Home, page==Page.HOME.name) { go(Page.HOME) }
                        NavItem("آموزش", Icons.Default.AutoStories, page==Page.TIPS.name) { go(Page.TIPS) }
                        NavItem("سناریوها", Icons.Default.Quiz, page==Page.SCENARIOS.name) { go(Page.SCENARIOS) }
                        NavItem("پیشرفت", Icons.Default.Insights, page==Page.PROGRESS.name) { go(Page.PROGRESS) }
                    }
                }
            }
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                when (Page.valueOf(page)) {
                    Page.HOME -> HomeScreen(learned.size, scores, onTips={go(Page.TIPS)}, onScenarios={go(Page.SCENARIOS)}, onProgress={go(Page.PROGRESS)}, onSources={go(Page.SOURCES)})
                    Page.TIPS -> TipsScreen(query, {query=it}, filter, {filter=it}, learned, onOpen={selectedTip=it;go(Page.TIP_DETAIL)}, onSources={go(Page.SOURCES)})
                    Page.TIP_DETAIL -> TipDetailScreen(LearningContent.tips.first{it.id==selectedTip}, selectedTip, learned.contains(selectedTip.toString()), onBack={go(Page.TIPS)}, onLearn={
                        val next=learned.toMutableSet().apply { add(selectedTip.toString()) }.toSet()
                        learned=next; prefs.edit().putStringSet("learned", next).apply()
                    }, onNext={selectedTip=if(selectedTip>=LearningContent.tips.size)1 else selectedTip+1})
                    Page.SCENARIOS -> ScenariosScreen(scores, ::passed, onStart={id->selectedScenario=id;questionIndex=0;selectedAnswer=-1;answers=emptyList();go(Page.QUIZ)}, onHome={go(Page.HOME)})
                    Page.QUIZ -> QuizScreen(scenario, questionIndex, selectedAnswer, onSelect={selectedAnswer=it}, onNext={
                        answers=answers + selectedAnswer
                        if(questionIndex < scenario.questions.lastIndex){questionIndex++;selectedAnswer=-1}
                        else { val finalAnswers=answers + selectedAnswer; val finalScore=finalAnswers.indices.count{finalAnswers[it]==scenario.questions[it].answer}*100/scenario.questions.size; saveScore(selectedScenario,finalScore); answers=finalAnswers;go(Page.RESULT) }
                    }, onBack={go(Page.SCENARIOS)})
                    Page.RESULT -> ResultScreen(scenario, answers, scoreFor(selectedScenario) ?: score, onRetry={questionIndex=0;selectedAnswer=-1;answers=emptyList();go(Page.QUIZ)}, onScenarios={go(Page.SCENARIOS)})
                    Page.PROGRESS -> ProgressScreen(learned.size, scores, onSources={go(Page.SOURCES)})
                    Page.SOURCES -> SourcesScreen(onBack={go(Page.HOME)})
                }
            }
        }
    }
}

@Composable
private fun NavItem(label:String, icon:ImageVector, selected:Boolean, onClick:()->Unit) {
    NavigationBarItem(selected=selected,onClick=onClick,icon={Icon(icon,null)},label={Text(label,fontSize=10.sp)},colors=NavigationBarItemDefaults.colors(selectedIconColor=Yellow,selectedTextColor=Yellow,unselectedIconColor=Muted,unselectedTextColor=Muted,indicatorColor=Yellow.copy(alpha=.12f)))
}

@Composable
private fun ScreenColumn(content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal=18.dp, vertical=12.dp), content=content)
}

@Composable
private fun TopBrand() {
    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween) {
        Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(42.dp).clip(RoundedCornerShape(15.dp)).background(Brush.linearGradient(listOf(Yellow,Color(0xFFF19B54)))),contentAlignment=Alignment.Center) {
                Text("ک",color=Bg,fontSize=23.sp,fontWeight=FontWeight.Black)
            }
            Column {
                Text("کرامت‌یار",fontSize=15.sp,fontWeight=FontWeight.Bold,color=Ink)
                Text("آموزش مراقبت محترمانه مادر",fontSize=9.sp,color=Muted)
            }
        }
        Box(Modifier.size(38.dp).clip(CircleShape).background(Panel2),contentAlignment=Alignment.Center) { Icon(Icons.Default.Person,null,tint=Yellow) }
    }
}

@Composable
private fun HomeScreen(learned:Int,scores:String,onTips:()->Unit,onScenarios:()->Unit,onProgress:()->Unit,onSources:()->Unit) {
    val done = scores.split(",").count { it.contains(":") && (it.substringAfter(":").toIntOrNull() ?: 0) >= 60 }
    ScreenColumn {
        TopBrand()
        Spacer(Modifier.height(18.dp))
        Text("خوش آمدید، دانشجوی مامایی",fontSize=10.sp,color=Muted)
        Text("یادگیریِ مراقبت محترمانه",fontSize=22.sp,fontWeight=FontWeight.Bold,color=Ink)
        Spacer(Modifier.height(14.dp))
        Card(shape=RoundedCornerShape(26.dp),colors=CardDefaults.cardColors(containerColor=Panel2),modifier=Modifier.fillMaxWidth()) {
            Box(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(Color(0xFF433651),Color(0xFF282944)))).padding(20.dp)) {
                Row(verticalAlignment=Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("مسیر یادگیری دانشجوی مامایی",fontSize=9.sp,color=Yellow,fontWeight=FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Text("کرامت مادر،\nبخشی از مراقبت است.",fontSize=21.sp,lineHeight=31.sp,fontWeight=FontWeight.Bold,color=Ink)
                        Spacer(Modifier.height(5.dp))
                        Text("از منشور کرامت مادری ایران یاد بگیرید و در سناریوهای آموزشی مبتنی بر WHO تمرین کنید.",fontSize=9.sp,lineHeight=18.sp,color=Color(0xFFE1DCEC))
                        Spacer(Modifier.height(12.dp))
                        Button(onClick=onTips,colors=ButtonDefaults.buttonColors(containerColor=Yellow,contentColor=Bg),shape=RoundedCornerShape(12.dp),contentPadding=PaddingValues(horizontal=16.dp,vertical=8.dp)) { Text("شروع یادگیری",fontSize=10.sp,fontWeight=FontWeight.Bold); Spacer(Modifier.width(6.dp)); Icon(Icons.Default.ArrowBack,null,Modifier.size(15.dp)) }
                    }
                    Spacer(Modifier.width(4.dp))
                    Box(Modifier.size(94.dp).clip(CircleShape).background(Yellow.copy(alpha=.12f)),contentAlignment=Alignment.Center) {
                        Icon(Icons.Default.Favorite,contentDescription=null,modifier=Modifier.size(58.dp),tint=Color(0xFFFFA0A0))
                    }
                }
            }
        }
        Spacer(Modifier.height(13.dp))
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp),modifier=Modifier.fillMaxWidth()) {
            StatTile("$learned / ۲۱","نکته آموخته‌شده",Modifier.weight(1f))
            StatTile("$done / ۷","سناریوی تکمیل‌شده",Modifier.weight(1f))
            StatTile("${(learned*100/21)}٪","پیشرفت آموزش",Modifier.weight(1f))
        }
        SectionTitle("مسیرهای اصلی یادگیری","دو مسیر برای یادگیری و تمرین")
        Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
            PathCard("نکات آموزشی","فلش‌کارت و سرفصل‌ها","▤",Yellow,Modifier.weight(1f),onTips)
            PathCard("سناریوهای WHO","۷ مرحله تصمیم‌گیری","◇",Mint,Modifier.weight(1f),onScenarios)
        }
        SectionTitle("سه محور منشور","چارچوب اصلی تکریم مادر")
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            AxisTile("ارتباط","کلامی و غیرکلامی",Yellow,Modifier.weight(1f))
            AxisTile("فرآیند","خدمت‌رسانی",Mint,Modifier.weight(1f))
            AxisTile("محیط","فضای فیزیکی",Lilac,Modifier.weight(1f))
        }
        SectionTitle("مرور و پیشرفت","یادگیری در مسیر شما")
        Card(colors=CardDefaults.cardColors(containerColor=Panel),shape=RoundedCornerShape(18.dp),modifier=Modifier.fillMaxWidth().clickable(onClick=onProgress)) {
            Row(Modifier.padding(14.dp),verticalAlignment=Alignment.CenterVertically) {
                Icon(Icons.Default.Refresh,null,tint=Yellow,modifier=Modifier.size(28.dp))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) { Text("مرور نکات و مشاهده پیشرفت",fontSize=10.sp,fontWeight=FontWeight.Bold); Text("وضعیت یادگیری روی همین دستگاه ذخیره می‌شود.",fontSize=8.sp,color=Muted) }
                Icon(Icons.Default.ChevronLeft,null,tint=Yellow)
            }
        }
        TextButton(onClick=onSources,modifier=Modifier.align(Alignment.CenterHorizontally)) { Text("منابع علمی و درباره برنامه",color=Muted,fontSize=9.sp) }
        Notice("این برنامه ابزار آموزشی است؛ جایگزین راهنماهای بالینی جاری وزارت بهداشت، سیاست مرکز درمانی یا نظارت استاد نیست.")
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun StatTile(value:String,label:String,modifier:Modifier=Modifier) {
    Card(modifier,shape=RoundedCornerShape(15.dp),colors=CardDefaults.cardColors(containerColor=Panel)) {
        Column(Modifier.fillMaxWidth().padding(vertical=12.dp,horizontal=4.dp),horizontalAlignment=Alignment.CenterHorizontally) {
            Text(value,color=Yellow,fontSize=17.sp,fontWeight=FontWeight.Bold)
            Text(label,color=Muted,fontSize=8.sp,textAlign=TextAlign.Center)
        }
    }
}
@Composable
private fun SectionTitle(title:String,sub:String) {
    Column(Modifier.padding(top=21.dp,bottom=10.dp)) { Text(title,fontSize=14.sp,fontWeight=FontWeight.Bold); Text(sub,fontSize=9.sp,color=Muted) }
}
@Composable
private fun PathCard(title:String,sub:String,icon:String,color:Color,modifier:Modifier=Modifier,onClick:()->Unit) {
    Card(modifier.height(130.dp).clickable(onClick=onClick),shape=RoundedCornerShape(19.dp),colors=CardDefaults.cardColors(containerColor=color)) {
        Column(Modifier.fillMaxSize().padding(14.dp),verticalArrangement=Arrangement.SpaceBetween) {
            Box(Modifier.size(34.dp).clip(RoundedCornerShape(11.dp)).background(Color.White.copy(alpha=.35f)),contentAlignment=Alignment.Center){Text(icon,color=Bg,fontSize=20.sp)}
            Column { Text(title,color=Bg,fontSize=11.sp,fontWeight=FontWeight.Bold); Text(sub,color=Bg.copy(alpha=.75f),fontSize=8.sp) }
        }
    }
}
@Composable
private fun AxisTile(title:String,sub:String,color:Color,modifier:Modifier) {
    Card(modifier,shape=RoundedCornerShape(14.dp),colors=CardDefaults.cardColors(containerColor=Panel)) {
        Column(Modifier.fillMaxWidth().padding(9.dp),horizontalAlignment=Alignment.CenterHorizontally) {
            Box(Modifier.size(27.dp).clip(CircleShape).background(color.copy(alpha=.18f)),contentAlignment=Alignment.Center){Icon(Icons.Default.Favorite,null,tint=color,modifier=Modifier.size(15.dp))}
            Text(title,fontSize=9.sp,fontWeight=FontWeight.Bold)
            Text(sub,fontSize=7.sp,color=Muted,textAlign=TextAlign.Center)
        }
    }
}
@Composable
private fun Notice(text:String) {
    Card(colors=CardDefaults.cardColors(containerColor=Yellow.copy(alpha=.09f)),shape=RoundedCornerShape(13.dp),modifier=Modifier.fillMaxWidth().padding(top=12.dp)) {
        Text(text,Modifier.padding(12.dp),fontSize=9.sp,color=Color(0xFFF4E5B4),lineHeight=18.sp)
    }
}

@Composable
private fun TipsScreen(query:String,onQuery:(String)->Unit,filter:String,onFilter:(String)->Unit,learned:Set<String>,onOpen:(Int)->Unit,onSources:()->Unit) {
    val list=LearningContent.tips.filter { (filter=="همه" || it.axis==filter) && (query.isBlank() || (it.title+it.body).contains(query,true)) }
    ScreenColumn {
        TopBrand(); Spacer(Modifier.height(18.dp))
        Text("نکات آموزشی",fontSize=21.sp,fontWeight=FontWeight.Bold)
        Text("فلش‌کارت‌های مبتنی بر منشور کرامت مادری ایران",fontSize=9.sp,color=Muted)
        Spacer(Modifier.height(13.dp))
        Card(colors=CardDefaults.cardColors(containerColor=Panel),shape=RoundedCornerShape(16.dp)) {
            Column(Modifier.padding(13.dp)) {
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text("پیشرفت یادگیری",fontSize=9.sp,fontWeight=FontWeight.Bold);Text("${learned.size} از ۲۱",fontSize=9.sp,color=Yellow)}
                Spacer(Modifier.height(8.dp)); LinearProgressIndicator(progress={learned.size/21f},modifier=Modifier.fillMaxWidth().height(7.dp).clip(CircleShape),color=Yellow,trackColor=Color.White.copy(alpha=.1f))
            }
        }
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(value=query,onValueChange=onQuery,modifier=Modifier.fillMaxWidth(),placeholder={Text("جست‌وجو در نکات آموزشی…",fontSize=10.sp,color=Muted)},singleLine=true,shape=RoundedCornerShape(13.dp),colors=OutlinedTextFieldDefaults.colors(focusedBorderColor=Yellow,unfocusedBorderColor=Color.White.copy(alpha=.16f),focusedTextColor=Ink,unfocusedTextColor=Ink))
        Spacer(Modifier.height(9.dp))
        Row(horizontalArrangement=Arrangement.spacedBy(6.dp),modifier=Modifier.horizontalScroll(rememberScrollState())) {
            listOf("همه","ارتباط","فرآیند","محیط").forEach { FilterChip(selected=filter==it,onClick={onFilter(it)},label={Text(it,fontSize=9.sp)},colors=FilterChipDefaults.filterChipColors(selectedContainerColor=Yellow,selectedLabelColor=Bg,containerColor=Panel,labelColor=Muted),border=FilterChipDefaults.filterChipBorder(enabled=true,selected=filter==it,borderColor=Color.White.copy(alpha=.12f),selectedBorderColor=Yellow)) }
        }
        SectionTitle("فلش‌کارت‌ها","${list.size} نکته آموزشی")
        list.forEach { tip ->
            TipRow(tip,learned.contains(tip.id.toString())) { onOpen(tip.id) }
        }
        SectionTitle("سرفصل‌های رسمی منشور","۱۰ ساعت آموزش نظری")
        ModuleRow("۱","کرامت مادر: دین، اخلاق و حقوق","۲ ساعت")
        ModuleRow("۲","مراقبت‌های مامایی و اخلاق حرفه‌ای قبل از بارداری، حین و پس از زایمان و کلاس‌های آمادگی برای زایمان","۱ ساعت")
        ModuleRow("۳","مهارت‌های ارتباطی؛ ارتباط کلامی و غیرکلامی با مادر با تأکید بر کرامت مادری","۳ ساعت")
        ModuleRow("۴","قصور در مراقبت‌های مامایی از دیدگاه اخلاق حرفه‌ای","۲ ساعت")
        ModuleRow("۵","اهمیت دلبستگی پره‌ناتال در سلامت مادر و کودک و بهبود آن با مهارت شفقت‌ورزی","۲ ساعت")
        ModuleRow("+","دوره توجیهی تمامی کارکنان","۲ ساعت")
        TextButton(onClick=onSources,modifier=Modifier.align(Alignment.CenterHorizontally)){Text("مشاهده منابع",color=Yellow,fontSize=9.sp)}
        Spacer(Modifier.height(20.dp))
    }
}
@Composable
private fun TipRow(tip:Tip,learned:Boolean,onClick:()->Unit) {
    val c=when(tip.axis){"ارتباط"->Yellow;"فرآیند"->Mint;else->Lilac}
    Card(modifier=Modifier.fillMaxWidth().padding(vertical=4.dp).clickable(onClick=onClick),shape=RoundedCornerShape(16.dp),colors=CardDefaults.cardColors(containerColor=Panel)) {
        Row(Modifier.padding(12.dp),verticalAlignment=Alignment.CenterVertically) {
            Box(Modifier.size(43.dp).clip(RoundedCornerShape(13.dp)).background(c.copy(alpha=.16f)),contentAlignment=Alignment.Center){Text(tip.icon,color=c,fontSize=17.sp,fontWeight=FontWeight.Bold)}
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text(tip.title,fontSize=10.sp,fontWeight=FontWeight.Bold)
                Text(tip.body.take(75)+"…",fontSize=8.sp,color=Muted,maxLines=2,lineHeight=14.sp)
                Text(if(learned)"✓ آموخته‌شده" else tip.axis+" • برای یادگیری",fontSize=8.sp,color=c)
            }
            Icon(Icons.Default.ChevronLeft,null,tint=Yellow)
        }
    }
}
@Composable
private fun ModuleRow(no:String,title:String,hours:String) {
    Card(Modifier.fillMaxWidth().padding(vertical=4.dp),shape=RoundedCornerShape(14.dp),colors=CardDefaults.cardColors(containerColor=Panel)) {
        Row(Modifier.padding(12.dp),verticalAlignment=Alignment.CenterVertically) {
            Box(Modifier.size(32.dp).clip(RoundedCornerShape(10.dp)).background(Yellow.copy(alpha=.13f)),contentAlignment=Alignment.Center){Text(no,color=Yellow,fontSize=10.sp,fontWeight=FontWeight.Bold)}
            Spacer(Modifier.width(9.dp)); Text(title,Modifier.weight(1f),fontSize=9.sp,lineHeight=16.sp)
            Text(hours,color=Yellow,fontSize=8.sp)
        }
    }
}
@Composable
private fun TipDetailScreen(tip:Tip,id:Int,learned:Boolean,onBack:()->Unit,onLearn:()->Unit,onNext:()->Unit) {
    ScreenColumn {
        TextButton(onClick=onBack){Icon(Icons.Default.ArrowForward,null,tint=Yellow);Text("بازگشت به نکات",color=Yellow,fontSize=10.sp)}
        Spacer(Modifier.height(14.dp))
        Card(Modifier.fillMaxWidth().heightIn(min=270.dp),shape=RoundedCornerShape(25.dp),colors=CardDefaults.cardColors(containerColor=Yellow)) {
            Column(Modifier.fillMaxWidth().padding(22.dp),verticalArrangement=Arrangement.SpaceBetween) {
                Text("KARAMATYAR • LEARNING CARD",fontSize=9.sp,color=Bg.copy(alpha=.65f),fontWeight=FontWeight.Bold)
                Spacer(Modifier.height(36.dp))
                Text(tip.title,fontSize=23.sp,color=Bg,fontWeight=FontWeight.Bold,lineHeight=35.sp)
                Spacer(Modifier.height(14.dp))
                Text(tip.body,fontSize=13.sp,color=Bg,lineHeight=26.sp)
                Spacer(Modifier.height(36.dp))
                Text("منبع: ${tip.source}",fontSize=9.sp,color=Bg.copy(alpha=.8f))
            }
        }
        Spacer(Modifier.height(13.dp))
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            Button(onClick=onLearn,modifier=Modifier.weight(1f),colors=ButtonDefaults.buttonColors(containerColor=Mint,contentColor=Bg),shape=RoundedCornerShape(12.dp)) { Text(if(learned)"✓ آموخته‌ام" else "این نکته را آموختم",fontSize=10.sp) }
            OutlinedButton(onClick=onNext,modifier=Modifier.weight(1f),border=BorderStroke(1.dp,Yellow),shape=RoundedCornerShape(12.dp)) { Text("نکته بعدی",fontSize=10.sp,color=Yellow) }
        }
        Notice("نکته‌های آموخته‌شده برای مرور دوره‌ای نگهداری می‌شوند. این نسخه یادآور درون‌برنامه‌ای دارد و هنوز اعلان زمان‌بندی‌شده سیستم‌عامل فعال نیست.")
    }
}

@Composable
private fun ScenariosScreen(scores:String,passed:(Int)->Boolean,onStart:(Int)->Unit,onHome:()->Unit) {
    ScreenColumn {
        TopBrand(); Spacer(Modifier.height(18.dp))
        Text("سناریوهای آموزشی",fontSize=21.sp,fontWeight=FontWeight.Bold)
        Text("۷ مرحله تصمیم‌گیری؛ طراحی آموزشی با الهام از نمونه و محتوای مبتنی بر WHO",fontSize=9.sp,color=Muted)
        Spacer(Modifier.height(12.dp))
        val done=LearningContent.scenarios.count { passed(it.id) }
        Card(colors=CardDefaults.cardColors(containerColor=Panel),shape=RoundedCornerShape(16.dp)) {
            Column(Modifier.padding(14.dp)) {
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text("پیشرفت سناریوها",fontSize=9.sp,fontWeight=FontWeight.Bold);Text("$done از ۷",fontSize=9.sp,color=Yellow)}
                Spacer(Modifier.height(8.dp));LinearProgressIndicator(progress={done/7f},modifier=Modifier.fillMaxWidth().height(7.dp).clip(CircleShape),color=Mint,trackColor=Color.White.copy(alpha=.1f))
                Text("برای باز شدن مرحله بعد حداقل ۶۰٪ امتیاز لازم است. مراحل قبلی همیشه قابل مرورند.",fontSize=8.sp,color=Muted)
            }
        }
        Spacer(Modifier.height(10.dp))
        LearningContent.scenarios.forEach { s ->
            val unlocked=s.id==1 || passed(s.id-1)
            val sc=scores.split(",").firstOrNull{it.startsWith("${s.id}:")}?.substringAfter(":")?.toIntOrNull()
            ScenarioRow(s,unlocked,sc){onStart(s.id)}
        }
        Notice("سناریوها تمرین آموزشی‌اند، نه دستور بالینی. در موقعیت واقعی، راهنمای بالینی جاری، سیاست مرکز و نظر استاد/مسئول بالینی ملاک است.")
        Spacer(Modifier.height(20.dp))
    }
}
@Composable
private fun ScenarioRow(s:Scenario,unlocked:Boolean,score:Int?,onClick:()->Unit) {
    Card(Modifier.fillMaxWidth().padding(vertical=5.dp),shape=RoundedCornerShape(16.dp),colors=CardDefaults.cardColors(containerColor=Panel)) {
        Row(Modifier.padding(12.dp),verticalAlignment=Alignment.CenterVertically) {
            Box(Modifier.size(42.dp).clip(RoundedCornerShape(13.dp)).background(Blue.copy(alpha=.15f)),contentAlignment=Alignment.Center){Text(s.id.toString().padStart(2,'0'),color=Blue,fontSize=12.sp,fontWeight=FontWeight.Bold)}
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(s.title,fontSize=10.sp,fontWeight=FontWeight.Bold)
                Text(s.subtitle,fontSize=8.sp,color=Muted)
                Text(if(score!=null)"نمره: $score٪" else if(unlocked)"آماده شروع" else "قفل • ابتدا مرحله قبلی را بگذرانید",fontSize=8.sp,color=Yellow)
            }
            Button(onClick=onClick,enabled=unlocked,shape=RoundedCornerShape(10.dp),colors=ButtonDefaults.buttonColors(containerColor=Yellow,contentColor=Bg,disabledContainerColor=Panel2,disabledContentColor=Muted),contentPadding=PaddingValues(horizontal=12.dp,vertical=7.dp)){Text(if(score!=null)"مرور" else "شروع",fontSize=8.sp)}
        }
    }
}

@Composable
private fun QuizScreen(s:Scenario,index:Int,selected:Int,onSelect:(Int)->Unit,onNext:()->Unit,onBack:()->Unit) {
    val q=s.questions[index]
    ScreenColumn {
        TextButton(onClick=onBack){Icon(Icons.Default.ArrowForward,null,tint=Yellow);Text("بازگشت به سناریوها",color=Yellow,fontSize=10.sp)}
        Text(s.title,fontSize=20.sp,fontWeight=FontWeight.Bold)
        Text(s.subtitle,fontSize=9.sp,color=Muted)
        Spacer(Modifier.height(13.dp))
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text("پرسش ${index+1} از ${s.questions.size}",fontSize=9.sp,color=Muted);Text("مرحله ${s.id} از ۷",fontSize=9.sp,color=Yellow)}
        Spacer(Modifier.height(7.dp))
        LinearProgressIndicator(progress={(index+1).toFloat()/s.questions.size},modifier=Modifier.fillMaxWidth().height(7.dp).clip(CircleShape),color=Yellow,trackColor=Color.White.copy(alpha=.1f))
        Spacer(Modifier.height(14.dp))
        Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(20.dp),colors=CardDefaults.cardColors(containerColor=Panel)) {
            Column(Modifier.padding(15.dp)) {
                Text("سناریوی آموزشی",fontSize=8.sp,color=Yellow,fontWeight=FontWeight.Bold)
                Spacer(Modifier.height(9.dp))
                Text(q.prompt,fontSize=13.sp,fontWeight=FontWeight.Bold,lineHeight=24.sp)
                Spacer(Modifier.height(10.dp))
                q.options.forEachIndexed { i,opt ->
                    val correct=selected>=0 && i==q.answer
                    val wrong=selected==i && selected!=q.answer
                    val bg=when { correct->Green.copy(alpha=.14f); wrong->Coral.copy(alpha=.14f); selected==i->Yellow.copy(alpha=.12f); else->Panel2 }
                    val border=when { correct->Green; wrong->Coral; selected==i->Yellow; else->Color.White.copy(alpha=.10f) }
                    Row(Modifier.fillMaxWidth().padding(vertical=4.dp).clip(RoundedCornerShape(13.dp)).background(bg).border(1.dp,border,RoundedCornerShape(13.dp)).clickable(enabled=selected<0){onSelect(i)}.padding(12.dp),verticalAlignment=Alignment.Top) {
                        Box(Modifier.size(18.dp).clip(CircleShape).background(if(correct)Green else Color.Transparent).border(1.dp,border,CircleShape),contentAlignment=Alignment.Center){if(correct)Icon(Icons.Default.Check,null,tint=Bg,modifier=Modifier.size(12.dp))}
                        Spacer(Modifier.width(9.dp));Text(opt,Modifier.weight(1f),fontSize=9.sp,lineHeight=18.sp)
                    }
                }
                if(selected>=0) {
                    Spacer(Modifier.height(9.dp))
                    Card(colors=CardDefaults.cardColors(containerColor=if(selected==q.answer)Green.copy(alpha=.12f) else Coral.copy(alpha=.12f)),shape=RoundedCornerShape(12.dp)) {
                        Column(Modifier.padding(12.dp)) {
                            Text(if(selected==q.answer)"پاسخ درست" else "نیازمند بازنگری",fontSize=10.sp,fontWeight=FontWeight.Bold,color=if(selected==q.answer)Green else Coral)
                            Text(q.explanation,fontSize=9.sp,lineHeight=18.sp,color=Ink)
                            Text("منبع: ${q.source}",fontSize=8.sp,color=Muted)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Button(onClick=onNext,enabled=selected>=0,modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(12.dp),colors=ButtonDefaults.buttonColors(containerColor=Yellow,contentColor=Bg,disabledContainerColor=Panel2,disabledContentColor=Muted)) {
            Text(if(index==s.questions.lastIndex)"مشاهده نتیجه" else "پرسش بعدی",fontSize=10.sp,fontWeight=FontWeight.Bold)
            Spacer(Modifier.width(7.dp));Icon(Icons.Default.ArrowBack,null,Modifier.size(15.dp))
        }
        Notice("منبع این سناریو: ${s.source}. پاسخ‌ها برای آموزش طراحی شده‌اند و باید پیش از استفاده رسمی توسط هیئت علمی بازبینی شوند.")
    }
}

@Composable
private fun ResultScreen(s:Scenario,answers:List<Int>,score:Int,onRetry:()->Unit,onScenarios:()->Unit) {
    ScreenColumn {
        TextButton(onClick=onScenarios){Icon(Icons.Default.ArrowForward,null,tint=Yellow);Text("بازگشت به سناریوها",color=Yellow,fontSize=10.sp)}
        Spacer(Modifier.height(10.dp))
        Text(if(score>=60)"آفرین؛ مرحله تکمیل شد" else "یک بار دیگر تمرین کنید",modifier=Modifier.fillMaxWidth(),fontSize=19.sp,fontWeight=FontWeight.Bold,textAlign=TextAlign.Center)
        Text(s.title,modifier=Modifier.fillMaxWidth(),fontSize=9.sp,color=Muted,textAlign=TextAlign.Center)
        Spacer(Modifier.height(16.dp))
        Box(Modifier.fillMaxWidth(),contentAlignment=Alignment.Center) {
            Box(Modifier.size(132.dp).clip(CircleShape).background(Yellow.copy(alpha=.12f)).border(7.dp,Yellow.copy(alpha=.3f),CircleShape),contentAlignment=Alignment.Center) {
                Column(horizontalAlignment=Alignment.CenterHorizontally){Text("$score٪",fontSize=29.sp,color=Yellow,fontWeight=FontWeight.Bold);Text("${answers.indices.count{answers[it]==s.questions[it].answer} } پاسخ درست از ۳",fontSize=8.sp,color=Muted)}
            }
        }
        Text(if(score>=60)"حدنصاب ۶۰٪ را کسب کردید. مرحله بعد باز شد." else "برای باز شدن مرحله بعد حداقل ۶۰٪ لازم است. توضیحات را مرور کنید.",modifier=Modifier.fillMaxWidth(),fontSize=9.sp,color=Muted,textAlign=TextAlign.Center)
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp),modifier=Modifier.fillMaxWidth()) {
            Button(onClick=onRetry,modifier=Modifier.weight(1f),colors=ButtonDefaults.buttonColors(containerColor=Yellow,contentColor=Bg),shape=RoundedCornerShape(12.dp)){Text("تلاش دوباره",fontSize=9.sp)}
            OutlinedButton(onClick=onScenarios,modifier=Modifier.weight(1f),border=BorderStroke(1.dp,Yellow),shape=RoundedCornerShape(12.dp)){Text("فهرست سناریوها",fontSize=9.sp,color=Yellow)}
        }
        SectionTitle("مرور پاسخ‌ها","پاسخ و توضیح هر پرسش")
        s.questions.forEachIndexed { i,q ->
            Card(Modifier.fillMaxWidth().padding(vertical=4.dp),shape=RoundedCornerShape(14.dp),colors=CardDefaults.cardColors(containerColor=Panel)) {
                Column(Modifier.padding(12.dp)) {
                    Text("${i+1}. ${q.prompt}",fontSize=9.sp,fontWeight=FontWeight.Bold,lineHeight=17.sp)
                    Text(if(answers.getOrNull(i)==q.answer)"✓ پاسخ درست" else "✕ پاسخ انتخابی: ${q.options.getOrNull(answers.getOrNull(i) ?: -1) ?: "بدون پاسخ"}",fontSize=8.sp,color=if(answers.getOrNull(i)==q.answer)Green else Coral)
                    Text(q.explanation,fontSize=8.sp,color=Muted,lineHeight=17.sp)
                    Text("منبع: ${q.source}",fontSize=7.sp,color=Yellow)
                }
            }
        }
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun ProgressScreen(learned:Int,scores:String,onSources:()->Unit) {
    val done=scores.split(",").count { it.contains(":") && (it.substringAfter(":").toIntOrNull() ?: 0)>=60 }
    ScreenColumn {
        TopBrand();Spacer(Modifier.height(18.dp))
        Text("پیشرفت یادگیری",fontSize=21.sp,fontWeight=FontWeight.Bold)
        Text("وضعیت یادگیری روی همین گوشی ذخیره می‌شود.",fontSize=9.sp,color=Muted)
        Spacer(Modifier.height(14.dp))
        Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(20.dp),colors=CardDefaults.cardColors(containerColor=Panel)) {
            Column(Modifier.fillMaxWidth().padding(18.dp),horizontalAlignment=Alignment.CenterHorizontally) {
                Text("${learned*100/21}٪",fontSize=32.sp,color=Yellow,fontWeight=FontWeight.Bold)
                Text("پیشرفت فلش‌کارت‌ها",fontSize=10.sp,fontWeight=FontWeight.Bold)
                Spacer(Modifier.height(10.dp));LinearProgressIndicator(progress={learned/21f},modifier=Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),color=Yellow,trackColor=Color.White.copy(alpha=.1f))
                Spacer(Modifier.height(14.dp))
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceEvenly) {
                    StatTile("$learned","آموخته‌شده",Modifier.weight(1f))
                    Spacer(Modifier.width(8.dp));StatTile("$done / ۷","سناریو",Modifier.weight(1f))
                }
            }
        }
        SectionTitle("وضعیت سناریوها","نمره هر مرحله")
        LearningContent.scenarios.forEach { s ->
            val sc=scores.split(",").firstOrNull{it.startsWith("${s.id}:")}?.substringAfter(":")?.toIntOrNull()
            Card(Modifier.fillMaxWidth().padding(vertical=4.dp),shape=RoundedCornerShape(14.dp),colors=CardDefaults.cardColors(containerColor=Panel)) {
                Column(Modifier.padding(12.dp)) {
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text("${s.id}. ${s.title}",fontSize=9.sp,fontWeight=FontWeight.Bold);Text(if(sc==null)"—" else "$sc٪",fontSize=9.sp,color=Yellow)}
                    Spacer(Modifier.height(7.dp));LinearProgressIndicator(progress={(sc?:0)/100f},modifier=Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),color=Mint,trackColor=Color.White.copy(alpha=.1f))
                    Text(if((sc?:0)>=60)"تکمیل‌شده" else "هنوز تکمیل نشده",fontSize=8.sp,color=Muted)
                }
            }
        }
        Notice("اطلاعات پیشرفت فقط در حافظه داخلی این گوشی ذخیره می‌شود. با حذف داده‌های برنامه ممکن است پاک شود؛ همگام‌سازی ابری در این نسخه وجود ندارد.")
        TextButton(onClick=onSources,modifier=Modifier.align(Alignment.CenterHorizontally)){Text("منابع علمی",color=Yellow,fontSize=9.sp)}
    }
}

@Composable
private fun SourcesScreen(onBack:()->Unit) {
    ScreenColumn {
        TextButton(onClick=onBack){Icon(Icons.Default.ArrowForward,null,tint=Yellow);Text("بازگشت",color=Yellow,fontSize=10.sp)}
        Text("منابع علمی و ملاحظات",fontSize=21.sp,fontWeight=FontWeight.Bold)
        Text("تفکیک منبع اصلی ایرانی، منبع تکمیلی WHO و مرجع طراحی",fontSize=9.sp,color=Muted)
        Spacer(Modifier.height(14.dp))
        SourceCard("۱. منشور کرامت مادری ایران","منبع اصلی ساختار آموزشی، سه محور تکریم و پنج سرفصل رسمی. فایل ارسالی کاربر.")
        SourceCard("۲. WHO — Compendium on respectful maternal and newborn care (2025)","منبع تکمیلی مفاهیم مراقبت محترمانه، ارتباط مؤثر، همراه منتخب، حریم خصوصی، محرمانگی و رضایت آگاهانه.")
        SourceCard("۳. Maghalian et al. (2025)","Developing and testing a mobile application for imparting knowledge: the positive birth experience guideline to midwifery students. BMC Medical Education, 25:577. DOI: 10.1186/s12909-025-07147-1. فقط مرجع طراحی آموزشی و تعامل.")
        Notice("سناریوها بازنویسی آموزشی بر پایه مفاهیم WHO هستند و متن رسمی راهنمای بالینی محسوب نمی‌شوند. پیش از استفاده دانشگاهی، پرسش‌ها و پاسخ‌ها باید توسط هیئت علمی مامایی و متخصصان اخلاق حرفه‌ای بازبینی شوند.")
        Text("نسخه ۱.۱ • کرامت‌یار • اجرای آفلاین • ذخیره‌سازی محلی",fontSize=8.sp,color=Muted)
    }
}
@Composable
private fun SourceCard(title:String,body:String) {
    Card(Modifier.fillMaxWidth().padding(vertical=5.dp),shape=RoundedCornerShape(15.dp),colors=CardDefaults.cardColors(containerColor=Panel)) {
        Column(Modifier.padding(13.dp)) { Text(title,fontSize=10.sp,fontWeight=FontWeight.Bold,color=Yellow);Spacer(Modifier.height(5.dp));Text(body,fontSize=9.sp,color=Muted,lineHeight=18.sp) }
    }
}
