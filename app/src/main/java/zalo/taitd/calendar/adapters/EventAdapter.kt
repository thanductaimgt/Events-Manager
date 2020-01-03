package zalo.taitd.calendar.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_event.view.*
import zalo.taitd.calendar.MainActivity
import zalo.taitd.calendar.R
import zalo.taitd.calendar.models.Account
import zalo.taitd.calendar.models.Event
import zalo.taitd.calendar.utils.EventDiffUtil
import zalo.taitd.calendar.utils.Utils

class EventAdapter(eventDiffUtil: EventDiffUtil) :
    ListAdapter<Event, EventAdapter.EventViewHolder>(eventDiffUtil) {

    var account: Account? = null

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): EventViewHolder {
        return EventViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.item_event, parent, false)
        ).apply { itemView.setOnClickListener(parent.context as View.OnClickListener) }
    }

    override fun onBindViewHolder(
        holder: EventViewHolder,
        position: Int
    ) {
        holder.bind(position)
    }

    override fun onBindViewHolder(
        holder: EventViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (payloads.isNotEmpty()) {
            val event = currentList[position]

            (payloads[0] as ArrayList<*>).forEach {
                when (it) {
                    Event.PAYLOAD_TITLE -> holder.bindTitle(event)
                    Event.PAYLOAD_TIME -> holder.bindTime(event)
                    Event.PAYLOAD_LOCATION -> holder.bindLocation(event)
                    Event.PAYLOAD_SELECT_STATE -> holder.bindSelectState(event)
                }
            }
        } else {
            onBindViewHolder(holder, position)
        }
    }

    fun updateEvents(events: List<Event>?, account: Account? = this.account) {
        this.account = account
        submitList(events)
    }

    inner class EventViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(position: Int) {
            val event = currentList[position]

            bindTitle(event)
            bindTime(event)
            bindLocation(event)
            bindSelectState(event)

            itemView.apply {
                editImgView.setOnClickListener(context as View.OnClickListener)
                deleteImgView.setOnClickListener(context as View.OnClickListener)
                itemEvent.setOnClickListener(context as View.OnClickListener)
                itemEvent.setOnLongClickListener(context as View.OnLongClickListener)
            }
        }

        fun bindTitle(event: Event) {
            itemView.titleEditText.text =
                if (event.title != "") event.title else itemView.context.getString(R.string.no_title)
        }

        fun bindLocation(event: Event) {
            itemView.apply {
                if (event.location != "") {
                    locationTextView.text = event.location
                    locationIcon.visibility = View.VISIBLE
                    locationTextView.visibility = View.VISIBLE
                } else {
                    locationIcon.visibility = View.GONE
                    locationTextView.visibility = View.GONE
                }
            }
        }

        fun bindTime(event: Event) {
            itemView.startDateTextView.text = Utils.getDateFormat(event.startTime, false)
            itemView.startTimeTextView.text = Utils.getTimeFormat(event.startTime)
            itemView.endDateTextView.text = Utils.getDateFormat(event.endTime, false)
            itemView.endTimeTextView.text = Utils.getTimeFormat(event.endTime)
        }

        private fun bindActions(event: Event) {
            if (account!!.calendars[event.calendarId]!!.isEditable()) {
                itemView.editImgView.visibility = View.VISIBLE
                itemView.deleteImgView.visibility = View.VISIBLE
            } else {
                itemView.editImgView.visibility = View.INVISIBLE
                itemView.deleteImgView.visibility = View.INVISIBLE
            }
        }

        fun bindSelectState(event: Event) {
            (itemView as CardView).apply {
                val indexOfDownloadTask =
                    (context as MainActivity).selectedEventsId.indexOfFirst { it == event.id }
                if (indexOfDownloadTask != -1) {//contains
                    setCardBackgroundColor(ContextCompat.getColor(context, R.color.lightAccent))
                    editImgView.visibility = View.INVISIBLE
                    deleteImgView.visibility = View.INVISIBLE
                } else {
                    setCardBackgroundColor(ContextCompat.getColor(context, android.R.color.white))
                    bindActions(event)
                }
            }
        }
    }
}