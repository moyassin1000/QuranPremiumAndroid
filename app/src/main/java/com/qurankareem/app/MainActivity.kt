package com.qurankareem.app

import android.Manifest
import android.app.AlarmManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Navigation
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.produceState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.qurankareem.core.audio.AudioDownloadStore
import com.qurankareem.core.audio.HifzReciter
import com.qurankareem.core.audio.buildHifzMediaItems
import com.qurankareem.core.audio.parseHifzMediaId
import com.qurankareem.core.audio.QuranReciter
import com.qurankareem.core.design.QuranPremiumTheme
import com.qurankareem.core.prayer.PrayerCalculator
import com.qurankareem.core.practice.PracticeState
import com.qurankareem.core.practice.PracticeStore
import com.qurankareem.core.settings.AppSettings
import com.qurankareem.core.settings.PrayerKey
import com.qurankareem.core.settings.SettingsStore
import com.qurankareem.core.settings.ThemeMode
import com.qurankareem.core.stats.AppStats
import com.qurankareem.core.stats.StatsStore
import com.qurankareem.feature.audio.AudioScreen
import com.qurankareem.feature.audio.DownloadsScreen
import com.qurankareem.feature.adhkar.AdhkarScreen
import com.qurankareem.feature.home.HomeScreen
import com.qurankareem.feature.hifz.HifzScreen
import com.qurankareem.feature.khatma.KhatmaScreen
import com.qurankareem.feature.mushaf.MushafScreen
import com.qurankareem.feature.mushaf.QuranReaderScreen
import com.qurankareem.feature.mushaf.ReaderKind
import com.qurankareem.feature.prayer.PrayerScreen
import com.qurankareem.feature.qibla.QiblaScreen
import com.qurankareem.feature.settings.SettingsScreen
import com.qurankareem.feature.stats.StatsScreen
import com.qurankareem.app.widget.WidgetSnapshotPublisher
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate

class MainActivity : ComponentActivity() {
    private val externalDestination = MutableStateFlow<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        externalDestination.value = intent?.getStringExtra(EXTRA_DESTINATION)
        enableEdgeToEdge()
        setContent {
            val store = remember { SettingsStore(applicationContext) }
            val settings by store.settings.collectAsStateWithLifecycle(initialValue = AppSettings())
            val systemDark = isSystemInDarkTheme()
            QuranPremiumTheme(
                darkTheme = settings.themeMode == ThemeMode.DARK || (settings.themeMode == ThemeMode.SYSTEM && systemDark),
                amoled = settings.themeMode == ThemeMode.AMOLED,
            ) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    QuranApp(
                        settingsStore = store,
                        settings = settings,
                        externalDestination = externalDestination,
                        onExternalDestinationHandled = { externalDestination.value = null },
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        externalDestination.value = intent.getStringExtra(EXTRA_DESTINATION)
    }
}

private const val EXTRA_DESTINATION = "destination"

private data class Destination(
    val route: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
)

private val bottomDestinations = listOf(
    Destination("home", "الرئيسية", Icons.Outlined.Home),
    Destination("mushaf", "المصحف", Icons.Outlined.MenuBook),
    Destination("audio", "الاستماع", Icons.Outlined.Headphones),
    Destination("qibla", "القبلة", Icons.Outlined.Navigation),
    Destination("more", "المزيد", Icons.Outlined.MoreHoriz),
)

private data class PlayerSnapshot(
    val mediaId: String? = null,
    val title: String = "",
    val artist: String = "",
    val isPlaying: Boolean = false,
    val playbackSpeed: Float = 1f,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
    val currentIndex: Int = 0,
    val mediaItemCount: Int = 0,
)

@Composable
private fun QuranApp(
    settingsStore: SettingsStore,
    settings: AppSettings,
    externalDestination: StateFlow<String?>,
    onExternalDestinationHandled: () -> Unit,
) {
    val navController = rememberNavController()
    val requestedDestination by externalDestination.collectAsStateWithLifecycle()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val readingStore = remember { ReadingStore(context.applicationContext) }
    val readingState by readingStore.state.collectAsStateWithLifecycle(initialValue = ReadingState())
    val practiceStore = remember { PracticeStore(context.applicationContext) }
    val practiceState by practiceStore.state.collectAsStateWithLifecycle(initialValue = PracticeState())
    val statsStore = remember { StatsStore(context.applicationContext) }
    val statsState by statsStore.state.collectAsStateWithLifecycle(initialValue = AppStats())
    val scope = rememberCoroutineScope()
    val showBottomBar = currentRoute?.startsWith("reader/") != true
    val audioDownloads = remember { AudioDownloadStore(context.applicationContext) }
    val locationRepository = remember { LocationRepository(context.applicationContext) }
    val alarmScheduler = remember { PrayerAlarmScheduler(context.applicationContext) }
    var appLocation by remember { mutableStateOf(locationRepository.saved()) }
    var canScheduleExactAlarms by remember { mutableStateOf(alarmScheduler.canScheduleExact()) }
    var backupStatus by remember { mutableStateOf<String?>(null) }
    var pendingBackupPayload by remember { mutableStateOf<String?>(null) }

    val backupCreateLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        val payload = pendingBackupPayload
        if (uri == null || payload == null) {
            if (uri == null) backupStatus = "تم إلغاء إنشاء النسخة الاحتياطية"
            return@rememberLauncherForActivityResult
        }
        runCatching {
            context.contentResolver.openOutputStream(uri, "wt")?.use { stream ->
                stream.write(payload.toByteArray(Charsets.UTF_8))
            } ?: error("تعذر فتح الملف للكتابة")
        }.onSuccess {
            backupStatus = "تم حفظ النسخة الاحتياطية بنجاح"
        }.onFailure {
            backupStatus = "تعذر حفظ النسخة الاحتياطية: ${it.message ?: "خطأ غير معروف"}"
        }
        pendingBackupPayload = null
    }

