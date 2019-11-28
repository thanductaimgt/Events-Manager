package zalo.taitd.calendar

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentUris
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.CalendarContract
import android.util.Log
import android.view.View
import android.widget.AdapterView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import androidx.recyclerview.widget.LinearLayoutManager
import io.reactivex.CompletableObserver
import io.reactivex.SingleObserver
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity(), View.OnClickListener {
    private val compositeDisposable = CompositeDisposable()
    private val adapter = EventAdapter(EventDiffUtil())
    private var curAccountName: String? = null

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initView()

        if (hasPermissions(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR)) {
            getData()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR),
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
        recyclerView.setItemViewCacheSize(30)

        swipeRefresh.setOnRefreshListener {
            getData()
        }

        initAccountSpinner()

        addEventImgView.setOnClickListener(this)
        refreshImgView.setOnClickListener(this)
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
                    Utils.setCurAccount(this@MainActivity, curAccountName!!)

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
                val intent = Intent(Intent.ACTION_INSERT)
                    .setData(CalendarContract.Events.CONTENT_URI)
                    .putExtra(CalendarContract.Events.OWNER_ACCOUNT, curAccountName)
                startActivity(intent)
            }
            R.id.itemEvent -> {
                val position = recyclerView.getChildLayoutPosition(view)
                val eventId = adapter.currentList[position].id

                val uri: Uri =
                    ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId)
                val intent = Intent(Intent.ACTION_VIEW).setData(uri)
                startActivity(intent)
            }
            R.id.refreshImgView -> {
                swipeRefresh.isRefreshing = true
                CalendarProviderDAO.getEventsAsync(
                    this,
                    GetCurAccountEventsObserver(),
                    curAccountName!!
                )
            }
        }
    }

    private fun createReminders(events: List<Event>) {
        events.forEach {
            Utils.createRemindersAsync(this, it, CreateRemindersObserver())
        }
    }

    override fun onDestroy() {
        compositeDisposable.clear()
        super.onDestroy()
    }

    private fun updateAccountList(accountNames: ArrayList<String>) {
        curAccountName = Utils.getCurAccount(this) ?: accountNames.firstOrNull()
        Utils.setCurAccount(this@MainActivity, curAccountName!!)

        val spinnerAdapter = (spinner.adapter as AccountSpinnerAdapter)
        spinnerAdapter.clear()
        spinnerAdapter.addAll(accountNames)
        spinner.setSelection(spinnerAdapter.getPosition(curAccountName))
        spinnerAdapter.notifyDataSetChanged()
    }

    private fun hasPermissions(vararg permissionsId: String): Boolean {
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
    //

    inner class GetCurAccountEventsObserver : SingleObserver<List<Event>> {
        override fun onSuccess(t: List<Event>) {
            Log.d(TAG, "get events of $curAccountName success")
            adapter.submitList(t)
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

    inner class GetOtherAccountsEventsObserver : SingleObserver<List<Event>> {
        override fun onSuccess(t: List<Event>) {
            Log.d(TAG, "get other accounts events success")

            createReminders(t)
        }

        override fun onSubscribe(d: Disposable) {
            compositeDisposable.add(d)
        }

        override fun onError(e: Throwable) {
            e.printStackTrace()
        }
    }

    inner class GetAccountsObserver : SingleObserver<ArrayList<String>> {
        override fun onSuccess(t: ArrayList<String>) {
            this@MainActivity.updateAccountList(t)

            if (t.isNotEmpty()) CalendarProviderDAO.getEventsAsync(
                this@MainActivity,
                GetOtherAccountsEventsObserver(),
                curAccountName!!,
                true
            )
        }

        override fun onSubscribe(d: Disposable) {
            compositeDisposable.add(d)
        }

        override fun onError(e: Throwable) {
            e.printStackTrace()
        }
    }

    inner class CreateRemindersObserver : CompletableObserver {
        override fun onComplete() {
        }

        override fun onSubscribe(d: Disposable) {
            compositeDisposable.add(d)
        }

        override fun onError(e: Throwable) {
            e.printStackTrace()
        }

    }
}
