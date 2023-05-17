package eu.jonahbauer.qed.activities.imageActivity;

import android.content.Intent;
import android.os.Bundle;
import android.transition.Fade;
import android.transition.Visibility;
import android.view.View;
import android.view.Window;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.SharedElementCallback;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.navigation.Navigation;

import eu.jonahbauer.qed.R;
import eu.jonahbauer.qed.databinding.ActivityImageBinding;
import eu.jonahbauer.qed.util.TransitionUtils;

import java.util.List;
import java.util.Map;

public class ImageActivity extends AppCompatActivity {
    public static final int RESULT_OK = 2;
    public static final String RESULT_EXTRA_IMAGE_ID = "image_id";

    private ActivityImageBinding mBinding;

    private WindowInsetsControllerCompat mWindowInsetsController;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        var window = getWindow();
        window.requestFeature(Window.FEATURE_ACTIVITY_TRANSITIONS);
        window.setEnterTransition(new Fade(Visibility.MODE_IN));
        window.setExitTransition(new Fade(Visibility.MODE_OUT));
        TransitionUtils.postponeEnterTransition(this, 500);

        setEnterSharedElementCallback(new SharedElementCallback() {
            @Override
            public void onMapSharedElements(List<String> names, Map<String, View> sharedElements) {
                super.onMapSharedElements(names, sharedElements);

                var imageTransitionName = getString(R.string.transition_name_image_fragment_image);
                if (!names.contains(imageTransitionName)) return;

                var target = mBinding.getRoot().findViewById(R.id.gallery_image);
                if (target == null) return;

                target.setTransitionName(imageTransitionName);
                sharedElements.put(imageTransitionName, target);
            }
        });

        mBinding = ActivityImageBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());

        var navController = Navigation.findNavController(this, R.id.nav_host);
        navController.setGraph(R.navigation.image, getIntent().getExtras());

        mWindowInsetsController = new WindowInsetsControllerCompat(getWindow(), mBinding.getRoot());
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
    }

    public WindowInsetsControllerCompat getWindowInsetsController() {
        return mWindowInsetsController;
    }

    public void setResultImageId(long id) {
        var intent = new Intent();
        intent.putExtra(ImageActivity.RESULT_EXTRA_IMAGE_ID, id);
        setResult(ImageActivity.RESULT_OK, intent);
    }
}
