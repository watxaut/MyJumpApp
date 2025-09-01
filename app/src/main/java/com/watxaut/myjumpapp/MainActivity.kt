package com.watxaut.myjumpapp

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.rememberNavController
import com.watxaut.myjumpapp.presentation.navigation.MyJumpAppNavigation
import com.watxaut.myjumpapp.presentation.ui.theme.MyJumpAppTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Keep screen on for the entire app session (camera functionality)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        
        enableEdgeToEdge()
        setContent {
            MyJumpAppTheme {
                MyJumpApp()
            }
        }
    }
}

@Composable
fun MyJumpApp() {
    val navController = rememberNavController()
    MyJumpAppNavigation(navController = navController)
}

@Preview(showBackground = true)
@Composable
fun MyJumpAppPreview() {
    MyJumpAppTheme {
        MyJumpApp()
    }
}