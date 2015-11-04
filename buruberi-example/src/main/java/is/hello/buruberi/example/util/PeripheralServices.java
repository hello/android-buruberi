package is.hello.buruberi.example.util;

import android.content.res.Resources;
import android.support.annotation.NonNull;

import is.hello.buruberi.bluetooth.stacks.PeripheralService;
import is.hello.buruberi.example.R;

public class PeripheralServices {
    public static CharSequence getDisplayName(@NonNull PeripheralService service) {
        return service.getUuid().toString();
    }

    public static CharSequence getDetails(@NonNull PeripheralService service, @NonNull Resources resources) {
        return resources.getString(R.string.peripheral_service_details_format,
                                   getTypeString(service.getType(), resources),
                                   service.getCharacteristics().size());
    }

    public static String getTypeString(int type, @NonNull Resources resources) {
        switch (type) {
            case PeripheralService.SERVICE_TYPE_PRIMARY: {
                return resources.getString(R.string.service_type_primary);
            }
            case PeripheralService.SERVICE_TYPE_SECONDARY: {
                return resources.getString(R.string.service_type_secondary);
            }
            default: {
                throw new IllegalArgumentException();
            }
        }
    }
}
