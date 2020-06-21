/*
 * Copyright (C) github/laenger
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/*
 * Upgraded to androidx.viewpager2.widget.ViewPager2
 */
package com.jonahbauer.qed.layoutStuff.viewPagerSideSheet;

import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.viewpager2.widget.ViewPager2;

@SuppressWarnings("ALL")
public final class SideSheetUtils {

    public static void setupViewPager(ViewPager2 viewPager) {
        final View bottomSheetParent = findSideSheetParent(viewPager);
        if (bottomSheetParent != null) {
            viewPager.registerOnPageChangeCallback(new SideSheetViewPagerListener(viewPager, bottomSheetParent));
        }
    }

    private static class SideSheetViewPagerListener extends ViewPager2.OnPageChangeCallback {
        private final ViewPager2 viewPager;
        private final ViewPagerSideSheetBehavior<View> behavior;

        private SideSheetViewPagerListener(ViewPager2 viewPager, View bottomSheetParent) {
            this.viewPager = viewPager;
            this.behavior = ViewPagerSideSheetBehavior.from(bottomSheetParent);
        }

        @Override
        public void onPageSelected(int position) {
            viewPager.post(behavior::invalidateScrollingChild);
        }
    }

    private static View findSideSheetParent(final View view) {
        View current = view;
        while (current != null) {
            final ViewGroup.LayoutParams params = current.getLayoutParams();
            if (params instanceof CoordinatorLayout.LayoutParams && ((CoordinatorLayout.LayoutParams) params).getBehavior() instanceof ViewPagerSideSheetBehavior) {
                return current;
            }
            final ViewParent parent = current.getParent();
            current = parent == null || !(parent instanceof View) ? null : (View) parent;
        }
        return null;
    }

}
