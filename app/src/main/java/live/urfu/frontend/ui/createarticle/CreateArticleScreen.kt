package live.urfu.frontend.ui.createarticle

import android.annotation.SuppressLint
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.zIndex
import live.urfu.frontend.data.DTOs.DefaultResponse
import live.urfu.frontend.data.model.UserRole
import kotlinx.coroutines.launch
import live.urfu.frontend.R
import live.urfu.frontend.data.DTOs.PostDto

@SuppressLint("ConfigurationScreenWidthHeight")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateArticle(
    onClose: () -> Unit,
    viewModel: CreateArticleViewModel,
    onPostSuccess: (PostDto) -> Unit,
    onPostError: (Exception) -> Unit,
    animationsEnabled: Boolean = true,
) {
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp

    val horizontalPadding = screenWidth.times(0.04f).coerceAtLeast(16.dp)

    val contentFieldHeight = remember(screenHeight) {
        val percentHeight = when {
            screenHeight < 720.dp -> 0.35f
            screenHeight < 800.dp -> 0.40f
            else -> 0.45f
        }
        screenHeight.times(percentHeight)
    }

    val tagFieldHeight = remember(screenHeight) {
        val percentHeight = when {
            screenHeight < 720.dp -> 0.18f
            screenHeight < 800.dp -> 0.25f
            else -> 0.40f
        }
        screenHeight.times(percentHeight)
    }

    var isClosing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val tagsScrollState = rememberScrollState()

    val animatedAlpha = remember { Animatable(if (animationsEnabled) 0f else 1f) }
    val animatedOffset = remember { Animatable(if (animationsEnabled) screenHeight.value else 0f) }

    val userState by viewModel.user.collectAsState()

    var showSuggestions by remember { mutableStateOf(false) }
    var suggestions by remember { mutableStateOf<List<String>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var titleText by remember { mutableStateOf("") }
    var contentText by remember { mutableStateOf("") }
    var tagsInput by remember { mutableStateOf("") }
    var selectedTags by remember { mutableStateOf<List<String>>(emptyList()) }
    var showNewTagDialog by remember { mutableStateOf(false) }
    var newTagText by remember { mutableStateOf("") }

    val tags by viewModel.tags.collectAsState()

    val tagNames = tags.map { it.name }

    @Composable
    fun HighlightedText(text: String, query: String) {
        val startIndex = text.lowercase().indexOf(query.lowercase())
        if (startIndex >= 0) {
            val endIndex = startIndex + query.length
            Row {
                if (startIndex > 0) {
                    Text(
                        text = text.substring(0, startIndex),
                        color = Color.Gray,
                        style = typography.headlineMedium
                    )
                }
                Text(
                    text = text.substring(startIndex, endIndex),
                    color = Color.White,
                    style = typography.headlineMedium
                )
                if (endIndex < text.length) {
                    Text(
                        text = text.substring(endIndex),
                        color = Color.Gray,
                        style = typography.headlineMedium
                    )
                }
            }
        } else {
            Text(
                text = text,
                color = Color.White,
                fontSize = 14.sp
            )
        }
    }

    if (userState == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.95f)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(
                    color = Color(0xFFEE7E56),
                    strokeWidth = 4.dp,
                    modifier = Modifier.size(48.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Загрузка...",
                    color = Color.White,
                    style = typography.bodyLarge
                )
            }
        }
        return
    }

    if (userState!!.role == UserRole.USER) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.95f))
                .zIndex(300f),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .background(Color(0xFF292929), shape = RoundedCornerShape(52.dp))
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(28.5.dp))

                Text(
                    text = "Для того, чтобы публиковать посты, подайте заявку на получение прав автора",
                    color = Color.LightGray,
                    style = typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(start = 28.5.dp, end = 28.5.dp),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(28.5.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(40.dp)) {
                    Button(
                        onClick = { onClose() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF404040),
                            contentColor = Color.White
                        ),
                    ) {
                        Text("Отмена", style = typography.labelMedium.copy(fontSize = 14.sp))
                    }
                    Button(
                        onClick = { onClose() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF404040),
                            contentColor = Color.White
                        ),
                    ) {
                        Text("Подать заявку", style = typography.labelMedium.copy(fontSize = 14.sp))
                    }
                }
            }
        }
        return
    }


    // todo Вынести в ViewModel
    val postCallBack = remember {
        object : CreateArticleViewModel.PostCallBack {
            override fun onSuccess(user: PostDto) {
                onPostSuccess(user)
                onClose()
            }

            override fun onError(error: Exception) {
                println("Абеме")
                println(error)
                onPostError(error)
            }
        }
    }

    fun searchTags(query: String) {
        if (query.isBlank()) {
            showSuggestions = false
            return
        }

        isLoading = true

        val filtered = tagNames.filter { tag ->
            tag.lowercase().contains(query.lowercase()) && !selectedTags.contains(tag)
        }.take(5)

        suggestions = filtered
        showSuggestions =
            filtered.isNotEmpty() || query.length >= 2
        isLoading = false
    }

    fun selectTag(selectedTag: String) {
        if (!selectedTags.contains(selectedTag)) {
            selectedTags = selectedTags + selectedTag
        }
        tagsInput = ""
        showSuggestions = false
    }

    fun removeTag(tagToRemove: String) {
        selectedTags = selectedTags.filter { it != tagToRemove }
    }

    fun handleClose() {
        if (!isClosing) {
            isClosing = true
            if (animationsEnabled) {
                scope.launch {
                    launch {
                        animatedAlpha.animateTo(0f, tween(300, easing = FastOutSlowInEasing))
                    }
                    launch {
                        animatedOffset.animateTo(
                            screenHeight.value,
                            tween(300, easing = FastOutSlowInEasing)
                        )
                    }
                    onClose()
                }
            } else {
                onClose()
            }
        }
    }

    if (animationsEnabled) {
        LaunchedEffect(Unit) {
            launch {
                animatedAlpha.animateTo(1f, tween(300, easing = FastOutSlowInEasing))
            }
            launch {
                animatedOffset.animateTo(0f, tween(300, easing = FastOutSlowInEasing))
            }
        }
    }

    BackHandler(enabled = !isClosing) {
        handleClose()
    }

    val darkBackground = Color(0xFF131313)
    val darkSurface = Color(0xFF131313)
    val grayText = Color(red = 125, green = 125, blue = 125)
    val lightGrayText = Color(0xFFBBBBBB)

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(100f)
            .then(
                if (animationsEnabled) Modifier.graphicsLayer {
                    alpha = animatedAlpha.value
                    translationY = animatedOffset.value
                } else Modifier
            ),
        containerColor = darkBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Создать пост",
                        color = Color.White,
                        style = typography.headlineLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { handleClose() }) {
                        Image(
                            painter = painterResource(id = R.drawable.chevron_left),
                            contentDescription = "Назад"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF131313)
                )
            )
        }
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = horizontalPadding)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = titleText,
                onValueChange = { titleText = it },
                placeholder = { Text("Введите заголовок...", color = grayText) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = darkSurface,
                    unfocusedContainerColor = darkSurface,
                    disabledContainerColor = darkSurface,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = lightGrayText,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                textStyle = TextStyle(color = Color.White),
                singleLine = true
            )
            HorizontalDivider(
                thickness = 1.dp,
                color = Color(red = 131, green = 131, blue = 131)
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = contentText,
                onValueChange = { contentText = it },
                placeholder = {
                    Text(
                        "Напишите что-нибудь...",
                        style = typography.bodyLarge,
                        color = grayText
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 150.dp, max = contentFieldHeight)
                    .border(
                        width = 1.dp,
                        color = Color(red = 131, green = 131, blue = 131),
                        shape = RoundedCornerShape(8.dp)
                    ),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = lightGrayText,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                textStyle = TextStyle(color = Color.White)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Box {
                OutlinedTextField(
                    value = tagsInput,
                    onValueChange = { newText ->
                        tagsInput = newText
                        if (newText.trim().length >= 2) {
                            scope.launch {
                                searchTags(newText.trim())
                            }
                        } else {
                            showSuggestions = false
                        }
                    },
                    placeholder = {
                        Text(
                            "Введите тег и выберите из списка...",
                            color = grayText,
                            style = typography.bodyLarge
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = darkSurface,
                        unfocusedContainerColor = darkSurface,
                        disabledContainerColor = darkSurface,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = lightGrayText,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    textStyle = TextStyle(color = Color.White),
                    singleLine = true,
                    trailingIcon = {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color(0xFFEE7E56),
                                strokeWidth = 2.dp
                            )
                        }
                    }
                )
                DropdownMenu(
                    expanded = showSuggestions,
                    onDismissRequest = { showSuggestions = false },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = horizontalPadding)
                        .background(Color(0xFF232323))
                        .clip(RoundedCornerShape(8.dp)),
                    properties = PopupProperties(focusable = false),
                    containerColor = Color(0xFF131313),
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp
                ) {
                    suggestions.forEach { suggestion ->
                        DropdownMenuItem(
                            text = {
                                HighlightedText(text = suggestion, query = tagsInput.trim())
                            },
                            onClick = { selectTag(suggestion) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF232323))
                        )
                    }
                    if (tagsInput.trim().length >= 2) {
                        DropdownMenuItem(
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "Добавить новый тег",
                                        color = Color(0xFFA7A7A7),
                                        style = typography.titleSmall.copy(fontWeight = FontWeight.Medium, fontSize = 18.sp, lineHeight = 23.4.sp)
                                    )
                                }
                            },
                            onClick = {
                                newTagText = tagsInput.trim()
                                showNewTagDialog = true
                                showSuggestions = false
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF232323))
                        )
                    }
                }
            }
            if (selectedTags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = tagFieldHeight)
                        .padding(8.dp)
                        .verticalScroll(tagsScrollState)
                ) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        selectedTags.forEach { tag ->
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(52.dp)),
                                color = Color(0xFFEE7E56)
                            ) {
                                Row(
                                    modifier = Modifier.padding(
                                        horizontal = 15.dp,
                                        vertical = 10.dp
                                    ),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Text(
                                        text = tag,
                                        style = typography.labelMedium
                                    )
                                    Image(
                                        painter = painterResource(id = R.drawable.x),
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clickable { removeTag(tag) },
                                        colorFilter = ColorFilter.tint(Color.Black),
                                        contentDescription = "Удалить тег"
                                    )
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    onClick = {
                        viewModel.onPublishClick(titleText, contentText, selectedTags.joinToString(","), postCallBack)
                    },
                    shape = RoundedCornerShape(42.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(red = 238, green = 126, blue = 86),
                        contentColor = Color.Black
                    ),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Text(
                        "Опубликовать",
                        style = typography.bodyLarge
                    )
                }
            }
        }
    }
    if (showNewTagDialog) {
        AlertDialog(
            onDismissRequest = {
                showNewTagDialog = false
                newTagText = ""
            },
            title = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(28.5.dp))
                    Text(
                        text = "Добавление нового тега",
                        color = Color.LightGray,
                        style = typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(start = 28.5.dp, end = 28.5.dp),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(42.dp))
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    OutlinedTextField(
                        value = newTagText,
                        onValueChange = { newTagText = it },
                        placeholder = { Text("Введите новый тег", color = grayText) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF292929),
                            unfocusedContainerColor = Color(0xFF292929),
                            disabledContainerColor = Color(0xFF292929),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = lightGrayText,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        textStyle = TextStyle(color = Color.White),
                        singleLine = true
                    )
                    HorizontalDivider(
                        thickness = 1.dp,
                        color = Color(red = 131, green = 131, blue = 131)
                    )
                    Spacer(modifier = Modifier.height(28.5.dp))
                }
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Button(
                        onClick = {
                            if (newTagText.trim().isNotBlank() && !selectedTags.contains(newTagText.trim())) {
                                selectedTags = selectedTags + newTagText.trim()
                                tagsInput = ""
                            }
                            showNewTagDialog = false
                            newTagText = ""
                        },
                        colors = ButtonColors(
                            containerColor = Color(0xFF404040),
                            contentColor = Color.White,
                            disabledContainerColor = Color(0xFF404040),
                            disabledContentColor = Color(0xFF404040)
                        )
                    ) {
                        Text(
                            text = "Добавить",
                            style = typography.labelMedium
                        )
                    }
                }
            },
            dismissButton = {},
            containerColor = Color(0xFF292929),
            shape = RoundedCornerShape(52.dp),
            modifier = Modifier.padding(0.dp)
                .fillMaxWidth(1f)
        )
    }
}