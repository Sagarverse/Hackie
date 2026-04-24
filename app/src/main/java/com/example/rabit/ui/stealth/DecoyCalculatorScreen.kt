package com.example.rabit.ui.stealth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun DecoyCalculatorScreen(
    viewModel: DecoyViewModel,
    onUnlock: () -> Unit
) {
    val display by viewModel.calculatorDisplay.collectAsState()
    
    // Check if secret is entered via a side effect or button logic
    // In our case, the ViewModel will emit an event or we check the buffer on '='

    Column(
        modifier = Modifier.fillMaxSize().background(Color.Black).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(Modifier.weight(1f))
        
        Text(
            text = display,
            color = Color.White,
            fontSize = 64.sp,
            fontWeight = FontWeight.Light,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.End,
            maxLines = 1
        )

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            CalcButton("C", Color.LightGray, Color.Black, modifier = Modifier.weight(1f)) { viewModel.onClear() }
            CalcButton("±", Color.LightGray, Color.Black, modifier = Modifier.weight(1f)) {}
            CalcButton("%", Color.LightGray, Color.Black, modifier = Modifier.weight(1f)) {}
            CalcButton("÷", Color(0xFFFF9F0A), Color.White, modifier = Modifier.weight(1f)) { viewModel.onOperatorClick("÷") }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            CalcButton("7", Color(0xFF333333), Color.White, modifier = Modifier.weight(1f)) { viewModel.onNumberClick("7") }
            CalcButton("8", Color(0xFF333333), Color.White, modifier = Modifier.weight(1f)) { viewModel.onNumberClick("8") }
            CalcButton("9", Color(0xFF333333), Color.White, modifier = Modifier.weight(1f)) { viewModel.onNumberClick("9") }
            CalcButton("×", Color(0xFFFF9F0A), Color.White, modifier = Modifier.weight(1f)) { viewModel.onOperatorClick("×") }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            CalcButton("4", Color(0xFF333333), Color.White, modifier = Modifier.weight(1f)) { viewModel.onNumberClick("4") }
            CalcButton("5", Color(0xFF333333), Color.White, modifier = Modifier.weight(1f)) { viewModel.onNumberClick("5") }
            CalcButton("6", Color(0xFF333333), Color.White, modifier = Modifier.weight(1f)) { viewModel.onNumberClick("6") }
            CalcButton("-", Color(0xFFFF9F0A), Color.White, modifier = Modifier.weight(1f)) { viewModel.onOperatorClick("-") }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            CalcButton("1", Color(0xFF333333), Color.White, modifier = Modifier.weight(1f)) { viewModel.onNumberClick("1") }
            CalcButton("2", Color(0xFF333333), Color.White, modifier = Modifier.weight(1f)) { viewModel.onNumberClick("2") }
            CalcButton("3", Color(0xFF333333), Color.White, modifier = Modifier.weight(1f)) { viewModel.onNumberClick("3") }
            CalcButton("+", Color(0xFFFF9F0A), Color.White, modifier = Modifier.weight(1f)) { viewModel.onOperatorClick("+") }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            CalcButton("0", Color(0xFF333333), Color.White, modifier = Modifier.weight(2f)) { viewModel.onNumberClick("0") }
            CalcButton(".", Color(0xFF333333), Color.White, modifier = Modifier.weight(1f)) { viewModel.onNumberClick(".") }
            CalcButton("=", Color(0xFFFF9F0A), Color.White, modifier = Modifier.weight(1f)) { 
                if (display == "1337") onUnlock() else viewModel.onOperatorClick("=") 
            }
        }
    }
}

@Composable
fun CalcButton(
    text: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier.aspectRatio(if (text == "0") 2f else 1f),
        shape = CircleShape,
        color = containerColor,
        onClick = onClick
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(text = text, color = contentColor, fontSize = 28.sp, fontWeight = FontWeight.Medium)
        }
    }
}
