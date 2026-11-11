package com.example.rokidkeyboardbridge;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Handler;
import android.os.Looper;
import android.text.Layout;
import android.text.TextUtils;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/** Actual Android text shaping/ellipsis on detached synthetic views; no activity, location or network. */
public final class NavigationMovementLineChecks {
    private static int checks;
    private static void ok(boolean value, String reason) {
        checks++;
        if (!value) throw new AssertionError("movement row " + checks + ": " + reason);
    }

    public static int run(Context context) throws Exception {
        if (Looper.myLooper() == Looper.getMainLooper()) return onMain(context);
        CountDownLatch done = new CountDownLatch(1);
        AtomicReference<Throwable> error = new AtomicReference<>();
        int[] result = new int[1];
        new Handler(Looper.getMainLooper()).post(() -> {
            try { result[0] = onMain(context); } catch (Throwable failed) { error.set(failed); }
            finally { done.countDown(); }
        });
        if (!done.await(10, TimeUnit.SECONDS)) throw new AssertionError("movement row UI check timed out");
        if (error.get() != null) throw new AssertionError("movement row UI check failed", error.get());
        return result[0];
    }

    private static void measure(View view, int width) {
        if (view.getLayoutParams() == null) view.setLayoutParams(new LinearLayout.LayoutParams(width, -2));
        view.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        view.layout(0, 0, view.getMeasuredWidth(), view.getMeasuredHeight());
    }

    private static int onMain(Context context) {
        checks = 0;
        NavigationMovementLine row = new NavigationMovementLine(context);
        float density = context.getResources().getDisplayMetrics().density;
        ok(Math.abs(row.getTextSize() - 9f * density) < .1f, "physical font remains 9dp without text-length shrinking");
        ok(row.getMaxLines() == 1 && row.getEllipsize() == TextUtils.TruncateAt.END, "one-line END policy");
        ok(row.getIncludeFontPadding(), "retain full ascent/descent");
        ok(row.getVisibility() == View.GONE, "no empty row reserves HUD space");
        ok(row.remainingLines(8) == 8, "hidden row does not consume the existing guidance budget");
        for (String cue : new String[]{"次にすること：乗車を確認", "次にすること：降車地点を確認（合成駅）", "経路情報：合成出口", "乗車・降車は操作で確認"}) {
            ok(!row.bindAction(cue) && row.getVisibility() == View.GONE, "important instructions are not consumed: " + cue);
        }
        for (String movement : new String[]{"徒歩移動", "自転車移動", "車で移動", "移動手段未確認"}) {
            String value = "現在の区間：" + movement;
            ok(row.bindAction(value), "known presentation status is consumed");
            ok(row.getText().toString().equals(value) && row.getContentDescription().toString().equals(value), "transport content and full accessible text are unchanged");
            measure(row, Math.round(240 * density));
            ok(row.getLayout().getLineCount() == 1, "short movement occupies exactly one line");
        }

        StringBuilder longText = new StringBuilder("現在の区間：自転車移動 ");
        for (int i = 0; i < 18; i++) longText.append("合成の長い区間名・🚲乗換地点 ");
        String full = longText.toString().trim();
        for (int widthDp : new int[]{96, 132, 176, 240, 320}) {
            int width = Math.max(1, Math.round(widthDp * density));
            row.bindAction(full);
            measure(row, width);
            Layout layout = row.getLayout();
            ok(layout.getLineCount() == 1, "long text must never wrap at width " + widthDp);
            ok(layout.getEllipsisCount(0) > 0, "long label visibly indicates END omission");
            ok(layout.getLineRight(0) <= width - row.getCompoundPaddingLeft() - row.getCompoundPaddingRight() + 1, "ellipsis stays in its assigned width");
            ok(row.getText().toString().equals(full) && row.getContentDescription().toString().equals(full), "ellipsis does not delete the underlying status");
            ok(Math.abs(row.getTextSize() - 9f * density) < .1f, "narrow widths do not reduce legibility below 9dp");
            Paint.FontMetricsInt font = row.getPaint().getFontMetricsInt();
            ok(row.getHeight() >= font.bottom - font.top + row.getCompoundPaddingTop() + row.getCompoundPaddingBottom(), "small row still reserves complete font metrics");

            Bitmap bitmap = Bitmap.createBitmap(width, row.getHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            canvas.drawColor(Color.BLACK);
            row.draw(canvas);
            int visible = 0;
            for (int y = 0; y < bitmap.getHeight(); y++) for (int x = 0; x < bitmap.getWidth(); x++) {
                int pixel = bitmap.getPixel(x, y);
                if (Color.green(pixel) > 50) visible++;
            }
            ok(visible > 10, "actual text paints at the compact size");
            bitmap.recycle();

            // A following guidance row stays below one status line even for a very long movement name.
            LinearLayout column = new LinearLayout(context);
            column.setOrientation(LinearLayout.VERTICAL);
            column.addView(row, new LinearLayout.LayoutParams(-1, -2));
            TextView next = new TextView(context);
            next.setText("合成の次案内\n合成の到着予定");
            next.setTextSize(11f);
            column.addView(next, new LinearLayout.LayoutParams(-1, -2));
            measure(column, width);
            int longTop = next.getTop(), longColumnHeight = column.getMeasuredHeight();
            row.bindAction("現在の区間：自転車移動");
            measure(column, width);
            ok(next.getTop() == longTop && column.getMeasuredHeight() == longColumnHeight,
                    "long movement status cannot consume another guidance line");
            column.removeView(row);
        }
        for (int budget : new int[]{4, 6, 8}) {
            row.bindAction("現在の区間：自転車移動");
            ok(row.remainingLines(budget) == budget - 1, "status row is included in the old total line budget");
            LinearLayout column = new LinearLayout(context);
            column.setOrientation(LinearLayout.VERTICAL);
            column.addView(row, new LinearLayout.LayoutParams(-1, -2));
            StringBuilder lines = new StringBuilder();
            for (int i = 0; i < budget; i++) lines.append(i == 0 ? "合成の案内" : "\n合成の案内");
            TextView guidance = new TextView(context);
            guidance.setTextSize(13.5f);
            guidance.setText(lines.toString());
            guidance.setMaxLines(row.remainingLines(budget));
            column.addView(guidance, new LinearLayout.LayoutParams(-1, -2));
            int width = Math.round(240 * density);
            measure(column, width);
            TextView previous = new TextView(context);
            previous.setTextSize(13.5f);
            previous.setText(lines.toString());
            previous.setMaxLines(budget);
            measure(previous, width);
            ok(column.getMeasuredHeight() <= previous.getMeasuredHeight(), "separated small status does not increase the former HUD height budget");
            column.removeView(row);
        }
        row.bindAction("現在の移動：徒歩\r\n合成区間\t続き");
        ok(!row.getText().toString().contains("\n") && !row.getText().toString().contains("\r") && !row.getText().toString().contains("\t"), "provider control whitespace cannot force additional lines");
        ok(!row.bindAction(null) && row.getText().length() == 0 && row.getVisibility() == View.GONE, "stop/reset clears stale movement and its reserved space");
        ok(row.remainingLines(8) == 8, "reset restores the original complete guidance budget");
        ok(!row.bindAction("目的地に到着しました"), "arrival message remains in ordinary guidance");
        return checks;
    }
}
