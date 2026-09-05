package com.pix.dayline.ui.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pix.dayline.model.*
import com.pix.dayline.ui.theme.composeColor
import java.time.format.DateTimeFormatter
import java.util.UUID

@Composable fun TaskDetailScreen(item:DaylineItem,spaces:List<DaylineSpace>,onBack:()->Unit,onSave:(DaylineItem)->Unit,onDelete:(DaylineItem)->Unit){
var working by remember(item.id){mutableStateOf(item)};var detailText by remember(item.id){mutableStateOf(TextFieldValue(""))};val space=spaces.firstOrNull{it.id==working.spaceId}
Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(top=WindowInsets.statusBars.asPaddingValues().calculateTopPadding(),bottom=WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding())){
Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start=32.dp,end=32.dp,top=54.dp,bottom=110.dp)){Text("‹ Tasks",modifier=Modifier.clickable{onBack()},style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurfaceVariant);Spacer(Modifier.height(54.dp));Text(working.title,style=MaterialTheme.typography.displayMedium.copy(fontSize=38.sp,lineHeight=40.sp));Spacer(Modifier.height(28.dp));MetaRow("▦",space?.name?:"No space");MetaRow("◷",buildString{append(working.startDate.format(DateTimeFormatter.ofPattern("EEE, MMM d")));working.startTime?.let{append(", ${it.format(DateTimeFormatter.ofPattern("HH:mm"))}")}});MetaRow("♢",working.reminderMinutes?.let{"$it min before"}?:"No reminder");Spacer(Modifier.height(28.dp));working.details.forEach{detail->Row(Modifier.fillMaxWidth().padding(vertical=7.dp),verticalAlignment=Alignment.CenterVertically){Box(Modifier.size(20.dp).border(1.5.dp,working.color.composeColor().copy(alpha=.65f),CircleShape).clickable{working=working.copy(details=working.details.map{if(it.id==detail.id)it.copy(done=!it.done)else it});onSave(working)});Spacer(Modifier.width(14.dp));Text(detail.text,style=MaterialTheme.typography.bodyLarge,color=MaterialTheme.colorScheme.onBackground.copy(alpha=if(detail.done).4f else 1f))}};Row(Modifier.fillMaxWidth().padding(top=10.dp),verticalAlignment=Alignment.CenterVertically){Text("+",style=MaterialTheme.typography.titleMedium,color=working.color.composeColor());Spacer(Modifier.width(12.dp));BasicTextField(detailText,{detailText=it},Modifier.weight(1f),textStyle=MaterialTheme.typography.bodyLarge.copy(color=MaterialTheme.colorScheme.onBackground),singleLine=true,decorationBox={inner->Box{if(detailText.text.isBlank())Text("Detail",style=MaterialTheme.typography.bodyLarge,color=MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha=.45f));inner()}});if(detailText.text.isNotBlank())Text("Add",modifier=Modifier.clickable{working=working.copy(details=working.details+TaskDetail(UUID.randomUUID().toString(),detailText.text.trim()));detailText=TextFieldValue("");onSave(working)},style=MaterialTheme.typography.labelMedium)};Spacer(Modifier.height(50.dp));Text("Delete",modifier=Modifier.clickable{onDelete(working)},style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)}
Text("✓",modifier=Modifier.align(Alignment.BottomEnd).padding(28.dp).size(52.dp).background(MaterialTheme.colorScheme.onBackground,CircleShape).clickable{onSave(working);onBack()}.padding(13.dp),style=MaterialTheme.typography.titleMedium,color=MaterialTheme.colorScheme.background)}}
@Composable private fun MetaRow(icon:String,value:String){Row(Modifier.padding(vertical=5.dp),verticalAlignment=Alignment.CenterVertically){Text(icon,modifier=Modifier.width(30.dp),style=MaterialTheme.typography.bodyLarge);Text(value,style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)}}
