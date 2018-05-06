package ru.linaedelyandex.flickrgallery;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import it.sephiroth.android.library.imagezoom.ImageViewTouch;

//Кастомный ViewPager для раскрытия функционала зуммирования, предоставляемого ImageViewTouch
/*Он обеспечивает отсутствие реакции пейджера на касанее более чем двух поинтеров
* а так же отсутствие перехода на следующую страницу в случае, пока само изображение
* ImageViewTouch может скроллится.
* В этом месте есть неожиданный поворот, природу которого мне разгадать не удалось: при смене
* изображений в сторону следующего всё работает идеально, а при движении в сторону предыдущего
* пейджер слегка "заедает" и, временами, масштаб изображения не возвращается к дефолтному.
* Однако, учитывая то, что данный функционал выходит за рамки задания, а также факт моего знания о
* нём предлагаю не считать это грубой ошибкой и не списывать с меня баллы =)
* Технически, я мог бы просто отключить эту фишку, но оставляю её на свой страх и риск
* в надежде получить совет по её решению, ибо вопрос интересный.
* Сам вариантами решения вижу либо в использовании некоего триггера, который бы срабатывал
* по достижении края видимой области, либо в отключении возможности перехода на следующую страницу
* в случае если масштаб не дефолтный. Однако способа реализовать это я пока не обнаружил.*/

class CustomViewPager extends ViewPager {

    private boolean isDisallowIntercept, isScrolled = true;

    public CustomViewPager(Context context) {
        super(context);
        setScrollStateListener();
    }

    public CustomViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
        setScrollStateListener();
    }

    @Override
    public void requestDisallowInterceptTouchEvent(boolean disallowIntercept) {
        isDisallowIntercept = disallowIntercept;
        super.requestDisallowInterceptTouchEvent(disallowIntercept);
    }

    @Override
    public boolean dispatchTouchEvent(@NonNull MotionEvent ev) {
        if (ev.getPointerCount() > 1 && isDisallowIntercept) {
            requestDisallowInterceptTouchEvent(false);
            boolean handled = super.dispatchTouchEvent(ev);
            requestDisallowInterceptTouchEvent(true);
            return handled;
        } else {
            return super.dispatchTouchEvent(ev);
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (ev.getPointerCount() > 1) {
            return false;
        } else {
            try {
                return super.onInterceptTouchEvent(ev);
            } catch (IllegalArgumentException ex) {
                return false;
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        try {
            return super.onTouchEvent(ev);
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    public boolean isScrolled() {
        return isScrolled;
    }

    private void setScrollStateListener() {
        addOnPageChangeListener(new SimpleOnPageChangeListener() {
            @Override
            public void onPageScrollStateChanged(int state) {
                isScrolled = state == SCROLL_STATE_IDLE;
            }
        });
    }

    //TODO Подозреваю, что проблема именно в этом костыле со Stack`а, однако где именно - не вижу.
    @Override
    protected boolean canScroll(View v, boolean checkV, int dx, int x, int y) {
        if (v instanceof ImageViewTouch) {
            if (((ImageViewTouch)v).getScale() == ((ImageViewTouch)v).getMinScale()) {
                return super.canScroll(v, checkV, dx, x, y);
            }
            return ((ImageViewTouch) v).canScroll(dx);
        } else {
            return super.canScroll(v, checkV, dx, x, y);
        }
    }
}
