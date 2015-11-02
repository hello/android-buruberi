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

public class PeripheralDetailsAdapter extends RecyclerView.Adapter<PeripheralDetailsAdapter.BaseViewHolder> {
    private static final int TYPE_RECORD = 0;

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
    public int getItemViewType(int position) {
        return TYPE_RECORD;
    }

    @Override
    public int getItemCount() {
        return advertisingDataRecords.size();
    }

    @Override
    public BaseViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        final View view = inflater.inflate(R.layout.item_peripheral_record, parent, false);
        return new RecordViewHolder(view);
    }

    @Override
    public void onBindViewHolder(BaseViewHolder holder, int position) {
        holder.bind(position);
    }

    abstract class BaseViewHolder extends RecyclerView.ViewHolder {
        BaseViewHolder(@NonNull View itemView) {
            super(itemView);
        }

        abstract void bind(int position);
    }

    class RecordViewHolder extends BaseViewHolder {
        final TextView recordType;
        final TextView recordValues;

        RecordViewHolder(@NonNull View itemView) {
            super(itemView);

            this.recordType = (TextView) itemView.findViewById(R.id.item_peripheral_record_type);
            this.recordValues = (TextView) itemView.findViewById(R.id.item_peripheral_record_values);
        }

        @Override
        void bind(int position) {
            final Integer type = advertisingDataRecords.get(position);
            recordType.setText(AdvertisingData.typeToString(type));

            final String values = advertisingDataValues.get(position);
            recordValues.setText(values);
        }
    }
}
