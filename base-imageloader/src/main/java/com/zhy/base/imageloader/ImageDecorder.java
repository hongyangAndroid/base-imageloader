package com.zhy.base.imageloader;

import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Build;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.webkit.MimeTypeMap;
import android.widget.ImageView;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by zhy on 15/8/11.
 */
public class ImageDecorder
{
    public static class ImageDecorderParams
    {
        String url;
        ImageUtils.ImageSize actualSize;
        ImageUtils.ImageSize expectSize;
        ImageView imageView;
    }

    /**
     * {@value}
     */
    public static final int DEFAULT_HTTP_CONNECT_TIMEOUT = 5 * 1000; // milliseconds
    /**
     * {@value}
     */
    public static final int DEFAULT_HTTP_READ_TIMEOUT = 20 * 1000; // milliseconds

    /**
     * {@value}
     */
    protected static final int BUFFER_SIZE = 32 * 1024; // 32 Kb
    /**
     * {@value}
     */
    protected static final String ALLOWED_URI_CHARS = "@#&=*+-_.,:!?()/~'%";

    protected static final int MAX_REDIRECT_COUNT = 5;

    protected static final String CONTENT_CONTACTS_URI_PREFIX = "content://com.android.contacts/";

    private static final String ERROR_UNSUPPORTED_SCHEME = "Imageloader doesn't support scheme(protocol) by default [%s]. " + "You should implement this support yourself (ImageDecorder.getStreamFromOtherSource(...))";

    protected final Context context;
    protected final int connectTimeout;
    protected final int readTimeout;

    public ImageDecorder(Context context)
    {
        this(context, DEFAULT_HTTP_CONNECT_TIMEOUT, DEFAULT_HTTP_READ_TIMEOUT);
    }

    public ImageDecorder(Context context, int connectTimeout, int readTimeout)
    {
        this.context = context.getApplicationContext();
        this.connectTimeout = connectTimeout;
        this.readTimeout = readTimeout;
    }

    public Bitmap decodeByStream(InputStream imageStream, ImageDecorderParams params) throws IOException
    {
        if (imageStream == null)
        {
            return null;
        }
        //根据imageStream获得实际的宽高
        params.actualSize = ImageUtils.getImageSize(imageStream);
        //重置流，企图压缩
        imageStream = resetStream(imageStream, params.url);
        Bitmap bitmap = decodeBitmap(imageStream, params);


        return bitmap;
    }

    public Bitmap decode(ImageDecorderParams params) throws IOException
    {
        String imageUrl = params.url;
        InputStream imageStream = getImageStream(imageUrl);
        return decodeByStream(imageStream, params);

    }

    protected InputStream resetStream(InputStream imageStream, String url) throws IOException
    {
        if (imageStream.markSupported())
        {
            try
            {
                imageStream.reset();
                return imageStream;
            } catch (IOException ignored)
            {
            }
        }
        IoUtils.closeSilently(imageStream);
        return getImageStream(url);
    }

    private Bitmap decodeBitmap(InputStream imageStream, ImageDecorderParams params)
    {
        Bitmap bitmap = null;
        try
        {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = false;
            options.inSampleSize = ImageUtils.calculateInSampleSize(params.actualSize, params.expectSize);
            L.e("inSampleSize =" + options.inSampleSize + " ," + params.actualSize + " , " + params.expectSize + " , url = " + params.url);
            bitmap = BitmapFactory.decodeStream(imageStream, null, options);
        } finally
        {
            IoUtils.closeSilently(imageStream);
        }
        return bitmap;
    }


    public InputStream getImageStream(String imageUri) throws IOException
    {
        switch (Scheme.ofUri(imageUri))
        {
            case HTTP:
            case HTTPS:
                return getStreamFromNetwork(imageUri);
            case FILE:
                return getStreamFromFile(imageUri);
            case CONTENT:
                return getStreamFromContent(imageUri);
            case ASSETS:
                return getStreamFromAssets(imageUri);
            case DRAWABLE:
                return getStreamFromDrawable(imageUri);
            case UNKNOWN:
            default:
                return getStreamFromOtherSource(imageUri);
        }
    }

