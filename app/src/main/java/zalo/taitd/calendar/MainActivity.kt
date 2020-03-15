package zalo.taitd.calendar

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import androidx.recyclerview.widget.LinearLayoutManager
import io.reactivex.CompletableObserver
import io.reactivex.SingleObserver
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.activity_main.*
import zalo.taitd.calendar.adapters.AccountSpinnerAdapter
import zalo.taitd.calendar.adapters.EventAdapter
import zalo.taitd.calendar.fragments.ViewEventDialog
import zalo.taitd.calendar.models.Account
import zalo.taitd.calendar.models.Event
import zalo.taitd.calendar.utils.Constants
import zalo.taitd.calendar.utils.EventDiffUtil
import zalo.taitd.calendar.utils.TAG
import android.view.MenuItem
import zalo.taitd.calendar.fragments.ConfirmDeleteEventsDialog
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap


class MainActivity : AppCompatActivity(), View.OnClickListener, View.OnLongClickListener {
    private val compositeDisposable = CompositeDisposable()
    private val adapter = EventAdapter(EventDiffUtil())
    private lateinit var accounts: HashMap<String, Account>
    private var curAccountName: String? = null
    private lateinit var viewEventDialog: ViewEventDialog
    private lateinit var popupMenu: PopupMenu
    private var curAccountEvents: List<Event> = ArrayList()
    private lateinit var confirmDeleteEventsDialog: ConfirmDeleteEventsDialog
    val selectedEventsId = LinkedList<Long>()

