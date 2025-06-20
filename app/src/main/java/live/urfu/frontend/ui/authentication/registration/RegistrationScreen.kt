package live.urfu.frontend.ui.authentication.registration

import PrivacyPolicyOverlay
import android.annotation.SuppressLint
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import live.urfu.frontend.R
import live.urfu.frontend.data.DTOs.AuthResponse
import live.urfu.frontend.ui.snackBar.SnackBarManager
import live.urfu.frontend.ui.snackBar.SnackBarMessage
import live.urfu.frontend.ui.snackBar.SnackBarType
import live.urfu.frontend.ui.snackBar.TopSnackBarWithDismiss
import java.time.LocalDate

@SuppressLint("ConfigurationScreenWidthHeight", "NewApi")
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun RegistrationScreen(
    onLogoClick: () -> Unit,
    onRegisterClick: () -> Unit,
    onRegisterSuccess: (AuthResponse) -> Unit,
    onRegisterError: (Exception) -> Unit,
    viewModel: RegistrationViewModel = viewModel()
) {
    var currentStep by remember { mutableIntStateOf(1) }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPassword by remember { mutableStateOf("") }
    var showPrivacyPolicy by remember { mutableStateOf(false) }

    val loginValue by viewModel.login.collectAsState()
    val mailValue by viewModel.mail.collectAsState()
    val nameValue by viewModel.name.collectAsState()
    val birthDateValue by viewModel.birthDate.collectAsState()
    val passwordValue by viewModel.password.collectAsState()
    val privacyAccepted by viewModel.privacyPolicyAccepted.collectAsState()

    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp
    val isSmallScreen = screenWidth < 400

    val snackBarManager = remember { SnackBarManager() }
    val currentSnackBar by snackBarManager.currentMessage.collectAsState()

    val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$".toRegex()

    var isSnackBarVisible by remember { mutableStateOf(false) }
    LaunchedEffect(currentSnackBar) {
        isSnackBarVisible = currentSnackBar != null
    }

    LaunchedEffect(isSnackBarVisible) {
        if (!isSnackBarVisible && currentSnackBar != null) {
            delay(300)
            snackBarManager.dismissCurrent()
        }
    }


    val registerCallback = remember {
        object : RegistrationViewModel.RegisterCallback {
            override fun onSuccess(user: AuthResponse) {
                onRegisterSuccess(user)
                onRegisterClick()
            }

            override fun onError(error: Exception) {
                onRegisterError(error)
            }
        }
    }

    BackHandler(enabled = currentStep > 1) { currentStep -= 1 }

    Box(
        modifier = Modifier
            .background(Color(0xFF0D0D0D))
            .fillMaxSize()
    ) {
        Image(
            painter = painterResource(id = R.drawable.heartlast),
            contentDescription = "Logo",
            modifier = Modifier
                .padding(top = 38.dp, end = 41.dp)
                .clickable { onLogoClick() }
                .align(Alignment.TopEnd)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 41.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(100.dp))

            when (currentStep) {
                1 -> UsernameStep(
                    value = loginValue,
                    onValueChange = { viewModel.onLoginChange(it) },
                    isSmallScreen = isSmallScreen
                )
                2 -> NameStep(
                    value = nameValue,
                    onValueChange = { viewModel.onNameChange(it) },
                    isSmallScreen = isSmallScreen

                )
                3 -> EmailStep(
                    value = mailValue,
                    onValueChange = { viewModel.onMailChange(it) },
                    isSmallScreen = isSmallScreen
                )
                4 -> BirthDateStep(
                    value = birthDateValue,
                    onValueChange = { viewModel.onBirthDateChange(it) },
                    isSmallScreen = isSmallScreen
                )
                5 -> PasswordStep(
                    value = passwordValue,
                    confirmValue = confirmPassword,
                    onValueChange = { viewModel.onPasswordChange(it) },
                    onConfirmValueChange = { confirmPassword = it },
                    passwordVisible = passwordVisible,
                    onPasswordVisibilityToggle = { passwordVisible = !passwordVisible },
                    privacyAccepted = privacyAccepted,
                    onPrivacyAcceptedChange = { viewModel.onPrivacyPolicyAcceptedChange(it) },
                    onPrivacyPolicyClick = { showPrivacyPolicy = true },
                    isSmallScreen = isSmallScreen
                )
            }
        }

        RegistrationButtons(
            onBackClick = if (currentStep > 1) { { currentStep -= 1 } } else null,
            onNextClick@ {
                when (currentStep) {
                    1 -> if (loginValue.isNotBlank()) {
                        if (loginValue.length < 3 || loginValue.length > 20) {
                            snackBarManager.showMessage(
                                SnackBarMessage(
                                    "Длина имени пользователя должна быть от 3 до 20 символов",
                                    SnackBarType.ERROR
                                )
                            )
                            return@onNextClick
                        }

                        viewModel.checkUsername(loginValue) { isAvailable ->
                            if (isAvailable) {
                                currentStep = 2
                            } else {
                                snackBarManager.showMessage(
                                    SnackBarMessage(
                                        "Данное имя пользователя занято",
                                        SnackBarType.ERROR
                                    )
                                )
                            }
                        }
                    }
                    2 -> if (nameValue.isNotBlank()) {
                        currentStep = 3
                    }
                    3 -> if (mailValue.isNotBlank()) {
                        if (!emailRegex.matches(mailValue)) {
                            snackBarManager.showMessage(
                                SnackBarMessage(
                                    "Почта не является валидной",
                                    SnackBarType.ERROR
                                )
                            )
                            return@onNextClick
                        }

                        viewModel.checkEmail(mailValue) { isAvailable ->
                            if (isAvailable) {
                                currentStep = 4
                            } else {
                                snackBarManager.showMessage(
                                    SnackBarMessage(
                                        "Данная почта занята",
                                        SnackBarType.ERROR
                                    )
                                )
                            }
                        }
                    }
                    4 -> if (birthDateValue.isNotBlank()) {
                        try {
                            if (birthDateValue.length != 8) {
                                snackBarManager.showMessage(
                                    SnackBarMessage(
                                        "Дата должна быть в формате ДДММГГГГ",
                                        SnackBarType.ERROR
                                    )
                                )
                                return@onNextClick
                            }

                            val day = birthDateValue.substring(0, 2).toInt()
                            val month = birthDateValue.substring(2, 4).toInt()
                            val year = birthDateValue.substring(4, 8).toInt()

                            if (month !in 1..12 || day !in 1..31) {
                                snackBarManager.showMessage(
                                    SnackBarMessage(
                                        "Некорректная дата",
                                        SnackBarType.ERROR
                                    )
                                )
                                return@onNextClick
                            }

                            val birthDate = LocalDate.of(year, month, day)

                            if (year < 1900 || year > 2020) {
                                snackBarManager.showMessage(
                                    SnackBarMessage(
                                        "Год рождения должен быть от 1900 до 2020",
                                        SnackBarType.ERROR
                                    )
                                )
                                return@onNextClick
                            }

                            currentStep = 5
                        } catch (e: Exception) {
                            snackBarManager.showMessage(
                                SnackBarMessage(
                                    "Некорректная дата",
                                    SnackBarType.ERROR
                                )
                            )
                            return@onNextClick
                        }
                    }
                    5 -> if (passwordValue.isNotBlank() && passwordValue == confirmPassword) {
                        viewModel.onRegisterClick(
                            loginValue,
                            mailValue,
                            passwordValue,
                            nameValue,
                            birthDateValue,
                            privacyAccepted,
                            registerCallback
                        )
                    }
                }
            },
            isEnabled = when (currentStep) {
                1 -> loginValue.isNotBlank()
                2 -> nameValue.isNotBlank()
                3 -> mailValue.isNotBlank()
                4 -> birthDateValue.length >= 8
                5 -> passwordValue.isNotBlank() && confirmPassword.isNotBlank() && passwordValue == confirmPassword && privacyAccepted
                else -> false
            },
            buttonText = if (currentStep == 5) "Зарегистрироваться" else "Далее",
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 15.dp),
            isSmallScreen = isSmallScreen
        )

        PrivacyPolicyOverlay(
            isVisible = showPrivacyPolicy,
            onDismiss = { showPrivacyPolicy = false }
        )
    }
    currentSnackBar?.let { snackBar ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(Float.MAX_VALUE)
        ) {
            TopSnackBarWithDismiss(
                message = snackBar.message,
                visible = isSnackBarVisible,
                onDismiss = {
                    isSnackBarVisible = false
                },
                backgroundColor = when (snackBar.type) {
                    SnackBarType.SUCCESS -> Color(0xFF4CAF50)
                    SnackBarType.ERROR -> Color(0xFFB00020)
                    SnackBarType.INFO -> Color(0xFF2196F3)
                },
                autoHideDuration = snackBar.duration,
            )
        }
    }
}

