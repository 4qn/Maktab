package com.edu.maktab.pdf

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.edu.maktab.MainActivity
import com.edu.maktab.R
import com.edu.maktab.databinding.FragmentPdfFileListBinding
import com.edu.maktab.utils.FileUtils
import java.io.File

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * create an instance of this fragment.
 */
class PdfFileListFragment : Fragment() {

    private var _binding: FragmentPdfFileListBinding? = null
    private val binding get() = _binding!!
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentPdfFileListBinding.inflate(inflater, container, false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            getFileName()
        } else
            actionRequestPermission.launch(
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            )

        (activity as MainActivity).toolBar?.title = getString(R.string.export)
        return binding.root
    }

    var granted = true
    private val actionRequestPermission =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            it.entries.forEach { u ->
                Log.d("TAG", ": persmission = $u")
                if (!u.value) {
                    granted = u.value
                }
            }
            // check for below 10
            if (granted) {
                getFileName()
            } else {
                Log.d("TAG", "Name is permission :${granted} ")
            }
        }

    private fun getFileName() {
        val path = FileUtils.getAppPath(requireContext())
        val directory = File(path)
        binding.recyclerViewFileName.layoutManager = LinearLayoutManager(requireContext())
        val adapter = PdfNameListAdapter()
        binding.recyclerViewFileName.adapter = adapter
        adapter.submitList(directory.listFiles().toList())
        binding.recyclerViewFileName.addItemDecoration(
            DividerItemDecoration(
                requireContext(),
                RecyclerView.VERTICAL
            )
        )
        directory.listFiles().forEach {
            Log.d("TAG", "Name is :${it.name} ")
        }
    }

}