package is.hello.buruberi.example.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.support.annotation.NonNull;

import is.hello.buruberi.example.R;

public class LoadingDialog extends Dialog {
    public LoadingDialog(@NonNull Context context) {
        super(context, R.style.AppTheme_LoadingDialog);

        setContentView(R.layout.dialog_loading);
        setCancelable(false);
        setCanceledOnTouchOutside(false);
    }
}
