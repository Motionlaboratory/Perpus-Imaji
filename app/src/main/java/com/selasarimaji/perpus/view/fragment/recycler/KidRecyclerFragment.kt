package com.selasarimaji.perpus.view.fragment.recycler

import android.app.Activity
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.view.*
import kotlinx.android.synthetic.main.fragment_recycler.view.*
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import com.selasarimaji.perpus.ContentType
import com.selasarimaji.perpus.model.DataModel
import com.selasarimaji.perpus.view.activity.BaseNavigationActivity
import com.selasarimaji.perpus.view.activity.ContentCreationActivity
import com.selasarimaji.perpus.view.activity.ContentInspectActivity
import com.selasarimaji.perpus.view.adapter.ContentRecyclerAdapter
import com.selasarimaji.perpus.viewmodel.KidVM

class KidRecyclerFragment : BaseRecyclerFragment() {
    private val viewModel by lazy {
        ViewModelProviders.of(activity!!).get(KidVM::class.java)
    }

    override fun setupButton(view: View){
        view.fabButton.setOnClickListener {
            context?.let {
                val intent = ContentCreationActivity.createIntentToHere(it, ContentType.Kid)
                startActivityForResult(intent, CREATION_REQUEST_CODE)
            }
        }
    }

    override fun setupRecycler(view: View){
        val adapter = ContentRecyclerAdapter<DataModel.Kid>(ContentType.Kid){
            context?.run {
                startActivity(ContentInspectActivity.createIntentToHere(this, ContentType.Kid))
            }
        }
        val layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        view.recyclerView.layoutManager = layoutManager
        view.recyclerView.adapter = adapter

        view.recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView?, dx: Int, dy: Int) {
                val lastVisiblePosition = layoutManager.findLastVisibleItemPosition()
                val totalItemCount = layoutManager.itemCount

                val totalRemoteCount = viewModel.totalRemoteCount.value ?: totalItemCount
                if (lastVisiblePosition + thresholdItemCount >= totalItemCount
                        && totalItemCount < totalRemoteCount){
                    viewModel.loadMore()
                }
            }
        })

        viewModel.repo.fetchedData.observe(this, Observer {
            it?.let {
                adapter.setupNewData(it)
                if (it.isNotEmpty()) dismissLoading()
            }
        })
        viewModel.loadInitial()
    }

    override fun refresh(){
        super.refresh()
        viewModel.reload()
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CREATION_REQUEST_CODE && resultCode == Activity.RESULT_OK){
            refresh()
        }
    }
}
