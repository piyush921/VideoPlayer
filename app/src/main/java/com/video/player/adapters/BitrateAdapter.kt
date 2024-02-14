package com.video.player.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.video.player.R
import com.video.player.models.BitrateModel

class BitrateAdapter(private val list: List<BitrateModel>, private val listener: BitrateListener) :
    RecyclerView.Adapter<BitrateAdapter.BitrateViewHolder>() {

    companion object {
        const val ITEM_UNSELECTED = 0
        const val ITEM_SELECTED = 1
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BitrateViewHolder {
        val view: View = when (viewType) {
            ITEM_SELECTED -> {
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_bitrate_selected, parent, false)
            }

            else -> {
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_bitrate, parent, false)
            }
        }
        return BitrateViewHolder(view)
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun getItemViewType(position: Int): Int {
        return if (list[position].isSelected) {
            ITEM_SELECTED
        } else {
            ITEM_UNSELECTED
        }
    }

    override fun onBindViewHolder(holder: BitrateViewHolder, position: Int) {
        holder.bitrateValue.text = list[position].bitrate

        holder.bitrateValue.setOnClickListener {
            if (!list[holder.layoutPosition].isSelected) {
                listener.onBitrateSelect(list[holder.layoutPosition])
            }
        }
    }


    inner class BitrateViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var bitrateValue: TextView = itemView.findViewById(R.id.bitrate_value)
    }

    interface BitrateListener {
        fun onBitrateSelect(bitrate: BitrateModel)
    }
}