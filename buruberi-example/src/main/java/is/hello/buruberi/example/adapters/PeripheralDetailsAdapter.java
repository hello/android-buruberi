package is.hello.buruberi.example.adapters;

import android.content.Context;
import android.content.res.Resources;
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
import is.hello.buruberi.example.util.GattPeripherals;
import is.hello.buruberi.example.util.PeripheralServices;

public class PeripheralDetailsAdapter extends RecyclerView.Adapter<PeripheralDetailsAdapter.BaseViewHolder> {
    private static final int TYPE_RECORD = 0;
    private static final int TYPE_SERVICE = 1;

    private final LayoutInflater inflater;
    private final Resources resources;
    private final OnItemClickListener onItemClickListener;

    private final List<Integer> advertisingDataRecords = new ArrayList<>();
    private final List<String> advertisingDataValues = new ArrayList<>();
    private final List<PeripheralService> services = new ArrayList<>();

    public PeripheralDetailsAdapter(@NonNull Context context,
                                    @NonNull OnItemClickListener onItemClickListener) {
        this.inflater = LayoutInflater.from(context);
        this.resources = context.getResources();
        this.onItemClickListener = onItemClickListener;

    }

    public void bindAdvertisingData(@NonNull AdvertisingData advertisingData) {
        final int oldSize = getItemCount();

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

        notifyDataSetChanged(oldSize);
    }

    public void bindServices(@NonNull List<PeripheralService> services) {
        final int oldSize = getItemCount();

        this.services.clear();
        this.services.addAll(services);

        notifyDataSetChanged(oldSize);
    }

    private void notifyDataSetChanged(int oldSize) {
        final int newSize = getItemCount();
        if (newSize > oldSize) {
            notifyItemRangeChanged(0, oldSize);
            notifyItemRangeInserted(oldSize, newSize - oldSize);
        } else if (newSize < oldSize) {
            notifyItemChanged(0, newSize);
            notifyItemRangeRemoved(newSize, oldSize - newSize);
        } else {
            notifyItemRangeChanged(0, newSize);
        }
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
            title.setText(GattPeripherals.getAdvertisingDataTypeString(type));

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
            title.setText(PeripheralServices.getDisplayName(service));
            detail.setText(PeripheralServices.getDetails(service, resources));
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