    private val permissionsId = arrayOf(
        Manifest.permission.READ_CALENDAR,
        Manifest.permission.WRITE_CALENDAR,
        Manifest.permission.RECEIVE_BOOT_COMPLETED,
        Manifest.permission.READ_SYNC_SETTINGS,
        Manifest.permission.WRITE_SYNC_SETTINGS
    )

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        Log.d(TAG, "onNewIntent")
        intent?.let {
            viewEventIfStartFromNotification(it)
        }
    }

    private fun viewEventIfStartFromNotification(intent: Intent) {
        if (intent.action == Constants.ACTION_VIEW_EVENT) {
            val eventId = intent.getLongExtra(Constants.EXTRA_EVENT_ID, -1)
            if (eventId != -1L) {
                CalendarManager.getEventAsync(
                    this@MainActivity,
                    GetEventObserver(),
                    eventId
                )
            }
        }
    }

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initView()

        if (hasPermissions(permissionsId)) {
            getData()
        } else {
            ActivityCompat.requestPermissions(
                this,
                permissionsId,
                Constants.CALENDAR_PERMISSIONS_REQUEST
            )
        }
    }

    fun getData() {
        if (curAccountName != null) {
            CalendarManager.getEventsAsync(
                this,
                GetEventsObserver(),
                curAccountName!!
            )
        } else {
            CalendarManager.getAccountsAsync(this, GetAccountsObserver())
        }
    }

    private fun initView() {
        setContentView(R.layout.activity_main)

        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.setItemViewCacheSize(100)

        swipeRefresh.setOnRefreshListener {
            getData()
        }

        viewEventDialog = ViewEventDialog(supportFragmentManager)

        initAccountSpinner()
        initChooseCalendarMenu()
        initSelectLayout()

        confirmDeleteEventsDialog = ConfirmDeleteEventsDialog(supportFragmentManager)

        arrowUpImgView.setOnClickListener(this)
        arrowDownImgView.setOnClickListener(this)
        addEventImgView.setOnClickListener(this)
        refreshImgView.setOnClickListener(this)
        chooseCalendarsImgView.setOnClickListener(this)
    }

    private fun initSelectLayout() {
        selectAllImgView.setOnClickListener(this)
        discardAllImgView.setOnClickListener(this)
        deleteAllImgView.setOnClickListener(this)
    }

    private fun initAccountSpinner() {
        val spinnerAdapter = AccountSpinnerAdapter(this, spinner)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = spinnerAdapter
        spinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onNothingSelected(parent: AdapterView<*>?) {
                    adapter.updateEvents(null, null)
                }

                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    deselectAllEvents()

                    curAccountName = spinnerAdapter.getItem(position)
                    SharePrefsManager.setCurAccount(this@MainActivity, curAccountName!!)

                    CalendarManager.getEventsAsync(
                        this@MainActivity,
                        GetEventsObserver(),
                        curAccountName!!
                    )
                }
            }
    }

    private fun initChooseCalendarMenu() {
        popupMenu = PopupMenu(this, chooseCalendarsImgView)

        //registering popup with OnMenuItemClickListener
        popupMenu.setOnMenuItemClickListener { menuItem ->
            val calendarId = menuItem.itemId.toLong()

            if (menuItem.isChecked) {
                val eventsIdToBeRemoved = adapter.currentList.filter { it.calendarId == calendarId }
                deselectEvents(eventsIdToBeRemoved.map { it.id!! })

                adapter.updateEvents(adapter.currentList.toMutableList().apply {
                    removeAll(
                        eventsIdToBeRemoved
                    )
                })

                SharePrefsManager.addCalendarToAccExcludedCalendars(
                    this,
                    calendarId,
                    curAccountName!!
                )

                menuItem.isChecked = false
            } else {
                adapter.updateEvents(
                    adapter.currentList.toMutableList().apply {
                        addAll(
                            curAccountEvents.filter { it.calendarId == calendarId }
                        )
                    }.sortedBy { it.startTime }
                )

                SharePrefsManager.removeCalendarFromAccExcludedCalendars(
                    this,
                    calendarId,
                    curAccountName!!
                )

                menuItem.isChecked = true
            }

            menuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW)
            menuItem.actionView = View(this)
            menuItem.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
                override fun onMenuItemActionExpand(item: MenuItem): Boolean {
                    return false
                }

                override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
                    return false
                }
            })
            return@setOnMenuItemClickListener false
        }
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.addEventImgView -> {
//                val intent = Intent(Intent.ACTION_INSERT)
//                    .setData(CalendarContract.Events.CONTENT_URI)
//                    .putExtra(CalendarContract.Events.OWNER_ACCOUNT, curAccountName)
//                startActivity(intent)
                val event = Event(
                    accountName = curAccountName!!,
                    calendarId = getAccountPrimaryCalendarId(curAccountName!!)
                )

                viewEventDialog.show(event, ViewEventDialog.Session.SESSION_CREATE)
            }
            R.id.itemEvent -> {
//                val position = recyclerView.getChildLayoutPosition(view)
//                val eventId = adapter.currentList[position].id
//
//                val uri: Uri =
//                    ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId)
//                val intent = Intent(Intent.ACTION_VIEW).setData(uri)
//                startActivity(intent)
                val position = recyclerView.getChildLayoutPosition(view)
                val event = adapter.currentList[position]

                if (selectedEventsId.isEmpty()) {
                    val isEditable =
                        accounts[curAccountName!!]!!.calendars[event.calendarId]!!.isEditable()
                    viewEventDialog.show(event, ViewEventDialog.Session.SESSION_VIEW, isEditable)
                } else {
                    if (accounts[curAccountName!!]!!.calendars[event.calendarId]!!.isEditable()) {
                        selectOrDeselectEvent(position)
                    } else {
                        Toast.makeText(
                            this,
                            getString(R.string.cannot_perform_action),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
            R.id.refreshImgView -> {
                swipeRefresh.isRefreshing = true
                CalendarManager.getEventsAsync(
                    this,
                    GetEventsObserver(),
                    curAccountName!!
                )
            }
            R.id.arrowDownImgView -> recyclerView.scrollToPosition(adapter.currentList.lastIndex)
            R.id.arrowUpImgView -> recyclerView.scrollToPosition(0)
            R.id.editImgView -> {
                val position = recyclerView.getChildLayoutPosition(view.parent.parent as View)
                val event = adapter.currentList[position]

                viewEventDialog.show(event, ViewEventDialog.Session.SESSION_EDIT)
            }
            R.id.deleteImgView -> {
                val position = recyclerView.getChildLayoutPosition(view.parent.parent as View)
                val event = adapter.currentList[position]

                confirmDeleteEventsDialog.show(ArrayList<Long>().apply { add(event.id!!) })
            }
            R.id.chooseCalendarsImgView -> {
                showChooseCalendarsMenu()
            }
            R.id.deleteAllImgView -> {
                confirmDeleteEventsDialog.show(selectedEventsId)
            }
            R.id.discardAllImgView -> {
                deselectAllEvents()
            }
            R.id.selectAllImgView -> {
                selectAllEvents()
            }
        }
    }

    override fun onLongClick(view: View): Boolean {
        when (view.id) {
            R.id.itemEvent -> {
                val position = recyclerView.getChildLayoutPosition(view)
                val event = adapter.currentList[position]
                if (accounts[curAccountName!!]!!.calendars[event.calendarId]!!.isEditable()) {
                    selectOrDeselectEvent(position)
                } else {
                    Toast.makeText(
                        this,
                        getString(R.string.cannot_perform_action),
                        Toast.LENGTH_SHORT
                    ).show()
                }
                return true
            }
        }
        return false
    }

    private fun selectOrDeselectEvent(position: Int) {
        val event = adapter.currentList[position]
        // if selectedEvents not contains event ...
        if (!selectedEventsId.removeAll { it == event.id }) {
            selectedEventsId.add(event.id!!)
        }
        updateSelectLayout()
        adapter.notifyItemChanged(
            position,
            ArrayList<Int>().apply { add(Event.PAYLOAD_SELECT_STATE) })
    }

    private fun deselectEvents(eventIds: List<Long>) {
        selectedEventsId.removeAll(eventIds)
        updateSelectLayout()
        adapter.notifyDataSetChanged()
    }

    private fun deselectAllEvents() {
        selectedEventsId.clear()
        updateSelectLayout()
        adapter.notifyDataSetChanged()
    }

    private fun selectAllEvents() {
        selectedEventsId.addAll(adapter.currentList.map { it.id!! })
        updateSelectLayout()
        adapter.notifyDataSetChanged()
    }

    private fun updateSelectLayout() {
        if (selectedEventsId.isEmpty()) {
            hideSelectLayout()
        } else {
            selectCountTextView.text =
                String.format("${getString(R.string.selected)}: %d", selectedEventsId.size)
            showSelectLayout()
        }
    }

    private fun showSelectLayout() {
        selectLayout.apply {
            if (visibility == View.GONE) {
                visibility = View.VISIBLE
            }
        }
    }

    private fun hideSelectLayout() {
        selectLayout.apply {
            if (visibility == View.VISIBLE) {
                visibility = View.GONE
            }
        }
    }

    fun deleteEvents(eventsId: List<Long>) {
        CalendarManager.deleteEventsAsync(this, eventsId, DeleteEventsObserver(eventsId))
    }

    override fun onDestroy() {
        compositeDisposable.clear()
        super.onDestroy()
    }

    private fun getAccountPrimaryCalendarId(accountName: String): Long {
        return accounts[accountName]!!.calendars.values.first { it.isPrimary }.id
    }

    private fun updateAccountList(accounts: HashMap<String, Account>) {
        this.accounts = accounts

        val accountKeySorted = accounts.keys.sorted()

        curAccountName =
            SharePrefsManager.getCurAccountName(this) ?: accountKeySorted.firstOrNull()
        SharePrefsManager.setCurAccount(this@MainActivity, curAccountName!!)

        val spinnerAdapter = (spinner.adapter as AccountSpinnerAdapter)
        spinnerAdapter.clear()
        spinnerAdapter.addAll(accountKeySorted)
        spinner.setSelection(spinnerAdapter.getPosition(curAccountName))
        spinnerAdapter.notifyDataSetChanged()
    }

    private fun hasPermissions(permissionsId: Array<String>): Boolean {
        var hasPermissions = true
        for (p in permissionsId) {
            hasPermissions =
                hasPermissions && ContextCompat.checkSelfPermission(
                    this,
                    p
                ) == PermissionChecker.PERMISSION_GRANTED
        }

        return hasPermissions
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            Constants.CALENDAR_PERMISSIONS_REQUEST -> {
                if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                    getData()
                } else {
                    Log.e(TAG, "request permissions fail")
                    finish()
                }
            }
            else -> Log.d(TAG, "unknown requestCode: $requestCode")
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun showChooseCalendarsMenu() {
        val curAccountCalendars = accounts[curAccountName]!!.calendars
        val exCalendarsId = SharePrefsManager.getAccExcludedCalendarsId(this, curAccountName!!)

        popupMenu.menu.removeGroup(0)

        curAccountCalendars.values.forEach { calendar ->
            val calendarId = calendar.id.toInt()
            popupMenu.menu.add(0, calendarId, calendarId, calendar.displayName)
            popupMenu.menu.findItem(calendarId).isChecked =
                !exCalendarsId.contains(calendarId.toString())
        }

        popupMenu.menu.setGroupCheckable(0, true, false)

        popupMenu.show() //showing popup menu
    }

    //

    inner class GetEventsObserver : SingleObserver<List<Event>> {
        override fun onSuccess(t: List<Event>) {
            curAccountEvents = t.sortedBy { it.startTime }

            val exCalendarsId = SharePrefsManager.getAccExcludedCalendarsId(
                this@MainActivity,
                curAccountName!!
            )

            adapter.updateEvents(
                curAccountEvents.filter { !exCalendarsId.contains(it.calendarId.toString()) },
                accounts[curAccountName!!]
            )

            swipeRefresh.isRefreshing = false
        }

        override fun onSubscribe(d: Disposable) {
            compositeDisposable.add(d)
        }

        override fun onError(e: Throwable) {
            swipeRefresh.isRefreshing = false
            e.printStackTrace()
        }
    }

    inner class GetEventObserver : SingleObserver<Event> {
        override fun onSuccess(t: Event) {
            val isEditable = accounts[t.accountName]!!.calendars[t.calendarId]!!.isEditable()
            viewEventDialog.show(t, ViewEventDialog.Session.SESSION_VIEW, isEditable)
        }

        override fun onSubscribe(d: Disposable) {
            compositeDisposable.add(d)
        }

        override fun onError(e: Throwable) {
            e.printStackTrace()
        }
    }

    inner class GetAccountsObserver : SingleObserver<HashMap<String, Account>> {
        override fun onSuccess(t: HashMap<String, Account>) {
            updateAccountList(t)

            // show event if start app from notification
            viewEventIfStartFromNotification(intent)
        }

        override fun onSubscribe(d: Disposable) {
            compositeDisposable.add(d)
        }

        override fun onError(e: Throwable) {
            e.printStackTrace()
        }
    }

    inner class DeleteEventsObserver(private val eventsId: List<Long>) : CompletableObserver {
        override fun onComplete() {
            Toast.makeText(
                this@MainActivity,
                getString(
                    if (eventsId.size == 1)
                        R.string.event_deleted
                    else
                        R.string.events_deleted
                ),
                Toast.LENGTH_SHORT
            ).show()

            deselectEvents(eventsId)
            getData()

            if (!viewEventDialog.isCreateSession() &&
                eventsId.contains(viewEventDialog.originalEvent!!.id) &&
                viewEventDialog.isShown()) {
                viewEventDialog.dismiss()
            }
        }

        override fun onSubscribe(d: Disposable) {
            compositeDisposable.add(d)
        }

        override fun onError(e: Throwable) {
            e.printStackTrace()
            deselectEvents(eventsId)
        }
    }
}
