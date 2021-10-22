package com.zfs.slidetoggleview

import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.text.TextUtils
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.view.ViewCompat
import androidx.customview.widget.ViewDragHelper
import androidx.lifecycle.LifecycleCoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min

/**
 *
 */
class SlideToggleView: FrameLayout {

    private val TAG = "SlideToggleView"

    private val mViewDragHelper: ViewDragHelper by lazy { ViewDragHelper.create(this, 1.0f, mDragCallback) }

    //滑块
    private val mBlockView: ImageView by lazy { ImageView(context) }

    //滑块外边距
    private var mBlockLeftMargin = 0
    private var mBlockRightMargin = 0
    private var mBlockTopMargin = 0
    private var mBlockBottomMargin = 0

    //开启和关闭时的背景资源
    private var mOpenBackground: Drawable? = null
    private var mCloseBackground: Drawable? = null

    //触发打开事件允许剩余的距离
    private var mRemainDistance = 0

    //滑动总长度
    private var slideTotal = 0

    //是否开启的标识
    private var mIsOpen: Boolean = false

    //文字
    private val lightWaveTextView: LightWaveTextView by lazy { LightWaveTextView(context) }

    private var mInLayout = false

    //是否处于滑动状态的标识
    private var inScoring = false

    private var mLifecycleCoroutineScope: LifecycleCoroutineScope? = null

    //从左滑动开始还是从右滑动开始
    /**
     * 1 左边开始滑动 滑动到右边算开锁
     * 2 右边开始滑动 滑动到左边算开锁
     */
    /*private int leftOrRightStart = 1;*/

    //打开所显示的文字，关闭所显示的文字
    var openText: String? = null
    var closeText: String? = null

    private val shimmer by lazy { LightWave.AlphaHighlightBuilder().build() }

    private var mSlideToggleListener: SlideToggleListener? = null
    private var mSlideReleasedListener: SlideReleasedListener? = null


