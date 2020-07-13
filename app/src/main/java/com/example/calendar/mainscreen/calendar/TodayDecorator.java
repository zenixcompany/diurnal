package com.example.calendar.mainscreen.calendar;

import android.content.Context;
import android.text.style.TextAppearanceSpan;

import com.example.calendar.R;
import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.DayViewDecorator;
import com.prolificinteractive.materialcalendarview.DayViewFacade;

import java.util.Calendar;

public class TodayDecorator implements DayViewDecorator {
    private Context context;

    public TodayDecorator(Context context) {
        this.context = context;
    }

    @Override
    public boolean shouldDecorate(CalendarDay day) {
        Calendar cal1 = day.getCalendar();
        Calendar cal2 = Calendar.getInstance();

        return (cal1.get(Calendar.ERA) == cal2.get(Calendar.ERA)
                && cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR)
                && cal1.get(Calendar.DAY_OF_YEAR) ==
                cal2.get(Calendar.DAY_OF_YEAR));
    }

    @Override
    public void decorate(DayViewFacade view) {
        view.addSpan(new TextAppearanceSpan(context, R.style.Today));
    }
}
