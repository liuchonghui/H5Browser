package test.android.h5browser;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

public class MainActivity extends AppCompatActivity {

    String url = "https://gist.github.com";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = null;
                // u can
//                intent = new Intent("tools.android.h5browser.launch_action");
//                intent.putExtra("url", url);

                // also
//                intent = new Intent(Intent.ACTION_VIEW);
//                intent.setData(Uri.parse("h5br://launch?url=" + url));

                // and also
                intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse("h5br://launch/h5?url=" + url));

                try {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }
        });
    }
}