@Composable
private fun UsernameStep(
    value: String,
    onValueChange: (String) -> Unit,
    isSmallScreen: Boolean
) {
    RegistrationStepTemplate(
        title = "Введите имя пользователя",
        subtitle = "Оно может быть изменено в настройках приложения",
        content = {
            Text(
                "Имя пользователя",
                color = Color.White,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 11.5.dp, start = 10.5.dp),
                textAlign = TextAlign.Start,
                style = typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = value,
                singleLine = true,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(15.dp),
                colors = textFieldColors()
            )
        },
        isSmallScreen = isSmallScreen
    )
}

@Composable
private fun NameStep(
    value: String,
    onValueChange: (String) -> Unit,
    isSmallScreen: Boolean
) {
    RegistrationStepTemplate(
        title = "Введите ваше имя и фамилию",
        subtitle = "Они будут отображаться в профиле приложения",
        content = {
            Text(
                "Имя и Фамилия",
                color = Color.White,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 11.5.dp, start = 10.5.dp),
                textAlign = TextAlign.Start,
                style = typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = value,
                singleLine = true,
                onValueChange = onValueChange,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(15.dp),
                colors = textFieldColors()
            )
        },
        isSmallScreen = isSmallScreen
    )
}

@Composable
private fun EmailStep(
    value: String,
    onValueChange: (String) -> Unit,
    isSmallScreen: Boolean
) {
    RegistrationStepTemplate(
        title = "Введите адрес вашей электронной почты",
        subtitle = "Мы будем использовать его для отправки уведомлений",
        content = {
            Text(
                "E-mail",
                color = Color.White,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 11.5.dp, start = 10.5.dp),
                textAlign = TextAlign.Start,
                style = typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = value,
                singleLine = true,
                onValueChange = onValueChange,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(15.dp),
                colors = textFieldColors()
            )
        },
        isSmallScreen = isSmallScreen
    )
}

