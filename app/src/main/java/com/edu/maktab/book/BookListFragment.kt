package com.edu.maktab.book


import android.Manifest
import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.print.PrintAttributes
import android.provider.Settings
import android.text.Layout
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ImageSpan
import android.util.Log
import android.view.*
import android.widget.CompoundButton
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.edu.maktab.R
import com.edu.maktab.databinding.FragmentBookListBinding
import com.edu.maktab.utils.FileUtils
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.itextpdf.io.font.PdfEncodings
import com.itextpdf.kernel.colors.DeviceRgb
import com.itextpdf.kernel.font.PdfFontFactory
import com.itextpdf.kernel.geom.PageSize
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.kernel.pdf.canvas.draw.DottedLine
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.*
import com.itextpdf.layout.property.BaseDirection
import com.itextpdf.layout.property.TextAlignment
import com.wwdablu.soumya.simplypdf.DocumentInfo
import com.wwdablu.soumya.simplypdf.SimplyPdf
import com.wwdablu.soumya.simplypdf.SimplyPdfDocument
import com.wwdablu.soumya.simplypdf.composers.TableComposer
import com.wwdablu.soumya.simplypdf.composers.models.TableProperties
import com.wwdablu.soumya.simplypdf.composers.models.TextProperties
import com.wwdablu.soumya.simplypdf.composers.models.cell.TextCell
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.util.*
import java.util.regex.Pattern


