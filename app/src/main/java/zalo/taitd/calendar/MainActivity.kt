package zalo.taitd.calendar

import android.Manifest
import android.annotation.SuppressLint
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


class MainActivity : AppCompatActivity(), View.OnClickListener {
    private val compositeDisposable = CompositeDisposable()
    private val adapter = EventAdapter(EventDiffUtil())
    private lateinit var accounts: HashMap<String, Account>
    private var curAccountName: String? = null
    private lateinit var viewEventDialog: ViewEventDialog
    private lateinit var popupMenu:PopupMenu
    private var curAccountEvents :List<Event> = ArrayList()

    private val permissionsId = arrayOf(
        Manifest.permission.READ_CALENDAR,
        Manifest.permission.WRITE_CALENDAR,
        Manifest.permission.RECEIVE_BOOT_COMPLETED,
        Manifest.permission.READ_SYNC_SETTINGS,
        Manifest.permission.WRITE_SYNC_SETTINGS
    )

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

    private fun getData() {
        if (curAccountName != null) {
            CalendarProviderDAO.getEventsAsync(
                this,
                GetCurAccountEventsObserver(),
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
                    adapter.submitList(null)
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
                        GetCurAccountEventsObserver(),
                        curAccountName!!
                    )
                }
            }
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.addEventImgView -> {
//                val intent = Intent(Intent.ACTION_INSERT)
//                    .setData(CalendarContract.Events.CONTENT_URI)
//                    .putExtra(CalendarContract.Events.OWNER_ACCOUNT, curAccountName)
//                startActivity(intent)
                val event = Event(accountName = curAccountName!!, calendarId = getAccountPrimaryCalendarId(curAccountName!!))

                viewEventDialog.show(event, false)
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

                viewEventDialog.show(event, false)
            }
            R.id.refreshImgView -> {
                swipeRefresh.isRefreshing = true
                CalendarProviderDAO.getEventsAsync(
                    this,
                    GetCurAccountEventsObserver(),
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
                val eventId = adapter.currentList[position].id!!

                CalendarProviderDAO.deleteEventAsync(this, eventId, DeleteEventObserver())
            }
            R.id.chooseCalendarsImgView -> {
                showChooseCalendarsMenu()
            }
        }
    }

    override fun onDestroy() {
        compositeDisposable.clear()
        super.onDestroy()
    }

    private fun getAccountPrimaryCalendarId(accountName: String): Long {
        return accounts[accountName]!!.calendars.first { it.isPrimary }.id
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

        popupMenu = PopupMenu(this, chooseCalendarsImgView)

        //registering popup with OnMenuItemClickListener
        popupMenu.setOnMenuItemClickListener { menuItem ->
            val calendarId = curAccountCalendars[menuItem.itemId].id

            if(menuItem.isChecked){
                adapter.submitList(
                    adapter.currentList.filter { it.calendarId != calendarId }
                )

                SharePreferenceManager.addCalendarToAccExcludedCalendars(this, calendarId, curAccountName!!)

                menuItem.isChecked = false
            }else{
                adapter.submitList(
                    adapter.currentList.toMutableList().apply {
                        addAll(
                            curAccountEvents.filter { it.calendarId == curAccountCalendars[menuItem.itemId].id }
                        )
                    }.sortedBy { it.startTime }
                )

                SharePreferenceManager.removeCalendarFromAccExcludedCalendars(this, calendarId, curAccountName!!)

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

        popupMenu.menu.removeGroup(0)

        curAccountCalendars.forEachIndexed { i, calendar ->
            popupMenu.menu.add(0, i, i, calendar.displayName)
            popupMenu.menu.findItem(i).isChecked = !exCalendarsId.contains(calendar.id.toString())
        }

        popupMenu.menu.setGroupCheckable(0, true, false)

        popupMenu.show() //showing popup menu
    }

    //

    inner class GetCurAccountEventsObserver : SingleObserver<List<Event>> {
        override fun onSuccess(t: List<Event>) {
            curAccountEvents = t.sortedBy { it.startTime }

            val exCalendarsId = SharePreferenceManager.getAccExcludedCalendarsId(this@MainActivity, curAccountName!!)

            adapter.submitList(
                curAccountEvents.filter { !exCalendarsId.contains(it.calendarId.toString()) }
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

    inner class GetAccountsObserver : SingleObserver<HashMap<String, Account>> {
        override fun onSuccess(t: HashMap<String, Account>) {
            this@MainActivity.updateAccountList(t)
        }

        override fun onSubscribe(d: Disposable) {
            compositeDisposable.add(d)
        }

        override fun onError(e: Throwable) {
            e.printStackTrace()
        }
    }

    inner class DeleteEventObserver : CompletableObserver {
        override fun onComplete() {
            Toast.makeText(this@MainActivity, getString(R.string.event_deleted), Toast.LENGTH_SHORT).show()

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
