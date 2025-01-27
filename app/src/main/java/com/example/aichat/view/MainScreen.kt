//// MainScreen.kt
//
//package com.example.aichat.view
//
//import androidx.compose.animation.AnimatedVisibility
//import androidx.compose.foundation.Image
//import androidx.compose.foundation.background
//import androidx.compose.foundation.clickable
//import androidx.compose.foundation.layout.*
//import androidx.compose.foundation.lazy.LazyColumn
//import androidx.compose.foundation.lazy.items
//import androidx.compose.foundation.shape.RoundedCornerShape
//import androidx.compose.foundation.text.BasicTextField
//import androidx.compose.foundation.text.KeyboardActions
//import androidx.compose.foundation.text.KeyboardOptions
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.filled.ArrowBack
//import androidx.compose.material.icons.filled.ArrowForward
//import androidx.compose.material.icons.filled.Mic
//import androidx.compose.material3.Divider
//import androidx.compose.material3.Icon
//import androidx.compose.material3.IconButton
//import androidx.compose.material3.Text
//import androidx.compose.runtime.Composable
//import androidx.compose.runtime.MutableState
//import androidx.compose.runtime.remember
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.draw.clip
//import androidx.compose.ui.graphics.Brush
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.graphics.SolidColor
//import androidx.compose.ui.res.painterResource
//import androidx.compose.ui.text.TextStyle
//import androidx.compose.ui.text.input.ImeAction
//import androidx.compose.ui.unit.dp
//import androidx.compose.ui.unit.sp
//import com.example.aichat.R
//import com.example.aichat.model.ChatMessage
//import com.example.aichat.viewmodel.ChatViewModel
//
//@Composable
//fun MainScreen(
//    chatViewModel: ChatViewModel,
//    liveSpeechInput: MutableState<String>,
//    isExpanded: MutableState<Boolean>,
//    chatMessages: List<ChatMessage>,
//    startSpeechRecognition: () -> Unit,
//    onCancelInput: () -> Unit
//) {
//    Box(
//        modifier = Modifier
//            .fillMaxSize()
//    ) {
//        // Background Image
//        Image(
//            painter = painterResource(id = R.drawable.main_background_img),
//            contentDescription = null,
//            modifier = Modifier.fillMaxSize(),
//            contentScale = androidx.compose.ui.layout.ContentScale.Crop
//        )
//
//        // Foreground Content
//        Box(
//            modifier = Modifier
//                .fillMaxSize()
//                .background(
//                    Brush.verticalGradient(
//                        colors = listOf(Color(0xFF1F002A), Color(0xFF1F002A))
//                    )
//                )
//                .padding(16.dp)
//        ) {
//            Column(
//                modifier = Modifier.fillMaxSize(),
//                verticalArrangement = Arrangement.Top,
//                horizontalAlignment = Alignment.Start
//            ) {
//                AnimatedVisibility(visible = !isExpanded.value) {
//                    GreetingSection()
//                }
//
//                Spacer(modifier = Modifier.height(24.dp))
//
//                Row(
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .clip(RoundedCornerShape(16.dp))
//                        .background(Color(0xFF20FFFFFF))
//                        .padding(horizontal = 16.dp, vertical = 12.dp),
//                    verticalAlignment = Alignment.CenterVertically
//                ) {
//                    if (isExpanded.value) {
//                        IconButton(
//                            onClick = {
//                                isExpanded.value = false
//                            },
//                            modifier = Modifier.size(24.dp)
//                        ) {
//                            Icon(
//                                imageVector = Icons.Default.ArrowBack,
//                                contentDescription = "Back",
//                                tint = Color.White
//                            )
//                        }
//                    } else {
//                        Image(
//                            painter = painterResource(id = R.drawable.search_star_icon),
//                            contentDescription = "Star icon",
//                            modifier = Modifier.size(28.dp)
//                        )
//                    }
//
//                    Spacer(modifier = Modifier.width(12.dp))
//
//                    BasicTextField(
//                        value = liveSpeechInput.value,
//                        onValueChange = { liveSpeechInput.value = it },
//                        textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
//                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
//                        keyboardActions = KeyboardActions(
//                            onSearch = {
//                                if (liveSpeechInput.value.isNotBlank()) {
//                                    chatViewModel.sendMessage(liveSpeechInput.value)
//                                    liveSpeechInput.value = ""
//                                    isExpanded.value = true
//                                }
//                            }
//                        ),
//                        cursorBrush = SolidColor(Color.White),
//                        modifier = Modifier.weight(1f).padding(4.dp),
//                        decorationBox = { innerTextField ->
//                            if (liveSpeechInput.value.isEmpty()) {
//                                Text(
//                                    text = "Ask the universe...",
//                                    style = TextStyle(color = Color(0xFFAAAAAA), fontSize = 16.sp)
//                                )
//                            }
//                            innerTextField()
//                        }
//                    )
//
//                    Spacer(modifier = Modifier.width(12.dp))
//
//                    if (liveSpeechInput.value.isNotEmpty()) {
//                        IconButton(
//                            onClick = onCancelInput,
//                            modifier = Modifier.size(24.dp)
//                        ) {
//                            Icon(
//                                imageVector = Icons.Default.Mic,
//                                contentDescription = "Cancel Input",
//                                tint = Color.White
//                            )
//                        }
//                    } else {
//                        IconButton(
//                            onClick = startSpeechRecognition,
//                            modifier = Modifier.size(24.dp)
//                        ) {
//                            Icon(
//                                imageVector = Icons.Default.Mic,
//                                contentDescription = "Voice Input",
//                                tint = Color.White
//                            )
//                        }
//                    }
//                }
//
//                Spacer(modifier = Modifier.height(24.dp))
//
//                if (isExpanded.value) {
//                    ExpandedContent(chatMessages)
//                } else {
//                    DefaultContent(chatMessages)
//                }
//            }
//        }
//    }
//}
//
//@Composable
//fun GreetingSection() {
//    Column(
//        modifier = Modifier
//            .fillMaxWidth()
//            .padding(top = 40.dp),
//        horizontalAlignment = Alignment.Start
//    ) {
//        Text(
//            text = "Hi, Good Morning",
//            color = Color.White,
//            fontSize = 12.sp
//        )
//        Text(
//            text = "Ankit Tiwari",
//            color = Color.White,
//            fontSize = 20.sp
//        )
//    }
//}
//
//@Composable
//fun ExpandedContent(chatMessages: List<ChatMessage>) {
//    LazyColumn(
//        modifier = Modifier
//            .fillMaxWidth()
//            .padding(8.dp),
//        verticalArrangement = Arrangement.spacedBy(8.dp)
//    ) {
//        item {
//            SuggestionsHeader()
//        }
//
//        item {
//            Spacer(modifier = Modifier.height(16.dp))
//            Divider(
//                color = Color.Gray.copy(alpha = 0.5f),
//                thickness = 1.dp
//            )
//            Spacer(modifier = Modifier.height(16.dp))
//        }
//
//        items(chatMessages) { message ->
//            if (message.isUser) {
//                UserMessageBox(message.userMessage)
//            } else {
//                AIResponseBox(message.botResponse)
//            }
//        }
//    }
//}
//
//@Composable
//fun SuggestionsHeader() {
//    Column(modifier = Modifier.fillMaxWidth()) {
//        Text(
//            text = "Suggestions",
//            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium.copy(color = Color.White),
//            modifier = Modifier.padding(bottom = 8.dp)
//        )
//
//        val suggestions = listOf(
//            "View my Kundli-based predictions",
//            "Get daily insights from my birth chart",
//            "When is the next full moon?",
//            "Explore zodiac compatibility",
//            "Generate a new Kundli"
//        )
//
//        suggestions.forEach { suggestion ->
//            Spacer(modifier = Modifier.height(8.dp))
//
//            Box(
//                modifier = Modifier
//                    .clip(RoundedCornerShape(8.dp))
//                    .background(Color(0xFF9575CD))
//                    .clickable {
//                        // Handle suggestion click (e.g., send as query)
//                    }
//                    .padding(horizontal = 16.dp, vertical = 8.dp)
//            ) {
//                Row(verticalAlignment = Alignment.CenterVertically) {
//                    Text(
//                        text = suggestion,
//                        style = TextStyle(color = Color.White, fontSize = 16.sp)
//                    )
//
//                    Spacer(modifier = Modifier.width(8.dp))
//
//                    Icon(
//                        imageVector = Icons.Default.ArrowForward,
//                        contentDescription = "Arrow",
//                        tint = Color.White,
//                        modifier = Modifier.size(20.dp)
//                    )
//                }
//            }
//        }
//
//        Spacer(modifier = Modifier.height(8.dp))
//    }
//}
//
//@Composable
//fun DefaultContent(chatMessages: List<ChatMessage>) {
//    LazyColumn(
//        modifier = Modifier
//            .fillMaxWidth()
//            .padding(8.dp),
//        verticalArrangement = Arrangement.spacedBy(8.dp)
//    ) {
//        items(chatMessages) { message ->
//            if (message.isUser) {
//                UserMessageBox(message.userMessage)
//            } else {
//                AIResponseBox(message.botResponse)
//            }
//        }
//    }
//}
//
//@Composable
//fun UserMessageBox(message: String) {
//    Box(
//        modifier = Modifier
//            .fillMaxWidth()
//            .background(
//                Color(0xFFE5D9F2),
//                RoundedCornerShape(8.dp)
//            )
//            .padding(8.dp)
//    ) {
//        Text(
//            text = "Search: $message",
//            color = Color.Black
//        )
//    }
//}
//
//@Composable
//fun AIResponseBox(message: String) {
//    Column(
//        modifier = Modifier
//            .fillMaxWidth()
//            .padding(start = 8.dp, bottom = 8.dp)
//    ) {
//        Text(
//            text = message,
//            color = Color.White
//        )
//    }
//}
