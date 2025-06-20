package live.urfu.frontend.ui.search

import AdaptiveSizes
import ScreenSizeInfo
import SpacerType
import live.urfu.frontend.ui.main.TagChip
import live.urfu.frontend.ui.main.TagSizes
import adaptiveTextStyle
import android.annotation.SuppressLint
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import live.urfu.frontend.R
import live.urfu.frontend.data.model.Post
import live.urfu.frontend.ui.main.PostColorPatterns
import live.urfu.frontend.ui.main.PostViewModel
import live.urfu.frontend.ui.profile.ExpandedPostOverlay
import kotlinx.coroutines.launch
import rememberScreenSizeInfo

@SuppressLint("ConfigurationScreenWidthHeight")
@Composable
fun SearchScreen(
    initialTag: String = "",
    onClose: () -> Unit = {},
    onAuthorClick: (String) -> Unit = {},
    onCommentsClick: (postId: Long, searchQuery: String) -> Unit,
    viewModel: SearchViewModel = viewModel(),
    enableAnimations: Boolean = true,
    postViewModel: PostViewModel,
    onTagSearch: (String) -> Unit
) {
    val screenInfo = rememberScreenSizeInfo()
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp

    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val recentSearches by viewModel.recentSearches.collectAsState()
    val hasSearched by viewModel.hasSearched.collectAsState()
    var expandedPostIndex by remember { mutableStateOf<Int?>(null) }
    DisposableEffect(postViewModel, viewModel) {
        postViewModel.connectSearchViewModel(viewModel)

        onDispose {
            postViewModel.disconnectSearchViewModel(viewModel)
        }
    }

    LaunchedEffect(searchResults) {
        if (searchResults.isNotEmpty()) {
            postViewModel.addSearchPostsIfNeeded(searchResults)
        }
    }

    expandedPostIndex?.let { index ->
        if (index < searchResults.size) {
            ExpandedPostOverlay(
                post = searchResults[index],
                onClose = { expandedPostIndex = null },
                onCommentsClick = { onCommentsClick(searchResults[index].id,searchQuery) },
                onAuthorClick = { authorId ->
                    onAuthorClick(authorId)
                },
                viewModel = postViewModel
            )
        }
    }

    val searchBarAdapter = remember(viewModel) {
        SearchViewModel.SearchBarAdapter(viewModel)
    }

    BackHandler {
        if (expandedPostIndex != null) {
            expandedPostIndex = null
        } else {
            onClose()
        }
    }

    LaunchedEffect(initialTag) {
        if (initialTag.isNotBlank()) {
            viewModel.updateSearchQuery(initialTag)
            viewModel.searchByTag(initialTag)
            viewModel.initializeWithTag(initialTag)
        }
    }

    val animatedAlpha = remember { Animatable(if (enableAnimations) 0f else 1f) }
    val animatedOffset = remember { Animatable(if (enableAnimations) screenHeight.value else 0f) }

    if (enableAnimations) {
        LaunchedEffect(Unit) {
            launch {
                animatedAlpha.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
                )
            }
            launch {
                animatedOffset.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
                )
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(100f)
            .then(
                if (enableAnimations) Modifier.graphicsLayer {
                    alpha = animatedAlpha.value
                    translationY = animatedOffset.value
                } else Modifier
            )
            .background(Color(0xFF131313))
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            SearchBar(
                onClose = onClose,
                onTagSelected = { tag ->
                    viewModel.selectSuggestion(tag)
                    onTagSearch(tag)
                },
                screenInfo = screenInfo,
                adapter = searchBarAdapter,
                enableAnimations = enableAnimations,
                showBackButton = true
            )

            when {
                isLoading -> {
                    LoadingContent(screenInfo)
                }

                hasSearched && searchResults.isEmpty() -> {
                    EmptySearchResults(searchQuery, screenInfo)
                }

                hasSearched -> {
                    SearchResultsList(
                        posts = searchResults,
                        onPostClick = { index -> expandedPostIndex = index },
                        onAuthorClick = { authorId ->
                            onAuthorClick(authorId)
                        },
                        onCommentsClick = onCommentsClick,
                        screenInfo = screenInfo,
                        postViewModel = postViewModel,
                        searchQuery = searchQuery
                    )
                }

                else -> {
                    DefaultSearchContent(
                        recentSearches = recentSearches,
                        onRecentSearchClick = { viewModel.searchByTag(it) },
                        screenInfo = screenInfo
                    )
                }
            }
        }
    }
}

