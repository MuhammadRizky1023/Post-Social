package com.example.postsocial.customview



import android.content.Context
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.util.Patterns
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.content.ContextCompat
import com.example.postsocial.R


class MyEmailText : AppCompatEditText, View.OnTouchListener {
    var userEmailIsValid: Boolean = false
    private lateinit var emailIcon: Drawable

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
        emailIcon = ContextCompat.getDrawable(context, R.drawable.ic_baseline_email_24) as Drawable
        onShowVisibilityIcon(emailIcon)
    }

    private fun onShowVisibilityIcon(iconEmailDrawable: Drawable) {
        setButtonDrawables(startOfDrawable = iconEmailDrawable)
    }

    private fun setButtonDrawables(
        startOfDrawable: Drawable? = null,
        topOfDrawableThe: Drawable? = null,
        endOfDrawable: Drawable? = null,
        bottomOfDrawable: Drawable? = null
    ) {
        setCompoundDrawablesWithIntrinsicBounds(
            startOfDrawable,
            topOfDrawableThe,
            endOfDrawable,
            bottomOfDrawable
        )

    }

    private fun checkEmail() {
        val userEmail = text?.trim()
        if (userEmail.isNullOrEmpty()) {
            userEmailIsValid = false
            error = resources.getString(R.string.input_email)
        } else if (!Patterns.EMAIL_ADDRESS.matcher(userEmail).matches()) {
            userEmailIsValid = false
            error = resources.getString(R.string.valid_email_address)
        } else {
            userEmailIsValid = true
        }
    }

    override fun onFocusChanged(focused: Boolean, direction: Int, previouslyFocusedRect: Rect?) {
        super.onFocusChanged(focused, direction, previouslyFocusedRect)
        if (!focused) checkEmail()
    }

    override fun onTouch(p0: View?, p1: MotionEvent?): Boolean {
        return false
    }

}