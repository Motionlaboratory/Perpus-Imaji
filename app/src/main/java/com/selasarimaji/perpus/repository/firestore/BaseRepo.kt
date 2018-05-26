package com.selasarimaji.perpus.repository.firestore

import android.arch.lifecycle.MutableLiveData
import com.google.firebase.firestore.*
import com.selasarimaji.perpus.model.DataModel

abstract class BaseRepo <T:DataModel>{
    protected abstract val collectionName : String

    abstract val fetchedData : MutableLiveData<List<T>>

    private val db by lazy {
        FirebaseFirestore.getInstance().collection(collectionName)
    }

    fun getRemoteTotalCount(listener : (documentSnapshot: DocumentSnapshot) -> Unit){
        db.document(collectionName).get().addOnSuccessListener {
            listener(it)
        }
    }

    fun clearLocalData(){
        fetchedData.value = fetchedData.value?.toMutableList().apply { this?.clear() }
    }

    open fun load(startPosition: Int = -1, loadCount: Int = -1,
                  orderBy: String = "name",
                  filterMap: Map<String, String>? = null,
                  listener: (querySnapshot: QuerySnapshot) -> Unit){
        if (filterMap != null && filterMap.size > 1){

        } else {
            db.orderBy(orderBy).apply {
                if (loadCount > -1) startAt(startPosition)
                if (loadCount > -1) limit(loadCount.toLong())

                filterMap?.map {
                    this.whereGreaterThanOrEqualTo(it.key, it.value)
                }
            }.get().addOnSuccessListener {
                listener(it)
            }
        }
    }

    fun storeNewRemoteData(dataModel: T, loadingFlag: MutableLiveData<Boolean>,
                           successFlag: MutableLiveData<Boolean>, docRef: MutableLiveData<DocumentReference>){
        loadingFlag.value = true
        db.add(dataModel).addOnCompleteListener {
            docRef.value = it.result
            loadingFlag.value = false
            successFlag.value = it.isSuccessful
        }
    }

    fun updateRemoteData(dataModel: T){
        db.document(dataModel.id!!).set(dataModel, SetOptions.merge())
    }

    open fun addLocalItem(dataModel: T){
        if (editLocalItem(dataModel)) return // don't need to add if edit value

        val items = mutableListOf(dataModel)
        fetchedData.value?.run {
            items.addAll(0,this)
        }
        fetchedData.value = items
    }

    fun deleteLocalItem(dataModel: T){
        fetchedData.value?.toMutableList()?.run {
            val position = indexOfFirst { it.id == dataModel.id }
            if (position > -1){
                this.removeAt(position)
            }
            fetchedData.value = this
        }
    }

    fun editLocalItem(dataModel: T) : Boolean{
        fetchedData.value?.toMutableList()?.run {
            val position = indexOfFirst { it.id == dataModel.id }
            if (position > -1) {
                this[position] = dataModel
                fetchedData.value = this
                return true
            }
        }
        return false
    }

    fun getContentWith(field: String, query: String, listener : (querySnapshot:QuerySnapshot, query: String) -> Unit){
        load(filterMap = mapOf(field to query)){
            listener(it, query)
        }
    }
}