@Composable
private fun BirthDateStep(
    value: String,
    onValueChange: (String) -> Unit,
    isSmallScreen: Boolean
) {

    RegistrationStepTemplate(
        title = "Введите вашу дату рождения",
        subtitle = "Эта информация не будет показана другим пользователям",
        content = {
            Text(
                "Дата рождения",
                color = Color.White,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 11.5.dp, start = 10.5.dp),
                textAlign = TextAlign.Start,
                style = typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(10.dp))

            val keyboardController = LocalSoftwareKeyboardController.current
            OutlinedTextField(
                value = value,
                onValueChange = { newValue ->
                    val digits = newValue.filter { it.isDigit() }
                    val limitedDigits = if (digits.length > 8) digits.substring(0, 8) else digits
                    onValueChange(limitedDigits)
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(15.dp),
                placeholder = { Text("ДД-ММ-ГГГГ", color = Color.Gray) },
                colors = textFieldColors(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {keyboardController?.hide()}
                ),
                visualTransformation = DateTransformation()
            )
        },
        isSmallScreen = isSmallScreen
    )
}

@Composable
private fun PasswordStep(
    value: String,
    confirmValue: String,
    onValueChange: (String) -> Unit,
    onConfirmValueChange: (String) -> Unit,
    passwordVisible: Boolean,
    onPasswordVisibilityToggle: () -> Unit,
    privacyAccepted: Boolean,
    onPrivacyAcceptedChange: (Boolean) -> Unit,
    onPrivacyPolicyClick: () -> Unit,
    isSmallScreen: Boolean
) {
    RegistrationStepTemplate(
        title = "Введите и подтвердите ваш пароль",
        subtitle = "Придумайте надежный пароль для защиты аккаунта",
        content = {
            Text(
                "Пароль",
                color = Color.White,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 11.5.dp, start = 10.5.dp),
                textAlign = TextAlign.Start,
                style = typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = value,
                singleLine = true,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(15.dp),
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    Icon(
                        painter = painterResource(id = R.drawable.eye),
                        contentDescription = "Включить отобржаение пароля",
                        tint = Color.White,
                        modifier = Modifier
                            .size(30.dp)
                            .clickable { onPasswordVisibilityToggle() }
                    )
                },
                colors = textFieldColors(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                "Подтвердите пароль",
                color = Color.White,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 11.5.dp, start = 10.5.dp),
                textAlign = TextAlign.Start,
                style = typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = confirmValue,
                singleLine = true,
                onValueChange = onConfirmValueChange,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(15.dp),
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                colors = textFieldColors(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                isError = confirmValue.isNotEmpty() && value != confirmValue
            )

            if (confirmValue.isNotEmpty() && value != confirmValue) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Пароли не совпадают",
                    color = Color.Red,
                    style = typography.bodySmall,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 11.5.dp)
                )
            }
            Spacer(modifier = Modifier.height(20.dp))

            PrivacyConsentCheckbox(
                isChecked = privacyAccepted,
                onCheckedChange = onPrivacyAcceptedChange,
                onPrivacyPolicyClick = onPrivacyPolicyClick,
            )
        },
        isSmallScreen = isSmallScreen
    )
}

