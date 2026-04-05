package com.example.btth1;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import android.Manifest;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class MainActivityPermissionUiStateTest {

    @Rule
    public ActivityScenarioRule<MainActivity> activityRule = new ActivityScenarioRule<>(MainActivity.class);

    @Test
    public void initialUi_stateIsVisible() {
        onView(withId(R.id.tvStatus)).check(matches(isDisplayed()));
        onView(withId(R.id.btnDiscover)).check(matches(isDisplayed()));
        onView(withId(R.id.btnTalk)).check(matches(isDisplayed()));
    }

    @Ignore("Skeleton: Wi-Fi Direct broadcast may update status immediately on some devices")
    @Test
    public void initialUi_defaultStatusIsDisconnected() {
        onView(withId(R.id.tvStatus)).check(matches(withText(R.string.status_idle)));
    }

    @Ignore("Skeleton: requires controlled permission revoke flow on emulator/device")
    @Test
    public void missingPermission_showsRequiredPermissionMessage() {
        // TODO: Revoke runtime permissions with UiAutomation/AppOps then verify UI text.
        org.junit.Assert.assertNotNull(Manifest.permission.RECORD_AUDIO);
        org.junit.Assert.assertNotNull(Manifest.permission.ACCESS_FINE_LOCATION);
        org.junit.Assert.assertNotNull(Manifest.permission.NEARBY_WIFI_DEVICES);
    }

    @Ignore("Skeleton: requires 2-device Wi-Fi Direct environment")
    @Test
    public void connectionTimeout_showsTimeoutStatus() {
        // TODO: Simulate connection attempt without peer response and assert timeout status.
        org.junit.Assert.assertTrue(true);
    }
}




