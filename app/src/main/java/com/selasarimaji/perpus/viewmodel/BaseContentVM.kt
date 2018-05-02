package com.selasarimaji.perpus.viewmodel

import android.arch.lifecycle.MutableLiveData
import com.selasarimaji.perpus.model.DataModel
import com.selasarimaji.perpus.repository.firestore.BaseRepo

abstract class BaseContentVM <T: DataModel> : BaseLoadingVM() {

    abstract val TAG : String

    open val repo: BaseRepo<T>? = null

    var title = MutableLiveData<String>()
    protected var isInitialLoaded = MutableLiveData<Boolean>()

    open fun storeData(category: T){
        repo?.storeNewRemoteData(category, uploadingFlag, uploadingSuccessFlag)
    }

    abstract fun loadInitial()
}