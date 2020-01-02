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


class MainActivity : AppCompatActivity(), View.OnClickListener {
    private val compositeDisposable = CompositeDisposable()
    private val adapter = EventAdapter(EventDiffUtil())
    private lateinit var accounts: HashMap<String, Account>
    private var curAccountName: String? = null
    private lateinit var viewEventDialog: ViewEventDialog
    private lateinit var popupMenu: PopupMenu
    private var curAccountEvents: List<Event> = ArrayList()
    private lateinit var confirmDeleteEventsDialog: ConfirmDeleteEventsDialog

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

    private fun viewEventIfStartFromNotification(intent: Intent){
        if (intent.action == Constants.ACTION_VIEW_EVENT) {
            val eventId = intent.getLongExtra(Constants.EXTRA_EVENT_ID, -1)
            if (eventId != -1L) {
                CalendarProviderDAO.getEventAsync(
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
            CalendarProviderDAO.getEventsAsync(
                this,
                GetEventsObserver(),
                curAccountName!!
            )
        } else {
            CalendarProviderDAO.getAccountsAsync(this, GetAccountsObserver())
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

        confirmDeleteEventsDialog = ConfirmDeleteEventsDialog(supportFragmentManager)

        arrowUpImgView.setOnClickListener(this)
        arrowDownImgView.setOnClickListener(this)
        addEventImgView.setOnClickListener(this)
        refreshImgView.setOnClickListener(this)
        chooseCalendarsImgView.setOnClickListener(this)
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
                    curAccountName = spinnerAdapter.getItem(position)
                    SharePreferenceManager.setCurAccount(this@MainActivity, curAccountName!!)

                    CalendarProviderDAO.getEventsAsync(
                        this@MainActivity,
                        GetEventsObserver(),
                        curAccountName!!
                    )
                }
            }
    }

    private fun initChooseCalendarMenu(){
        popupMenu = PopupMenu(this, chooseCalendarsImgView)

        //registering popup with OnMenuItemClickListener
        popupMenu.setOnMenuItemClickListener { menuItem ->
            val calendarId = menuItem.itemId.toLong()

            if (menuItem.isChecked) {
                adapter.updateEvents(adapter.currentList.filter { it.calendarId != calendarId })

                SharePreferenceManager.addCalendarToAccExcludedCalendars(
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

                SharePreferenceManager.removeCalendarFromAccExcludedCalendars(
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

                viewEventDialog.show(event, true)
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

                val isEditable =
                    accounts[curAccountName!!]!!.calendars[event.calendarId]!!.isEditable()
                viewEventDialog.show(event, false, isEditable)
            }
            R.id.refreshImgView -> {
                swipeRefresh.isRefreshing = true
                CalendarProviderDAO.getEventsAsync(
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

                viewEventDialog.show(event, true)
            }
            R.id.deleteImgView -> {
                val position = recyclerView.getChildLayoutPosition(view.parent.parent as View)
                val event = adapter.currentList[position]

                confirmDeleteEventsDialog.show(ArrayList<Long>().apply { add(event.id!!) })
            }
            R.id.chooseCalendarsImgView -> {
                showChooseCalendarsMenu()
            }
        }
    }

    fun deleteEvents(eventsId: List<Long>) {
        CalendarProviderDAO.deleteEventsAsync(this, eventsId, DeleteEventsObserver(eventsId))
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
            SharePreferenceManager.getCurAccountName(this) ?: accountKeySorted.firstOrNull()
        SharePreferenceManager.setCurAccount(this@MainActivity, curAccountName!!)

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
        val exCalendarsId = SharePreferenceManager.getAccExcludedCalendarsId(this, curAccountName!!)

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

            val exCalendarsId = SharePreferenceManager.getAccExcludedCalendarsId(
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
            viewEventDialog.show(t, false, isEditable)
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

            getData()
        }

        override fun onSubscribe(d: Disposable) {
            compositeDisposable.add(d)
        }

        override fun onError(e: Throwable) {
            e.printStackTrace()
        }
    }
}
