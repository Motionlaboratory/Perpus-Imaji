package com.selasarimaji.perpus.view.fragment.content

import android.app.Activity
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.Intent
import android.support.design.widget.TextInputLayout
import android.text.InputType
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.AutoCompleteTextView
import android.widget.EditText
import android.widget.Toast
import com.selasarimaji.perpus.model.RepoDataModel
import com.selasarimaji.perpus.viewmodel.KidVM
import kotlinx.android.synthetic.main.layout_content_creation.*
import kotlinx.android.synthetic.main.content_kid.*
import java.util.*
import android.widget.ArrayAdapter
import com.esafirm.imagepicker.features.ImagePicker
import com.selasarimaji.perpus.*
import com.selasarimaji.perpus.model.LoadingType
import com.selasarimaji.perpus.model.RepoImage
import com.selasarimaji.perpus.model.getLoadingTypeText

class KidInspectFragment : BaseInspectFragment() {

    companion object {
        const val DoBKey = "KidCreationActivity-DoB"
    }

    override val viewModel by lazy {
        ViewModelProviders.of(activity!!).get(KidVM::class.java)
    }

    override fun setupView(){
        val view = layoutInflater.inflate(R.layout.content_kid, null)
        linearContainer.addView(view, 0)

        kidBirthDateInputLayout.editText?.run { showDatePickerOnClick(this) }
        val gender = arrayOf("Cowok", "Cewek")
        val adapter = ArrayAdapter<String>(context,
                android.R.layout.simple_dropdown_item_1line,
                gender)

        (kidGenderInputLayout.editText as AutoCompleteTextView).run {
            setAdapter(adapter)
            setOnFocusChangeListener { _, hasFocus ->
                if(hasFocus){
                    (kidGenderInputLayout.editText as AutoCompleteTextView).showDropDown()
                }
            }
        }

        kidImageButton.setOnClickListener {
            ImagePicker.create(this).startImagePicker()
        }
    }

    override fun setupToolbar(){
        viewModel.title.value = "Anak"
        viewModelInspect.getSelectedItemLiveData().observe(this, Observer {
            (it as RepoDataModel.Kid?)?.let {
                viewModel.title.value = it.name.toUpperCase()
            }
        })
    }

    override fun setupObserver(){
        viewModelInspect.getSelectedItemLiveData().observe(this, Observer {
            (it as RepoDataModel.Kid?)?.let {
                kidNameInputLayout.editText?.setText(it.name)
                kidAddressInputLayout.editText?.setText(it.address)
                kidBirthDateInputLayout.editText?.setText(it.birthDate)
                kidGenderInputLayout.editText?.setText(if (it.isMale) "Cowok" else "Cewek")

                viewModel.pickedImage.value = RepoImage(it.id, true)
            }
        })

        viewModelInspect.editOrCreateMode.observe(this, Observer {
            // it?.second -> can be null
            addButton.visibility = if (it?.second != true) View.GONE else View.VISIBLE
            kidImageButton.isEnabled = it?.second == true
        })

        viewModelInspect.editOrCreateMode.observe(this, Observer {
            arrayListOf<TextInputLayout>(kidNameInputLayout,
                    kidAddressInputLayout,
                    kidBirthDateInputLayout,
                    kidGenderInputLayout)
                    .apply {
                        if (it?.first != true) {
                            this.map {
                                it.editText?.inputType = InputType.TYPE_NULL
                            }
                        } else {
                            this.map {
                                it.editText?.inputType = InputType.TYPE_CLASS_TEXT
                            }
                        }
                        this[2].isEnabled = it?.first == true
                    }
        })
        viewModel.loadingProcess.observe(this, Observer {
            it?.run {
                // loading bar
                addButton.isEnabled = !isLoading
                kidImageButton.isEnabled = !isLoading

                // loading process
                when {
                    isSuccess && (viewModel.userHasLocalImage
                            || viewModel.isUserWantToUpdateRemoteImage(loadingType)) -> {
                        viewModel.storeImage(viewModel.repo,
                                viewModel.documentResultRef.value!!,
                                viewModel.loadingProcess)
                    }
                    isSuccess && viewModel.isUserWantToDeleteRemoteImage(loadingType) -> {
                        viewModel.deleteImage(viewModel.repo,
                                viewModel.documentResultRef.value!!,
                                viewModel.loadingProcess)
                    }
                    isSuccess -> {
                        Toast.makeText(context,
                                getLoadingTypeText(loadingType),
                                Toast.LENGTH_SHORT).show()
                        activity?.let {
                            it.setResult(Activity.RESULT_OK)
                            it.finish()
                        }
                    }
                    !isSuccess && !isLoading -> {
                        Toast.makeText(context,
                                "Gagal, Jaringan terganggu, silahkan coba lagi",
                                Toast.LENGTH_SHORT).show()
                    }
                    else -> {}
                }
            }
        })
        viewModel.pickedImage.observe(this, Observer {
            it?.run {
                context?.let {
                    GlideApp.with(it)
                            .load(if (!isRemoteSource) imagePath else viewModel.repo.getImageFull(imagePath))
                            .placeholder(R.drawable.ic_camera.resDrawable(it))
                            .into(kidImageButton)
                }
            }
        })
    }

    override fun submitValue() {
        val editTextList = arrayListOf<TextInputLayout>(kidNameInputLayout, kidAddressInputLayout,
                kidBirthDateInputLayout).apply {
            this.map {
                it.error = null
                it.isErrorEnabled = false
            }
        }

        val name = kidNameInputLayout.tryToRemoveFromList(editTextList)
        val address = kidAddressInputLayout.tryToRemoveFromList(editTextList)
        val gender = kidGenderInputLayout.editText?.text.toString() == "Cowok"
        val dateOfBirth = kidBirthDateInputLayout.tryToRemoveFromList(editTextList)

        editTextList.map {
            if (it.error.isNullOrEmpty()) it.error = "Silahkan diisi"
        }

        if(editTextList.isEmpty()) {
            viewModel.storeData(RepoDataModel.Kid(name, address, gender, dateOfBirth))
        }
    }

    private fun showDatePickerOnClick(editText: EditText){
        val savedString = context?.getStringVal(DoBKey, "") ?: ""
        var c = Calendar.getInstance()
        if (savedString.isNotEmpty()){
           c = parseDateString(savedString)
        }
        val year = c.get(Calendar.YEAR)
        val month = c.get(Calendar.MONTH)
        val day = c.get(Calendar.DAY_OF_MONTH)
        editText.setOnClickListener {
            DatePickerDialog(context,
                    DatePickerDialog.OnDateSetListener { _, year, month, day ->
                        editText.setText("$month/$day/$year")
                    },
                    year, month, day).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (ImagePicker.shouldHandle(requestCode, resultCode, data)) {
            ImagePicker.getFirstImageOrNull(data)?.let {
                viewModel.imagePickActivityResult(RepoImage(it.path, false))
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun focusFirstText() {
        kidNameInputLayout.requestFocus()
        (context?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?)?.
                toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY)
    }

    override fun clearFocus() {
        (context?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?)?.
                hideSoftInputFromWindow(linearContainer.windowToken, 0)
    }

    override fun tryDeleteCurrentItem() {
        AlertDialog.Builder(context).setTitle("Are you sure want to delete")
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
        viewModelInspect.getSelectedItemLiveData().value?.let {
            viewModel.deleteCurrent(it)
        }
    }
}
