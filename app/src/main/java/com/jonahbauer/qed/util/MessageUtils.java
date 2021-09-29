package com.jonahbauer.qed.util;

import android.app.Activity;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;

import com.jonahbauer.qed.R;
import com.jonahbauer.qed.activities.MainActivity;
import com.jonahbauer.qed.activities.mainFragments.QEDFragment;
import com.jonahbauer.qed.model.Message;
import com.jonahbauer.qed.model.adapter.MessageAdapter;

import java.util.Locale;

import lombok.experimental.UtilityClass;

@UtilityClass
public class MessageUtils {
    private static final String COPY_FORMAT = "%3$tm-%3$td %3$tH:%3$tM:%3$tS\t%4$s\t%1$s:\t%2$s";

    /**
     * Sets the checked item in the list and shows an appropriate toolbar.
     *
     * @param position the position of the checked item in the {@link MessageAdapter}
     * @param value if the item is checked or not
     */
    public static void setChecked(@NonNull QEDFragment fragment,
                                  @NonNull ListView listView,
                                  @NonNull MessageAdapter adapter,
                                  int position, boolean value) {
        listView.setItemChecked(position, value);

        Activity activity = fragment.getActivity();
        if (activity instanceof MainActivity) {
            MainActivity mainActivity = (MainActivity) activity;

            if (value) {
                Message msg = adapter.getItem(position);
                if (msg == null) return;

                Toolbar toolbar = mainActivity.borrowAltToolbar();
                toolbar.setNavigationOnClickListener(v -> setChecked(fragment, listView, adapter, position, false));

                toolbar.inflateMenu(R.menu.menu_message);
                toolbar.setOnMenuItemClickListener(item -> {
                    if (item.getItemId() == R.id.message_info) {
                        Actions.showInfoSheet(fragment, msg);
                    } else if (item.getItemId() == R.id.message_copy) {
                        Actions.copy(fragment.requireContext(), fragment.requireView(), msg.getName(), MessageUtils.copyFormat(msg));
                    }

                    return false;
                });

                toolbar.setTitle(msg.getName().trim().isEmpty() ? activity.getText(R.string.message_name_anonymous) : msg.getName());
            } else {
                mainActivity.returnAltToolbar();
            }
        }
    }

    /**
     * Returns a string that mimics what one would get when trying to copy a message on the
     * browser chat client.
     */
    public static String copyFormat(Message message) {
        return String.format(
                Locale.GERMANY,
                COPY_FORMAT,
                message.getName(),
                message.getMessage(),
                message.getDate(),
                message.getUserName() != null ? "✓" : ""
        );
    }
}
