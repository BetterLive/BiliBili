package com.bilibili.lingxiao.home.mikan.ui

import android.content.Intent
import android.net.Uri
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.LinearLayoutManager
import android.view.View
import android.widget.ImageView
import com.bilibili.lingxiao.R
import com.bilibili.lingxiao.home.mikan.adapter.MiKanFallAdapter
import com.bilibili.lingxiao.home.mikan.MiKanPresenter
import com.bilibili.lingxiao.home.mikan.adapter.MikanAdapter
import com.bilibili.lingxiao.home.mikan.MikanView
import com.bilibili.lingxiao.home.mikan.model.MiKanFallData
import com.bilibili.lingxiao.home.mikan.model.MiKanRecommendData
import com.bilibili.lingxiao.home.region.ui.BangumiDetailActivity
import com.bilibili.lingxiao.utils.ToastUtil
import com.bilibili.lingxiao.utils.UIUtil
import com.bilibili.lingxiao.web.WebActivity
import com.camera.lingxiao.common.app.BaseFragment
import com.chad.library.adapter.base.BaseQuickAdapter
import kotlinx.android.synthetic.main.fragment_mikan.*
import kotlinx.android.synthetic.main.fragment_mikan.view.*
import kotlinx.android.synthetic.main.item_mikan_fall.*
import kotlinx.android.synthetic.main.mikan_content_cn.*
import kotlinx.android.synthetic.main.mikan_content_cn.view.*
import kotlinx.android.synthetic.main.mikan_content_jp.view.*
import kotlin.properties.Delegates

class MikanFragment :BaseFragment(), MikanView {
    private var miKanPresenter: MiKanPresenter =
        MiKanPresenter(this, this)
    private var mCNAdapter: MikanAdapter by Delegates.notNull()
    private var mJPAdapter: MikanAdapter by Delegates.notNull()
    private var mFallAdapter: MiKanFallAdapter by Delegates.notNull()

    private var mCNVideoList = arrayListOf<MiKanRecommendData.Result.Recommend.Info>()
    private var mJPVideoList = arrayListOf<MiKanRecommendData.Result.Recommend.Info>()
    private var mEditList = arrayListOf<MiKanFallData.Result>()

    private val mikanHeaderView by lazy {
        View.inflate(activity,R.layout.mikan_header,null)
    }
    override val contentLayoutId: Int
        get() = R.layout.fragment_mikan

    override fun initInject() {
        super.initInject()
        UIUtil.getUiComponent().inject(this)
    }

    override fun initWidget(root: View) {
        super.initWidget(root)
        var manager:LinearLayoutManager = GridLayoutManager(activity,3)
        mCNAdapter = MikanAdapter(R.layout.item_mikan_video, mCNVideoList)
        mJPAdapter = MikanAdapter(R.layout.item_mikan_video, mJPVideoList)

        root.recycerView.layoutManager = manager
        root.recycerView.adapter = mJPAdapter
        root.recycerView.isNestedScrollingEnabled = false

        var manager_cn:LinearLayoutManager = GridLayoutManager(activity,3)
        root.recycerView_cn.layoutManager = manager_cn
        root.recycerView_cn.adapter = mCNAdapter
        root.recycerView_cn.isNestedScrollingEnabled = false

        mFallAdapter = MiKanFallAdapter(R.layout.item_mikan_fall, mEditList)
        var manager_fall = LinearLayoutManager(activity)
        root.recyclerview_edit.layoutManager = manager_fall
        root.recyclerview_edit.adapter = mFallAdapter


        root.refresh.setOnRefreshListener({
            miKanPresenter.getBanGuMiRecommend()
        })
        root.refresh.setOnLoadMoreListener {
            it.finishLoadMore()
            return@setOnLoadMoreListener  //TODO: 暂时不上拉加载更多，因为NestedScrollView和RecyclerView，RecyclerView不会复用
            var cursor:Long? = mFallAdapter.data.get(mFallAdapter.itemCount -1).cursor
            if (cursor != null && cursor != 0L)
            miKanPresenter.getBanGuMiFall(cursor)
        }

        mJPAdapter.setOnItemClickListener { adapter, view, position ->
            val intent = Intent(
                context,
                BangumiDetailActivity::class.java
            )
            intent.putExtra("id",mJPVideoList[position].seasonId.toString())
            intent.putExtra("type","bangumi")
            startActivity(intent)
        }
        mCNAdapter.setOnItemClickListener { adapter, view, position ->
            val intent = Intent(
                context,
                BangumiDetailActivity::class.java
            )
            intent.putExtra("id",mCNVideoList[position].seasonId.toString())
            intent.putExtra("type","bangumi")
            startActivity(intent)
        }
        mFallAdapter.setOnItemClickListener { adapter, view, position ->
            var intent = Intent(context, WebActivity::class.java)
            intent.putExtra("uri",mEditList[position].link)
            intent.putExtra("title",mEditList[position].title)
            intent.putExtra("image",mEditList[position].cover)
            startActivity(intent)
        }
        root.refresh.setEnableNestedScroll(true)
        var emptyView = View.inflate(context,R.layout.layout_empty,null)
        var image = emptyView.findViewById<ImageView>(R.id.image_error)
        image.setImageDrawable(resources.getDrawable(R.drawable.img_holder_error_style3))
        mFallAdapter.setEmptyView(emptyView)
    }

    data class MikanData(var type:Int){
        var mikanRecommend:MiKanRecommendData.Result.Recommend.Info?= null
        var mikanFall:MiKanFallData.Result?= null
    }

    override fun onFirstVisiblity() {
        super.onFirstVisiblity()
        refresh.autoRefresh()
    }

    override fun onVisiblityChanged(visiblity: Boolean) {
        super.onVisiblityChanged(visiblity)
        if (visiblity && mCNAdapter.itemCount - mCNAdapter.headerLayoutCount - mCNAdapter.footerLayoutCount < 1){
            refresh.autoRefresh()
        }
    }

    override fun onGetMikanRecommend(data: MiKanRecommendData) {
        mCNVideoList.clear()
        mJPVideoList.clear()

        mCNAdapter.addData(data.result.recommendCn.recommend)
        mJPAdapter.addData(data.result.recommendJp.recommend)
        if (data.result.recommendCn.foot.size > 0){
            mikan_image_cn.setImageURI(Uri.parse(data.result.recommendCn.foot[0].cover))
            title_cn.text =data.result.recommendCn.foot[0].title
            content_cn.text = data.result.recommendCn.foot[0].desc
        }

        if (data.result.recommendJp.foot.size > 0){
            mikan_image.setImageURI(Uri.parse(data.result.recommendJp.foot[0].cover))
            title.text =data.result.recommendJp.foot[0].title
            content.text = data.result.recommendJp.foot[0].desc
        }
        miKanPresenter.getBanGuMiFall(0L)
    }

    override fun onGetMikanFall(data: MiKanFallData) {
        //mEditList.clear()
        mFallAdapter.addData(data.result)

        refresh.finishRefresh()
        refresh.finishLoadMore()

    }

    override fun showDialog() {

    }

    override fun diamissDialog() {
    }

    override fun showToast(text: String?) {
        ToastUtil.show(text)
        refresh.finishRefresh()
        refresh.finishLoadMore()
    }
}