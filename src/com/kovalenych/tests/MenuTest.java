package com.kovalenych.tests;

import android.test.ActivityInstrumentationTestCase2;
import android.view.View;
import android.widget.Button;
import com.jayway.android.robotium.solo.Solo;
import com.kovalenych.MenuActivity;

import java.util.List;

/**
 * this class was made
 * by insomniac and angryded
 * for their purposes
 */
public class MenuTest extends
        GeneralTest {


    public void testMenu() throws Exception {

        solo.clickOnText("ВИДЕО");
        solo.sleep(1000);

        solo.clickOnText("СТАТЬИ");
        solo.sleep(1000);

        solo.clickOnText("РЕКОРДЫ");
        solo.sleep(1000);

        solo.clickOnText("ТАБЛИЦЫ");
        solo.sleep(1000);
    }
}