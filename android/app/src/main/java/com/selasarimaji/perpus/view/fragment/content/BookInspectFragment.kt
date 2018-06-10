package com.selasarimaji.perpus.view.fragment.content

import android.app.Activity
import android.app.AlertDialog
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Context.INPUT_METHOD_SERVICE
import android.content.Intent
import android.support.design.widget.Snackbar
import android.support.design.widget.TextInputLayout
import android.text.InputType
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.Toast
import com.esafirm.imagepicker.features.ImagePicker
import com.hootsuite.nachos.NachoTextView
import com.hootsuite.nachos.terminator.ChipTerminatorHandler
import com.jakewharton.rxbinding2.widget.RxTextView
import com.selasarimaji.perpus.*
import com.selasarimaji.perpus.model.RepoDataModel
import com.selasarimaji.perpus.model.RepoImage
import com.selasarimaji.perpus.viewmodel.content.BookVM
import kotlinx.android.synthetic.main.content_book.*
import kotlinx.android.synthetic.main.fragment_recycler.*
import kotlinx.android.synthetic.main.layout_content_creation.*
import java.util.concurrent.TimeUnit

class BookInspectFragment : BaseInspectFragment<RepoDataModel.Book>() {

    override val viewModel by lazy {
        ViewModelProviders.of(activity!!).get(BookVM::class.java)
    }

    private val parentCategoryText : String
        get() = (categoryListChipInput.editText as  NachoTextView).tokenValues.let {
            if (it.size > 0) it.last()
            else ""
        }

    override fun setupView(){
        val view = layoutInflater.inflate(R.layout.content_book, null)
        linearContainer.addView(view, 0)
        (bookAuthorInputLayout.editText as NachoTextView).also {
            it.addChipTerminator(';', ChipTerminatorHandler.BEHAVIOR_CHIPIFY_ALL)
            it.enableEditChipOnTouch(false, false)
        }
        (categoryListChipInput.editText as NachoTextView).also {
            it.addChipTerminator(';', ChipTerminatorHandler.BEHAVIOR_CHIPIFY_ALL)
        }

        categoryListChipInput.editText?.let{
            RxTextView.textChanges(it)
                    .skip(1)
                    .debounce(300, TimeUnit.MILLISECONDS)
                    .subscribe {
                        viewModel.getPossibleCategoryInputName(parentCategoryText)
                    }
        }

        bookImageButton.setOnClickListener {
            ImagePicker.create(this).startImagePicker()
        }
    }

    override fun setupToolbar(){
        viewModel.title.value = "Buku"
        viewModelInspect.getSelectedItemLiveData().observe(this, Observer {
            (it as RepoDataModel.Book?)?.let {
                viewModel.title.value = it.name.toUpperCase()
            }
        })
    }

    override fun setupObserver(){
        viewModelInspect.getSelectedItemLiveData().observe(this, Observer {
            (it as RepoDataModel.Book?)?.let {
                bookNameInputLayout.editText?.setText(it.name)
                bookAuthorInputLayout.editText?.setText(it.authors.joinToString(";", postfix = ";"))
                yearInputLayout.editText?.setText(it.year.toString())
                publisherInputLayout.editText?.setText(it.publisher)
                categoryListChipInput.editText?.setText(it.idCategoryList.joinToString(";", postfix = ";"))

                if (it.hasImage) {
                    viewModel.documentResultRef = it.id
                    viewModel.pickedImage.value = RepoImage(it.id, true)
                }
            }
        })

        viewModelInspect.editOrCreateMode.observe(this, Observer {
            addButton.visibility = if (it?.second != true) View.GONE else View.VISIBLE
            bookImageButton.isEnabled = it?.first == true
        })

        viewModelInspect.editOrCreateMode.observe(this, Observer {
            arrayListOf<TextInputLayout>(bookNameInputLayout,
                    bookAuthorInputLayout,
                    yearInputLayout,
                    publisherInputLayout,
                    categoryListChipInput)
                    .apply {
                        if (it?.first != true) {
                            this.map {
                                it.editText?.inputType = InputType.TYPE_NULL
                            }
                        } else {
                            this.map {
                                it.editText?.inputType = InputType.TYPE_CLASS_TEXT
                            }
                            this[2].editText?.inputType = InputType.TYPE_CLASS_NUMBER // year input
                        }
                    }
        })
        viewModel.isLoading.observe(this, Observer {
            addButton.isEnabled = !(it ?: false)
            bookImageButton.isEnabled = !(it ?: false)
        })
        viewModel.shouldFinish.observe(this, Observer {
            if (it == true){
                activity?.let {
                    it.setResult(Activity.RESULT_OK)
                    it.finish()
                }
            }
        })
        viewModel.repoCategoryVal.fetchedData.observe(this, Observer {
            it?.run {
                val adapter = ArrayAdapter<String>(context,
                        android.R.layout.simple_dropdown_item_1line,
                        this.filter { it.name.contains(parentCategoryText) }
                                .map { it.name.capitalizeWords() })
                (categoryListChipInput.editText as NachoTextView).run {
                    setAdapter(adapter)
                    showDropDown()
                }
            }
        })
        viewModel.pickedImage.observe(this, Observer {
            it?.run {
                context?.let {
                    GlideApp.with(it)
                            .load(if (!isRemoteSource) imagePath else viewModel.repo.getImageThumb(imagePath))
                            .placeholder(R.drawable.ic_camera.resDrawable(it))
                            .into(bookImageButton)
                }
            }
        })
    }

