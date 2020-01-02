package zalo.taitd.calendar.fragments

import android.app.DatePickerDialog
import android.app.Dialog
import android.app.TimePickerDialog
import android.content.ContentUris
import android.content.ContentValues
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.provider.CalendarContract
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.animation.CycleInterpolator
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import kotlinx.android.synthetic.main.dialog_view_event.*
import zalo.taitd.calendar.MainActivity
import zalo.taitd.calendar.R
import zalo.taitd.calendar.models.Event
import zalo.taitd.calendar.utils.TAG
import zalo.taitd.calendar.utils.Utils
import java.util.*


class ViewEventDialog(private val fm: FragmentManager) : DialogFragment(),
    View.OnClickListener {
    private lateinit var originalEvent: Event
    private lateinit var editedEvent: Event
    private var isEditing = false
    private var isEdit = false
    private var isEditable = false
    private val startCalendar = Calendar.getInstance()
    private val endCalendar = Calendar.getInstance()

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    }

    override fun onPause() {
        editedEvent.title = titleEditText.text.toString()
        editedEvent.location = locationEditText.text.toString()
        editedEvent.description = descEditText.text.toString()
        super.onPause()
    }

    override fun onResume() {
        titleEditText.setText(editedEvent.title)
        locationEditText.setText(editedEvent.location)
        descEditText.setText(editedEvent.description)
        super.onResume()
    }

    override fun setupDialog(dialog: Dialog, style: Int) {
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_view_event, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        initView()
    }

    private fun initView() {
        // dialog full screen
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        startCalendar.time = originalEvent.startTime
        endCalendar.time = originalEvent.endTime

        startDateTV.text = Utils.getDateFormat(startCalendar.time)
        startTimeTV.text = Utils.getTimeFormat(startCalendar.time)

        endDateTV.text = Utils.getDateFormat(endCalendar.time)
        endTimeTV.text = Utils.getTimeFormat(endCalendar.time)

        titleEditText.setText(originalEvent.title)
        locationEditText.setText(originalEvent.location)
        descEditText.setText(originalEvent.description)

        hostTextView.text = originalEvent.accountName

        if (isEditing) {
            enableEdit()
        } else {
            disableEdit()
        }

        cancelImgView.setOnClickListener(this)
        saveImgView.setOnClickListener(this)
        if (isEditable) {
            editImgView.setOnClickListener(this)
        } else {
            editImgView.visibility = View.INVISIBLE
        }

        warningTextView.visibility = View.GONE

        chooseStartDateTimeImgView.setOnClickListener(this)
        chooseEndDateTimeImgView.setOnClickListener(this)
    }

    fun show(event: Event, isEdit: Boolean, isEditable: Boolean = true) {
        this.originalEvent = event
        this.editedEvent = event.copy()
        this.isEdit = isEdit
        this.isEditable = isEditable
        this.isEditing = isEdit

        if (dialog != null && dialog!!.isShowing) {
            initView()
        } else {
            show(fm, TAG)
        }
    }

    override fun onClick(p0: View?) {
        when (p0!!.id) {
            R.id.cancelImgView -> {
                if (originalEvent.id == null || isEdit || !isEditing) {
                    dismiss()
                } else {
                    initView()
                    disableEdit()
                    isEditing = false
                }
            }
            R.id.editImgView -> {
                enableEdit()
                isEditing = !isEditing
            }
            R.id.saveImgView -> {
                if (warningTextView.visibility == View.VISIBLE) {
                    warningTextView.animate()
                        .translationX(16f).interpolator = CycleInterpolator(7f)
                } else {
                    if (originalEvent.id == null) {
                        createEvent()
                        Toast.makeText(
                            context,
                            getString(R.string.event_created),
                            Toast.LENGTH_SHORT
                        )
                            .show()
                        dismiss()
                    } else {
                        updateEvent()
                        disableEdit()
                        isEditing = false
                        Toast.makeText(
                            context,
                            getString(R.string.event_edited),
                            Toast.LENGTH_SHORT
                        )
                            .show()
                        if (isEdit) {
                            dismiss()
                        }
                    }
                    (context as MainActivity).getData()
                }
            }
            R.id.chooseStartDateTimeImgView -> showDatePickerDialog(
                isStart = true,
                isPickBoth = true
            )
            R.id.chooseEndDateTimeImgView -> showDatePickerDialog(
                isStart = false,
                isPickBoth = true
            )
            R.id.startDateTV -> showDatePickerDialog(isStart = true, isPickBoth = false)
            R.id.startTimeTV -> showTimePickerDialog(isStart = true)
            R.id.endDateTV -> showDatePickerDialog(isStart = false, isPickBoth = false)
            R.id.endTimeTV -> showTimePickerDialog(isStart = false)
        }
    }

    private fun getCurrentValues(): ContentValues {
        return ContentValues().apply {
            put(CalendarContract.Events.TITLE, titleEditText.text.toString())
            put(CalendarContract.Events.DTSTART, startCalendar.time.time)
            put(CalendarContract.Events.DTEND, endCalendar.time.time)
            put(CalendarContract.Events.EVENT_LOCATION, locationEditText.text.toString())
            put(CalendarContract.Events.DESCRIPTION, descEditText.text.toString())
        }
    }

    private fun createEvent() {
        val values = getCurrentValues().apply {
            put(
                CalendarContract.Events.CALENDAR_ID,
                originalEvent.calendarId
            )
            put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().displayName)
        }
        context!!.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
    }

    private fun updateEvent() {
        val values = getCurrentValues()
        val updateUri =
            ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, originalEvent.id!!)
        context!!.contentResolver.update(updateUri, values, null, null)
    }

    private fun enableEdit() {
        titleEditText.isEnabled = true
        locationEditText.isEnabled = true
        descEditText.isEnabled = true

        editImgView.visibility = View.GONE
        saveImgView.visibility = View.VISIBLE

        chooseStartDateTimeImgView.visibility = View.VISIBLE
        chooseEndDateTimeImgView.visibility = View.VISIBLE

        val editTextBackground =
            ContextCompat.getDrawable(context!!, R.drawable.shape_round_corners_gray)

        titleEditText.background = editTextBackground
        locationEditText.background = editTextBackground
        descEditText.background = editTextBackground

        startDateTV.setOnClickListener(this)
        startTimeTV.setOnClickListener(this)

        endDateTV.setOnClickListener(this)
        endTimeTV.setOnClickListener(this)
    }

    private fun disableEdit() {
        titleEditText.isEnabled = false
        locationEditText.isEnabled = false
        descEditText.isEnabled = false

        editImgView.visibility = View.VISIBLE
        saveImgView.visibility = View.GONE

        chooseStartDateTimeImgView.visibility = View.INVISIBLE
        chooseEndDateTimeImgView.visibility = View.INVISIBLE

        val editTextBackground =
            ContextCompat.getDrawable(context!!, R.drawable.shape_padding_trans)

        titleEditText.background = editTextBackground
        locationEditText.background = editTextBackground
        descEditText.background = editTextBackground

        startDateTV.setOnClickListener(null)
        startTimeTV.setOnClickListener(null)

        endDateTV.setOnClickListener(null)
        endTimeTV.setOnClickListener(null)
    }

    private fun checkStartTimeEndTime() {
        if (startCalendar.timeInMillis > endCalendar.timeInMillis) {
            warningTextView.visibility = View.VISIBLE
        } else {
            warningTextView.visibility = View.GONE
        }
    }

    private fun showTimePickerDialog(isStart: Boolean) {
        val calendar: Calendar
        val textView: TextView

        if (isStart) {
            calendar = startCalendar
            textView = startTimeTV
        } else {
            calendar = endCalendar
            textView = endTimeTV
        }

        TimePickerDialog(
            context,
            TimePickerDialog.OnTimeSetListener { _, hourOfDay, minute ->
                calendar.apply {
                    set(Calendar.HOUR_OF_DAY, hourOfDay)
                    set(Calendar.MINUTE, minute)
                }
                textView.text = Utils.getTimeFormat(calendar.time)

                checkStartTimeEndTime()
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            true
        ).show()
    }

    private fun showDatePickerDialog(isStart: Boolean, isPickBoth: Boolean) {
        val calendar: Calendar
        val textView: TextView

        if (isStart) {
            calendar = startCalendar
            textView = startDateTV
        } else {
            calendar = endCalendar
            textView = endDateTV
        }

        DatePickerDialog(
            context!!,
            DatePickerDialog.OnDateSetListener { _, year, monthOfYear, dayOfMonth ->
                calendar.apply {
                    set(Calendar.DAY_OF_MONTH, dayOfMonth)
                    set(Calendar.MONTH, monthOfYear)
                    set(Calendar.YEAR, year)
                }
                textView.text = Utils.getDateFormat(calendar.time)
                if (isPickBoth) {
                    showTimePickerDialog(isStart)
                }

                checkStartTimeEndTime()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }
}