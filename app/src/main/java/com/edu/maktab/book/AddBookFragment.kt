package com.edu.maktab.book

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.edu.maktab.R
import com.edu.maktab.databinding.AddBookFragmentBinding
import com.edu.maktab.helper.AppConstant
import com.edu.maktab.helper.PreferenceHelper
import com.edu.maktab.helper.PreferenceHelper.get
import com.edu.maktab.model.Book
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.Source
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class AddBookFragment : Fragment() {

    private lateinit var viewModel: AddBookViewModel
    private var _binding: AddBookFragmentBinding? = null
    private val binding get() = _binding!!
    private val db = Firebase.firestore
    private val autoCompleterBooKName = ArrayList<String>()
    private val autoCompleterAuthor = ArrayList<String>()
    private val indexList = ArrayList<String>()
    private var authorAdapter: ArrayAdapter<String>? = null
    private var bookNameAdapter: ArrayAdapter<String>? = null
    private var libraryName: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = AddBookFragmentBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(this).get(AddBookViewModel::class.java)
        val args = AddBookFragmentArgs.fromBundle(requireArguments())
        val book = args.book
        val pref = PreferenceHelper.defaultPrefs(requireContext())
        libraryName = pref[AppConstant.LIBRARY_NAME]
        if (book != null) {
            binding.editTextBookName.setText(book.Book_Name)
            binding.editTextRack.setText(book.Book_Rack)
            binding.editTextIndex.setText(book.Number_On_Book)
            binding.editTextAuthor.setText(book.Author)
            binding.editTextCategory.setText(book.Book_Category)
            binding.editTextDesc.setText(book.Description)
            if (book.Pages != 0)
                binding.editTextPages.setText(book.Pages.toString())
            if (book.Price != 0f) {
                binding.editTextPrice.setText(book.Price.toString())
            }
            binding.editTextPublisher.setText(book.Publisher)
            binding.editTextLanguage.setText(book.Book_Language)
            binding.btnAddBook.text = getString(R.string.update)

        }
        binding.editTextIndex.doAfterTextChanged {
            if (it.toString().length > 2) {
                val indexElement = indexList.find { index -> index == it.toString() }
                if (indexElement.isNullOrEmpty())
                    binding.inputLayoutIndex.error = null
                else
                    binding.inputLayoutIndex.error = "Index already added"
            }

        }
        db.collection(Firebase.auth.currentUser?.uid!!)
            .get(Source.CACHE)
            .addOnSuccessListener { result ->

                result.documents.forEach {
                    autoCompleterBooKName.add(it.get("Book_Name").toString())
                    indexList.add(it.get("Number_On_Book").toString())
                    autoCompleterAuthor.add(it.get("Author").toString())
                    bookNameAdapter = ArrayAdapter(
                        requireContext(),
                        R.layout.language_list_item,
                        autoCompleterBooKName
                    )
                    authorAdapter = ArrayAdapter(
                        requireContext(),
                        R.layout.language_list_item,
                        autoCompleterAuthor
                    )
                    binding.editTextBookName.setAdapter(bookNameAdapter)
                    binding.editTextAuthor.setAdapter(authorAdapter)
                }

            }
            .addOnFailureListener { exception ->
                Log.w("c", "Error getting documents.", exception)
            }
        val items = listOf("اُردُو", "فارسی", "اَلْعَرَبِيَّةُ", "हिन्दी", "English", "Other")
        val adapter = ArrayAdapter(requireContext(), R.layout.language_list_item, items)

        binding.editTextLanguage.setAdapter(adapter)
        binding.btnAddBook.setOnClickListener {
            if (validate()) {
                addBook(book)
            }
        }
        return binding.root
    }

    private fun validate(): Boolean {
        return when {
            binding.editTextBookName.text.isNullOrEmpty() -> {
                binding.inputLayoutBookName.error = "Enter Book Name"
                false
            }
            binding.editTextCategory.text.isNullOrEmpty() -> {
                binding.inputLayoutBookCategory.error = "Enter Book Category"
                false
            }
            binding.editTextAuthor.text.isNullOrEmpty() -> {
                binding.inputLayoutAuthor.error = "Enter Author name"
                false
            }
            binding.editTextIndex.text.isNullOrEmpty() -> {
                binding.inputLayoutIndex.error = "Enter Index Number"
                false
            }
            binding.editTextRack.text.isNullOrEmpty() -> {
                binding.inputLayoutRack.error = "Enter Rack Number"
                false
            }
            else -> true
        }

    }

    private fun addBook(bookDetail: Book?) {
// Create a new user with a first, middle, and last name
        val bookName = binding.editTextBookName.text.toString()
        val author = binding.editTextAuthor.text.toString()
        val publisher = binding.editTextPublisher.text?.toString() ?: ""
        val description = binding.editTextDesc.text?.toString() ?: ""
        val pages = if (binding.editTextPages.text.isNullOrEmpty()) {
            0
        } else
            binding.editTextPages.text.toString().toInt()
        val price = if (binding.editTextPrice.text.isNullOrEmpty())
            0f
        else
            binding.editTextPrice.text.toString().toFloat()
        val book = hashMapOf(
            "Book_Name" to bookName,
            "Book_Category" to binding.editTextCategory.text.toString(),
            "Author" to author,
            "Book_Language" to binding.editTextLanguage.text.toString(),
            "Number_On_Book" to binding.editTextIndex.text.toString(),
            "Book_Rack" to binding.editTextRack.text.toString(),
            "Publisher" to publisher,
            "Pages" to pages,
            "Price" to price,
            "Description" to description,
            "library_name" to libraryName
        )
        authorAdapter?.add(author)
        bookNameAdapter?.add(bookName)
//        autoCompleterAuthor.add(author)
        if (bookDetail != null) {
            db.collection(Firebase.auth.currentUser?.uid!!).document(bookDetail.id).set(book)
                .addOnSuccessListener {
                    Snackbar.make(
                        requireView(),
                        getString(R.string.updated_succ),
                        Snackbar.LENGTH_SHORT
                    ).show()
                }
                .addOnFailureListener { e -> Log.w("TAG", "Error writing document", e) }
        } else {
            db.collection(Firebase.auth.currentUser?.uid!!).add(book)
                .addOnSuccessListener {
                    Snackbar.make(
                        requireView(),
                        getString(R.string.msg_book_added),
                        Snackbar.LENGTH_SHORT
                    ).show()
                    resetField()

                }
                .addOnFailureListener {
                    Snackbar.make(
                        requireView(),
                        getString(R.string.msg_try_again),
                        Snackbar.LENGTH_SHORT
                    ).show()

                }

        }

    }

    private fun resetField() {
        binding.editTextPublisher.setText("")
        binding.editTextDesc.setText("")
        binding.editTextPages.setText("")
        binding.editTextPrice.setText("")
        binding.editTextBookName.setText("")
        binding.editTextCategory.setText("")
        binding.editTextAuthor.setText("")
        binding.editTextLanguage.setText("")
        binding.editTextIndex.setText("")
        binding.editTextRack.setText("")
    }
}