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

import is.hello.buruberi.bluetooth.stacks.GattPeripheral;
import is.hello.buruberi.example.R;
import is.hello.buruberi.example.util.GattPeripherals;

public class ScanResultsAdapter extends RecyclerView.Adapter<ScanResultsAdapter.ViewHolder> {
    private final LayoutInflater inflater;
    private final Resources resources;
    private final List<GattPeripheral> peripherals = new ArrayList<>();
    private final OnItemClickListener onItemClickListener;

    public ScanResultsAdapter(@NonNull Context context,
                              @NonNull OnItemClickListener onItemClickListener) {
        this.inflater = LayoutInflater.from(context);
        this.resources = context.getResources();
        this.onItemClickListener = onItemClickListener;
    }

    public void clear() {
        final int oldSize = peripherals.size();
        peripherals.clear();
        notifyDataSetChanged(oldSize);
    }

    public void addPeripherals(@NonNull List<GattPeripheral> newPeripherals) {
        final int oldSize = getItemCount();
        peripherals.addAll(newPeripherals);
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
    public int getItemCount() {
        return peripherals.size();
    }

    public GattPeripheral getItem(int position) {
        return peripherals.get(position);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        final View view = inflater.inflate(R.layout.item_scan_result, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        final GattPeripheral peripheral = getItem(position);
        holder.title.setText(GattPeripherals.getDisplayName(peripheral, resources));
        holder.details.setText(GattPeripherals.getDetails(peripheral, resources));
    }

    class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        final TextView title;
        final TextView details;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            this.title = (TextView) itemView.findViewById(R.id.item_scan_result_title);
            this.details = (TextView) itemView.findViewById(R.id.item_scan_result_details);

            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View sender) {
            final int adapterPosition = getAdapterPosition();
            if (adapterPosition != RecyclerView.NO_POSITION) {
                onItemClickListener.onItemClick(adapterPosition, getItem(adapterPosition));
            }
        }
    }

    public interface OnItemClickListener {
        void onItemClick(int position, @NonNull GattPeripheral peripheral);
    }
}
