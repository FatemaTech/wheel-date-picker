package tech.shikho.wheeldatepicker.datepicker;


import static tech.shikho.wheeldatepicker.datepicker.widget.SingleDateAndTimeConstants.DAYS_PADDING;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import tech.shikho.wheeldatepicker.R;
import tech.shikho.wheeldatepicker.datepicker.widget.DateWithLabel;
import tech.shikho.wheeldatepicker.datepicker.widget.WheelDayOfMonthPicker;
import tech.shikho.wheeldatepicker.datepicker.widget.WheelDayPicker;
import tech.shikho.wheeldatepicker.datepicker.widget.WheelMonthPicker;
import tech.shikho.wheeldatepicker.datepicker.widget.WheelPicker;
import tech.shikho.wheeldatepicker.datepicker.widget.WheelYearPicker;

public class SingleDateAndTimePicker extends LinearLayout {

    public static final boolean IS_CYCLIC_DEFAULT = true;
    public static final boolean IS_CURVED_DEFAULT = false;
    public static final boolean MUST_BE_ON_FUTURE_DEFAULT = false;
    public static final int DELAY_BEFORE_CHECK_PAST = 200;
    private static final int VISIBLE_ITEM_COUNT_DEFAULT = 7;
    private static final int PM_HOUR_ADDITION = 12;
    public static final int ALIGN_CENTER = 0;
    public static final int ALIGN_LEFT = 1;
    public static final int ALIGN_RIGHT = 2;
    private DateHelper dateHelper = new DateHelper();

    private static final CharSequence FORMAT_24_HOUR = "EEE d MMM H:mm";
    private static final CharSequence FORMAT_12_HOUR = "EEE d MMM h:mm a";

    @NonNull
    private final WheelYearPicker yearsPicker;

    @NonNull
    private final WheelMonthPicker monthPicker;

    @NonNull
    private final WheelDayOfMonthPicker daysOfMonthPicker;

    @NonNull
    private final WheelDayPicker daysPicker;

    private List<WheelPicker> pickers = new ArrayList<>();

    private List<OnDateChangedListener> listeners = new ArrayList<>();

    private View dtSelector;
    private boolean mustBeOnFuture;

    @Nullable
    private Date minDate;
    @Nullable
    private Date maxDate;
    @NonNull
    private Date defaultDate;

    private boolean displayYears = false;
    private boolean displayMonth = false;
    private boolean displayDaysOfMonth = false;
    private boolean displayDays = true;
    private boolean displayMinutes = true;
    private boolean displayHours = true;

    private boolean isAmPm;

    public SingleDateAndTimePicker(Context context) {
        this(context, null);
    }

    public SingleDateAndTimePicker(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SingleDateAndTimePicker(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        defaultDate = new Date();
        isAmPm = !(DateFormat.is24HourFormat(context));

        inflate(context, R.layout.single_day_and_time_picker, this);

        yearsPicker = findViewById(R.id.yearPicker);
        monthPicker = findViewById(R.id.monthPicker);
        daysOfMonthPicker = findViewById(R.id.daysOfMonthPicker);
        daysPicker = findViewById(R.id.daysPicker);
        dtSelector = findViewById(R.id.dtSelector);

        pickers.addAll(Arrays.asList(
                daysPicker,
                daysOfMonthPicker,
                monthPicker,
                yearsPicker
        ));
        for (WheelPicker wheelPicker : pickers) {
            wheelPicker.setDateHelper(dateHelper);
        }
        init(context, attrs);

        setDisplayDays(false);
        setDisplayMonths(true);
        setDisplayDaysOfMonth(true);
        setDisplayYears(true);
        setDefaultDate(defaultDate);
        setDisplayMonthNumbers(false);
    }

      @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        yearsPicker.setOnYearSelectedListener((picker, position, year) -> {
            updateListener();
            checkMinMaxDate(picker);

            if (displayDaysOfMonth) {
                updateDaysOfMonth();
            }
        });

        monthPicker.setOnMonthSelectedListener((picker, monthIndex, monthName) -> {
            updateListener();
            checkMinMaxDate(picker);

            if (displayDaysOfMonth) {
                updateDaysOfMonth();
            }
        });

        daysOfMonthPicker
                .setDayOfMonthSelectedListener((picker, dayIndex) -> {
                    updateListener();
                    checkMinMaxDate(picker);
                });

        daysOfMonthPicker
                .setOnFinishedLoopListener(picker -> {
                    if (displayMonth) {
                        monthPicker.scrollTo(monthPicker.getCurrentItemPosition() + 1);
                        updateDaysOfMonth();
                    }
                });

        daysPicker
                .setOnDaySelectedListener((picker, position, name, date) -> {
                    updateListener();
                    checkMinMaxDate(picker);
                });

        setDefaultDate(this.defaultDate); //update displayed date
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        for (WheelPicker picker : pickers) {
            picker.setEnabled(enabled);
        }
    }

