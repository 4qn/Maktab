package com.edu.maktab.home

import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.edu.maktab.MainActivity
import com.edu.maktab.R
import com.edu.maktab.databinding.FragmentHomeBinding
import com.edu.maktab.helper.AppConstant
import com.edu.maktab.helper.PreferenceHelper
import com.edu.maktab.helper.PreferenceHelper.get
import com.edu.maktab.helper.PreferenceHelper.set


/**
 * A simple [Fragment] subclass.
 * Use the [HomeFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class HomeFragment : Fragment(), View.OnClickListener {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private var pref: SharedPreferences? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        pref = PreferenceHelper.defaultPrefs(requireContext())
        val libraryName: String? = pref!![AppConstant.LIBRARY_NAME]
        if (libraryName == null)
            libraryName()
        else {
            (activity as MainActivity).toolBar?.title = libraryName
            binding.textTitle.text = libraryName
        }
        binding.btnAddBook.setOnClickListener(this)
        binding.btnMaktab.setOnClickListener(this)
        binding.btnSearch.setOnClickListener(this)
        binding.btnReport.setOnClickListener(this)
        return binding.root
    }

    override fun onClick(v: View?) {

        when (v!!.id) {
            R.id.btn_maktab -> {
                val direction = HomeFragmentDirections.actionBookListFragment()
                findNavController().navigate(direction)
            }
            R.id.btn_add_book -> {
                val direction = HomeFragmentDirections.actionAddBookFragment(null)
                findNavController().navigate(direction)
            }
            R.id.btn_search -> {
                val direction = HomeFragmentDirections.actionBookListFragment(1)
                findNavController().navigate(direction)
            }
            R.id.btn_report -> {
                val direction = HomeFragmentDirections.actionPdfFileListFragment()
                findNavController().navigate(direction)
            }
        }

    }

    private fun libraryName() {
        val alertDialog = AlertDialog.Builder(requireContext())
        alertDialog.setTitle("Library Name");
        val linearLayout = LinearLayout(requireContext())

        val editText = EditText(requireContext())
        val param = ViewGroup.MarginLayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        param.setMargins(20, 20, 20, 0)

        editText.hint = getString(R.string.library_name)

        editText.layoutParams = param
        editText.setPadding(20, 25, 20, 20)
        linearLayout.addView(editText)
        alertDialog.setCancelable(false)
        editText.setBackgroundResource(R.drawable.edit_txt_pressed)
        alertDialog.setView(linearLayout)
        alertDialog.setPositiveButton(
            "OK"
        ) { _, _ ->
            val libName = editText.text.toString()
            if (libName.isEmpty()) {
                alertDialog.show()
                Toast.makeText(
                    requireContext(),
                    getString(R.string.library_name),
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                pref!![AppConstant.LIBRARY_NAME] = libName
                (activity as MainActivity).toolBar?.title = libName
                binding.textTitle.text = libName
            }
        }
        alertDialog.show()
    }

}