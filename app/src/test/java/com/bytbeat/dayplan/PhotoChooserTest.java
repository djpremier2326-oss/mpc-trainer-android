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
    @Test public void multiSelectionUsesDocumentsAndOffersTheNormalGallery(){
        Intent[] choices=PlannerActivity.photoPickers(true,true);
        assertEquals(Intent.ACTION_CHOOSER,choices[0].getAction());
        Intent documents=choices[0].getParcelableExtra(Intent.EXTRA_INTENT);
        assertEquals(Intent.ACTION_OPEN_DOCUMENT,documents.getAction());
        assertTrue(documents.hasCategory(Intent.CATEGORY_OPENABLE));
        assertTrue(documents.getBooleanExtra(Intent.EXTRA_ALLOW_MULTIPLE,false));
        assertArrayEquals(new String[]{"image/*","video/*"},documents.getStringArrayExtra(Intent.EXTRA_MIME_TYPES));
        Parcelable[] alternatives=choices[0].getParcelableArrayExtra(Intent.EXTRA_INITIAL_INTENTS);
        assertEquals(Intent.ACTION_PICK,((Intent)alternatives[0]).getAction());
        for(int i=1;i<choices.length;i++){
            assertTrue(choices[i].getBooleanExtra(Intent.EXTRA_ALLOW_MULTIPLE,false));
            assertNotEquals(android.provider.MediaStore.ACTION_PICK_IMAGES,choices[i].getAction());
        }
    }
    @Test public void photoOnlySingleSelectionRetainsImageFilter(){
        Intent documents=PlannerActivity.photoPickers(false,false)[1];
        assertEquals("image/*",documents.getType());
        assertFalse(documents.getBooleanExtra(Intent.EXTRA_ALLOW_MULTIPLE,true));
    }
}