    //Http/Https
    protected InputStream getStreamFromNetwork(String imageUri) throws IOException
    {
        HttpURLConnection conn = createConnection(imageUri);

        int redirectCount = 0;
        while (conn.getResponseCode() / 100 == 3 && redirectCount < MAX_REDIRECT_COUNT)
        {
            conn = createConnection(conn.getHeaderField("Location"));
            redirectCount++;
        }

        InputStream imageStream;
        try
        {
            imageStream = conn.getInputStream();
        } catch (IOException e)
        {
            // Read all data to allow reuse connection (http://bit.ly/1ad35PY)
            IoUtils.readAndCloseStream(conn.getErrorStream());
            throw e;
        }
        if (!shouldBeProcessed(conn))
        {
            IoUtils.closeSilently(imageStream);
            throw new IOException("Image request failed with response code " + conn.getResponseCode());
        }

        return new ContentLengthInputStream(new BufferedInputStream(imageStream, BUFFER_SIZE), conn.getContentLength());
    }


    protected HttpURLConnection createConnection(String url) throws IOException
    {
        String encodedUrl = Uri.encode(url, ALLOWED_URI_CHARS);
        HttpURLConnection conn = (HttpURLConnection) new URL(encodedUrl).openConnection();
        conn.setConnectTimeout(connectTimeout);
        conn.setReadTimeout(readTimeout);
        return conn;
    }

    protected boolean shouldBeProcessed(HttpURLConnection conn) throws IOException
    {
        return conn.getResponseCode() == 200;
    }

    //Schema.File
    protected InputStream getStreamFromFile(String imageUri) throws IOException
    {
        String filePath = Scheme.FILE.crop(imageUri);
        if (isVideoFileUri(imageUri))
        {
            return getVideoThumbnailStream(filePath);
        } else
        {
            BufferedInputStream imageStream = new BufferedInputStream(new FileInputStream(filePath), BUFFER_SIZE);
            return new ContentLengthInputStream(imageStream, (int) new File(filePath).length());
        }
    }

    private boolean isVideoFileUri(String uri)
    {
        String extension = MimeTypeMap.getFileExtensionFromUrl(uri);
        String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        return mimeType != null && mimeType.startsWith("video/");
    }


    @TargetApi(Build.VERSION_CODES.FROYO)
    private InputStream getVideoThumbnailStream(String filePath)
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO)
        {
            Bitmap bitmap = ThumbnailUtils
                    .createVideoThumbnail(filePath, MediaStore.Images.Thumbnails.FULL_SCREEN_KIND);
            if (bitmap != null)
            {
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.PNG, 0, bos);
                return new ByteArrayInputStream(bos.toByteArray());
            }
        }
        return null;
    }

    //---Schema.CONTENT

    protected InputStream getStreamFromContent(String imageUri) throws FileNotFoundException
    {
        ContentResolver res = context.getContentResolver();

        Uri uri = Uri.parse(imageUri);
        if (isVideoContentUri(uri))
        { // video thumbnail
            Long origId = Long.valueOf(uri.getLastPathSegment());
            Bitmap bitmap = MediaStore.Video.Thumbnails
                    .getThumbnail(res, origId, MediaStore.Images.Thumbnails.MINI_KIND, null);
            if (bitmap != null)
            {
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.PNG, 0, bos);
                return new ByteArrayInputStream(bos.toByteArray());
            }
        } else if (imageUri.startsWith(CONTENT_CONTACTS_URI_PREFIX))
        { // contacts photo
            return getContactPhotoStream(uri);
        }

        return res.openInputStream(uri);
    }

    private boolean isVideoContentUri(Uri uri)
    {
        String mimeType = context.getContentResolver().getType(uri);
        return mimeType != null && mimeType.startsWith("video/");
    }


    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    protected InputStream getContactPhotoStream(Uri uri)
    {
        ContentResolver res = context.getContentResolver();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH)
        {
            return ContactsContract.Contacts.openContactPhotoInputStream(res, uri, true);
        } else
        {
            return ContactsContract.Contacts.openContactPhotoInputStream(res, uri);
        }
    }

    //Schema.asset
    protected InputStream getStreamFromAssets(String imageUri) throws IOException
    {
        String filePath = Scheme.ASSETS.crop(imageUri);
        return context.getAssets().open(filePath);
    }

    //Schema.drawable
    protected InputStream getStreamFromDrawable(String imageUri)
    {
        String drawableIdString = Scheme.DRAWABLE.crop(imageUri);
        int drawableId = Integer.parseInt(drawableIdString);
        return context.getResources().openRawResource(drawableId);
    }

    //Schema.UNKNOWN
    protected InputStream getStreamFromOtherSource(String imageUri) throws IOException
    {
        throw new UnsupportedOperationException(String.format(ERROR_UNSUPPORTED_SCHEME, imageUri));
    }


}
