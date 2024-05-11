package com.example.postsocial.customview


import android.content.Context
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.content.ContextCompat
import com.example.postsocial.R


class MyNameText: AppCompatEditText, View.OnTouchListener {

    var  userNameIsValid: Boolean = false
    private lateinit var nameIcon: Drawable

    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init()
    }

    private fun init() {
        nameIcon = ContextCompat.getDrawable(context, R.drawable.ic_baseline_person_24) as Drawable
        onShowVisibilityIcon(nameIcon)
    }

    private fun onShowVisibilityIcon(iconName: Drawable) {
        setButtonDrawables(startOfDrawable = iconName)
    }

    private fun setButtonDrawables(
        startOfDrawable: Drawable? = null,
        topOfDrawable: Drawable? = null,
        endOfDrawable: Drawable? = null,
        bottomOfDrawable: Drawable? = null
    ) {
        setCompoundDrawablesWithIntrinsicBounds(
            startOfDrawable,
            topOfDrawable,
            endOfDrawable,
            bottomOfDrawable
        )

    }


    private fun checkName() {
        val userName = text?.trim()
        if (userName.isNullOrEmpty()) {
            userNameIsValid = false
            error = resources.getString(R.string.name_is_empty)
        } else {
            userNameIsValid = true
        }
    }

    override fun onFocusChanged(focused: Boolean, direction: Int, previouslyFocusedRect: Rect?) {
        super.onFocusChanged(focused, direction, previouslyFocusedRect)
        if (!focused) checkName()
    }

    override fun onTouch(p0: View?, p1: MotionEvent?): Boolean {
        return false
    }

}