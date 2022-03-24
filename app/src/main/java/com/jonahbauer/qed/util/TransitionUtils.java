package com.jonahbauer.qed.util;

import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.core.app.SharedElementCallback;
import androidx.core.view.OneShotPreDrawListener;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.transition.Transition;
import androidx.transition.TransitionListenerAdapter;
import com.google.android.material.transition.MaterialContainerTransform;
import com.google.android.material.transition.MaterialElevationScale;
import com.google.android.material.transition.MaterialFadeThrough;
import com.jonahbauer.qed.R;
import com.jonahbauer.qed.activities.MainActivity;
import com.jonahbauer.qed.layoutStuff.transition.ActionBarAnimation;

import java.util.List;
import java.util.Map;

import lombok.experimental.UtilityClass;

@UtilityClass
public class TransitionUtils {

    /*
     * Default Transitions
     */

    public static void setupDefaultTransitions(Fragment fragment) {
        setupDefaultTransitions(fragment, Colors.getPrimaryColor(fragment.requireContext()));
    }

    public static void setupDefaultTransitions(Fragment fragment, @ColorInt int actionBarColor) {
        setupDefaultTransitions(fragment, getTransitionDuration(fragment), actionBarColor);
    }

    public static void setupDefaultTransitions(Fragment fragment, int duration, @ColorInt int actionBarColor) {
        setupDefaultEnterTransition(fragment, duration, actionBarColor);
        setupDefaultReenterTransition(fragment, duration, actionBarColor);
        setupDefaultExitTransition(fragment, duration);
        setupDefaultReturnTransition(fragment, duration);
        fragment.getLifecycle().addObserver(new DefaultLifecycleObserver() {
            @Override
            public void onResume(@NonNull LifecycleOwner owner) {
                ((MainActivity) fragment.requireActivity()).setActionBarColor(actionBarColor);
            }
        });
    }

    public static void setupDefaultEnterTransition(Fragment fragment, int duration, @ColorInt int actionBarColor) {
        var transition = new MaterialFadeThrough();
        transition.addListener(new ActionBarAnimation(fragment, actionBarColor));
        transition.setDuration(duration);
        fragment.setEnterTransition(transition);
    }

    public static void setupDefaultReenterTransition(Fragment fragment, int duration, @ColorInt int actionBarColor) {
        var transition = new MaterialFadeThrough();
        transition.addListener(new ActionBarAnimation(fragment, actionBarColor));
        transition.setDuration(duration);
        fragment.setReenterTransition(transition);
    }

    public static void setupDefaultExitTransition(@NonNull Fragment fragment, int duration) {
        var transition = new MaterialFadeThrough();
        transition.setDuration(duration);
        fragment.setExitTransition(transition);
    }

    public static void setupDefaultReturnTransition(@NonNull Fragment fragment, int duration) {
        var transition = new MaterialFadeThrough();
        transition.setDuration(duration);
        fragment.setReturnTransition(transition);
    }

    /*
     * Shared Element Transitions
     */

    public static void setupReenterElevationScale(@NonNull Fragment fragment) {
        setupReenterElevationScale(fragment, getTransitionDuration(fragment), Colors.getPrimaryColor(fragment.requireContext()));
    }

    public static void setupReenterElevationScale(@NonNull Fragment fragment, int duration, @ColorInt int actionBarColor) {
        var reenterTransition = new MaterialElevationScale(true);
        reenterTransition.addListener(new ActionBarAnimation(fragment, actionBarColor));
        reenterTransition.setDuration(duration);
        reenterTransition.addListener(new TransitionListenerAdapter() {
            @Override
            public void onTransitionEnd(@NonNull Transition transition) {
                setupDefaultReenterTransition(fragment, duration, actionBarColor);
            }
        });
        fragment.setReenterTransition(reenterTransition);

        var exitTransition = new MaterialElevationScale(false);
        exitTransition.setDuration(duration);
        exitTransition.addListener(new TransitionListenerAdapter() {
            @Override
            public void onTransitionEnd(@NonNull Transition transition) {
                setupDefaultExitTransition(fragment, duration);
            }
        });
        fragment.setExitTransition(exitTransition);
    }

    public static void setupEnterContainerTransform(@NonNull Fragment fragment, @ColorInt int actionBarColor) {
        // since the non-shared element transition might not have any targets anymore we need
        // to add an ActionBarAnimation to the shared element transition ...
        var sharedEnterTransition = new MaterialContainerTransform(fragment.requireContext(), true);
        sharedEnterTransition.setFadeMode(MaterialContainerTransform.FADE_MODE_CROSS);
        sharedEnterTransition.addListener(new ActionBarAnimation(fragment, actionBarColor));
        sharedEnterTransition.setDrawingViewId(R.id.nav_host);
        fragment.setSharedElementEnterTransition(sharedEnterTransition);

        // ... and remove it from the non-shared element transition
        var enterTransition = new MaterialFadeThrough();
        fragment.setEnterTransition(enterTransition);

        fragment.setEnterSharedElementCallback(new SharedElementCallback() {
            @Override
            public void onMapSharedElements(List<String> names, Map<String, View> sharedElements) {
                super.onMapSharedElements(names, sharedElements);
                if (sharedElements.size() == 0) {
                    // shared element transition will not run, so we have to add the action bar
                    // animation to the enter transition again, but only once
                    enterTransition.addListener(new ActionBarAnimation(fragment, actionBarColor) {
                        @Override
                        public void onTransitionCancel(@NonNull Transition transition) {
                            super.onTransitionCancel(transition);
                            enterTransition.removeListener(this);
                        }

                        @Override
                        public void onTransitionEnd(@NonNull Transition transition) {
                            super.onTransitionEnd(transition);
                            enterTransition.removeListener(this);
                        }
                    });
                }
            }
        });

        var sharedReturnTransition = new MaterialContainerTransform(fragment.requireContext(), true);
        sharedReturnTransition.setFadeMode(MaterialContainerTransform.FADE_MODE_CROSS);
        sharedReturnTransition.setDrawingViewId(R.id.nav_host);
        fragment.setSharedElementReturnTransition(sharedReturnTransition);
    }

    public static void postponeEnterAnimationToPreDraw(@NonNull Fragment fragment, @NonNull View view) {
        fragment.postponeEnterTransition();
        OneShotPreDrawListener.add(((ViewGroup) view.getParent()), fragment::startPostponedEnterTransition);
    }

    public static int getTransitionDuration(@NonNull Fragment fragment) {
        TypedValue value = new TypedValue();
        fragment.requireContext().getTheme().resolveAttribute(R.attr.motionDurationLong1, value, true);
        return value.data;
    }
}
