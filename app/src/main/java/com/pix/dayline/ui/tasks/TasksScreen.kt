package com.pix.dayline.ui.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pix.dayline.model.*
import com.pix.dayline.ui.components.FloatingControls
import com.pix.dayline.ui.theme.composeColor
import java.time.LocalDate

@Composable fun TasksScreen(items:List<DaylineItem>,spaces:List<DaylineSpace>,onMenu:()->Unit,onToday:()->Unit,onAdd:(LocalDate)->Unit,onOpenTask:(DaylineItem)->Unit,onToggleTask:(DaylineItem,LocalDate)->Unit){val today=remember{LocalDate.now()};val tasks=items.filter{it.kind==AgendaKind.TASK}.sortedBy{it.startDate};Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(top=WindowInsets.statusBars.asPaddingValues().calculateTopPadding(),bottom=WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding())){Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start=30.dp,end=30.dp,top=112.dp,bottom=138.dp)){Text("Tasks",style=MaterialTheme.typography.displayMedium);Spacer(Modifier.height(30.dp));if(tasks.isEmpty())Text("No tasks yet.",style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant);tasks.forEach{task->val completed=task.occursOn(today)&&task.isCompletedOn(today);val space=spaces.firstOrNull{it.id==task.spaceId};Row(Modifier.fillMaxWidth().padding(vertical=12.dp).clickable(interactionSource=remember{MutableInteractionSource()},indication=null){onOpenTask(task)},verticalAlignment=Alignment.CenterVertically){Box(Modifier.size(20.dp).background(task.color.composeColor().copy(alpha=.12f),CircleShape).clickable{if(task.occursOn(today))onToggleTask(task,today)});Spacer(Modifier.width(14.dp));Column(Modifier.weight(1f)){Text(task.title,style=MaterialTheme.typography.bodyLarge,color=MaterialTheme.colorScheme.onBackground.copy(alpha=if(completed).4f else 1f));if(space!=null)Text(space.name,style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)};Text(if(completed)"DONE" else "TODO",style=MaterialTheme.typography.labelMedium,color=task.color.composeColor())}}};FloatingControls(Modifier.align(Alignment.BottomEnd).padding(end=20.dp,bottom=28.dp),onMenu=onMenu,onToday=onToday,onAdd={onAdd(today)})}}
