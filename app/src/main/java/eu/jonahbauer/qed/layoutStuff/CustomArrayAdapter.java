package eu.jonahbauer.qed.layoutStuff;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;

@SuppressWarnings("unused")
public class CustomArrayAdapter<T> extends ArrayAdapter<T> {
    @IdRes
    private final int mTextViewResourceId;

    private Function<T, CharSequence> mToString = Objects::toString;

    public CustomArrayAdapter(@NonNull Context context, int resource) {
        super(context, resource);
        mTextViewResourceId = 0;
    }

    public CustomArrayAdapter(@NonNull Context context, int resource, int textViewResourceId) {
        super(context, resource, textViewResourceId);
        mTextViewResourceId = textViewResourceId;
    }

    public CustomArrayAdapter(@NonNull Context context, int resource, @NonNull T[] objects) {
        super(context, resource, objects);
        mTextViewResourceId = 0;
    }

    public CustomArrayAdapter(@NonNull Context context, int resource, int textViewResourceId, @NonNull T[] objects) {
        super(context, resource, textViewResourceId, objects);
        mTextViewResourceId = textViewResourceId;
    }

    public CustomArrayAdapter(@NonNull Context context, int resource, @NonNull List<T> objects) {
        super(context, resource, objects);
        mTextViewResourceId = 0;
    }

    public CustomArrayAdapter(@NonNull Context context, int resource, int textViewResourceId, @NonNull List<T> objects) {
        super(context, resource, textViewResourceId, objects);
        mTextViewResourceId = textViewResourceId;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View view = super.getView(position, convertView, parent);
        setText(view, position);
        return view;
    }

    @Override
    public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View view = super.getDropDownView(position, convertView, parent);
        setText(view, position);
        return view;
    }

    private void setText(View view, int position) {
        TextView text;

        try {
            if (mTextViewResourceId == 0) {
                //  If no custom field is assigned, assume the whole resource is a TextView
                text = (TextView) view;
            } else {
                //  Otherwise, find the TextView field within the layout
                text = view.findViewById(mTextViewResourceId);

                if (text == null) {
                    throw new RuntimeException("Failed to find view with ID "
                            + getContext().getResources().getResourceName(mTextViewResourceId)
                            + " in item layout");
                }
            }
        } catch (ClassCastException e) {
            Log.e("CustomArrayAdapter", "You must supply a resource ID for a TextView");
            throw new IllegalStateException(
                    "ArrayAdapter requires the resource ID to be a TextView", e);
        }

        final T item = getItem(position);
        text.setText(mToString.apply(item));
    }

    public void setToString(Function<T, CharSequence> toString) {
        mToString = toString;
    }
}
