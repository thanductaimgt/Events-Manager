package zalo.taitd.calendar

import androidx.recyclerview.widget.DiffUtil

class EventDiffUtil :DiffUtil.ItemCallback<Event>(){
    override fun areItemsTheSame(
        oldItem: Event,
        newItem: Event
    ): Boolean {
        return oldItem == newItem
    }

    override fun areContentsTheSame(
        oldItem: Event,
        newItem: Event
    ): Boolean {
        return oldItem.id == newItem.id
    }

    override fun getChangePayload(oldItem: Event, newItem: Event): Any? {
        val res = ArrayList<Int>()
        if (oldItem.title != newItem.title) {
            res.add(Event.PAYLOAD_TITLE)
        }
        if (oldItem.location != newItem.location) {
            res.add(Event.PAYLOAD_LOCATION)
        }
        if (oldItem.startTime != newItem.startTime || oldItem.endTime != newItem.endTime) {
            res.add(Event.PAYLOAD_TIME)
        }
        return res
    }
}