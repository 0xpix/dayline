package com.pix.dayline.ui.upcoming

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pix.dayline.model.*
import com.pix.dayline.ui.components.FloatingControls
import com.pix.dayline.ui.theme.composeColor
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun UpcomingScreen(items: List<DaylineItem>, spaces: List<DaylineSpace>, onMenu: () -> Unit, onToday: () -> Unit, onAdd: (LocalDate) -> Unit, onEdit: (DaylineItem) -> Unit, onToggleTask: (DaylineItem, LocalDate) -> Unit) {
    val today=remember{LocalDate.now()}; val dates=remember(items,today){(0L..60L).map{today.plusDays(it)}.filter{d->items.any{it.occursOn(d)}}}
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(top=WindowInsets.statusBars.asPaddingValues().calculateTopPadding(),bottom=WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding())){
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start=30.dp,end=30.dp,top=56.dp,bottom=138.dp)){
            Text("Upcoming",style=MaterialTheme.typography.displayMedium); Spacer(Modifier.height(28.dp))
            if(dates.isEmpty()) Text("Nothing upcoming.",style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
            dates.forEach{date-> val dayItems=items.filter{it.occursOn(date)}.sortedWith(compareBy<DaylineItem>{it.startTime==null}.thenBy{it.startTime}); UpcomingDay(date,today,dayItems,spaces,onEdit,onToggleTask)}
        }
        FloatingControls(Modifier.align(Alignment.BottomEnd).padding(end=20.dp,bottom=28.dp),onMenu=onMenu,onToday=onToday,onAdd={onAdd(today)})
    }
}

@Composable private fun UpcomingDay(date:LocalDate,today:LocalDate,items:List<DaylineItem>,spaces:List<DaylineSpace>,onEdit:(DaylineItem)->Unit,onToggleTask:(DaylineItem,LocalDate)->Unit){
    val accent=items.firstOrNull()?.color?.composeColor()?:MaterialTheme.colorScheme.onBackground
    Row(Modifier.fillMaxWidth().padding(bottom=34.dp),verticalAlignment=Alignment.Top){
        Text(date.dayOfMonth.toString(),style=MaterialTheme.typography.displayMedium.copy(fontSize=46.sp,lineHeight=48.sp),color=if(date==today)accent else MaterialTheme.colorScheme.onBackground.copy(alpha=if(date.isBefore(today.plusDays(2)))1f else .30f),modifier=Modifier.width(72.dp))
        Column(Modifier.weight(1f)){
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.End){ Column(horizontalAlignment=Alignment.End){Text(if(date==today)"Today" else date.dayOfWeek.getDisplayName(TextStyle.FULL,Locale.getDefault()),style=MaterialTheme.typography.labelMedium);Text(date.month.getDisplayName(TextStyle.FULL,Locale.getDefault()),style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)} }
            Spacer(Modifier.height(16.dp))
            items.forEach{item-> val done=item.kind==AgendaKind.TASK&&item.isCompletedOn(date); Row(Modifier.fillMaxWidth().padding(vertical=5.dp).clickable(interactionSource=remember{MutableInteractionSource()},indication=null,onClick={if(item.kind==AgendaKind.TASK)onToggleTask(item,date)else onEdit(item)}),verticalAlignment=Alignment.CenterVertically){
                Text(if(done)"DONE" else item.startTime?.format(DateTimeFormatter.ofPattern("HH:mm"))?:if(item.kind==AgendaKind.TASK)"TODO" else "ALL",modifier=Modifier.width(64.dp),style=MaterialTheme.typography.labelMedium,color=item.color.composeColor().copy(alpha=if(done).45f else 1f))
                Column(Modifier.weight(1f)){Text(item.title,style=MaterialTheme.typography.bodyLarge,color=MaterialTheme.colorScheme.onBackground.copy(alpha=if(done).45f else 1f));spaces.firstOrNull{it.id==item.spaceId}?.let{Text(it.name,style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)}}
            }}
        }
    }
}
