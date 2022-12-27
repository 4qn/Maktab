package com.edu.maktab.book

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.edu.maktab.R
import com.edu.maktab.databinding.FragmentBookDetailBinding
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [BookDetailFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class BookDetailFragment : Fragment() {

    private var _binding: FragmentBookDetailBinding? = null
    private val binding get() = _binding!!
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentBookDetailBinding.inflate(inflater, container, false)
        val args = BookDetailFragmentArgs.fromBundle(requireArguments())
        val book = args.book!!
        binding.textBookNo.text = book.Number_On_Book
        if (book.Price > 0) {
            binding.price.text = "${getString(R.string.price)} ${book.Price}"
        } else {
            binding.price.visibility = View.GONE
        }
        if (book.Pages > 0) {
            binding.pages.text = "${book.Pages} ${getString(R.string.pages)}"
        } else {
            binding.pages.visibility = View.GONE
        }
        binding.textTitle.text = book.Book_Name
        binding.rack.text = book.Book_Rack
        binding.category.text = book.Book_Category
        binding.chipAuthor.text = book.Author
        binding.publisher.text = book.Publisher
        binding.description.text = book.Description
        binding.language.text = book.Book_Language
        binding.btnDelete.setOnClickListener {
            val db = Firebase.firestore
            db.collection(Firebase.auth.currentUser?.uid!!).document(book.id)
                .delete()
                .addOnSuccessListener { Log.d("TAG", "DocumentSnapshot successfully deleted!") }
                .addOnFailureListener { e -> Log.w("TAG", "Error deleting document", e) }

        }
        binding.btnEdit.setOnClickListener {
            val directions =
                BookDetailFragmentDirections.actionBookDetailFragmentToAddBookFragment(book)
            findNavController().navigate(directions)
        }
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}