package com.github.tvbox.osc.ui.adapter;

import android.text.TextUtils;
import android.widget.ImageView;
import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.github.tvbox.osc.R;
import com.github.tvbox.osc.cache.VodCollect;
import com.github.tvbox.osc.util.ImgUtil;
import java.util.ArrayList;
import me.jessyan.autosize.utils.AutoSizeUtils;

public class HomeFavoritesAdapter extends BaseQuickAdapter<VodCollect, BaseViewHolder> {
    public HomeFavoritesAdapter() {
        super(R.layout.item_home_favorite, new ArrayList<>());
    }

    @Override
    protected void convert(BaseViewHolder helper, VodCollect item) {
        helper.setText(R.id.tvName, item.name);
        ImageView image = helper.getView(R.id.ivThumb);
        if (TextUtils.isEmpty(item.pic)) {
            image.setImageDrawable(ImgUtil.createTextDrawable(item.name));
        } else {
            ImgUtil.load(item.pic, image, AutoSizeUtils.mm2px(mContext, 10),
                    AutoSizeUtils.mm2px(mContext, 214), AutoSizeUtils.mm2px(mContext, 128), item.name);
        }
    }
}
