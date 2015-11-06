package is.hello.buruberi.example.adapters;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import is.hello.buruberi.bluetooth.stacks.util.AdvertisingData;
import is.hello.buruberi.bluetooth.stacks.util.Bytes;
import is.hello.buruberi.example.R;
import is.hello.buruberi.example.util.GattPeripherals;

public class PeripheralDetailsAdapter extends RecyclerView.Adapter<PeripheralDetailsAdapter.RecordViewHolder> {
    private final LayoutInflater inflater;

    private final List<Integer> advertisingDataRecords = new ArrayList<>();
    private final List<String> advertisingDataValues = new ArrayList<>();

    public PeripheralDetailsAdapter(@NonNull Context context) {
        this.inflater = LayoutInflater.from(context);

    }

    public void bindAdvertisingData(@NonNull AdvertisingData advertisingData) {
        advertisingDataRecords.clear();
        advertisingDataValues.clear();

        final List<Integer> recordTypes = advertisingData.copyRecordTypes();
        advertisingDataRecords.addAll(recordTypes);
        for (final Integer type : recordTypes) {
            final List<byte[]> recordsForType = advertisingData.getRecordsForType(type);
            if (recordsForType == null) {
                continue;
            }

            final StringBuilder valueBuilder = new StringBuilder();
            for (final byte[] record : recordsForType) {
                valueBuilder.append(Bytes.toString(record));
                valueBuilder.append('\n');
            }
            if (valueBuilder.length() > 0) { // Trailing \n
                valueBuilder.deleteCharAt(valueBuilder.length() - 1);
            }
            advertisingDataValues.add(valueBuilder.toString());
        }

        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return advertisingDataRecords.size();
    }

    @Override
    public RecordViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        final View view = inflater.inflate(R.layout.item_peripheral_detail, parent, false);
        return new RecordViewHolder(view);
    }

    @Override
    public void onBindViewHolder(RecordViewHolder holder, int position) {
        final Integer type = advertisingDataRecords.get(position);
        holder.title.setText(GattPeripherals.getAdvertisingDataTypeString(type));

        final String values = advertisingDataValues.get(position);
        holder.detail.setText(values);
    }

    class RecordViewHolder extends RecyclerView.ViewHolder {
        final TextView title;
        final TextView detail;

        RecordViewHolder(@NonNull View itemView) {
            super(itemView);

            this.title = (TextView) itemView.findViewById(R.id.item_peripheral_detail_title);
            this.detail = (TextView) itemView.findViewById(R.id.item_peripheral_detail_detail);
        }
    }
}
