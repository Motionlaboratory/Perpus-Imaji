package com.selasarimaji.perpus.viewmodel

import android.arch.lifecycle.MutableLiveData
import com.google.firebase.firestore.QuerySnapshot
import com.selasarimaji.perpus.model.DataModel
import com.selasarimaji.perpus.repository.firestore.BaseRepo
import com.selasarimaji.perpus.repository.firestore.BookRepo
import com.selasarimaji.perpus.repository.firestore.CategoryRepo

class EditBookVM : BaseContentVM<DataModel.Book>() {
    override val TAG: String
        get() = EditBookVM::class.java.name

    private val repoVal by lazy {
        BookRepo()
    }
    override val repo: BaseRepo<DataModel.Book>
        get() = repoVal


    // Auto complete
    private val repoCategoryVal by lazy {
        CategoryRepo()
    }
    private var categoryQuery : String = ""
    val filteredCategory = MutableLiveData<List<DataModel.Category>>()

    override fun loadInitial(){
        super.loadInitial()
        if (isInitialLoaded.value == null){
            lastIndex.value = 0
            isInitialLoaded.value = true
            repo.loadRange(0, 10, listener = this@EditBookVM::handleFirebaseQueryCallback)
        }
    }

    override fun loadMore() {
        isContentLoading.value?.run {
            if (!this){
                isContentLoading.value = true
                repo.loadRange(lastIndex.value!!, 10, listener = this@EditBookVM::handleFirebaseQueryCallback)
            }
        }
    }

    private fun handleFirebaseQueryCallback(querySnapshot: QuerySnapshot){
        querySnapshot.documents.map {
            repo.addLocalItem(DataModel.Book.turnDocumentToObject(it))
        }
        lastIndex.value = lastIndex.value!! + 10
        isContentLoading.value = false
    }

    fun getPossibleCategoryInputName(charSequence: CharSequence){
        if (charSequence.toString() != categoryQuery) { // blocking un needed response
            categoryQuery = charSequence.toString()
            repoCategoryVal.getContentWith("name", categoryQuery) { querySnapshot, query ->
                if (query == categoryQuery) { // blocking un needed response
                    val list = querySnapshot.documents.map {
                        DataModel.Category.turnDocumentToObject(it)
                    }
                    filteredCategory.value = list
                }
            }
        }
    }
}