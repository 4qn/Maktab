package com.edu.maktab.pdf


import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.edu.maktab.databinding.ItemPdfNameBinding
import com.edu.maktab.utils.FileUtils
import java.io.File


class PdfNameListAdapter : ListAdapter<File, PdfNameListAdapter.PdfNameViewHolder>(
    DIFF_CALLBACK
) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PdfNameViewHolder {
        val item = ItemPdfNameBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PdfNameViewHolder(item)
    }

    override fun onBindViewHolder(holder: PdfNameViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class PdfNameViewHolder(private val itemBinding: ItemPdfNameBinding) :
        RecyclerView.ViewHolder(itemBinding.root), View.OnClickListener {
        private var data: Map<String, Any>? = null
        private var file: File? = null

        init {
            itemView.setOnClickListener(this)
        }

        fun bind(item: File) {

            file = item
            itemBinding.textNo.text = (adapterPosition + 1).toString()
            itemBinding.title.text = item.name
//            itemBinding.author.text = data!!["Author"].toString()
//            itemBinding.tags.text = data!!["Book_Language"].toString()
//            itemBinding.category.text = data!!["Book_Category"].toString()
//            itemBinding.textRack.text = data!!["Book_Rack"].toString()
//             itemBinding.title.text = data?.get("Publisher").toString()
//             itemBinding.title.text = data?.get("Pages").toString()
//             itemBinding.title.text = data?.get("Price").toString()
//             itemBinding.title.text = data?.get("Description").toString()
        }

        override fun onClick(v: View?) {
            FileUtils.openFile(itemBinding.root.context, file)
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<File>() {
            override fun areItemsTheSame(
                oldItem: File,
                newItem: File
            ): Boolean {
                return oldItem == newItem
            }

            override fun areContentsTheSame(
                oldItem: File,
                newItem: File
            ): Boolean {
                return newItem.toString() == oldItem.toString()
            }
        }
    }
}