    constructor(context: Context): this(context, null)

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int): super(context, attrs, defStyleAttr) {
        init(context, attrs, defStyleAttr)
    }

    private fun init(context: Context, attrs: AttributeSet?, defStyleAttr: Int) {
        context.obtainStyledAttributes(attrs, R.styleable.SlideToggleView, defStyleAttr, 0).apply {
            openText = getString(R.styleable.SlideToggleView_stv_openText)
            closeText = getString(R.styleable.SlideToggleView_stv_closeText)
            val textColor = getColor(R.styleable.SlideToggleView_stv_textColor, -0x1)
            val textSize = getDimensionPixelSize(R.styleable.SlideToggleView_stv_textSize, dp2px(14).toInt())
            val slideBlock = getDrawable(R.styleable.SlideToggleView_stv_slideBlock)
            mBlockLeftMargin = getDimensionPixelSize(R.styleable.SlideToggleView_stv_blockLeftMargin, 1)
            mBlockRightMargin = getDimensionPixelSize(R.styleable.SlideToggleView_stv_blockRightMargin, 1)
            mBlockTopMargin = getDimensionPixelSize(R.styleable.SlideToggleView_stv_blockTopMargin, 1)
            mBlockBottomMargin = getDimensionPixelSize(R.styleable.SlideToggleView_stv_blockBottomMargin, 1)
            mRemainDistance = getDimensionPixelSize(R.styleable.SlideToggleView_stv_remain, 10)
            mOpenBackground = getDrawable(R.styleable.SlideToggleView_stv_openBackground)
            mCloseBackground = getDrawable(R.styleable.SlideToggleView_stv_closeBackground)
            val mSlideBlockWidth = getDimensionPixelSize(R.styleable.SlideToggleView_stv_slideBlockWidth, dp2px(50).toInt())
            //        leftOrRightStart = getInt(R.styleable.SlideToggleView_stv_leftOrRightStart,1);
            recycle()

            //文字
            val textParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
            textParams.gravity = Gravity.CENTER
            lightWaveTextView.text = closeText
            lightWaveTextView.setTextColor(textColor)
            lightWaveTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize.toFloat())
            lightWaveTextView.ellipsize = TextUtils.TruncateAt.END
            lightWaveTextView.isSingleLine = true
            val horizontalPadding = mSlideBlockWidth + mBlockLeftMargin + mBlockRightMargin
            lightWaveTextView.setPadding(horizontalPadding, 0, horizontalPadding, 0)
            addView(lightWaveTextView, textParams)

            //滑块
            mBlockView.scaleType = ImageView.ScaleType.CENTER_CROP
            mBlockView.setImageDrawable(slideBlock)
            val blockParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
            blockParams.width = mSlideBlockWidth
            blockParams.setMargins(mBlockLeftMargin, mBlockTopMargin, mBlockRightMargin, mBlockBottomMargin)
            addView(mBlockView, blockParams)
        }

        //默认初始为关闭
        mIsOpen = false
        background = mCloseBackground
    }

    /**
     * 绑定外部生命周期域
     */
    fun addLifecycleScope(lifecycleScope: LifecycleCoroutineScope) {
        mLifecycleCoroutineScope = lifecycleScope
    }




    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        Log.i(TAG, "onLayout -> changed:$changed")
        mInLayout = true
        if (!mViewDragHelper.continueSettling(true)) {
            if (changed) {
                super.onLayout(changed, left, top, right, bottom)
                Log.i(TAG, "onLayout -> super.onLayout()")
                return
            }
            val childLeft: Int = if (mIsOpen) {
                measuredWidth - paddingRight - mBlockRightMargin - mBlockView.measuredWidth
            } else {
                paddingLeft + mBlockLeftMargin
            }
            Log.i(TAG, "onLayout -> mBlockView.layout")
            mBlockView.layout(childLeft, mBlockTopMargin, childLeft + mBlockView.measuredWidth, mBlockTopMargin + mBlockView.measuredHeight)
        } else super.onLayout(changed, left, top, right, bottom)
        mInLayout = false
    }

    override fun requestLayout() {
        Log.i(TAG, "requestLayout -> requestLayout()")
        if (!mInLayout) {
            super.requestLayout()
        }
    }

    override fun onDraw(canvas: Canvas?) {
        Log.i(TAG, "onDraw -> onDraw()")
        super.onDraw(canvas)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        //计算滑动总长度
        slideTotal = measuredWidth - paddingLeft - paddingRight - mBlockLeftMargin - mBlockRightMargin - mBlockView.measuredWidth
        Log.d(TAG, "--mWidth==$measuredWidth--mWidth==$measuredHeight--slideTotal==$slideTotal--mRemainDistance==$mRemainDistance")
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        //固定写法
        val action = ev.action
        if (action == MotionEvent.ACTION_CANCEL
            || action == MotionEvent.ACTION_UP
        ) {
            mViewDragHelper.cancel()
            return false
        }
        Log.i(TAG, "onInterceptTouchEvent -> ev action: " + ev.action)
        return mViewDragHelper.shouldInterceptTouchEvent(ev)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (
            (event.action == MotionEvent.ACTION_MOVE || event.action == MotionEvent.ACTION_DOWN)
            && !inScoring
            && mViewDragHelper.viewDragState == ViewDragHelper.STATE_DRAGGING
        ) {
            //更新滑动标识，说明进行的是滑动操作
            inScoring = true
        }

        mViewDragHelper.processTouchEvent(event)

        //针对点击事件的处理，只有满足滑动状态是未拖动以及手指放开的条件下，才会触发点击事件
        if (mViewDragHelper.viewDragState == ViewDragHelper.STATE_IDLE && event.action == MotionEvent.ACTION_UP && !inScoring) {
            callOnClick()
        }

        if (inScoring && event.action == MotionEvent.ACTION_UP && mViewDragHelper.viewDragState == ViewDragHelper.STATE_IDLE) {
            //更新滑动标识，表明滑动后已放开手指
            inScoring = false
        }
        Log.i(TAG, "onTouchEvent -> event action= ${event.action}, ViewDragHelper state= ${mViewDragHelper.viewDragState}")
        return true
    }

    override fun computeScroll() {
        val b = mViewDragHelper.continueSettling(true)
        if (b) {
            invalidate()
        }
        /*else {
            super.computeScroll();
        }*/
        Log.i(TAG, "computeScroll -> b: $b")
    }




    private val mDragCallback: ViewDragHelper.Callback = object : ViewDragHelper.Callback() {
        override fun tryCaptureView(view: View, pointerId: Int): Boolean {
            return view == mBlockView
        }

        override fun onViewCaptured(capturedChild: View, activePointerId: Int) {
            val parent = capturedChild.parent
            parent?.requestDisallowInterceptTouchEvent(true)
            Log.v(TAG, "onViewCaptured")
        }

        override fun clampViewPositionHorizontal(child: View, left: Int, dx: Int): Int {
            //防止滑块超出容器边界
            var newLeft = left
            val min: Int = paddingLeft + mBlockLeftMargin
            if (newLeft < min) {
                newLeft = min
            }
            val max: Int = (measuredWidth - paddingRight - mBlockRightMargin - mBlockView.measuredWidth)
            if (newLeft > max) {
                newLeft = max
            }
            Log.v(TAG, "clampViewPositionHorizontal left:$newLeft")
            return newLeft
        }

        override fun clampViewPositionVertical(child: View, top: Int, dy: Int): Int {
            //滑动过程保持上边距
//            return getPaddingTop() + mBlockTopMargin;
//            return super.clampViewPositionVertical(child, top, dy);//默认为0
            Log.i(TAG, "clampViewPositionVertical() -> top: " + top + "dy: " + dy)
            val topBound: Int = paddingTop
            val bottomBound: Int = height - mBlockView.height
            return min(max(top, topBound), bottomBound)
        }

        override fun onViewPositionChanged(changedView: View, left: Int, top: Int, dx: Int, dy: Int) {
            /*if (mSlideToggleListener != null) {
                int slide = left - getPaddingLeft() - mBlockLeftMargin;
                mSlideToggleListener.onBlockPositionChanged(SlideToggleView.this, left, slideTotal, slide);
            }*/
            Log.d(TAG, "onViewPositionChanged -> total==$slideTotal---left--$left")
        }

        override fun onViewReleased(releasedChild: View, xvel: Float, yvel: Float) {
            mLifecycleCoroutineScope?.launchWhenResumed {
                val slide: Int = releasedChild.left - paddingLeft - mBlockLeftMargin
                Log.d(TAG, "onViewReleased -> total==$slideTotal---slide--$slide---xvel--$xvel---yvel--$yvel")

                if (releasedChild == mBlockView) {
                    if (!mIsOpen) {
                        if (slide >= mRemainDistance) {
                            Log.d(TAG, "打开")
                            if (mSlideReleasedListener != null && mSlideReleasedListener!!.onOpenBeforeListener()) {
                                withContext(Dispatchers.Main.immediate) {
                                    //恢复到原来位置
                                    val finalLeft: Int = paddingLeft + mBlockLeftMargin
                                    val finalTop: Int = paddingTop + mBlockTopMargin
                                    mViewDragHelper.smoothSlideViewTo(mBlockView, finalLeft, finalTop)
                                    postInvalidate()
                                }
                                return@launchWhenResumed
                            }

                            //滑动到最终位置
                            val finalLeft: Int = measuredWidth - paddingRight - mBlockRightMargin - mBlockView.measuredWidth
                            val finalTop: Int = paddingTop + mBlockTopMargin
                            Log.d(TAG, "打开$finalLeft")
                            mViewDragHelper.smoothSlideViewTo(mBlockView, finalLeft, finalTop)
                            postInvalidate()
                            lightWaveTextView.text = openText
                            background = mOpenBackground
                            mIsOpen = true
                            lightWaveTextView.postDelayed({
                                lightWaveTextView.stopLightWave()
                                lightWaveTextView.setLightWave(null)
                            }, 100)
                            mSlideToggleListener?.onSlideListener(this@SlideToggleView, 1)
                        } else {
                            //不满足则恢复到原来位置
                            Log.d(TAG, "恢复原位")
                            val finalLeft: Int = paddingLeft + mBlockLeftMargin
                            val finalTop: Int = paddingTop + mBlockTopMargin
                            mViewDragHelper.smoothSlideViewTo(mBlockView, finalLeft, finalTop)
                            postInvalidate()
                        }
                    } else {
                        if (slideTotal - slide >= mRemainDistance) {
                            Log.d(TAG, "关闭")
                            if (mSlideReleasedListener != null && mSlideReleasedListener!!.onCloseBeforeListener()) {
                                val finalLeft: Int =
                                    measuredWidth - paddingRight - mBlockRightMargin - mBlockView.measuredWidth
                                val finalTop: Int = paddingTop + mBlockTopMargin
                                mViewDragHelper.smoothSlideViewTo(mBlockView, finalLeft, finalTop)
                                postInvalidate()
                                return@launchWhenResumed
                            }
                            val finalLeft: Int = paddingLeft + mBlockLeftMargin
                            val finalTop: Int = paddingTop + mBlockTopMargin
                            mViewDragHelper.smoothSlideViewTo(mBlockView, finalLeft, finalTop)
                            postInvalidate()
                            lightWaveTextView.text = closeText
                            background = mCloseBackground
                            mIsOpen = false
                            lightWaveTextView.postDelayed({
                                lightWaveTextView.setLightWave(shimmer)
                                lightWaveTextView.startLightWave()
                            }, 100)
                            mSlideToggleListener?.onSlideListener(this@SlideToggleView, 0)
                        } else {
                            val finalLeft: Int = measuredWidth - paddingRight - mBlockRightMargin - mBlockView.measuredWidth
                            val finalTop: Int = paddingTop + mBlockTopMargin
                            mViewDragHelper.smoothSlideViewTo(mBlockView, finalLeft, finalTop)
                            postInvalidate()
                        }
                    }
                }
            }
        }
    }




    /**
     * 点击逻辑处理
     * @return 点击是否可执行
     */
    suspend fun clickToggle(): Boolean {
        if (mIsOpen) {
            //执行关闭
            if (mSlideReleasedListener != null && mSlideReleasedListener!!.onCloseBeforeListener()) return false
            val finalLeft: Int = paddingLeft + mBlockLeftMargin
            val finalTop: Int = paddingTop + mBlockTopMargin
            mViewDragHelper.smoothSlideViewTo(mBlockView, finalLeft, finalTop)
            ViewCompat.postInvalidateOnAnimation(this@SlideToggleView)
            lightWaveTextView.text = closeText
            background = mCloseBackground
            mIsOpen = false
            lightWaveTextView.postDelayed({
                lightWaveTextView.setLightWave(shimmer)
                lightWaveTextView.startLightWave()
            }, 100)
        } else {
            //执行打开
            if (mSlideReleasedListener != null && mSlideReleasedListener!!.onOpenBeforeListener()) return false
            val finalLeft: Int = measuredWidth - paddingRight - mBlockRightMargin - mBlockView.measuredWidth
            val finalTop: Int = paddingTop + mBlockTopMargin
            mViewDragHelper.smoothSlideViewTo(mBlockView, finalLeft, finalTop)
            ViewCompat.postInvalidateOnAnimation(this@SlideToggleView)
            lightWaveTextView.text = openText
            background = mOpenBackground
            mIsOpen = true
            lightWaveTextView.postDelayed({
                lightWaveTextView.stopLightWave()
                lightWaveTextView.setLightWave(null)
            }, 100)
        }
        return true
    }

    fun resetToggle() {
        if (!mIsOpen) return
        val finalLeft: Int = paddingLeft + mBlockLeftMargin
        val finalTop: Int = paddingTop + mBlockTopMargin
        mViewDragHelper.smoothSlideViewTo(mBlockView, finalLeft, finalTop)
        lightWaveTextView.text = closeText
        background = mCloseBackground
        mIsOpen = false
        lightWaveTextView.postDelayed({
            lightWaveTextView.setLightWave(shimmer)
            lightWaveTextView.startLightWave()
        }, 100)
    }


    fun updateShimmerText(txt: String) {
        if (mIsOpen) closeText = txt else openText = txt
        lightWaveTextView.text = txt
    }





    /**
     * 设置滑动开关监听器
     *
     * @param listener 滑动开关监听器
     */
    fun setSlideToggleListener(listener: SlideToggleListener?) {
        mSlideToggleListener = listener
    }

    fun setSlideReleaseListener(listener: SlideReleasedListener?) {
        mSlideReleasedListener = listener
    }

    interface SlideToggleListener {
        /**
         * 滑块位置改变回调
         *
         * @param left  滑块左侧位置，值等于[.getLeft]
         * @param total 滑块可以滑动的总距离
         * @param slide 滑块已经滑动的距离
         */
        //        void onBlockPositionChanged(SlideToggleView view, int left, int total, int slide);
        /**
         * 滑动打开
         * @param leftOrRight  0 左边
         * @param leftOrRight  1 右边
         */
        fun onSlideListener(view: SlideToggleView?, leftOrRight: Int)
    }

    interface SlideReleasedListener {
        /**
         *
         * @return true表示拦截
         */
        suspend fun onOpenBeforeListener(): Boolean
        suspend fun onCloseBeforeListener(): Boolean
    }

    private fun dp2px(dp: Int): Float = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp.toFloat(), this.resources.displayMetrics)
}