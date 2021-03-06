package com.selasarimaji.perpus.view.fragment.content

import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.selasarimaji.perpus.R
import com.selasarimaji.perpus.model.Loading
import com.selasarimaji.perpus.model.RepoDataModel
import com.selasarimaji.perpus.viewmodel.BaseLoadingVM
import com.selasarimaji.perpus.viewmodel.InspectVM
import kotlinx.android.synthetic.main.layout_content_creation.*

abstract class BaseInspectFragment<T: RepoDataModel> : Fragment() {
    abstract val viewModel : BaseLoadingVM

    protected val viewModelInspect by lazy {
        ViewModelProviders.of(activity!!).get(InspectVM::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.layout_content_creation, null)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupView()
        setupToolbar()
        setupObserver()

        addButton.setOnClickListener {
            submitValue()
        }
    }

    protected abstract fun setupToolbar()
    protected abstract fun createValue(): T?
    protected abstract fun submitValue()
    protected abstract fun setupView()
    protected abstract fun setupObserver()

    abstract fun focusFirstText()
    abstract fun clearFocus()

    open fun tryDeleteCurrentItem(){
        deleteCurrentItem()
    }
    abstract fun deleteCurrentItem()

    open fun tryUpdateCurrentItem(){
        updateCurrentItem()
    }
    abstract fun updateCurrentItem()

    fun showErrorConnectionToast(){
        clearFocus()
        Toast.makeText(context,
                "Gagal, Jaringan terganggu, silahkan coba lagi",
                Toast.LENGTH_SHORT).show()
    }

    fun showLoadingResultToast(loadingType: Loading.Type){
        clearFocus()
        Toast.makeText(context,
                Loading.getLoadingTypeText(loadingType),
                Toast.LENGTH_SHORT).show()
    }
}
