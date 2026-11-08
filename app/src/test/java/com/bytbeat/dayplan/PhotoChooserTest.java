package com.bytbeat.dayplan;

import android.app.Application;
import android.app.Activity;
import android.net.Uri;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import java.util.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk={28,35},application=Application.class)
public class PhotoChooserTest {
    @Test public void allRealFoldersAreShownInsteadOfThreeSystemCategories(){
        List<AlbumPickerActivity.Photo> photos=new ArrayList<>();String[] names={"Камера","Telegram","WhatsApp","Моя папка","Скриншоты"};
        for(int i=0;i<names.length;i++)photos.add(new AlbumPickerActivity.Photo(Uri.parse("content://media/external/images/media/"+i),"Pictures/"+names[i],names[i],i));
        photos.add(new AlbumPickerActivity.Photo(Uri.parse("content://media/external/images/media/10"),"Pictures/Telegram","Telegram",10));
        List<AlbumPickerActivity.Album> albums=AlbumPickerActivity.albums(photos);assertEquals(5,albums.size());assertEquals("Telegram",albums.get(0).name);assertEquals(2,albums.get(0).photos.size());
    }
    @Test public void severalSelectedPhotosAreReturnedTogetherWithoutDuplicates(){
        LinkedHashSet<Uri> selected=new LinkedHashSet<>();for(int i=0;i<4;i++)selected.add(Uri.parse("content://media/external/images/media/"+i));
        assertArrayEquals(selected.toArray(new Uri[0]),PlannerActivity.photoUris(Activity.RESULT_OK,AlbumPickerActivity.result(selected)));
        assertNull(PlannerActivity.photoUris(Activity.RESULT_CANCELED,AlbumPickerActivity.result(selected)));
    }
    @Test public void foldersWithTheSameNameKeepTheirDifferentPaths(){
        Uri uri=Uri.parse("content://media/external/images/media/1");
        List<AlbumPickerActivity.Photo> photos=Arrays.asList(new AlbumPickerActivity.Photo(uri,"Pictures/Camera","Камера",1),new AlbumPickerActivity.Photo(uri,"DCIM/Camera","Камера",2));
        assertEquals(2,AlbumPickerActivity.albums(photos).size());
    }
    @Test public void tappingSeveralPhotosInOneAlbumReturnsThemTogether()throws Exception{
        android.content.Intent launch=new android.content.Intent().putExtra("multiple",true);
        var controller=org.robolectric.Robolectric.buildActivity(AlbumPickerActivity.class,launch).create();
        AlbumPickerActivity activity=controller.get();
        try{
            List<AlbumPickerActivity.Photo> photos=Arrays.asList(new AlbumPickerActivity.Photo(Uri.parse("content://media/external/images/media/1"),"DCIM/Camera","Камера",1),new AlbumPickerActivity.Photo(Uri.parse("content://media/external/images/media/2"),"DCIM/Camera","Камера",2));
            var method=AlbumPickerActivity.class.getDeclaredMethod("showPhotos",AlbumPickerActivity.Album.class);method.setAccessible(true);method.invoke(activity,AlbumPickerActivity.albums(photos).get(0));
            android.view.ViewGroup root=(android.view.ViewGroup)activity.findViewById(android.R.id.content);
            android.widget.GridView grid=findGrid(root);assertNotNull(grid);
            grid.performItemClick(null,0,0);grid.performItemClick(null,1,1);
            var field=AlbumPickerActivity.class.getDeclaredField("done");field.setAccessible(true);((android.widget.Button)field.get(activity)).performClick();
            var shadow=org.robolectric.Shadows.shadowOf(activity);
            assertEquals(Activity.RESULT_OK,shadow.getResultCode());assertEquals(2,PlannerActivity.photoUris(shadow.getResultCode(),shadow.getResultIntent()).length);
        }finally{controller.destroy();}
    }
    private static android.widget.GridView findGrid(android.view.View view){
        if(view instanceof android.widget.GridView)return (android.widget.GridView)view;
        if(view instanceof android.view.ViewGroup){var group=(android.view.ViewGroup)view;for(int i=0;i<group.getChildCount();i++){var found=findGrid(group.getChildAt(i));if(found!=null)return found;}}
        return null;
    }

}
