package com.pix.dayline.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.pix.dayline.data.*
import com.pix.dayline.model.*
import com.pix.dayline.notifications.NotificationScheduler
import com.pix.dayline.ui.calendar.CalendarScreen
import com.pix.dayline.ui.components.*
import com.pix.dayline.ui.settings.SettingsScreen
import com.pix.dayline.ui.spaces.*
import com.pix.dayline.ui.tasks.*
import com.pix.dayline.ui.theme.DaylineTheme
import com.pix.dayline.ui.today.TodayScreen
import com.pix.dayline.ui.upcoming.UpcomingScreen
import java.time.LocalDate

enum class DaylineScreen { TODAY, CALENDAR, UPCOMING, TASKS, SPACES, SETTINGS }
private data class AddRequest(val date:LocalDate,val kind:AgendaKind)

@Composable fun DaylineApp(){val context=LocalContext.current;val appContext=context.applicationContext;val store=remember(context){DaylineStore(appContext)};var items by remember{mutableStateOf(store.loadItems())};var spaces by remember{mutableStateOf(store.loadSpaces())};var appearance by remember{mutableStateOf(store.loadAppearance())};var showOrb by remember{mutableStateOf(store.loadShowOrb())};var weekStartsMonday by remember{mutableStateOf(store.loadWeekStartsMonday())};val permissionLauncher=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()){};LaunchedEffect(Unit){NotificationScheduler.syncAll(appContext,items)};val dark=when(appearance){Appearance.SYSTEM->isSystemInDarkTheme();Appearance.LIGHT->false;Appearance.DARK->true};DaylineTheme(darkTheme=dark){var screen by remember{mutableStateOf(DaylineScreen.TODAY)};var menuOpen by remember{mutableStateOf(false)};var addRequest by remember{mutableStateOf<AddRequest?>(null)};var editing by remember{mutableStateOf<DaylineItem?>(null)};var taskDetail by remember{mutableStateOf<DaylineItem?>(null)};var spaceEditing by remember{mutableStateOf<DaylineSpace?>(null)};var newSpace by remember{mutableStateOf(false)}
fun saveAll(next:List<DaylineItem>){items=next;store.saveItems(next);NotificationScheduler.syncAll(appContext,next)}
fun saveItem(item:DaylineItem){saveAll(if(items.any{it.id==item.id})items.map{if(it.id==item.id)item else it}else items+item);if(taskDetail?.id==item.id)taskDetail=item;if(item.reminderMinutes!=null&&Build.VERSION.SDK_INT>=Build.VERSION_CODES.TIRAMISU&&ContextCompat.checkSelfPermission(context,Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);editing=null;addRequest=null}
fun deleteItem(item:DaylineItem){NotificationScheduler.cancel(appContext,item);saveAll(items.filterNot{it.id==item.id});editing=null;taskDetail=null}
fun toggleTask(item:DaylineItem,date:LocalDate){saveItem(item.copy(completedDates=item.completedDates.toMutableSet().apply{if(date in this)remove(date)else add(date)}))}
fun saveSpaces(next:List<DaylineSpace>){spaces=next;store.saveSpaces(next)}
val detail=taskDetail;if(detail!=null){val live=items.firstOrNull{it.id==detail.id}?:detail;TaskDetailScreen(live,spaces,{taskDetail=null},::saveItem,::deleteItem)}else when(screen){DaylineScreen.TODAY->TodayScreen(items,showOrb,{menuOpen=true},{addRequest=AddRequest(it,AgendaKind.EVENT)},{if(it.kind==AgendaKind.TASK)taskDetail=it else editing=it},::toggleTask);DaylineScreen.CALENDAR->CalendarScreen(items,weekStartsMonday,{menuOpen=true},{screen=DaylineScreen.TODAY},{addRequest=AddRequest(it,AgendaKind.EVENT)},{if(it.kind==AgendaKind.TASK)taskDetail=it else editing=it},::toggleTask);DaylineScreen.UPCOMING->UpcomingScreen(items,spaces,{menuOpen=true},{screen=DaylineScreen.TODAY},{addRequest=AddRequest(it,AgendaKind.EVENT)},{if(it.kind==AgendaKind.TASK)taskDetail=it else editing=it},::toggleTask);DaylineScreen.TASKS->TasksScreen(items,spaces,{menuOpen=true},{screen=DaylineScreen.TODAY},{addRequest=AddRequest(it,AgendaKind.TASK)},{taskDetail=it},::toggleTask);DaylineScreen.SPACES->SpacesScreen(spaces,items,{menuOpen=true},{screen=DaylineScreen.TODAY},{newSpace=true},{spaceEditing=it});DaylineScreen.SETTINGS->SettingsScreen(appearance,showOrb,weekStartsMonday,{appearance=it;store.saveAppearance(it)},{showOrb=it;store.saveShowOrb(it)},{weekStartsMonday=it;store.saveWeekStartsMonday(it)},{menuOpen=true},{screen=DaylineScreen.TODAY})}
if(menuOpen)NavigationSheet(screen,{screen=it;menuOpen=false;taskDetail=null},{menuOpen=false});editing?.let{item->QuickAddSheet(item.startDate,item.kind,item,spaces,::saveItem,::deleteItem){editing=null}}?:addRequest?.let{req->QuickAddSheet(req.date,req.kind,spaces=spaces,onSave=::saveItem){addRequest=null}};if(newSpace)SpaceEditSheet(onSave={saveSpaces(spaces+it);newSpace=false},onDismiss={newSpace=false});spaceEditing?.let{s->SpaceEditSheet(s,onSave={u->saveSpaces(spaces.map{if(it.id==u.id)u else it});spaceEditing=null},onDelete={d->saveSpaces(spaces.filterNot{it.id==d.id});saveAll(items.map{if(it.spaceId==d.id)it.copy(spaceId=null)else it});spaceEditing=null},onDismiss={spaceEditing=null})}}}