/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 */
class BookListFragment : Fragment(), SearchView.OnQueryTextListener,
    CompoundButton.OnCheckedChangeListener {
    private var _binding: FragmentBookListBinding? = null
    private val binding get() = _binding!!
    private val db = Firebase.firestore
    private var searchChannel = BroadcastChannel<String>(1)
    private lateinit var adapter: BookListAdapter
    private var bookList: List<DocumentSnapshot>? = null
    private var filterList: List<DocumentSnapshot>? = null

    private var searchView: SearchView? = null
    private val viewModel: BookListViewModel by viewModels()
    val args: BookListFragmentArgs by navArgs()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentBookListBinding.inflate(inflater, container, false)

        val ss = SpannableString(getString(R.string.msg_add_book))
        val d = ContextCompat.getDrawable(requireContext(), R.drawable.ic_add_book)
        d?.setTint(Color.RED)

        d!!.setBounds(0, 0, d.intrinsicWidth, d.intrinsicHeight)
        val span = ImageSpan(d, ImageSpan.ALIGN_CENTER)
        ss.setSpan(span, 11, 12, Spannable.SPAN_INCLUSIVE_EXCLUSIVE)
        binding.textEmptyMsg.text = ss
        //after adding new home desing ,no need of addbook button from here
        binding.fabAddBook.hide()
        binding.fabAddBook.setOnClickListener {
            val direction = BookListFragmentDirections.actionAddBookFragment()
            findNavController().navigate(direction)
        }
        binding.recyclerViewBookList.layoutManager = LinearLayoutManager(requireContext())
        adapter = BookListAdapter()
        binding.recyclerViewBookList.adapter = adapter
        binding.recyclerViewBookList.addItemDecoration(
            DividerItemDecoration(
                requireContext(),
                RecyclerView.VERTICAL
            )
        )
        db.collection(Firebase.auth.currentUser?.uid!!)
            .get()
            .addOnSuccessListener { result ->

                bookList = result.documents
                adapter.submitList(result.documents)
                if (result.documents.size > 0) {
                    binding.textEmptyMsg.visibility = View.GONE
                }
//                for (document in result) {
//                    Log.d("TAG", " documents${document.id} => ${document.data}")
//                }
            }
            .addOnFailureListener { exception ->
                Log.w("c", "Error getting documents.", exception)
            }
        configureSearch()

        binding.byName.setOnCheckedChangeListener(this)
        binding.byAuhtor.setOnCheckedChangeListener(this)
        binding.byCategory.setOnCheckedChangeListener(this)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                try {
                    val permissionIntent =
                        Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                    permissionIntent.addCategory("android.intent.category.DEFAULT")
                    permissionIntent.data =
                        Uri.parse(String.format("package:%s", requireActivity().packageName))
                    startActivity(permissionIntent)
                } catch (e: Exception) {
                    val intent = Intent().apply {
                        action = Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION
                    }
                    startActivity(intent)
                }

            }
        }
        actionRequestPermission.launch(
            arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        )


    }

    override fun onCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean) {
        if (searchView!!.query.isNotEmpty()) {
            lifecycleScope.launch {
                searchText(searchView?.query?.toString()?.trim()!!)
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu_landing, menu)
        val searchManager =
            requireContext().getSystemService(Context.SEARCH_SERVICE) as SearchManager
        val item = menu.findItem(R.id.app_bar_search)
        item.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
            override fun onMenuItemActionExpand(item: MenuItem?): Boolean {

                binding.linearLayoutFilter.visibility = View.VISIBLE
                return true
            }

            override fun onMenuItemActionCollapse(item: MenuItem?): Boolean {
                binding.linearLayoutFilter.visibility = View.GONE
                return true
            }
        })
        searchView = item.actionView as SearchView
        searchView?.apply {
            setSearchableInfo(searchManager.getSearchableInfo(activity?.componentName))

        }
        if (args.type == 1) {
            searchView?.setOnQueryTextListener(this)
            searchView?.setIconifiedByDefault(true)
            searchView?.isFocusable = true
            searchView?.isIconified = false
//        searchView?.clearFocus()
            searchView?.requestFocusFromTouch()
            binding.linearLayoutFilter.visibility = View.VISIBLE
        } else {
            item.isVisible = false
        }

    }


    private fun configureSearch() {
        lifecycleScope.launch {
            searchChannel.consumeEach {
                delay(300)
                val list = search(it)
                if (list.isNullOrEmpty().not()) {
                    binding.recyclerViewBookList.visibility = View.VISIBLE
                    adapter.submitList(list)
                } else {
                    binding.recyclerViewBookList.visibility = View.GONE
                }
            }
        }
    }


    private fun search(query: String): List<DocumentSnapshot>? {
        if (query.isEmpty()) {
            return bookList
        }
        val regex = ".*" + query.trim().lowercase(Locale.ROOT) + ".*"
        val pattern = Pattern.compile(regex)

        filterList = bookList?.filter {
            val bookName = it.data?.get("Book_Name")!!.toString()
            val author = it.data!!["Author"].toString()
            val topic = it.data!!["Book_Category"].toString()
            val language = it.data!!["Book_Language"].toString()
            val bookPlace = it.data!!["Book_Rack"].toString()
            val bookNo = it.data!!["Number_On_Book"].toString()
            when {
                binding.byName.isChecked && binding.byAuhtor.isChecked && binding.byCategory.isChecked -> {
                    pattern.matcher(bookName.lowercase(Locale.ROOT)).matches() ||
                            pattern.matcher(topic.lowercase(Locale.ROOT)).matches() ||
                            pattern.matcher(author.lowercase(Locale.ROOT)).matches()
                }
                binding.byName.isChecked && binding.byAuhtor.isChecked -> {
                    pattern.matcher(bookName.lowercase(Locale.ROOT)).matches() ||
                            pattern.matcher(author.lowercase(Locale.ROOT)).matches()
                }
                binding.byAuhtor.isChecked && binding.byCategory.isChecked -> {
                    pattern.matcher(topic.lowercase(Locale.ROOT)).matches() ||
                            pattern.matcher(author.lowercase(Locale.ROOT)).matches()
                }
                binding.byName.isChecked -> {
                    pattern.matcher(bookName.lowercase(Locale.ROOT)).matches()
                }
                binding.byAuhtor.isChecked -> {
                    pattern.matcher(author.lowercase(Locale.ROOT)).matches()
                }
                binding.byCategory.isChecked -> {
                    pattern.matcher(topic.lowercase(Locale.ROOT)).matches()
                }
                else -> {
                    pattern.matcher(bookName.lowercase(Locale.ROOT)).matches() ||
                            pattern.matcher(topic.lowercase(Locale.ROOT)).matches() ||
                            pattern.matcher(language.lowercase(Locale.ROOT)).matches() ||
                            pattern.matcher(bookPlace.lowercase(Locale.ROOT)).matches() ||
                            pattern.matcher(bookNo.lowercase(Locale.ROOT)).matches() ||
                            pattern.matcher(author.lowercase(Locale.ROOT)).matches()
                }
            }


        }
        return filterList
    }

    private suspend fun searchText(query: String) {
        searchChannel.send(query)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.app_bar_search -> true
            R.id.log_out -> {
                Firebase.auth.signOut()
                val direction = BookListFragmentDirections.actionLoginFragment()
                findNavController().navigate(direction)

                true
            }
            R.id.export -> {

                exportFileName()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onQueryTextSubmit(query: String?): Boolean {
        return true
    }

    private fun exportPdf(name: String) {
        val simplyPdfDocument: SimplyPdfDocument = SimplyPdf.with(requireContext(), File(name))
            .colorMode(DocumentInfo.ColorMode.COLOR)
            .paperSize(PrintAttributes.MediaSize.ISO_A4)
            .margin(DocumentInfo.Margins.DEFAULT)
            .paperOrientation(DocumentInfo.Orientation.PORTRAIT)
            .build()

        //Create text entries of variable font size
        val properties = TableProperties().apply {
            borderColor = "#000000"
            borderWidth = 1
        }
        val textProperties = TextProperties().apply {
            textColor = "#000000"
            textSize = 16
            alignment = Layout.Alignment.ALIGN_NORMAL
        }

        val rows = LinkedList<LinkedList<com.wwdablu.soumya.simplypdf.composers.models.cell.Cell>>()

        val halfWidth = simplyPdfDocument.usablePageWidth / 4
        LinkedList<com.wwdablu.soumya.simplypdf.composers.models.cell.Cell>().apply {
            textProperties.alignment = Layout.Alignment.ALIGN_NORMAL
            add(TextCell(getString(R.string.rack), textProperties, halfWidth / 2))
            add(TextCell(getString(R.string.book_topic), textProperties, halfWidth))
            add(TextCell(getString(R.string.author), textProperties, halfWidth))
            add(TextCell(getString(R.string.book_name), textProperties, halfWidth))
            add(TextCell("فہرست", textProperties, halfWidth / 2))
            rows.add(this)
        }
        filterList = filterList ?: bookList
        val sortList = filterList?.sortedBy { it.get("Book_Category").toString() }
        sortList?.forEachIndexed { index, it ->
            val bookName = it.get("Book_Name")!!.toString()
            val author = it["Author"].toString()
            val cat = it["Book_Category"].toString()
            val rack = it["Book_Rack"].toString()
            LinkedList<com.wwdablu.soumya.simplypdf.composers.models.cell.Cell>().apply {
                textProperties.alignment = Layout.Alignment.ALIGN_NORMAL
                add(TextCell(rack, textProperties, halfWidth / 2))
                add(TextCell(cat, textProperties, halfWidth))
                add(TextCell(author, textProperties, halfWidth))
                add(TextCell(bookName, textProperties, halfWidth))
                add(TextCell((index + 1).toString(), textProperties, halfWidth / 2))
                rows.add(this)
            }
        }
        val tableComposer = TableComposer(simplyPdfDocument)
        tableComposer.setProperties(properties)
        tableComposer.draw(rows as List<MutableList<com.wwdablu.soumya.simplypdf.composers.models.cell.Cell>>)

        /*   //Create text entries of variable font size
           val properties = TextProperties().apply {
               textColor = "#000000"
           }
           val textProperties = TextProperties()
           textProperties.textSize = 24
           textProperties.alignment = Layout.Alignment.ALIGN_CENTER
           textProperties.typeface = ResourcesCompat.getFont(requireContext(),R.font.urdu)

           val textComposer1 = TextComposer(simplyPdfDocument)
           for (i in 1..10) {

               textComposer1.write(getString(R.string.search), textProperties)
           }

           //Insert a new page
           simplyPdfDocument.newPage()

           //Text with red color
           properties.textSize = 16
           properties.textColor = "#FF0000"

           val textComposer = TextComposer(simplyPdfDocument)
           textComposer.write(getString(R.string.search), properties)

           //Text with custom color
           properties.textColor = "#ABCDEF"
          textComposer.write("Text with color set as #ABCDEF", properties)

           //Text with bullet
           properties.textColor = "#000000"
           properties.bulletSymbol = "•"
           properties.isBullet = true
           textComposer.write("Text with bullet mark at the start", properties)
           textComposer.write("Text with bullet mark at the start 2nd line", properties)

           properties.isBullet = false
           textComposer.write("Normal text after bullets", properties)
           textComposer.write("Normal text 2nd line", properties)

           //Text with alignments
           properties.alignment = Layout.Alignment.ALIGN_NORMAL
           properties.isBullet = false
           textComposer.write("Normal text alignment", properties)

           properties.alignment = Layout.Alignment.ALIGN_CENTER
           textComposer.write("Center text alignment", properties)

           properties.alignment = Layout.Alignment.ALIGN_OPPOSITE
           textComposer.write("Opposite text alignment", properties)

           //Bold typeface
           properties.alignment = Layout.Alignment.ALIGN_NORMAL
           properties.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
           textComposer.write("Bold text", properties)

           //Underlined text
           properties.underline = true
           properties.typeface = null
           textComposer.write("Underlined text", properties)

           //Test to write text with a fixed width in the page
           simplyPdfDocument.insertEmptySpace(25)
           properties.apply {
               underline = false
               textSize = 32
               alignment = Layout.Alignment.ALIGN_CENTER
           }
           textComposer.write("The quick brown fox jumps over the hungry lazy dog. " +
                   "This text is written keeping the page width as half.",
               properties)

           textComposer.write("Complete", properties.apply { textSize = 12 })*/
        simplyPdfDocument.finish()
        /*viewLifecycleOwner.lifecycleScope.launch {

            if (viewModel.canAddDocument) {
                viewModel.addRandomFile()
            }
        }*/
        /*  if (File(name).exists()) {
              File(name).delete()
          }*/
//
//        viewLifecycleOwner.lifecycleScope.launch {
////            val os = viewModel.filePathName(name)
//            createPdf(FileOutputStream(name))
//        }


//        actionRequestPermission.launch(
//            arrayOf(
//                Manifest.permission.READ_EXTERNAL_STORAGE,
//                Manifest.permission.WRITE_EXTERNAL_STORAGE
//            )
//        )

    }

    private fun exportFileName() {
        val alertDialog = AlertDialog.Builder(requireContext())
        alertDialog.setTitle("PDF File")
        val linearLayout = LinearLayout(requireContext())

        val editText = EditText(requireContext())
        val param = ViewGroup.MarginLayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        param.setMargins(20, 20, 20, 0)

        editText.hint = "Enter pdf file Name"

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
                showToast("Enter pdf file Name")

            } else {
                val dest = FileUtils.getAppPath(requireContext()) + libName + ".pdf"
                if (File(dest).exists()) {
                    showToast(getString(R.string.file_exist))
                } else {
                    exportPdf(dest)
                }

            }
        }
        alertDialog.show()
    }

    private fun showToast(message: String) {
        Toast.makeText(
            requireContext(),
            message,
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun createPdf(outputstream: FileOutputStream) {
        val pdfWriter = PdfWriter(outputstream)
        val pdfDocument = PdfDocument(pdfWriter)
        /*   val info: PdfDocumentInfo = pdfDocument.documentInfo
           info.title = "Example of iText7 by 4qn"
           info.author = "XYZ autor"
           info.subject = "iText7 PDF Demo"
           info.keywords = "iText, PDF, Pratik Butani"
           info.creator = "A simple tutorial example"*/

        val document = Document(pdfDocument, PageSize.A4, true)

        /***
         * Variables for further use....
         */
        /***
         * Variables for further use....
         */
        val mColorAccent = DeviceRgb(153, 204, 255)
        val mColorBlack = DeviceRgb(0, 0, 0)
        val mHeadingFontSize = 20.0f
        val mValueFontSize = 26.0f

        /**
         * How to USE FONT....
         */
        /**
         * How to USE FONT....
         */

        val font = PdfFontFactory.createFont(
            "assets/fonts/NotoNaskhArabic-Regular.ttf",
            PdfEncodings.IDENTITY_H
        )

        // LINE SEPARATOR

        // LINE SEPARATOR
        val lineSeparator = LineSeparator(DottedLine())
        lineSeparator.strokeColor = DeviceRgb(0, 0, 68)

        // Title Order Details...
        // Adding Title....

        // Title Order Details...
        // Adding Title....
        val mOrderDetailsTitleChunk: Text =
            Text("Order Details")/*.setFont(font)*/.setFontSize(36.0f).setFontColor(mColorBlack)
        val mOrderDetailsTitleParagraph: Paragraph = Paragraph(mOrderDetailsTitleChunk)
            .setTextAlignment(TextAlignment.CENTER)
        document.add(mOrderDetailsTitleParagraph)

        // Fields of Order Details...
        // Adding Chunks for Title and value

        // Fields of Order Details...
        // Adding Chunks for Title and value
        val mOrderIdChunk: Text =
            Text("Order No:")/*.setFont(font)*/.setFontSize(mHeadingFontSize)
                .setFontColor(mColorAccent)
        val mOrderIdParagraph = Paragraph(mOrderIdChunk)
        document.add(mOrderIdParagraph)

        val mOrderIdValueChunk: Text =
            Text("#123123")/*.setFont(font)*/.setFontSize(mValueFontSize)
                .setFontColor(mColorBlack)
        val mOrderIdValueParagraph = Paragraph(mOrderIdValueChunk)
        document.add(mOrderIdValueParagraph)

        // Adding Line Breakable Space....

        // Adding Line Breakable Space....
        document.add(Paragraph(""))
        // Adding Horizontal Line...
        // Adding Horizontal Line...
        document.add(lineSeparator)
        // Adding Line Breakable Space....
        // Adding Line Breakable Space....
        document.add(Paragraph(""))

        // Fields of Order Details...

        // Fields of Order Details...
        val mOrderDateChunk: Text =
            Text("Order Date:")/*.setFont(font)*/.setFontSize(mHeadingFontSize)
                .setFontColor(mColorAccent)
        val mOrderDateParagraph = Paragraph(mOrderDateChunk)
        document.add(mOrderDateParagraph)

        val mOrderDateValueChunk: Text =
            Text("06/07/2017")/*.setFont(font)*/.setFontSize(mValueFontSize)
                .setFontColor(mColorBlack)
        val mOrderDateValueParagraph = Paragraph(mOrderDateValueChunk)
        document.add(mOrderDateValueParagraph)

        document.add(Paragraph(""))
        document.add(lineSeparator)
        document.add(Paragraph(""))

        // Fields of Order Details...

        // Fields of Order Details...
        val mOrderAcNameChunk: Text =
            Text("Account Name:")/*.setFont(font)*/.setFontSize(mHeadingFontSize)
                .setFontColor(mColorAccent)
        val mOrderAcNameParagraph = Paragraph(mOrderAcNameChunk)
        document.add(mOrderAcNameParagraph)

        val mOrderAcNameValueChunk: Text =
            Text("Pratik Butani")/*.setFont(font)*/.setFontSize(mValueFontSize)
                .setFontColor(mColorBlack)
        val mOrderAcNameValueParagraph = Paragraph(mOrderAcNameValueChunk)
        document.add(mOrderAcNameValueParagraph)

        document.add(Paragraph(""))
        document.add(lineSeparator)
        document.add(Paragraph(""))
        val table = Table(4).useAllAvailableWidth()
        table.addCell(Cell().add(Paragraph("Index")))
        val cell = Cell().add(Paragraph(Text(getString(R.string.author)).setFont(font)))

        cell.setBaseDirection(BaseDirection.RIGHT_TO_LEFT)
            .setTextAlignment(TextAlignment.RIGHT)
        table.addCell(cell)
//        .setFontSize(mHeadingFontSize).setFontColor(mColorAccent)
        table.addCell(Cell().add(Paragraph(getString(R.string.author))))
        table.addCell(Cell().add(Paragraph(getString(R.string.book_topic))))
        table.addCell(Cell().add(Paragraph(getString(R.string.language))))
        document.add(table)

        document.close()
        pdfWriter.close()
        pdfDocument.close()

        Toast.makeText(requireContext(), "Created... :)", Toast.LENGTH_SHORT).show()
    }

    private val actionRequestPermission =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
//            handlePermissionSectionVisibility()
        }

    override fun onQueryTextChange(newText: String?): Boolean {
        lifecycleScope.launch {
            searchText(newText?.trim()!!)
        }
        return true
    }
}