package com.example.features.intro

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.example.R
import com.example.core.model.ArtistArtwork
import com.example.shared.components.AnimatedAudioWave
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

@Composable
fun InteractiveIntroScreen(
    onFinishIntro: () -> Unit,
    isReplay: Boolean = false
) {
    if (isReplay) {
        BackHandler { onFinishIntro() }
    }

    val slides = ArtistArtwork.introStories
    val pagerState = rememberPagerState(pageCount = { slides.size })
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    var isPaused by remember { mutableStateOf(false) }

    val storyProgress = remember { Animatable(0f) }

    LaunchedEffect(pagerState.currentPage, isPaused) {
        storyProgress.snapTo(0f)
        if (!isPaused) {
            storyProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 4800, easing = LinearEasing)
            )
            if (pagerState.currentPage < slides.lastIndex) {
                pagerState.animateScrollToPage(pagerState.currentPage + 1)
            }
        }
    }

    val kenBurnsTransition = rememberInfiniteTransition(label = "kenBurns")
    val kenBurnsScale by kenBurnsTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.09f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 7000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF07080D))
    ) {
        // Full-bleed swipeable interactive story cards showcasing all 6 uploaded photos
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val slide = slides[page]
            val pageOffset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction).absoluteValue
            val cardScale = lerp(0.92f, 1.0f, 1f - pageOffset.coerceIn(0f, 1f))
            val cardAlpha = lerp(0.55f, 1.0f, 1f - pageOffset.coerceIn(0f, 1f))

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = cardScale
                        scaleY = cardScale
                        alpha = cardAlpha
                    }
                    .pointerInput(page) {
                        detectTapGestures(
                            onLongPress = { isPaused = true },
                            onPress = {
                                tryAwaitRelease()
                                isPaused = false
                            },
                            onTap = { offset ->
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                if (offset.x < size.width * 0.35f) {
                                    if (pagerState.currentPage > 0) {
                                        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
                                    }
                                } else {
                                    if (pagerState.currentPage < slides.lastIndex) {
                                        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                                    } else {
                                        onFinishIntro()
                                    }
                                }
                            }
                        )
                    }
            ) {
                Image(
                    painter = painterResource(id = slide.imageRes),
                    contentDescription = slide.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = kenBurnsScale
                            scaleY = kenBurnsScale
                        }
                )

                // Atmospheric studio gradient overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colorStops = arrayOf(
                                    0.0f to Color(0xAA07080D),
                                    0.28f to Color(0x2207080D),
                                    0.58f to Color(0x9907080D),
                                    0.84f to Color(0xEE07080D),
                                    1.0f to Color(0xFF07080D)
                                )
                            )
                        )
                )
            }
        }

        // Foreground HUD
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top segmented story bars & Skip/Pause controls
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    slides.forEachIndexed { idx, _ ->
                        val progress = when {
                            idx < pagerState.currentPage -> 1f
                            idx == pagerState.currentPage -> storyProgress.value
                            else -> 0f
                        }
                        LinearProgressIndicator(
                            progress = { progress },
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = Color.White.copy(alpha = 0.25f),
                            modifier = Modifier
                                .weight(1f)
                                .height(3.5.dp)
                                .clip(CircleShape)
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.45f),
                        shape = CircleShape
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.GraphicEq,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "ISAI STUDIO COLLECTIVE",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { isPaused = !isPaused },
                            modifier = Modifier.testTag("intro_pause_button")
                        ) {
                            Icon(
                                imageVector = if (isPaused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
                                contentDescription = if (isPaused) "Resume story" else "Pause story",
                                tint = Color.White
                            )
                        }
                        TextButton(
                            onClick = onFinishIntro,
                            modifier = Modifier.testTag("intro_skip_button")
                        ) {
                            Text(
                                text = stringResource(R.string.intro_skip),
                                color = Color.White.copy(alpha = 0.85f),
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }

            // Bottom interactive caption card & thumbnail strip of all 6 photos
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 560.dp)
                    .align(Alignment.CenterHorizontally)
            ) {
                val activeSlide = slides[pagerState.currentPage]

                AnimatedContent(
                    targetState = activeSlide,
                    transitionSpec = { fadeIn(tween(260)) togetherWith fadeOut(tween(180)) },
                    label = "storyCaption"
                ) { slide ->
                    Column {
                        Surface(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = slide.chapter,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = slide.title,
                            style = MaterialTheme.typography.displayMedium,
                            color = Color.White
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = slide.caption,
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White.copy(alpha = 0.86f)
                        )
                        Spacer(Modifier.height(12.dp))
                        Surface(
                            color = Color.White.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                AnimatedAudioWave(
                                    isPlaying = !isPaused,
                                    color = MaterialTheme.colorScheme.secondary,
                                    maxHeight = 16.dp
                                )
                                Text(
                                    text = slide.highlightTrackTitle,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(18.dp))

                // Interactive 6-photo story thumbnail selector strip
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    slides.forEachIndexed { idx, slide ->
                        val selected = idx == pagerState.currentPage
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(if (selected) 52.dp else 42.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (selected) MaterialTheme.colorScheme.primary
                                    else Color.White.copy(alpha = 0.2f)
                                )
                                .padding(if (selected) 2.dp else 0.dp)
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    scope.launch { pagerState.animateScrollToPage(idx) }
                                }
                                .testTag("intro_thumb_$idx")
                        ) {
                            Image(
                                painter = painterResource(id = slide.imageRes),
                                contentDescription = slide.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(8.dp))
                            )
                        }
                    }
                }

                Spacer(Modifier.height(18.dp))

                val isLastPage = pagerState.currentPage == slides.lastIndex
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        if (isLastPage) {
                            onFinishIntro()
                        } else {
                            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color(0xFF0A0B10)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("intro_primary_cta")
                ) {
                    Text(
                        text = if (isLastPage) stringResource(R.string.intro_enter_studio) else stringResource(R.string.intro_next),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                        contentDescription = null
                    )
                }
            }
        }
    }
}