@Composable
private fun RegistrationStepTemplate(
    title: String,
    subtitle: String,
    content: @Composable ColumnScope.() -> Unit,
    isSmallScreen: Boolean
) {
    val paddingBeforeContent = when {
        isSmallScreen -> 48.dp
        else -> 60.dp
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = title,
            style = typography.titleMedium,
            color = Color.White,
            textAlign = TextAlign.Start,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = subtitle,
            style = typography.bodyLarge,
            color = Color.Gray,
            textAlign = TextAlign.Start,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(paddingBeforeContent))

        content()
    }
}

@Composable
private fun RegistrationButtons(
    onBackClick: (() -> Unit)?,
    onNextClick: () -> Unit,
    isEnabled: Boolean,
    buttonText: String,
    modifier: Modifier = Modifier,
    isSmallScreen: Boolean
) {
    val fontSize = when {
        isSmallScreen -> 14.sp
        else -> 16.sp
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .systemBarsPadding(),
        horizontalArrangement = if (onBackClick != null) Arrangement.spacedBy(12.dp) else Arrangement.End
    ) {
        if (onBackClick != null) {
            OutlinedButton(
                onClick = onBackClick,
                modifier = Modifier.weight(0.45f),
                shape = RoundedCornerShape(15.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color.White
                )
            ) {
                Text(
                    "Назад", style = typography.headlineMedium.copy(fontSize = fontSize, lineHeight = fontSize)
                )
            }
        }

        Button(
            onClick = onNextClick,
            modifier = if (onBackClick != null) Modifier.weight(1f) else Modifier.width(150.dp),
            shape = RoundedCornerShape(15.dp),
            enabled = isEnabled,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(238, 126, 86),
                contentColor = Color.Black,
                disabledContainerColor = Color.Gray,
                disabledContentColor = Color.DarkGray
            )
        ) {
            Text(
                buttonText,
                style = typography.headlineMedium.copy(fontSize = fontSize, lineHeight = fontSize)
            )
        }
    }

}


class DateTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val trimmed = text.text.take(8)
        val formatted = buildString {
            for (i in trimmed.indices) {
                append(trimmed[i])
                when (i) {
                    1 -> if (trimmed.length > 2) append('-')
                    3 -> if (trimmed.length > 4) append('-')
                }
            }
        }

        val dateMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                val transformedOffset = when {
                    offset <= 1 -> offset
                    offset <= 3 -> offset + 1
                    offset <= 7 -> offset + 2
                    else -> formatted.length
                }
                return transformedOffset.coerceAtMost(formatted.length)
            }

            override fun transformedToOriginal(offset: Int): Int {
                return when {
                    offset <= 2 -> offset
                    offset <= 5 -> offset - 1
                    else -> offset - 2
                }.coerceIn(0, trimmed.length)
            }
        }

        return TransformedText(AnnotatedString(formatted), dateMapping)
    }
}

@Composable
private fun textFieldColors() = OutlinedTextFieldDefaults.colors(
    unfocusedTextColor = Color.White,
    focusedTextColor = Color.White,
    unfocusedContainerColor = Color(0xFF1D1D1D),
    focusedContainerColor = Color(0xFF1D1D1D),
    unfocusedBorderColor = Color.Transparent,
    focusedBorderColor = Color.Transparent,
    cursorColor = Color.White
)