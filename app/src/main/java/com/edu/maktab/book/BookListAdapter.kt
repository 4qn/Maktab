package com.edu.maktab.book

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.Navigation
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.edu.maktab.databinding.ItemBookBinding
import com.edu.maktab.model.Book
import com.google.firebase.firestore.DocumentSnapshot
import com.google.gson.Gson

class BookListAdapter : ListAdapter<DocumentSnapshot, BookListAdapter.BookViewHolder>(
    DIFF_CALLBACK
) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookViewHolder {
        val item = ItemBookBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BookViewHolder(item)
    }

    override fun onBindViewHolder(holder: BookViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class BookViewHolder(private val itemBinding: ItemBookBinding) :
        RecyclerView.ViewHolder(itemBinding.root), View.OnClickListener {
        private var data: Map<String, Any>? = null
        private var id = ""

        init {
            itemView.setOnClickListener(this)
        }

        fun bind(item: DocumentSnapshot) {
            id = item.id
            data = item.data
            Log.d("TAG", "bind: before $data")

            itemBinding.textNo.text = (adapterPosition + 1).toString()
            itemBinding.title.text = data?.get("Book_Name")!!.toString()
            itemBinding.author.text = data!!["Author"].toString()
            itemBinding.tags.text = data!!["Book_Language"].toString()
            itemBinding.category.text = data!!["Book_Category"].toString()
            itemBinding.textRack.text = data!!["Book_Rack"].toString()
//             itemBinding.title.text = data?.get("Publisher").toString()
//             itemBinding.title.text = data?.get("Pages").toString()
//             itemBinding.title.text = data?.get("Price").toString()
//             itemBinding.title.text = data?.get("Description").toString()
        }

        override fun onClick(v: View?) {
            val gson = Gson()
            val string = gson.toJson(data)
            val book = gson.fromJson(string, Book::class.java)
            book.id = id

            val directions = BookListFragmentDirections.actionBookDetailFragment(book)
            Navigation.findNavController(v!!).navigate(directions)
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<DocumentSnapshot>() {
            override fun areItemsTheSame(
                oldItem: DocumentSnapshot,
                newItem: DocumentSnapshot
            ): Boolean {
                return oldItem == newItem
            }

            override fun areContentsTheSame(
                oldItem: DocumentSnapshot,
                newItem: DocumentSnapshot
            ): Boolean {
                return newItem.id == oldItem.id
            }
        }
    }
}