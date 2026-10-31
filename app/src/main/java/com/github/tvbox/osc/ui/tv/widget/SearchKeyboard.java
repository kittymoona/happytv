package com.github.tvbox.osc.ui.tv.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.util.TypedValue;

import androidx.annotation.AttrRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.chad.library.adapter.base.BaseMultiItemQuickAdapter;
import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.chad.library.adapter.base.entity.MultiItemEntity;
import com.github.tvbox.osc.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import me.jessyan.autosize.utils.AutoSizeUtils;


/**
 * @author pj567
 * @date :2020/12/23
 * @description:
 */
public class SearchKeyboard extends FrameLayout {
    private RecyclerView mRecyclerView;
    private KeyboardAdapter keyboardAdapter;
    private List<String> keys = Arrays.asList("远程搜索", "删除", "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z", "1", "2", "3", "4", "5", "6", "7", "8", "9", "0");
    private List<Keyboard> keyboardList = new ArrayList<>();
    private OnSearchKeyListener searchKeyListener;
    private OnFocusChangeListener focusChangeListener = new OnFocusChangeListener() {
        @Override
        public void onFocusChange(View itemView, boolean hasFocus) {
            if (null != itemView && itemView != mRecyclerView) {
                itemView.setSelected(hasFocus);
                itemView.animate().scaleX(hasFocus ? 1.04f : 1f)
                        .scaleY(hasFocus ? 1.04f : 1f).setDuration(120).start();
            }
        }
    };

    public SearchKeyboard(@NonNull Context context) {
        this(context, null);
    }

    public SearchKeyboard(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SearchKeyboard(@NonNull Context context, @Nullable AttributeSet attrs, @AttrRes int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView();
    }

    private void initView() {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.layout_keyborad, this);
        mRecyclerView = (RecyclerView) view.findViewById(R.id.mRecyclerView);
        GridLayoutManager manager = new GridLayoutManager(getContext(), 6);
        mRecyclerView.setLayoutManager(manager);
        mRecyclerView.addOnChildAttachStateChangeListener(new RecyclerView.OnChildAttachStateChangeListener() {
            @Override
            public void onChildViewAttachedToWindow(@NonNull View child) {
                if (child.isFocusable() && null == child.getOnFocusChangeListener()) {
                    child.setOnFocusChangeListener(focusChangeListener);
                }
            }

            @Override
            public void onChildViewDetachedFromWindow(@NonNull View view) {

            }
        });
        int size = keys.size();
        for (int i = 0; i < size; i++) {
            keyboardList.add(new Keyboard(1, keys.get(i)));
        }
        final KeyboardAdapter adapter = new KeyboardAdapter(keyboardList);
        keyboardAdapter = adapter;
        mRecyclerView.setAdapter(adapter);
        adapter.setSpanSizeLookup(new BaseQuickAdapter.SpanSizeLookup() {
            @Override
            public int getSpanSize(GridLayoutManager gridLayoutManager, int position) {
                if (position == 0)
                    return 3;
                else if (position == 1)
                    return 3;
                return 1;
            }
        });

        adapter.setOnItemClickListener(new BaseQuickAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(BaseQuickAdapter adapter, View view, int position) {
                Keyboard keyboard = (Keyboard) adapter.getItem(position);
                if (searchKeyListener != null) {
                    searchKeyListener.onSearchKey(position, keyboard.getKey());
                }
            }
        });
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (h <= 0 || keyboardAdapter == null) return;
        // Seven rows always fit the available space, including TV and tablet ratios.
        int margin = getResources().getDimensionPixelSize(R.dimen.vs_4);
        final int rowHeight = Math.max(1,
                (h - mRecyclerView.getPaddingTop() - mRecyclerView.getPaddingBottom()) / 7 - margin * 2);
        mRecyclerView.post(() -> {
            if (keyboardAdapter.rowHeight != rowHeight) {
                keyboardAdapter.rowHeight = rowHeight;
                keyboardAdapter.notifyDataSetChanged();
            }
        });
    }

    static class Keyboard implements MultiItemEntity {
        private int itemType;
        private String key;

        private Keyboard(int itemType, String key) {
            this.itemType = itemType;
            this.key = key;
        }

        @Override
        public int getItemType() {
            return itemType;
        }

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }
    }

    private static class KeyboardAdapter extends BaseMultiItemQuickAdapter<Keyboard, BaseViewHolder> {
        private int rowHeight;

        private KeyboardAdapter(List<Keyboard> data) {
            super(data);
            addItemType(1, R.layout.item_keyboard);
        }

        @Override
        protected void convert(BaseViewHolder helper, Keyboard item) {
            switch (helper.getItemViewType()) {
                case 1:
                    String label = item.key.equals("远程搜索") ? "手机输入"
                            : item.key.equals("删除") ? "退格删除" : item.key;
                    helper.setText(R.id.keyName, label);
                    TextView name = helper.getView(R.id.keyName);
                    name.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                            AutoSizeUtils.mm2px(mContext, item.key.length() > 1 ? 24 : 32));
                    if (rowHeight > 0) {
                        helper.itemView.getLayoutParams().height = rowHeight;
                    }
                    break;
            }
        }
    }

    public void setOnSearchKeyListener(OnSearchKeyListener listener) {
        searchKeyListener = listener;
    }

    public interface OnSearchKeyListener {
        void onSearchKey(int pos, String key);
    }
}
