package com.example.borrowbox.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.example.borrowbox.MainActivity
import com.example.borrowbox.data.local.BorrowBoxDatabase
import kotlinx.coroutines.flow.first

class BorrowBoxWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val database = BorrowBoxDatabase.getInstance(context)
        val now = System.currentTimeMillis()
        val overdueCount = try { database.borrowingDao().countOverdueBorrowings(now).first() } catch (e: Exception) { 0 }
        val activeCount = try { database.borrowingDao().countActiveBorrowings().first() } catch (e: Exception) { 0 }

        provideContent {
            GlanceTheme {
                WidgetContent(
                    overdueCount = overdueCount,
                    activeCount = activeCount
                )
            }
        }
    }

    @Composable
    private fun WidgetContent(overdueCount: Int, activeCount: Int) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(androidx.glance.color.ColorProvider(day = androidx.compose.ui.graphics.Color(0xFF1E1E1E), night = androidx.compose.ui.graphics.Color(0xFF1E1E1E)))
                .padding(16.dp)
                .clickable(actionStartActivity<MainActivity>()),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "BorrowBox Overview",
                style = TextStyle(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = androidx.glance.color.ColorProvider(day = androidx.compose.ui.graphics.Color(0xFFA78BFA), night = androidx.compose.ui.graphics.Color(0xFFA78BFA))
                )
            )
            Spacer(modifier = GlanceModifier.height(12.dp))
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = GlanceModifier.defaultWeight()) {
                    Text(
                        text = activeCount.toString(),
                        style = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold, color = androidx.glance.color.ColorProvider(day = androidx.compose.ui.graphics.Color.White, night = androidx.compose.ui.graphics.Color.White))
                    )
                    Text(
                        text = "Active",
                        style = TextStyle(fontSize = 11.sp, color = androidx.glance.color.ColorProvider(day = androidx.compose.ui.graphics.Color.LightGray, night = androidx.compose.ui.graphics.Color.LightGray))
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = GlanceModifier.defaultWeight()) {
                    Text(
                        text = overdueCount.toString(),
                        style = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold, color = androidx.glance.color.ColorProvider(day = androidx.compose.ui.graphics.Color(0xFFE57373), night = androidx.compose.ui.graphics.Color(0xFFE57373)))
                    )
                    Text(
                        text = "Overdue",
                        style = TextStyle(fontSize = 11.sp, color = androidx.glance.color.ColorProvider(day = androidx.compose.ui.graphics.Color.LightGray, night = androidx.compose.ui.graphics.Color.LightGray))
                    )
                }
            }
        }
    }
}
