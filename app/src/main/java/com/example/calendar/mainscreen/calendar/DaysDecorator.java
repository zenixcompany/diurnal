package com.example.calendar.mainscreen.calendar;

import android.content.Context;
import android.text.style.TextAppearanceSpan;

import com.example.calendar.R;
import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.DayViewDecorator;
import com.prolificinteractive.materialcalendarview.DayViewFacade;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;

import java.util.Calendar;

public class DaysDecorator implements DayViewDecorator {
    private Context context;
    private MaterialCalendarView calendarView;

    public DaysDecorator(Context context, MaterialCalendarView calendarView) {
        this.context = context;
        this.calendarView = calendarView;
    }

    @Override
    public boolean shouldDecorate(CalendarDay day) {
        Calendar cal1 = day.getCalendar();
        Calendar cal2 = calendarView.getCurrentDate().getCalendar();

        return cal1.get(Calendar.ERA) == cal2.get(Calendar.ERA) &&
                cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH);
    }

    @Override
    public void decorate(DayViewFacade view) {
        view.addSpan(new TextAppearanceSpan(context, R.style.Day));
    }
}
