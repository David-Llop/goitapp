package de.christinecoenen.code.zapp.app;


import android.content.pm.ActivityInfo;
import android.support.test.espresso.contrib.DrawerActions;
import android.support.test.espresso.contrib.DrawerMatchers;
import android.support.test.espresso.contrib.NavigationViewActions;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import de.christinecoenen.code.zapp.R;

import static android.support.test.InstrumentationRegistry.getInstrumentation;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu;
import static android.support.test.espresso.Espresso.pressBack;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.closeSoftKeyboard;
import static android.support.test.espresso.action.ViewActions.replaceText;
import static android.support.test.espresso.action.ViewActions.swipeLeft;
import static android.support.test.espresso.action.ViewActions.swipeRight;
import static android.support.test.espresso.action.ViewActions.swipeUp;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;

@RunWith(AndroidJUnit4.class)
public class MainUiTest {

	@Rule
	public final ActivityTestRule<MainActivity> activityTestRule = new ActivityTestRule<>(MainActivity.class);

	@BeforeClass
	public static void enableAccessibilityChecks() {
		// TODO: re-enable as soon as 3.0.2 is released
		//AccessibilityChecks.enable()
		//	.setRunChecksFromRootView(true)
		//	.setThrowExceptionForErrors(false);
	}

	/**
	 * Basic ui test that calls every screen and asserts nothing
	 * is crashing very badly. Run this when you are unsure if your
	 * changes broke something.
	 * <p>
	 * Also, this test will output acessibility errors to logcat.
	 */
	@Test
	public void mainUiTest() throws InterruptedException {
		// open menu
		onView(withId(R.id.layout_drawer))
			.perform(DrawerActions.open())
			.check(matches(DrawerMatchers.isOpen()));

		// change to mediathek
		onView(withId(R.id.nav_view))
			.perform(NavigationViewActions.navigateTo(R.id.menu_mediathek));

		// scroll down and select a show
		onView(withId(R.id.list))
			.perform(swipeUp())
			.perform(swipeUp())
			.perform(actionOnItemAtPosition(19, click()));

		// press play in detail view
		onView(withId(R.id.btn_play))
			.check(matches(isDisplayed()))
			.perform(click());

		// play pause button on video view
		onView(withId(R.id.exo_pause))
			.check(matches(isDisplayed()))
			.perform(click());

		onView(withId(R.id.exo_play))
			.check(matches(isDisplayed()))
			.perform(click());

		// go back to mediathek
		pressBack();
		pressBack();

		// change back to portrait
		activityTestRule.getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		Thread.sleep(500);

		// try to refresh
		openActionBarOverflowOrOptionsMenu(getInstrumentation().getTargetContext());
		Thread.sleep(200);
		onView(withText(R.string.menu_refresh))
			.check(matches(isDisplayed()))
			.perform(click());

		// search for shows
		onView(withId(R.id.search_src_text))
			.check(matches(isDisplayed()))
			.perform(replaceText("test"), closeSoftKeyboard());

		// cancel search
		onView(withId(R.id.search_close_btn))
			.check(matches(isDisplayed()))
			.perform(click());

		// open menu
		onView(withId(R.id.layout_drawer))
			.perform(DrawerActions.open())
			.check(matches(DrawerMatchers.isOpen()));

		// go to about screen
		onView(withId(R.id.nav_view))
			.perform(NavigationViewActions.navigateTo(R.id.menu_about));
		pressBack();

		// open menu
		onView(withId(R.id.layout_drawer))
			.perform(DrawerActions.open())
			.check(matches(DrawerMatchers.isOpen()));

		// go to FAQ screen
		onView(withId(R.id.nav_view))
			.perform(NavigationViewActions.navigateTo(R.id.menu_about));

		// go to changelog
		onView(withText(R.string.activity_changelog_title))
			.check(matches(isDisplayed()))
			.perform(click());
		pressBack();

		// back to mediathek
		pressBack();
		Thread.sleep(200);

		// change to live tab
		onView(withId(R.id.list))
			.perform(swipeRight());

		// select a channel
		onView(withId(R.id.gridview_channels))
			.perform(actionOnItemAtPosition(0, click()));

		onView(withId(R.id.fullscreen_content))
			.perform(swipeLeft())
			.perform(swipeLeft());

		// back live tab
		pressBack();

		// open menu
		onView(withId(R.id.layout_drawer))
			.perform(DrawerActions.open())
			.check(matches(DrawerMatchers.isOpen()));

		// go to FAQ screen
		onView(withId(R.id.nav_view))
			.perform(NavigationViewActions.navigateTo(R.id.menu_settings));

		// open channel selection
		onView(withText(R.string.pref_channel_selection_title))
			.perform(click());
	}
}
