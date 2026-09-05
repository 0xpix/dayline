package com.pix.dayline.ui.spaces

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.pix.dayline.model.*
import com.pix.dayline.ui.theme.composeColor
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class) @Composable fun SpaceEditSheet(editing:DaylineSpace?=null,onSave:(DaylineSpace)->Unit,onDelete:((DaylineSpace)->Unit)?=null,onDismiss:()->Unit){
var name by remember(editing?.id){mutableStateOf(TextFieldValue(editing?.name.orEmpty()))};var color by remember(editing?.id){mutableStateOf(editing?.color?:ItemColor.BLUE)}
ModalBottomSheet(onDismissRequest=onDismiss,containerColor=MaterialTheme.colorScheme.background){Column(Modifier.padding(horizontal=28.dp,vertical=10.dp)){Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text(if(editing==null)"New space" else "Edit space",style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurfaceVariant);if(editing!=null&&onDelete!=null)Text("Delete",modifier=Modifier.clickable{onDelete(editing)},style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)};Spacer(Modifier.height(20.dp));BasicTextField(name,{name=it},Modifier.fillMaxWidth(),textStyle=MaterialTheme.typography.headlineLarge.copy(color=MaterialTheme.colorScheme.onBackground),decorationBox={inner->Box{if(name.text.isBlank())Text("Space name",style=MaterialTheme.typography.headlineLarge,color=MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha=.45f));inner()}});Spacer(Modifier.height(28.dp));Text("Color",style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurfaceVariant);Spacer(Modifier.height(12.dp));Row(horizontalArrangement=Arrangement.spacedBy(14.dp)){ItemColor.entries.forEach{c->Box(Modifier.size(30.dp).border(if(c==color)2.dp else 1.dp,if(c==color)MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.outline,CircleShape).padding(4.dp).background(c.composeColor(),CircleShape).clickable{color=c})}};Spacer(Modifier.height(30.dp));Button(onClick={if(name.text.isNotBlank())onSave(DaylineSpace(editing?.id?:UUID.randomUUID().toString(),name.text.trim(),color))},modifier=Modifier.align(Alignment.End),shape=CircleShape,enabled=name.text.isNotBlank()){Text("Save")};Spacer(Modifier.height(22.dp))}}
}
