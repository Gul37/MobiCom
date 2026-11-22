package kz.mobicom.app;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.DatePicker;
import android.widget.RadioGroup;
import android.widget.TimePicker;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.Locale;

public class ReminderDialogFragment extends DialogFragment {

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_reminder, null);

        RadioGroup typeGroup = view.findViewById(R.id.reminderType);
        DatePicker date = view.findViewById(R.id.reminderDate);
        TimePicker time = view.findViewById(R.id.reminderTime);
        ToggleButton enabled = view.findViewById(R.id.reminderEnabled);

        time.setIs24HourView(true);

        // Диалог түріндегі таңдау құралдары (жоғарыдағы виджеттерді жаңартады)
        view.findViewById(R.id.pickDate).setOnClickListener(v ->
                new DatePickerDialog(requireContext(), (picker, y, m, d) -> date.updateDate(y, m, d),
                        date.getYear(), date.getMonth(), date.getDayOfMonth()).show());
        view.findViewById(R.id.pickTime).setOnClickListener(v ->
                new TimePickerDialog(requireContext(), (picker, h, min) -> {
                    time.setHour(h);
                    time.setMinute(min);
                }, time.getHour(), time.getMinute(), true).show());

        return new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.reminder_title)
                .setView(view)
                .setPositiveButton(R.string.action_save, (d, w) -> {
                    if (!enabled.isChecked()) {
                        Prefs.setReminder(requireContext(), "");
                        Toast.makeText(requireContext(), R.string.reminder_off, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    String[] types = getResources().getStringArray(R.array.reminder_types);
                    int index = typeGroup.getCheckedRadioButtonId() == R.id.typeTariff ? 1 : 0;
                    String text = String.format(Locale.US, "%s: %02d.%02d.%d %02d:%02d", types[index],
                            date.getDayOfMonth(), date.getMonth() + 1, date.getYear(),
                            time.getHour(), time.getMinute());
                    Prefs.setReminder(requireContext(), text);
                    Toast.makeText(requireContext(), getString(R.string.reminder_saved, text),
                            Toast.LENGTH_LONG).show();
                })
                .setNegativeButton(R.string.action_cancel, null)
                .create();
    }
}