@Composable
private fun LoadingContent(screenInfo: ScreenSizeInfo) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                color = Color(0xFFEE7E56),
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Поиск постов...",
                color = Color.White,
                style = adaptiveTextStyle(MaterialTheme.typography.bodyLarge, screenInfo)
            )
        }
    }
}

@Composable
private fun EmptySearchResults(
    searchQuery: String,
    screenInfo: ScreenSizeInfo,
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.search),
                contentDescription = "Нет результатов",
                modifier = Modifier.size(64.dp),
                colorFilter = ColorFilter.tint(Color.Gray)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Ничего не найдено",
                style = adaptiveTextStyle(
                    MaterialTheme.typography.titleMedium.copy(fontSize = 20.sp),
                    screenInfo
                ),
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "По запросу \"$searchQuery\" постов не найдено.\nПопробуйте другой тег.",
                style = adaptiveTextStyle(MaterialTheme.typography.bodyMedium, screenInfo),
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun DefaultSearchContent(
    recentSearches: List<String>,
    onRecentSearchClick: (String) -> Unit,
    screenInfo: ScreenSizeInfo,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = if (screenInfo.isCompact) 16.dp else 24.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        if (recentSearches.isNotEmpty()) {
            item {
                Text(
                    text = "Недавние поиски",
                    style = adaptiveTextStyle(
                        MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
                        screenInfo
                    ),
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    recentSearches.forEach { search ->
                        TagChip(
                            tag = search,
                            color = Color(0xFF404040),
                            size = TagSizes.Standard
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        item {
            Text(
                text = "Популярные теги",
                style = adaptiveTextStyle(
                    MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
                    screenInfo
                ),
                color = Color.White,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            val popularTags = listOf(
                "Учеба", "Программирование", "Спорт", "Здоровье",
                "Юмор", "Стажировка", "Работа", "Волонтерство"
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                popularTags.forEach { tag ->
                    Box(
                        modifier = Modifier
                            .clickable { onRecentSearchClick(tag) }
                            .background(Color(0xFF292929), RoundedCornerShape(52.dp))
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = tag,
                            color = Color.White,
                            style = adaptiveTextStyle(
                                MaterialTheme.typography.labelSmall,
                                screenInfo
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchResultsList(
    posts: List<Post>,
    onPostClick: (Int) -> Unit,
    onAuthorClick: (String) -> Unit,
    onCommentsClick: (postId: Long, searchQuery: String) -> Unit,
    screenInfo: ScreenSizeInfo,
    postViewModel: PostViewModel,
    searchQuery: String
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            horizontal = if (screenInfo.isCompact) 16.dp else 24.dp,
            vertical = 16.dp
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "${
                    when {
                        posts.size % 10 == 1 && posts.size % 100 != 11 -> "Найден"
                        else -> "Найдено"
                    }
                } ${posts.size} ${
                    when {
                        posts.size % 10 == 1 && posts.size % 100 != 11 -> "пост"
                        posts.size % 10 in 2..4 && posts.size % 100 !in 12..14 -> "поста"
                        else -> "постов"
                    }
                }",
                style = adaptiveTextStyle(MaterialTheme.typography.bodyLarge, screenInfo),
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        itemsIndexed(posts) { index, post ->
            SearchPostCard(
                post = post,
                onClick = { onPostClick(index) },
                onAuthorClick = { onAuthorClick(post.author.id) },
                onCommentsClick = onCommentsClick,
                screenInfo = screenInfo,
                postViewModel = postViewModel,
                searchQuery = searchQuery
            )
        }
    }
}

@Composable
private fun SearchPostCard(
    post: Post,
    onClick: () -> Unit,
    onAuthorClick: () -> Unit,
    onCommentsClick: (postId: Long, searchQuery: String) -> Unit,
    screenInfo: ScreenSizeInfo,
    postViewModel: PostViewModel,
    searchQuery: String
) {
    val colorPatternIndex = post.id.rem(PostColorPatterns.size).toInt()
    val colorPattern = PostColorPatterns[colorPatternIndex]

    val isLiked = postViewModel.isPostLikedByCurrentUser(post.id)
    val actualLikesCount = postViewModel.getPostLikesCount(post.id)
    val isProcessing = postViewModel.isPostProcessing(post.id)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = colorPattern.background
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = if (screenInfo.isCompact) 16.dp else 20.dp,
                    vertical = if (screenInfo.isCompact) 12.dp else 16.dp
                )
        ) {
            Text(
                text = post.title,
                style = adaptiveTextStyle(
                    MaterialTheme.typography.labelLarge.copy(
                        color = colorPattern.textColor,
                        fontWeight = FontWeight.Bold
                    ),
                    screenInfo
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(
                modifier = Modifier.height(
                    AdaptiveSizes.spacerHeight(
                        screenInfo,
                        SpacerType.Medium
                    )
                )
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                AsyncImage(
                    model = post.author.avatarUrl,
                    contentDescription = "Author avatar",
                    modifier = Modifier
                        .size(AdaptiveSizes.authorAvatarSize(screenInfo))
                        .clip(CircleShape)
                        .border(2.dp, Color.White, CircleShape)
                        .clickable { onAuthorClick() },
                    contentScale = ContentScale.Crop,
                    placeholder = painterResource(R.drawable.ava),
                    error = painterResource(R.drawable.ava)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Автор:",
                        style = adaptiveTextStyle(MaterialTheme.typography.titleLarge, screenInfo),
                        color = Color.Black
                    )
                    Text(
                        text = post.author.username,
                        style = adaptiveTextStyle(MaterialTheme.typography.titleLarge, screenInfo),
                        color = colorPattern.textColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.clickable { onAuthorClick() }
                    )
                }

                Text(
                    text = post.time.substring(0, 10),
                    style = adaptiveTextStyle(MaterialTheme.typography.displaySmall, screenInfo),
                    color = Color.Black
                )
            }

            Spacer(
                modifier = Modifier.height(
                    AdaptiveSizes.spacerHeight(
                        screenInfo,
                        SpacerType.Medium
                    )
                )
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                post.tags.take(3).forEach { tag ->
                    TagChip(
                        tag = tag.name,
                        color = colorPattern.buttonColor,
                        size = TagSizes.Small
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(
                    AdaptiveSizes.spacerHeight(
                        screenInfo,
                        SpacerType.Medium
                    )
                )
            )

            Text(
                text = post.text,
                style = adaptiveTextStyle(MaterialTheme.typography.displayMedium, screenInfo),
                color = colorPattern.textColor,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(
                modifier = Modifier.height(
                    AdaptiveSizes.spacerHeight(
                        screenInfo,
                        SpacerType.Small
                    )
                )
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.clickable(
                        enabled = !isProcessing
                    ) {
                        postViewModel.likeAndDislike(post.id)
                    }
                ) {

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(SearchAdaptiveSizes.reactionIconSize(screenInfo))
                    ) {
                        if (isLiked) {
                            Image(
                                painter = painterResource(id = R.drawable.like_filling),
                                contentDescription = null,
                                colorFilter = ColorFilter.tint(colorPattern.reactionColorFilling),
                                modifier = Modifier.size(
                                    SearchAdaptiveSizes.reactionIconSize(
                                        screenInfo
                                    ) - 2.dp
                                )
                            )
                        }

                        Image(
                            painter = painterResource(id = R.drawable.likebottom),
                            contentDescription = "Лайки",
                            modifier = Modifier.fillMaxSize(),
                            colorFilter = ColorFilter.tint(colorPattern.reactionColor)
                        )
                    }
                    Text(
                        text = actualLikesCount.toString(),
                        color = colorPattern.textColor,
                        style = adaptiveTextStyle(MaterialTheme.typography.displayLarge, screenInfo)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.clickable {
                        onCommentsClick(post.id, searchQuery)
                    }
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.commentbottom),
                        contentDescription = "Комментарии",
                        modifier = Modifier.size(SearchAdaptiveSizes.reactionIconSize(screenInfo)),
                        colorFilter = ColorFilter.tint(colorPattern.reactionColor)
                    )
                    Text(
                        text = post.comments.toString(),
                        color = colorPattern.textColor,
                        style = adaptiveTextStyle(MaterialTheme.typography.displayLarge, screenInfo)
                    )
                }
            }
        }
    }
}
