package zalo.taitd.calendar.fragments

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import kotlinx.android.synthetic.main.dialog_confirm.*
import zalo.taitd.calendar.MainActivity
import zalo.taitd.calendar.R
import zalo.taitd.calendar.utils.TAG


class ConfirmDeleteEventsDialog(private val fm: FragmentManager) : DialogFragment(),
    View.OnClickListener {
    private var eventsId: List<Long> = ArrayList()

    override fun onClick(v: View?) {
        when (v!!.id) {
            R.id.cancelTextView -> dismiss()
            R.id.confirmTextView -> {
                (context as MainActivity).deleteEvents(eventsId)
                dismiss()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_confirm, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        initView()
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    private fun initView() {
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog?.window?.requestFeature(Window.FEATURE_NO_TITLE)

        if (eventsId.size == 1) {
            titleTextView.text = getString(R.string.delete_event_title)
            descTextView.text = getString(R.string.delete_event_desc)
        } else {
            titleTextView.text = getString(R.string.delete_events_title)
            descTextView.text = getString(R.string.delete_events_desc)
        }

        confirmTextView.setOnClickListener(this)
        cancelTextView.setOnClickListener(this)
    }

    fun show(eventsId: List<Long>) {
        this.eventsId = eventsId
        show(fm, TAG)
    }
}