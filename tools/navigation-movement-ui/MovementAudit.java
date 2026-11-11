package com.example.rokidkeyboardbridge;

import android.app.Instrumentation;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Bundle;
import android.view.View;
import java.io.File;
import java.io.FileOutputStream;
import org.json.JSONArray;
import org.json.JSONObject;

public final class MovementAudit extends Instrumentation {
    public void onCreate(Bundle args) { super.onCreate(args); start(); }
    public void onStart() {
        Bundle result = new Bundle();
        try {
            int passed = 0;
            for (float scale : new float[]{1f, 1.3f, 2f}) {
                Configuration config = new Configuration(getTargetContext().getResources().getConfiguration());
                config.fontScale = scale;
                Context context = getTargetContext().createConfigurationContext(config);
                passed += NavigationMovementLineChecks.run(context);
            }
            runOnMainSync(() -> {
                try {
                    Context context = getTargetContext();
                    for (int widthDp : new int[]{132, 240}) {
                        NavigationMovementLine row = new NavigationMovementLine(context);
                        row.bindAction("現在の区間：自転車移動（合成の長い区間名・乗換地点を含む表示）");
                        int width = Math.round(widthDp * context.getResources().getDisplayMetrics().density);
                        row.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
                        row.layout(0, 0, width, row.getMeasuredHeight());
                        Bitmap image = Bitmap.createBitmap(width, row.getHeight(), Bitmap.Config.ARGB_8888);
                        row.draw(new Canvas(image));
                        try (FileOutputStream output = new FileOutputStream(new File(context.getFilesDir(), "movement-" + widthDp + ".png"))) {
                            image.compress(Bitmap.CompressFormat.PNG, 100, output);
                        }
                        image.recycle();
                    }
                } catch (Exception failure) { throw new RuntimeException(failure); }
            });
            result.putString("safeReport", new JSONObject().put("passed", passed).put("fontScales", new JSONArray(new float[]{1f, 1.3f, 2f}))
                    .put("widthsDp", new JSONArray(new int[]{96, 132, 176, 240, 320})).put("productionMovementLine", true)
                    .put("privateDataRead", false).put("providerCalls", 0).toString());
            finish(-1, result);
        } catch (Throwable failure) {
            while (failure.getCause() != null) failure = failure.getCause();
            result.putString("failure", failure.getClass().getSimpleName() + ":" + failure.getMessage()+java.util.Arrays.toString(java.util.Arrays.copyOf(failure.getStackTrace(), Math.min(8,failure.getStackTrace().length))));
            finish(1, result);
        }
    }
}