    val backupRestoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            runCatching {
                val raw = context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                    ?: error("تعذر قراءة ملف النسخة الاحتياطية")
                UserBackupManager.restoreJson(
                    rawJson = raw,
                    readingStore = readingStore,
                    settingsStore = settingsStore,
                    practiceStore = practiceStore,
                    statsStore = statsStore,
                    locationRepository = locationRepository,
                )
            }.onSuccess { restoredLocation ->
                appLocation = restoredLocation
                backupStatus = "تمت استعادة البيانات بنجاح"
            }.onFailure {
                backupStatus = "تعذر الاستعادة: ${it.message ?: "ملف غير صالح"}"
            }
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) canScheduleExactAlarms = alarmScheduler.canScheduleExact()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(settings, appLocation, canScheduleExactAlarms) {
        alarmScheduler.reschedule(settings, appLocation)
    }

    LaunchedEffect(readingState, practiceState, appLocation) {
        WidgetSnapshotPublisher.publish(context.applicationContext, readingState, practiceState, appLocation)
    }

    LaunchedEffect(requestedDestination) {
        val destination = requestedDestination ?: return@LaunchedEffect
        val valid = destination in setOf("home", "mushaf", "audio", "qibla", "more", "prayer", "khatma", "hifz", "adhkar", "downloads", "stats", "settings") ||
            Regex("reader/page/(?:[1-9]|[1-9]\\d|[1-5]\\d{2}|60[0-4])").matches(destination)
        if (valid) {
            navController.navigate(destination) { launchSingleTop = true }
        }
        onExternalDestinationHandled()
    }

    val prayerSummary by produceState(
        initialValue = Pair("الصلاة القادمة", "--:--"),
        appLocation.latitude,
        appLocation.longitude,
    ) {
        while (true) {
            val now = System.currentTimeMillis()
            val today = LocalDate.now()
            val day = PrayerCalculator.calculate(appLocation.latitude, appLocation.longitude, today)
            val next = day.all.firstOrNull { it.name != "الشروق" && it.epochMillis > now }
                ?: PrayerCalculator.calculate(appLocation.latitude, appLocation.longitude, today.plusDays(1)).fajr
            value = next.name to PrayerCalculator.format(next.epochMillis)
            delay(30_000)
        }
    }

    var controller by remember { mutableStateOf<MediaController?>(null) }
    var playerSnapshot by remember { mutableStateOf(PlayerSnapshot()) }
    var sleepDeadlineMillis by remember { mutableLongStateOf(0L) }
    var sleepJob by remember { mutableStateOf<Job?>(null) }

    DisposableEffect(Unit) {
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        future.addListener({
            runCatching { future.get() }.onSuccess { mediaController ->
                controller = mediaController
                fun refresh() {
                    playerSnapshot = PlayerSnapshot(
                        mediaId = mediaController.currentMediaItem?.mediaId,
                        title = mediaController.currentMediaItem?.mediaMetadata?.title?.toString().orEmpty(),
                        artist = mediaController.currentMediaItem?.mediaMetadata?.artist?.toString().orEmpty(),
                        isPlaying = mediaController.isPlaying,
                        playbackSpeed = mediaController.playbackParameters.speed,
                        repeatMode = mediaController.repeatMode,
                        currentIndex = mediaController.currentMediaItemIndex.coerceAtLeast(0),
                        mediaItemCount = mediaController.mediaItemCount.coerceAtLeast(0),
                    )
                }
                val listener = object : Player.Listener {
                    override fun onIsPlayingChanged(isPlaying: Boolean) = refresh()
                    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) = refresh()
                    override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) = refresh()
                    override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) = refresh()
                    override fun onRepeatModeChanged(repeatMode: Int) = refresh()
                }
                mediaController.addListener(listener)
                refresh()
            }
        }, ContextCompat.getMainExecutor(context))
        onDispose {
            sleepJob?.cancel()
            runCatching { MediaController.releaseFuture(future) }
            controller = null
        }
    }

    LaunchedEffect(controller, settings.playbackSpeed, settings.repeatOne) {
        controller?.let {
            it.setPlaybackSpeed(settings.playbackSpeed)
            it.repeatMode = if (settings.repeatOne) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
        }
    }

    LaunchedEffect(playerSnapshot.isPlaying) {
        while (playerSnapshot.isPlaying) {
            delay(30_000)
            if (controller?.isPlaying == true) statsStore.addAudioSeconds(30)
        }
    }

    val sleepTimerLabel by produceState(initialValue = "متوقف", sleepDeadlineMillis) {
        while (sleepDeadlineMillis > System.currentTimeMillis()) {
            val left = ((sleepDeadlineMillis - System.currentTimeMillis()) / 60_000L).coerceAtLeast(0L) + 1L
            value = "$left د"
            delay(15_000)
        }
        value = "متوقف"
    }

    val locationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        locationRepository.bestLastKnown()?.let { appLocation = it }
    }
    val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    fun createBackup() {
        scope.launch {
            backupStatus = "جارٍ تجهيز النسخة الاحتياطية…"
            runCatching {
                UserBackupManager.exportJson(
                    readingStore = readingStore,
                    settingsStore = settingsStore,
                    practiceStore = practiceStore,
                    statsStore = statsStore,
                    locationRepository = locationRepository,
                )
            }.onSuccess { payload ->
                pendingBackupPayload = payload
                backupCreateLauncher.launch("QuranPremium_backup_${LocalDate.now()}.json")
            }.onFailure {
                backupStatus = "تعذر تجهيز النسخة الاحتياطية: ${it.message ?: "خطأ غير معروف"}"
            }
        }
    }

    fun restoreBackup() {
        backupRestoreLauncher.launch(arrayOf("application/json", "text/plain"))
    }

    fun requestNotifications() {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    fun updateLocation() {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (fine || coarse) locationRepository.bestLastKnown()?.let { appLocation = it }
        else locationLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
    }

    fun requestExactAlarm() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmScheduler.canScheduleExact()) {
            runCatching {
                context.startActivity(
                    Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                        data = Uri.parse("package:${context.packageName}")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                )
            }
        }
    }

    fun openPage(page: Int) {
        navController.navigate("reader/page/${page.coerceIn(1, 604)}") { launchSingleTop = true }
    }

    fun play(reciter: QuranReciter, surah: Int, surahName: String) {
        val uri = audioDownloads.playableUri(reciter, surah)
        val media = MediaItem.Builder()
            .setMediaId("${reciter.id}:${reciter.moshaf.id}:$surah")
            .setUri(uri)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle("سورة $surahName")
                    .setArtist(reciter.name)
                    .build()
            )
            .build()
        controller?.apply {
            setMediaItem(media)
            prepare()
            play()
        }
    }

    fun playHifz(item: com.qurankareem.core.practice.HifzItem, reciter: HifzReciter) {
        val queueSize = (item.toAyah - item.fromAyah + 1).coerceAtLeast(1) * item.ayahRepeat * item.rangeRepeat
        if (queueSize > 1000) return
        val surahName = com.qurankareem.core.quran.QuranMetadata.surah(item.surah).nameArabic
        val mediaItems = buildHifzMediaItems(
            reciter = reciter,
            surah = item.surah,
            fromAyah = item.fromAyah,
            toAyah = item.toAyah,
            ayahRepeat = item.ayahRepeat,
            rangeRepeat = item.rangeRepeat,
            surahName = surahName,
        )
        if (mediaItems.isEmpty()) return
        controller?.apply {
            repeatMode = Player.REPEAT_MODE_OFF
            setMediaItems(mediaItems)
            prepare()
            play()
        }
    }

    fun setSleepTimer(minutes: Int) {
        sleepJob?.cancel()
        if (minutes <= 0) {
            sleepDeadlineMillis = 0L
            return
        }
        sleepDeadlineMillis = System.currentTimeMillis() + minutes * 60_000L
        sleepJob = scope.launch {
            delay(minutes * 60_000L)
            controller?.pause()
            sleepDeadlineMillis = 0L
        }
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                Column {
                    if (playerSnapshot.mediaId != null) {
                        MiniPlayer(
                            snapshot = playerSnapshot,
                            onToggle = { controller?.let { if (it.isPlaying) it.pause() else it.play() } },
                            onOpen = { navController.navigate(if (playerSnapshot.mediaId?.startsWith("hifz:") == true) "hifz" else "audio") },
                        )
                    }
                    NavigationBar {
                        bottomDestinations.forEach { item ->
                            NavigationBarItem(
                                selected = currentRoute == item.route,
                                onClick = {
                                    navController.navigate(item.route) {
                                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = { Icon(item.icon, contentDescription = item.label) },
                                label = { Text(item.label) },
                            )
                        }
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(innerPadding),
        ) {
            composable("home") {
                HomeScreen(
                    lastPage = readingState.lastPage,
                    nextPrayerName = prayerSummary.first,
                    nextPrayerTime = prayerSummary.second,
                    locationLabel = appLocation.label,
                    khatmaActive = practiceState.khatma.active,
                    wirdDone = practiceState.khatma.todayCount,
                    wirdGoal = practiceState.khatma.dailyGoal,
                    dueReviews = practiceState.dueReviews.size,
                    onContinueReading = { openPage(readingState.lastPage) },
                    onOpenMushaf = { navController.navigate("mushaf") },
                    onOpenAudio = { navController.navigate("audio") },
                    onOpenQibla = { navController.navigate("qibla") },
                    onOpenPrayer = { navController.navigate("prayer") },
                    onOpenKhatma = { navController.navigate("khatma") },
                    onOpenHifz = { navController.navigate("hifz") },
                    onOpenAdhkar = { navController.navigate("adhkar") },
                )
            }
            composable("mushaf") {
                MushafScreen(
                    lastPage = readingState.lastPage,
                    bookmarkedAyahs = readingState.bookmarkedAyahs,
                    favoriteAyahs = readingState.favoriteAyahs,
                    onContinueReading = { openPage(readingState.lastPage) },
                    onOpenPage = ::openPage,
                    onOpenSurah = { navController.navigate("reader/surah/$it") },
                    onOpenJuz = { navController.navigate("reader/juz/$it") },
                    onOpenAyah = { surah, ayah -> navController.navigate("reader/surah/$surah?ayah=$ayah") },
                )
            }
            composable("audio") {
                AudioScreen(
                    currentMediaId = playerSnapshot.mediaId,
                    isPlaying = playerSnapshot.isPlaying,
                    playbackSpeed = playerSnapshot.playbackSpeed,
                    repeatOne = playerSnapshot.repeatMode == Player.REPEAT_MODE_ONE,
                    sleepTimerLabel = sleepTimerLabel,
                    defaultReciterId = settings.defaultReciterId,
                    defaultMoshafId = settings.defaultMoshafId,
                    wifiOnlyDownloads = settings.wifiOnlyDownloads,
                    onPlay = ::play,
                    onPauseResume = { controller?.let { if (it.isPlaying) it.pause() else it.play() } },
                    onPlaybackSpeed = { speed ->
                        controller?.setPlaybackSpeed(speed)
                        scope.launch { settingsStore.setPlaybackSpeed(speed) }
                    },
                    onRepeatOne = { enabled ->
                        controller?.repeatMode = if (enabled) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
                        scope.launch { settingsStore.setRepeatOne(enabled) }
                    },
                    onSleepTimer = ::setSleepTimer,
                    onDefaultReciter = { reciterId, moshafId -> scope.launch { settingsStore.setDefaultReciter(reciterId, moshafId) } },
                )
            }
            composable("qibla") {
                QiblaScreen(
                    latitude = appLocation.latitude,
                    longitude = appLocation.longitude,
                    locationLabel = appLocation.label,
                    onUseMyLocation = ::updateLocation,
                )
            }
            composable("prayer") {
                PrayerScreen(
                    latitude = appLocation.latitude,
                    longitude = appLocation.longitude,
                    locationLabel = appLocation.label,
                    onUseMyLocation = ::updateLocation,
                )
            }
            composable("downloads") { DownloadsScreen() }
            composable("stats") { StatsScreen(stats = statsState, practice = practiceState) }
            composable("settings") {
                SettingsScreen(
                    settings = settings,
                    canScheduleExactAlarms = canScheduleExactAlarms,
                    onThemeMode = { scope.launch { settingsStore.setThemeMode(it) } },
                    onWifiOnlyDownloads = { scope.launch { settingsStore.setWifiOnlyDownloads(it) } },
                    onDefaultReciter = { r, m -> scope.launch { settingsStore.setDefaultReciter(r, m) } },
                    onPrayerEnabled = { key: PrayerKey, enabled: Boolean -> scope.launch { settingsStore.setPrayerEnabled(key, enabled) } },
                    onPrayerReminder = { key: PrayerKey, minutes: Int -> scope.launch { settingsStore.setPrayerReminderMinutes(key, minutes) } },
                    onAdhanSound = { uri, label -> scope.launch { settingsStore.setAdhanSound(uri, label) } },
                    onPrecisePrayerAlerts = { scope.launch { settingsStore.setPrecisePrayerAlerts(it) } },
                    onRequestNotifications = ::requestNotifications,
                    onRequestExactAlarm = ::requestExactAlarm,
                    backupStatus = backupStatus,
                    onCreateBackup = ::createBackup,
                    onRestoreBackup = ::restoreBackup,
                )
            }
            composable("more") {
                MoreScreen(
                    onPrayer = { navController.navigate("prayer") },
                    onHifz = { navController.navigate("hifz") },
                    onKhatma = { navController.navigate("khatma") },
                    onAdhkar = { navController.navigate("adhkar") },
                    onDownloads = { navController.navigate("downloads") },
                    onStats = { navController.navigate("stats") },
                    onSettings = { navController.navigate("settings") },
                )
            }
            composable("hifz") {
                HifzScreen(
                    items = practiceState.hifzItems,
                    currentPlayback = parseHifzMediaId(playerSnapshot.mediaId),
                    playbackQueueIndex = playerSnapshot.currentIndex,
                    playbackQueueSize = playerSnapshot.mediaItemCount,
                    isPlaying = playerSnapshot.isPlaying,
                    onAddPlan = { surah, fromAyah, toAyah, ayahRepeat, rangeRepeat ->
                        scope.launch { practiceStore.addHifzPlan(surah, fromAyah, toAyah, ayahRepeat, rangeRepeat) }
                    },
                    onReview = { key, success -> scope.launch { practiceStore.markHifzReviewed(key, success) } },
                    onRemove = { key -> scope.launch { practiceStore.removeHifzPlan(key) } },
                    onOpenAyah = { surah, ayah -> navController.navigate("reader/surah/$surah?ayah=$ayah") },
                    onPlayHifz = ::playHifz,
                    onTogglePlayback = { controller?.let { if (it.isPlaying) it.pause() else it.play() } },
                    onStopPlayback = { controller?.apply { stop(); clearMediaItems() } },
                )
            }
            composable("khatma") {
                KhatmaScreen(
                    progress = practiceState.khatma,
                    onStart = { days -> scope.launch { practiceStore.startKhatma(days) } },
                    onStop = { scope.launch { practiceStore.stopKhatma() } },
                    onOpenPage = ::openPage,
                )
            }
            composable("adhkar") {
                AdhkarScreen(
                    counts = practiceState.adhkarCounts,
                    onIncrement = { id, target -> scope.launch { practiceStore.incrementDhikr(id, target) } },
                    onReset = { id -> scope.launch { practiceStore.resetDhikr(id) } },
                )
            }
            composable(
                route = "reader/page/{number}",
                arguments = listOf(navArgument("number") { type = NavType.IntType }),
            ) { entry ->
                val page = entry.arguments?.getInt("number")?.coerceIn(1, 604) ?: 1
                QuranReaderScreen(
                    kind = ReaderKind.PAGE,
                    number = page,
                    bookmarkedPages = readingState.bookmarkedPages,
                    favoritePages = readingState.favoritePages,
                    bookmarkedAyahs = readingState.bookmarkedAyahs,
                    favoriteAyahs = readingState.favoriteAyahs,
                    onBack = { navController.popBackStack() },
                    onOpenPage = ::openPage,
                    onPageViewed = { pageNumber ->
                        scope.launch {
                            readingStore.setLastPage(pageNumber)
                            practiceStore.recordPageRead(pageNumber)
                            statsStore.recordPageView(pageNumber)
                        }
                    },
                    onToggleBookmark = { scope.launch { readingStore.toggleBookmark(it) } },
                    onToggleFavorite = { scope.launch { readingStore.toggleFavorite(it) } },
                    onToggleAyahBookmark = { surah, ayah -> scope.launch { readingStore.toggleAyahBookmark(surah, ayah) } },
                    onToggleAyahFavorite = { surah, ayah -> scope.launch { readingStore.toggleAyahFavorite(surah, ayah) } },
                )
            }
            composable(
                route = "reader/surah/{number}?ayah={ayah}",
                arguments = listOf(
                    navArgument("number") { type = NavType.IntType },
                    navArgument("ayah") { type = NavType.IntType; defaultValue = 0 },
                ),
            ) { entry ->
                val surah = entry.arguments?.getInt("number")?.coerceIn(1, 114) ?: 1
                val targetAyah = entry.arguments?.getInt("ayah") ?: 0
                QuranReaderScreen(
                    kind = ReaderKind.SURAH,
                    number = surah,
                    bookmarkedPages = readingState.bookmarkedPages,
                    favoritePages = readingState.favoritePages,
                    bookmarkedAyahs = readingState.bookmarkedAyahs,
                    favoriteAyahs = readingState.favoriteAyahs,
                    initialAyah = targetAyah,
                    onBack = { navController.popBackStack() },
                    onOpenPage = ::openPage,
                    onPageViewed = {},
                    onToggleBookmark = {},
                    onToggleFavorite = {},
                    onToggleAyahBookmark = { sNo, aNo -> scope.launch { readingStore.toggleAyahBookmark(sNo, aNo) } },
                    onToggleAyahFavorite = { sNo, aNo -> scope.launch { readingStore.toggleAyahFavorite(sNo, aNo) } },
                )
            }
            composable(
                route = "reader/juz/{number}",
                arguments = listOf(navArgument("number") { type = NavType.IntType }),
            ) { entry ->
                val juz = entry.arguments?.getInt("number")?.coerceIn(1, 30) ?: 1
                QuranReaderScreen(
                    kind = ReaderKind.JUZ,
                    number = juz,
                    bookmarkedPages = readingState.bookmarkedPages,
                    favoritePages = readingState.favoritePages,
                    bookmarkedAyahs = readingState.bookmarkedAyahs,
                    favoriteAyahs = readingState.favoriteAyahs,
                    onBack = { navController.popBackStack() },
                    onOpenPage = ::openPage,
                    onPageViewed = {},
                    onToggleBookmark = {},
                    onToggleFavorite = {},
                    onToggleAyahBookmark = { sNo, aNo -> scope.launch { readingStore.toggleAyahBookmark(sNo, aNo) } },
                    onToggleAyahFavorite = { sNo, aNo -> scope.launch { readingStore.toggleAyahFavorite(sNo, aNo) } },
                )
            }
        }
    }
}

@Composable
private fun MiniPlayer(snapshot: PlayerSnapshot, onToggle: () -> Unit, onOpen: () -> Unit) {
    Surface(tonalElevation = 3.dp, modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp)) {
            Column(Modifier.weight(1f)) {
                Text(snapshot.title, style = MaterialTheme.typography.titleSmall)
                Text("${snapshot.artist} • ${if (snapshot.playbackSpeed == 1f) "1x" else "${snapshot.playbackSpeed}x"}", style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = onToggle) {
                Icon(if (snapshot.isPlaying) Icons.Outlined.Pause else Icons.Outlined.PlayArrow, contentDescription = null)
            }
        }
    }
}

@Composable
private fun MoreScreen(
    onPrayer: () -> Unit,
    onHifz: () -> Unit,
    onKhatma: () -> Unit,
    onAdhkar: () -> Unit,
    onDownloads: () -> Unit,
    onStats: () -> Unit,
    onSettings: () -> Unit,
) {
    LazyColumn(Modifier.padding(horizontal = 20.dp)) {
        item { Text("المزيد", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(top = 12.dp)) }
        item { MoreItem("الحفظ والمراجعة", "خطط حفظ ومراجعات متباعدة", Icons.Outlined.Bookmark, onHifz) }
        item { MoreItem("الختمة والورد", "هدف يومي وتقدم حتى 604 صفحات", Icons.Outlined.CheckCircle, onKhatma) }
        item { MoreItem("الأذكار", "الصباح والمساء والصلاة والنوم", Icons.Outlined.FavoriteBorder, onAdhkar) }
        item { MoreItem("مواقيت الصلاة", "عرض الصلوات والموقع الحالي", Icons.Outlined.Schedule, onPrayer) }
        item { MoreItem("التنزيلات", "إدارة السور الصوتية المحملة", Icons.Outlined.Download, onDownloads) }
        item { MoreItem("الإحصائيات", "القراءة والاستماع والحفظ والختمة", Icons.Outlined.BarChart, onStats) }
        item { MoreItem("الإعدادات", "الثيم، الأذان، القارئ والتنزيل", Icons.Outlined.Settings, onSettings) }
        item { androidx.compose.foundation.layout.Spacer(Modifier.padding(bottom = 100.dp)) }
    }
}

@Composable
private fun MoreItem(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
) {
    Card(Modifier.fillMaxWidth().padding(top = 10.dp).clickable(onClick = onClick)) {
        Row(Modifier.fillMaxWidth().padding(16.dp)) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.padding(horizontal = 12.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
