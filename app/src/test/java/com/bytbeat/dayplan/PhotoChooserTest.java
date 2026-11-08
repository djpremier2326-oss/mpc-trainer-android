package com.bytbeat.dayplan;

import android.app.Application;
import android.content.Intent;
import android.os.Parcelable;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk={28,35},application=Application.class)
public class PhotoChooserTest {
    @Test public void multiSelectionLaunchesDocumentsDirectlyWithoutSingleSelectGallery(){
        Intent[] choices=PlannerActivity.photoPickers(true,true);
        assertEquals(1,choices.length);
        Intent documents=choices[0];
        assertEquals(Intent.ACTION_OPEN_DOCUMENT,documents.getAction());
        assertTrue(documents.hasCategory(Intent.CATEGORY_OPENABLE));
        assertTrue(documents.getBooleanExtra(Intent.EXTRA_ALLOW_MULTIPLE,false));
        assertArrayEquals(new String[]{"image/*","video/*"},documents.getStringArrayExtra(Intent.EXTRA_MIME_TYPES));
        assertNull(documents.getData()); // No restricted media collection or gallery package.
        assertNull(documents.getPackage());
    }
    @Test public void calendarMultiSelectionAlsoUsesDocumentsDirectly(){
        Intent[] choices=PlannerActivity.photoPickers(false,true);
        assertEquals(1,choices.length);
        assertEquals(Intent.ACTION_OPEN_DOCUMENT,choices[0].getAction());
        assertEquals("image/*",choices[0].getType());
        assertTrue(choices[0].getBooleanExtra(Intent.EXTRA_ALLOW_MULTIPLE,false));
    }
    @Test public void photoOnlySingleSelectionRetainsImageFilter(){
        Intent documents=PlannerActivity.photoPickers(false,false)[1];
        assertEquals("image/*",documents.getType());
        assertFalse(documents.getBooleanExtra(Intent.EXTRA_ALLOW_MULTIPLE,true));
    }
}