    public void setDisplayYears(boolean displayYears) {
        this.displayYears = displayYears;
        yearsPicker.setVisibility(displayYears ? VISIBLE : GONE);
    }

    public void setDisplayMonths(boolean displayMonths) {
        this.displayMonth = displayMonths;
        monthPicker.setVisibility(displayMonths ? VISIBLE : GONE);
        checkSettings();
    }

    public void setDisplayDaysOfMonth(boolean displayDaysOfMonth) {
        this.displayDaysOfMonth = displayDaysOfMonth;
        daysOfMonthPicker.setVisibility(displayDaysOfMonth ? VISIBLE : GONE);

        if (displayDaysOfMonth) {
            updateDaysOfMonth();
        }
        checkSettings();
    }

    public void setDisplayDays(boolean displayDays) {
        this.displayDays = displayDays;
        daysPicker.setVisibility(displayDays ? VISIBLE : GONE);
        checkSettings();
    }

    public void setDisplayMonthNumbers(boolean displayMonthNumbers) {
        this.monthPicker.setDisplayMonthNumbers(displayMonthNumbers);
        this.monthPicker.updateAdapter();
    }

    public void setMonthFormat(String monthFormat) {
        this.monthPicker.setMonthFormat(monthFormat);
        this.monthPicker.updateAdapter();
    }

    public void setTodayText(DateWithLabel todayText) {
        if (todayText != null && todayText.label != null && !todayText.label.isEmpty()) {
            daysPicker.setTodayText(todayText);
        }
    }

    public void setItemSpacing(int size) {
        for (WheelPicker picker : pickers) {
            picker.setItemSpace(size);
        }
    }

    public void setCurvedMaxAngle(int angle) {
        for (WheelPicker picker : pickers) {
            picker.setCurvedMaxAngle(angle);
        }
    }

    public void setCurved(boolean curved) {
        for (WheelPicker picker : pickers) {
            picker.setCurved(curved);
        }
    }

    public void setCyclic(boolean cyclic) {
        for (WheelPicker picker : pickers) {
            picker.setCyclic(cyclic);
        }
    }

    public void setTextSize(int textSize) {
        for (WheelPicker picker : pickers) {
            picker.setItemTextSize(textSize);
        }
    }

    public void setSelectedTextColor(int selectedTextColor) {
        for (WheelPicker picker : pickers) {
            picker.setSelectedItemTextColor(selectedTextColor);
        }
    }

    public void setTextColor(int textColor) {
        for (WheelPicker picker : pickers) {
            picker.setItemTextColor(textColor);
        }
    }

    public void setTextAlign(int align) {
        for (WheelPicker picker : pickers) {
            picker.setItemAlign(align);
        }
    }

    public void setTypeface(Typeface typeface) {
        if (typeface == null) return;
        for (WheelPicker picker : pickers) {
            picker.setTypeface(typeface);
        }
    }

    private void setFontToAllPickers(int resourceId) {
        if (resourceId > 0) {
            for (int i = 0; i < pickers.size(); i++) {
                pickers.get(i).setTypeface(ResourcesCompat.getFont(getContext(), resourceId));
            }
        }
    }

    public void setSelectorColor(int selectorColor) {
        dtSelector.setBackgroundColor(selectorColor);
    }

    public void setSelectorHeight(int selectorHeight) {
        final ViewGroup.LayoutParams dtSelectorLayoutParams = dtSelector.getLayoutParams();
        dtSelectorLayoutParams.height = selectorHeight;
        dtSelector.setLayoutParams(dtSelectorLayoutParams);
    }

    public void setVisibleItemCount(int visibleItemCount) {
        for (WheelPicker picker : pickers) {
            picker.setVisibleItemCount(visibleItemCount);
        }
    }

    private void checkMinMaxDate(final WheelPicker picker) {
        checkBeforeMinDate(picker);
        checkAfterMaxDate(picker);
    }

