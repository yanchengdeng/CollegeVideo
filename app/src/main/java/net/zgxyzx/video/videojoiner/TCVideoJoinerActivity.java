package net.zgxyzx.video.videojoiner;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import net.zgxyzx.video.R;
import net.zgxyzx.video.common.utils.TCConstants;
import net.zgxyzx.video.common.utils.TCVideoFileInfo;
import net.zgxyzx.video.videojoiner.swipemenu.Closeable;
import net.zgxyzx.video.videojoiner.swipemenu.OnSwipeMenuItemClickListener;
import net.zgxyzx.video.videojoiner.swipemenu.SwipeMenu;
import net.zgxyzx.video.videojoiner.swipemenu.SwipeMenuCreator;
import net.zgxyzx.video.videojoiner.swipemenu.SwipeMenuItem;
import net.zgxyzx.video.videojoiner.swipemenu.SwipeMenuRecyclerView;
import net.zgxyzx.video.videojoiner.swipemenu.touch.OnItemMoveListener;

import java.util.ArrayList;
import java.util.Collections;

public class TCVideoJoinerActivity extends Activity implements View.OnClickListener {

    private static final String TAG = TCVideoJoinerActivity.class.getSimpleName();

    private Context mContext;
    private ArrayList<TCVideoFileInfo> mTCVideoFileInfoList;
    private MenuAdapter mMenuAdapter;

    private Button mBtnPreview;
    private TextView mBtnBack;
    private SwipeMenuRecyclerView mSwipeMenuRecyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_video_joiner);
        mTCVideoFileInfoList = (ArrayList<TCVideoFileInfo>) getIntent().getSerializableExtra(TCConstants.INTENT_KEY_MULTI_CHOOSE);
        if (mTCVideoFileInfoList == null || mTCVideoFileInfoList.size() == 0) {
            finish();
            return;
        }
        mContext = this;
        init();
    }

    private void init() {
        mBtnBack = (TextView) findViewById(R.id.btn_back);
        mBtnBack.setOnClickListener(this);

        mBtnPreview = (Button) findViewById(R.id.segment_preview);
        mBtnPreview.setOnClickListener(this);

        mSwipeMenuRecyclerView = (SwipeMenuRecyclerView) findViewById(R.id.swipe_menu_recycler_view);
        mSwipeMenuRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mSwipeMenuRecyclerView.setHasFixedSize(true);
        mSwipeMenuRecyclerView.setItemAnimator(new DefaultItemAnimator()); // 设置Item默认动画
        mSwipeMenuRecyclerView.addItemDecoration(new ListViewDecoration());// 添加分割线。

        mSwipeMenuRecyclerView.setSwipeMenuCreator(swipeMenuCreator);
        mSwipeMenuRecyclerView.setSwipeMenuItemClickListener(menuItemClickListener);

        mMenuAdapter = new MenuAdapter(this, mTCVideoFileInfoList);
        mMenuAdapter.setOnItemDeleteListener(onItemDeleteListener);
        mSwipeMenuRecyclerView.setAdapter(mMenuAdapter);

        mSwipeMenuRecyclerView.setLongPressDragEnabled(true);
        mSwipeMenuRecyclerView.setOnItemMoveListener(onItemMoveListener);
    }

    private OnItemMoveListener onItemMoveListener = new OnItemMoveListener() {
        @Override
        public boolean onItemMove(int fromPosition, int toPosition) {
            // 当Item被拖拽的时候。
            Collections.swap(mTCVideoFileInfoList, fromPosition, toPosition);
            mMenuAdapter.notifyItemMoved(fromPosition, toPosition);
            return true;
        }

        @Override
        public void onItemDismiss(int position) {
            // 当Item被滑动删除掉的时候，在这里是无效的，因为这里没有启用这个功能。
            // 使用Menu时就不用使用这个侧滑删除啦，两个是冲突的。
        }
    };


    private SwipeMenuCreator swipeMenuCreator = new SwipeMenuCreator() {
        @Override
        public void onCreateMenu(SwipeMenu swipeLeftMenu, SwipeMenu swipeRightMenu, int viewType) {
            int width = getResources().getDimensionPixelSize(R.dimen.qav_multi_video_friend_item_width);
            int height = getResources().getDimensionPixelSize(R.dimen.qav_grid_view_item_width_audio);
            SwipeMenuItem deleteItem = new SwipeMenuItem(mContext)
                    .setBackgroundDrawable(R.color.btn_red)
                    .setText("删除")
                    .setWidth(width)
                    .setHeight(height);

            swipeRightMenu.addMenuItem(deleteItem);
        }
    };

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.segment_preview:
                if (mTCVideoFileInfoList == null || mTCVideoFileInfoList.size() < 2) {
                    Toast.makeText(this, "必须选择两个以上视频文件", Toast.LENGTH_SHORT).show();
                    return;
                }
                Intent intent = new Intent(TCVideoJoinerActivity.this, TCVideoJoinerPreviewActivity.class);
                intent.putExtra(TCConstants.INTENT_KEY_MULTI_CHOOSE, mTCVideoFileInfoList);
                startActivity(intent);
                break;
            case R.id.btn_back:
                finish();
                break;
        }
    }

    public interface OnDeleteListener {
        void onDelete(int position);
    }

    private OnDeleteListener onItemDeleteListener = new OnDeleteListener() {
        @Override
        public void onDelete(int position) {
            mSwipeMenuRecyclerView.smoothOpenRightMenu(position);
        }
    };

    private OnSwipeMenuItemClickListener menuItemClickListener = new OnSwipeMenuItemClickListener() {
        @Override
        public void onItemClick(Closeable closeable, int adapterPosition, int menuPosition, int direction) {
            closeable.smoothCloseMenu();// 关闭被点击的菜单。

            if (direction == SwipeMenuRecyclerView.RIGHT_DIRECTION) {
                mMenuAdapter.removeIndex(adapterPosition);
            }
        }
    };

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        }
        return true;
    }
}
