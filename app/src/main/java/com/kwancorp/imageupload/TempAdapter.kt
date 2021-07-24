package com.kwancorp.imageupload

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.kwancorp.imageupload.databinding.ItemImageRecyclerviewBinding
import com.kwancorp.imageupload.databinding.TempBinding

class TempAdapter: ListAdapter<Item, TempAdapter.ViewHolder>(diffUtilCallback) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding: TempBinding = DataBindingUtil.inflate (
            inflater, R.layout.temp, parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    class ViewHolder(
        private val binding: TempBinding
    ): RecyclerView.ViewHolder(binding.root)  {
        fun bind(item: Item) {
            binding.item = item
            binding.executePendingBindings()
        }
    }

    companion object {
        val diffUtilCallback = object: DiffUtil.ItemCallback<Item>() {
            override fun areItemsTheSame(oldItem: Item, newItem: Item): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: Item, newItem: Item): Boolean {
                return oldItem == newItem
            }
        }
    }
}