    private void checkBeforeMinDate(final WheelPicker picker) {
        picker.postDelayed(() -> {
            if (minDate != null && isBeforeMinDate(getDate())) {
                for (WheelPicker p : pickers) {
                    p.scrollTo(p.findIndexOfDate(minDate));
                }
            }
        }, DELAY_BEFORE_CHECK_PAST);
    }

    private void checkAfterMaxDate(final WheelPicker picker) {
        picker.postDelayed(() -> {
            if (maxDate != null && isAfterMaxDate(getDate())) {
                for (WheelPicker p : pickers) {
                    p.scrollTo(p.findIndexOfDate(maxDate));
                }
            }
        }, DELAY_BEFORE_CHECK_PAST);
    }

    private boolean isBeforeMinDate(Date date) {
        return dateHelper.getCalendarOfDate(date).before(dateHelper.getCalendarOfDate(minDate));
    }

    private boolean isAfterMaxDate(Date date) {
        return dateHelper.getCalendarOfDate(date).after(dateHelper.getCalendarOfDate(maxDate));
    }

    public void addOnDateChangedListener(OnDateChangedListener listener) {
        this.listeners.add(listener);
    }

    public void removeOnDateChangedListener(OnDateChangedListener listener) {
        this.listeners.remove(listener);
    }

    public void checkPickersMinMax() {
        for (WheelPicker picker : pickers) {
            checkMinMaxDate(picker);
        }
    }

    public Date getDate() {


        final Calendar calendar = Calendar.getInstance();
        calendar.setTimeZone(dateHelper.getTimeZone());
        if (displayDays) {
            final Date dayDate = daysPicker.getCurrentDate();
            calendar.setTime(dayDate);
        } else {
            if (displayMonth) {
                calendar.set(Calendar.MONTH, monthPicker.getCurrentMonth());
            }

            if (displayYears) {
                calendar.set(Calendar.YEAR, yearsPicker.getCurrentYear());
            }

            if (displayDaysOfMonth) {
                int daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
                if (daysOfMonthPicker.getCurrentDay() >= daysInMonth) {
                    calendar.set(Calendar.DAY_OF_MONTH, daysInMonth);
                } else {
                    calendar.set(Calendar.DAY_OF_MONTH, daysOfMonthPicker.getCurrentDay() + 1);
                }
            }
        }
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }

