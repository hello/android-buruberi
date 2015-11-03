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

import is.hello.buruberi.bluetooth.stacks.PeripheralService;
import is.hello.buruberi.bluetooth.stacks.util.AdvertisingData;
import is.hello.buruberi.bluetooth.stacks.util.Bytes;
import is.hello.buruberi.example.R;

public class PeripheralDetailsAdapter extends RecyclerView.Adapter<PeripheralDetailsAdapter.BaseViewHolder> {
    private static final int TYPE_RECORD = 0;
    private static final int TYPE_SERVICE = 1;

    private final LayoutInflater inflater;
    private final OnItemClickListener onItemClickListener;

    private final List<Integer> advertisingDataRecords = new ArrayList<>();
    private final List<String> advertisingDataValues = new ArrayList<>();
    private final List<PeripheralService> services = new ArrayList<>();

    public PeripheralDetailsAdapter(@NonNull Context context,
                                    @NonNull OnItemClickListener onItemClickListener) {
        this.inflater = LayoutInflater.from(context);
        this.onItemClickListener = onItemClickListener;

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

    public void bindServices(@NonNull List<PeripheralService> services) {
        this.services.clear();
        this.services.addAll(services);

        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        if (position < advertisingDataRecords.size()) {
            return TYPE_RECORD;
        } else {
            return TYPE_SERVICE;
        }
    }

    @Override
    public int getItemCount() {
        return advertisingDataRecords.size() + services.size();
    }

    @Override
    public BaseViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        final View view = inflater.inflate(R.layout.item_peripheral_detail, parent, false);
        switch (viewType) {
            case TYPE_RECORD: {
                return new RecordViewHolder(view);
            }
            case TYPE_SERVICE: {
                return new ServiceViewHolder(view);
            }
            default: {
                throw new IllegalArgumentException();
            }
        }
    }

    @Override
    public void onBindViewHolder(BaseViewHolder holder, int position) {
        holder.bind(position);
    }

    abstract class BaseViewHolder extends RecyclerView.ViewHolder
        implements View.OnClickListener {
        final TextView title;
        final TextView detail;

        BaseViewHolder(@NonNull View itemView) {
            super(itemView);

            this.title = (TextView) itemView.findViewById(R.id.item_peripheral_detail_title);
            this.detail = (TextView) itemView.findViewById(R.id.item_peripheral_detail_detail);

            itemView.setOnClickListener(this);
        }

        abstract void bind(int position);
    }

    class RecordViewHolder extends BaseViewHolder {
        RecordViewHolder(@NonNull View itemView) {
            super(itemView);
        }

        @Override
        void bind(int position) {
            final Integer type = advertisingDataRecords.get(position);
            title.setText(AdvertisingData.typeToString(type));

            final String values = advertisingDataValues.get(position);
            detail.setText(values);
        }

        @Override
        public void onClick(View ignored) {
            final int adapterPosition = getAdapterPosition();
            if (adapterPosition != RecyclerView.NO_POSITION) {
                onItemClickListener.onAdvertisingRecordClick(advertisingDataRecords.get(adapterPosition),
                                                             advertisingDataValues.get(adapterPosition));
            }
        }
    }

    class ServiceViewHolder extends BaseViewHolder {
        ServiceViewHolder(@NonNull View itemView) {
            super(itemView);
        }

        @Override
        void bind(int position) {
            final PeripheralService service = services.get(position - advertisingDataRecords.size());
            title.setText(service.getUuid().toString());

            final String type;
            switch (service.getType()) {
                case PeripheralService.SERVICE_TYPE_PRIMARY: {
                    type = "SERVICE_TYPE_PRIMARY";
                    break;
                }
                case PeripheralService.SERVICE_TYPE_SECONDARY: {
                    type = "SERVICE_TYPE_SECONDARY";
                    break;
                }
                default: {
                    throw new IllegalArgumentException();
                }
            }
            detail.setText(type);
        }

        @Override
        public void onClick(View ignored) {
            final int adapterPosition = getAdapterPosition();
            if (adapterPosition != RecyclerView.NO_POSITION) {
                final PeripheralService service =
                        services.get(adapterPosition - advertisingDataRecords.size());
                onItemClickListener.onServiceClick(service);
            }
        }
    }


    public interface OnItemClickListener {
        void onAdvertisingRecordClick(int type, @NonNull String value);
        void onServiceClick(@NonNull PeripheralService service);
    }
}
