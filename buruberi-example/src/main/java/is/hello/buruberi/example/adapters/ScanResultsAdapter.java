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

import is.hello.buruberi.bluetooth.stacks.GattPeripheral;
import is.hello.buruberi.example.R;

public class ScanResultsAdapter extends RecyclerView.Adapter<ScanResultsAdapter.ViewHolder> {
    private final LayoutInflater inflater;
    private final List<GattPeripheral> peripherals = new ArrayList<>();

    public ScanResultsAdapter(@NonNull Context context) {
        this.inflater = LayoutInflater.from(context);
    }

    public void setPeripherals(@NonNull List<GattPeripheral> newPeripherals) {
        if (!peripherals.isEmpty()) {
            peripherals.clear();
            notifyItemRangeRemoved(0, peripherals.size());
        }

        peripherals.addAll(newPeripherals);
        if (!peripherals.isEmpty()) {
            notifyItemRangeInserted(0, newPeripherals.size());
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
        holder.title.setText(peripheral.getName());
        holder.details.setText(peripheral.getAddress());
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        final TextView title;
        final TextView details;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            this.title = (TextView) itemView.findViewById(R.id.item_scan_result_title);
            this.details = (TextView) itemView.findViewById(R.id.item_scan_result_details);
        }
    }
}
