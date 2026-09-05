package com.pix.dayline.ui.spaces

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.pix.dayline.model.*
import com.pix.dayline.ui.components.FloatingControls
import com.pix.dayline.ui.theme.composeColor

@Composable fun SpacesScreen(spaces:List<DaylineSpace>,items:List<DaylineItem>,onMenu:()->Unit,onToday:()->Unit,onAdd:()->Unit,onSpace:(DaylineSpace)->Unit){
Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(top=WindowInsets.statusBars.asPaddingValues().calculateTopPadding(),bottom=WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding())){
Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start=30.dp,end=30.dp,top=56.dp,bottom=138.dp)){Text("Spaces",style=MaterialTheme.typography.displayMedium);Spacer(Modifier.height(6.dp));Text("\\ Personal",style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant);Spacer(Modifier.height(28.dp));spaces.forEach{space->Row(Modifier.fillMaxWidth().padding(vertical=12.dp).clickable(interactionSource=remember{MutableInteractionSource()},indication=null,onClick={onSpace(space)}),verticalAlignment=Alignment.CenterVertically){Box(Modifier.size(62.dp).clip(CircleShape).background(space.color.composeColor()));Spacer(Modifier.width(20.dp));Column{Text(space.name,style=MaterialTheme.typography.titleMedium);Text("${items.count{it.spaceId==space.id}} items",style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)}}};Spacer(Modifier.height(16.dp));Text("+ New space",modifier=Modifier.clickable(interactionSource=remember{MutableInteractionSource()},indication=null,onClick=onAdd).padding(vertical=12.dp),style=MaterialTheme.typography.titleMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)}
FloatingControls(Modifier.align(Alignment.BottomEnd).padding(end=20.dp,bottom=28.dp),showAdd=false,onMenu=onMenu,onToday=onToday)}}
