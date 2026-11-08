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
    @Test public void multiSelectionUsesGalleryImportModeInsteadOfFilesOrSinglePick(){
        Intent[] choices=PlannerActivity.photoPickers(true,true);
        assertEquals("com.miui.gallery",choices[0].getPackage());
        for(Intent gallery:choices){
            assertEquals(Intent.ACTION_GET_CONTENT,gallery.getAction());
            assertEquals("image/*",gallery.getType());
            assertTrue(gallery.getBooleanExtra(Intent.EXTRA_ALLOW_MULTIPLE,false));
            assertTrue(gallery.getBooleanExtra("pick-multiple",false));
            assertTrue(gallery.getIntExtra("pick-upper-bound",1)>1);
            assertNull(gallery.getData());
            assertArrayEquals(new String[]{"image/*","video/*"},gallery.getStringArrayExtra(Intent.EXTRA_MIME_TYPES));
        }
        assertNull(choices[choices.length-1].getPackage());
    }
    @Test public void singleSelectionKeepsOnePhotoMode(){
        for(Intent gallery:PlannerActivity.photoPickers(false,false)){
            assertEquals(Intent.ACTION_GET_CONTENT,gallery.getAction());
            assertFalse(gallery.getBooleanExtra(Intent.EXTRA_ALLOW_MULTIPLE,true));
            assertEquals(1,gallery.getIntExtra("pick-upper-bound",0));
            assertNull(gallery.getStringArrayExtra(Intent.EXTRA_MIME_TYPES));
        }
    }
}
