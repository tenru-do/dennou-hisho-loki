package com.example.rokidgeminisecretary;
import android.graphics.Bitmap;
import android.graphics.Color;
import io.nayuki.qrcodegen.QrCode;
final class LokiConnectionQr {
    static Bitmap image(String link) {
        QrCode qr=QrCode.encodeText(link,QrCode.Ecc.MEDIUM);
        int scale=8,border=4,size=(qr.size+border*2)*scale;
        int[] pixels=new int[size*size];java.util.Arrays.fill(pixels,Color.WHITE);
        for(int y=0;y<qr.size;y++)for(int x=0;x<qr.size;x++)if(qr.getModule(x,y))
            for(int dy=0;dy<scale;dy++)for(int dx=0;dx<scale;dx++)pixels[((y+border)*scale+dy)*size+(x+border)*scale+dx]=Color.BLACK;
        return Bitmap.createBitmap(pixels,size,size,Bitmap.Config.ARGB_8888);
    }
}
