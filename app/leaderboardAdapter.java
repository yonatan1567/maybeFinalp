package com.example.maybefinalp;
mport android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class leaderboardAdapter extends RecyclerView.Adapter<leaderboardAdapter.ViewHolder> {
    private List<User> userList;

    public LeaderboardAdapter(List<User> userList) {
        this.userList = userList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_leaderboard, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        User user = userList.get(position);
        holder.rank.setText("#" + (position + 1));
        holder.name.setText(user.getName());
        holder.coins.setText(user.getCoins() + " Coins");
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView rank, name, coins;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            rank = itemView.findViewById(R.id.rank);
            name = itemView.findViewById(R.id.name);
            coins = itemView.findViewById(R.id.coins);
        }
    }
}
