package com.bytbeat.dayplan;

import android.Manifest;
import android.app.Activity;
import android.content.*;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.*;
import android.provider.MediaStore;
import android.util.LruCache;
import android.util.Size;
import android.view.*;
import android.widget.*;
import java.util.*;
import java.util.concurrent.*;

/** Lists real MediaStore folders rather than the system Photo Picker's curated categories. */
public class AlbumPickerActivity extends Activity {
    static final class Photo {
        final Uri uri; final String folder,name; final long added;
        Photo(Uri uri,String folder,String name,long added){this.uri=uri;this.folder=folder;this.name=name;this.added=added;}
    }
    static final class Album {
        final String name; final ArrayList<Photo> photos=new ArrayList<>();
        Album(String name){this.name=name;}
    }
    static List<Album> albums(List<Photo> photos){
        LinkedHashMap<String,Album> groups=new LinkedHashMap<>();
        ArrayList<Photo> sorted=new ArrayList<>(photos);sorted.sort((a,b)->Long.compare(b.added,a.added));
        for(Photo photo:sorted)groups.computeIfAbsent(photo.folder,key->new Album(photo.name)).photos.add(photo);
        return new ArrayList<>(groups.values());
    }
    static Intent result(Collection<Uri> selected){
        Intent result=new Intent();ClipData clips=null;
        for(Uri uri:selected){if(clips==null){clips=ClipData.newRawUri("Фото",uri);result.setData(uri);}else clips.addItem(new ClipData.Item(uri));}
        if(clips!=null)result.setClipData(clips);
        return result.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
    }
    private final ExecutorService worker=Executors.newFixedThreadPool(3);
    private final LruCache<String,Bitmap> cache=new LruCache<String,Bitmap>(24*1024*1024){@Override protected int sizeOf(String key,Bitmap value){return value.getByteCount();}};
    private final LinkedHashSet<Uri> selected=new LinkedHashSet<>();
    private List<Album> allAlbums=new ArrayList<>();
    private Album opened;
    private LinearLayout root,header;
    private FrameLayout content;
    private TextView title;
    private Button done;
    private boolean withVideo,multiple,settingsOpened;
    private int generation;
    private static final int PERMISSIONS=420;
    private int dp(int n){return (int)(getResources().getDisplayMetrics().density*n+.5f);}
    private boolean granted(String permission){return checkSelfPermission(permission)==PackageManager.PERMISSION_GRANTED;}
    private boolean imagesAllowed(){return Build.VERSION.SDK_INT>=33?granted(Manifest.permission.READ_MEDIA_IMAGES):granted(Manifest.permission.READ_EXTERNAL_STORAGE);}
    private boolean videosAllowed(){return Build.VERSION.SDK_INT>=33?granted(Manifest.permission.READ_MEDIA_VIDEO):imagesAllowed();}
    private boolean partialAllowed(){return Build.VERSION.SDK_INT>=34&&granted(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED);}
    private boolean fullAccess(){return imagesAllowed()&&(!withVideo||videosAllowed());}
    @Override public void onCreate(Bundle state){
        super.onCreate(state);getWindow().setStatusBarColor(0xff202124);getWindow().setNavigationBarColor(0xff202124);
        withVideo=getIntent().getBooleanExtra("video",false);multiple=getIntent().getBooleanExtra("multiple",true);
        root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(0xff202124);
        header=new LinearLayout(this);header.setGravity(Gravity.CENTER_VERTICAL);header.setPadding(dp(8),dp(8),dp(8),dp(8));
        Button back=button("‹");back.setContentDescription("Назад");back.setOnClickListener(v->onBackPressed());header.addView(back,new LinearLayout.LayoutParams(dp(48),dp(48)));
        title=text("Альбомы",20);title.setSingleLine(true);title.setEllipsize(android.text.TextUtils.TruncateAt.END);header.addView(title,new LinearLayout.LayoutParams(0,dp(48),1));
        done=button("Добавить");done.setOnClickListener(v->{if(!selected.isEmpty()){setResult(RESULT_OK,result(selected));finish();}});header.addView(done);root.addView(header);
        content=new FrameLayout(this);root.addView(content,new LinearLayout.LayoutParams(-1,0,1));setContentView(root);updateCount();
        if(fullAccess())load();else requestMedia();
    }
    private TextView text(String label,int size){TextView view=new TextView(this);view.setText(label);view.setTextColor(Color.WHITE);view.setTextSize(size);view.setGravity(Gravity.CENTER_VERTICAL);return view;}
    private Button button(String label){Button button=new Button(this);button.setText(label);button.setTextColor(0xffeedfff);button.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xff37383b));button.setAllCaps(false);return button;}
    private void requestMedia(){
        if(Build.VERSION.SDK_INT>=34)requestPermissions(withVideo?new String[]{Manifest.permission.READ_MEDIA_IMAGES,Manifest.permission.READ_MEDIA_VIDEO,Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED}:new String[]{Manifest.permission.READ_MEDIA_IMAGES,Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED},PERMISSIONS);
        else if(Build.VERSION.SDK_INT>=33)requestPermissions(withVideo?new String[]{Manifest.permission.READ_MEDIA_IMAGES,Manifest.permission.READ_MEDIA_VIDEO}:new String[]{Manifest.permission.READ_MEDIA_IMAGES},PERMISSIONS);
        else requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},PERMISSIONS);
    }
    @Override public void onRequestPermissionsResult(int code,String[] permissions,int[] results){super.onRequestPermissionsResult(code,permissions,results);if(code==PERMISSIONS){if(imagesAllowed()||withVideo&&videosAllowed()||partialAllowed())load();else showPermission();}}
    private void showPermission(){
        content.removeAllViews();LinearLayout help=new LinearLayout(this);help.setPadding(dp(24),dp(20),dp(24),dp(20));help.setOrientation(LinearLayout.VERTICAL);
        help.addView(text("Чтобы показать альбомы телефона, разрешите доступ к фото. Для полного списка выберите «Все фото».",17));
        Button settings=button("Разрешить доступ");settings.setOnClickListener(v->openSettings());help.addView(settings);content.addView(help);
    }
    private void openSettings(){settingsOpened=true;startActivity(new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:"+getPackageName())));}
    @Override protected void onResume(){super.onResume();if(settingsOpened){settingsOpened=false;if(imagesAllowed()||withVideo&&videosAllowed()||partialAllowed())load();else showPermission();}}
    private void query(Uri collection,List<Photo> photos){
        String[] columns=Build.VERSION.SDK_INT>=29?new String[]{"_id","bucket_id","bucket_display_name","date_added","relative_path"}:new String[]{"_id","bucket_id","bucket_display_name","date_added","_data"};
        try(Cursor cursor=getContentResolver().query(collection,columns,null,null,"date_added DESC")){
            if(cursor==null)return;
            while(cursor.moveToNext()){
                long id=cursor.getLong(0);String bucket=cursor.getString(1),name=cursor.getString(2),path=cursor.getString(4);
                if(Build.VERSION.SDK_INT<29&&path!=null){int slash=path.lastIndexOf('/');path=slash<0?path:path.substring(0,slash);}
                if(name==null||name.isEmpty())name=path==null?"Без названия":path.replaceAll("/$","").replaceAll("^.*/","");
                photos.add(new Photo(ContentUris.withAppendedId(collection,id),(path==null?"":path)+"|"+bucket,name,cursor.getLong(3)));
            }
        }
    }
    private void load(){
        final int token=++generation;content.removeAllViews();content.addView(text("Загружаем альбомы…",17));
        worker.execute(()->{try{ArrayList<Photo> photos=new ArrayList<>();if(imagesAllowed()||partialAllowed())query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,photos);if(withVideo&&(videosAllowed()||partialAllowed()))query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,photos);List<Album> result=albums(photos);runOnUiThread(()->{if(isFinishing()||isDestroyed()||token!=generation)return;allAlbums=result;opened=null;showAlbums();});}catch(Exception error){runOnUiThread(()->{if(!isFinishing()){content.removeAllViews();content.addView(text("Не удалось прочитать альбомы. Проверьте доступ к фото в настройках приложения.",17));}});}});
    }
    private void showAlbums(){
        opened=null;title.setText("Альбомы");content.removeAllViews();LinearLayout page=new LinearLayout(this);page.setOrientation(LinearLayout.VERTICAL);
        if(!fullAccess()){Button banner=button("Доступ ограничен. Разрешить все фото");banner.setOnClickListener(v->openSettings());page.addView(banner);}
        if(allAlbums.isEmpty())page.addView(text("Доступных фотографий пока нет.",17));
        ListView list=new ListView(this);list.setDividerHeight(dp(1));list.setAdapter(new BaseAdapter(){public int getCount(){return allAlbums.size();}public Object getItem(int i){return allAlbums.get(i);}public long getItemId(int i){return i;}public View getView(int position,View convert,ViewGroup parent){Album album=allAlbums.get(position);LinearLayout row=new LinearLayout(AlbumPickerActivity.this);row.setPadding(dp(12),dp(8),dp(12),dp(8));row.setGravity(Gravity.CENTER_VERTICAL);ImageView cover=new ImageView(AlbumPickerActivity.this);cover.setScaleType(ImageView.ScaleType.CENTER_CROP);row.addView(cover,new LinearLayout.LayoutParams(dp(76),dp(76)));LinearLayout labels=new LinearLayout(AlbumPickerActivity.this);labels.setOrientation(LinearLayout.VERTICAL);labels.setPadding(dp(16),0,0,0);labels.addView(text(album.name,18));labels.addView(text(String.valueOf(album.photos.size()),13));row.addView(labels,new LinearLayout.LayoutParams(0,-2,1));thumbnail(cover,album.photos.get(0));return row;}});
        list.setOnItemClickListener((parent,view,position,id)->showPhotos(allAlbums.get(position)));page.addView(list,new LinearLayout.LayoutParams(-1,0,1));content.addView(page,new FrameLayout.LayoutParams(-1,-1));
    }
    private void showPhotos(Album album){
        opened=album;title.setText(album.name);content.removeAllViews();GridView grid=new GridView(this);grid.setNumColumns(3);grid.setHorizontalSpacing(dp(3));grid.setVerticalSpacing(dp(3));
        BaseAdapter adapter=new BaseAdapter(){public int getCount(){return album.photos.size();}public Object getItem(int i){return album.photos.get(i);}public long getItemId(int i){return i;}public View getView(int position,View convert,ViewGroup parent){Photo photo=album.photos.get(position);FrameLayout cell=new FrameLayout(AlbumPickerActivity.this);int width=Math.max(dp(90),(getResources().getDisplayMetrics().widthPixels-dp(6))/3);cell.setLayoutParams(new android.widget.AbsListView.LayoutParams(-1,width));ImageView image=new ImageView(AlbumPickerActivity.this);image.setScaleType(ImageView.ScaleType.CENTER_CROP);cell.addView(image,new FrameLayout.LayoutParams(-1,-1));TextView check=text(selected.contains(photo.uri)?"✓":"○",25);check.setGravity(Gravity.CENTER);check.setBackgroundColor(selected.contains(photo.uri)?0xff8e58bb:0x99000000);FrameLayout.LayoutParams mark=new FrameLayout.LayoutParams(dp(34),dp(34),Gravity.TOP|Gravity.RIGHT);mark.setMargins(0,dp(5),dp(5),0);cell.addView(check,mark);cell.setContentDescription(selected.contains(photo.uri)?"Выбрано":"Выбрать фото");thumbnail(image,photo);return cell;}};
        grid.setAdapter(adapter);grid.setOnItemClickListener((parent,view,position,id)->{Uri uri=album.photos.get(position).uri;if(selected.contains(uri))selected.remove(uri);else{if(!multiple)selected.clear();selected.add(uri);}adapter.notifyDataSetChanged();updateCount();});content.addView(grid,new FrameLayout.LayoutParams(-1,-1));
    }
    private void updateCount(){done.setText("Добавить ("+selected.size()+")");done.setEnabled(!selected.isEmpty());}
    private void thumbnail(ImageView image,Photo photo){
        if(worker.isShutdown())return;
        String key=photo.uri.toString();image.setTag(key);Bitmap existing=cache.get(key);if(existing!=null){image.setImageBitmap(existing);return;}image.setBackgroundColor(0xff37383b);
        worker.execute(()->{try{Bitmap bitmap;if(Build.VERSION.SDK_INT>=29)bitmap=getContentResolver().loadThumbnail(photo.uri,new Size(240,240),null);else if(key.contains("/video/"))bitmap=MediaStore.Video.Thumbnails.getThumbnail(getContentResolver(),ContentUris.parseId(photo.uri),MediaStore.Video.Thumbnails.MINI_KIND,null);else bitmap=MediaStore.Images.Thumbnails.getThumbnail(getContentResolver(),ContentUris.parseId(photo.uri),MediaStore.Images.Thumbnails.MINI_KIND,null);if(bitmap==null)return;cache.put(key,bitmap);runOnUiThread(()->{if(!isFinishing()&&key.equals(image.getTag()))image.setImageBitmap(bitmap);});}catch(Exception ignored){}});
    }
    @Override public void onBackPressed(){if(opened!=null)showAlbums();else{setResult(RESULT_CANCELED);finish();}}
    @Override protected void onDestroy(){generation++;worker.shutdownNow();cache.evictAll();super.onDestroy();}
}
