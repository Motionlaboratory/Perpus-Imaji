package com.selasarimaji.perpus.view.activity

import android.os.Bundle
import com.selasarimaji.perpus.R
import kotlinx.android.synthetic.main.activity_content_creation.*

abstract class BaseContentCreationActivity : BaseNavigationActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_content_creation)
        setupToolbar()
        setupView()
        setupObserver()

        addButton.setOnClickListener {
            submitValue()
        }
    }

    protected abstract fun setupToolbar()
    protected abstract fun submitValue()
    protected abstract fun setupView()
    protected abstract fun setupObserver()
}
