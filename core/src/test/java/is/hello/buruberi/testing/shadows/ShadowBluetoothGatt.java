package is.hello.buruberi.testing.shadows;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.support.annotation.NonNull;
import android.util.Pair;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.StringDescription;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.fail;

@SuppressWarnings("unused")
@Implements(BluetoothGatt.class)
public class ShadowBluetoothGatt {
    private final List<Pair<Call, Object[]>> calls = new ArrayList<>();
    private BluetoothGattCallback gattCallback;
    private boolean autoConnect;
    private List<BluetoothGattService> services;


    public BluetoothGattCallback getGattCallback() {
        return gattCallback;
    }

    void setGattCallback(BluetoothGattCallback gattCallback) {
        this.gattCallback = gattCallback;
    }

    public boolean isAutoConnect() {
        return autoConnect;
    }

    void setAutoConnect(boolean autoConnect) {
        this.autoConnect = autoConnect;
    }

    @Implementation
    public boolean connect() {
        trackCall(Call.CONNECT);
        return true;
    }

    @Implementation
    public void disconnect() {
        trackCall(Call.DISCONNECT);
    }

    @Implementation
    public void close() {
        trackCall(Call.CLOSE);
    }

    @Implementation
    public boolean discoverServices() {
        trackCall(Call.DISCOVER_SERVICES);
        return true;
    }

    public void setServices(List<BluetoothGattService> services) {
        this.services = services;
    }

    @Implementation
    public List<BluetoothGattService> getServices() {
        trackCall(Call.GET_SERVICES);
        return services;
    }

    @Implementation
    public BluetoothGattService getService(UUID uuid) {
        trackCall(Call.GET_SERVICES, uuid);
        for (BluetoothGattService service : services) {
            if (service.getUuid().equals(uuid)) {
                return service;
            }
        }
        return null;
    }

    @Implementation
    public boolean setCharacteristicNotification(BluetoothGattCharacteristic characteristic,
                                                 boolean enable) {
        trackCall(Call.SET_CHAR_NOTIFICATION, characteristic, enable);
        return true;
    }

    @Implementation
    public boolean writeDescriptor(BluetoothGattDescriptor descriptor) {
        trackCall(Call.WRITE_DESCRIPTOR, descriptor);
        return true;
    }

    @Implementation
    public boolean writeCharacteristic(BluetoothGattCharacteristic characteristic) {
        trackCall(Call.WRITE_DESCRIPTOR, characteristic);
        return true;
    }


    //region Call Tracking

    public void trackCall(@NonNull Call call, @NonNull Object... arguments) {
        calls.add(Pair.create(call, arguments));
    }

    public void verifyCall(@NonNull Call call, @NonNull Matcher... matchers) {
        for (Pair<Call, Object[]> entry : calls) {
            if (entry.first == call) {
                Object[] arguments = entry.second;
                if (arguments.length != matchers.length) {
                    fail("expected " + matchers.length + " arguments " + " got " + arguments.length);
                }

                for (int i = 0; i < arguments.length; i++) {
                    Object argument = arguments[i];
                    Matcher matcher = matchers[i];
                    if (!matcher.matches(argument)) {
                        Description description = new StringDescription();
                        description.appendText(call + ": ");
                        matcher.describeMismatch(argument, description);
                        fail(description.toString());
                    }
                }
            }
        }
    }

    public void verifyCall(@NonNull Call call, @NonNull Object... objects) {
        Matcher[] matchers = new Matcher[objects.length];
        for (int i = 0; i < objects.length; i++) {
            matchers[i] = equalTo(objects[i]);
        }
        verifyCall(call, matchers);
    }

    public void clearCalls() {
        calls.clear();
    }

    public enum Call {
        CONNECT,
        DISCONNECT,
        CLOSE,
        DISCOVER_SERVICES,
        GET_SERVICES,
        GET_SERVICE,
        SET_CHAR_NOTIFICATION,
        WRITE_DESCRIPTOR,
        WRITE_CHAR,
    }

    //endregion
}
