package zalo.taitd.calendar

import android.Manifest
import android.annotation.SuppressLint
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
import io.reactivex.SingleObserver
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.activity_main.*
import zalo.taitd.calendar.adapters.AccountSpinnerAdapter
import zalo.taitd.calendar.adapters.EventAdapter
import zalo.taitd.calendar.models.Event
import zalo.taitd.calendar.utils.Constants
import zalo.taitd.calendar.utils.EventDiffUtil
import zalo.taitd.calendar.utils.TAG


class MainActivity : AppCompatActivity(), View.OnClickListener {
    private val compositeDisposable = CompositeDisposable()
    private val adapter = EventAdapter(EventDiffUtil())
    private var curAccountName: String? = null

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

        initAccountSpinner()

        arrowDownImgView.setOnClickListener(this)
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
            R.id.arrowDownImgView -> recyclerView.scrollToPosition(adapter.currentList.lastIndex)
        }
    }

    override fun onDestroy() {
        compositeDisposable.clear()
        super.onDestroy()
    }

    private fun updateAccountList(accountNames: ArrayList<String>) {
        curAccountName = SharePreferenceManager.getCurAccount(this) ?: accountNames.firstOrNull()
        SharePreferenceManager.setCurAccount(this@MainActivity, curAccountName!!)

        val spinnerAdapter = (spinner.adapter as AccountSpinnerAdapter)
        spinnerAdapter.clear()
        spinnerAdapter.addAll(accountNames)
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
    //

    inner class GetCurAccountEventsObserver : SingleObserver<List<Event>> {
        override fun onSuccess(t: List<Event>) {
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

    inner class GetAccountsObserver : SingleObserver<ArrayList<String>> {
        override fun onSuccess(t: ArrayList<String>) {
            this@MainActivity.updateAccountList(t)
        }

        override fun onSubscribe(d: Disposable) {
            compositeDisposable.add(d)
        }

        override fun onError(e: Throwable) {
            e.printStackTrace()
        }
    }
}