    override fun createValue(): RepoDataModel.Book? {
        val editTextList = arrayListOf<TextInputLayout>(bookNameInputLayout,
                bookAuthorInputLayout, yearInputLayout,
                publisherInputLayout, categoryListChipInput).apply {
            this.map {
                it.error = null
                it.isErrorEnabled = false
            }
        }

        val name = bookNameInputLayout.tryToRemoveFromList(editTextList)
        val authors = (bookAuthorInputLayout.editText as NachoTextView).chipValues.map {
            it.toLowerCase()
        }.also {
            if (it.isNotEmpty()) {
                editTextList.remove(bookAuthorInputLayout)
            }
        }
        val year = yearInputLayout.tryToRemoveFromList(editTextList)
        val publisher = publisherInputLayout.tryToRemoveFromList(editTextList)
        val category = (categoryListChipInput.editText as NachoTextView).chipValues.map {
            val value = it
            viewModel.repoCategoryVal.fetchedData.value?.
                    find { it.name == value.toLowerCase() }?.id
                    ?: ""
        }.also {
            if (it.isNotEmpty() && !it.contains("")) {
                editTextList.remove(categoryListChipInput)
            } else {
                categoryListChipInput.error = "Pastikan kategori telah terdaftar"
            }
        }
        val hasImage = viewModel.pickedImage.value?.isRemoteSource == false

        editTextList.map {
            if (it.error.isNullOrEmpty()) it.error = "Silahkan diisi"
        }

        return if(editTextList.isEmpty())
            RepoDataModel.Book(name, authors, year.toInt(), publisher, category, hasImage)
        else
            null
    }

    override fun submitValue() {
        createValue()?.let {
            viewModel.storeData(it){
                if (it.isSuccess && viewModel.userHasLocalImage && it.data != null){
                    viewModel.storeImage(viewModel.repo, it.data, viewModel.isLoading){
                        if (it.isSuccess) {
                            showLoadingResultToast(it.loadingType)
                            viewModel.shouldFinish.value = true
                        }
                    }
                } else if(it.isSuccess) {
                    showLoadingResultToast(it.loadingType)
                    viewModel.shouldFinish.value = true
                } else {
                    showErrorConnectionToast()
                }
            }
        }
    }

    override fun focusFirstText() {
        bookNameInputLayout.requestFocus()
        (context?.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager?)?.
                toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY)
    }

    override fun clearFocus() {
        (context?.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager?)?.
                hideSoftInputFromWindow(linearContainer.windowToken, 0)

        viewModelInspect.setSelectedItem(viewModelInspect.getSelectedItemLiveData().value)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (ImagePicker.shouldHandle(requestCode, resultCode, data)) {
            ImagePicker.getFirstImageOrNull(data)?.let {
                viewModel.imagePickActivityResult(RepoImage(it.path, false))
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun tryDeleteCurrentItem() {
        AlertDialog.Builder(context).setTitle("Are you sure want to delete?")
                .setPositiveButton("Yes"){ dialog, _ ->
                    super.tryDeleteCurrentItem()
                    dialog.dismiss()
                }
                .setNegativeButton("No"){ dialog, _ ->
                    dialog.dismiss()
                }
                .show()
    }

    override fun deleteCurrentItem() {
        viewModelInspect.getSelectedItemLiveData().value?.run{
            viewModel.canSafelyDeleted(id){
                when (it.data) {
                    true -> {
                        viewModel.deleteCurrent(this){
                            if (it.isSuccess && viewModel.userHasRemoteImage){
                                viewModel.deleteImage(viewModel.repo,
                                        viewModel.documentResultRef!!,
                                        viewModel.isLoading){
                                    if (it.isSuccess) {
                                        showLoadingResultToast(it.loadingType)
                                        viewModel.shouldFinish.value = true
                                    }
                                }
                            } else if (it.isSuccess) {
                                showLoadingResultToast(it.loadingType)
                                viewModel.shouldFinish.value = true
                            } else{
                                showErrorConnectionToast()
                            }
                        }
                        viewModelInspect.editOrCreateMode.value = Pair(false, false)
                    }
                    null -> Toast.makeText(context,
                            "Gagal, Jaringan terganggu, silahkan coba lagi",
                            Toast.LENGTH_SHORT).show()
                    else -> Snackbar.make(linearContainer,
                            "Item ini masih digunakan oleh item lain, edit atau hapus item tersebut terlebih dahulu",
                            Snackbar.LENGTH_INDEFINITE).run {
                        setAction("OK"){
                            dismiss()
                        }
                    }.show()
                }
            }
        }
    }

    override fun tryUpdateCurrentItem() {
        AlertDialog.Builder(context).setTitle("Are you sure want to update?")
                .setPositiveButton("Yes"){ dialog, _ ->
                    super.tryUpdateCurrentItem()
                    dialog.dismiss()
                }
                .setNegativeButton("No"){ dialog, _ ->
                    dialog.dismiss()
                }
                .show()
    }

    override fun updateCurrentItem() {
        createValue()?.let {
            viewModel.updateData(it.apply {
                id = viewModelInspect.getSelectedItemLiveData().value!!.id
            }){
                if (it.isSuccess && viewModel.userHasLocalImage){
                    viewModel.storeImage(viewModel.repo,
                            viewModel.documentResultRef!!,
                            viewModel.isLoading){
                        if (it.isSuccess) {
                            showLoadingResultToast(it.loadingType)
                            viewModel.shouldFinish.value = true
                        }
                    }
                } else if(it.isSuccess) {
                    showLoadingResultToast(it.loadingType)
                    viewModel.shouldFinish.value = true
                } else {
                    showErrorConnectionToast()
                }
            }
            viewModelInspect.editOrCreateMode.value = Pair(false, false)
        }
    }
}