    public void setDefaultDate(Date date) {
        if (date != null) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeZone(dateHelper.getTimeZone());
            calendar.setTime(date);
            this.defaultDate = calendar.getTime();

            updateDaysOfMonth(calendar);

            for (WheelPicker picker : pickers) {
                picker.setDefaultDate(defaultDate);
            }
        }
    }

    public void selectDate(Calendar calendar) {
        if (calendar == null) {
            return;
        }

        final Date date = calendar.getTime();
        for (WheelPicker picker : pickers) {
            picker.selectDate(date);
        }

        if (displayDaysOfMonth) {
            updateDaysOfMonth();
        }
    }

    private void updateListener() {
        final Date date = getDate();
        final CharSequence format = isAmPm ? FORMAT_12_HOUR : FORMAT_24_HOUR;
        final String displayed = DateFormat.format(format, date).toString();
        for (OnDateChangedListener listener : listeners) {
            listener.onDateChanged(displayed, date);
        }
    }

    private void updateDaysOfMonth() {
        final Date date = getDate();
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeZone(dateHelper.getTimeZone());
        calendar.setTime(date);
        updateDaysOfMonth(calendar);
    }

    private void updateDaysOfMonth(@NonNull Calendar calendar) {
        int daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
        daysOfMonthPicker.setDaysInMonth(daysInMonth);
        daysOfMonthPicker.updateAdapter();
    }

    public void setMustBeOnFuture(boolean mustBeOnFuture) {
        this.mustBeOnFuture = mustBeOnFuture;
        daysPicker.setShowOnlyFutureDate(mustBeOnFuture);
        if (mustBeOnFuture) {
            Calendar now = Calendar.getInstance();
            now.setTimeZone(dateHelper.getTimeZone());
            minDate = now.getTime(); //minDate is Today
        }
    }

    public boolean mustBeOnFuture() {
        return mustBeOnFuture;
    }

    private void setMinYear() {

        if (displayYears && this.minDate != null && this.maxDate != null) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeZone(dateHelper.getTimeZone());
            calendar.setTime(this.minDate);
            yearsPicker.setMinYear(calendar.get(Calendar.YEAR));
            calendar.setTime(this.maxDate);
            yearsPicker.setMaxYear(calendar.get(Calendar.YEAR));
        }
    }

    private void checkSettings() {
        if (displayDays && (displayDaysOfMonth || displayMonth)) {
            throw new IllegalArgumentException("You can either display days with months or days and months separately");
        }
    }

    private void init(Context context, AttributeSet attrs) {
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.SingleDateAndTimePicker);

        final Resources resources = getResources();
        setTodayText(new DateWithLabel(a.getString(R.styleable.SingleDateAndTimePicker_picker_todayText), new Date()));
        setTextColor(a.getColor(R.styleable.SingleDateAndTimePicker_picker_textColor, ContextCompat.getColor(context, R.color.picker_default_text_color)));
        setSelectedTextColor(a.getColor(R.styleable.SingleDateAndTimePicker_picker_selectedTextColor, ContextCompat.getColor(context, R.color.picker_default_selected_text_color)));
        setSelectorColor(a.getColor(R.styleable.SingleDateAndTimePicker_picker_selectorColor, ContextCompat.getColor(context, R.color.picker_default_selector_color)));
        setItemSpacing(a.getDimensionPixelSize(R.styleable.SingleDateAndTimePicker_picker_itemSpacing, resources.getDimensionPixelSize(R.dimen.wheelSelectorHeight)));
        setCurvedMaxAngle(a.getInteger(R.styleable.SingleDateAndTimePicker_picker_curvedMaxAngle, WheelPicker.MAX_ANGLE));
        setSelectorHeight(a.getDimensionPixelSize(R.styleable.SingleDateAndTimePicker_picker_selectorHeight, resources.getDimensionPixelSize(R.dimen.wheelSelectorHeight)));
        setTextSize(a.getDimensionPixelSize(R.styleable.SingleDateAndTimePicker_picker_textSize, resources.getDimensionPixelSize(R.dimen.WheelItemTextSize)));
        setCurved(a.getBoolean(R.styleable.SingleDateAndTimePicker_picker_curved, IS_CURVED_DEFAULT));
        setCyclic(a.getBoolean(R.styleable.SingleDateAndTimePicker_picker_cyclic, IS_CYCLIC_DEFAULT));
        setMustBeOnFuture(a.getBoolean(R.styleable.SingleDateAndTimePicker_picker_mustBeOnFuture, MUST_BE_ON_FUTURE_DEFAULT));
        setVisibleItemCount(a.getInt(R.styleable.SingleDateAndTimePicker_picker_visibleItemCount, VISIBLE_ITEM_COUNT_DEFAULT));
        daysPicker.setDayCount(a.getInt(R.styleable.SingleDateAndTimePicker_picker_dayCount, DAYS_PADDING));
        setDisplayDays(a.getBoolean(R.styleable.SingleDateAndTimePicker_picker_displayDays, displayDays));
        setDisplayMonths(a.getBoolean(R.styleable.SingleDateAndTimePicker_picker_displayMonth, displayMonth));
        setDisplayYears(a.getBoolean(R.styleable.SingleDateAndTimePicker_picker_displayYears, displayYears));
        setDisplayDaysOfMonth(a.getBoolean(R.styleable.SingleDateAndTimePicker_picker_displayDaysOfMonth, displayDaysOfMonth));
        setDisplayMonthNumbers(a.getBoolean(R.styleable.SingleDateAndTimePicker_picker_displayMonthNumbers, monthPicker.displayMonthNumbers()));
        setFontToAllPickers(a.getResourceId(R.styleable.SingleDateAndTimePicker_fontFamily, 0));
        setFontToAllPickers(a.getResourceId(R.styleable.SingleDateAndTimePicker_android_fontFamily, 0));
        String monthFormat = a.getString(R.styleable.SingleDateAndTimePicker_picker_monthFormat);
        setMonthFormat(TextUtils.isEmpty(monthFormat) ? WheelMonthPicker.MONTH_FORMAT : monthFormat);
        setTextAlign(a.getInt(R.styleable.SingleDateAndTimePicker_picker_textAlign, ALIGN_CENTER));

        checkSettings();
        setMinYear();

        a.recycle();
        if (displayDaysOfMonth) {
            Calendar now = Calendar.getInstance();
            now.setTimeZone(dateHelper.getTimeZone());
            updateDaysOfMonth(now);
        }
        daysPicker.updateAdapter(); // For MustBeFuture and dayCount
    }

    public interface OnDateChangedListener {
        void onDateChanged(String displayed, Date date);
    }
}
