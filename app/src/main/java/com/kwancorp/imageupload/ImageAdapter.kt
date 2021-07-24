package com.kwancorp.imageupload

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.kwancorp.imageupload.databinding.ItemImageRecyclerviewBinding

class ImageAdapter: ListAdapter<Image, ImageAdapter.ViewHolder>(diffUtilCallback) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding: ItemImageRecyclerviewBinding = DataBindingUtil.inflate (
            inflater, R.layout.item_image_recyclerview, parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    class ViewHolder(
        private val binding: ItemImageRecyclerviewBinding
    ): RecyclerView.ViewHolder(binding.root)  {
        fun bind(image: Image) {
            binding.imageView.setImageBitmap(image.bitmap)
        }
    }

    companion object {
        val diffUtilCallback = object: DiffUtil.ItemCallback<Image>() {
            override fun areItemsTheSame(oldItem: Image, newItem: Image): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: Image, newItem: Image): Boolean {
                return oldItem == newItem
            }
        }
    }
}